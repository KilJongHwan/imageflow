import { startTransition, useEffect, useRef, useState } from "react";
import { ConfigProvider, theme } from "antd";

import { AuthPage } from "./components/AuthPage";
import { HeroSection } from "./components/HeroSection";
import { LandingPage } from "./components/LandingPage";
import { OptimizationForm } from "./components/OptimizationForm";
import { ResultPanel } from "./components/ResultPanel";
import { WorkspaceDashboard } from "./components/WorkspaceDashboard";

const POLL_INTERVAL_MS = 2500;
const HEALTH_TIMEOUT_MS = 8000;
const WAKE_RETRY_DELAY_MS = 2500;
const WAKE_MAX_ATTEMPTS = 20;
// 백엔드 app.upload 한도와 맞춰둡니다. 무료 인스턴스 메모리 안에서 처리할 수 있는 크기입니다.
const MAX_IMAGE_FILE_BYTES = 10 * 1024 * 1024;
const MAX_ZIP_FILE_BYTES = 40 * 1024 * 1024;
const MAX_TOTAL_UPLOAD_BYTES = 40 * 1024 * 1024;
const TOKEN_STORAGE_KEY = "imageflow.jwt";
const DEFAULT_API_BASE_URL = import.meta.env.DEV
  ? "http://localhost:8080"
  : (import.meta.env.VITE_API_BASE_URL || window.location.origin);

