# Наполнение Инфобазы

Инфобаза ноутбука полностью собирается из отдельных JSON-файлов в:

`src/main/resources/data/riftborne/codex_entries/`

Один файл описывает одну статью. Java-код для добавления новой статьи менять не нужно.

## Быстрое добавление

1. Скопируйте любой существующий JSON из папки `codex_entries`.
2. Дайте файлу уникальное имя латиницей, например `unstable_crystal.json`.
3. Замените `id`, заголовок, раздел и содержимое.
4. Выполните `/reload` и переоткройте ноутбук.

Перед запуском можно проверить все статьи командой:

`python tools/validate_infobase.py`

## Полный шаблон

```json
{
  "id": "riftborne:unstable_crystal",
  "title": "Нестабильный кристалл",
  "category": "ITEMS",
  "state": "LOCKED",
  "summary": "Короткое описание для списка и карточки статьи.",
  "fullContent": "Основной вводный текст статьи.",
  "sourceType": "SCAN",
  "flags": ["field_data", "dangerous"],
  "metadata": {
    "origin": "unknown",
    "classification": "material"
  },
  "requirements": ["scan:unstable_crystal"],
  "threat": 3,
  "order": 20,
  "tags": ["кристалл", "материал", "аномалия"],
  "related": [
    "riftborne:rift_basic"
  ],
  "sections": [
    {
      "heading": "Поведение",
      "body": "Текст отдельной главы. Глав можно добавить сколько угодно."
    },
    {
      "heading": "Рекомендации",
      "body": "Практические рекомендации для игрока."
    }
  ],
  "decryptData": {
    "type": "NONE",
    "requiredFragments": 0,
    "successState": "UNLOCKED"
  }
}
```

## Разделы

Допустимые значения `category`:

- `RIFTS` — явления и разломы;
- `DIMENSIONS` — пространства и межпространства;
- `MOBS` — сущности;
- `RNA` — РНА;
- `ASPECTS` — аспекты;
- `TECHNIQUES` — архив наблюдений за техниками;
- `ITEMS` — материалы и предметы;
- `DEVICES` — устройства;
- `SIGNALS` — сигналы;
- `FIELD_ARCHIVE` — полевые записи;
- `ARCHIVE` — архив;
- `SYSTEM` — системные материалы.

Раздел появляется в интерфейсе автоматически, когда в нём существует хотя бы одна статья.

## Состояния

`state` определяет начальное состояние статьи:

- `LOCKED` — неизвестная запись;
- `PARTIAL` — частично изучена;
- `UNLOCKED` — полностью доступна;
- `DAMAGED` — повреждена;
- `ENCRYPTED` — зашифрована;
- `NEEDS_DECRYPTION` — требует восстановления.

## Важные поля

- `threat` — опасность от `0` до `5`.
- `order` — порядок статьи внутри раздела; меньшее число отображается выше.
- `tags` — участвуют в поиске.
- `related` — идентификаторы кликабельных связанных статей.
- `sections` — главы статьи. Поля `heading` и `body` можно писать обычным текстом.
- `metadata` — короткие технические пары «ключ: значение» в карточке данных.
- `requirements` — игровые условия получения записи; сама Инфобаза только отображает данные.

Минимально обязательны `title` и `category`. Остальные поля имеют безопасные значения по умолчанию.

## Добавление цели для Полевого регистратора

Правила сканирования лежат отдельно:

`src/main/resources/data/riftborne/codex_scan_targets/`

Для нового моба достаточно создать статью Инфобазы и добавить такой файл:

```json
{
  "entry": "riftborne:my_creature",
  "type": "ENTITY",
  "targets": ["riftborne:my_creature"],
  "priority": 100
}
```

Для блока используется `"type": "BLOCK"`:

```json
{
  "entry": "riftborne:unstable_crystal",
  "type": "BLOCK",
  "targets": [
    "riftborne:unstable_crystal",
    "#riftborne:unstable_crystals"
  ],
  "damaged": false,
  "priority": 50
}
```

- `entry` — идентификатор существующей статьи Инфобазы.
- `type` — `BLOCK` или `ENTITY`.
- `targets` — конкретные ID и/или теги с префиксом `#`.
- `damaged` — сохранять результат как повреждённые данные.
- `priority` — какое правило выбрать, если цель подходит сразу под несколько правил.

После добавления выполните `/reload`. Java-код сканера менять не требуется.
