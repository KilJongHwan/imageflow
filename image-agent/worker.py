import json
import os
import time
from io import BytesIO
from pathlib import Path

import redis
import requests
from PIL import Image, ImageDraw, ImageFont


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
                    "sourceFileSizeBytes": result["sourceFileSizeBytes"],
                    "resultFileSizeBytes": result["resultFileSizeBytes"],
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
    image = apply_crop_mode(image, job)

    target_width = job.get("targetWidth")
    target_height = job.get("targetHeight")
    if target_width:
        width = int(target_width)
        if target_height:
            height = int(target_height)
        else:
            ratio = width / image.width
            height = max(1, int(image.height * ratio))
        image = image.resize((width, height))

    output_format = normalize_format(job.get("outputFormat"))
    quality = int(job.get("quality") or 80)
    object_key = job.get("outputObjectKey") or f"optimized/{job['jobId']}.{output_format.lower()}"
    output_file_path = job.get("outputFilePath")
    result_image_url = job.get("resultImageUrl")

    output_buffer = BytesIO()
    image = apply_watermark(image, job.get("watermarkText"))
    save_image(image, output_buffer, output_format, quality)
    output_buffer.seek(0)

    upload_to_r2_or_local(object_key, output_buffer.getvalue(), output_format, output_file_path)

    return {
        "outputObjectKey": object_key,
        "resultImageUrl": result_image_url or f"{R2_PUBLIC_BASE_URL.rstrip('/')}/{object_key}",
        "sourceFileSizeBytes": source_size_bytes(job),
        "resultFileSizeBytes": len(output_buffer.getvalue()),
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


def apply_crop_mode(image, job):
    crop_mode = (job.get("cropMode") or "fit").strip().lower()
    aspect_ratio = (job.get("aspectRatio") or "original").strip().lower()

    if crop_mode == "manual":
        return apply_manual_crop(image, job)

    if crop_mode != "center-crop" or aspect_ratio == "original":
        return image

    ratio_width, ratio_height = parse_aspect_ratio(aspect_ratio)
    target_ratio = ratio_width / ratio_height
    current_ratio = image.width / image.height

    if current_ratio > target_ratio:
        new_width = int(image.height * target_ratio)
        left = (image.width - new_width) // 2
        return image.crop((left, 0, left + new_width, image.height))

    new_height = int(image.width / target_ratio)
    top = (image.height - new_height) // 2
    return image.crop((0, top, image.width, top + new_height))


def apply_manual_crop(image, job):
    crop_x = int(job.get("cropX") or 0)
    crop_y = int(job.get("cropY") or 0)
    crop_width = int(job.get("cropWidth") or 0)
    crop_height = int(job.get("cropHeight") or 0)

    if crop_width <= 0 or crop_height <= 0:
        raise ValueError("manual crop requires cropWidth and cropHeight")
    if crop_x < 0 or crop_y < 0:
        raise ValueError("manual crop requires non-negative cropX and cropY")
    if crop_x + crop_width > image.width or crop_y + crop_height > image.height:
        raise ValueError("manual crop must stay within the source image bounds")

    return image.crop((crop_x, crop_y, crop_x + crop_width, crop_y + crop_height))


def apply_watermark(image, watermark_text):
    if not watermark_text:
        return image

    if image.mode != "RGBA":
        image = image.convert("RGBA")

    overlay = Image.new("RGBA", image.size, (255, 255, 255, 0))
    draw = ImageDraw.Draw(overlay)
    font = ImageFont.load_default()
    text = watermark_text.strip()
    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]
    x = max(20, image.width - text_width - 28)
    y = max(20, image.height - text_height - 28)
    draw.rounded_rectangle(
        (x - 12, y - 8, x + text_width + 12, y + text_height + 8),
        radius=14,
        fill=(17, 24, 39, 120),
    )
    draw.text((x, y), text, fill=(255, 255, 255, 220), font=font)
    return Image.alpha_composite(image, overlay)


def parse_aspect_ratio(aspect_ratio):
    try:
        left, right = aspect_ratio.split(":")
        return float(left), float(right)
    except ValueError as error:
        raise ValueError(f"invalid aspect ratio: {aspect_ratio}") from error


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


def source_size_bytes(job):
    source_file_path = job.get("sourceFilePath")
    if source_file_path:
        source_path = Path(source_file_path)
        if source_path.exists():
            return source_path.stat().st_size
    return None


if __name__ == "__main__":
    while True:
        try:
            main()
        except Exception as error:
            print(f"[worker] fatal loop error: {error}")
            time.sleep(3)
