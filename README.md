# Better TabList

A server-side Minecraft mod that fully customizes the in-game tab list with animated headers/footers, AFK detection, player sorting, and FTB Ranks integration.

Supports **NeoForge**, **Fabric**, and **Forge** for Minecraft 1.21–1.21.1.

## Features

- **Animated Header & Footer** — Multiple frames that cycle automatically, with configurable speed
- **AFK Detection** — Greyed-out names and an `#AFK` placeholder after a configurable timeout
- **Player Sorting** — Alphabetical or rank-based (via FTB Ranks) tab list ordering
- **FTB Ranks Integration** — Optional; uses rank permissions for display names and sorting
- **Hex Color Support** — Full `&#RRGGBB` hex colors alongside standard `&` color codes
- **Efficient Updates** — Only sends packets when content actually changes

## Installation

1. Download the JAR for your mod loader from the releases page.
2. Place it in your server's `mods/` folder.
3. Start the server — a `config/tablist.toml` file will be generated with defaults.
4. Edit the config and restart (or reload) to apply changes.

## Configuration

All settings live in `config/tablist.toml`.

### Placeholders

| Placeholder    | Description                                        |
|----------------|----------------------------------------------------|
| `#N`           | New line                                           |
| `#SERVERNAME`  | Server name (set in config)                        |
| `#TPS`         | Ticks per second                                   |
| `#CTPS`        | TPS with automatic color (green/yellow/red)        |
| `#MSPT`        | Milliseconds per tick                              |
| `#PLAYERCOUNT` | Number of online players                           |
| `#MAXPLAYERS`  | Maximum player slots                               |
| `#PLAYERNAME`  | Viewing player's name                              |
| `#PING`        | Player ping in ms                                  |
| `#RANK`        | Player rank (requires FTB Ranks)                   |
| `#AFK`         | Shows "AFK" if the player is AFK, empty otherwise  |
| `#WORLD`       | Player's current dimension                         |
| `#MEMORY`      | Memory usage (used / max)                          |
| `#UPTIME`      | Server uptime                                      |
| `#DATE`        | Real date (yyyy-MM-dd)                             |
| `#TIME`        | Real time (HH:mm)                                  |

### Color Codes

- Standard: `&a` (green), `&c` (red), `&l` (bold), `&r` (reset), etc.
- Hex: `&#FF5555` for any RGB color

### Display Name Format

The `display_name_format` option controls how player names appear in the tab list. Use `{name}` and `{rank}` as placeholders:

```
display_name_format = "{name} &7#AFK"
```

### Example Config

```toml
[appearance]
server_name = "My Server"
header = [
    "#N        &#FF5555&l#SERVERNAME        #N&#AAAAAA&m            #N",
    "#N        &#5555FF&l#SERVERNAME        #N&#AAAAAA&m            #N"
]
footer = [
    "&fOnline: &e#PLAYERCOUNT&7/&e#MAXPLAYERS #N&7TPS: #CTPS &7| MSPT: &#55FFFF#MSPT #N&7Memory: &#AA55FF#MEMORY &7| Uptime: &#FFAA00#UPTIME"
]
display_name_format = "{name} &7#AFK"
update_interval = 500
animation_interval = 4

[sorting]
sort_mode = "NONE"

[ftbranks]
enable_ftbranks_formatting = true

[afk]
afk_enabled = true
afk_timeout = 300
```

## Building from Source

Requires Java 21.

```sh
./gradlew build
```

Output JARs:
- `neoforge/build/libs/` — NeoForge
- `fabric/build/libs/` — Fabric
- `forge/build/libs/` — Forge
