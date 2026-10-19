package net.mofucraft.mofudps.storage;

import net.mofucraft.mofudps.dps.PlayerRecord;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SQLiteDpsStorage implements DpsStorage {

    private static final String TABLE = "mofudps_players";

    private final File databaseFile;
    private Connection connection;

    public SQLiteDpsStorage(File databaseFile) {
        this.databaseFile = databaseFile;
    }

    @Override
    public void init() throws Exception {
        File parent = databaseFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "max_dps REAL NOT NULL, " +
                    "updated_at INTEGER NOT NULL" +
                    ")");
        }
    }

    @Override
    public Map<UUID, PlayerRecord> loadAll() throws Exception {
        Map<UUID, PlayerRecord> result = new HashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT uuid, name, max_dps FROM " + TABLE)) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                result.put(uuid, new PlayerRecord(uuid, rs.getString("name"), rs.getDouble("max_dps")));
            }
        }
        return result;
    }

    @Override
    public void upsertMaxDps(UUID uuid, String name, double maxDps) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO " + TABLE + " (uuid, name, max_dps, updated_at) VALUES (?, ?, ?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET name = excluded.name, max_dps = excluded.max_dps, updated_at = excluded.updated_at")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            statement.setDouble(3, maxDps);
            statement.setLong(4, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    @Override
    public void resetAll() throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("DELETE FROM " + TABLE);
        }
    }

    @Override
    public void resetPlayer(UUID uuid) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM " + TABLE + " WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        }
    }

    @Override
    public void close() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
