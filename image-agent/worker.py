import json
import logging
import os
import time
from concurrent.futures import ThreadPoolExecutor
from io import BytesIO
from pathlib import Path
from urllib.parse import urlparse, urlunparse

import redis
import requests
from PIL import Image, ImageDraw, ImageFont, ImageOps


# 타임스탬프·레벨·메시지에 실은 jobId 형태의 구조화 로그. 같은 jobId를 백엔드도 남기므로
# job 하나를 워커와 백엔드 로그에서 함께 grep으로 추적할 수 있다.
logging.basicConfig(
    level=os.getenv("LOG_LEVEL", "INFO").upper(),
    format="%(asctime)s %(levelname)-5s [worker] %(message)s",
)
logger = logging.getLogger("imageflow.worker")


REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))
REDIS_URL = os.getenv("REDIS_URL", "")
QUEUE_KEY = os.getenv("IMAGE_JOB_QUEUE_KEY", "imageflow:image-jobs")
BACKEND_BASE_URL = os.getenv("BACKEND_BASE_URL", "")
BACKEND_HOSTPORT = os.getenv("BACKEND_HOSTPORT", "")
R2_PUBLIC_BASE_URL = os.getenv("R2_PUBLIC_BASE_URL", "https://example-r2-public-url.invalid")
STORAGE_ROOT = os.getenv("STORAGE_ROOT", "")
POLL_TIMEOUT_SECONDS = int(os.getenv("POLL_TIMEOUT_SECONDS", "5"))
WORKER_CONCURRENCY = max(1, int(os.getenv("WORKER_CONCURRENCY", "3")))
WORKER_RESULT_CALLBACK_ENABLED = os.getenv("WORKER_RESULT_CALLBACK_ENABLED", "false").lower() == "true"


def resolve_backend_base_url():
    if BACKEND_BASE_URL:
        return BACKEND_BASE_URL.rstrip("/")
    if BACKEND_HOSTPORT:
        return f"http://{BACKEND_HOSTPORT}"
    return "http://localhost:8080"


BACKEND_BASE_URL = resolve_backend_base_url()


def main():
    client = redis.from_url(REDIS_URL, decode_responses=True) if REDIS_URL else redis.Redis(
        host=REDIS_HOST,
        port=REDIS_PORT,
        decode_responses=True,
    )
    redis_target = REDIS_URL or f"redis://{REDIS_HOST}:{REDIS_PORT}"
    logger.info(
        "listening on %s queue=%s concurrency=%s",
        redis_target, QUEUE_KEY, WORKER_CONCURRENCY,
    )

    with ThreadPoolExecutor(max_workers=WORKER_CONCURRENCY) as executor:
        inflight = {}  # future -> jobId

        while True:
            # 끝난 작업을 회수하고 결과를 확인한다. process_job이 스스로 FAILED를 백엔드에 보고하지만,
            # 그 보고 자체가 실패한 경우(예: 백엔드 접속 불가)는 여기서 .result()로 잡는다.
            # 이전에는 그 예외가 아무 흔적 없이 사라졌다.
            for future in [f for f in inflight if f.done()]:
                job_id = inflight.pop(future)
                try:
                    future.result()
                except Exception:
                    logger.exception("job crashed without being reported job=%s", job_id)

            if len(inflight) >= WORKER_CONCURRENCY:
                time.sleep(0.1)
                continue

            _, payload = client.blpop(QUEUE_KEY, timeout=POLL_TIMEOUT_SECONDS) or (None, None)
            if payload is None:
                continue

            try:
                job = json.loads(payload)
                job_id = job["jobId"]
            except (ValueError, KeyError) as error:
                # 예전엔 json.loads가 try 밖에 있어서 잘못된 메시지 하나가 루프 전체를 죽였다.
                logger.error("dropping malformed queue message: %s", error)
                continue

            logger.info("received job=%s", job_id)
            inflight[executor.submit(process_job, job)] = job_id


def process_job(job):
    job_id = job.get("jobId")
    started_at = time.perf_counter()
    try:
        update_job(job_id, {"status": "PROCESSING"})
        result = optimize_image(job)
        if should_upload_result_file():
            upload_result_file(job_id, result)
        else:
            update_job(
                job_id,
                {
                    "status": "SUCCEEDED",
                    "resultImageUrl": result["resultImageUrl"],
                    "outputObjectKey": result["outputObjectKey"],
                    "sourceFileSizeBytes": result["sourceFileSizeBytes"],
                    "resultFileSizeBytes": result["resultFileSizeBytes"],
                },
            )
        logger.info(
            "job succeeded job=%s elapsedMs=%d",
            job_id, int((time.perf_counter() - started_at) * 1000),
        )
    except Exception as error:
        # 아래 보고가 실패해도 실패 자체는 남도록, 스택 트레이스를 먼저 기록한다.
        logger.exception("job failed job=%s: %s", job_id, error)
        update_job(job_id, {"status": "FAILED", "failureReason": str(error)})


