# Полевой ноутбук «Кодекс»

Основной редактируемый файл:

- `bbmodels/Codex_Field_Laptop.bbmodel`

Связанные игровые ресурсы:

- `assets/riftborne/geo/codex_laptop.geo.json`
- `assets/riftborne/animations/codex_laptop.animation.json`
- `assets/riftborne/textures/block/codex_laptop.png`
- `assets/riftborne/textures/block/codex_laptop_glowmask.png`

## Кости

- `root` — общий корень модели.
- `base` — защищённый нижний корпус.
- `keyboard` — клавиатура, тачпад, кнопки и индикаторы.
- `hinges` — шарниры и центральная ось.
- `details` — порты, вентиляция, защёлки, ручка и антенна.
- `lid` — вся крышка; pivot расположен точно на оси шарниров.
- `screen_glow` — экран и светящиеся индикаторы крышки.

Крышку следует анимировать только через кость `lid`. Переносить её pivot не нужно: текущая точка `[0, 3, 5.25]` совпадает с осью шарниров.

## Анимации

- `open` — открытие из закрытого положения.
- `close` — закрытие.
- `working` — спокойная пульсация работающего экрана.

Игровой рендер при загрузке проигрывает `animation.codex_laptop.open`, затем удерживает открытое положение. Анимация закрытия уже экспортирована и готова для будущего вызова через GeckoLib.

## Повторная генерация

Если структура модели меняется через генератор, запустить:

```powershell
& 'C:\Users\PR1TCHA\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' tools\generate_codex_laptop_assets.py
```

Ручные правки, сделанные только внутри `.bbmodel`, генератор перезапишет. После художественной доводки в Blockbench лучше экспортировать `geo`, `animation` и текстуры напрямую поверх игровых ресурсов.
