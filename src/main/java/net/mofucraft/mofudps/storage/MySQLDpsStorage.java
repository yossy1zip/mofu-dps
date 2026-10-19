package net.mofucraft.mofudps.storage;

import net.mofucraft.mofudps.dps.PlayerRecord;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MySQLDpsStorage implements DpsStorage {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean useSSL;
    private final String table;

    private Connection connection;

    public MySQLDpsStorage(String host, int port, String database, String username, String password,
                            boolean useSSL, String tablePrefix) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.useSSL = useSSL;
        this.table = tablePrefix + "players";
    }

    @Override
    public void init() throws Exception {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=" + useSSL + "&autoReconnect=true&characterEncoding=utf8";
        connection = DriverManager.getConnection(url, username, password);
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS `" + table + "` (" +
                    "uuid CHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(16) NOT NULL, " +
                    "max_dps DOUBLE NOT NULL, " +
                    "updated_at BIGINT NOT NULL" +
                    ")");
        }
    }

    @Override
    public Map<UUID, PlayerRecord> loadAll() throws Exception {
        Map<UUID, PlayerRecord> result = new HashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT uuid, name, max_dps FROM `" + table + "`")) {
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
                "INSERT INTO `" + table + "` (uuid, name, max_dps, updated_at) VALUES (?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE name = VALUES(name), max_dps = VALUES(max_dps), updated_at = VALUES(updated_at)")) {
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
            statement.execute("DELETE FROM `" + table + "`");
        }
    }

    @Override
    public void resetPlayer(UUID uuid) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM `" + table + "` WHERE uuid = ?")) {
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
