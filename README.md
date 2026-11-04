<p align="center">
  <img src="https://raw.githubusercontent.com/SlimefunNewHorizons/S-PlayerWarps-Drake/main/banner.svg" width="100%" alt="S PLAYER WARPS animated banner" />
</p>

# About

> ### 🏰 ¡Únete a la Comunidad Oficial de DrakesCraft!
> 
> * 🎮 **IP del Servidor**: `mc.drakescraft.cl` *(Java 1.21.11 & Bedrock)*
> * 💬 **Discord Oficial**: [discord.gg/drakescraft](https://discord.gg/rv3vtXZTk7)
> * 🌐 **Web & Guía**: [web.drakescraft.cl](https://web.drakescraft.cl) — 🛒 **Tienda**: [web.drakescraft.cl/store](https://web.drakescraft.cl/store.html)
> 
> *¡Juega con este addon y más de 80 expansiones optimizadas en vivo en nuestra network de supervivencia técnica!*

---

> [!NOTE]
> **Fork de mantenimiento de SlimefunNewHorizons.** Basado en
> `Losterixx/S-PlayerWarps` para preservar correcciones de iconos inválidos y
> persistencia de warps en DrakesCraft. El upstream se conserva como referencia;
> cualquier JAR requiere validación antes de producción.
S-PlayerWarps is a simple Paper plugin that lets players create personal warps and teleport to them.

## Features
- Reload the plugin directly in-game.
- Teleport with optional delay and cancel-on-move.
- Customizable messages (english.yml & german.yml, you can add more).
- Sound effects for teleportation events.
- Cancel teleportation due to movement.
- Configurable GUIs.
- Customizable player warps (displayname, material).
- Tracking visits and unique visits.
- More features in development ...

## Requirements
- Java 21 or higher
- Paper server (1.21.8+)

## Modality isolation (ticket #569, pending runtime validation)

Warp menus, teleport suggestions and teleport execution use the destination
world to determine its modality. Existing warp records do not need migration.
Denied worlds take precedence over configured groups, including during the
teleport countdown. The existing staff bypass permission is unchanged.

Set `modalities.isolate-onechunk: true` in the deployment configuration to keep
OneChunk separate from Survival; the default `false` preserves the previous
behaviour. Custom world names can be assigned through `modalities.groups`.
This patch is source work until its build and runtime checks are recorded;
it does not establish Paper 26.x compatibility by itself.

Regression tests: `./gradlew test` (via `build_seguro.sh` on Star).

## Documentation
GitBook docu: [Link](https://s-projects.gitbook.io/s-projects/s-playerwarps/home)

## Download
- **GitHub Releases:** [Link](https://github.com/Losterixx/S-PlayerWarps/releases)
- **Modrinth:** [Link](https://modrinth.com/plugin/s-playerwarps)

## Support
My Discord-Server: [S-Projects](https://discord.gg/gnGsUAwp76)

---

## 📄 License & Upstream Attribution

This project is a sovereign fork maintained by [**JackStar6677-1**](https://github.com/JackStar6677-1) under [**DrakesCraft Labs**](https://github.com/SlimefunNewHorizons).

- **Original Project:** Created by the upstream authors and the open-source community.
- **DrakesCraft Optimizations:** Modernized for Paper/Purpur 1.21.11+, Java 21, high concurrency, asynchronous safety, and exploit/duplication prevention.
- **License:** Distributed under the original **GNU General Public License v3.0 (GPLv3)** (or original upstream license). See the [LICENSE](LICENSE) file for complete terms.
