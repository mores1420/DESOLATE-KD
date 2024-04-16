package com.mores.desolatekd;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Desolatekd extends JavaPlugin implements Listener {
    private FileConfiguration config;
    private File configFile;
    private Map<UUID, Integer[]> kdMap;

    @Override
    public void onEnable() {
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        this.kdMap = new HashMap<>();
        this.loadKdData();
        this.getCommand("kd").setExecutor(new KDCommandExecutor());
        getServer().getPluginManager().registerEvents(this, this);
        saveConfig();
        config = getConfig();
        getLogger().info("DESOLATE-KD LOADED!");
    }

    private void loadKdData() {
        FileConfiguration config = this.getConfig();
        for (String key : config.getKeys(false)) {
            UUID uuid = UUID.fromString(key);
            int kills = config.getInt(key + ".kills");
            int deaths = config.getInt(key + ".deaths");
            kdMap.put(uuid, new Integer[]{kills, deaths});
        }
    }

    private void saveKdData() {
        FileConfiguration config = this.getConfig();
        for (UUID uuid : kdMap.keySet()) {
            Integer[] kd = kdMap.get(uuid);
            config.set(uuid.toString() + ".kills", kd[0]);
            config.set(uuid + ".deaths", kd[1]);
        }
        this.saveConfig();
    }

    public void onDisable() {
        getLogger().info("DESOLATE-KD has been disabled!");
        saveKdData();
    }


    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUniqueId();
        if (player.getKiller() instanceof Player) {
            Player killer = player.getKiller();
            UUID killerUUID = killer.getUniqueId();
            int kills = kdMap.getOrDefault(killerUUID, new Integer[]{0, 0})[0] + 1;
            kdMap.put(killerUUID, new Integer[]{kills, kdMap.getOrDefault(killerUUID, new Integer[]{0, 0})[1]});
            int deaths = kdMap.getOrDefault(playerUUID, new Integer[]{0, 0})[1] + 1;
            kdMap.put(playerUUID, new Integer[]{kdMap.getOrDefault(playerUUID, new Integer[]{0, 0})[0], deaths});

        } else {
            return;
        }
        saveKdData();
    }

    private class KDCommandExecutor implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command!");
                return true;
            }
            Player player = (Player) sender;
            UUID playerUUID = player.getUniqueId();
            Integer[] kd = kdMap.getOrDefault(playerUUID, new Integer[]{0, 0});
            double kills = kd[0];
            double deaths = kd[1];
            double kdRatio = 0;
            if (deaths > 0) {
                kdRatio = kills / deaths;
            }
            String Kdstring = String.format("%.2f", kdRatio);
            player.sendMessage(ChatColor.GREEN + "--------" + ChatColor.GOLD + "战绩汇总" + ChatColor.GREEN + "--------");
            player.sendMessage("    " + ChatColor.BLUE + "生涯总击杀：" + kd[0]);
            player.sendMessage("    " + ChatColor.RED + "生涯总死亡：" + kd[1]);
            player.sendMessage("    " + ChatColor.DARK_PURPLE + "KD：" + Kdstring);
            player.sendMessage(ChatColor.GREEN + "----------------------");
            return true;
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {

        Entity damagee = event.getEntity();
        Entity damager = event.getDamager();
        double damage = event.getDamage();
        EntityType damagerType=damager.getType();
        EntityType damageeType=damagee.getType();

        System.out.println(damagerType+"111");
        if (damagerType==EntityType.PLAYER) {
            String damageeName = damagee.getName();
            System.out.println(damageeName+"333");
            Player Pdamager = ((Player) damager).getPlayer();
            Pdamager.sendMessage(
                    ChatColor.RED + "对" +
                            ChatColor.DARK_PURPLE + damageeName +
                            ChatColor.RED + "造成" +
                            ChatColor.GREEN + damage +
                            ChatColor.RED + "伤害");
        } else if (damageeType==EntityType.PLAYER) {
            damagee.sendMessage("你受到"+damage+"伤害");
        }
    }

}
