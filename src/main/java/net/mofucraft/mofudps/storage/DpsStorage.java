package net.mofucraft.mofudps.storage;

import net.mofucraft.mofudps.dps.PlayerRecord;

import java.util.Map;
import java.util.UUID;

/**
 * プレイヤーごとの過去最大DPSを永続化するストレージ層。
 * すべてのメソッドは呼び出し元(DpsManagerの専用ワーカースレッド)によって直列に呼び出される前提。
 */
public interface DpsStorage {

    void init() throws Exception;

    Map<UUID, PlayerRecord> loadAll() throws Exception;

    void upsertMaxDps(UUID uuid, String name, double maxDps) throws Exception;

    void resetAll() throws Exception;

    void resetPlayer(UUID uuid) throws Exception;

    void close() throws Exception;
}
