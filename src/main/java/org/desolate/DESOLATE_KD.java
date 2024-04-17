package org.desolate;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
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
import java.util.Objects;
import java.util.UUID;

public final class DESOLATE_KD extends JavaPlugin implements Listener {
    private Map<UUID, Integer[]> kdMap;

    @Override
    public void onEnable() {
        //配置文件加载
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            boolean isCreateDir = configFile.getParentFile().mkdirs();
            //添加一个文件夹创建判断
            if (!isCreateDir) {
                getLogger().warning("Failed to create config.yml!");
                return;
            }
            saveResource("config.yml", false);
        }
        //移除无用的配置文件读取
        this.kdMap = new HashMap<>();
        this.loadKdData();
        //命令注册修改可能出现的空指针异常
        Objects.requireNonNull(this.getCommand("kd")).setExecutor(new KDCommandExecutor());
        getServer().getPluginManager().registerEvents(this, this);
        saveConfig();
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
        //判断玩家是否是否为空，如果为空不走如下逻辑，否则会出现空指针异常(而不是instanceof)
        if (player.getKiller() != null) {
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
        Entity attackEntity = event.getDamager(); //攻击者
        Entity attackedEntity = event.getEntity(); //被攻击者
        double damageValue = event.getDamage(); //伤害数值
        EntityType attackedType = attackedEntity.getType(); //被攻击者类型
        //攻击事件格式化调试输出
        getLogger().info("实体:" + attackedEntity.getName() + "被" + attackEntity.getName() + "攻击了");
        if (attackedType == EntityType.PLAYER) {
            Player attackPlayerObj = ((Player) attackEntity).getPlayer();
            //判断空指针异常再行使用玩家对象
            Objects.requireNonNull(attackPlayerObj).sendMessage(
                    ChatColor.RED + "对" +
                    ChatColor.DARK_PURPLE + attackedEntity.getName() +
                    ChatColor.RED + "造成" +
                    ChatColor.GREEN + damageValue +
                    ChatColor.RED + "点伤害"
            );
            //取消无用判断
            attackedEntity.sendMessage("你受到" + damageValue + "点伤害");
        }
    }
}