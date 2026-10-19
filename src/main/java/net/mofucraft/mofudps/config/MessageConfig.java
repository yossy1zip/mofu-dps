package net.mofucraft.mofudps.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

/**
 * config.ymlのmessagesセクションを読み込み、色コード変換とプレースホルダー置換を行う。
 */
public final class MessageConfig {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private String prefix;

    public MessageConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        this.prefix = colorize(config.getString("messages.prefix", ""));
    }

    /** prefix付きの単一メッセージを取得する。placeholdersは key1, value1, key2, value2 ... の順。 */
    public String get(String path, String... placeholders) {
        return prefix + getRaw(path, placeholders);
    }

    /** prefixなしのメッセージを取得する(ランキング表示など複数行の一覧向け)。 */
    public String getRaw(String path, String... placeholders) {
        String raw = config.getString("messages." + path, "");
        return colorize(applyPlaceholders(raw, placeholders));
    }

    private String applyPlaceholders(String message, String... placeholders) {
        String result = message;
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            result = result.replace("%" + placeholders[i] + "%", placeholders[i + 1]);
        }
        return result;
    }

    public static String colorize(String message) {
        return message == null ? "" : ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String formatNumber(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
