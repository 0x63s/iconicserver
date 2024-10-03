package ch.stefo.mcplugins;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.imageio.ImageIO;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.CachedServerIcon;

public final class IconicServer extends JavaPlugin implements Listener {

    // List of all icon files available
    private List<File> icons = new ArrayList<>();

    // List of cached server icons for quick access
    private List<CachedServerIcon> cachedIcons = new ArrayList<>();

    // Map of date-specific icons (e.g., for holidays)
    private Map<String, File> dateSpecificIcons = new HashMap<>();

    // Folders for storing icons and input icons
    private File iconsFolder;
    private File inputIconsFolder;

    // Random number generator for selecting icons randomly
    private Random random = new Random();

    // The current icon used in cycle mode
    private CachedServerIcon currentIcon;

    // Task ID for the icon rotation task
    private int rotationTaskId = -1;

    // Index of the last icon used in cycle mode
    private int lastIconIndex = -1;

    // Mode for icon selection (cycle, random, per-ping-random, static)
    private String iconSelectionMode;

    // The default icon to use when no special icons are set
    private CachedServerIcon defaultIcon;

    @Override
    public void onEnable() {
        getLogger().info("IconicServer Plugin loading...");

        // Create plugin directories if they don't exist
        iconsFolder = new File(getDataFolder(), "icons");
        if (!iconsFolder.exists()) {
            iconsFolder.mkdirs();
        }
        inputIconsFolder = new File(getDataFolder(), "input-icons");
        if (!inputIconsFolder.exists()) {
            inputIconsFolder.mkdirs();
        }

        // Load configuration and settings
        saveDefaultConfig();
        loadDateSpecificIcons();
        iconSelectionMode = getConfig().getString("icon-selection-mode", "cycle");

        // Process any new icons placed in the input folder
        processInputIcons();

        // Load icons from the icons folder
        refreshIconList();

        // Register event listeners
        Bukkit.getPluginManager().registerEvents(this, this);

        // Start icon rotation task if in cycle mode
        if (iconSelectionMode.equalsIgnoreCase("cycle")) {
            startIconRotationTask();
        }

        // Register command executor
        getCommand("icon").setExecutor(this);
    }

    /**
     * Loads date-specific icons from the configuration file.
     */
    public void loadDateSpecificIcons() {
        dateSpecificIcons.clear();
        if (getConfig().isConfigurationSection("date-specific-icons")) {
            for (String dateKey : getConfig().getConfigurationSection("date-specific-icons").getKeys(false)) {
                String iconName = getConfig().getString("date-specific-icons." + dateKey);
                File iconFile = new File(iconsFolder, iconName);
                if (iconFile.exists()) {
                    dateSpecificIcons.put(dateKey, iconFile);
                } else {
                    getLogger().warning("Icon file " + iconName + " for date " + dateKey + " does not exist.");
                }
            }
        }
    }

    /**
     * Refreshes the list of icons and caches them for quick access.
     */
    public void refreshIconList() {
        icons.clear();
        cachedIcons.clear();

        if (iconsFolder.exists()) {
            File[] files = iconsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
            if (files != null) {
                icons.addAll(Arrays.asList(files));
                for (File iconFile : icons) {
                    try {
                        CachedServerIcon icon = Bukkit.loadServerIcon(iconFile);
                        cachedIcons.add(icon);
                    } catch (Exception e) {
                        getLogger().warning("Failed to load icon " + iconFile.getName() + ": " + e.getMessage());
                    }
                }
            }
        }

        // Load default icon if set
        String defaultIconName = getConfig().getString("default-icon");
        if (defaultIconName != null) {
            File defaultIconFile = new File(iconsFolder, defaultIconName);
            if (defaultIconFile.exists()) {
                try {
                    defaultIcon = Bukkit.loadServerIcon(defaultIconFile);
                } catch (Exception e) {
                    getLogger().warning("Failed to load default icon: " + e.getMessage());
                }
            } else {
                getLogger().warning("Default icon file " + defaultIconName + " does not exist.");
            }
        }
    }

