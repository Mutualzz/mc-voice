# Mutualzz Voice (client mod)

Hear and talk in Mutualzz voice **from inside Minecraft**.

This is a **Fabric client mod** for your PC. The server needs the official [Mutualzz Bridge](../mc-bridge) plugin, which connects to **Mutualzz’s hosted hub**.

Without this mod you can still `/mzvoice join` for presence in the Mutualzz app — you just won’t get in-game audio.

---

## License & redistribution

Source is available for transparency and **community contributions**. That is **not** permission to redistribute builds.

**You may:**

- Install official builds and use them with Mutualzz Bridge on a server linked to Mutualzz
- Fork the repo and open pull requests — contributors get credit (see [`CONTRIBUTING.md`](./CONTRIBUTING.md))

**You may not** (without written permission from Mutualzz):

- Re-upload or share general-purpose jars / unofficial builds
- Publish it on Modrinth, CurseForge, GitHub Releases, etc. for others to install
- Bundle it in public modpacks without permission

See [`LICENSE`](./LICENSE). Contact [mutualzz.com](https://mutualzz.com) for partnership rights.

---

## What you need

| | |
|---|---|
| Minecraft | **26.1.x** (same version as the server) |
| Loader | [Fabric Loader](https://fabricmc.net/use/) **0.19.3+** |
| Required mods | [Fabric API](https://modrinth.com/mod/fabric-api) and [Amecs](https://modrinth.com/mod/amecs) |
| Server | Official Mutualzz Bridge, your account linked, a voice room bound in Mutualzz |

---

## Install (players)

1. Install a **Fabric** profile for Minecraft **26.1.x** (use the [Fabric installer](https://fabricmc.net/use/) or a launcher that supports Fabric).
2. Download these jars (matching your Minecraft version):
   - **Fabric API** — [Modrinth](https://modrinth.com/mod/fabric-api)
   - **Amecs** — [Modrinth](https://modrinth.com/mod/amecs)
   - **Mutualzz Voice** — official `mutualzz-voice-1.0.0.jar` from Mutualzz (not third-party mirrors)
3. Put **all three** jars in your Minecraft `mods` folder.
   - Prism / MultiMC / CurseForge: open the instance → Mods folder  
   - Vanilla launcher: `.minecraft/mods`
4. Launch Minecraft with the Fabric profile.
5. Join a server that has Mutualzz Bridge.
6. Link your account if you haven’t: `/mzlink` (follow the code in Mutualzz settings).
7. Join voice: `/mzvoice join`  
   Optional room name: `/mzvoice join lobby` (tab-complete shows rooms bound on that server).  
   Leave with `/mzvoice leave`.

When you’re connected, a green **Mutualzz Voice** label appears in the top-left of the screen.

---

## Controls

Open the settings screen anytime with **Ctrl+O** (volume, mic, PTT, mute, per-user).

You can also use shortcuts (rebind under **Options → Controls → Mutualzz Voice**):

| Key | Action |
|---|---|
| **Ctrl+O** | Voice settings |
| **Ctrl+M** | Mute mic |
| **Ctrl+D** | Deafen |
| **Ctrl+H** | Show / hide who’s talking |
| **Ctrl+I** | Switch voice activity ↔ push-to-talk |
| **V** (hold) | Talk in push-to-talk mode |
| **Ctrl+=** / **Ctrl+-** | Output volume up / down |
| **Ctrl+Shift+=** / **Ctrl+Shift+-** | Mic sensitivity up / down |
| **Ctrl+[** / **Ctrl+]** | Select previous / next user |
| **Ctrl+↑** / **Ctrl+↓** | That user’s volume |
| **Ctrl+U** | Mute that user locally |

Settings are saved in `config/mutualzz_voice.json`.

---

## Troubleshooting

- **No green HUD after `/mzvoice join`** — Confirm Fabric API + Amecs are loaded, and the server has Mutualzz Bridge online.
- **Joined but can’t hear anyone** — Check output volume in **Ctrl+O**, and that you’re not deafened (**Ctrl+D**).
- **Nobody can hear you** — Unmute (**Ctrl+M**), raise mic sensitivity, or switch to push-to-talk and hold **V**.
- **Kicked / left and it keeps reconnecting** — Update to the latest official mod; session ends cleanly when you’re removed from the channel.
- **Wrong Minecraft version** — This mod only supports **26.1.x**.
