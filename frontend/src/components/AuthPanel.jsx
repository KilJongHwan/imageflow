import { useState } from "react";

const initialState = {
  email: "",
  password: ""
};

export function AuthPanel({ baseUrl, error, onBaseUrlChange, onSubmit }) {
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState(initialState);

  function updateField(key, value) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function handleSubmit(event) {
    event.preventDefault();
    onSubmit(mode, form);
  }

  return (
    <section className="control-panel auth-panel">
      <div className="panel-head">
        <div>
          <div className="section-kicker">Workspace Access</div>
          <h2>Open the seller console</h2>
          <p>Authenticate first, then upload product images into a private optimization workspace.</p>
        </div>
      </div>

      <div className="auth-toggle">
        <button
          className={mode === "login" ? "toggle-button active" : "toggle-button"}
          type="button"
          onClick={() => setMode("login")}
        >
          Login
        </button>
        <button
          className={mode === "signup" ? "toggle-button active" : "toggle-button"}
          type="button"
          onClick={() => setMode("signup")}
        >
          Sign Up
        </button>
      </div>

      <form className="control-form" onSubmit={handleSubmit}>
        <label className="field">
          <span>Backend URL</span>
          <input value={baseUrl} onChange={(event) => onBaseUrlChange(event.target.value)} />
        </label>

        <label className="field auth-field">
          <span>Email</span>
          <input
            type="email"
            value={form.email}
            onChange={(event) => updateField("email", event.target.value)}
            placeholder="you@brand.com"
          />
        </label>

        <label className="field auth-field">
          <span>Password</span>
          <input
            type="password"
            value={form.password}
            onChange={(event) => updateField("password", event.target.value)}
            placeholder="At least 8 characters"
          />
        </label>

        <button className="primary-button" type="submit">
          {mode === "login" ? "Login to Continue" : "Create Account"}
        </button>
        <div className="auth-footnote">
          <div className="auth-mini-card">
            <strong>Protected uploads</strong>
            <span>Anonymous traffic is blocked so image processing stays tied to a signed-in account.</span>
          </div>
          <div className="auth-mini-card">
            <strong>Immediate testing</strong>
            <span>New accounts start with demo credits so the full upload flow can be tried right away.</span>
          </div>
        </div>
        <p className="status-copy">
          {mode === "login"
            ? "Existing account holders can jump straight into optimization."
            : "New accounts start with demo credits so you can test the flow immediately."}
        </p>
        {error ? <p className="error-copy">{error}</p> : null}
      </form>
    </section>
  );
}
