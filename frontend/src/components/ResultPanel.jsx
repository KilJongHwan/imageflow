function MetaRow({ label, value }) {
  return (
    <div className="meta-row">
      <span>{label}</span>
      <strong>{value || "-"}</strong>
    </div>
  );
}

function formatBytes(value) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return "-";
  }
  if (value < 1024) {
    return `${value} B`;
  }
  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`;
  }
  return `${(value / (1024 * 1024)).toFixed(2)} MB`;
}

function formatPercent(value) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return "-";
  }
  return `${(value * 100).toFixed(1)}%`;
}

export function ResultPanel({ jobs, onDownloadBatch }) {
  const primaryJob = jobs[0];
  const succeededJobs = jobs.filter((job) => job.status === "SUCCEEDED");
  const totalSourceBytes = succeededJobs.reduce((sum, job) => sum + (job.sourceFileSizeBytes || 0), 0);
  const totalResultBytes = succeededJobs.reduce((sum, job) => sum + (job.resultFileSizeBytes || 0), 0);
  const totalSavedBytes = succeededJobs.length ? totalSourceBytes - totalResultBytes : 0;
  const totalReductionRate = totalSourceBytes > 0 ? totalSavedBytes / totalSourceBytes : null;

  return (
    <section className="result-panel">
      <div className="panel-head">
        <div>
          <div className="section-kicker">Output & Savings</div>
          <h2>Review the optimized payload</h2>
          <p>Track visible quality decisions together with byte reduction and export readiness.</p>
        </div>
        {succeededJobs.length > 0 ? (
          <button
            className="download-button"
            type="button"
            onClick={() => onDownloadBatch(succeededJobs.map((job) => job.id))}
          >
            Download ZIP
          </button>
        ) : null}
      </div>

      {primaryJob ? (
        <div className="result-grid">
          <div className="meta-stack">
            <div className="impact-card">
              <span className="impact-label">Batch overview</span>
              <strong>{jobs.length} assets</strong>
              <p>{succeededJobs.length} optimized successfully and ready for grouped download.</p>
            </div>
            <div className="impact-card accent">
              <span className="impact-label">Payload reduction</span>
              <strong>{formatPercent(totalReductionRate)}</strong>
              <p>{formatBytes(totalSavedBytes)} saved across the current successful results.</p>
            </div>
            <MetaRow label="Original Total" value={formatBytes(totalSourceBytes)} />
            <MetaRow label="Optimized Total" value={formatBytes(totalResultBytes)} />
            <MetaRow label="Saved Total" value={formatBytes(totalSavedBytes)} />
            <MetaRow label="Job ID" value={primaryJob.id} />
            <MetaRow label="Status" value={primaryJob.status} />
            <MetaRow label="Width" value={primaryJob.targetWidth} />
            <MetaRow label="Height" value={primaryJob.targetHeight} />
            <MetaRow label="Aspect Ratio" value={primaryJob.aspectRatio} />
            <MetaRow label="Crop" value={primaryJob.cropMode} />
            <MetaRow label="Crop X" value={primaryJob.cropX} />
            <MetaRow label="Crop Y" value={primaryJob.cropY} />
            <MetaRow label="Crop Width" value={primaryJob.cropWidth} />
            <MetaRow label="Crop Height" value={primaryJob.cropHeight} />
            <MetaRow label="Watermark" value={primaryJob.watermarkText} />
            <MetaRow label="Quality" value={primaryJob.quality} />
            <MetaRow label="Source Size" value={formatBytes(primaryJob.sourceFileSizeBytes)} />
            <MetaRow label="Result Size" value={formatBytes(primaryJob.resultFileSizeBytes)} />
            <MetaRow label="Saved Bytes" value={formatBytes(primaryJob.savedBytes)} />
            <MetaRow label="Reduction Rate" value={formatPercent(primaryJob.reductionRate)} />
            {primaryJob.failureReason ? <MetaRow label="Error" value={primaryJob.failureReason} /> : null}
            {jobs.length > 1 ? (
              <div className="batch-list">
                {jobs.map((job) => (
                  <div key={job.id} className="batch-row">
                    <strong>{job.id.slice(0, 8)}</strong>
                    <span>{job.status}</span>
                  </div>
                ))}
              </div>
            ) : null}
          </div>

          <div className="preview-panel">
            {primaryJob.resultImageUrl ? (
              <>
                <img className="preview-image" src={primaryJob.resultImageUrl} alt="optimized asset" />
                <div className="preview-actions">
                  <a className="preview-link" href={primaryJob.resultImageUrl} target="_blank" rel="noreferrer">
                    Open exported image
                  </a>
                  <span className="preview-caption">The first successful result is shown as the representative preview.</span>
                </div>
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
          로그인 후 이미지를 업로드하면 처리 상태와 결과 이미지가 여기 표시됩니다.
        </div>
      )}
    </section>
  );
}
