export function HeroSection() {
  return (
    <section className="hero-panel">
      <div className="eyebrow">ImageFlow Studio</div>
      <div className="hero-grid">
        <div>
          <h1>Images, refined for product teams that care about detail.</h1>
          <p className="hero-description">
            Drop in a source image, tune the frame, preserve the ratio you want, and prepare a cleaner
            optimized asset without wading through a crowded console.
          </p>
        </div>

        <div className="hero-notes">
          <div className="note-card">
            <strong>Fast start</strong>
            <span>Upload once, tune once, review the result in the same view.</span>
          </div>
          <div className="note-card">
            <strong>Built for real assets</strong>
            <span>Width, height, crop mode, ratio, watermark and quality live in one compact panel.</span>
          </div>
        </div>
      </div>
    </section>
  );
}
