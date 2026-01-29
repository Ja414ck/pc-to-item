# PC To Item

A Minecraft Fabric mod for Cobblemon that lets you browse your PC storage and convert Pokemon to tradeable items.

## Features

- **GUI Browser**: Use `/pcbrowser` to open a visual PC browser
- **Box Navigation**: Navigate between PC boxes with arrow buttons
- **Pokemon Display**: See your Pokemon with their actual 3D models
- **Safe Extraction**: Two-click confirmation to prevent accidental extractions
- **Sound Effects**: Audio feedback for all actions
- **Item Compatibility**: Extracted Pokemon items work with other mods that use the PTI_NBT format

## Requirements

- Minecraft 1.21.1
- Fabric Loader 0.16.0+
- Fabric API
- Fabric Language Kotlin 1.12.0+
- Cobblemon 1.6.0+

## Installation

1. Download the latest release from the releases page
2. Place the `.jar` file in your `mods` folder
3. Make sure all dependencies are installed
4. Launch Minecraft

## Usage

1. In-game, type `/pcbrowser` to open the PC browser
2. Click on a Pokemon to select it
3. Click the green **CONFIRM EXTRACT** button to convert it to an item
4. Use the arrows at the bottom to navigate between boxes
5. Click **Close** or press ESC to exit

## GUI Layout

```
[Pokemon][Pokemon][Pokemon][Pokemon][Pokemon][Pokemon][     ][     ][Box #]
[Pokemon][Pokemon][Pokemon][Pokemon][Pokemon][Pokemon][     ][     ][     ]
[Pokemon][Pokemon][Pokemon][Pokemon][Pokemon][Pokemon][     ][     ][     ]
[Pokemon][Pokemon][Pokemon][Pokemon][Pokemon][Pokemon][     ][     ][     ]
[Pokemon][Pokemon][Pokemon][Pokemon][Pokemon][Pokemon][     ][     ][     ]
[<< Prev][Cancel ][       ][       ][Close  ][       ][     ][Confirm][Next>>]
```

## Building from Source

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/pc-to-item.git
cd pc-to-item

# Generate wrapper (if needed)
gradle wrapper

# Build the mod
./gradlew build
```

The compiled `.jar` will be in `build/libs/`.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Credits

- Created by Jack Goren
- Built for use with [Cobblemon](https://cobblemon.com/)
