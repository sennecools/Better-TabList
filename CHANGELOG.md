## What's New in 1.5

- Added gradient color support with two syntax options:
  - MiniMessage-style: `<gradient:#FF0000:#0000FF>text</gradient>` (supports 2+ color stops)
  - TAB-style: `<#FF0000>text</#0000FF>` (2 colors)
- Gradients preserve formatting codes like `&l` (bold), `&o` (italic), etc.
- Improved color code parsing with proper Component-based text rendering
