package net.mofucraft.mofudps.command;

import net.mofucraft.mofudps.MofuDPS;
import net.mofucraft.mofudps.config.MessageConfig;
import net.mofucraft.mofudps.dps.DpsManager;
import net.mofucraft.mofudps.dps.PlayerRecord;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class MofuDPSCommand implements CommandExecutor, TabCompleter {

    private final MofuDPS plugin;
    private final DpsManager dpsManager;
    private final MessageConfig messages;

    public MofuDPSCommand(MofuDPS plugin, DpsManager dpsManager, MessageConfig messages) {
        this.plugin = plugin;
        this.dpsManager = dpsManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(messages.get("unknown-command"));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "measure" -> handleMeasure(sender, args);
            case "ranking" -> handleRanking(sender, args);
            case "reset" -> handleReset(sender, args);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage(messages.get("unknown-command"));
        }
        return true;
    }

    private void handleMeasure(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mofudps.measure")) {
            sender.sendMessage(messages.get("no-permission"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("player-only"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.get("measure.usage"));
            return;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(messages.get("measure.invalid-seconds"));
            return;
        }
        if (seconds <= 0) {
            sender.sendMessage(messages.get("measure.invalid-seconds"));
            return;
        }
        if (dpsManager.hasActiveMeasurement(player.getUniqueId())) {
            sender.sendMessage(messages.get("measure.already-running"));
            return;
        }

        new MeasureCountdownTask(plugin, dpsManager, messages, player, seconds).start();
    }

    private void handleRanking(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mofudps.ranking")) {
            sender.sendMessage(messages.get("no-permission"));
            return;
        }

        int count = plugin.getConfig().getInt("ranking.display-count", 10);
        if (args.length >= 2) {
            try {
                count = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
                // 数値でなければconfigのデフォルト値を使用する
            }
        }

        List<PlayerRecord> ranking = dpsManager.getRanking(count);
        sender.sendMessage(messages.getRaw("ranking.header", "count", String.valueOf(count)));
        if (ranking.isEmpty()) {
            sender.sendMessage(messages.getRaw("ranking.empty"));
        } else {
            int rank = 1;
            for (PlayerRecord record : ranking) {
                sender.sendMessage(messages.getRaw("ranking.entry",
                        "rank", String.valueOf(rank),
                        "player", record.getName(),
                        "dps", MessageConfig.formatNumber(record.getMaxDps())));
                rank++;
            }
        }
        sender.sendMessage(messages.getRaw("ranking.footer"));
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mofudps.reset")) {
            sender.sendMessage(messages.get("no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.get("reset.usage"));
            return;
        }

        String target = args[1];
        boolean confirmed = args.length >= 3 && args[2].equalsIgnoreCase("confirm");

        if (target.equalsIgnoreCase("all")) {
            if (!confirmed) {
                sender.sendMessage(messages.get("reset.confirm-required"));
                return;
            }
            dpsManager.resetAll();
            sender.sendMessage(messages.get("reset.all-done"));
            return;
        }

        Optional<PlayerRecord> record = dpsManager.findByName(target);
        if (record.isEmpty()) {
            sender.sendMessage(messages.get("reset.player-not-found"));
            return;
        }
        if (!confirmed) {
            sender.sendMessage(messages.get("reset.confirm-required"));
            return;
        }
        dpsManager.resetPlayer(record.get().getUuid());
        sender.sendMessage(messages.get("reset.player-done", "player", record.get().getName()));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("mofudps.reload")) {
            sender.sendMessage(messages.get("no-permission"));
            return;
        }
        messages.load();
        sender.sendMessage(messages.get("reloaded"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(List.of("measure", "ranking", "reset", "reload"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            List<String> names = new ArrayList<>();
            names.add("all");
            for (PlayerRecord record : dpsManager.getRanking(Integer.MAX_VALUE)) {
                names.add(record.getName());
            }
            return filterStartsWith(names, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("reset")) {
            return filterStartsWith(List.of("confirm"), args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> filterStartsWith(List<String> options, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
