package at.haha007.edennewbies;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerTimeActionRunner implements Listener {
    private final NewbiePlayerDAO playerDAO;
    private Map<Integer, List<Action>> actionsMap;

    //amount of online players should always be small, so ArrayList is fine
    private final List<NewbiePlayer> onlineNewbiePlayers = new ArrayList<>();

    public PlayerTimeActionRunner(EdenNewbiesPlugin plugin, Map<Integer, List<Action>> actionsMap) {
        this.playerDAO = plugin.getNewbieDao();
        this.actionsMap = actionsMap;
        //schedule task to run every second
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::run, 20L, 20L);
        //schedule save task every 5 minutes
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin,
                () -> Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveNow),
                6000L,
                6000L);
    }

    private void run() {
        for (NewbiePlayer newbiePlayer : onlineNewbiePlayers) {
            Player player = Bukkit.getPlayer(newbiePlayer.getUuid());
            if (player == null) continue; //player is offline
            long totalTimePlayed = newbiePlayer.getTotalTimePlayed();
            long lastChecked = newbiePlayer.getLastCheckedPlaytime();
            for (Map.Entry<Integer, List<Action>> entry : actionsMap.entrySet()) {
                int requiredTime = entry.getKey();
                if (lastChecked < requiredTime && totalTimePlayed >= requiredTime) {
                    for (Action action : entry.getValue()) {
                        action.execute(newbiePlayer,player);
                    }
                }
            }
            newbiePlayer.setLastCheckedPlaytime(totalTimePlayed);
        }
    }

    public void saveNow() {
        for (NewbiePlayer p : onlineNewbiePlayers) {
            playerDAO.saveOrUpdate(p);
        }
        //remove offline players from the list
        onlineNewbiePlayers.removeIf(p -> Bukkit.getPlayer(p.getUuid()) == null);
    }

    public void setActionsMap(Map<Integer, List<Action>> actionsMap) {
        this.actionsMap = actionsMap;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!event.getPlayer().hasPlayedBefore()) {
            NewbiePlayer newbiePlayer = new NewbiePlayer(event.getPlayer().getUniqueId());
            newbiePlayer.setTimePlayedAtLastSync(-1);
            newbiePlayer.setLastCheckedPlaytime(-1);
            onlineNewbiePlayers.add(newbiePlayer);
        }
        playerDAO.getByUUID(event.getPlayer().getUniqueId()).ifPresent(onlineNewbiePlayers::add);
    }

    public void resetPlayer(UUID uuid) {
        onlineNewbiePlayers.removeIf(p -> p.getUuid().equals(uuid));
        NewbiePlayer newbiePlayer = new NewbiePlayer(uuid);
        newbiePlayer.setTimePlayedAtLastSync(-1);
        newbiePlayer.setLastCheckedPlaytime(-1);
        onlineNewbiePlayers.add(newbiePlayer);
    }
}
