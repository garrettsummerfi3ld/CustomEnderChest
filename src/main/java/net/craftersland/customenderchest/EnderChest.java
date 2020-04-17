package net.craftersland.customenderchest;

import net.craftersland.customenderchest.commands.FileToMysqlCmd;
import net.craftersland.customenderchest.storage.FlatFileStorage;
import net.craftersland.customenderchest.storage.MysqlSetup;
import net.craftersland.customenderchest.storage.MysqlStorage;
import net.craftersland.customenderchest.storage.StorageInterface;
import net.craftersland.customenderchest.utils.EnderChestUtils;
import net.craftersland.customenderchest.utils.ModdedSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class EnderChest extends JavaPlugin {

    public static Logger log;
    public static boolean is19Server = true;
    public static boolean is13Server = false;
    public static String pluginName = "CustomEnderChest";
    private static ConfigHandler configHandler;
    private static StorageInterface storageInterface;
    private static EnderChestUtils enderchestUtils;
    private static DataHandler dH;
    private static MysqlSetup mysqlSetup;
    private static SoundHandler sH;
    private static ModdedSerializer ms;
    private static FileToMysqlCmd ftmc;
    public Map<Inventory, UUID> admin = new HashMap<>();

    public void onEnable() {
        log = getLogger();
        getMcVersion();
        configHandler = new ConfigHandler(this);
        checkForModdedNBTsupport();
        enderchestUtils = new EnderChestUtils(this);
        if (configHandler.getString("database.typeOfDatabase").equalsIgnoreCase("mysql")) {
            log.info("Using MySQL database for data.");
            mysqlSetup = new MysqlSetup(this);
            storageInterface = new MysqlStorage(this);
        } else {
            log.info("Using FlatFile system for data. IMPORTANT! We recommend MySQL.");
            File pluginFolder = new File("plugins" + System.getProperty("file.separator") + pluginName + System.getProperty("file.separator") + "PlayerData");
            if (!pluginFolder.exists()) {
                pluginFolder.mkdir();
            }
            storageInterface = new FlatFileStorage(this);
        }
        dH = new DataHandler(this);
        sH = new SoundHandler(this);
        ftmc = new FileToMysqlCmd(this);
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerHandler(this), this);
        CommandHandler cH = new CommandHandler(this);
        Objects.requireNonNull(getCommand("customec")).setExecutor(cH);
        Objects.requireNonNull(getCommand("ec")).setExecutor(cH);
        Objects.requireNonNull(getCommand("customenderchest")).setExecutor(cH);
        log.info(pluginName + " loaded successfully!");
    }

    //Disabling plugin
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        if (configHandler.getString("database.typeOfDatabase").equalsIgnoreCase("mysql")) {
            if (mysqlSetup.getConnection() != null) {
                log.info("Closing database connection...");
                mysqlSetup.closeDatabase();
            }
        }
        log.info("Cleaning internal data...");
        dH.clearLiveData();
        HandlerList.unregisterAll(this);
        log.info(pluginName + " is disabled!");
    }

    private boolean getMcVersion() {
        log.info("Getting Minecraft Version...");
        String[] serverVersion = Bukkit.getBukkitVersion().split("-");
        String version = serverVersion[0];
        String[] legacyVersions = new String[]{"1.7.10", "1.7.9", "1.7.5", "1.7.2", "1.8.8", "1.8.3", "1.8.3", "1.8.4", "1.8"};
        String[] CombatUpdateVersions = new String[]{"1.9", "1.9.1", "1.9.2", "1.9.3", "1.9.4", "1.10", "1.10.1", "1.10.2", "1.11", "1.11.1", "1.11.2", "1.12", "1.12.1", "1.12.2"};
        String[] AquaticUpdateVersions = new String[]{"1.13", "1.13.1", "1.13.2", "1.14", "1.14.1", "1.14.2", "1.14.3", "1.14.4"};
        String[] BeeUpdateVersions = new String[]{"1.15", "1.15.1", "1.15.2"};

        if (version.matches(Arrays.toString(legacyVersions))) {
            is19Server = false;
            is13Server = false;
            log.info("Compatible server version detected: " + version);
            log.info("Running older versions of CraftBukkit, running legacy APIs...");
            return true;
        } else if (version.matches(Arrays.toString(CombatUpdateVersions))) {
            is19Server = true;
            is13Server = false;
            log.info("Compatible server version detected: " + version);
            log.info("Running plugin with CombatUpdate APIs...");
            return true;
        } else if (version.matches(Arrays.toString(AquaticUpdateVersions))) {
            is19Server = true;
            is13Server = true;
            log.info("Compatible server version detected: " + version);
            log.info("Running plugin with AquaticUpdate APIs...");
            return true;
        } else if (version.matches(Arrays.toString(BeeUpdateVersions))) {
            is19Server = true;
            is13Server = true;
            log.info("Compatible server version detected: " + version);
            log.info("Running plugin with BuzzyBees APIs...");
            return true;
        } else {
            //Default fallback to 1.15 API
            is19Server = true;
            is13Server = true;
            log.warning("Incompatible/unknown server version detected: " + version);
            log.warning("Running plug with BuzzyBees APIs as fallback. If you think this is an error please submit a issue on our GitHub.");
        }
        return false;
    }

    private void checkForModdedNBTsupport() {
        log.info("Checking NBT support from settings...");
        if (configHandler.getBoolean("settings.modded-NBT-data-support")) {
            log.info("NBT support is set 'true' in your 'config.yml'...");
            if (configHandler.getString("database.typeOfDatabase").equalsIgnoreCase("mysql")) {
                if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
                    ms = new ModdedSerializer(this);
                    log.info("Soft dependency 'ProtocolLib' found. Modded NBT data support is enabled!");
                } else {
                    log.warning("Soft dependency 'ProtocolLib' not found! Modded NBT data support is disabled! Is 'ProtocolLib' in your 'plugins' folder?");
                }
            } else {
                log.warning("NBT Modded data support only works for MySQL storage. Modded NBT data support is disabled!");
            }
        }
    }

    public ConfigHandler getConfigHandler() {
        return configHandler;
    }

    public StorageInterface getStorageInterface() {
        return storageInterface;
    }

    public EnderChestUtils getEnderChestUtils() {
        return enderchestUtils;
    }

    public MysqlSetup getMysqlSetup() {
        return mysqlSetup;
    }

    public SoundHandler getSoundHandler() {
        return sH;
    }

    public DataHandler getDataHandler() {
        return dH;
    }

    public ModdedSerializer getModdedSerializer() {
        return ms;
    }

    public FileToMysqlCmd getFileToMysqlCmd() {
        return ftmc;
    }

}
