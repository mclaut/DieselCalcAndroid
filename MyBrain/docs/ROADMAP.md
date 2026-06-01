# Roadmap

## Phase 0 — Фундамент (без коду)

- [ ] Створити `baza-znan-vault` приватний repo
- [ ] Створити `baza-znan` (або MyBrain) repo для коду застосунку
- [ ] Клонувати vault на Mac, налаштувати Obsidian
- [ ] Working Copy на iPhone, клон vault
- [ ] Fine-grained PAT для застосунку

Деталі: [`SETUP-PHASE-0.md`](SETUP-PHASE-0.md).

## Phase 1 — iOS capture v0.1 (поточна)

- [x] XcodeGen проєкт-скаффолд
- [x] SwiftData модель `PendingNote` (черга + кеш)
- [x] `VaultStore` orchestrator: capture → файли + PendingNote
- [x] `Filenames`, `Frontmatter`, `NoteFile` value types
- [x] `VoiceRecorder` (AVAudioRecorder) + `SpeechTranscriber` (on-device)
- [x] `QuickCaptureView` з текст/фото/голос
- [x] `RecentCapturesView` для перегляду pending/synced
- [x] `SettingsView` з repo config, PAT (Keychain), verify/push/retry
- [x] `GitHubClient` через REST API (Trees, batched commits)
- [x] `SyncCoordinator` як `@ModelActor`
- [x] `LLMProvider` protocol (NoOp impl)
- [x] Unit тести для Filenames і Frontmatter
- [ ] **Smoke test на симуляторі + реальному iPhone**
- [ ] **Перший справжній commit з застосунку у vault repo**

### Open для Phase 1
- Slug latinization для українських/російських символів (зараз залишається порожнім).
- Конфлікт при одночасних capture в ту саму секунду (нинішній контракт — однакові filename, треба додати UUID-суфікс).
- Авто-sync при app background (BGTaskScheduler).

## Phase 1.5 — Безпека

- [ ] **age-шифрування** raw/ файлів у застосунку перед commit
- [ ] Per-vault keypair у Secure Enclave / Keychain
- [ ] CLI tool на Mac для розшифровки + автоматичний git filter

## Phase 2 — Сила (на iPhone)

- [ ] **FTS5 SQLite** індекс над markdown в `Documents/Vault/`
- [ ] **Search tab** з instant пошуком, фільтрами за тегами
- [ ] **Share Extension** — Share Sheet → write to raw/
- [ ] **Spotlight integration** — Core Spotlight
- [ ] **Background sync** — BGTaskScheduler
- [ ] **Merge на pull** — простий fast-forward або UI для рішення
- [ ] **Покращений editor** — markdown live preview, inline images
- [ ] **Multi-vault** — підтримка декількох vaults (особистий / робочі)

## Phase 3 — AI Pipeline (workstation)

Коли приїде workstation:

- [ ] Watcher: моніторить `raw/` у клонованому vault на workstation
- [ ] **Fetcher** — резолвить URLs (Jina Reader), YouTube transcripts
- [ ] **Enricher** — TL;DR (локальна LLM), embeddings (BGE-M3)
- [ ] **Classifier** — local LLM → {project, tags, type}
- [ ] **Linker** — vector search → top-20 → LLM пропонує backlinks
- [ ] **Filer** — commit з осмисленим message, перенесення raw → sources
- [ ] **Librarian agent** — Karpathy pattern: будує/підтримує `wiki/index.md`
- [ ] **Trust gradient** — auto-accept threshold 1.0 → 0.9 за тижні
- [ ] iOS LLM imp lementations: `AppleFoundationModelsProvider`, `OllamaProvider`

## Phase 4 — Розширення

- [ ] **Граф звʼязків** — SwiftUI Canvas / Metal візуалізація
- [ ] **Соцмережі ingestion** — ScrapeCreators API на workstation, JSON → raw/
- [ ] **YouTube** — youtube-transcript-api на workstation
- [ ] **Web clipper** — Safari Extension
- [ ] **Telegram bot** — пересилаєш повідомлення → workstation парсить → raw/
- [ ] **Widgets** — recent captures, quick note from home screen
- [ ] **Apple Watch capture** — голос на годиннику
- [ ] **Shortcuts integration** — повний Capture API для iOS Shortcuts
- [ ] **Daily/Weekly digest** — LLM генерує підсумок на основі activity

## Поза скоупом (поки)

- Multi-user / shared vaults
- Real-time collaboration
- Web версія
- Android (ймовірно через окремий код, не cross-platform)
