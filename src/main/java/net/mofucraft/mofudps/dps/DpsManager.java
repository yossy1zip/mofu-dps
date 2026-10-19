package net.mofucraft.mofudps.dps;

import net.mofucraft.mofudps.storage.DpsStorage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * ダメージ計測(直近5秒平均DPS)・過去最大DPSの永続化キャッシュ・
 * /mofudps measure 用の一時セッションを一元管理する。
 */
public final class DpsManager {

    private static final long SAMPLING_PERIOD_TICKS = 4L;

    private final JavaPlugin plugin;
    private final DpsStorage storage;
    private final int windowSeconds;

    private final Map<UUID, DpsTracker> trackers = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerRecord> cache = new ConcurrentHashMap<>();
    private final Map<UUID, MeasurementSession> measurementSessions = new ConcurrentHashMap<>();
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "MofuDPS-Storage");
        thread.setDaemon(true);
        return thread;
    });

    private BukkitTask samplingTask;

    public DpsManager(JavaPlugin plugin, DpsStorage storage, int windowSeconds) {
        this.plugin = plugin;
        this.storage = storage;
        this.windowSeconds = windowSeconds;
    }

    public void init() {
        dbExecutor.submit(() -> {
            try {
                storage.init();
                Map<UUID, PlayerRecord> loaded = storage.loadAll();
                cache.putAll(loaded);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "ストレージの初期化に失敗しました", e);
            } finally {
                plugin.getServer().getScheduler().runTask(plugin, this::startSamplingTask);
            }
        });
    }

    private void startSamplingTask() {
        samplingTask = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::sampleAll, SAMPLING_PERIOD_TICKS, SAMPLING_PERIOD_TICKS);
    }

    public void shutdown() {
        if (samplingTask != null) {
            samplingTask.cancel();
        }
        dbExecutor.shutdown();
        try {
            storage.close();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "ストレージのクローズに失敗しました", e);
        }
    }

    public void recordDamage(Player attacker, double amount) {
        UUID uuid = attacker.getUniqueId();
        trackers.computeIfAbsent(uuid, u -> new DpsTracker(windowSeconds)).addDamage(amount);

        MeasurementSession session = measurementSessions.get(uuid);
        if (session != null) {
            session.addDamage(amount);
        }
    }

    private void sampleAll() {
        for (Map.Entry<UUID, DpsTracker> entry : trackers.entrySet()) {
            UUID uuid = entry.getKey();
            double current = entry.getValue().getCurrentDps();
            if (current <= 0) {
                continue;
            }

            PlayerRecord record = cache.get(uuid);
            if (record != null && current <= record.getMaxDps()) {
                continue;
            }

            Player player = plugin.getServer().getPlayer(uuid);
            String name = player != null ? player.getName() : (record != null ? record.getName() : uuid.toString());

            if (record == null) {
                record = new PlayerRecord(uuid, name, current);
                cache.put(uuid, record);
            } else {
                record.setName(name);
                record.setMaxDps(current);
            }
            persist(record);
        }
    }

    private void persist(PlayerRecord record) {
        UUID uuid = record.getUuid();
        String name = record.getName();
        double maxDps = record.getMaxDps();
        dbExecutor.submit(() -> {
            try {
                storage.upsertMaxDps(uuid, name, maxDps);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "DPS記録の保存に失敗しました", e);
            }
        });
    }

    public double getCurrentDps(UUID uuid) {
        DpsTracker tracker = trackers.get(uuid);
        return tracker != null ? tracker.getCurrentDps() : 0.0;
    }

    public double getMaxDps(UUID uuid) {
        PlayerRecord record = cache.get(uuid);
        return record != null ? record.getMaxDps() : 0.0;
    }

    public List<PlayerRecord> getRanking(int count) {
        List<PlayerRecord> list = new ArrayList<>(cache.values());
        list.sort((a, b) -> Double.compare(b.getMaxDps(), a.getMaxDps()));
        int limit = Math.max(count, 0);
        if (list.size() > limit) {
            return list.subList(0, limit);
        }
        return list;
    }

    public int getRank(UUID uuid) {
        List<PlayerRecord> list = new ArrayList<>(cache.values());
        list.sort((a, b) -> Double.compare(b.getMaxDps(), a.getMaxDps()));
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getUuid().equals(uuid)) {
                return i + 1;
            }
        }
        return -1;
    }

    public Optional<PlayerRecord> findByName(String name) {
        return cache.values().stream()
                .filter(record -> record.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public boolean hasActiveMeasurement(UUID uuid) {
        return measurementSessions.containsKey(uuid);
    }

    public void startMeasurement(UUID uuid) {
        measurementSessions.put(uuid, new MeasurementSession());
    }

    public double endMeasurement(UUID uuid) {
        MeasurementSession session = measurementSessions.remove(uuid);
        return session != null ? session.getTotalDamage() : 0.0;
    }

    public void resetAll() {
        cache.clear();
        trackers.clear();
        dbExecutor.submit(() -> {
            try {
                storage.resetAll();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "全記録のリセットに失敗しました", e);
            }
        });
    }

    public void resetPlayer(UUID uuid) {
        cache.remove(uuid);
        trackers.remove(uuid);
        dbExecutor.submit(() -> {
            try {
                storage.resetPlayer(uuid);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "記録のリセットに失敗しました", e);
            }
        });
    }
}
