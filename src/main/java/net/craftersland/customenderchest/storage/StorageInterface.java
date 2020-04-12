package net.craftersland.customenderchest.storage;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public interface StorageInterface {

    //Storage Flat File data methods
	boolean hasDataFile(UUID playerUUID);

    boolean deleteDataFile(UUID playerUUID);

    boolean saveEnderChest(Player p, Inventory inv);

    boolean saveEnderChest(UUID p, Inventory inv);

    boolean loadEnderChest(Player p, Inventory inv);

    boolean loadEnderChest(UUID playerUUID, Inventory inv);

    String loadName(UUID playerUUID);

    Integer loadSize(UUID playerUUID);

    //For import command only
	void saveEnderChest(UUID uuid, Inventory endInv, String playerName, int invSize);

}