    /**
     * Event handler for when a server list ping occurs.
     * Sets the server icon based on the mode and date-specific icons.
     */
    @EventHandler
    public void onServerPing(ServerListPingEvent event) {
        // Check for date-specific icon
        String today = new SimpleDateFormat("dd.MM").format(new Date());
        if (dateSpecificIcons.containsKey(today)) {
            File iconFile = dateSpecificIcons.get(today);
            try {
                event.setServerIcon(Bukkit.loadServerIcon(iconFile));
            } catch (Exception e) {
                getLogger().severe("Error setting date-specific server icon: " + e.getMessage());
            }
            return;
        }

        // Handle icon selection based on mode
        if (iconSelectionMode.equalsIgnoreCase("static")) {
            // Use the default icon
            if (defaultIcon != null) {
                event.setServerIcon(defaultIcon);
            }
        } else if (iconSelectionMode.equalsIgnoreCase("random")) {
            // Random icon for all players
            if (!cachedIcons.isEmpty()) {
                event.setServerIcon(cachedIcons.get(random.nextInt(cachedIcons.size())));
            } else if (defaultIcon != null) {
                event.setServerIcon(defaultIcon);
            }
        } else if (iconSelectionMode.equalsIgnoreCase("per-ping-random")) {
            // Random icon per ping
            if (!cachedIcons.isEmpty()) {
                event.setServerIcon(cachedIcons.get(random.nextInt(cachedIcons.size())));
            } else if (defaultIcon != null) {
                event.setServerIcon(defaultIcon);
            }
        } else if (iconSelectionMode.equalsIgnoreCase("cycle")) {
            // Cycle through icons
            if (currentIcon != null) {
                event.setServerIcon(currentIcon);
            } else if (defaultIcon != null) {
                event.setServerIcon(defaultIcon);
            }
        } else {
            // Use default icon if set
            if (defaultIcon != null) {
                event.setServerIcon(defaultIcon);
            }
        }
    }

