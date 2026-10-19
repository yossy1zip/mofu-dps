package net.mofucraft.mofudps.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.mofucraft.mofudps.MofuDPS;
import net.mofucraft.mofudps.config.MessageConfig;
import net.mofucraft.mofudps.dps.DpsManager;
import net.mofucraft.mofudps.dps.PlayerRecord;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * PlaceholderAPI拡張(identifier: mofudps)。
 * %mofudps_dps% / %mofudps_maxdps% / %mofudps_rank% / %mofudps_rank_<N>_name(dps)%
 */
public final class MofuDpsExpansion extends PlaceholderExpansion {

    private final MofuDPS plugin;
    private final DpsManager dpsManager;

    public MofuDpsExpansion(MofuDPS plugin, DpsManager dpsManager) {
        this.plugin = plugin;
        this.dpsManager = dpsManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "mofudps";
    }

    @Override
    public @NotNull String getAuthor() {
        return "mofucraft";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        String lower = params.toLowerCase(Locale.ROOT);

        if (lower.equals("dps")) {
            return player == null ? "0.00" : MessageConfig.formatNumber(dpsManager.getCurrentDps(player.getUniqueId()));
        }

        if (lower.equals("maxdps")) {
            return player == null ? "0.00" : MessageConfig.formatNumber(dpsManager.getMaxDps(player.getUniqueId()));
        }

        if (lower.equals("rank")) {
            if (player == null) {
                return "-";
            }
            int rank = dpsManager.getRank(player.getUniqueId());
            return rank > 0 ? String.valueOf(rank) : "-";
        }

        if (lower.startsWith("rank_")) {
            String[] parts = lower.split("_");
            if (parts.length == 3) {
                int position;
                try {
                    position = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    return null;
                }

                List<PlayerRecord> ranking = dpsManager.getRanking(Math.max(position, 0));
                boolean withinRange = position >= 1 && position <= ranking.size();
                PlayerRecord record = withinRange ? ranking.get(position - 1) : null;

                if (parts[2].equals("name")) {
                    return record != null ? record.getName() : "-";
                }
                if (parts[2].equals("dps")) {
                    return record != null ? MessageConfig.formatNumber(record.getMaxDps()) : "0.00";
                }
            }
        }

        return null;
    }
}
