# База Знань (MyBrain)

Personal second-brain iOS застосунок. Швидке захоплення текстів, фото, голосових приміток. Markdown + Git як єдине джерело правди. AI-обробка делегується на workstation (Karpathy LLM Wiki pattern) — у Phase 1 ще не використовується.

> **Status:** Phase 1 skeleton (capture-only). Builds on iOS 18+ with Xcode 16+.

## Архітектурні принципи

- **Markdown + Git = source of truth.** Жодних пропрієтарних форматів, БД як SOT, чи cloud-only сервісів.
- **Локальна БД (SwiftData)** — лише як кеш/черга для офлайн-роботи. При невдалій синхронізації нотатка ніколи не губиться.
- **Privacy-first.** Дані ніколи не залишають вашого репозиторію (GitHub private) і вашої workstation.
- **Pluggable LLM.** Apple Foundation Models / Ollama / Claude API / NoOp — обирається в Settings.
- **iOS app — capture-фронт.** Не дублює Obsidian як редактор; повноцінне редагування на Mac.

## Поточний скоуп (Phase 1)

- ✅ Quick capture: текст / фото / голос (з on-device транскрипцією)
- ✅ Локальна SwiftData-черга на офлайн
- ✅ Запис в `raw/YYYY-MM-DD-HHMMSS-{slug}.md`
- ✅ Push на GitHub через REST API (Trees API — batched commit)
- ✅ Налаштування: vault repo, PAT, Speech locale
- ⏳ Browse останніх захоплень (read-only)
- ❌ FTS5 пошук — Phase 2
- ❌ Share Extension — Phase 2
- ❌ AI/LLM — Phase 3 (workstation)
- ❌ Граф звʼязків — Phase 4

## Швидкий старт

```bash
brew install xcodegen
cd MyBrain
xcodegen generate
open MyBrain.xcodeproj
```

В Xcode:
1. Target → Signing & Capabilities → виберіть team
2. Build для iOS 18+ симулятора або iPhone
3. У застосунку: Settings → введіть owner/repo + PAT свого vault-репозиторію

Повна інструкція: [`docs/SETUP-PHASE-0.md`](docs/SETUP-PHASE-0.md).

## Документація

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — детальна архітектура
- [`docs/ROADMAP.md`](docs/ROADMAP.md) — план Phase 1-4
- [`docs/SETUP-PHASE-0.md`](docs/SETUP-PHASE-0.md) — як підняти vault repo + Obsidian

## Tech stack

- **SwiftUI** + Swift 5.10
- **SwiftData** — локальна черга та метадані
- **AVFoundation** + **Speech** — запис і транскрипція
- **PhotosUI** — пошук фото
- **URLSession** — GitHub REST API (без libgit2 залежностей)
- **Keychain** — зберігання PAT
- **XcodeGen** — генерація .xcodeproj

## Ліцензія

Особистий проект. Ліцензію визначимо перед публічним релізом.
