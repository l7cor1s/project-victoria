# Viktoria (Виктория)

NeoForge **1.21.1** single-player companion mod with an extensible character framework. First character: **Вика**.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.x
- [GeckoLib](https://modrinth.com/mod/geckolib) 4.7+ for 1.21.1 NeoForge

## Build

```bash
./gradlew build
```

Jar: `build/libs/viktoria-0.1.0.jar`

## Play / test (SP)

1. Install NeoForge 1.21.1 + GeckoLib + this mod.
2. In a world, run:
   - `/viktoria summon` or `/vika spawn` — spawn Виктория
   - Right-click her — Russian dialogue GUI
   - Hold an item and right-click — gift (flowers/cake loved; rotten flesh hated)
   - Dialogue **Иди / останься** or `/viktoria follow` — follow toggle
   - `/viktoria affinity <0-100>` — set relationship for testing intimate/home gates
   - `/viktoria home` — claim home at your position
   - `/viktoria status` — affinity / mood

## Features (slice)

| System | Status |
|--------|--------|
| Character registry (`CompanionCharacter`) | Working |
| Viktoria entity + GeckoLib model/anims | Working (stylized placeholder toward reference) |
| Summon commands | Working |
| Dialogue GUI (RU + EN keys) | Working |
| Affinity / mood persistence (player attachment) | Working |
| Gifts / follow / reactions | Working |
| Home claim + live-together | Working (hooks) |
| Quests | Working (simple chain auto-progress) |
| Intimate gate (tier + consent) | Working (pose hook) |
| Village chance spawn | Working |
| MCA town spawn | Stub (optional soft-dep) |
| Custom house structure | Stub / future |

## Extending

Register another `CompanionCharacter` in `CharacterRegistry` during common setup; reuse `CompanionEntity` with a different `characterId`, dialogue tree map, gift profile, and geo assets.
