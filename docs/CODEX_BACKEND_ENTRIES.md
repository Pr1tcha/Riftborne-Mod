# Записи бэкенда Кодекса

Все записи лежат в:

`src/main/resources/data/riftborne/codex_entries/`

Чтобы добавить запись без изменения Java-кода:

1. Скопируйте любой JSON-файл из этой папки.
2. Переименуйте файл латиницей, например `my_signal.json`.
3. Обязательно задайте новый уникальный `id`, например `riftborne:my_signal`.
4. Измените текст и нужные параметры.
5. В игре выполните `/reload`.
6. Проверьте запись командой `/riftborne codex status riftborne:my_signal`.

## Шаблон

```json
{
  "id": "riftborne:my_entry",
  "title": "Название записи",
  "category": "ARCHIVE",
  "state": "LOCKED",
  "summary": "Краткое описание.",
  "fullContent": "Полный текст записи.",
  "sourceType": "OTHER",
  "flags": ["lore"],
  "metadata": {
    "author": "Имя",
    "threat": "1"
  },
  "requirements": [],
  "decryptData": {
    "type": "NONE",
    "requiredFragments": 0,
    "successState": "UNLOCKED"
  }
}
```

## Допустимые значения

- `category`: `RIFTS`, `MOBS`, `DIMENSIONS`, `RNA`, `ITEMS`, `SIGNALS`, `ARCHIVE`, `SYSTEM`.
- `state`: `LOCKED`, `PARTIAL`, `UNLOCKED`, `DAMAGED`, `ENCRYPTED`, `NEEDS_DECRYPTION`.
- `sourceType`: `SCAN`, `SIGNAL`, `FLASH_DRIVE`, `AUTO_UNLOCK`, `QUEST`, `SYSTEM`, `OTHER`.
- `decryptData.type`: `NONE`, `FRAGMENTED`, `CORRUPTED`, `ENCRYPTED`, `UNKNOWN`.

`flags`, `metadata`, `requirements` и `decryptData` можно не указывать.

## Команды

- `/riftborne codex list`
- `/riftborne codex status <id>`
- `/riftborne codex unlock <id>`
- `/riftborne codex lock <id>`
- `/riftborne codex set_state <id> <state>`
- `/riftborne codex reset`
- `/riftborne codex give_fragment <id> [amount]`
- `/riftborne codex decrypt start <id>`
- `/riftborne codex decrypt complete <id>`

Команды изменения данных требуют права оператора уровня 2.

## Datapack

Записи можно добавлять и отдельным datapack. Путь внутри datapack:

`data/<namespace>/codex_entries/<имя>.json`

После добавления или изменения файлов достаточно выполнить `/reload`.
