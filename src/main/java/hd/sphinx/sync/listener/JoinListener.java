package hd.sphinx.sync.listener;

import hd.sphinx.sync.Main;
import hd.sphinx.sync.MainManageData;
import hd.sphinx.sync.api.SyncSettings;
import hd.sphinx.sync.api.events.GeneratingPlayerProfileEvent;
import hd.sphinx.sync.api.events.ProcessingLoadingPlayerDataEvent;
import hd.sphinx.sync.util.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JoinListener implements Listener {
    public static final Map<UUID, Boolean> loadPlayerDataMap = new HashMap<>();

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        loadPlayerDataMap.put(player.getUniqueId(), false);

        if (ConfigManager.getBoolean("settings.onlySyncPermission") && !player.hasPermission("sync.sync")) return;
        MainManageData.loadedPlayerData.add(player);
        MainManageData.commandHashMap.put(player, new ArrayList<String>());
        if (DeathListener.deadPlayers.contains(player)) {
            DeathListener.deadPlayers.remove(player);
        }

        if (!MainManageData.isPlayerKnown(player)) {
            if (ConfigManager.getBoolean("settings.sending.generated")) {
                player.sendMessage(ConfigManager.getColoredString("messages.generated"));
            }
            MainManageData.generatePlayer(player);
            Bukkit.getPluginManager().callEvent(new GeneratingPlayerProfileEvent(player));
            if (!ConfigManager.getBoolean("settings.usingOldData")) {
                if (ConfigManager.getBoolean("settings.syncing.enderchest")) {
                    player.getEnderChest().clear();
                }
                if (ConfigManager.getBoolean("settings.syncing.inventory")) {
                    player.getInventory().clear();
                }
                if (ConfigManager.getBoolean("settings.syncing.exp")) {
                    player.setLevel(0);
                }
            }
        } else {
            if (ConfigManager.getBoolean("settings.syncing.enderchest")) {
                player.getEnderChest().clear();
            }
            if (ConfigManager.getBoolean("settings.syncing.inventory")) {
                player.getInventory().clear();
            }
            if (ConfigManager.getBoolean("settings.syncing.exp")) {
                player.setLevel(0);
            }
        }

        player.sendMessage(ConfigManager.getColoredString("messages.loading"));
        Bukkit.getPluginManager().callEvent(new ProcessingLoadingPlayerDataEvent(player, new SyncSettings()));
        MainManageData.sendMessage("&6&l[데이터] &f&a&l"+ player.getName() +"&f님의 데이터를 불러오고 있습니다.", player);
        player.setNoDamageTicks(20 * 5);

        Bukkit.getScheduler().runTaskLater(Main.main, new Runnable() {
            @Override
            public void run() {
                MainManageData.loadPlayer(player);
                loadPlayerDataMap.put(player.getUniqueId(), true);
                MainManageData.sendMessage("&6&l[데이터] &f&a&l"+ player.getName() +"&f님의 데이터 로딩이 완료 되었습니다.", player);
            }
        }, 20 * 5);
    }
    
    @EventHandler
    public void onInteractPlayer(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        //데이터 로딩 되기 전에 모든 인터렉션 제한
        if (loadPlayerDataMap.get(player.getUniqueId()) == null ||
            !loadPlayerDataMap.get(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        //데이터 로딩 되기 전에 모든 인터렉션 제한
        if (loadPlayerDataMap.get(player.getUniqueId()) == null ||
                !loadPlayerDataMap.get(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onClickInventory(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        //데이터 로딩 되기 전에 모든 인터렉션 제한
        if (loadPlayerDataMap.get(player.getUniqueId()) == null ||
                !loadPlayerDataMap.get(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void openInventory(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();

        //데이터 로딩 되기 전에 모든 인터렉션 제한
        if (loadPlayerDataMap.get(event.getPlayer().getUniqueId()) == null ||
                !loadPlayerDataMap.get(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        if (MainManageData.loadedPlayerData.contains(event.getPlayer())) event.setCancelled(true);
        if (DeathListener.deadPlayers.contains(event.getPlayer())) event.setCancelled(true);

        if (loadPlayerDataMap.get(event.getPlayer().getUniqueId()) == null ||
            !loadPlayerDataMap.get(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!MainManageData.loadedPlayerData.contains(event.getPlayer())) return;
        if (event.getClickedBlock() != null && event.getClickedBlock().getType() != Material.AIR) event.setCancelled(true);
    }


    @EventHandler
    public void onRunningCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (loadPlayerDataMap.get(player.getUniqueId()) == null ||
                !loadPlayerDataMap.get(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
