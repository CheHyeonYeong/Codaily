const STORAGE_KEY = "codailySettings";
const ROOT_ID = "codaily-extension-root";

const PROVIDER_DEFAULTS = {
  gemini: {
    model: "gemini-3.5-flash",
    header: "X-Gemini-Api-Key",
    label: "Gemini"
  },
  openai: {
    model: "chat-latest",
    header: "X-OpenAI-Api-Key",
    label: "OpenAI"
  },
  anthropic: {
    model: "claude-sonnet-4-20250514",
    header: "X-Anthropic-Api-Key",
    label: "Anthropic"
  }
};

const defaults = {
  backendBaseUrl: "http://localhost:8080",
  aiProvider: "gemini",
  apiKey: "",
  aiModel: PROVIDER_DEFAULTS.gemini.model,
  focus: "Highlight what was learned and the next actions"
};

let lastHref = location.href;
let rootHost;
let elements;
let currentResultUrl = "";
let isSubmitting = false;

bootstrap();

function bootstrap() {
  ensureRoot();
  render();

  const observer = new MutationObserver(() => {
    if (location.href !== lastHref) {
      lastHref = location.href;
      render();
    }
  });

  observer.observe(document.documentElement, { childList: true, subtree: true });
  document.addEventListener("pjax:end", render);
  document.addEventListener("turbo:render", render);
  chrome.storage.onChanged.addListener((changes, areaName) => {
    if (areaName === "local" && changes[STORAGE_KEY]) {
      render();
    }
  });
}

function ensureRoot() {
  rootHost = document.getElementById(ROOT_ID);
  if (rootHost) {
    return;
  }

  rootHost = document.createElement("div");
  rootHost.id = ROOT_ID;
  document.body.appendChild(rootHost);

  const shadow = rootHost.attachShadow({ mode: "open" });
  shadow.innerHTML = `
    <style>
      :host {
        all: initial;
      }

      .shell {
        position: fixed;
        right: 24px;
        bottom: 24px;
        z-index: 2147483647;
        width: 320px;
        font-family: "Segoe UI", "Noto Sans", sans-serif;
        color: #201d17;
      }

      .panel {
        background: linear-gradient(180deg, #fffdf9 0%, #f4ede3 100%);
        border: 1px solid #d8d0c4;
        border-radius: 22px;
        box-shadow: 0 22px 48px rgba(32, 29, 23, 0.18);
        padding: 16px;
      }

      .hidden {
        display: none;
      }

      .eyebrow {
        display: inline-block;
        margin-bottom: 8px;
        padding: 5px 8px;
        border-radius: 999px;
        background: #d7f0ef;
        color: #115e59;
        font-size: 11px;
        font-weight: 800;
        letter-spacing: 0.08em;
        text-transform: uppercase;
      }

      .title {
        margin: 0 0 4px;
        font-size: 20px;
        line-height: 1.1;
        font-weight: 800;
      }

      .repo {
        margin: 0 0 10px;
        color: #6d655a;
        font-size: 13px;
      }

      .meta {
        margin: 0 0 14px;
        color: #6d655a;
        font-size: 12px;
        line-height: 1.5;
      }

      .button {
        width: 100%;
        border: 0;
        border-radius: 999px;
        padding: 12px 14px;
        background: #1f2937;
        color: white;
        font-size: 14px;
        font-weight: 800;
        cursor: pointer;
      }

      .button[disabled] {
        opacity: 0.65;
        cursor: wait;
      }

      .status {
        min-height: 18px;
        margin: 10px 2px 0;
        font-size: 12px;
        font-weight: 700;
      }

      .status.info {
        color: #155e75;
      }

      .status.error {
        color: #b42318;
      }

      .status.success {
        color: #166534;
      }

      .actions {
        display: flex;
        gap: 10px;
        margin-top: 12px;
      }

      .link-button {
        flex: 1;
        border: 1px solid #d8d0c4;
        border-radius: 999px;
        padding: 10px 12px;
        background: white;
        color: #201d17;
        font-size: 12px;
        font-weight: 800;
        cursor: pointer;
      }
    </style>
    <div class="shell hidden" id="shell">
      <section class="panel">
        <span class="eyebrow">Codaily</span>
        <h2 class="title">Generate retrospective</h2>
        <p class="repo" id="repo-name"></p>
        <p class="meta" id="meta"></p>
        <button class="button" id="generate-button" type="button">Generate with Codaily</button>
        <p class="status info" id="status"></p>
        <div class="actions hidden" id="result-actions">
          <button class="link-button" id="open-link" type="button">Open result</button>
          <button class="link-button" id="copy-link" type="button">Copy link</button>
        </div>
      </section>
    </div>
  `;

  elements = {
    shell: shadow.getElementById("shell"),
    repoName: shadow.getElementById("repo-name"),
    meta: shadow.getElementById("meta"),
    status: shadow.getElementById("status"),
    generateButton: shadow.getElementById("generate-button"),
    resultActions: shadow.getElementById("result-actions"),
    openLink: shadow.getElementById("open-link"),
    copyLink: shadow.getElementById("copy-link")
  };

  elements.generateButton.addEventListener("click", handleGenerate);
  elements.openLink.addEventListener("click", () => {
    if (currentResultUrl) {
      window.open(currentResultUrl, "_blank", "noopener,noreferrer");
    }
  });
  elements.copyLink.addEventListener("click", async () => {
    if (!currentResultUrl) {
      return;
    }

    await navigator.clipboard.writeText(currentResultUrl);
    setStatus("Share link copied.", "success");
  });
}

