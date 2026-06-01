# Архітектура — База Знань

## Принципи

1. **Markdown + Git як єдине джерело правди.** Локальна БД (SwiftData) лише як кеш/черга. Якщо локальна БД зникне — всі нотатки відновлюються з Git.
2. **Capture > read > edit.** iOS-фронт оптимізований під швидке захоплення. Повноцінне редагування — на Mac в Obsidian.
3. **Privacy-first.** Жодних третіх стореджів з даними. PAT у Keychain. Транскрипція on-device.
4. **Pluggable AI.** LLMProvider — protocol. У Phase 1 — NoOp. У Phase 2-3 додаються імплементації.
5. **Без сторонніх iOS залежностей у Phase 1.** GitHub REST API через URLSession; жодного libgit2/SwiftGit2.

## Шари

```
┌──────────────────────────────────────────────────────┐
│ UI (SwiftUI Views)                                   │
│   QuickCaptureView | RecentCapturesView | Settings   │
└──────────────┬───────────────────────────────────────┘
               │
┌──────────────▼───────────────────────────────────────┐
│ Vault (orchestration)                                │
│   VaultStore — capture draft → файли + PendingNote   │
│   VaultLocation — де лежить Documents/Vault/         │
│   NoteFile / Frontmatter / Filenames — pure types    │
└──────────────┬───────────────────────────────────────┘
               │
┌──────────────▼───────────────────────────────────────┐
│ Local storage                                        │
│   SwiftData: PendingNote (queue + cache)             │
│   FileSystem: Documents/Vault/raw|assets|wiki/...    │
└──────────────┬───────────────────────────────────────┘
               │
┌──────────────▼───────────────────────────────────────┐
│ Sync                                                 │
│   SyncCoordinator (@ModelActor) — drain pending      │
│   GitHubClient — REST Trees API, batched commits     │
│   CredentialsStore — Keychain PAT                    │
└──────────────┬───────────────────────────────────────┘
               │ HTTPS
               ▼
        GitHub vault repo (приватний)
```

## Capture flow

```
QuickCaptureView
  ├─ TextEditor → body: String
  ├─ PhotosPicker → [Data] (photos)
  └─ VoiceRecorder + SpeechTranscriber → (audioURL, transcript)
        │
        ▼ Save
  VaultStore.save(draft)
    1. assets/<YYYY>/<MM>/<DD>/<uuid>.{jpg|m4a} ← write binaries
    2. raw/<timestamp>-<slug>.md               ← write markdown with FM
    3. context.insert(PendingNote(...))         ← enqueue for sync
```

## Sync flow

```
Settings → "Push зараз" або фоновий тригер
  │
  ▼
SyncCoordinator (ModelActor)
  ├─ fetch PendingNote where status = pending
  ├─ mark them syncing, save
  ├─ build [FileBlob] = markdown + binary attachments
  ├─ GitHubClient.commitBatch(files, message)
  │    ├─ GET branch ref → commit SHA
  │    ├─ GET commit → tree SHA
  │    ├─ POST blob (per file)
  │    ├─ POST tree (base_tree + new entries)
  │    ├─ POST commit
  │    └─ PATCH ref
  └─ mark them synced, save
```

При невдачі — status → failed, retryCount++, lastError записаний. UI має кнопку "Retry failed".

## Чому REST API а не libgit2

| Підхід | Плюси | Мінуси |
|---|---|---|
| **REST API через URLSession** | Zero deps. Атомарні commits через Trees API. Без libgit2 крос-компіляції. | Не вміє реального merge. Конфлікти треба обробляти вручну (Phase 2). Залежить від GitHub uptime. |
| SwiftGit2 / libgit2 | Повний git протокол. Локальні merge. Works offline. | iOS sandbox + obj-c bridging. Заморожений maintainance. Багато overhead. |
| Working Copy URL scheme | Делегує всю складність. | Потребує що користувач купив Working Copy. Не контролюємо UX. |

**Phase 1 ставка:** REST API. Якщо в Phase 2 потрібен справжній git (offline pull, merge) — переходимо на libgit2.

## Локальний кеш — навіщо

iPhone в літаку, в метро, в селі без LTE. Користувач захоплює 5 нотаток. Якщо БД немає — нотатки треба було б тримати в файлах і парсити при запуску. SwiftData дає індексовану чергу:
- статус (pending / syncing / synced / failed) — для UI
- retryCount, lastError — для resilience
- createdAt index — для сортування

При synced нотатки **залишаються** в БД для browse, але джерело правди — це файл на диску і в Git.

## Pluggable LLM

Protocol `LLMProvider`. У Phase 1 — `NoOpLLMProvider`. Майбутні:
- `AppleFoundationModelsProvider` (iOS 18+ on-device) — для класифікації, тегів, summary
- `OllamaProvider` (HTTP до workstation через Tailscale) — для batch обробки raw/
- `ClaudeAPIProvider` (з `anthropic-beta: zero-retention`) — для високоякісних linker запитів

User обирає активний в Settings. Capture stage ніколи не блокується на LLM — enrichment асинхронний.

## Що в Phase 1 свідомо НЕ зроблено

- **age-шифрування** — додамо Phase 1.5. Зараз надія на приватність репо.
- **Real merge / conflict resolution** — Phase 2.
- **FTS5 повнотекстовий пошук** — Phase 2.
- **Share Extension** — Phase 2.
- **Spotlight** — Phase 2.
- **Граф звʼязків** — Phase 4.
- **Соцмережі ingestion** — Phase 4 (workstation-side).