const initialOptions = {
  presetId: "kurly",
  width: "1600",
  height: "",
  quality: "82",
  aspectRatio: "original",
  watermarkEnabled: false,
  watermarkMode: "text",
  watermarkText: "",
  watermarkFontFamily: "sans",
  watermarkAccentText: "",
  watermarkImageFile: null,
  watermarkImagePreviewUrl: "",
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

function formatMegabytes(bytes) {
  return `${Math.round(bytes / (1024 * 1024))}MB`;
}

function formatApiErrorMessage(message) {
  if (!message) {
    return "요청을 처리하지 못했습니다. 잠시 후 다시 시도해주세요.";
  }

  if (message.includes("image file is too large")) {
    return `이미지 용량이 너무 커서 메모리가 부족합니다. 한 장당 ${formatMegabytes(MAX_IMAGE_FILE_BYTES)} 이하로 줄여서 업로드해주세요.`;
  }

  if (message.includes("total upload size is too large")) {
    return `한 번에 올린 이미지 용량이 너무 커서 메모리가 부족합니다. 합계 ${formatMegabytes(MAX_TOTAL_UPLOAD_BYTES)} 이하로 나눠서 업로드해주세요.`;
  }

  if (message.includes("image resolution is too large")) {
    return "이미지 해상도가 너무 커서 메모리가 부족합니다. 3000만 화소 이하로 줄여서 업로드해주세요.";
  }

  if (message.includes("zip entry is too large")) {
    return `ZIP 안의 개별 이미지 용량이 너무 커서 메모리가 부족합니다. 한 장당 ${formatMegabytes(MAX_IMAGE_FILE_BYTES)} 이하 이미지로 다시 압축해서 업로드해주세요.`;
  }

  if (message.includes("zip archive is too large after extraction")) {
    return `ZIP 압축 해제 용량이 너무 커서 메모리가 부족합니다. 합계 ${formatMegabytes(MAX_TOTAL_UPLOAD_BYTES)} 이하로 나눠서 업로드해주세요.`;
  }

  if (message.includes("zip archive contains too many entries")) {
    return "ZIP 안의 파일 개수가 너무 많습니다. 한 번에 32개 이하로 나눠 업로드해주세요.";
  }

  if (message.includes("batch upload supports up to")) {
    return "한 번에 처리할 수 있는 이미지 수를 초과했습니다. 배치를 나눠 다시 업로드해주세요.";
  }

  if (message.includes("Maximum upload size exceeded")
    || message.includes("max-request-size")
    || message.includes("max-file-size")) {
    return `이미지 용량이 너무 커서 메모리가 부족합니다. 한 장당 ${formatMegabytes(MAX_IMAGE_FILE_BYTES)}, 합계 ${formatMegabytes(MAX_TOTAL_UPLOAD_BYTES)} 이하로 나눠서 업로드해주세요.`;
  }

  if (message.includes("Failed to fetch") || message.includes("NetworkError")) {
    return "백엔드 연결이 끊겼습니다. 서버 실행 상태를 확인한 뒤 다시 시도해주세요.";
  }

  return message;
}

export default function App() {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || DEFAULT_API_BASE_URL;
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_STORAGE_KEY) || "");
  const [user, setUser] = useState(null);
  const [publicScreen, setPublicScreen] = useState("landing");
  const [files, setFiles] = useState([]);
  const [filePreviewUrls, setFilePreviewUrls] = useState(new Map());
  const [options, setOptions] = useState(initialOptions);
  const [statusMessage, setStatusMessage] = useState("로그인 후 이미지를 업로드하면 바로 최적화를 시작할 수 있습니다.");
  const [jobs, setJobs] = useState([]);
  const [recentJobs, setRecentJobs] = useState([]);
  const [selectedJobId, setSelectedJobId] = useState("");
  const [error, setError] = useState("");
  const [authError, setAuthError] = useState("");
  const [authLoading, setAuthLoading] = useState(false);
  const [authProviders, setAuthProviders] = useState([]);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [health, setHealth] = useState({
    status: "checking",
    processingMode: "",
    maxBatchSize: null,
    queueEnabled: false,
    queueDepth: 0,
    outboxPendingCount: 0,
    maxQueueBacklogDepth: null,
    queueWritable: true,
    lastCheckedAt: "",
    message: ""
  });
  const [wakeAttempt, setWakeAttempt] = useState(0);
  const pollTimerRef = useRef(null);
  const pollingEnabledRef = useRef(false);
  const dragDepthRef = useRef(0);
  const [pageDropActive, setPageDropActive] = useState(false);

  useEffect(() => () => {
    stopPolling();
  }, []);

  useEffect(() => () => {
    if (options.watermarkImagePreviewUrl) {
      URL.revokeObjectURL(options.watermarkImagePreviewUrl);
    }
  }, [options.watermarkImagePreviewUrl]);

  useEffect(() => {
    checkHealth();
    loadAuthProviders();
  }, []);

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
    const supportedFiles = nextFiles.filter(isSupportedUploadFile);
    const invalidFiles = nextFiles.filter((file) => !isSupportedUploadFile(file));
    const oversizedFiles = supportedFiles.filter((file) => !isWithinSizeLimit(file));
    const validFiles = supportedFiles.filter(isWithinSizeLimit);

    setFiles((currentFiles) => mergeFiles(currentFiles, validFiles));

    setFilePreviewUrls((currentUrls) => {
      const updated = new Map(currentUrls);
      for (const file of validFiles) {
        if (!isZipFile(file)) {
          const key = `${file.name}|${file.size}|${file.lastModified}`;
          if (!updated.has(key)) {
            updated.set(key, URL.createObjectURL(file));
          }
        }
      }
      return updated;
    });

    setOptions((current) => ({
      ...current,
      cropX: "",
      cropY: "",
      cropWidth: "",
      cropHeight: ""
    }));

    if (oversizedFiles.length > 0) {
      const excludedNames = oversizedFiles.map((file) => file.name).join(", ");
      setError(
        `이미지 용량이 너무 커서 메모리가 부족합니다. 한 장당 ${formatMegabytes(MAX_IMAGE_FILE_BYTES)} 이하로 줄여서 업로드해주세요. (제외된 파일: ${excludedNames})`
      );
    } else if (invalidFiles.length > 0) {
      setError("jpg, jpeg, png, webp 이미지와 zip 파일만 업로드할 수 있습니다.");
    } else {
      setError("");
    }
  }

  function isWithinSizeLimit(file) {
    const limitBytes = isZipFile(file) ? MAX_ZIP_FILE_BYTES : MAX_IMAGE_FILE_BYTES;
    return file.size <= limitBytes;
  }

  function isZipFile(file) {
    const filename = (file?.name || "").toLowerCase();
    const mimeType = (file?.type || "").toLowerCase();

    return filename.endsWith(".zip")
      || mimeType === "application/zip"
      || mimeType === "application/x-zip-compressed";
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

    const key = `${fileToRemove.name}|${fileToRemove.size}|${fileToRemove.lastModified}`;
    setFilePreviewUrls((currentUrls) => {
      const updated = new Map(currentUrls);
      const previewUrl = updated.get(key);
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
        updated.delete(key);
      }
      return updated;
    });
  }

  function handleClearFiles() {
    setFiles([]);

    setFilePreviewUrls((currentUrls) => {
      currentUrls.forEach((url) => URL.revokeObjectURL(url));
      return new Map();
    });

    setOptions((current) => ({
      ...current,
      cropX: "",
      cropY: "",
      cropWidth: "",
      cropHeight: ""
    }));
  }

  function handleWatermarkImageChange(file) {
    setOptions((current) => {
      if (current.watermarkImagePreviewUrl) {
        URL.revokeObjectURL(current.watermarkImagePreviewUrl);
      }

      if (!file) {
        return {
          ...current,
          watermarkMode: "text",
          watermarkImageFile: null,
          watermarkImagePreviewUrl: ""
        };
      }

      return {
        ...current,
        watermarkMode: "upload",
        watermarkImageFile: file,
        watermarkImagePreviewUrl: URL.createObjectURL(file)
      };
    });
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

  function delay(durationMs) {
    return new Promise((resolve) => setTimeout(resolve, durationMs));
  }

  async function fetchHealthOnce(timeoutMs) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    try {
      const response = await fetch(`${baseUrl.replace(/\/$/, "")}/api/health`, {
        signal: controller.signal
      });
      if (!response.ok) {
        throw new Error(`health check failed with ${response.status}`);
      }
      return await response.json();
    } finally {
      clearTimeout(timer);
    }
  }

  function applyOnlineHealth(payload) {
    setHealth({
      status: "online",
      processingMode: payload.processingMode || "",
      maxBatchSize: payload.maxBatchSize ?? null,
      queueEnabled: Boolean(payload.queueEnabled),
      queueDepth: payload.queueDepth ?? 0,
      outboxPendingCount: payload.outboxPendingCount ?? 0,
      maxQueueBacklogDepth: payload.maxQueueBacklogDepth ?? null,
      queueWritable: payload.queueWritable ?? true,
      lastCheckedAt: payload.timestamp || new Date().toISOString(),
      message: ""
    });
  }

  // Render 무료 서버는 15분 미사용 시 슬립에 들어가 첫 요청이 느립니다.
  // 빠른 1회 프로브로 깨어있으면 바로 통과하고, 느리면 깨어날 때까지 재시도합니다.
  // (이 화면 오버레이는 서버를 깨워두는 keep-alive가 아니라, 콜드스타트가
  //  실제로 발생했을 때 방문자에게 보여줄 폴백입니다. keep-alive는 GitHub Actions cron이 담당합니다.)
  async function checkHealth() {
    try {
      const payload = await fetchHealthOnce(HEALTH_TIMEOUT_MS);
      applyOnlineHealth(payload);
      return;
    } catch (_error) {
      // 첫 프로브 실패/타임아웃 → 슬립 상태로 보고 깨우기 루프로 진입합니다.
    }

    // 로컬 개발 환경에는 콜드스타트(슬립)가 없으므로 깨우기 재시도 루프를 돌지 않습니다.
    // 백엔드가 떠 있지 않으면 한 번만 확인하고 바로 offline 으로 표시합니다.
    if (!import.meta.env.DEV) {
      setHealth((current) => ({ ...current, status: "waking", message: "" }));

      for (let attempt = 1; attempt <= WAKE_MAX_ATTEMPTS; attempt += 1) {
        setWakeAttempt(attempt);
        try {
          const payload = await fetchHealthOnce(HEALTH_TIMEOUT_MS);
          applyOnlineHealth(payload);
          setWakeAttempt(0);
          return;
        } catch (_error) {
          await delay(WAKE_RETRY_DELAY_MS);
        }
      }
    }

    setWakeAttempt(0);
    setHealth({
      status: "offline",
      processingMode: "",
      maxBatchSize: null,
      queueEnabled: false,
      queueDepth: 0,
      outboxPendingCount: 0,
      maxQueueBacklogDepth: null,
      queueWritable: true,
      lastCheckedAt: new Date().toISOString(),
      message: "API health endpoint에 연결할 수 없습니다."
    });
  }

  async function loadAuthProviders() {
    try {
      const response = await fetch(`${baseUrl.replace(/\/$/, "")}/api/auth/providers`);
      if (!response.ok) {
        throw new Error("provider lookup failed");
      }
      const payload = await response.json();
      setAuthProviders(Array.isArray(payload) ? payload : []);
    } catch (_error) {
      setAuthProviders([]);
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
      setPublicScreen("landing");
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

      startTransition(() => {
        setToken(payload.token);
        setUser(payload.user);
      });
      setPublicScreen("landing");
      window.scrollTo({ top: 0, behavior: "smooth" });
      setStatusMessage(mode === "login" ? "로그인이 완료되었습니다. 이미지를 올리면 바로 처리됩니다." : "계정 생성이 완료되었습니다. 워크스페이스에 입장합니다.");
    } catch (submitError) {
      setAuthError(formatApiErrorMessage(submitError.message));
    } finally {
      setAuthLoading(false);
    }
  }

  function handleLogout() {
    stopPolling();
    startTransition(() => {
      setToken("");
      setUser(null);
      setJobs([]);
      setRecentJobs([]);
      setFiles([]);
      setOptions((current) => {
        if (current.watermarkImagePreviewUrl) {
          URL.revokeObjectURL(current.watermarkImagePreviewUrl);
        }
        return initialOptions;
      });
      setSelectedJobId("");
      setPageDropActive(false);
      setError("");
      setAuthError("");
      setPublicScreen("landing");
      setStatusMessage("로그인 후 이미지를 업로드하면 바로 최적화를 시작할 수 있습니다.");
    });
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  async function handleSubmit(event) {
    if (event?.preventDefault) {
      event.preventDefault();
    }

    if (!files.length) {
      setError("먼저 최적화할 이미지를 선택해주세요.");
      return;
    }
    const totalUploadBytes = files.reduce((total, file) => total + file.size, 0);
    if (totalUploadBytes > MAX_TOTAL_UPLOAD_BYTES) {
      setError(
        `한 번에 올린 이미지 용량이 너무 커서 메모리가 부족합니다. 합계 ${formatMegabytes(MAX_TOTAL_UPLOAD_BYTES)} 이하로 나눠서 업로드해주세요. (현재 ${formatMegabytes(totalUploadBytes)})`
      );
      return;
    }
    if (options.cropMode === "manual" && files.length > 1) {
      setError("수동 크롭은 한 번에 한 장 업로드할 때만 사용할 수 있습니다.");
      return;
    }
    if (options.watermarkEnabled && options.watermarkMode !== "upload" && !options.watermarkText.trim()) {
      setError("워터마크 적용을 켠 경우 브랜드명 또는 워터마크 텍스트가 필요합니다.");
      return;
    }

    setError("");
    setJobs([]);
    stopPolling();
    setSubmitLoading(true);
    setStatusMessage("원본을 업로드하고 처리 작업을 준비하는 중입니다.");

    const containsArchive = files.some(isZipFile);
    const shouldUseBatchEndpoint = files.length > 1 || containsArchive;

    const formData = new FormData();
    if (!shouldUseBatchEndpoint) {
      formData.append("file", files[0]);
    } else {
      files.forEach((file) => formData.append("files", file));
    }
    if (options.width) formData.append("width", options.width);
    if (options.height) formData.append("height", options.height);
    if (options.quality) formData.append("quality", options.quality);
    if (options.aspectRatio) formData.append("aspectRatio", options.aspectRatio);
    if (options.watermarkEnabled) {
      if (options.watermarkMode !== "upload" && options.watermarkText) formData.append("watermarkText", options.watermarkText);
      if (options.watermarkMode !== "upload" && options.watermarkAccentText) formData.append("watermarkAccentText", options.watermarkAccentText);
      if (options.watermarkMode !== "upload" && options.watermarkFontFamily) formData.append("watermarkFontFamily", options.watermarkFontFamily);
      if (options.watermarkImageFile) formData.append("watermarkImage", options.watermarkImageFile);
      if (options.watermarkStyle) formData.append("watermarkStyle", options.watermarkStyle);
      if (options.watermarkPosition) formData.append("watermarkPosition", options.watermarkPosition);
      if (options.watermarkOpacity) formData.append("watermarkOpacity", options.watermarkOpacity);
      if (options.watermarkScalePercent) formData.append("watermarkScalePercent", options.watermarkScalePercent);
    }
    if (options.cropMode) formData.append("cropMode", options.cropMode);
    if (options.cropX) formData.append("cropX", options.cropX);
    if (options.cropY) formData.append("cropY", options.cropY);
    if (options.cropWidth) formData.append("cropWidth", options.cropWidth);
    if (options.cropHeight) formData.append("cropHeight", options.cropHeight);

    try {
      const endpoint = shouldUseBatchEndpoint ? "/api/image-jobs/uploads" : "/api/image-jobs/upload";
      const created = await request(endpoint, {
        method: "POST",
        body: formData
      });
      const nextJobs = Array.isArray(created.jobs) ? created.jobs : [created];

      const nonZipFiles = files.filter((f) => !isZipFile(f));
      const jobsWithPreview = nextJobs.map((job, index) => ({
        ...job,
        sourceImagePreviewUrl: index < nonZipFiles.length
          ? filePreviewUrls.get(`${nonZipFiles[index].name}|${nonZipFiles[index].size}|${nonZipFiles[index].lastModified}`)
          : undefined
      }));

      setJobs(jobsWithPreview);
      setSelectedJobId(jobsWithPreview[0]?.id || "");
      mergeJobsIntoHistory(jobsWithPreview);
      setStatusMessage(
        jobsWithPreview.length > 1
          ? `${jobsWithPreview.length}개 작업이 등록되었습니다. 결과를 확인하고 ZIP으로 다운로드할 수 있습니다.`
          : "큐에 등록되었습니다. 최적화 결과를 확인하는 중입니다."
      );
      if (jobsWithPreview.length === 1) {
        pollingEnabledRef.current = true;
        pollJob(jobsWithPreview[0].id);
      } else {
        pollingEnabledRef.current = true;
        pollBatch(jobsWithPreview.map((job) => job.id));
      }
    } catch (submitError) {
      setError(formatApiErrorMessage(submitError.message));
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
      setJobs((currentJobs) => {
        const existingJob = currentJobs.find((j) => j.id === latestJob.id);
        return [{
          ...latestJob,
          sourceImagePreviewUrl: existingJob?.sourceImagePreviewUrl
        }];
      });
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
      setError(formatApiErrorMessage(pollError.message));
      setStatusMessage("상태 확인이 중단되었습니다.");
    }
  }

  async function pollBatch(jobIds) {
    if (!pollingEnabledRef.current || !jobIds.length) {
      return;
    }

    stopPolling();
    pollingEnabledRef.current = true;

    try {
      const latestJobs = await Promise.all(
        jobIds.map((jobId) => request(`/api/image-jobs/${jobId}`))
      );

      if (!pollingEnabledRef.current) {
        return;
      }

      setJobs((currentJobs) => {
        const previewUrlMap = new Map(currentJobs.map((j) => [j.id, j.sourceImagePreviewUrl]));
        return latestJobs.map((job) => ({
          ...job,
          sourceImagePreviewUrl: previewUrlMap.get(job.id)
        }));
      });
      mergeJobsIntoHistory(latestJobs);
      setSelectedJobId((currentSelectedId) => {
        if (currentSelectedId && latestJobs.some((job) => job.id === currentSelectedId)) {
          return currentSelectedId;
        }
        return latestJobs[0]?.id || "";
      });

      const completedCount = latestJobs.filter((job) => job.status === "SUCCEEDED").length;
      const failedCount = latestJobs.filter((job) => job.status === "FAILED").length;
      const processingCount = latestJobs.length - completedCount - failedCount;

      if (processingCount === 0) {
        stopPolling();
        if (failedCount > 0) {
          setStatusMessage(`${latestJobs.length}개 중 ${completedCount}개 완료, ${failedCount}개 실패했습니다.`);
        } else {
          setStatusMessage(`${latestJobs.length}개 작업이 모두 완료되었습니다.`);
        }
        return;
      }

      setStatusMessage(`${latestJobs.length}개 중 ${completedCount}개 완료, ${processingCount}개 처리 중입니다.`);
      pollTimerRef.current = setTimeout(() => pollBatch(jobIds), POLL_INTERVAL_MS);
    } catch (pollError) {
      stopPolling();
      setError(formatApiErrorMessage(pollError.message));
      setStatusMessage("배치 상태 확인이 중단되었습니다.");
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
      setError(formatApiErrorMessage(downloadError.message));
    }
  }

  const healthView = {
    ...health,
    lastCheckedLabel: health.lastCheckedAt
      ? new Intl.DateTimeFormat("ko-KR", { hour: "2-digit", minute: "2-digit", second: "2-digit" }).format(new Date(health.lastCheckedAt))
      : "-",
    summary: health.status === "online"
      ? "백엔드와 정상 연결되었습니다."
      : health.status === "waking"
        ? "서버를 깨우는 중입니다."
        : health.status === "checking"
          ? "백엔드 연결 상태를 확인하는 중입니다."
          : "백엔드 연결에 실패했습니다.",
    description: health.status === "online"
      ? `${health.processingMode || "sync"} 모드 / 배치 최대 ${health.maxBatchSize || "-"}개 / ${health.queueEnabled ? `queue ${health.queueDepth ?? 0}/${health.maxQueueBacklogDepth || "-"}` : "queue disabled"}`
      : health.status === "waking"
        ? "무료 서버가 슬립에서 깨어나는 중입니다. 30~60초 정도 걸릴 수 있어요."
        : "배포 환경 설정 또는 백엔드 실행 상태를 확인한 뒤 다시 시도해주세요."
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
        {health.status === "waking" ? (
          <div className="wake-overlay">
            <div className="wake-card">
              <span className="section-kicker">Server Warming Up</span>
              <div className="wake-spinner" aria-hidden="true" />
              <strong>서버를 깨우는 중입니다</strong>
              <p>
                무료 호스팅(Render)은 일정 시간 미사용 시 잠자기 상태로 전환됩니다.
                지금 첫 요청을 처리할 수 있도록 서버를 깨우고 있어요. 보통 30~60초 정도 걸립니다.
              </p>
              <div className="wake-progress" aria-hidden="true"><span /></div>
              {wakeAttempt > 1 ? (
                <p className="wake-hint">계속 깨우는 중이에요 · 시도 {wakeAttempt}회</p>
              ) : null}
            </div>
          </div>
        ) : null}

        {pageDropActive ? (
          <div className="page-drop-overlay">
            <div className="page-drop-card">
              <span className="section-kicker">Drop to Upload</span>
              <strong>Release anywhere to add images or ZIP files</strong>
              <p>ImageFlow will queue the dropped files into the current workspace immediately.</p>
            </div>
          </div>
        ) : null}

        <div key={user ? "workspace" : publicScreen} className="app-view-shell">
          {user ? (
            <>
            <HeroSection user={user} onLogout={handleLogout} health={healthView} />

              <WorkspaceDashboard user={user} recentJobs={recentJobs} jobs={jobs} health={healthView} />

              <main className="workspace-app-grid">
                <div className="workspace-primary-column">
                <OptimizationForm
                  files={files}
                  options={options}
                  error={error}
                  statusMessage={statusMessage}
                  user={user}
                  submitLoading={submitLoading}
                  health={healthView}
                  onFileChange={handleFilesSelected}
                  onFileRemove={handleRemoveFile}
                  onFilesClear={handleClearFiles}
                  onOptionsChange={setOptions}
                  onWatermarkImageChange={handleWatermarkImageChange}
                  onSubmit={handleSubmit}
                />
                </div>

                <div className="workspace-secondary-column">
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
            </>
          ) : (
            publicScreen === "auth" ? (
              <AuthPage
                error={authError}
                providers={authProviders}
                submitLoading={authLoading}
                health={healthView}
                onBack={() => setPublicScreen("landing")}
                onSubmit={handleAuthSubmit}
              />
            ) : (
              <LandingPage
                health={healthView}
                onOpenAuth={() => setPublicScreen("auth")}
              />
            )
          )}
        </div>
      </div>
    </ConfigProvider>
  );
}
