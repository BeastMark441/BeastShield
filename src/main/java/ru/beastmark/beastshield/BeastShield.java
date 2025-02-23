package ru.beastmark.beastshield;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class BeastShield extends JavaPlugin implements Listener {
    private static class ItemConfig {
        boolean enabled;
        int cooldown;

        ItemConfig(boolean enabled, int cooldown) {
            this.enabled = enabled;
            this.cooldown = cooldown;
        }
    }

    private Map<Material, ItemConfig> itemConfigs;
    private int defaultCooldown;
    private String latestVersion;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        checkUpdate();
    }

    private void checkUpdate() {
        new UpdateChecker(this).getVersion(version -> {
            latestVersion = version;
            if (!this.getDescription().getVersion().equals(version)) {
                getLogger().info("Доступна новая версия BeastShield: " + version);
                getLogger().info("Скачать: https://github.com/BeastMark441/BeastShield/releases/latest");
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("beastshield.use") && latestVersion != null && 
            !this.getDescription().getVersion().equals(latestVersion)) {
            player.sendMessage(ChatColor.GREEN + "[BeastShield] " + ChatColor.YELLOW + 
                "Доступна новая версия: " + latestVersion);
            player.sendMessage(ChatColor.GREEN + "[BeastShield] " + ChatColor.YELLOW + 
                "Скачать: https://github.com/BeastMark/BeastShield/releases/latest");
        }
    }

    private void loadConfig() {
        reloadConfig();
        FileConfiguration config = getConfig();
        
        defaultCooldown = config.getInt("default-shield-cooldown", 100);
        itemConfigs = new HashMap<>();
        
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                try {
                    Material material = Material.valueOf(key.toUpperCase());
                    boolean enabled = itemsSection.getBoolean(key + ".enabled", false);
                    int cooldown = itemsSection.getInt(key + ".cooldown", defaultCooldown);
                    
                    itemConfigs.put(material, new ItemConfig(enabled, cooldown));
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Неверный материал в конфигурации: " + key);
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("beastshield")) {
            if (!sender.hasPermission("beastshield.use")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    getConfig().getString("messages.no-permission", "&cУ вас нет прав для использования этой команды!")));
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                loadConfig();
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    getConfig().getString("messages.reload", "&aКонфигурация успешно перезагружена!")));
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        
        // Проверяем, есть ли у игрока право на обход ограничений
        if (player.hasPermission("beastshield.exempt")) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        ItemConfig itemConfig = itemConfigs.get(item.getType());
        
        // Проверяем, есть ли предмет в конфигурации и включен ли он
        if (itemConfig != null && itemConfig.enabled) {
            if (event.getRightClicked() instanceof Player) {
                Player target = (Player) event.getRightClicked();
                
                // Проверяем, использует ли цель щит
                if (target.isBlocking()) {
                    // Устанавливаем КД щита с учетом индивидуальной настройки предмета
                    target.setCooldown(Material.SHIELD, itemConfig.cooldown);
                }
            }
        }
    }
}
