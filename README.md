# Viktoria (Виктория)

NeoForge **1.21.1** single-player companion mod with an extensible character framework. First character: **Вика**.

## Скачать мод

**[⬇ Скачать viktoria-0.1.4.jar](dist/viktoria-0.1.4.jar)**  
([raw с ветки](https://github.com/l7cor1s/project-victoria/raw/cursor/viktoria-companion-scaffold-3c34/dist/viktoria-0.1.4.jar) · также `dist/viktoria-0.1.3.jar`)

Положите файл в папку `mods` вместе с:
- NeoForge **1.21.1**
- [GeckoLib](https://modrinth.com/mod/geckolib) 4.7+ (обязательно)
- опционально [MCA Reborn](https://modrinth.com/mod/minecraft-comes-alive-reborn)

Призыв в игре: `/viktoria summon` или `/vika spawn`

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.x
- [GeckoLib](https://modrinth.com/mod/geckolib) 4.7+ for 1.21.1 NeoForge
- Optional: [MCA Reborn](https://modrinth.com/mod/minecraft-comes-alive-reborn) for town appearance

## Build

```bash
./gradlew build
```

Готовый jar также копируется в `dist/viktoria-0.1.4.jar` (см. секцию **Скачать мод**). Сборка из исходников: `build/libs/viktoria-0.1.4.jar`.

## Play / test (SP)

1. Install NeoForge 1.21.1 + GeckoLib + this mod (+ MCA optional).
2. Commands:
   - `/viktoria summon` or `/vika spawn` — spawn Виктория
   - Right-click — Russian dialogue GUI (mood-aware lines + gated choices)
   - Hold flower/cake and right-click — gift (+ flower quest progress)
   - Dialogue follow toggle or `/viktoria follow`
   - `/viktoria affinity <0-100>` — test intimate/home gates
   - `/viktoria home` / `/viktoria status`
3. Quests (real objectives):
   - Flowers: gift 3 flowers
   - Home: claim home via dialogue (affinity ≥20 to unlock quest)
   - Evening walk: follow at night (~400 ticks)
4. World: rare dark-oak cottage feature; MCA towns spawn her via soft-dep hooks when MCA is installed.

## Features

| System | Status |
|--------|--------|
| Character registry | Working |
| JSON dialogue loader + RU trees | Working |
| Affinity / mood reactions | Working |
| Gifts / follow + **gift JSON loader** | Working |
| Real quest objectives | Working |
| House Feature + jigsaw/NBT cottage | Working |
| MCA town soft-dep (building-aware) | Working (graceful without MCA) |
| Intimate poses (gated) | Working (`intimate` / `intimate_close` / `intimate_embrace`) |
| GeckoLib model toward reference | Layered outfit geo + 256² texture + `.bbmodel` |

### Intimate test
`/viktoria summon` → `/viktoria affinity 70` → dialogue **Близость…** → consent → pose choice.

## Extending

Register another `CompanionCharacter`; add `data/<ns>/dialogue/<id>.json` overlays; reuse `CompanionEntity` with a new `characterId`.
