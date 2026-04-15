import { useEffect, useRef, useState } from "react";
import { ConfigProvider, theme } from "antd";

import { AuthPanel } from "./components/AuthPanel";
import { HeroSection } from "./components/HeroSection";
import { OptimizationForm } from "./components/OptimizationForm";
import { ResultPanel } from "./components/ResultPanel";

const POLL_INTERVAL_MS = 2500;
const TOKEN_STORAGE_KEY = "imageflow.jwt";
const API_BASE_URL_STORAGE_KEY = "imageflow.api-base-url";

const initialOptions = {
  presetId: "kurly",
  width: "1600",
  height: "",
  quality: "82",
  aspectRatio: "original",
  watermarkText: "",
  watermarkBrandText: "",
  watermarkAccentText: "",
  watermarkTone: "clean",
  watermarkPresetId: "",
  watermarkPresets: [],
  watermarkStyle: "signature",
  watermarkPosition: "bottom-right",
  watermarkOpacity: "56",
  watermarkScalePercent: "18",
  cropMode: "fit",
  cropX: "",
  cropY: "",
  cropWidth: "",
  cropHeight: ""
};

export default function App() {
  const [baseUrl, setBaseUrl] = useState(() => localStorage.getItem(API_BASE_URL_STORAGE_KEY) || import.meta.env.VITE_API_BASE_URL || "http://localhost:8080");
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_STORAGE_KEY) || "");
  const [user, setUser] = useState(null);
  const [files, setFiles] = useState([]);
  const [options, setOptions] = useState(initialOptions);
  const [statusMessage, setStatusMessage] = useState("로그인 후 이미지를 업로드하면 바로 최적화를 시작할 수 있습니다.");
  const [jobs, setJobs] = useState([]);
  const [recentJobs, setRecentJobs] = useState([]);
  const [selectedJobId, setSelectedJobId] = useState("");
  const [error, setError] = useState("");
  const [authError, setAuthError] = useState("");
  const [authLoading, setAuthLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [watermarkGenerateLoading, setWatermarkGenerateLoading] = useState(false);
  const [watermarkGenerateError, setWatermarkGenerateError] = useState("");
  const [health, setHealth] = useState({
    status: "checking",
    processingMode: "",
    maxBatchSize: null,
    queueEnabled: false,
    lastCheckedAt: "",
    message: ""
  });
  const pollTimerRef = useRef(null);
  const pollingEnabledRef = useRef(false);
  const dragDepthRef = useRef(0);
  const healthCheckTimerRef = useRef(null);
  const [pageDropActive, setPageDropActive] = useState(false);

  useEffect(() => () => {
    stopPolling();
    if (healthCheckTimerRef.current) {
      clearTimeout(healthCheckTimerRef.current);
    }
  }, []);

  useEffect(() => {
    localStorage.setItem(API_BASE_URL_STORAGE_KEY, baseUrl);
    setHealth((current) => ({ ...current, status: "checking" }));

    if (healthCheckTimerRef.current) {
      clearTimeout(healthCheckTimerRef.current);
    }

    healthCheckTimerRef.current = setTimeout(() => {
      checkHealth();
    }, 300);

    return () => {
      if (healthCheckTimerRef.current) {
        clearTimeout(healthCheckTimerRef.current);
      }
    };
  }, [baseUrl]);

  useEffect(() => {
    if (!token) {
      stopPolling();
      setUser(null);
      setRecentJobs([]);
      setSelectedJobId("");
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      return;
    }

    localStorage.setItem(TOKEN_STORAGE_KEY, token);
    restoreSession(token);
  }, [token]);

  useEffect(() => {
    if (!user) {
      setRecentJobs([]);
      return;
    }

    loadRecentJobs();
  }, [user]);

  useEffect(() => {
    function hasFiles(event) {
      return Array.from(event.dataTransfer?.items || []).some((item) => item.kind === "file");
    }

    function handleWindowDragEnter(event) {
      if (!user || !hasFiles(event)) {
        return;
      }
      event.preventDefault();
      dragDepthRef.current += 1;
      setPageDropActive(true);
    }

    function handleWindowDragOver(event) {
      if (!user || !hasFiles(event)) {
        return;
      }
      event.preventDefault();
      event.dataTransfer.dropEffect = "copy";
    }

    function handleWindowDragLeave(event) {
      if (!user || !hasFiles(event)) {
        return;
      }
      event.preventDefault();
      dragDepthRef.current = Math.max(0, dragDepthRef.current - 1);
      if (dragDepthRef.current === 0) {
        setPageDropActive(false);
      }
    }

    function handleWindowDrop(event) {
      if (!user || !hasFiles(event)) {
        return;
      }
      event.preventDefault();
      dragDepthRef.current = 0;
      setPageDropActive(false);
      handleFilesSelected(Array.from(event.dataTransfer.files || []));
    }

    window.addEventListener("dragenter", handleWindowDragEnter);
    window.addEventListener("dragover", handleWindowDragOver);
    window.addEventListener("dragleave", handleWindowDragLeave);
    window.addEventListener("drop", handleWindowDrop);

    return () => {
      window.removeEventListener("dragenter", handleWindowDragEnter);
      window.removeEventListener("dragover", handleWindowDragOver);
      window.removeEventListener("dragleave", handleWindowDragLeave);
      window.removeEventListener("drop", handleWindowDrop);
    };
  }, [user]);

  function isSupportedUploadFile(file) {
    const mimeType = (file.type || "").toLowerCase();
    const filename = file.name.toLowerCase();
    const hasSupportedExtension = [".jpg", ".jpeg", ".png", ".webp", ".zip"].some((extension) => filename.endsWith(extension));
    const hasSupportedMime = mimeType === ""
      || mimeType.startsWith("image/")
      || mimeType === "application/zip"
      || mimeType === "application/x-zip-compressed";

    return hasSupportedExtension && hasSupportedMime;
  }

  function mergeFiles(currentFiles, nextFiles) {
    const merged = [...currentFiles];

    for (const file of nextFiles) {
      const duplicated = merged.some(
        (existing) =>
          existing.name === file.name &&
          existing.size === file.size &&
          existing.lastModified === file.lastModified
      );

      if (!duplicated) {
        merged.push(file);
      }
    }

    return merged;
  }

  function handleFilesSelected(nextFiles) {
    const validFiles = nextFiles.filter(isSupportedUploadFile);
    const invalidFiles = nextFiles.filter((file) => !isSupportedUploadFile(file));

    setFiles((currentFiles) => mergeFiles(currentFiles, validFiles));
    setOptions((current) => ({
      ...current,
      cropX: "",
      cropY: "",
      cropWidth: "",
      cropHeight: ""
    }));

    if (invalidFiles.length > 0) {
      setError("jpg, jpeg, png, webp 이미지와 zip 파일만 업로드할 수 있습니다.");
    } else {
      setError("");
    }
  }

  function handleRemoveFile(fileToRemove) {
    setFiles((currentFiles) =>
      currentFiles.filter(
        (file) =>
          !(file.name === fileToRemove.name
            && file.size === fileToRemove.size
            && file.lastModified === fileToRemove.lastModified)
      )
    );
  }

  function handleClearFiles() {
    setFiles([]);
    setOptions((current) => ({
      ...current,
      cropX: "",
      cropY: "",
      cropWidth: "",
      cropHeight: ""
    }));
  }

  function stopPolling() {
    pollingEnabledRef.current = false;
    if (pollTimerRef.current) {
      clearTimeout(pollTimerRef.current);
      pollTimerRef.current = null;
    }
  }

  async function request(path, requestOptions = {}) {
    const headers = new Headers(requestOptions.headers || {});
    if (token) {
      headers.set("Authorization", `Bearer ${token}`);
    }

    const response = await fetch(`${baseUrl.replace(/\/$/, "")}${path}`, {
      ...requestOptions,
      headers
    });
    const contentType = response.headers.get("content-type") || "";
    const payload = contentType.includes("application/json")
      ? await response.json()
      : await response.text();

    if (!response.ok) {
      throw new Error(payload?.message || payload || `Request failed with ${response.status}`);
    }

    return payload;
  }

  async function checkHealth() {
    try {
      const response = await fetch(`${baseUrl.replace(/\/$/, "")}/api/health`);
      if (!response.ok) {
        throw new Error(`health check failed with ${response.status}`);
      }

      const payload = await response.json();
      setHealth({
        status: "online",
        processingMode: payload.processingMode || "",
        maxBatchSize: payload.maxBatchSize ?? null,
        queueEnabled: Boolean(payload.queueEnabled),
        lastCheckedAt: payload.timestamp || new Date().toISOString(),
        message: ""
      });
    } catch (_error) {
      setHealth({
        status: "offline",
        processingMode: "",
        maxBatchSize: null,
        queueEnabled: false,
        lastCheckedAt: new Date().toISOString(),
        message: "API health endpoint에 연결할 수 없습니다."
      });
    }
  }

  async function restoreSession(accessToken) {
    try {
      const response = await fetch(`${baseUrl.replace(/\/$/, "")}/api/auth/me`, {
        headers: {
          Authorization: `Bearer ${accessToken}`
        }
      });

      if (!response.ok) {
        throw new Error("session expired");
      }

      const me = await response.json();
      pollingEnabledRef.current = true;
      setUser(me);
      setAuthError("");
    } catch (_error) {
      stopPolling();
      setToken("");
      setUser(null);
      setAuthError("세션이 만료되어 다시 로그인해주세요.");
    }
  }

  async function loadRecentJobs() {
    setHistoryLoading(true);
    try {
      const response = await request("/api/image-jobs");
      setRecentJobs(response);
      setSelectedJobId((currentSelectedId) => {
        if (currentSelectedId && response.some((job) => job.id === currentSelectedId)) {
          return currentSelectedId;
        }
        return response[0]?.id || "";
      });
    } catch (_error) {
      // Keep the workspace usable even if history refresh fails.
    } finally {
      setHistoryLoading(false);
    }
  }

  async function handleGenerateWatermarkPresets(payload) {
    setWatermarkGenerateError("");
    setWatermarkGenerateLoading(true);

    try {
      const presets = await request("/api/watermarks/generate", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(payload)
      });

      setOptions((current) => {
        const firstPreset = presets[0];
        if (!firstPreset) {
          return {
            ...current,
            watermarkPresets: []
          };
        }

        return {
          ...current,
          watermarkBrandText: payload.brandText,
          watermarkAccentText: payload.accentText || "",
          watermarkTone: payload.tone,
          watermarkPresets: presets,
          watermarkPresetId: firstPreset.id,
          watermarkText: firstPreset.brandText,
          watermarkStyle: firstPreset.style,
          watermarkPosition: firstPreset.position,
          watermarkOpacity: String(firstPreset.recommendedOpacity),
          watermarkScalePercent: String(firstPreset.recommendedScalePercent)
        };
      });
    } catch (generateError) {
      setWatermarkGenerateError(generateError.message);
    } finally {
      setWatermarkGenerateLoading(false);
    }
  }

  function mergeJobsIntoHistory(nextJobs) {
    setRecentJobs((currentJobs) => {
      const byId = new Map();
      [...nextJobs, ...currentJobs].forEach((job) => {
        byId.set(job.id, job);
      });
      return Array.from(byId.values()).sort((left, right) => new Date(right.createdAt) - new Date(left.createdAt));
    });
  }

  async function handleAuthSubmit(mode, credentials) {
    setAuthError("");
    setError("");
    setAuthLoading(true);

    try {
      const payload = await request(`/api/auth/${mode}`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(credentials)
      });

      setToken(payload.token);
      setUser(payload.user);
      setStatusMessage("로그인이 완료되었습니다. 이미지를 올리면 바로 처리됩니다.");
    } catch (submitError) {
      setAuthError(submitError.message);
    } finally {
      setAuthLoading(false);
    }
  }

  function handleLogout() {
    stopPolling();
    setToken("");
    setUser(null);
    setJobs([]);
    setRecentJobs([]);
    setFiles([]);
    setSelectedJobId("");
    setPageDropActive(false);
    setError("");
    setAuthError("");
    setStatusMessage("로그인 후 이미지를 업로드하면 바로 최적화를 시작할 수 있습니다.");
  }

  async function handleSubmit(event) {
    if (event?.preventDefault) {
      event.preventDefault();
    }

    if (!files.length) {
      setError("먼저 최적화할 이미지를 선택해주세요.");
      return;
    }
    if (options.cropMode === "manual" && files.length > 1) {
      setError("수동 크롭은 한 번에 한 장 업로드할 때만 사용할 수 있습니다.");
      return;
    }

    setError("");
    setJobs([]);
    stopPolling();
    setSubmitLoading(true);
    setStatusMessage("원본을 업로드하고 처리 작업을 준비하는 중입니다.");

    const formData = new FormData();
    if (files.length === 1) {
      formData.append("file", files[0]);
    } else {
      files.forEach((file) => formData.append("files", file));
    }
    if (options.width) formData.append("width", options.width);
    if (options.height) formData.append("height", options.height);
    if (options.quality) formData.append("quality", options.quality);
    if (options.aspectRatio) formData.append("aspectRatio", options.aspectRatio);
    if (options.watermarkText) formData.append("watermarkText", options.watermarkText);
    if (options.watermarkAccentText) formData.append("watermarkAccentText", options.watermarkAccentText);
    if (options.watermarkStyle) formData.append("watermarkStyle", options.watermarkStyle);
    if (options.watermarkPosition) formData.append("watermarkPosition", options.watermarkPosition);
    if (options.watermarkOpacity) formData.append("watermarkOpacity", options.watermarkOpacity);
    if (options.watermarkScalePercent) formData.append("watermarkScalePercent", options.watermarkScalePercent);
    if (options.cropMode) formData.append("cropMode", options.cropMode);
    if (options.cropX) formData.append("cropX", options.cropX);
    if (options.cropY) formData.append("cropY", options.cropY);
    if (options.cropWidth) formData.append("cropWidth", options.cropWidth);
    if (options.cropHeight) formData.append("cropHeight", options.cropHeight);

    try {
      const endpoint = files.length === 1 ? "/api/image-jobs/upload" : "/api/image-jobs/uploads";
      const created = await request(endpoint, {
        method: "POST",
        body: formData
      });
      const nextJobs = Array.isArray(created.jobs) ? created.jobs : [created];
      setJobs(nextJobs);
      setSelectedJobId(nextJobs[0]?.id || "");
      mergeJobsIntoHistory(nextJobs);
      setStatusMessage(
        nextJobs.length > 1
          ? `${nextJobs.length}개 작업이 등록되었습니다. 결과를 확인하고 ZIP으로 다운로드할 수 있습니다.`
          : "큐에 등록되었습니다. 최적화 결과를 확인하는 중입니다."
      );
      if (nextJobs.length === 1) {
        pollingEnabledRef.current = true;
        pollJob(nextJobs[0].id);
      } else {
        loadRecentJobs();
      }
    } catch (submitError) {
      setError(submitError.message);
      setStatusMessage("요청을 완료하지 못했습니다.");
    } finally {
      setSubmitLoading(false);
    }
  }

  async function pollJob(jobId) {
    if (!pollingEnabledRef.current) {
      return;
    }
    stopPolling();
    pollingEnabledRef.current = true;

    try {
      const latestJob = await request(`/api/image-jobs/${jobId}`);
      if (!pollingEnabledRef.current) {
        return;
      }
      setJobs([latestJob]);
      mergeJobsIntoHistory([latestJob]);
      setSelectedJobId(latestJob.id);

      if (latestJob.status === "SUCCEEDED") {
        stopPolling();
        setStatusMessage("최적화가 완료되었습니다. 결과를 확인해보세요.");
        return;
      }

      if (latestJob.status === "FAILED") {
        stopPolling();
        setStatusMessage("처리 중 문제가 발생했습니다.");
        return;
      }

      setStatusMessage(`현재 상태: ${latestJob.status}`);
      pollTimerRef.current = setTimeout(() => pollJob(jobId), POLL_INTERVAL_MS);
    } catch (pollError) {
      stopPolling();
      setError(pollError.message);
      setStatusMessage("상태 확인이 중단되었습니다.");
    }
  }

  async function handleDownloadBatch(jobIds) {
    if (!jobIds.length) {
      return;
    }

    try {
      const query = jobIds.map((jobId) => `jobIds=${encodeURIComponent(jobId)}`).join("&");
      const response = await fetch(`${baseUrl.replace(/\/$/, "")}/api/image-jobs/download?${query}`, {
        headers: {
          Authorization: `Bearer ${token}`
        }
      });

      if (!response.ok) {
        const contentType = response.headers.get("content-type") || "";
        const payload = contentType.includes("application/json") ? await response.json() : await response.text();
        throw new Error(payload?.message || payload || "download failed");
      }

      const blob = await response.blob();
      const objectUrl = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = objectUrl;
      anchor.download = "imageflow-batch.zip";
      anchor.click();
      URL.revokeObjectURL(objectUrl);
    } catch (downloadError) {
      setError(downloadError.message);
    }
  }

  const healthView = {
    ...health,
    baseUrlLabel: baseUrl.replace(/^https?:\/\//, ""),
    lastCheckedLabel: health.lastCheckedAt
      ? new Intl.DateTimeFormat("ko-KR", { hour: "2-digit", minute: "2-digit", second: "2-digit" }).format(new Date(health.lastCheckedAt))
      : "-",
    summary: health.status === "online"
      ? "백엔드와 정상 연결되었습니다."
      : health.status === "checking"
        ? "백엔드 연결 상태를 확인하는 중입니다."
        : "백엔드 연결에 실패했습니다.",
    description: health.status === "online"
      ? `${health.processingMode || "sync"} 모드 / 배치 최대 ${health.maxBatchSize || "-"}개 / ${health.queueEnabled ? "queue enabled" : "queue disabled"}`
      : "API 주소와 백엔드 실행 상태를 확인한 뒤 다시 시도해주세요."
  };

  return (
    <ConfigProvider
      theme={{
        algorithm: theme.defaultAlgorithm,
        token: {
          colorPrimary: "#1677ff",
          colorBgLayout: "#eef3f8",
          colorBorderSecondary: "#d9e2ef",
          borderRadius: 18,
          fontFamily: "\"IBM Plex Sans\", sans-serif"
        }
      }}
    >
      <div className="app-shell">
        {pageDropActive ? (
          <div className="page-drop-overlay">
            <div className="page-drop-card">
              <span className="section-kicker">Drop to Upload</span>
              <strong>Release anywhere to add images or ZIP files</strong>
              <p>ImageFlow will queue the dropped files into the current workspace immediately.</p>
            </div>
          </div>
        ) : null}

        <HeroSection user={user} onLogout={handleLogout} health={healthView} />

        <main className="main-grid">
          {user ? (
            <div className="content-column">
              <OptimizationForm
                baseUrl={baseUrl}
                files={files}
                options={options}
                error={error}
                statusMessage={statusMessage}
                user={user}
                submitLoading={submitLoading}
                watermarkGenerateLoading={watermarkGenerateLoading}
                watermarkGenerateError={watermarkGenerateError}
                health={healthView}
                onBaseUrlChange={setBaseUrl}
                onFileChange={handleFilesSelected}
                onFileRemove={handleRemoveFile}
                onFilesClear={handleClearFiles}
                onOptionsChange={setOptions}
                onGenerateWatermarkPresets={handleGenerateWatermarkPresets}
                onSubmit={handleSubmit}
              />
            </div>
          ) : (
            <div className="content-column">
              <AuthPanel
                baseUrl={baseUrl}
                error={authError}
                submitLoading={authLoading}
                health={healthView}
                onBaseUrlChange={setBaseUrl}
                onSubmit={handleAuthSubmit}
              />
            </div>
          )}

          <div className="content-column">
            <ResultPanel
              jobs={jobs}
              recentJobs={recentJobs}
              selectedJobId={selectedJobId}
              historyLoading={historyLoading}
              onDownloadBatch={handleDownloadBatch}
              onSelectJob={setSelectedJobId}
            />
          </div>
        </main>
      </div>
    </ConfigProvider>
  );
}
