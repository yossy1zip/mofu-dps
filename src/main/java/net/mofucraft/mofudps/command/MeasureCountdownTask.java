package net.mofucraft.mofudps.command;

import net.mofucraft.mofudps.config.MessageConfig;
import net.mofucraft.mofudps.dps.DpsManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * /mofudps measure <seconds> のカウントダウン(5秒固定)と、
 * その後の指定秒数間の平均DPS計測を行う。Placeholder/ランキングとは無関係。
 */
public final class MeasureCountdownTask extends BukkitRunnable {

    private static final int COUNTDOWN_SECONDS = 5;
    private static final long TICKS_PER_SECOND = 20L;

    private final Plugin plugin;
    private final DpsManager dpsManager;
    private final MessageConfig messages;
    private final Player player;
    private final int measureSeconds;

    private int remaining = COUNTDOWN_SECONDS;

    public MeasureCountdownTask(Plugin plugin, DpsManager dpsManager, MessageConfig messages,
                                 Player player, int measureSeconds) {
        this.plugin = plugin;
        this.dpsManager = dpsManager;
        this.messages = messages;
        this.player = player;
        this.measureSeconds = measureSeconds;
    }

    public void start() {
        runTaskTimer(plugin, 0L, TICKS_PER_SECOND);
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            cancel();
            return;
        }

        if (remaining > 0) {
            player.sendMessage(messages.get("measure.countdown", "seconds_left", String.valueOf(remaining)));
            remaining--;
            return;
        }

        cancel();
        player.sendMessage(messages.get("measure.start"));
        dpsManager.startMeasurement(player.getUniqueId());

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            double total = dpsManager.endMeasurement(player.getUniqueId());
            double dps = total / measureSeconds;
            player.sendMessage(messages.get("measure.result",
                    "seconds", String.valueOf(measureSeconds),
                    "dps", MessageConfig.formatNumber(dps),
                    "total_damage", MessageConfig.formatNumber(total)));
        }, measureSeconds * TICKS_PER_SECOND);
    }
}
