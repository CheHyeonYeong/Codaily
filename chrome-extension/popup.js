const STORAGE_KEY = "codailySettings";

const PROVIDER_DEFAULTS = {
  gemini: {
    apiKeyPlaceholder: "AIza...",
    model: "gemini-3.5-flash",
    header: "X-Gemini-Api-Key",
    label: "Gemini API key"
  },
  openai: {
    apiKeyPlaceholder: "sk-...",
    model: "chat-latest",
    header: "X-OpenAI-Api-Key",
    label: "OpenAI API key"
  },
  anthropic: {
    apiKeyPlaceholder: "sk-ant-...",
    model: "claude-sonnet-4-20250514",
    header: "X-Anthropic-Api-Key",
    label: "Anthropic API key"
  }
};

const defaults = {
  backendBaseUrl: "http://localhost:8080",
  aiProvider: "gemini",
  apiKey: "",
  aiModel: PROVIDER_DEFAULTS.gemini.model,
  focus: "Highlight what was learned and the next actions"
};

const form = document.getElementById("settings-form");
const status = document.getElementById("status");
const clearKeyButton = document.getElementById("clear-key");
const apiKeyLabel = document.getElementById("apiKeyLabel");

init();

async function init() {
  const settings = await loadSettings();
  form.backendBaseUrl.value = settings.backendBaseUrl;
  form.aiProvider.value = settings.aiProvider;
  form.apiKey.value = settings.apiKey;
  form.aiModel.value = settings.aiModel;
  form.focus.value = settings.focus;
  applyProviderUi(settings.aiProvider, settings.aiModel);
}

form.aiProvider.addEventListener("change", () => {
  const currentModel = form.aiModel.value.trim();
  applyProviderUi(form.aiProvider.value, currentModel);
});

form.addEventListener("submit", async (event) => {
  event.preventDefault();

  const provider = form.aiProvider.value;
  const defaultsForProvider = PROVIDER_DEFAULTS[provider];
  const settings = {
    backendBaseUrl: normalizeBaseUrl(form.backendBaseUrl.value),
    aiProvider: provider,
    apiKey: form.apiKey.value.trim(),
    aiModel: form.aiModel.value.trim() || defaultsForProvider.model,
    focus: form.focus.value.trim()
  };

  await chrome.storage.local.set({ [STORAGE_KEY]: settings });
  setStatus("Saved.");
});

clearKeyButton.addEventListener("click", async () => {
  const settings = await loadSettings();
  settings.apiKey = "";
  form.apiKey.value = "";
  await chrome.storage.local.set({ [STORAGE_KEY]: settings });
  setStatus("API key removed.");
});

async function loadSettings() {
  const stored = await chrome.storage.local.get(STORAGE_KEY);
  return {
    ...defaults,
    ...(stored[STORAGE_KEY] || {})
  };
}

function applyProviderUi(provider, currentModel) {
  const providerDefaults = PROVIDER_DEFAULTS[provider] || PROVIDER_DEFAULTS.gemini;
  apiKeyLabel.textContent = providerDefaults.label;
  form.apiKey.placeholder = providerDefaults.apiKeyPlaceholder;
  if (!currentModel || Object.values(PROVIDER_DEFAULTS).some((item) => item.model === currentModel)) {
    form.aiModel.value = providerDefaults.model;
  }
}

function normalizeBaseUrl(value) {
  return value.trim().replace(/\/+$/, "");
}

function setStatus(message) {
  status.textContent = message;
  window.clearTimeout(setStatus.timeoutId);
  setStatus.timeoutId = window.setTimeout(() => {
    status.textContent = "";
  }, 2500);
}