def optimize_image(job):
    image = load_source_image(job)
    image = apply_crop_mode(image, job)
    image = resize_image(image, job)

    output_format = normalize_format(job.get("outputFormat"))
    quality = int(job.get("quality") or 80)
    object_key = job.get("outputObjectKey") or f"optimized/{job['jobId']}.{output_format.lower()}"
    output_file_path = job.get("outputFilePath")
    result_image_url = job.get("resultImageUrl")

    output_buffer = BytesIO()
    image = apply_watermark(
        image,
        job.get("watermarkText"),
        job.get("watermarkFontFamily"),
        job.get("watermarkAccentText"),
        job.get("watermarkImageUrl"),
        job.get("watermarkImageFilePath"),
        job.get("watermarkStyle"),
        job.get("watermarkPosition"),
        job.get("watermarkOpacity"),
        job.get("watermarkScalePercent"),
    )
    save_image(image, output_buffer, output_format, quality)
    output_buffer.seek(0)

    output_bytes = output_buffer.getvalue()
    if not should_upload_result_file():
        upload_to_r2_or_local(object_key, output_bytes, output_format, output_file_path)

    return {
        "outputObjectKey": object_key,
        "resultImageUrl": result_image_url or f"{R2_PUBLIC_BASE_URL.rstrip('/')}/{object_key}",
        "sourceFileSizeBytes": source_size_bytes(job),
        "resultFileSizeBytes": len(output_bytes),
        "outputBytes": output_bytes,
    }


def resize_image(image, job):
    target_width = job.get("targetWidth")
    target_height = job.get("targetHeight")

    if not target_width and not target_height:
        return image

    if target_width and target_height:
        width = int(target_width)
        height = int(target_height)
    elif target_width:
        width = int(target_width)
        ratio = width / image.width
        height = max(1, int(round(image.height * ratio)))
    else:
        height = int(target_height)
        ratio = height / image.height
        width = max(1, int(round(image.width * ratio)))

    return image.resize((width, height), Image.Resampling.LANCZOS)


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


