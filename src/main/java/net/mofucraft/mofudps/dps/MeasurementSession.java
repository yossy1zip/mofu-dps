package net.mofucraft.mofudps.dps;

/**
 * /mofudps measure コマンド専用の一時計測セッション。
 * 直近5秒平均DPSやランキングとは完全に独立して合計ダメージのみを積算する。
 */
public final class MeasurementSession {

    private double totalDamage = 0;

    public synchronized void addDamage(double amount) {
        totalDamage += amount;
    }

    public synchronized double getTotalDamage() {
        return totalDamage;
    }
}
