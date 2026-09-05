# SurvivalIsland

[![Build and Release](https://github.com/Glatinis/SurvivalIsland/actions/workflows/release.yml/badge.svg)](https://github.com/Glatinis/SurvivalIsland/actions/workflows/release.yml)
[![Latest Release](https://img.shields.io/github/v/release/Glatinis/SurvivalIsland)](https://github.com/Glatinis/SurvivalIsland/releases)
![Paper API](https://img.shields.io/badge/Paper%20API-26.2-blue)
![Java](https://img.shields.io/badge/Java-25-orange)
![WorldGuard](https://img.shields.io/badge/WorldGuard-7.0.17-green)

A Paper plugin for running a multi-island survival show: a lives based elimination scoreboard, gift triggered effects, contestant to island management, and mob and world containment built on top of WorldGuard and WorldEdit.

## Requirements

- Paper 26.2 or newer
- Java 25
- WorldGuard 7.0.17 (required)
- WorldEdit 7.3.18 or newer (required by WorldGuard)

## Installation

1. Install WorldGuard and WorldEdit on the server.
2. Drop the built `SurvivalIsland-<version>.jar` into the `plugins` folder.
3. Start the server once to generate `config.yml` and `contestants.yml`.
4. Define `island1`, `island2`, `island3`, a `safe-tower` region, and an arena region with WorldGuard, then adjust `config.yml` to match.

## Commands

All commands live under `/survivalisland` (alias `/si`) and require the `survivalisland.admin` permission unless noted otherwise.

### Contestant

| Command | Description |
| --- | --- |
| `/survivalisland contestant add <player> <island1\|island2\|island3>` | Assigns a player to an island. Fails if the island is already taken. |
| `/survivalisland contestant remove <player>` | Clears a player's island assignment. |
| `/survivalisland contestant list` | Lists every current contestant to island assignment. |

### Rule

| Command | Description |
| --- | --- |
| `/survivalisland rule pvp <on\|off>` | Turns global PvP on or off. |
| `/survivalisland rule pvp buy <player>` | Lets one player fight even while PvP is off. |
| `/survivalisland rule theft <on\|off>` | Allows or blocks opening containers outside a player's own island. |
| `/survivalisland rule protection <locked\|own\|free>` | Sets the island breaking mode: nobody, owners only, or anybody. The safe tower is always protected and TNT always breaks blocks except there. |

### Event

| Command | Description |
| --- | --- |
| `/survivalisland event acidrain <player\|all> <start\|stop>` | Personal acid rainstorm: damages the target and nearby island animals, applies poison. |
| `/survivalisland event acidocean <player\|all> <start\|stop>` | Water becomes lethal like lava (without fire) plus poison for the target. |
| `/survivalisland event deepfreeze <on\|off>` | Converts water to ice in the configured area and slows every player for the configured duration. |
| `/survivalisland event enderdragon destruction <on\|off>` | Allows or blocks ender dragons from destroying blocks. |

### Spawn

| Command | Description |
| --- | --- |
| `/survivalisland spawn <mobtype> <island\|player> [amount]` | Spawns mobs that target the resolved player and stay confined to their island. |

### Command Mode

Requires the `survivalisland.commandmode` permission.

| Command | Description |
| --- | --- |
| `/survivalisland commandmode start` | Chat messages are read as triggers instead of being sent publicly. |
| `/survivalisland commandmode stop` | Returns chat to normal. |
| `/survivalisland commandmode reload` | Reloads the trigger map from `config.yml`. |

## Configuration

All tunables (starting lives, effect damage and duration, containment interval, region names, command mode triggers) live in `config.yml`. Contestant assignments persist in `contestants.yml`.

## Building

```bash
./gradlew build
```

The built jar is placed in `build/libs`.

## Author

Made by [Glatinis](https://github.com/Glatinis). For help or issues, contact me.
