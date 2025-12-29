package at.haha007.edennewbies;

import java.util.UUID;

public class NewbiePlayer {
    private final UUID uuid;
    private long timePlayedAtLastSync;
    private long lastSyncTimestamp;
    private long lastCheckedPlaytime;

    public NewbiePlayer(UUID uuid) {
        this.uuid = uuid;
        lastSyncTimestamp = System.currentTimeMillis();
    }

    public void updateLastSyncTimestamp() {
        this.lastSyncTimestamp = System.currentTimeMillis();
    }

    public long getTotalTimePlayed() {
        return timePlayedAtLastSync + (System.currentTimeMillis() - lastSyncTimestamp);
    }

    public long getLastCheckedPlaytime() {
        return lastCheckedPlaytime;
    }

    public void setLastCheckedPlaytime(long lastCheckedPlaytime) {
        this.lastCheckedPlaytime = lastCheckedPlaytime;
    }

    public UUID getUuid() {
        return uuid;
    }

    public long getTimePlayedAtLastSync() {
        return timePlayedAtLastSync;
    }

    public void setTimePlayedAtLastSync(long timePlayedAtLastSync) {
        this.timePlayedAtLastSync = timePlayedAtLastSync;
    }
}
