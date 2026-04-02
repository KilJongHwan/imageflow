import json
import os
import time
from io import BytesIO
from pathlib import Path

import redis
import requests
from PIL import Image


REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))
QUEUE_KEY = os.getenv("IMAGE_JOB_QUEUE_KEY", "imageflow:image-jobs")
BACKEND_BASE_URL = os.getenv("BACKEND_BASE_URL", "http://localhost:8080")
R2_PUBLIC_BASE_URL = os.getenv("R2_PUBLIC_BASE_URL", "https://example-r2-public-url.invalid")
POLL_TIMEOUT_SECONDS = int(os.getenv("POLL_TIMEOUT_SECONDS", "5"))


def main():
    client = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, decode_responses=True)
    print(f"[worker] listening on redis://{REDIS_HOST}:{REDIS_PORT} queue={QUEUE_KEY}")

    while True:
        _, payload = client.blpop(QUEUE_KEY, timeout=POLL_TIMEOUT_SECONDS) or (None, None)
        if payload is None:
            continue

        job = json.loads(payload)
        print(f"[worker] received job {job['jobId']}")

        try:
            update_job(job["jobId"], {"status": "PROCESSING"})
            result = optimize_image(job)
            update_job(
                job["jobId"],
                {
                    "status": "SUCCEEDED",
                    "resultImageUrl": result["resultImageUrl"],
                    "outputObjectKey": result["outputObjectKey"],
                },
            )
            print(f"[worker] job succeeded {job['jobId']}")
        except Exception as error:
            update_job(
                job["jobId"],
                {
                    "status": "FAILED",
                    "failureReason": str(error),
                },
            )
            print(f"[worker] job failed {job['jobId']}: {error}")


def optimize_image(job):
    image = load_source_image(job)
    target_width = job.get("targetWidth")
    if target_width:
        width = int(target_width)
        ratio = width / image.width
        target_height = max(1, int(image.height * ratio))
        image = image.resize((width, target_height))

    output_format = normalize_format(job.get("outputFormat"))
    quality = int(job.get("quality") or 80)
    object_key = job.get("outputObjectKey") or f"optimized/{job['jobId']}.{output_format.lower()}"
    output_file_path = job.get("outputFilePath")
    result_image_url = job.get("resultImageUrl")

    output_buffer = BytesIO()
    save_image(image, output_buffer, output_format, quality)
    output_buffer.seek(0)

    upload_to_r2_or_local(object_key, output_buffer.getvalue(), output_format, output_file_path)

    return {
        "outputObjectKey": object_key,
        "resultImageUrl": result_image_url or f"{R2_PUBLIC_BASE_URL.rstrip('/')}/{object_key}",
    }


def normalize_format(output_format):
    if not output_format:
        return "WEBP"
    normalized = output_format.strip().upper()
    if normalized == "JPG":
        return "JPEG"
    return normalized


def save_image(image, output_buffer, output_format, quality):
    if output_format in {"JPEG", "WEBP"} and image.mode in ("RGBA", "LA"):
        image = image.convert("RGB")
    image.save(output_buffer, format=output_format, quality=quality, optimize=True)


def load_source_image(job):
    source_file_path = job.get("sourceFilePath")
    if source_file_path:
        source_path = Path(source_file_path)
        if source_path.exists():
            return Image.open(source_path)

    source_url = job.get("sourceImageUrl")
    if not source_url:
        raise ValueError("source image path or url is required")

    response = requests.get(source_url, timeout=30)
    response.raise_for_status()
    return Image.open(BytesIO(response.content))


def upload_to_r2_or_local(object_key, data, output_format, output_file_path):
    access_key = os.getenv("R2_ACCESS_KEY_ID")
    secret_key = os.getenv("R2_SECRET_ACCESS_KEY")
    bucket_name = os.getenv("R2_BUCKET_NAME")
    endpoint = os.getenv("R2_ENDPOINT")

    if not all([access_key, secret_key, bucket_name, endpoint]):
        if output_file_path:
            output_path = Path(output_file_path)
            output_path.parent.mkdir(parents=True, exist_ok=True)
            output_path.write_bytes(data)
            print(f"[worker] saved optimized file locally: {output_path}")
            return
        print("[worker] R2 credentials not set, simulating upload only")
        return

    import boto3

    content_type = {
        "WEBP": "image/webp",
        "JPEG": "image/jpeg",
        "PNG": "image/png",
    }.get(output_format, "application/octet-stream")

    s3_client = boto3.client(
        "s3",
        endpoint_url=endpoint,
        aws_access_key_id=access_key,
        aws_secret_access_key=secret_key,
    )
    s3_client.put_object(
        Bucket=bucket_name,
        Key=object_key,
        Body=data,
        ContentType=content_type,
    )


def update_job(job_id, payload):
    response = requests.patch(
        f"{BACKEND_BASE_URL.rstrip('/')}/api/image-jobs/{job_id}/result",
        json=payload,
        timeout=15,
    )
    response.raise_for_status()


if __name__ == "__main__":
    while True:
        try:
            main()
        except Exception as error:
            print(f"[worker] fatal loop error: {error}")
            time.sleep(3)
