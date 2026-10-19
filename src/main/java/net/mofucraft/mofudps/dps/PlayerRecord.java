package net.mofucraft.mofudps.dps;

import java.util.UUID;

/**
 * プレイヤーごとの永続DPS記録(ランキング/キャッシュ用)。
 */
public final class PlayerRecord {

    private final UUID uuid;
    private volatile String name;
    private volatile double maxDps;

    public PlayerRecord(UUID uuid, String name, double maxDps) {
        this.uuid = uuid;
        this.name = name;
        this.maxDps = maxDps;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getMaxDps() {
        return maxDps;
    }

    public void setMaxDps(double maxDps) {
        this.maxDps = maxDps;
    }
}
