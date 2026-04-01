import { useState } from "react";

const initialUserForm = {
  email: "hello@imageflow.dev",
  plan: "PRO",
  initialCredits: 10
};

const initialJobForm = {
  userId: "",
  prompt: "Turn this product shot into a premium landing page hero image.",
  sourceImageUrl: "https://example.com/source.png",
  creditsToUse: 3
};

function pretty(data) {
  return typeof data === "string" ? data : JSON.stringify(data, null, 2);
}

export default function App() {
  const [baseUrl, setBaseUrl] = useState("http://localhost:8080");
  const [userForm, setUserForm] = useState(initialUserForm);
  const [jobForm, setJobForm] = useState(initialJobForm);
  const [imageJobId, setImageJobId] = useState("");
  const [healthResult, setHealthResult] = useState("Health response will appear here.");
  const [userResult, setUserResult] = useState("Created user will appear here.");
  const [jobResult, setJobResult] = useState("Created image job will appear here.");
  const [lookupResult, setLookupResult] = useState("Fetched image job will appear here.");

  async function request(path, options = {}) {
    const headers = {
      ...(options.headers || {})
    };

    if (options.body && !headers["Content-Type"]) {
      headers["Content-Type"] = "application/json";
    }

    const response = await fetch(`${baseUrl.replace(/\/$/, "")}${path}`, {
      headers,
      ...options
    });

    const text = await response.text();
    const payload = text ? JSON.parse(text) : null;

    if (!response.ok) {
      throw new Error(payload?.message || `Request failed with ${response.status}`);
    }

    return payload;
  }

  async function handleHealthCheck() {
    try {
      setHealthResult("Loading...");
      const payload = await request("/api/health", { method: "GET" });
      setHealthResult(pretty(payload));
    } catch (error) {
      setHealthResult(`Health check failed: ${error.message}`);
    }
  }

  async function handleCreateUser(event) {
    event.preventDefault();

    try {
      setUserResult("Creating user...");
      const createdUser = await request("/api/users", {
        method: "POST",
        body: JSON.stringify({
          ...userForm,
          initialCredits: Number(userForm.initialCredits)
        })
      });
      setUserResult(pretty(createdUser));
      setJobForm((current) => ({ ...current, userId: createdUser.id }));
    } catch (error) {
      setUserResult(`Create user failed: ${error.message}`);
    }
  }

  async function handleCreateImageJob(event) {
    event.preventDefault();

    try {
      setJobResult("Creating image job...");
      const imageJob = await request("/api/image-jobs", {
        method: "POST",
        body: JSON.stringify({
          ...jobForm,
          creditsToUse: Number(jobForm.creditsToUse)
        })
      });
      setJobResult(pretty(imageJob));
      setImageJobId(imageJob.id);
    } catch (error) {
      setJobResult(`Create image job failed: ${error.message}`);
    }
  }

  async function handleLookup(event) {
    event.preventDefault();

    if (!imageJobId.trim()) {
      setLookupResult("Image job id is required.");
      return;
    }

    try {
      setLookupResult("Fetching image job...");
      const payload = await request(`/api/image-jobs/${imageJobId.trim()}`, { method: "GET" });
      setLookupResult(pretty(payload));
    } catch (error) {
      setLookupResult(`Fetch image job failed: ${error.message}`);
    }
  }

  return (
    <div className="page-shell">
      <header className="hero">
        <div className="eyebrow">ImageFlow React Starter</div>
        <h1>Run the first image job from one small React console.</h1>
        <p className="hero-copy">
          This frontend is now structured as a React app so we can keep the current API playground
          while growing into reusable components and real screens.
        </p>
        <div className="hero-meta">
          <span>Backend default: <code>http://localhost:8080</code></span>
          <span>Flow: user create -&gt; job create -&gt; job lookup</span>
        </div>
      </header>

      <main className="dashboard">
        <section className="panel">
          <div className="panel-header">
            <h2>Connection</h2>
            <p>Point the app at the backend API.</p>
          </div>
          <label className="field">
            <span>Backend Base URL</span>
            <input value={baseUrl} onChange={(event) => setBaseUrl(event.target.value)} />
          </label>
          <button className="secondary-button" type="button" onClick={handleHealthCheck}>Check Health</button>
          <pre className="output">{healthResult}</pre>
        </section>

        <section className="panel">
          <div className="panel-header">
            <h2>Create User</h2>
            <p>Create a user with credits for image generation.</p>
          </div>
          <form className="stack" onSubmit={handleCreateUser}>
            <label className="field">
              <span>Email</span>
              <input
                type="email"
                value={userForm.email}
                onChange={(event) => setUserForm((current) => ({ ...current, email: event.target.value }))}
                required
              />
            </label>
            <label className="field">
              <span>Plan</span>
              <select
                value={userForm.plan}
                onChange={(event) => setUserForm((current) => ({ ...current, plan: event.target.value }))}
              >
                <option value="FREE">FREE</option>
                <option value="PRO">PRO</option>
              </select>
            </label>
            <label className="field">
              <span>Initial Credits</span>
              <input
                type="number"
                min="0"
                value={userForm.initialCredits}
                onChange={(event) => setUserForm((current) => ({ ...current, initialCredits: event.target.value }))}
              />
            </label>
            <button className="primary-button" type="submit">Create User</button>
          </form>
          <pre className="output">{userResult}</pre>
        </section>

        <section className="panel">
          <div className="panel-header">
            <h2>Create Image Job</h2>
            <p>Use the created user to reserve credits and create an image-processing job.</p>
          </div>
          <form className="stack" onSubmit={handleCreateImageJob}>
            <label className="field">
              <span>User ID</span>
              <input
                value={jobForm.userId}
                onChange={(event) => setJobForm((current) => ({ ...current, userId: event.target.value }))}
                placeholder="Filled automatically after user creation"
                required
              />
            </label>
            <label className="field">
              <span>Prompt</span>
              <textarea
                rows="4"
                value={jobForm.prompt}
                onChange={(event) => setJobForm((current) => ({ ...current, prompt: event.target.value }))}
                required
              />
            </label>
            <label className="field">
              <span>Source Image URL</span>
              <input
                type="url"
                value={jobForm.sourceImageUrl}
                onChange={(event) => setJobForm((current) => ({ ...current, sourceImageUrl: event.target.value }))}
              />
            </label>
            <label className="field">
              <span>Credits To Use</span>
              <input
                type="number"
                min="1"
                value={jobForm.creditsToUse}
                onChange={(event) => setJobForm((current) => ({ ...current, creditsToUse: event.target.value }))}
              />
            </label>
            <button className="primary-button" type="submit">Create Image Job</button>
          </form>
          <pre className="output">{jobResult}</pre>
        </section>

        <section className="panel">
          <div className="panel-header">
            <h2>Lookup Image Job</h2>
            <p>Fetch the latest state of an image job by id.</p>
          </div>
          <form className="stack" onSubmit={handleLookup}>
            <label className="field">
              <span>Image Job ID</span>
              <input
                value={imageJobId}
                onChange={(event) => setImageJobId(event.target.value)}
                placeholder="Filled automatically after job creation"
                required
              />
            </label>
            <button className="secondary-button" type="submit">Fetch Job</button>
          </form>
          <pre className="output">{lookupResult}</pre>
        </section>
      </main>
    </div>
  );
}
