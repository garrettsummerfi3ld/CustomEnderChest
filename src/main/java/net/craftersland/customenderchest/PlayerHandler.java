package net.craftersland.customenderchest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerHandler implements Listener {

    private final EnderChest enderchest;
    private final Set<Player> interactCooldown = new HashSet<Player>();

    public PlayerHandler(EnderChest enderchest) {
        this.enderchest = enderchest;
    }

    @EventHandler
    public void onCmd(ServerCommandEvent event) {
        if ((event.getCommand().matches("reload") || (event.getCommand().matches("/reload")))) {
            event.getSender().sendMessage(enderchest.getConfigHandler().getStringWithColor("chatMessages.reloadCmdWarning"));
        }
    }

    @EventHandler
    public void onPlayerJoinEvent(final PlayerJoinEvent e) {
        enderchest.getDataHandler().addJoinDelay(e.getPlayer());
        Bukkit.getScheduler().runTaskLaterAsynchronously(enderchest, () -> {
            enderchest.getDataHandler().loadPlayerFromStorage(e.getPlayer());
            enderchest.getDataHandler().removeJoinDelay(e.getPlayer());
        }, 20L * 2);
    }

    @EventHandler
    public void onPlayerDisconnectEvent(PlayerQuitEvent e) {
        enderchest.getDataHandler().removeJoinDelay(e.getPlayer());
        enderchest.getDataHandler().removeData(e.getPlayer().getUniqueId());
    }

    //Player click event
    @EventHandler(priority = EventPriority.HIGHEST)
    @SuppressWarnings("deprecation")
    public void onPlayerInteract(final PlayerInteractEvent e) {
        if (e.getClickedBlock() != null) {
            if (!e.isCancelled()) {
                if (e.getClickedBlock().getType() == Material.ENDER_CHEST) {
                    if (!enderchest.getConfigHandler().getBoolean("settings.disable-enderchest-click")) {
                        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                            if (enderchest.getDataHandler().hasJoinDelay(e.getPlayer())) {
                                e.setCancelled(true);
                                e.getPlayer().sendMessage(enderchest.getConfigHandler().getStringWithColor("chatMessages.prefix") + enderchest.getConfigHandler().getStringWithColor("chatMessages.joinDelay"));
                                enderchest.getSoundHandler().sendFailedSound(e.getPlayer());
                                return;
                            }
                            if (!interactCooldown.contains(e.getPlayer())) {
                                if (!e.getPlayer().isSneaking()) {
                                    //e.setCancelled(true);
                                    addInteractCooldown(e.getPlayer());
                                    enderchest.getEnderChestUtils().openMenu(e.getPlayer());
                                } else {
                                    // This is why we have the warning suppression in this event handler, as "getItemInHand()" is considered
                                    // deprecated, and should switch to "getItemInMainHand()" and "getItemInOffHand()" for those checks,
                                    // However this is a compatibility layer for older Minecraft versions
                                    if (!EnderChest.is19Server) {
                                        if (!hasItemInHand(e.getPlayer().getItemInHand())) {
                                            //e.setCancelled(true);
                                            addInteractCooldown(e.getPlayer());
                                            enderchest.getEnderChestUtils().openMenu(e.getPlayer());
                                        }
                                    } else {
                                        if (!hasItemInHand(e.getPlayer().getInventory().getItemInMainHand()) && !hasItemInHand(e.getPlayer().getInventory().getItemInOffHand())) {
                                            //e.setCancelled(true);
                                            addInteractCooldown(e.getPlayer());
                                            enderchest.getEnderChestUtils().openMenu(e.getPlayer());
                                        }
                                    }
                                }
                            }
                            e.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    private void addInteractCooldown(final Player p) {
        interactCooldown.add(p);
        Bukkit.getScheduler().runTaskLaterAsynchronously(enderchest, () -> interactCooldown.remove(p), 20L);
    }

    private boolean hasItemInHand(ItemStack item) {
        if (item == null) {
            return false;
        } else {
            return item.getType() != Material.AIR;
        }
    }

    private void saveEnderchest(final Inventory inv, final Player p, final UUID u) {
        Bukkit.getScheduler().runTaskAsynchronously(enderchest, () -> {
            if (u == null) {
                enderchest.getStorageInterface().saveEnderChest(p, inv);
            } else {
                enderchest.getStorageInterface().saveEnderChest(u, inv);
                //enderchest.admin.remove(inv);
            }
        });
    }

    //Player inventory close event
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        e.getInventory();
        try {
            if (enderchest.getDataHandler().isLiveEnderchest(e.getInventory())) {
                enderchest.getSoundHandler().sendEnderchestCloseSound(p);
                if (enderchest.admin.containsKey(e.getInventory())) {
                    UUID u = enderchest.admin.get(e.getInventory());
                    if (!u.equals(e.getPlayer().getUniqueId())) {
                        enderchest.admin.remove(e.getInventory());
                    }
                    //enderchest.getDataHandler().setData(u, e.getInventory());
                    //saveEnderchest(e.getInventory(), (Player) e.getPlayer(), u);
                } else {
                    enderchest.getDataHandler().setData(e.getPlayer().getUniqueId(), e.getInventory());
                    saveEnderchest(e.getInventory(), (Player) e.getPlayer(), null);
                }
            } else if (enderchest.admin.containsKey(e.getInventory())) {
                enderchest.getSoundHandler().sendEnderchestCloseSound(p);
                saveEnderchest(e.getInventory(), (Player) e.getPlayer(), enderchest.admin.get(e.getInventory()));
                enderchest.admin.remove(e.getInventory());
            }
        } catch (Exception ex) {
            EnderChest.log.severe("Error saving enderchest data for player: " + p.getName() + " . Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

}
