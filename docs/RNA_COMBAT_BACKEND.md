# RNA Combat Backend

Серверный слой боевых РНА-способностей реализован поверх существующего `RnaApi`.
GUI Кодекса, сканер, модели, текстуры и визуальная логика не изменялись.

## Источник правды

Профиль, характеристики, мета-износ и разрушение РНА по-прежнему принадлежат `RnaApi`.
Боевой слой отвечает только за способности:

```text
Riftborne
└── RNA
    ├── ... прежние поля РНА ...
    └── Combat
        ├── UnlockedAbilities
        ├── ActiveAbilities
        ├── Cooldowns
        ├── LastUseTicks
        ├── GrowthCooldowns
        └── Version
```

Сохранение обычного профиля РНА сохраняет вложенный `Combat`, поэтому старые операции
инициализации, изменения характеристик и мета-износа его не перетирают.

## Зарегистрированная способность

```text
id: riftborne:telekinesis
type: CONTROL
requires: active RNA + unlocked ability
heavy: false
```

Существующие формулы дальности, силы, связи и сопротивления перегрузке оставлены.

Действия:

| Действие | Базовый износ | Рост | Защита роста |
|---|---:|---|---:|
| Захват сущности/предмета | 1 | Connectivity +1 | 240 тиков |
| Структурный захват блока | 2 | NodeDensity +1 | 400 тиков |
| Силовой бросок | 2 | Throughput +1 | 240 тиков |

Успешное применение на стадии `STRAIN` и выше также может дать
`OverloadResistance +1`, но не чаще одного раза в 600 тиков для этой способности.

Рост происходит только после успешного осмысленного действия. Захват воздуха и
невалидной цели ничего не начисляет.

## Модификаторы стадий

| Стадия | Кулдаун | Износ | Тяжёлые способности | Сбой |
|---|---:|---:|---|---:|
| STABLE | x1.00 | x1.00 | разрешены | 0% |
| STRAIN | x1.15 | x1.10 | разрешены | 0% |
| DISTORTION | x1.35 | x1.20 | разрешены | 0% |
| REJECTION | x1.75 | x1.35 | запрещены | 0% |
| ARCHITECTURE_BREAK | x2.00 | x1.50 | запрещены | 0% |

Шанс сбоя подготовлен структурно, но в MVP равен нулю. Лёгкий телекинез на стадии
`ARCHITECTURE_BREAK` доходит до общего начисления износа, после чего существующая логика
`RnaApi` разрушает архитектуру и дальнейшее удержание прекращается.

## Миграция

При входе игрока или первом обращении к способностям:

1. Проверяется старый флаг `RiftborneTelekinesis`.
2. Если он установлен, `riftborne:telekinesis` добавляется в `UnlockedAbilities`.
3. Старый флаг удаляется.
4. Дальше доступ определяется только новой системой.

Старый класс команды телекинеза, если будет вызван из другого кода, также делегирует
выдачу и отзыв новому менеджеру. Отдельная ветка `/riftborne aspects` больше не регистрируется.

## Команды

Все основные команды находятся в `/riftborne rna`.

```mcfunction
/riftborne rna get [player]

/riftborne rna profile init <TRAINING|STRESS|ARTIFICIAL_BORN> [target]
/riftborne rna profile reset [target]
/riftborne rna profile path set <TRAINING|STRESS|ARTIFICIAL_BORN> [target]

/riftborne rna stats set <stat> <0-100> [target]
/riftborne rna stats add <stat> <amount> [source] [target]

/riftborne rna metawear get [target]
/riftborne rna metawear set <0-100> [target]
/riftborne rna metawear add <1-100> [source] [target]
/riftborne rna metawear clear [target]
/riftborne rna metawear collapse [target]

/riftborne rna abilities list [target]
/riftborne rna abilities grant <ability_id> [targets]
/riftborne rna abilities revoke <ability_id> [targets]
/riftborne rna abilities cooldown get <ability_id|all> [target]
/riftborne rna abilities cooldown clear <ability_id|all> [targets]
/riftborne rna abilities debug <ability_id> [target]
```

Старые `/riftborne rna init`, `/riftborne rna reset`, `/riftborne rna set` и
`/riftborne rna path set` оставлены как короткие aliases и вызывают ту же реализацию.

Отдельные корни `/riftborne metawear` и `/riftborne aspects` не регистрируются.

## Основные классы

- `rna.combat.RnaAbilityManager`
- `rna.combat.RnaCombatEvents`
- `rna.combat.ability.RnaAbility`
- `rna.combat.ability.RnaAbilityType`
- `rna.combat.data.RnaAbilityData`
- `rna.combat.data.RnaAbilityCost`
- `rna.combat.data.RnaAbilityUseContext`
- `rna.combat.data.RnaAbilityResult`
- `rna.combat.registry.RnaAbilityRegistry`
- `rna.combat.requirement.RnaAbilityRequirement`
- `rna.combat.scaling.RnaStageModifiers`
- `rna.combat.cooldown.RnaCooldown`

## Добавление будущей способности

1. Зарегистрировать `RnaAbility` в `RnaAbilityRegistry`.
2. Указать тип, требования, стоимость действий, кулдаун и признак тяжёлой способности.
3. Перед эффектом вызвать `RnaAbilityManager.checkUse`.
4. После успешного осмысленного эффекта вызвать `completeSuccessfulUse`.
5. Не начислять мета-износ или рост напрямую в реализации способности.
