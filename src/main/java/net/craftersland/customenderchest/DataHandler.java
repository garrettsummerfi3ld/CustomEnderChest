package net.craftersland.customenderchest;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.*;

public class DataHandler {

    private final Map<UUID, Inventory> liveData = new HashMap<UUID, Inventory>();
    private final Set<Player> joinDelay = new HashSet<Player>();
    private final EnderChest pl;

    public DataHandler(EnderChest plugin) {
        this.pl = plugin;
        loadAlreadyOnlinePlayers();
    }

    public void addJoinDelay(Player p) {
        joinDelay.add(p);
    }

    public void removeJoinDelay(Player p) {
        joinDelay.remove(p);
    }

    public boolean hasJoinDelay(Player p) {
        return joinDelay.contains(p);
    }

    public Inventory getData(UUID playerUUID) {
        return liveData.get(playerUUID);
    }

    public void setData(UUID playerUUID, Inventory enderchestInventory) {
        liveData.put(playerUUID, enderchestInventory);
    }

    public void removeData(UUID playerUUID) {
        liveData.remove(playerUUID);
    }

    public boolean isLiveEnderchest(Inventory inventory) {
        return liveData.containsValue(inventory);
    }

    public void clearLiveData() {
        liveData.clear();
    }

    public void loadPlayerFromStorage(Player p) {
        if (p.isOnline()) {
            int size = pl.getEnderChestUtils().getSize(p);
            if (size == 0) {
                size = 9;
            }
            String enderChestTitle = pl.getEnderChestUtils().getTitle(p);
            Inventory inv = Bukkit.getServer().createInventory(p, size, enderChestTitle);
            if (pl.getStorageInterface().hasDataFile(p.getUniqueId())) {
                pl.getStorageInterface().loadEnderChest(p, inv);
            }
            setData(p.getUniqueId(), inv);
        }
    }

    private void loadAlreadyOnlinePlayers() {
        if (!Bukkit.getOnlinePlayers().isEmpty()) {
            EnderChest.log.info("Loading data for online players...");
            for (Player p : Bukkit.getOnlinePlayers()) {
                loadPlayerFromStorage(p);
            }
        }
    }

}
