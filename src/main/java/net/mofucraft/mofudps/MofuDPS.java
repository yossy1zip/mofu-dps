package net.mofucraft.mofudps;

import net.mofucraft.mofudps.command.MofuDPSCommand;
import net.mofucraft.mofudps.config.MessageConfig;
import net.mofucraft.mofudps.dps.DpsManager;
import net.mofucraft.mofudps.listener.DamageListener;
import net.mofucraft.mofudps.placeholder.MofuDpsExpansion;
import net.mofucraft.mofudps.storage.DpsStorage;
import net.mofucraft.mofudps.storage.MySQLDpsStorage;
import net.mofucraft.mofudps.storage.SQLiteDpsStorage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class MofuDPS extends JavaPlugin {

    private MessageConfig messageConfig;
    private DpsManager dpsManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        messageConfig = new MessageConfig(this);
        messageConfig.load();

        DpsStorage storage = createStorage();
        int windowSeconds = getConfig().getInt("damage.window-seconds", 5);
        dpsManager = new DpsManager(this, storage, windowSeconds);
        dpsManager.init();

        getServer().getPluginManager().registerEvents(new DamageListener(dpsManager), this);

        MofuDPSCommand command = new MofuDPSCommand(this, dpsManager, messageConfig);
        Objects.requireNonNull(getCommand("mofudps")).setExecutor(command);
        Objects.requireNonNull(getCommand("mofudps")).setTabCompleter(command);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MofuDpsExpansion(this, dpsManager).register();
            getLogger().info("PlaceholderAPIとの連携を有効化しました。");
        }
    }

    @Override
    public void onDisable() {
        if (dpsManager != null) {
            dpsManager.shutdown();
        }
    }

    private DpsStorage createStorage() {
        FileConfiguration config = getConfig();
        String type = config.getString("storage.type", "sqlite");

        if (type.equalsIgnoreCase("mysql")) {
            return new MySQLDpsStorage(
                    config.getString("storage.mysql.host", "localhost"),
                    config.getInt("storage.mysql.port", 3306),
                    config.getString("storage.mysql.database", "mofudps"),
                    config.getString("storage.mysql.username", "root"),
                    config.getString("storage.mysql.password", ""),
                    config.getBoolean("storage.mysql.useSSL", false),
                    config.getString("storage.mysql.table-prefix", "mofudps_")
            );
        }

        String fileName = config.getString("storage.sqlite.file", "mofudps.db");
        return new SQLiteDpsStorage(new File(getDataFolder(), fileName));
    }

    public MessageConfig getMessageConfig() {
        return messageConfig;
    }

    public DpsManager getDpsManager() {
        return dpsManager;
    }
}
