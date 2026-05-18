# Dummy

Language / 语言: [简体中文](README.md) | English

![:Dummy](https://count.getloli.com/@railgun19457_Dummy?name=railgun19457_Dummy&theme=minecraft&padding=6&offset=0&align=top&scale=1&pixelated=1&darkmode=auto)

Dummy is a fake player plugin for modern Paper servers. It creates real server-side fake player entities and provides inventory, equipment, experience, actions, skins, persistence, and chunk loading.

## Features

- Creates real `ServerPlayer` dummy players using Paper + NMS
- Supports spawn, remove, list, reload, and teleport management
- Opens a GUI by right-clicking a dummy, with inventory, armor, and offhand management
- Supports per-dummy settings: invulnerable, collision, ghost mode, chunk loader, Tab visibility, and name format
- Uses the creator skin by default and supports fetching skins by premium player name
- Caches fetched skins to reduce repeated Mojang API requests
- Persists dummy inventory, armor, offhand, experience, skin, settings, and location
- Can keep removed dummy data and restore it when the same name is spawned again
- Loads chunks around a dummy using the world's `simulation-distance` for redstone and entity ticking
- Includes `zh_CN` and `en_US` language files
- Supports `/dummy` and `/dm` command aliases

## Support Matrix

| Item | Supported Range |
| --- | --- |
| Java | 21+ |
| Server | Paper |
| Verified Paper versions | 1.21.11, 26.1.2 |

## Installation

1. Download the plugin Jar from Releases, or build it locally.
2. Put the Jar into the Paper server `plugins` directory.
3. Start the server. The plugin will generate these files on first startup:
   - `plugins/Dummy/config.yml`
   - `plugins/Dummy/lang/zh_CN.yml`
   - `plugins/Dummy/lang/en_US.yml`
   - `plugins/Dummy/dummies.yml`
   - `plugins/Dummy/skins.yml`

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/dummy` | Shows command help | `dummy.command` |
| `/dm` | Alias of `/dummy` | `dummy.command` |
| `/dummy spawn <name>` | Spawns a dummy at your current location | `dummy.command.spawn` |
| `/dummy remove <name>` | Removes one dummy | `dummy.command.remove` |
| `/dummy remove all` | Removes all dummies | `dummy.command.remove-all` |
| `/dummy list` | Lists active dummies | `dummy.command.list` |
| `/dummy reload` | Reloads config and language files | `dummy.command.reload` |
| `/dummy config <name> [key] [value]` | Opens the config GUI or updates dummy settings | `dummy.command.config` |
| `/dummy skin <name> set <playerName>` | Uses the skin of a premium player name | `dummy.command.skin` |
| `/dummy skin <name> clear` | Clears the dummy skin | `dummy.command.skin` |
| `/dummy exp <name> <amount|all>` | Transfers dummy experience to the command player | `dummy.command.exp` |
| `/dummy inv <name>` | Opens dummy inventory, armor, and offhand slots | `dummy.command.inv` |
| `/dummy tpto <name>` | Teleports you to a dummy | `dummy.command.tpto` |
| `/dummy tphere <name>` | Teleports a dummy to your current location | `dummy.command.tphere` |
| `/dummy tps <name>` | Swaps positions with a dummy | `dummy.command.tps` |
| `/dummy actions <name> <action> ...` | Controls dummy actions | `dummy.command.actions` |

## Actions

Available actions:

- `attack` Attacks the looked-at or nearest entity
- `chat <message>` Sends a chat message
- `command <command>` Runs a command as the dummy
- `drop` Drops the held item
- `hold <0-8>` Switches the selected hotbar slot
- `jump` Jumps
- `look <yaw> <pitch>` Looks in a specific rotation
- `look <north|east|south|west|entity>` Looks in a fixed direction or at the nearest entity
- `lookat <x> <y> <z>` Looks at a block position, supports `~`, `~1`, `~-1`
- `mine` Mines the looked-at block
- `mount` Mounts or dismounts a nearby mountable entity
- `move [speed]` Moves toward the current look direction
- `place` Places the main-hand block against the looked-at block
- `sneak [toggle|on|off]` Toggles or sets sneaking
- `sprint [toggle|on|off]` Toggles or sets sprinting
- `swap` Swaps main-hand and offhand items
- `use` Uses the main-hand item
- `stop [action]` Stops all actions or one specific action

### Action Examples

```text
/dummy actions bot attack
/dummy actions bot attack repeat 20
/dummy actions bot attack repeat 20 1200
/dummy actions bot look entity repeat 5
/dummy actions bot lookat ~ ~ ~5
/dummy actions bot move 0.25 repeat 1 100
/dummy actions bot stop
/dummy actions bot stop attack
```

Action mode notes:

- If no mode is specified, the action runs once.
- `once` runs the action once.
- `repeat <intervalTicks> [durationTicks]` repeats the action at a tick interval, with an optional duration.
- Some actions also support suffix syntax: `<action> <args...> repeat <intervalTicks> [durationTicks]`.
- Repeated actions pause by default when a dummy dies, is removed, or quits, and continue when the same UUID dummy appears again. Disable this with `actions.preserve-on-lifecycle`.

## Permissions

| Permission | Description | Default |
| --- | --- | --- |
| `dummy.command` | Allows using the base command | All players |
| `dummy.command.spawn` | Allows spawning dummies | All players |
| `dummy.command.remove` | Allows removing one dummy | All players |
| `dummy.command.remove-all` | Allows removing all dummies | OP |
| `dummy.command.list` | Allows listing dummies | All players |
| `dummy.command.reload` | Allows reloading config | OP |
| `dummy.command.config` | Allows changing dummy settings | All players |
| `dummy.command.skin` | Allows changing dummy skins | All players |
| `dummy.command.exp` | Allows transferring dummy experience | All players |
| `dummy.command.inv` | Allows opening dummy inventories | All players |
| `dummy.command.tpto` | Allows teleporting to dummies | All players |
| `dummy.command.tphere` | Allows teleporting dummies to you | All players |
| `dummy.command.tps` | Allows swapping positions | All players |
| `dummy.command.actions` | Allows controlling dummy actions | All players |

## Configuration Overview

Main `config.yml` sections:

- `language`: Language file, built-in values are `zh_CN` and `en_US`
- `defaults`: Default settings for newly created dummies
- `limits`: Server-wide and per-player dummy limits, use `-1` for unlimited
- `storage`: Startup restoration and removed-data retention
- `inventory`: Whether inventory, armor, and offhand items drop when a dummy is removed or the plugin shuts down
- `commands`: Console commands executed before and after dummy creation
- `death`: Whether a dead dummy is automatically re-summoned
- `actions`: Action system settings, such as preserving lifecycle actions and mount search range

Per-dummy settings configurable by command:

- `invulnerable`: Whether the dummy is invulnerable
- `collision`: Whether collision is enabled
- `ghost`: Ghost mode; the dummy is invulnerable, non-collidable, gravity-free, and does not load chunks
- `chunk-loader`: Whether to load surrounding chunks using the world's `simulation-distance`
- `show-in-tab`: Whether the dummy appears in the Tab list
- `name-format`: Display name format, supports `%name%`

Command placeholders for `commands.before-spawn` and `commands.after-spawn`:

- `%dummy%`: Dummy name
- `%uuid%`: Dummy UUID
- `%creator%`: Creator name
- `%creator_uuid%`: Creator UUID
- `%world%`: Spawn world
- `%x%`: Spawn X coordinate
- `%y%`: Spawn Y coordinate
- `%z%`: Spawn Z coordinate

## Data Files

- `dummies.yml`: Stores active and removed dummy data
- `skins.yml`: Stores skin cache fetched by player name
- `lang/zh_CN.yml`: Chinese language file
- `lang/en_US.yml`: English language file

## Local Build

```bash
gradle clean build
```

Build output:

- `build/libs/Dummy-<version>.jar`
