## Unreleased

- Made generated configs easier to edit with section guides, setup examples, clear timing units, and one animation frame per line.
- Added missing config guidance while preserving custom values, comments, and legacy settings.
- Refreshed the setup documentation and Modrinth description for the 1.6 features.

## What's New in 1.6

- Added optional LuckPerms prefixes, suffixes, groups, and rank sorting.
- Added dimension, health, ping, and game-mode placeholders for player names.
- Added colored MSPT/CPU metrics and optional spark statistics, with improved update performance.
- Supports Fabric and NeoForge on Minecraft 1.21–1.21.11 and preserves existing name formats.
- Thanks to [MihailDenisov](https://github.com/MihailDenisov) for the original implementation and ideas!

## What's New in 1.5

- Added gradient color support with two syntax options:
  - MiniMessage-style: `<gradient:#FF0000:#0000FF>text</gradient>` (supports 2+ color stops)
  - TAB-style: `<#FF0000>text</#0000FF>` (2 colors)
- Gradients preserve formatting codes like `&l` (bold), `&o` (italic), etc.
- Improved color code parsing with proper Component-based text rendering