def apply_watermark(
    image,
    watermark_text,
    watermark_font_family,
    accent_text,
    watermark_image_url,
    watermark_image_file_path,
    watermark_style,
    watermark_position,
    watermark_opacity,
    watermark_scale_percent,
):
    if not watermark_text and not watermark_image_url and not watermark_image_file_path:
        return image

    if image.mode != "RGBA":
        image = image.convert("RGBA")

    if watermark_image_url or watermark_image_file_path:
        return apply_image_watermark(
            image,
            watermark_image_url,
            watermark_image_file_path,
            watermark_position,
            watermark_opacity,
            watermark_scale_percent,
        )

    overlay = Image.new("RGBA", image.size, (255, 255, 255, 0))
    draw = ImageDraw.Draw(overlay)
    style = (watermark_style or "signature").strip().lower()
    position = (watermark_position or "bottom-right").strip().lower()
    opacity = max(20, min(90, int(watermark_opacity or 56)))
    scale_percent = max(10, min(40, int(watermark_scale_percent or 18)))
    accent = (accent_text or "").strip()
    text = watermark_text.strip()
    title_font_size, accent_font_size = resolve_font_sizes(image.height, accent)
    title_font = load_font(watermark_font_family, title_font_size, bold=True)
    accent_font = load_font(watermark_font_family, accent_font_size)
    padding = max(20, image.width // 34)
    box_width = max(180, min(image.width - (padding * 2), int(image.width * scale_percent / 100)))
    box_height = max(54, int(image.height / 8) if accent else int(image.height / 10))
    x, y = resolve_watermark_position(image.width, image.height, box_width, box_height, position)
    fill = resolve_background_fill(style, opacity)
    radius = 28 if style == "monogram" else 22
    draw_text_background(draw, style, fill, x, y, box_width, box_height, radius)

    title_y = y + 16
    draw.text((x + 16, title_y), text, fill=(255, 255, 255, 235), font=title_font)
    if accent:
        draw.text((x + 16, title_y + 18), accent, fill=resolve_accent_fill(style), font=accent_font)

    return Image.alpha_composite(image, overlay)


def apply_image_watermark(image, watermark_image_url, watermark_image_file_path, watermark_position, watermark_opacity, watermark_scale_percent):
    watermark = load_watermark_image(watermark_image_file_path, watermark_image_url)
    if watermark.mode != "RGBA":
        watermark = watermark.convert("RGBA")

    opacity = max(20, min(90, int(watermark_opacity or 56)))
    scale_percent = max(10, min(40, int(watermark_scale_percent or 18)))
    max_width = max(60, int(image.width * scale_percent / 100))
    ratio = max_width / watermark.width
    height = max(1, int(round(watermark.height * ratio)))
    watermark = watermark.resize((max_width, height), Image.Resampling.LANCZOS)

    alpha = watermark.getchannel("A")
    alpha = alpha.point(lambda value: int(value * opacity / 100))
    watermark.putalpha(alpha)

    x, y = resolve_watermark_position(image.width, image.height, watermark.width, watermark.height, (watermark_position or "bottom-right").strip().lower())
    overlay = Image.new("RGBA", image.size, (255, 255, 255, 0))
    overlay.alpha_composite(watermark, dest=(x, y))
    return Image.alpha_composite(image, overlay)


def draw_text_background(draw, style, fill, x, y, box_width, box_height, radius):
    if style == "monogram":
        draw.ellipse((x, y, x + box_width, y + box_height), fill=fill)
        return

    draw.rounded_rectangle((x, y, x + box_width, y + box_height), radius=radius, fill=fill)

    if style == "ribbon":
        tail_width = max(40, box_width // 4)
        draw.rectangle((x + 18, y + box_height - 18, x + 18 + tail_width, y + box_height - 4), fill=fill)

    if style == "outline":
        draw.rounded_rectangle((x, y, x + box_width, y + box_height), radius=radius, outline=(191, 219, 254, 170), width=2)


def resolve_font_sizes(image_height, accent):
    if accent:
        title_size = max(18, image_height // 16)
    else:
        title_size = max(18, image_height // 14)
    accent_size = max(12, title_size - 6)
    return title_size, accent_size


def load_font(font_family, font_size, bold=False):
    for candidate in font_candidates(font_family, bold):
        if Path(candidate).exists():
            try:
                return ImageFont.truetype(candidate, font_size)
            except OSError:
                continue
    return ImageFont.load_default()


def font_candidates(font_family, bold):
    family = (font_family or "sans").strip().lower()
    windows_root = Path("C:/Windows/Fonts")
    linux_candidates = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf",
        "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf",
        "/usr/share/fonts/truetype/liberation2/LiberationSerif-Regular.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
    ]
    families = {
        "sans": [windows_root / ("arialbd.ttf" if bold else "arial.ttf"), windows_root / ("segoeuib.ttf" if bold else "segoeui.ttf"), linux_candidates[0]],
        "serif": [windows_root / ("georgiab.ttf" if bold else "georgia.ttf"), windows_root / ("timesbd.ttf" if bold else "times.ttf"), linux_candidates[1]],
        "mono": [windows_root / ("consolab.ttf" if bold else "consola.ttf"), linux_candidates[4]],
        "script": [windows_root / "comic.ttf", windows_root / "comicbd.ttf", linux_candidates[2]],
        "display": [windows_root / ("trebucbd.ttf" if bold else "trebuc.ttf"), linux_candidates[0]],
    }
    return [str(path) for path in families.get(family, families["sans"])]


def load_watermark_image(watermark_image_file_path, watermark_image_url):
    if watermark_image_file_path:
        path = Path(watermark_image_file_path)
        if path.exists():
            return Image.open(path)

    if watermark_image_url:
        response = requests.get(resolve_backend_url(watermark_image_url), timeout=30)
        response.raise_for_status()
        return Image.open(BytesIO(response.content))

    raise ValueError("watermark image path or url is required")


def resolve_watermark_position(image_width, image_height, box_width, box_height, position):
    padding = max(20, image_width // 34)
    if position == "bottom-left":
        return padding, image_height - box_height - padding
    if position == "top-right":
        return image_width - box_width - padding, padding
    if position == "center":
        return (image_width - box_width) // 2, (image_height - box_height) // 2
    return image_width - box_width - padding, image_height - box_height - padding


def resolve_background_fill(style, opacity):
    alpha = max(40, min(230, int(255 * opacity / 100)))
    fills = {
        "outline": (15, 23, 42, int(alpha * 0.4)),
        "badge": (17, 24, 39, alpha),
        "plaque": (17, 24, 39, alpha),
        "monogram": (17, 24, 39, int(alpha * 0.7)),
        "ribbon": (31, 41, 55, int(alpha * 0.8)),
        "pop": (190, 24, 93, alpha),
        "sticker": (124, 45, 18, int(alpha * 0.86)),
        "label": (29, 78, 216, int(alpha * 0.86)),
    }
    return fills.get(style, (15, 23, 42, int(alpha * 0.76)))


def resolve_accent_fill(style):
    fills = {
        "plaque": (245, 213, 139, 235),
        "monogram": (245, 213, 139, 235),
        "ribbon": (245, 213, 139, 235),
        "pop": (253, 230, 138, 235),
        "label": (191, 219, 254, 235),
    }
    return fills.get(style, (219, 234, 254, 235))


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
            img = Image.open(source_path)
            return ImageOps.exif_transpose(img)

    source_url = job.get("sourceImageUrl")
    if not source_url:
        raise ValueError("source image path or url is required")

    response = requests.get(resolve_backend_url(source_url), timeout=30)
    response.raise_for_status()
    img = Image.open(BytesIO(response.content))
    return ImageOps.exif_transpose(img)


def resolve_backend_url(url):
    if not url:
        return url

    parsed = urlparse(url)
    if parsed.hostname not in {"localhost", "127.0.0.1"}:
        return url

    backend_base = BACKEND_BASE_URL.rstrip("/")
    if not backend_base:
        return url

    backend_parsed = urlparse(backend_base)
    if not backend_parsed.scheme or not backend_parsed.netloc:
        return url

    return urlunparse((
        backend_parsed.scheme,
        backend_parsed.netloc,
        parsed.path,
        parsed.params,
        parsed.query,
        parsed.fragment,
    ))


def upload_to_r2_or_local(object_key, data, output_format, output_file_path):
    access_key = os.getenv("R2_ACCESS_KEY_ID")
    secret_key = os.getenv("R2_SECRET_ACCESS_KEY")
    bucket_name = os.getenv("R2_BUCKET_NAME")
    endpoint = os.getenv("R2_ENDPOINT")

    if not all([access_key, secret_key, bucket_name, endpoint]):
        output_path = resolve_local_output_path(object_key, output_file_path)
        if output_path:
            output_path.parent.mkdir(parents=True, exist_ok=True)
            output_path.write_bytes(data)
            logger.info("saved optimized file locally: %s", output_path)
            return
        logger.warning("R2 credentials not set, simulating upload only")
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


def upload_result_file(job_id, result):
    files = {
        "file": (
            Path(result["outputObjectKey"]).name,
            result["outputBytes"],
            guess_content_type(result["outputObjectKey"]),
        )
    }
    data = {
        "outputObjectKey": result["outputObjectKey"],
        "resultImageUrl": result["resultImageUrl"],
        "resultFileSizeBytes": result["resultFileSizeBytes"],
    }
    if result["sourceFileSizeBytes"] is not None:
        data["sourceFileSizeBytes"] = result["sourceFileSizeBytes"]
    response = requests.patch(
        f"{BACKEND_BASE_URL.rstrip('/')}/api/image-jobs/{job_id}/result-file",
        files=files,
        data=data,
        timeout=30,
    )
    response.raise_for_status()


def should_upload_result_file():
    access_key = os.getenv("R2_ACCESS_KEY_ID")
    secret_key = os.getenv("R2_SECRET_ACCESS_KEY")
    bucket_name = os.getenv("R2_BUCKET_NAME")
    endpoint = os.getenv("R2_ENDPOINT")
    has_object_storage = all([access_key, secret_key, bucket_name, endpoint])
    return WORKER_RESULT_CALLBACK_ENABLED and not has_object_storage


def guess_content_type(object_key):
    suffix = Path(object_key).suffix.lower()
    if suffix == ".webp":
        return "image/webp"
    if suffix in {".jpg", ".jpeg"}:
        return "image/jpeg"
    if suffix == ".png":
        return "image/png"
    return "application/octet-stream"


def source_size_bytes(job):
    source_file_path = job.get("sourceFilePath")
    if source_file_path:
        source_path = Path(source_file_path)
        if source_path.exists():
            return source_path.stat().st_size
    return None


def resolve_local_output_path(object_key, output_file_path):
    normalized_key = (object_key or "").replace("\\", "/")
    filename = Path(normalized_key).name if normalized_key else ""

    if STORAGE_ROOT and filename:
        return Path(STORAGE_ROOT).resolve().joinpath("output", filename)

    if output_file_path:
        return Path(output_file_path)

    return None


if __name__ == "__main__":
    while True:
        try:
            main()
        except Exception as error:
            logger.exception("fatal loop error, restarting in 3s: %s", error)
            time.sleep(3)
