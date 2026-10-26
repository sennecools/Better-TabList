# Better TabList

A customizable, server-side tab list with animated headers and footers, rank prefixes, player status, and live server statistics.

Supports **NeoForge** and **Fabric** for Minecraft **1.21–1.21.11**.

**Install on the server. Players can join with vanilla clients.** LuckPerms, FTB Ranks, and spark are optional.

![Example of Better TabList with FTB Ranks](https://cdn.modrinth.com/data/3o3inmWM/images/2cf58e8c05e52c0fc875141cfd76afdb5da15d20.png)

Inspired by [Tab List](https://modrinth.com/mod/tab-list).

## Features

- **Animated Header & Footer** — Multiple frames that cycle automatically, with configurable speed
- **AFK Detection** — Greyed-out names and an `#AFK` placeholder after a configurable timeout
- **Player Sorting** — Alphabetical or rank-based (via FTB Ranks power or LuckPerms group weight) tab list ordering
- **FTB Ranks Integration** — Optional; uses rank permissions for display names and sorting
- **LuckPerms Integration** — Optional; contextual prefixes/suffixes, primary groups, and rank sorting
- **Player Status** — Optional colored dimension symbols, health (including absorption), ping, and game mode in display names
- **Performance Metrics** — Colored TPS/MSPT/CPU, with optional spark rolling averages and MSPT percentiles
- **Hex Color Support** — Full `&#RRGGBB` hex colors alongside standard `&` color codes
- **Gradients** — Smooth color transitions with two supported gradient syntaxes
- **Efficient Updates** — Header/footer packets are sent when their content changes; metric samples are shared across players

## Installation

1. [Download](https://modrinth.com/mod/better-tablist/versions) the JAR matching your server's **Minecraft version and loader**.
2. Put it in the server's `mods/` folder. **Fabric servers also need [Fabric API](https://modrinth.com/mod/fabric-api).**
3. Start the server once to generate `config/tablist.toml`.
4. Change `server_name`, then customize the header, footer, and player names.
5. Run `/tablist reload` as an operator (permission level 2), or restart the server.

## Quick Setup

### 1. Choose how player names should look

Edit the **existing `[appearance]` section** in `config/tablist.toml`. Replace the matching settings below; do not add a second `[appearance]` section.

**Plain names, no rank mod needed:**

```toml
name_formatting_provider = "NONE"
display_name_format = "{name} &7#AFK"
```

**Names with dimension and health:**

```toml
name_formatting_provider = "NONE"
display_name_format = "{dimension} {name} {health} &7#AFK"
```

**LuckPerms prefixes and suffixes:** install [LuckPerms](https://luckperms.net/download) on the server, then use:

```toml
name_formatting_provider = "LP"
display_name_format = "{prefix}{name}{suffix}&r &7#AFK"
```

For status icons too, use `"{dimension} {prefix}{name}{suffix}&r {health} &7#AFK"`.

**FTB Ranks:** install FTB Ranks and its required dependencies, then use:

```toml
name_formatting_provider = "FTB"
display_name_format = "[{rank}] {name} &7#AFK"
```

If the player has an `ftbranks.name_format` permission, **that format takes precedence**. To use your config format exclusively, select `NONE` instead. The default provider is `FTB` for compatibility; the mod still works when no rank integration is installed.

### 2. Set the header and footer

Each quoted item is **one animation frame**. Use `#N` for multiple lines inside a frame.

```toml
# In the existing [appearance] section:
server_name = "My Server"
header = [
    "#N<gradient:#FF5555:#FFAA55>&l#SERVERNAME</gradient>&r#N&7Welcome, #PLAYERNAME!#N"
]
footer = [
    "#N&fOnline: &e#PLAYERCOUNT&7/&e#MAXPLAYERS#N&7TPS: #CTPS &7| MSPT: #CMSPT &7| Ping: &f#PING ms#N"
]
```

- **One item:** static text. **Two or more items:** animated text. **`[]`:** hide that header/footer.
- `update_interval = 500` refreshes every **500 milliseconds**.
- `animation_interval = 4` changes frames every **4 refreshes** — about **2 seconds** at the default refresh interval.

### 3. Optional rank sorting and spark metrics

To sort by rank, change `sort_mode` in the existing `[sorting]` section:

```toml
sort_mode = "RANK" # NONE, ALPHABETICAL, or RANK
```

`RANK` uses the selected provider's highest inherited LuckPerms group weight or FTB rank power, then sorts alphabetically. Without an available rank provider, weights are zero. Players on another scoreboard team stay on that team.

To enable [spark](https://modrinth.com/mod/spark) statistics, install spark on the server and change the existing `[performance]` section:

```toml
metrics_provider = "SPARK" # VANILLA or SPARK
```

Rolling TPS and MSPT percentiles require spark. Without it, basic TPS/MSPT/CPU use built-in measurements and rolling statistics show `N/A`.

## Configuration

All settings live in `config/tablist.toml`. Keep text values in quotes, use unquoted `true` / `false` for switches, and keep the section names shown below.

| Section | What to change |
|---------|----------------|
| `[appearance]` | Server name, header/footer frames, player-name format, rank provider, refresh timing |
| `[sorting]` | Player ordering: `NONE`, `ALPHABETICAL`, or `RANK` |
| `[afk]` | Enable AFK detection and set the timeout **in seconds**; `300` means 5 minutes |
| `[performance]` | Metrics source: `VANILLA` or `SPARK` |

Existing configs remain compatible. Your names and formats are preserved, and the old `ftbranks.enable_ftbranks_formatting` setting is used to choose a provider when `name_formatting_provider` is missing. Once the new option exists, use it to change providers.

### Header and Footer Placeholders

Player-specific values refer to the player viewing the tab list. These placeholders also work in `display_name_format`.

| Placeholder    | Description                                        |
|----------------|----------------------------------------------------|
| `#N`           | New line                                           |
| `#SERVERNAME`  | Server name (set in config)                        |
| `#TPS`         | Estimated TPS (vanilla), or 1-minute TPS (spark)     |
| `#CTPS`        | TPS with automatic color (green/yellow/red)        |
| `#MSPT`        | Milliseconds per tick                              |
| `#CMSPT`       | MSPT with automatic color                          |
| `#CPU`         | Server process CPU usage percentage                |
| `#CCPU`        | CPU usage with automatic color                     |
| `#TPS_1M` / `#CTPS_1M` | 1-minute TPS, plain / colored (spark)      |
| `#TPS_5M` / `#CTPS_5M` | 5-minute TPS, plain / colored (spark)      |
| `#MSPT_P95` / `#CMSPT_P95` | 1-minute MSPT 95th percentile, plain / colored (spark) |
| `#PLAYERCOUNT` | Number of online players                           |
| `#MAXPLAYERS`  | Maximum player slots                               |
| `#PLAYERNAME`  | Viewing player's name                              |
| `#PING`        | Player ping in ms                                  |
| `#RANK`        | FTB rank or LuckPerms primary group, using the selected provider |
| `#AFK`         | Shows "AFK" if the player is AFK, empty otherwise  |
| `#WORLD`       | Player's current dimension                         |
| `#DIMENSION`   | Colored dimension symbol (Ⓦ / Ⓝ / Ⓔ), or custom dimension name |
| `#HEALTH`      | Health + absorption, e.g. `[20❤]` (20 health points = 10 hearts) |
| `#MEMORY`      | Memory usage (used / max)                          |
| `#UPTIME`      | Server uptime                                      |
| `#DATE`        | Real date (yyyy-MM-dd)                             |
| `#TIME`        | Real time (HH:mm)                                  |

### Color Codes

- Standard: `&a` (green), `&c` (red), `&l` (bold), `&r` (reset), etc.
- Hex: `&#FF5555` for any RGB color
- Gradients: `<gradient:#FF0000:#0000FF>text</gradient>` or `<#FF0000>text</#0000FF>`

### Display Name Format

Use these in `display_name_format`, headers, or footers:

| Placeholder | Description |
|-------------|-------------|
| `{name}` | Player name |
| `{rank}` | FTB rank or LuckPerms primary group |
| `{prefix}`, `{suffix}` | LuckPerms cached prefix/suffix in the player's current context |
| `{primary_group}` | LuckPerms primary group |
| `{dimension}` | Colored dimension symbol, or custom dimension name |
| `{health}` | Colored health + absorption indicator |
| `{world}`, `{ping}`, `{gamemode}` | Dimension name, ping in ms, and game mode |

`{prefix}`, `{suffix}`, and `{primary_group}` require the `LP` provider. `{rank}` uses the selected provider. Unavailable integration values are empty. LuckPerms formatting supports the color and gradient syntax above; chat plugins may have additional syntax.

`#AFK` is empty while the player is active. AFK detection watches movement, turning, and chat; it is an inactivity indicator rather than a check of every possible player action.

### Performance Metrics

Install spark separately to use rolling TPS and MSPT percentiles. If spark is unavailable or not ready, basic TPS/MSPT/CPU fall back to vanilla/JVM measurements; rolling statistics show `N/A`. Unsupported CPU measurements also show `N/A`. Vanilla TPS is estimated from average tick execution time, rather than a measured rolling tick rate.

Automatic colors use green/yellow/red thresholds: TPS ≥18 / ≥15 / <15, MSPT ≤40 / ≤50 / >50, CPU ≤60% / ≤85% / >85%. Metrics are sampled once per tab-list update and shared across players.

### Complete Starter Config

For a new setup, this is a complete, simple example. For an existing setup, edit the matching settings rather than replacing your custom configuration.

```toml
[appearance]
server_name = "My Server"
header = [
    "#N&a&l#SERVERNAME&r#N&7Welcome, #PLAYERNAME!#N"
]
footer = [
    "#N&fOnline: &e#PLAYERCOUNT&7/&e#MAXPLAYERS#N&7TPS: #CTPS &7| MSPT: #CMSPT#N"
]
name_formatting_provider = "NONE"
display_name_format = "{name} &7#AFK"
update_interval = 500
animation_interval = 4

[sorting]
sort_mode = "NONE"

[afk]
afk_enabled = true
afk_timeout = 300

[performance]
metrics_provider = "VANILLA"
```

### Common Questions

- **My LuckPerms prefix is missing.** Select `LP`, include `{prefix}` in your format, and check that the player has a prefix in their current LuckPerms context.
- **My config name format does not apply with FTB Ranks.** Check the `ftbranks.name_format` permission; it takes precedence when present.
- **Extra header items are not appearing as extra lines.** Items are animation frames. Use `#N` to add lines inside one frame.
- **I see `N/A` in spark statistics.** Select `SPARK` and check that spark is installed and ready. Unsupported statistics show `N/A`.
- **How do I apply changes?** Run `/tablist reload` as an operator, or restart. Invalid TOML, such as a missing quote or duplicate section, must be fixed before reloading.

Need help? [Report an issue](https://github.com/sennecools/Better-TabList/issues) with your Minecraft version, loader, and relevant config settings.

## Contributors

- **sennecools** and **NeofastFTL** — Better TabList
- **[MihailDenisov](https://github.com/MihailDenisov)** — Original LuckPerms integration, dimension/health display, rank-weight caching, and performance-metric ideas/code from the [LPintegration branch](https://github.com/MihailDenisov/Better-TabList/tree/LPintegration), adapted for all supported targets. See [issue #13](https://github.com/sennecools/Better-TabList/issues/13).

## Building from Source

Requires Java 21.

```sh
./gradlew build
```

This builds and checks all 24 Minecraft/loader targets. Output JARs are in `versions/<minecraft>-<loader>/build/libs/`.

For a single target:

```sh
./gradlew :1.21.1-neoforge:build --configure-on-demand
```

To sync the user-facing guide above to Modrinth, set `MODRINTH_TOKEN` with the `PROJECT_WRITE` scope and run:

```sh
./gradlew updateModrinthDescription --configure-on-demand
```
