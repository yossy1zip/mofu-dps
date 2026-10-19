package net.mofucraft.mofudps.dps;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * プレイヤー1人分の直近ダメージ履歴を保持し、スライディングウィンドウ平均DPSを計算する。
 */
public final class DpsTracker {

    private record DamageEntry(long timestampMillis, double amount) {
    }

    private final Deque<DamageEntry> entries = new ArrayDeque<>();
    private final long windowMillis;
    private final double windowSeconds;

    public DpsTracker(int windowSeconds) {
        this.windowSeconds = windowSeconds;
        this.windowMillis = windowSeconds * 1000L;
    }

    public synchronized void addDamage(double amount) {
        long now = System.currentTimeMillis();
        entries.addLast(new DamageEntry(now, amount));
        prune(now);
    }

    public synchronized double getCurrentDps() {
        long now = System.currentTimeMillis();
        prune(now);
        double sum = 0;
        for (DamageEntry entry : entries) {
            sum += entry.amount();
        }
        return sum / windowSeconds;
    }

    private void prune(long now) {
        while (!entries.isEmpty() && now - entries.peekFirst().timestampMillis() > windowMillis) {
            entries.pollFirst();
        }
    }
}
