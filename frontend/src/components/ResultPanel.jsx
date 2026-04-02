function MetaRow({ label, value }) {
  return (
    <div className="meta-row">
      <span>{label}</span>
      <strong>{value || "-"}</strong>
    </div>
  );
}

export function ResultPanel({ job }) {
  return (
    <section className="result-panel">
      <div className="panel-head">
        <div>
          <h2>Result Preview</h2>
          <p>Review the render state, dimensions and final delivered image.</p>
        </div>
      </div>

      {job ? (
        <div className="result-grid">
          <div className="meta-stack">
            <MetaRow label="Job ID" value={job.id} />
            <MetaRow label="Status" value={job.status} />
            <MetaRow label="Width" value={job.targetWidth} />
            <MetaRow label="Height" value={job.targetHeight} />
            <MetaRow label="Aspect Ratio" value={job.aspectRatio} />
            <MetaRow label="Crop" value={job.cropMode} />
            <MetaRow label="Watermark" value={job.watermarkText} />
            <MetaRow label="Quality" value={job.quality} />
            {job.failureReason ? <MetaRow label="Error" value={job.failureReason} /> : null}
          </div>

          <div className="preview-panel">
            {job.resultImageUrl ? (
              <>
                <img className="preview-image" src={job.resultImageUrl} alt="optimized asset" />
                <a className="preview-link" href={job.resultImageUrl} target="_blank" rel="noreferrer">
                  Open exported image
                </a>
              </>
            ) : (
              <div className="preview-empty">
                Your optimized asset will appear here as soon as the worker completes the job.
              </div>
            )}
          </div>
        </div>
      ) : (
        <div className="preview-empty large">
          Upload an asset to see the optimized result, processing state and output settings.
        </div>
      )}
    </section>
  );
}
