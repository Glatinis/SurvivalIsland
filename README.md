# SurvivalIsland

[![Build and Release](https://github.com/Glatinis/SurvivalIsland/actions/workflows/release.yml/badge.svg)](https://github.com/Glatinis/SurvivalIsland/actions/workflows/release.yml)
[![Latest Release](https://img.shields.io/github/v/release/Glatinis/SurvivalIsland)](https://github.com/Glatinis/SurvivalIsland/releases)
![Paper API](https://img.shields.io/badge/Paper%20API-26.2-blue)
![Java](https://img.shields.io/badge/Java-25-orange)
![WorldGuard](https://img.shields.io/badge/WorldGuard-7.0.17-green)

A Paper plugin for running a multi-island survival show: a lives based elimination scoreboard, gift triggered effects, restocking loot chests, contestant to island management, and mob and world containment built on top of WorldGuard and WorldEdit.

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

### Lives

Every online player sees the lives board, but only assigned contestants get an actual row (a number) on it, and only they can be targeted by these commands. The board is a private scoreboard the plugin manages itself, not the server's shared main scoreboard, so it will not appear in `/scoreboard objectives list` and vanilla `/scoreboard players` commands cannot read or change it. Use the commands below instead.

| Command | Description |
| --- | --- |
| `/survivalisland lives set <player> <amount>` | Sets a contestant's lives to an exact value. |
| `/survivalisland lives add <player> <amount>` | Adds to a contestant's lives. |
| `/survivalisland lives remove <player> <amount>` | Subtracts from a contestant's lives. |

Every lives value is mirrored to `lives.yml` as it changes and restored automatically, so a server crash or restart mid-show does not reset anyone's lives back to full. Removing a contestant clears their saved value too, so a later re-add starts them fresh rather than resuming their old count.

A contestant is automatically switched to spectator mode the moment their lives hit 0, and switched back to survival mode if a `/survivalisland lives add` brings them back above 0.

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
| `/survivalisland event deepfreeze <on\|off>` | Converts water to ice in the configured area, slows every player for the configured duration, fills the area with falling snow particles, and gives every contestant an unlimited "Deepfreeze!" snowball. The ice, slowness, particles, and snowballs all revert when it ends - except one thing: every exposed grass block on each island gets a permanent thin snow layer the moment it turns on, which is never cleared. |
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

### Chest

Look directly at a chest before running these.

| Command | Description |
| --- | --- |
| `/survivalisland chest mark [interval-seconds] [item-count]` | Marks the chest so it periodically clears and refills itself from the loot pool. Both arguments are optional and override the configured defaults for that chest only. |
| `/survivalisland chest unmark` | Stops a chest from restocking. |
| `/survivalisland chest restock` | Forces an already-marked chest to restock immediately. |
| `/survivalisland chest list` | Shows how many restocking chests are currently tracked. |

## Configuration

Every tunable lives in `config.yml`, generated on first run. Contestant to island assignments are saved separately in `contestants.yml` and do not need to be edited by hand.

### lives

| Key | Default | Description |
| --- | --- | --- |
| `starting-lives` | `20` | Lives every contestant starts with. |
| `scoreboard-title-json` | (see file) | Adventure JSON text used as the sidebar scoreboard title. |

### command-mode

| Key | Default | Description |
| --- | --- | --- |
| `triggers` | `{}` | Map of trigger word to a list of console commands to run. `%player%` is replaced with the name of whoever typed the trigger. |

### acid-rain

| Key | Default | Description |
| --- | --- | --- |
| `duration-ticks` | `8000` | How long the storm lasts before automatically stopping. |
| `tick-interval` | `20` | How often (in ticks) damage and poison are reapplied. |
| `player-damage` | `1.0` | Damage dealt to the targeted player each tick. |
| `mob-damage` | `2.0` | Damage dealt to passive and tameable mobs on the target's island each tick. |
| `poison-amplifier` | `0` | Poison effect level applied to the target. |
| `poison-duration-ticks` | `8100` | How long each poison application lasts, refreshed every tick so it never runs out mid storm. |

### acid-ocean

| Key | Default | Description |
| --- | --- | --- |
| `tick-interval` | `10` | How often submerged targets are checked and damaged. |
| `player-damage` | `2.0` | Damage dealt while the target is in water. |
| `poison-amplifier` | `0` | Poison effect level applied while submerged. |
| `poison-duration-ticks` | `60` | How long each poison application lasts. |
| `restrict-to-island` | `false` | If `true`, only damages the target while they are in water on their own island instead of anywhere. |

### deep-freeze

| Key | Default | Description |
| --- | --- | --- |
| `duration-ticks` | `18000` | How long the freeze lasts (18000 ticks is 15 minutes) before automatically reverting. |
| `region` | `"arena"` | WorldGuard region whose water is converted to ice. Falls back to the arena box below if the region does not exist. |
| `slowness-amplifier` | `1` | Slowness effect level applied to every player while active. |
| `snow-particles.interval-ticks` | `4` | How often a burst of falling-snow particles is spawned. |
| `snow-particles.count-per-cycle` | `150` | How many particles spawn per burst. |
| `snow-particles.height-above-area` | `4.0` | How far above the top of the deep-freeze area the particles start falling from. |
| `snow-particles.speed` | `0.05` | Fall speed passed to the particle effect. |

Each island's own region (from `islands`, not the tighter `islands-land` containment region) also gets a permanent thin snow layer over every exposed grass block the moment deep freeze turns on - see the `event deepfreeze` command above.

### containment

| Key | Default | Description |
| --- | --- | --- |
| `check-interval-ticks` | `10` | How often leashed mobs and ender dragons are checked against their bounds. |
| `arena.world` | `"world"` | World the arena box is in. |
| `arena.min` / `arena.max` | `[-500, 0, -500]` / `[500, 256, 500]` | Corners of the box ender dragons are confined to, and the fallback area for deep freeze. |
| `safe-tower-region` | `"safe-tower"` | WorldGuard region where nothing can ever spawn or be broken. |

### islands

A list of the WorldGuard region ids treated as islands, for example `["island1", "island2", "island3"]`. Used by contestant assignment, protection, and theft.

### islands-land

Optional, same order as `islands`. If your island regions include a bit of shoreline or ocean (a common reason to draw them a little larger than the dry land), leashed mobs would otherwise be free to wander into that water since it's technically still "in bounds." Draw one more, tighter WorldGuard region per island that hugs just the dry land (for example `island1-land`) and list them here in the same order, mob containment will use these tighter regions instead of the full island ones. Leave empty, or shorter than `islands`, to skip this and just use each island's own region.

### protection

| Key | Default | Description |
| --- | --- | --- |
| `default-mode` | `LOCKED` | Starting protection mode: `LOCKED`, `OWN_ISLAND_ONLY`, or `FREE_FOR_ALL`. |

### natural-spawn-block-reasons

A list of Bukkit `SpawnReason` values that are always cancelled, so mobs only ever appear from gift commands, spawn eggs, or spawners. Defaults to `NATURAL`, `CHUNK_GEN`, `PATROL`, `VILLAGE_DEFENSE`, `VILLAGE_INVASION`, `RAID`, and `REINFORCEMENTS`.

### pvp / theft

| Key | Default | Description |
| --- | --- | --- |
| `pvp.default-enabled` | `true` | Whether PvP is on when the server starts. |
| `theft.default-enabled` | `false` | Whether players can open containers outside their own island when the server starts. |

### restocking-chests

| Key | Default | Description |
| --- | --- | --- |
| `default-interval-ticks` | `6000` | How often a marked chest restocks (6000 ticks is 5 minutes), unless overridden per-chest with `/survivalisland chest mark`. |
| `default-item-count` | `5` | How many random rolls fill a chest per restock, unless overridden per-chest. |
| `loot-pool` | (see file) | The pool restocks draw from. Each entry is `{ material, amount, weight }`, plus an optional `potion` (a `PotionType` name) for `POTION`, `SPLASH_POTION`, or `LINGERING_POTION` entries. Higher weight means more common. |

Note: Minecraft doesn't have separate "blue egg" or "brown egg" items, both just show up in the pool as a plain `EGG` entry. Marked chest locations are saved in `restocking-chests.yml` and reload automatically on startup, but each chest's restock timer restarts from zero rather than resuming exactly where it left off.

## Building

```bash
./gradlew build
```

The built jar is placed in `build/libs`.

## Author

Made by [Glatinis](https://github.com/Glatinis). For help or issues, contact me.
