[中文](README_zh.md)

# Rukkit X Project
![RukkitLogo](rukkit.png)
**Original Repository: https://github.com/RukkitDev/Rukkit**

[![](https://img.shields.io/badge/QQ_Group-751977820-red.svg)](http://qm.qq.com/cgi-bin/qm/qr?_wv=1027&k=ekpIy0vjVhGpCZ5cpszxP6vaR9nRIaFc&authKey=wtgSzxj7uZ7zk%2F4GO20B%2FWVXP9WcZMC7c2FMynjZkx8B%2BkntiSeybBZZ6O3g7p90&noverify=0&group_code=751977820)
[![](https://img.shields.io/badge/Discord-link-purple.svg)](https://discord.gg/JJJ6GST)

## What is Rukkit?
Rukkit is a third-party Rusted Warfare server that allows you to build hosting servers with an official-like experience. It offers better performance, more features, supports custom mods/maps and server plugins, and includes a game listening mechanism that enables you to expand your gaming experience or create new gameplay during matches. Built on the high-performance Netty framework, Rukkit theoretically delivers better performance than official servers/clients.

## About This Fork
This fork originated from **RukkitX** - a relatively independent third-party branch (not developed by RukkitDev, maintained by "UNCSYS (Micro)") with slightly ahead development progress compared to the main project, offering more cutting-edge features and bug fixes (Note: RukkitX does not represent RukkitDev's views or decisions).

**However**, after Micro joined RukkitDev, RukkitX was merged into the Rukkit Project and typically releases accumulated updates on the dev branch.

The actual situation might be more complex... but all you need to know is: **the dev branch is more cutting-edge than the main branch**.

## Current Progress
- [x] Basic gameplay (playable)
- [x] Custom maps
- [x] Game synchronization/player reconnection (official maps only)
- [x] Server plugin system
- [x] Metadata-based mod system (requires tool conversion for rwmod)
- [x] Support for more than 10 players (1.14+)
- [X] Relay mode (experimental)
- [ ] Non-stop mode (support join anytime, vote map change) (no plan)
- [ ] Game event mechanism (partially supported)
- [ ] Anti-cheat checks (requires game simulation layer, not feasible)
- [ ] Game command reconstruction (quite difficult and prone to breaking the game)

## About
The plugin system design draws inspiration from [Nukkit](https://github.com/Nukkit/Nukkit)

## Stability Warning
This version remains unstable. If you discover any bugs, please submit an issue or PR. Thank you for your support!

## Contributing
- Suggest desired features or report bugs in Issues
- Submit a pull request (PR)