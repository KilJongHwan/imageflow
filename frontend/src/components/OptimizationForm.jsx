import { CropSelector } from "./CropSelector";
import { useState } from "react";

const aspectRatioOptions = [
  { value: "original", label: "Original" },
  { value: "1:1", label: "1:1" },
  { value: "4:5", label: "4:5" },
  { value: "3:4", label: "3:4" },
  { value: "16:9", label: "16:9" },
  { value: "9:16", label: "9:16" }
];

const cropModeOptions = [
  { value: "fit", label: "Fit Inside" },
  { value: "center-crop", label: "Center Crop" },
  { value: "manual", label: "Manual Region" }
];

export function OptimizationForm({
  baseUrl,
  files,
  options,
  error,
  statusMessage,
  user,
  onBaseUrlChange,
  onFileChange,
  onOptionsChange,
  onSubmit
}) {
  const acceptedFileTypes = ".jpg,.jpeg,.png,.webp,.zip";
  const [dragActive, setDragActive] = useState(false);

  function updateOption(key, value) {
    onOptionsChange((current) => {
      if (key === "cropMode" && value !== "manual") {
        return {
          ...current,
          cropMode: value,
          cropX: "",
          cropY: "",
          cropWidth: "",
          cropHeight: ""
        };
      }

      return { ...current, [key]: value };
    });
  }

  function handleFileChange(nextFiles) {
    onFileChange(nextFiles);
  }

  function handleDrop(event) {
    event.preventDefault();
    setDragActive(false);
    handleFileChange(Array.from(event.dataTransfer.files || []));
  }

  return (
    <section className="control-panel">
      <div className="panel-head">
        <div>
          <div className="section-kicker">Optimization Workspace</div>
          <h2>Prepare the output rules</h2>
          <p>{user.email} 계정으로 로그인된 상태입니다. 셀러용 업로드 규칙을 정하고 바로 처리하세요.</p>
        </div>
        <div className="panel-badge">Up to 10 files per batch</div>
      </div>

      <form className="control-form" onSubmit={onSubmit}>
        <div className="settings-card">
          <div className="settings-card-head">
            <strong>Environment</strong>
            <span>Choose the API target used for uploads and processing.</span>
          </div>
          <label className="field">
            <span>Backend URL</span>
            <input value={baseUrl} onChange={(event) => onBaseUrlChange(event.target.value)} />
          </label>
        </div>

        <div className="settings-card">
          <div className="settings-card-head">
            <strong>Source Assets</strong>
            <span>Select one hero image or a small batch to optimize with the same rule set.</span>
          </div>
          <label
            className={dragActive ? "upload-slot dragging" : "upload-slot"}
            onDragOver={(event) => {
              event.preventDefault();
              setDragActive(true);
            }}
            onDragLeave={() => setDragActive(false)}
            onDrop={handleDrop}
          >
            <input
              type="file"
              accept={acceptedFileTypes}
              multiple
              onChange={(event) => handleFileChange(Array.from(event.target.files || []))}
            />
            <strong>
              {files.length
                ? files.length === 1
                  ? files[0].name
                  : `${files.length} assets ready`
                : "Drop images or ZIP files into the workspace"}
            </strong>
            <span>
              {files.length
                ? files.length === 1
                  ? `${Math.round(files[0].size / 1024)} KB queued for optimization`
                  : "Batch upload ready. Images and ZIP archives will be expanded with the same optimization rule."
                : "Drag files anywhere on the page, or click this area to browse the local workspace."}
            </span>
            {files.length > 0 ? (
              <div className="file-pill-row">
                {files.map((file) => (
                  <span key={`${file.name}-${file.size}`} className="file-pill">
                    {file.name}
                  </span>
                ))}
              </div>
            ) : null}
          </label>
        </div>

        <div className="settings-card">
          <div className="settings-card-head">
            <strong>Output Rules</strong>
            <span>Define how aggressively the uploaded assets should be trimmed and exported.</span>
          </div>
          <div className="field-grid">
            <label className="field">
              <span>Width</span>
              <input value={options.width} onChange={(event) => updateOption("width", event.target.value)} />
            </label>
            <label className="field">
              <span>Height</span>
              <input value={options.height} onChange={(event) => updateOption("height", event.target.value)} />
            </label>
            <label className="field">
              <span>Quality</span>
              <input value={options.quality} onChange={(event) => updateOption("quality", event.target.value)} />
            </label>
          </div>

          <div className="field-grid">
            <label className="field">
              <span>Aspect Ratio</span>
              <select value={options.aspectRatio} onChange={(event) => updateOption("aspectRatio", event.target.value)}>
                {aspectRatioOptions.map((option) => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>Crop</span>
              <select value={options.cropMode} onChange={(event) => updateOption("cropMode", event.target.value)}>
                {cropModeOptions.map((option) => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>Watermark</span>
              <input
                placeholder="Optional brand mark"
                value={options.watermarkText}
                onChange={(event) => updateOption("watermarkText", event.target.value)}
              />
            </label>
          </div>
          <div className="rule-summary">
            <span>Current mode</span>
            <strong>{options.cropMode === "manual" ? "Manual crop with focal selection" : options.cropMode === "center-crop" ? "Automatic center crop" : "Fit inside export"}</strong>
          </div>
        </div>

        {options.cropMode === "manual" && files.length === 1 ? (
          <div className="settings-card">
            <div className="settings-card-head">
              <strong>Crop Target</strong>
              <span>Drag the exact visible product area that should survive the optimization pass.</span>
            </div>
            <CropSelector
              file={files[0]}
              options={options}
              onOptionsChange={onOptionsChange}
            />
          </div>
        ) : null}

        {options.cropMode === "manual" && files.length > 1 ? (
          <div className="settings-card">
            <div className="crop-selector-empty">
              수동 크롭은 한 번에 한 장만 지정할 수 있습니다. 여러 장을 처리할 때는 `Fit Inside` 또는 `Center Crop`을 사용하세요.
            </div>
          </div>
        ) : null}

        <div className="action-row">
          <button className="primary-button" type="submit">Start Optimization</button>
          <div className="action-note">
            <strong>Expected outcome</strong>
            <span>Upload traffic stays authenticated, output payload drops, and batch results can be downloaded together.</span>
          </div>
        </div>
        <p className="status-copy">{statusMessage}</p>
        {error ? <p className="error-copy">{error}</p> : null}
      </form>
    </section>
  );
}
