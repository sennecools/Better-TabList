# TabList Mod

A dynamic Minecraft server mod that enhances the in-game player tab list by providing real-time server statistics and performance metrics.

## Overview

The TabList mod updates the in-game tab list for players with valuable server information such as:
- **TPS (Ticks per Second)**
- **MSPT (Milliseconds per Tick)**
- **Player Count**
- **Memory Usage**
- **Server Uptime**

The mod minimizes unnecessary network updates by only refreshing the tab list when the header or footer changes.

## Features

- **Dynamic Metrics:** Displays live server data (TPS, MSPT, memory usage, and uptime).
- **Customizable Templates:** Modify header and footer using placeholders.
- **Efficient Updates:** Only sends updates when there are actual changes, reducing load.
- **Simple Configuration:** Manage settings via an auto-generated configuration file.

## Installation

1. **Requirements:**
   - A compatible Minecraft server.
   - Minecraft NeoForge 1.21+.

2. **Steps:**
   - Download the latest release of the TabList mod.
   - Place the mod's JAR file into your server's `mods` directory.
   - Start the server to generate the default configuration file.
   - (Optional) Stop the server to edit the configuration file to your liking.

## Configuration

The mod configuration file lets you customize the tab list appearance by using placeholders in the header and footer templates.

### Available Placeholders
- `#N` : Inserts a new line.
- `#TPS` : Displays the server's ticks per second.
- `#MSPT` : Displays the milliseconds per tick.
- `#PLAYERCOUNT` : Shows the number of online players.
- `#MEMORY` : Displays memory usage.
- `#UPTIME` : Displays the server uptime.

### Example Config Template

```plaintext
Header: "#N             &a&lYOUR SERVER           #N&a&l&m    #N"
Footer: "&fOnline: &e#PLAYERCOUNT #N&7| TPS: &a#TPS &7| #NMemory: &b#MEMORY &7| Uptime: &d#UPTIME"
Update Interval: 500