    /**
     * Command handler for the /icon command.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if arguments are provided
        if (args.length == 0) {
            sender.sendMessage("Usage: /icon <refresh | download | set | list | process | setinterval | setmode | adddateicon | removedateicon | rename>");
            return true;
        }

        // Handle subcommands
        switch (args[0].toLowerCase()) {
            case "refresh" -> {
                // Refresh the icon list
                if (!sender.hasPermission("icon.refresh")) {
                    sender.sendMessage("You do not have permission to use this command.");
                    return true;
                }
                refreshIconList();
                sender.sendMessage("Icon list refreshed.");
                return true;
            }
            case "download" -> {
                // Download an icon from a URL
                if (!sender.hasPermission("icon.download")) {
                    sender.sendMessage("You do not have permission to use this command.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("Usage: /icon download <URL> [name]");
                    return true;
                }
                String url = args[1];
                String name = null;
                if (args.length >= 3) {
                    name = args[2];
                }
                try {
                    downloadIcon(url, name);
                    sender.sendMessage("Icon downloaded and converted.");
                } catch (Exception e) {
                    sender.sendMessage("Error downloading icon: " + e.getMessage());
                }
                return true;
            }
            case "set" -> {
                // Set the default icon
                if (!sender.hasPermission("icon.set")) {
                    sender.sendMessage("You do not have permission to use this command.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("Usage: /icon set <iconName|iconID>");
                    return true;
                }
                String iconIdentifier = args[1];
                File iconFile = getIconFileByIdentifier(iconIdentifier);
                if (iconFile == null) {
                    sender.sendMessage("Icon " + iconIdentifier + " does not exist.");
                    return true;
                }
                getConfig().set("default-icon", iconFile.getName());
                saveConfig();
                try {
                    defaultIcon = Bukkit.loadServerIcon(iconFile);
                    sender.sendMessage("Default icon set to " + iconFile.getName());
                } catch (Exception e) {
                    sender.sendMessage("Failed to set default icon: " + e.getMessage());
                }
                return true;
            }
            case "list" -> {
                // List all available icons
                if (!sender.hasPermission("icon.list")) {
                    sender.sendMessage("You do not have permission to use this command.");
                    return true;
                }
                sender.sendMessage("Available icons:");
                for (int i = 0; i < icons.size(); i++) {
                    File icon = icons.get(i);
                    sender.sendMessage(ChatColor.YELLOW + "[" + i + "] " + ChatColor.RESET + icon.getName());
                }
                return true;
            }
            case "process" -> {
                // Process icons in the input folder
                if (!sender.hasPermission("icon.process")) {
                    sender.sendMessage("You do not have permission to use this command.");
                    return true;
                }
                processInputIcons();
                sender.sendMessage("Processing input icons...");
                return true;
            }
            case "setinterval" -> {
                // Set the interval for icon rotation
                if (!sender.hasPermission("icon.setinterval")) {
                    sender.sendMessage("You do not have permission to use this command.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("Usage: /icon setinterval <seconds>");
                    return true;
                }
                try {
                    int interval = Integer.parseInt(args[1]);
                    if (interval <= 0) {
                        sender.sendMessage("Interval must be positive.");
                        return true;
                    }
                    getConfig().set("icon-rotation-interval", interval);
                    saveConfig();
                    // Restart rotation task
                    if (rotationTaskId != -1) {
                        Bukkit.getScheduler().cancelTask(rotationTaskId);
                    }
                    startIconRotationTask();
                    sender.sendMessage("Icon rotation interval set to " + interval + " seconds.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("Invalid number.");
                }
                return true;
            }
            case "setmode" -> {
                // Set the icon selection mode
                if (!sender.hasPermission("icon.setmode")) {
                    sender.sendMessage("You do not have permission to use this command.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("Usage: /icon setmode <static|cycle|random|per-ping-random>");
                    return true;
                }
                String mode = args[1].toLowerCase();
                if (!mode.equals("static") && !mode.equals("cycle") && !mode.equals("random") && !mode.equals("per-ping-random")) {
                    sender.sendMessage("Invalid mode. Valid modes are: static, cycle, random, per-ping-random");
                    return true;
                }
                iconSelectionMode = mode;
                getConfig().set("icon-selection-mode", mode);
                saveConfig();
                sender.sendMessage("Icon selection mode set to " + mode);
                // Restart rotation task if necessary
                if (rotationTaskId != -1) {
                    Bukkit.getScheduler().cancelTask(rotationTaskId);
                    rotationTaskId = -1;
                }
                if (iconSelectionMode.equalsIgnoreCase("cycle")) {
                    startIconRotationTask();
                }
                return true;
            }
            case "adddateicon" -> {
                // Add a date-specific icon
                if (!sender.hasPermission("icon.adddateicon")) {
                    sender.sendMessage("You do not have permission to use this command.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("Usage: /icon adddateicon <dd.MM> <iconName|iconID>");
                    return true;
                }
                String dateKey = args[1];
                String dateIconIdentifier = args[2];
                File dateIconFile = getIconFileByIdentifier(dateIconIdentifier);
                if (dateIconFile == null) {
                    sender.sendMessage("Icon " + dateIconIdentifier + " does not exist.");
                    return true;
                }
                dateSpecificIcons.put(dateKey, dateIconFile);
                getConfig().set("date-specific-icons." + dateKey, dateIconFile.getName());
                saveConfig();
                sender.sendMessage("Added date-specific icon for " + dateKey + ": " + dateIconFile.getName());
                return true;
            }
            case "removedateicon" -> {
                // Remove a date-specific icon
                if (!sender.hasPermission("icon.removedateicon")) {
                    sender.sendMessage("You do not have permission to use this command.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("Usage: /icon removedateicon <dd.MM>");
                    return true;
                }
                String dateKeyToRemove = args[1];
                if (dateSpecificIcons.containsKey(dateKeyToRemove)) {
                    dateSpecificIcons.remove(dateKeyToRemove);
                    getConfig().set("date-specific-icons." + dateKeyToRemove, null);
                    saveConfig();
                    sender.sendMessage("Removed date-specific icon for " + dateKeyToRemove);
                } else {
                    sender.sendMessage("No date-specific icon set for " + dateKeyToRemove);
                }
                return true;
            }
            case "rename" -> {
                // Rename an existing icon
                if (!sender.hasPermission("icon.rename")) {
                    sender.sendMessage("You do not have permission to use this command.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("Usage: /icon rename <oldName|iconID> <newName>");
                    return true;
                }
                String oldIdentifier = args[1];
                String newName = args[2];
                File oldFile = getIconFileByIdentifier(oldIdentifier);
                if (oldFile == null) {
                    sender.sendMessage("Icon " + oldIdentifier + " does not exist.");
                    return true;
                }
                if (!newName.toLowerCase().endsWith(".png")) {
                    newName += ".png";
                }
                File newFile = new File(iconsFolder, newName);
                if (newFile.exists()) {
                    sender.sendMessage("An icon with the name " + newName + " already exists.");
                    return true;
                }
                boolean success = oldFile.renameTo(newFile);
                if (success) {
                    refreshIconList();
                    sender.sendMessage("Icon renamed from " + oldFile.getName() + " to " + newFile.getName());
                } else {
                    sender.sendMessage("Failed to rename icon.");
                }
                return true;
            }
            default -> {
                sender.sendMessage("Unknown subcommand.");
                return true;
            }
        }
    }

    /**
     * Helper method to get an icon file by its name or ID.
     *
     * @param identifier The icon name or ID.
     * @return The icon file, or null if not found.
     */
    public File getIconFileByIdentifier(String identifier) {
        File iconFile = null;
        try {
            int iconId = Integer.parseInt(identifier);
            if (iconId >= 0 && iconId < icons.size()) {
                iconFile = icons.get(iconId);
            }
        } catch (NumberFormatException e) {
            iconFile = new File(iconsFolder, identifier);
            if (!iconFile.exists()) {
                iconFile = null;
            }
        }
        return iconFile;
    }

