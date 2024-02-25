package hd.sphinx.sync;

import hd.sphinx.sync.backup.BackupHandler;
import hd.sphinx.sync.backup.CustomSyncSettings;
import hd.sphinx.sync.listener.DeathListener;
import hd.sphinx.sync.mongo.ManageMongoData;
import hd.sphinx.sync.mongo.MongoDB;
import hd.sphinx.sync.mysql.ManageMySQLData;
import hd.sphinx.sync.mysql.MySQL;
import hd.sphinx.sync.util.ConfigManager;
import hd.sphinx.sync.util.InventoryManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class MainManageData {

    public static StorageType storageType;

    public static ArrayList<Player> loadedPlayerData = new ArrayList<Player>();
    public static HashMap<Player, ArrayList<String>> commandHashMap = new HashMap<Player, ArrayList<String>>();

    public static void initialize() {
        try {
            storageType = StorageType.valueOf(ConfigManager.getString("settings.storageType"));
        } catch (Exception ignored) {
            Main.logger.severe("No valid StorageType is set in Config!\n Disabling Plugin!");
            Bukkit.getPluginManager().disablePlugin(Main.main);
        }

        if (storageType == StorageType.MYSQL) {
            MySQL.connectMySQL();
            try {
                MySQL.registerMySQL();
            } catch (SQLException ignored) {
                Main.logger.severe("Could not initialize Database!\n Disabling Plugin!");
                Bukkit.getPluginManager().disablePlugin(Main.main);
            }
        } else if (storageType == StorageType.MONGODB) {
            MongoDB.connectMongoDB();
        }

        BackupHandler.initialize();
    }

    public static void reload() {
        BackupHandler.shutdown();

        try {
            storageType = StorageType.valueOf(ConfigManager.getString("settings.storageType"));
        } catch (Exception exception) {
            Main.logger.severe("No valid StorageType is set in Config!\n Disabling Plugin!");
            Bukkit.getPluginManager().disablePlugin(Main.main);
        }

        if (storageType == StorageType.MYSQL) {
            if (MySQL.isConnected()) {
                MySQL.disconnectMySQL();
            } else if (MongoDB.isConnected()) {
                MongoDB.disconnectMongoDB();
            }
            MySQL.connectMySQL();
            try {
                MySQL.registerMySQL();
            } catch (SQLException ignored) {
                Main.logger.severe("Could not initialize Database!\n Disabling Plugin!");
                Bukkit.getPluginManager().disablePlugin(Main.main);
            }
        } else if (storageType == StorageType.MONGODB) {
            if (MySQL.isConnected()) {
                MySQL.disconnectMySQL();
            } else if (MongoDB.isConnected()) {
                MongoDB.disconnectMongoDB();
            }
            MongoDB.connectMongoDB();
        }

        BackupHandler.initialize();
    }

    public static void startShutdown() {
        BackupHandler.shutdown();

        Collection<Player> players = (Collection<Player>) Bukkit.getOnlinePlayers();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.iterator().next();
            savePlayer(player);
            if (i == players.size() - 1) {
                shutdown();
            }
        }
    }

    public static void shutdown() {
        if (storageType == StorageType.MYSQL) {
            MySQL.disconnectMySQL();
        } else if (storageType == StorageType.MONGODB) {
            MongoDB.disconnectMongoDB();
        }
    }

    public static Boolean isPlayerKnown(Player player) {
        if (storageType == StorageType.MYSQL) {
            return ManageMySQLData.isPlayerInDB(player);
        } else if (storageType == StorageType.MONGODB) {
            return ManageMongoData.isPlayerInDB(player);
        }
        return false;
    }

    public static void generatePlayer(Player player) {
        if (storageType == StorageType.MYSQL) {
            ManageMySQLData.generatePlayer(player);
        } else if (storageType == StorageType.MONGODB) {
            ManageMongoData.generatePlayer(player);
        }
    }

    public static void loadPlayer(Player player) {
        if (storageType == StorageType.MYSQL) {
            ManageMySQLData.loadPlayer(player);
        } else if (storageType == StorageType.MONGODB) {
            ManageMongoData.loadPlayer(player);
        }
    }

    public static boolean isEmptyItem(ItemStack itemStack) {
        return (itemStack == null || itemStack.getType() == Material.AIR);
    }

    public static boolean isEmptyPlayerInventory(Player player) {
        for (int i = 0; i < player.getInventory().getContents().length; i++) {
            if (i < 36) {
                ItemStack item = player.getInventory().getContents()[i];
                if(isEmptyItem(item)) return true;
            }
        }

        return false;
    }

    public static void dropItem(Player player, Location location, ItemStack itemStack) {
        Location l = location.clone();
        player.getWorld().dropItem(l.add(0.5, 0.5, 0.5), itemStack).setVelocity(new Vector(0, 0, 0));
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static void sendMessage(String message, Player player) {
        player.sendMessage(color(message));
    }

    public static void savePlayer(Player player) {
        if (DeathListener.deadPlayers.contains(player)) {
            player.getInventory().clear();
            player.setHealth(20);
            player.setFoodLevel(20);
            player.setLevel(0);
        }

        try {
            ItemStack itemStack = player.getItemOnCursor();
            if (!isEmptyItem(itemStack)) {
                if (isEmptyPlayerInventory(player)) {
                    player.getInventory().addItem(itemStack);
                } else {
                    dropItem(player, player.getLocation(), itemStack);
                    sendMessage("&6&l[데이터] &f: 데이터 세이브 과정에 손에 든 아이템이 바닥에 떨어졌습니다.", player);
                }
            }

            player.setItemOnCursor(new ItemStack(Material.AIR));
        } catch (Exception ignored) { }
        if (storageType == StorageType.MYSQL) {
            ManageMySQLData.savePlayer(player, InventoryManager.saveItems(player, player.getInventory()), InventoryManager.saveEChest(player));
        } else if (storageType == StorageType.MONGODB) {
            ManageMongoData.savePlayer(player, InventoryManager.saveItems(player, player.getInventory()), InventoryManager.saveEChest(player));
        }
    }

    public static void savePlayer(Player player, CustomSyncSettings customSyncSettings) {
        try {
            ItemStack itemStack = player.getItemOnCursor();
            if (!isEmptyItem(itemStack)) {
                if (isEmptyPlayerInventory(player)) {
                    player.getInventory().addItem(itemStack);
                } else {
                    dropItem(player, player.getLocation(), itemStack);
                    sendMessage("&6&l[데이터] &f: 데이터 세이브 과정에 손에 든 아이템이 바닥에 떨어졌습니다.", player);
                }
            }

            player.setItemOnCursor(new ItemStack(Material.AIR));
        } catch (Exception ignored) { }
        if (storageType == StorageType.MYSQL) {
            ManageMySQLData.savePlayer(player, customSyncSettings);
        } else if (storageType == StorageType.MONGODB) {
            ManageMongoData.savePlayer(player, customSyncSettings);
        }
    }

    public enum StorageType {

        MYSQL,
        MONGODB,
        CLOUD; // For a future Update

    }
}
