# Phase 0 — Підняття фундаменту (без коду)

Ця фаза робиться **руками за вечір**. Після неї у вас вже працююча база знань (Obsidian + Working Copy), і застосунок з Phase 1 — це **upgrade** для швидкого захоплення на iPhone, а не блокатор.

## 1. Створити два приватні репозиторії на GitHub

| Repo | Зміст | Видимість |
|---|---|---|
| `baza-znan-vault` | Власне база знань (markdown файли) | **private** |
| `baza-znan` (або `MyBrain`) | Код iOS застосунку | private спочатку, кандидат на public |

```bash
# В GitHub web: New → Private → без README (його створимо локально)
```

## 2. Локальний клон vault репо на Mac

```bash
cd ~/Documents
git clone git@github.com:mclaut/baza-znan-vault.git
cd baza-znan-vault
```

Створіть структуру:

```bash
mkdir -p raw wiki sources assets .kb
touch raw/.gitkeep wiki/.gitkeep sources/.gitkeep assets/.gitkeep
cat > README.md <<'EOF'
# baza-znan — vault

Markdown файли — джерело правди для персональної бази знань.

- `raw/` — інбокс, нещодавно захоплене (через iOS, web clipper тощо)
- `wiki/` — LLM-compiled статті (Phase 3)
- `sources/` — оригінали з метаданими (Phase 3)
- `assets/` — фото, аудіо, бінарні вкладення
- `.kb/` — конфіг librarian
EOF

cat > .gitignore <<'EOF'
.DS_Store
.obsidian/workspace*
EOF

git add .
git commit -m "Initialize vault structure"
git push -u origin main
```

## 3. Obsidian на Mac (основний редактор)

1. Завантажити [Obsidian](https://obsidian.md).
2. **Open folder as vault** → виберіть `~/Documents/baza-znan-vault/`.
3. Settings → Files & Links → Default location for new notes = `raw/`.
4. Settings → Daily notes → enable, folder = `journals/` (опційно).
5. Community plugins: `Templater`, `Dataview` (бажано), `Backlinks` (вбудоване).

## 4. Working Copy на iPhone

1. App Store → **Working Copy** ($20 одноразово).
2. Clone `baza-znan-vault` через SSH key (Working Copy генерує).
3. Додайте SSH public key з Working Copy в GitHub → Settings → SSH keys.
4. Перевірте pull / push.

## 5. Obsidian Mobile на iPhone (опційно для перегляду)

1. Obsidian Mobile (безкоштовно).
2. **Open vault from Files** → виберіть папку клонована Working Copy.
3. Тепер можна читати/редагувати vault на телефоні.

## 6. Personal Access Token для iOS застосунку

Phase 1 застосунок використовує REST API GitHub. Потрібен **Fine-grained PAT**:

1. GitHub → Settings → Developer settings → Personal access tokens → Fine-grained tokens → Generate new token.
2. **Resource owner:** ви.
3. **Repository access:** Only select repositories → `baza-znan-vault`.
4. **Permissions:**
   - Contents: **Read and write**
   - Metadata: Read-only (вбудовано)
5. Expiration: 90 днів (поновлюватимете).
6. Скопіюйте token (показується один раз) — введете в Settings застосунку.

## 7. Перевірка

Після цих кроків:

- ✅ Можете писати/редагувати нотатки в Obsidian на Mac → commit → push
- ✅ З iPhone через Working Copy робите pull/push
- ✅ Vault репо містить нотатки які видно у GitHub web

**Phase 1 застосунок** додає до цього: швидке захоплення з iPhone без потреби відкривати Working Copy/Obsidian.