    /**
     * Downloads an icon from a URL and saves it to the icons folder.
     *
     * @param urlString The URL of the image.
     * @param fileName  The desired file name.
     * @throws IOException If an error occurs during download.
     */
    private void downloadIcon(String urlString, String fileName) throws IOException {
        URL url = new URL(urlString);

        // Check that URL uses HTTPS
        if (!url.getProtocol().equalsIgnoreCase("https")) {
            throw new IOException("URL must use HTTPS.");
        }

        // Open connection with timeouts
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000); // 5 seconds
        connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        // Check HTTP response code
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Failed to download image. HTTP response code: " + responseCode);
        }

        // Check content type
        String contentType = connection.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("URL does not point to an image.");
        }

        // Limit size (e.g., max 5MB)
        int contentLength = connection.getContentLength();
        if (contentLength > 5 * 1024 * 1024) {
            throw new IOException("Image is too large.");
        }

        // Read image
        BufferedImage image = ImageIO.read(connection.getInputStream());
        connection.disconnect();

        if (image == null) {
            throw new IOException("Failed to read image.");
        }

        // Resize image if necessary
        if (image.getHeight() != 64 || image.getWidth() != 64) {
            getLogger().info("Downloaded image is not 64x64 pixels. Resizing...");
            image = resizeToServerIcon(image);
        }

        // Use the specified file name or generate one
        if (fileName == null || fileName.isEmpty()) {
            fileName = "downloaded_" + System.currentTimeMillis() + ".png";
        } else {
            // Ensure filename ends with .png
            if (!fileName.toLowerCase().endsWith(".png")) {
                fileName += ".png";
            }
        }

        File outputFile = new File(iconsFolder, fileName);
        ImageIO.write(image, "png", outputFile);
        refreshIconList();
    }

    /**
     * Processes icons placed in the input icons folder.
     * This method runs asynchronously to prevent blocking the main thread.
     */
    private void processInputIcons() {
        new BukkitRunnable() {
            @Override
            public void run() {
                File[] files = inputIconsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
                if (files != null) {
                    for (File inputFile : files) {
                        try {
                            BufferedImage image = ImageIO.read(inputFile);
                            if (image == null) {
                                getLogger().warning("Invalid image file: " + inputFile.getName());
                                continue;
                            }
                            if (image.getHeight() != 64 || image.getWidth() != 64) {
                                String name = inputFile.getName();
                                getLogger().info("Image " + name + " is not 64x64 pixels. Resizing...");
                                image = resizeToServerIcon(image);
                            }
                            File outputFile = new File(iconsFolder, inputFile.getName());
                            ImageIO.write(image, "png", outputFile);
                            if (inputFile.delete()) {
                                getLogger().info("Input icon " + inputFile.getName() + " processed and deleted.");
                            } else {
                                getLogger().warning("Failed to delete input icon " + inputFile.getName());
                            }
                        } catch (IOException e) {
                            getLogger().warning("Error processing image file " + inputFile.getName() + ": " + e.getMessage());
                        }
                    }
                }
                // Refresh icon list on the main thread after processing
                Bukkit.getScheduler().runTask(IconicServer.this, IconicServer.this::refreshIconList);
            }
        }.runTaskAsynchronously(this);
    }

    /**
     * Resizes an image to the server icon size (64x64 pixels).
     *
     * @param originalImage The original image.
     * @return The resized image.
     */
    private BufferedImage resizeToServerIcon(BufferedImage originalImage) {
        BufferedImage resizedImage = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, 64, 64, null);
        g.dispose();
        return resizedImage;
    }

    /**
     * Starts the icon rotation task for cycle mode.
     */
    private void startIconRotationTask() {
        int interval = getConfig().getInt("icon-rotation-interval", 300); // Default 300 seconds
        rotationTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if (!cachedIcons.isEmpty()) {
                lastIconIndex = (lastIconIndex + 1) % cachedIcons.size();
                currentIcon = cachedIcons.get(lastIconIndex);
            }
        }, 0L, interval * 20L); // Convert seconds to ticks (20 ticks per second)
    }
}
