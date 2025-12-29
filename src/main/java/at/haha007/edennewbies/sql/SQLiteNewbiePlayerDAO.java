package at.haha007.edennewbies.sql;

import at.haha007.edennewbies.NewbiePlayer;
import at.haha007.edennewbies.NewbiePlayerDAO;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SQLiteNewbiePlayerDAO implements NewbiePlayerDAO {
    private final File dbFile;
    private final String jdbcUrl;

    public SQLiteNewbiePlayerDAO(File dbFile) {
        this.dbFile = dbFile;
        this.jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        init();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void init() {
        try {
            File parent = dbFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            if (!dbFile.exists()) dbFile.createNewFile();

            try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS newbie_players (
                                uuid TEXT PRIMARY KEY,
                                play_time INTEGER
                            );
                            """);
                }
            }
        } catch (Exception e) {
            throw new DAOException("Failed to initialize SQLite database", e);
        }
    }

    @Override
    public Optional<NewbiePlayer> getByUUID(UUID uuid) {
        String sql = "SELECT uuid, play_time FROM newbie_players WHERE uuid = ?";
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    NewbiePlayer p = mapRow(rs);
                    return Optional.of(p);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DAOException("Failed to get player by UUID", e);
        }
    }

    @Override
    public void saveOrUpdate(NewbiePlayer player) {
        String sql = "INSERT INTO newbie_players(uuid, play_time) VALUES(?,?)"
                + " ON CONFLICT(uuid) DO UPDATE SET play_time=excluded.play_time;";
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player.getUuid().toString());
            ps.setLong(2, player.getLastCheckedPlaytime());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("Failed to saveOrUpdate player", e);
        }
    }

    @Override
    public boolean remove(UUID uuid) {
        String sql = "DELETE FROM newbie_players WHERE uuid = ?";
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            int updated = ps.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            throw new DAOException("Failed to remove player", e);
        }
    }

    @Override
    public List<NewbiePlayer> listAll() {
        String sql = "SELECT uuid, play_time FROM newbie_players";
        List<NewbiePlayer> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DAOException("Failed to listAll players", e);
        }
    }

    private NewbiePlayer mapRow(ResultSet rs) throws SQLException {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        NewbiePlayer p = new NewbiePlayer(uuid);
        p.setLastCheckedPlaytime(rs.getLong("play_time"));
        return p;
    }
}
