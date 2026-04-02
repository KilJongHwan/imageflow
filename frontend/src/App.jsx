import { useEffect, useRef, useState } from "react";

import { HeroSection } from "./components/HeroSection";
import { OptimizationForm } from "./components/OptimizationForm";
import { ResultPanel } from "./components/ResultPanel";

const POLL_INTERVAL_MS = 2500;

const initialOptions = {
  width: "1600",
  height: "",
  quality: "82",
  aspectRatio: "original",
  watermarkText: "",
  cropMode: "fit"
};

export default function App() {
  const [baseUrl, setBaseUrl] = useState(import.meta.env.VITE_API_BASE_URL || "http://localhost:8080");
  const [file, setFile] = useState(null);
  const [options, setOptions] = useState(initialOptions);
  const [statusMessage, setStatusMessage] = useState("작업 준비가 끝나면 대기열 없이 바로 진행됩니다.");
  const [job, setJob] = useState(null);
  const [error, setError] = useState("");
  const pollTimerRef = useRef(null);

  useEffect(() => () => {
    if (pollTimerRef.current) {
      clearTimeout(pollTimerRef.current);
    }
  }, []);

  async function request(path, requestOptions = {}) {
    const response = await fetch(`${baseUrl.replace(/\/$/, "")}${path}`, requestOptions);
    const contentType = response.headers.get("content-type") || "";
    const payload = contentType.includes("application/json")
      ? await response.json()
      : await response.text();

    if (!response.ok) {
      throw new Error(payload?.message || payload || `Request failed with ${response.status}`);
    }

    return payload;
  }

  async function handleSubmit(event) {
    event.preventDefault();

    if (!file) {
      setError("먼저 최적화할 이미지를 선택해주세요.");
      return;
    }

    setError("");
    setJob(null);
    setStatusMessage("원본을 업로드하고 처리 작업을 준비하는 중입니다.");

    const formData = new FormData();
    formData.append("file", file);
    if (options.width) formData.append("width", options.width);
    if (options.height) formData.append("height", options.height);
    if (options.quality) formData.append("quality", options.quality);
    if (options.aspectRatio) formData.append("aspectRatio", options.aspectRatio);
    if (options.watermarkText) formData.append("watermarkText", options.watermarkText);
    if (options.cropMode) formData.append("cropMode", options.cropMode);

    try {
      const createdJob = await request("/api/image-jobs/upload", {
        method: "POST",
        body: formData
      });
      setJob(createdJob);
      setStatusMessage("큐에 등록되었습니다. 최적화 결과를 확인하는 중입니다.");
      pollJob(createdJob.id);
    } catch (submitError) {
      setError(submitError.message);
      setStatusMessage("요청을 완료하지 못했습니다.");
    }
  }

  async function pollJob(jobId) {
    if (pollTimerRef.current) {
      clearTimeout(pollTimerRef.current);
    }

    try {
      const latestJob = await request(`/api/image-jobs/${jobId}`);
      setJob(latestJob);

      if (latestJob.status === "SUCCEEDED") {
        setStatusMessage("최적화가 완료되었습니다. 결과를 확인해보세요.");
        return;
      }

      if (latestJob.status === "FAILED") {
        setStatusMessage("처리 중 문제가 발생했습니다.");
        return;
      }

      setStatusMessage(`현재 상태: ${latestJob.status}`);
      pollTimerRef.current = setTimeout(() => pollJob(jobId), POLL_INTERVAL_MS);
    } catch (pollError) {
      setError(pollError.message);
      setStatusMessage("상태 확인이 중단되었습니다.");
    }
  }

  return (
    <div className="app-shell">
      <HeroSection />

      <main className="main-grid">
        <OptimizationForm
          baseUrl={baseUrl}
          file={file}
          options={options}
          error={error}
          statusMessage={statusMessage}
          onBaseUrlChange={setBaseUrl}
          onFileChange={setFile}
          onOptionsChange={setOptions}
          onSubmit={handleSubmit}
        />

        <ResultPanel job={job} />
      </main>
    </div>
  );
}
