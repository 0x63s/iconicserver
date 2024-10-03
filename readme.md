# IconicServer Plugin

**IconicServer** is a Minecraft plugin that allows server administrators to dynamically manage server icons. Enhance your server's appearance by rotating icons, setting date-specific icons, and customizing icon selection modes.

## Features

- **Icon Selection Modes:**
  - **Static:** Use a single default icon.
  - **Cycle:** Rotate through a list of icons at set intervals.
  - **Random:** Select a random icon from the list.
  - **Per-Ping Random:** Change the icon randomly on each server ping.

- **Date-Specific Icons:** Set special icons for specific dates (format `dd.MM`), such as holidays or events.

- **Icon Management Commands:** Download, set, list, process, rename, and remove icons directly from the game.

- **Automatic Icon Processing:** Automatically resize and process icons placed in the input folder.

## Installation

1. **Download the Plugin:** Obtain the latest version of the plugin jar file.
2. **Place in Plugins Folder:** Move the jar file into your server's `plugins` directory.
3. **Start the Server:** Run your Minecraft server to generate the necessary configuration files and directories.
4. **Configure the Plugin (optional):**
- Navigate to the `plugins/IconicServer` directory.
- Edit the `config.yml` file to adjust settings like icon rotation interval and selection mode.

## Commands and Permissions

### Commands

- `/icon refresh`  
  **Description:** Refreshes the icon list from the icons folder.  
  **Permission:** `icon.refresh`

- `/icon download <URL> [name]`  
  **Description:** Downloads an icon from a URL.  
  **Permission:** `icon.download`

- `/icon set <iconName|iconID>`  
  **Description:** Sets the default server icon.  
  **Permission:** `icon.set`

- `/icon list`  
  **Description:** Lists all available icons with their IDs.  
  **Permission:** `icon.list`

- `/icon process`  
  **Description:** Processes icons placed in the input-icons folder.  
  **Permission:** `icon.process`

- `/icon setinterval <seconds>`  
  **Description:** Sets the interval for cycling icons.  
  **Permission:** `icon.setinterval`

- `/icon setmode <static|cycle|random|per-ping-random>`  
  **Description:** Sets the icon selection mode.  
  **Permission:** `icon.setmode`

- `/icon adddateicon <dd.MM> <iconName|iconID>`  
  **Description:** Adds a date-specific icon.  
  **Permission:** `icon.adddateicon`

- `/icon removedateicon <dd.MM>`  
  **Description:** Removes a date-specific icon.  
  **Permission:** `icon.removedateicon`

- `/icon rename <oldName|iconID> <newName>`  
  **Description:** Renames an existing icon.  
  **Permission:** `icon.rename`

### Permissions

- `icon.refresh`
- `icon.download`
- `icon.set`
- `icon.list`
- `icon.process`
- `icon.setinterval`
- `icon.setmode`
- `icon.adddateicon`
- `icon.removedateicon`
- `icon.rename`

_All permissions default to `op`. Assign them to specific users or groups using your permissions plugin._

## Configuration

The `config.yml` file contains the following options:

```yaml
icon-selection-mode: cycle   # Modes: static, cycle, random, per-ping-random
icon-rotation-interval: 300  # Time in seconds for cycling icons
default-icon: default.png    # Default icon file name
date-specific-icons:
  25.12: christmas.png       # Example of a date-specific icon (dd.MM format)
```

## Icon Management

### Icons Folder

- Place your icon files (PNG format, 64x64 pixels) in the `plugins/IconicServer/icons` folder.
- Icons can be named anything but must end with `.png`.

### Input Icons Folder

- Place unprocessed icons in the `plugins/IconicServer/input-icons` folder.
- Use the `/icon process` command to process and resize these icons automatically.

## Building from Source

```bash
git clone https://github.com/0x63s/iconicserver.git
cd iconicserver
mvn clean package
```

## Contributing
Feel free to contribute to this project by submitting issues or pull requests.

## License //MIT

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.


## Disclaimer
This project has not been tested thoroughly and may contain bugs. Use at your own risk.



