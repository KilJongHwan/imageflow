export function HeroSection({ user, onLogout }) {
  return (
    <section className="hero-panel">
      <div className="hero-topbar">
        <div className="eyebrow">ImageFlow Commerce Ops</div>
        {user ? (
          <div className="session-chip">
            <span>{user.email}</span>
            <button type="button" onClick={onLogout}>Log out</button>
          </div>
        ) : null}
      </div>
      <div className="hero-grid">
        <div className="hero-copy">
          <h1>Reduce image payload, keep the product visible, and ship cleaner assets faster.</h1>
          <p className="hero-description">
            Built for commerce teams handling large product uploads across channels like Naver Smart Store,
            Coupang, and Kurly. Upload in batches, crop the exact focal area, optimize aggressively, and
            prove the savings with visible size-reduction metrics.
          </p>
          <div className="hero-metrics">
            <div className="metric-card">
              <strong>Batch-ready</strong>
              <span>Upload multiple product images and export them as a single ZIP package.</span>
            </div>
            <div className="metric-card">
              <strong>Cost-aware</strong>
              <span>Track original bytes, optimized bytes, and reduction rate directly in the workspace.</span>
            </div>
            <div className="metric-card">
              <strong>Operator-friendly</strong>
              <span>Manual crop, ratio rules, watermarking, and repeatable output settings stay in one flow.</span>
            </div>
          </div>
        </div>

        <div className="hero-notes">
          <div className="note-card">
            <span className="note-label">Workflow</span>
            <strong>Upload, crop, optimize, download</strong>
            <span>Designed to feel like an operator desk rather than a raw image utility.</span>
          </div>
          <div className="note-card">
            <span className="note-label">Impact</span>
            <strong>Storage and traffic reduction</strong>
            <span>Optimization results are framed around payload reduction, not just visual editing.</span>
          </div>
          <div className="note-card accent">
            <span className="note-label">Mode</span>
            <strong>{user ? "Authenticated workspace active" : "Secure seller workspace"}</strong>
            <span>{user ? "You can process assets and download results immediately." : "Sign in to open upload access and result history."}</span>
          </div>
        </div>
      </div>
    </section>
  );
}
