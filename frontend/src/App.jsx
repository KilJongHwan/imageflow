import { useEffect, useRef, useState } from "react";

const POLL_INTERVAL_MS = 2500;

export default function App() {
  const [baseUrl, setBaseUrl] = useState("http://localhost:8080");
  const [file, setFile] = useState(null);
  const [targetWidth, setTargetWidth] = useState("1200");
  const [quality, setQuality] = useState("80");
  const [outputFormat, setOutputFormat] = useState("webp");
  const [statusMessage, setStatusMessage] = useState("이미지를 올리면 최적화 작업을 시작합니다.");
  const [job, setJob] = useState(null);
  const [error, setError] = useState("");
  const pollTimerRef = useRef(null);

  useEffect(() => () => {
    if (pollTimerRef.current) {
      clearTimeout(pollTimerRef.current);
    }
  }, []);

  async function request(path, options = {}) {
    const response = await fetch(`${baseUrl.replace(/\/$/, "")}${path}`, options);
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
      setError("먼저 이미지를 선택해주세요.");
      return;
    }

    setError("");
    setJob(null);
    setStatusMessage("업로드 중...");

    const formData = new FormData();
    formData.append("file", file);
    if (targetWidth) formData.append("targetWidth", targetWidth);
    if (quality) formData.append("quality", quality);
    if (outputFormat) formData.append("outputFormat", outputFormat);
    try {
      const createdJob = await request("/api/image-jobs/upload", {
        method: "POST",
        body: formData
      });

      setJob(createdJob);
      setStatusMessage("작업이 큐에 들어갔습니다. 결과를 확인하는 중...");
      pollJob(createdJob.id);
    } catch (submitError) {
      setError(submitError.message);
      setStatusMessage("작업 생성에 실패했습니다.");
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
        setStatusMessage("최적화가 완료되었습니다.");
        return;
      }

      if (latestJob.status === "FAILED") {
        setStatusMessage("최적화에 실패했습니다.");
        return;
      }

      setStatusMessage(`현재 상태: ${latestJob.status}`);
      pollTimerRef.current = setTimeout(() => pollJob(jobId), POLL_INTERVAL_MS);
    } catch (pollError) {
      setError(pollError.message);
      setStatusMessage("상태 확인에 실패했습니다.");
    }
  }

  return (
    <div className="landing-shell">
      <section className="hero-card">
        <div className="hero-copy">
          <div className="eyebrow">ImageFlow</div>
          <h1>이미지 한 장 올리고 바로 최적화하세요.</h1>
          <p>
            로그인 없이 바로 테스트할 수 있는 첫 화면입니다.
            이미지를 업로드하면 최적화 작업을 만들고, 완료되면 결과 이미지를 보여줍니다.
          </p>
        </div>

        <form className="upload-card" onSubmit={handleSubmit}>
          <label className="field">
            <span>백엔드 주소</span>
            <input value={baseUrl} onChange={(event) => setBaseUrl(event.target.value)} />
          </label>

          <label className="file-drop">
            <input
              type="file"
              accept="image/*"
              onChange={(event) => setFile(event.target.files?.[0] || null)}
            />
            <strong>{file ? file.name : "이미지를 선택하세요"}</strong>
            <span>jpg, png, webp 모두 가능합니다.</span>
          </label>

          <div className="option-grid">
            <label className="field">
              <span>가로 폭</span>
              <input value={targetWidth} onChange={(event) => setTargetWidth(event.target.value)} />
            </label>
            <label className="field">
              <span>품질</span>
              <input value={quality} onChange={(event) => setQuality(event.target.value)} />
            </label>
            <label className="field">
              <span>포맷</span>
              <select value={outputFormat} onChange={(event) => setOutputFormat(event.target.value)}>
                <option value="webp">webp</option>
                <option value="jpeg">jpeg</option>
                <option value="png">png</option>
              </select>
            </label>
          </div>

          <button className="primary-button" type="submit">최적화 시작</button>
          <p className="status-text">{statusMessage}</p>
          {error ? <p className="error-text">{error}</p> : null}
        </form>
      </section>

      <section className="result-card">
        <div className="result-header">
          <h2>결과</h2>
          <p>작업 상태와 최종 이미지를 여기서 확인합니다.</p>
        </div>

        {job ? (
          <div className="result-layout">
            <div className="job-meta">
              <div className="meta-row"><span>Job ID</span><code>{job.id}</code></div>
              <div className="meta-row"><span>Status</span><strong>{job.status}</strong></div>
              <div className="meta-row"><span>Width</span><strong>{job.targetWidth || "-"}</strong></div>
              <div className="meta-row"><span>Quality</span><strong>{job.quality}</strong></div>
              <div className="meta-row"><span>Format</span><strong>{job.outputFormat}</strong></div>
              {job.failureReason ? <div className="meta-row"><span>Error</span><strong>{job.failureReason}</strong></div> : null}
            </div>

            <div className="preview-pane">
              {job.resultImageUrl ? (
                <>
                  <img src={job.resultImageUrl} alt="optimized result" className="preview-image" />
                  <a href={job.resultImageUrl} target="_blank" rel="noreferrer" className="secondary-link">
                    새 탭에서 결과 보기
                  </a>
                </>
              ) : (
                <div className="preview-placeholder">최적화가 완료되면 결과 이미지가 여기에 표시됩니다.</div>
              )}
            </div>
          </div>
        ) : (
          <div className="preview-placeholder large">아직 생성된 작업이 없습니다.</div>
        )}
      </section>
    </div>
  );
}
