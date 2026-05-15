import argparse
import base64
import concurrent.futures
import json
import statistics
import threading
import time

import requests


PNG_BYTES = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9p7gP7cAAAAASUVORK5CYII="
)


def percentile(values, p):
    if not values:
        return 0.0
    ordered = sorted(values)
    index = max(0, min(len(ordered) - 1, int(round((len(ordered) - 1) * p))))
    return ordered[index]


def login(base_url, email, password, timeout):
    response = requests.post(
        f"{base_url.rstrip('/')}/api/auth/login",
        json={"email": email, "password": password},
        timeout=timeout,
    )
    response.raise_for_status()
    return response.json()["token"]


def upload_once(base_url, token, timeout, width, quality, wait_completion, completion_timeout):
    started_at = time.perf_counter()
    response = requests.post(
        f"{base_url.rstrip('/')}/api/image-jobs/upload",
        headers={"Authorization": f"Bearer {token}"},
        files={"file": ("sample.png", PNG_BYTES, "image/png")},
        data={"width": str(width), "quality": str(quality)},
        timeout=timeout,
    )
    upload_duration_ms = (time.perf_counter() - started_at) * 1000

    result = {
        "upload_status_code": response.status_code,
        "upload_duration_ms": upload_duration_ms,
        "job_status": None,
        "completion_duration_ms": None,
        "error": None,
    }

    if not response.ok:
        try:
            result["error"] = response.json()
        except ValueError:
            result["error"] = response.text
        return result

    payload = response.json()
    job_id = payload.get("id")

    if not wait_completion or not job_id:
        return result

    completion_started_at = time.perf_counter()
    deadline = time.time() + completion_timeout
    while time.time() < deadline:
        poll = requests.get(
            f"{base_url.rstrip('/')}/api/image-jobs/{job_id}",
            headers={"Authorization": f"Bearer {token}"},
            timeout=timeout,
        )
        poll.raise_for_status()
        job_payload = poll.json()
        job_status = job_payload.get("status")
        if job_status in {"SUCCEEDED", "FAILED"}:
            result["job_status"] = job_status
            result["completion_duration_ms"] = (time.perf_counter() - completion_started_at) * 1000
            return result
        time.sleep(1.0)

    result["job_status"] = "TIMEOUT"
    result["completion_duration_ms"] = (time.perf_counter() - completion_started_at) * 1000
    return result


def main():
    parser = argparse.ArgumentParser(description="Concurrent upload/load test for ImageFlow.")
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--email", default="imageflowmaster@master")
    parser.add_argument("--password", default="imageflow123!")
    parser.add_argument("--requests", type=int, default=12)
    parser.add_argument("--concurrency", type=int, default=3)
    parser.add_argument("--timeout", type=int, default=30)
    parser.add_argument("--width", type=int, default=1200)
    parser.add_argument("--quality", type=int, default=82)
    parser.add_argument("--wait-completion", action="store_true")
    parser.add_argument("--completion-timeout", type=int, default=120)
    args = parser.parse_args()

    token = login(args.base_url, args.email, args.password, args.timeout)

    started_at = time.perf_counter()
    results = []
    lock = threading.Lock()

    with concurrent.futures.ThreadPoolExecutor(max_workers=args.concurrency) as executor:
        futures = [
            executor.submit(
                upload_once,
                args.base_url,
                token,
                args.timeout,
                args.width,
                args.quality,
                args.wait_completion,
                args.completion_timeout,
            )
            for _ in range(args.requests)
        ]
        for future in concurrent.futures.as_completed(futures):
            with lock:
                results.append(future.result())

    total_duration_ms = (time.perf_counter() - started_at) * 1000
    upload_latencies = [item["upload_duration_ms"] for item in results]
    upload_success_count = sum(1 for item in results if 200 <= item["upload_status_code"] < 300)
    error_count = len(results) - upload_success_count
    completion_latencies = [item["completion_duration_ms"] for item in results if item["completion_duration_ms"] is not None]

    summary = {
        "baseUrl": args.base_url,
        "requests": args.requests,
        "concurrency": args.concurrency,
        "waitCompletion": args.wait_completion,
        "totalDurationMs": round(total_duration_ms, 2),
        "upload": {
            "successCount": upload_success_count,
            "errorCount": error_count,
            "avgMs": round(statistics.mean(upload_latencies), 2) if upload_latencies else 0,
            "p95Ms": round(percentile(upload_latencies, 0.95), 2),
            "maxMs": round(max(upload_latencies), 2) if upload_latencies else 0,
        },
        "completion": {
            "avgMs": round(statistics.mean(completion_latencies), 2) if completion_latencies else None,
            "p95Ms": round(percentile(completion_latencies, 0.95), 2) if completion_latencies else None,
            "maxMs": round(max(completion_latencies), 2) if completion_latencies else None,
            "statusCounts": {},
        },
        "statusCodes": {},
    }

    for item in results:
        code = str(item["upload_status_code"])
        summary["statusCodes"][code] = summary["statusCodes"].get(code, 0) + 1
        if item["job_status"] is not None:
            summary["completion"]["statusCounts"][item["job_status"]] = summary["completion"]["statusCounts"].get(item["job_status"], 0) + 1

    print(json.dumps(summary, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
