package at.haha007.edennewbies;

import java.util.UUID;

public class NewbiePlayer {
    private final UUID uuid;
    private long lastCheckedPlaytime;

    public NewbiePlayer(UUID uuid) {
        this.uuid = uuid;
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

}
