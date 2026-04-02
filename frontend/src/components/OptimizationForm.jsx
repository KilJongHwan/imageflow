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
  { value: "center-crop", label: "Center Crop" }
];

export function OptimizationForm({
  baseUrl,
  file,
  options,
  error,
  statusMessage,
  onBaseUrlChange,
  onFileChange,
  onOptionsChange,
  onSubmit
}) {
  function updateOption(key, value) {
    onOptionsChange((current) => ({ ...current, [key]: value }));
  }

  return (
    <section className="control-panel">
      <div className="panel-head">
        <div>
          <h2>Optimization Setup</h2>
          <p>Configure the output frame before the worker picks up the job.</p>
        </div>
      </div>

      <form className="control-form" onSubmit={onSubmit}>
        <label className="field">
          <span>Backend URL</span>
          <input value={baseUrl} onChange={(event) => onBaseUrlChange(event.target.value)} />
        </label>

        <label className="upload-slot">
          <input
            type="file"
            accept="image/*"
            onChange={(event) => onFileChange(event.target.files?.[0] || null)}
          />
          <strong>{file ? file.name : "Select a source image"}</strong>
          <span>{file ? `${Math.round(file.size / 1024)} KB ready for upload` : "JPEG, PNG and WEBP are all supported."}</span>
        </label>

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

        <button className="primary-button" type="submit">Start Optimization</button>
        <p className="status-copy">{statusMessage}</p>
        {error ? <p className="error-copy">{error}</p> : null}
      </form>
    </section>
  );
}
