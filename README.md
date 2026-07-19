# Mutualzz Voice

Talk in Mutualzz voice **from inside Minecraft**. Client mod — the server needs [Mutualzz Bridge](../mc-bridge).

Without this mod you can still `/mzvoice join` for presence in the Mutualzz app; you just won’t get in-game audio.

## License

Official builds only. Forks/PRs welcome (see [`CONTRIBUTING.md`](./CONTRIBUTING.md)). No redistributing jars without permission — see [`LICENSE`](./LICENSE).

## Supported versions

Use the jar that matches **your Minecraft version and loader**.

| Minecraft | Fabric | NeoForge | Forge |
|-----------|--------|----------|-------|
| 1.18.2 | yes | — | yes |
| 1.19.2 | yes | — | yes |
| 1.19.4 | yes | — | yes |
| 1.20.1 | yes | — | yes |
| 1.20.4 | yes | yes | — |
| 1.21.1 | yes | yes | — |
| 1.21.4 | yes | yes | — |
| 26.1.2 | yes | yes | — |

- **Fabric** — also need [Fabric API](https://modrinth.com/mod/fabric-api). On **26.1.2**, also [Amecs](https://modrinth.com/mod/amecs).
- **Server** — Mutualzz Bridge, account linked, voice room bound.

## Install

1. Install Fabric, NeoForge, or Forge for your Minecraft version.
2. Download the matching Mutualzz Voice jar from [Modrinth](https://modrinth.com) or the GitHub release `minecraft-v…` (same release as Bridge).
3. Put it in `mods/` (plus Fabric API / Amecs if needed).
4. Join a server with Mutualzz Bridge → `/mzlink` → `/mzvoice join` (optional room: `/mzvoice join lobby`). Leave with `/mzvoice leave`.

A green **Mutualzz Voice** label shows in the top-left when connected. Settings: **Alt+O** (`config/mutualzz_voice.json`).

## Controls

Rebind under **Options → Controls → Mutualzz Voice**. On NeoForge/Forge, some binds use **Shift** instead of **Alt+Shift** (loader limit — single modifier only).

| Key | Action |
|---|---|
| **Alt+O** | Voice settings |
| **Alt+M** | Mute mic |
| **Alt+Shift+D** / **Shift+D** | Deafen |
| **Alt+H** | Show / hide who’s talking |
| **Alt+I** | Voice activity ↔ push-to-talk |
| **V** (hold) | Talk (push-to-talk) |
| **Alt+=** / **Alt+-** | Output volume |
| **Alt+Shift+=/-** / **Shift+=/-** | Mic sensitivity |
| **Alt+[** / **]** | Select user |
| **Alt+↑** / **↓** | That user’s volume |
| **Alt+U** | Mute that user locally |

Noise suppression (RNNoise) is on by default; toggle in **Alt+O**.

## Build

```bash
./gradlew buildAll
```

Jars: `versions/<mc>/<loader>/build/libs/`.

## Troubleshooting

- **No HUD** — wrong jar for your MC/loader, or missing Fabric API/Amecs; server needs Bridge.
- **Can’t hear** — check volume in Alt+O; not deafened.
- **Nobody hears you** — unmute (Alt+M), raise mic sensitivity, or hold **V** in PTT.
