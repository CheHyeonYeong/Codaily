# Codaily

Codaily is a Spring Boot MVP that collects GitHub activity, generates a retrospective, stores it, and exposes share and ranking pages.

## Current flow

- `GET /api/github/repositories/{owner}/{repo}/activity`
- `POST /api/retrospects`
- `GET /api/retrospects/{id}`
- `GET /api/rankings/retrospects`
- `GET /retrospects/{id}`
- `GET /rankings`

## Run

```powershell
.\gradlew.bat bootRun
```

Default mode is demo mode, so the app works without GitHub OAuth or any LLM provider.

## Hot reload

For development, use the `dev` profile and keep a continuous compile process running.

Terminal 1:

```powershell
$env:SPRING_PROFILES_ACTIVE='dev'
.\gradlew.bat bootRun
```

Terminal 2:

```powershell
.\gradlew.bat classes --continuous
```

What this gives you:

- Java changes: auto recompile in terminal 2, then Spring Boot DevTools restarts the app
- Thymeleaf / static resource changes: reflected faster because resources are served from source and caches are disabled in `application-dev.yml`

If you are already running `bootRun`, stop it with `Ctrl + C` and restart with the `dev` profile once.

## Supported AI providers

- `gemini`
- `openai`
- `anthropic`
- `template`

Provider aliases accepted by the backend:

- `chatgpt` -> `openai`
- `claude` -> `anthropic`
- `stub` -> `template`

## Request format

Use `aiProvider` in the JSON body and send the matching API key in the request header.

Headers:

- `X-Gemini-Api-Key`
- `X-OpenAI-Api-Key`
- `X-Anthropic-Api-Key`

Body example:

```json
{
  "owner": "openai",
  "repo": "openai",
  "focus": "Highlight what was learned and the next actions",
  "aiProvider": "openai",
  "aiModel": "chat-latest"
}
```

## Manual API examples

Gemini:

```powershell
$headers = @{
  "X-Gemini-Api-Key" = "YOUR_GEMINI_API_KEY"
}

Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/retrospects `
  -Headers $headers `
  -ContentType 'application/json' `
  -Body '{"owner":"google","repo":"gemini-cli","focus":"Summarize the main engineering progress","aiProvider":"gemini","aiModel":"gemini-3.5-flash"}'
```

OpenAI:

```powershell
$headers = @{
  "X-OpenAI-Api-Key" = "YOUR_OPENAI_API_KEY"
}

Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/retrospects `
  -Headers $headers `
  -ContentType 'application/json' `
  -Body '{"owner":"openai","repo":"openai","focus":"Highlight what was learned and the next actions","aiProvider":"openai","aiModel":"chat-latest"}'
```

Anthropic:

```powershell
$headers = @{
  "X-Anthropic-Api-Key" = "YOUR_ANTHROPIC_API_KEY"
}

Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/retrospects `
  -Headers $headers `
  -ContentType 'application/json' `
  -Body '{"owner":"anthropics","repo":"anthropic-sdk-typescript","focus":"Call out strengths, gaps, and next actions","aiProvider":"anthropic","aiModel":"claude-sonnet-4-20250514"}'
```

## Server-side fallback keys

You can also configure server-owned fallback keys:

```powershell
$env:AI_ENABLED='true'
$env:AI_PROVIDER='openai'
$env:AI_MODEL='chat-latest'
$env:OPENAI_API_KEY='YOUR_SERVER_KEY'
.\gradlew.bat bootRun
```

Request priority:

1. Request header key for the selected provider
2. Matching server env key
3. Template fallback when AI is not forced

## Env vars

See [.env.example](C:\Users\hyeonyeong\Desktop\prj\toy prj\.env.example).

Important ones:

- `GITHUB_DEMO`
- `GITHUB_PERSONAL_ACCESS_TOKEN`
- `AI_ENABLED`
- `AI_PROVIDER`
- `AI_MODEL`
- `GEMINI_API_BASE_URL`
- `GEMINI_API_KEY`
- `OPENAI_API_BASE_URL`
- `OPENAI_API_KEY`
- `ANTHROPIC_API_BASE_URL`
- `ANTHROPIC_API_KEY`
- `ANTHROPIC_VERSION`

## GitHub OAuth

```powershell
$env:SPRING_PROFILES_ACTIVE='oauth'
$env:GITHUB_CLIENT_ID='your-client-id'
$env:GITHUB_CLIENT_SECRET='your-client-secret'
.\gradlew.bat bootRun
```

## Chrome extension

Load the extension from [chrome-extension](</C:/Users/hyeonyeong/Desktop/prj/toy prj/chrome-extension>).

1. Open `chrome://extensions`
2. Turn on Developer mode
3. Click `Load unpacked`
4. Select the `chrome-extension` folder
5. Open the extension popup
6. Save the backend URL, provider, API key, model, and default focus
7. Open any GitHub repository page
8. Click `Generate with Codaily`

The popup stores the selected provider and API key in `chrome.storage.local`, and the content script sends the matching provider header to the backend.

## Provider notes

For OpenAI, the implementation uses the Responses API, which OpenAI recommends for new projects. OpenAI docs also note that `chat-latest` points to the latest instant model used in ChatGPT, while the broader models guide recommends `gpt-5.5` for many production API use cases. For Anthropic, the implementation uses `POST /v1/messages` with `x-api-key` and `anthropic-version`. These details are based on the official docs:

- https://platform.openai.com/docs/api-reference/responses/retrieve
- https://platform.openai.com/docs/guides/responses-vs-chat-completions
- https://developers.openai.com/api/docs/models
- https://developers.openai.com/api/docs/models/chat-latest
- https://docs.anthropic.com/en/api/messages-examples
- https://docs.anthropic.com/en/api/overview

## Notes

- For a browser extension, passing the key per request is safer than storing plaintext provider keys in the backend DB.
- If you later want persistent per-user keys, store them encrypted, not in plaintext.