async function render() {
  const repo = parseRepoFromLocation(location.pathname);
  if (!repo) {
    elements.shell.classList.add("hidden");
    return;
  }

  const settings = await loadSettings();
  const providerDefaults = PROVIDER_DEFAULTS[settings.aiProvider] || PROVIDER_DEFAULTS.gemini;
  elements.shell.classList.remove("hidden");
  elements.repoName.textContent = `${repo.owner}/${repo.repo}`;
  elements.meta.textContent = settings.apiKey
    ? `${providerDefaults.label} key ready. Model: ${settings.aiModel || providerDefaults.model}`
    : `No ${providerDefaults.label} key saved. The backend will use its fallback key or template mode.`;

  if (!isSubmitting) {
    setStatus(`Backend: ${settings.backendBaseUrl || defaults.backendBaseUrl}`, "info");
  }
}

async function handleGenerate() {
  if (isSubmitting) {
    return;
  }

  const repo = parseRepoFromLocation(location.pathname);
  if (!repo) {
    setStatus("This is not a repository page.", "error");
    return;
  }

  const settings = await loadSettings();
  const providerDefaults = PROVIDER_DEFAULTS[settings.aiProvider] || PROVIDER_DEFAULTS.gemini;
  const backendBaseUrl = normalizeBaseUrl(settings.backendBaseUrl || defaults.backendBaseUrl);
  if (!backendBaseUrl) {
    setStatus("Set the backend URL in the extension popup first.", "error");
    return;
  }

  const headers = {
    "Content-Type": "application/json"
  };

  if (settings.apiKey) {
    headers[providerDefaults.header] = settings.apiKey;
  }

  const payload = {
    owner: repo.owner,
    repo: repo.repo,
    focus: settings.focus || "",
    aiProvider: settings.aiProvider || defaults.aiProvider,
    aiModel: settings.aiModel || providerDefaults.model
  };

  setSubmitting(true);
  hideResultActions();
  setStatus("Generating retrospective...", "info");

  try {
    const response = await fetch(`${backendBaseUrl}/api/retrospects`, {
      method: "POST",
      headers,
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      const message = await parseErrorMessage(response);
      throw new Error(message);
    }

    const data = await response.json();
    currentResultUrl = data.shareUrl || "";
    setStatus("Retrospective created.", "success");

    if (currentResultUrl) {
      showResultActions();
    }
  } catch (error) {
    setStatus(error.message || "Request failed.", "error");
  } finally {
    setSubmitting(false);
  }
}

async function loadSettings() {
  const stored = await chrome.storage.local.get(STORAGE_KEY);
  return {
    ...defaults,
    ...(stored[STORAGE_KEY] || {})
  };
}

function parseRepoFromLocation(pathname) {
  const segments = pathname.split("/").filter(Boolean);
  if (segments.length < 2) {
    return null;
  }

  const reserved = new Set([
    "about",
    "account",
    "codespaces",
    "collections",
    "contact",
    "customer-stories",
    "enterprise",
    "events",
    "explore",
    "features",
    "gist",
    "login",
    "marketplace",
    "new",
    "notifications",
    "orgs",
    "organizations",
    "pricing",
    "pulls",
    "search",
    "security",
    "settings",
    "signup",
    "site",
    "sponsors",
    "team",
    "topics",
    "trending",
    "users"
  ]);

  if (reserved.has(segments[0].toLowerCase())) {
    return null;
  }

  return {
    owner: segments[0],
    repo: segments[1]
  };
}

function normalizeBaseUrl(value) {
  return (value || "").trim().replace(/\/+$/, "");
}

async function parseErrorMessage(response) {
  const text = await response.text();
  if (!text) {
    return `${response.status} ${response.statusText}`;
  }

  try {
    const json = JSON.parse(text);
    return json.message || json.error || text;
  } catch {
    return text;
  }
}

function setSubmitting(submitting) {
  isSubmitting = submitting;
  elements.generateButton.disabled = submitting;
  elements.generateButton.textContent = submitting ? "Generating..." : "Generate with Codaily";
}

function setStatus(message, tone) {
  elements.status.textContent = message;
  elements.status.className = `status ${tone}`;
}

function showResultActions() {
  elements.resultActions.classList.remove("hidden");
}

function hideResultActions() {
  elements.resultActions.classList.add("hidden");
}
