package at.haha007.edennewbies;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ActionFactory {

    private ActionFactory() {
        throw new IllegalStateException("Utility class");
    }

    public static Map<Integer, List<Action>> createActionsMapFromConfig(ConfigurationSection config) {
        Set<String> delays = config.getKeys(false);
        Map<Integer, List<Action>> actionsMap = new HashMap<>();
        for (String delayStr : delays) {
            int delay;
            try {
                delay = Integer.parseInt(delayStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid delay key: " + delayStr);
            }
            List<ConfigurationSection> actionConfigs = config.getMapList(delayStr).stream()
                    .map(ConfigurationSection.class::cast)
                    .toList();
            List<Action> actions = createActionsFromConfig(actionConfigs);
            actionsMap.put(delay, actions);
        }
        return actionsMap;
    }

    public static List<Action> createActionsFromConfig(List<ConfigurationSection> configList) {
        return configList.stream()
                .map(ActionFactory::createFromConfig)
                .toList();
    }

    public static Action createFromConfig(ConfigurationSection config) {
        String type = config.getString("action");
        switch (type) {
            case "send-message":
                String message = config.getString("message");
                if (message == null) {
                    throw new IllegalArgumentException("Missing 'message' for send-message action");
                }
                return createSendMessageAction(message);
            case "sound":
                String soundName = config.getString("sound");
                float volume = (float) config.getDouble("volume", 1.0);
                float pitch = (float) config.getDouble("pitch", 1.0);
                if (soundName == null) {
                    throw new IllegalArgumentException("Missing 'sound' for sound action");
                }
                return createSoundAction(soundName, volume, pitch);
            case "title":
                String title = config.getString("title", "");
                String subtitle = config.getString("subtitle", "");
                double fadeInSeconds = config.getDouble("fade_in", 0.5);
                double staySeconds = config.getDouble("stay", 2.0);
                double fadeOutSeconds = config.getDouble("fade_out", 0.5);
                if (title.isEmpty() && subtitle.isEmpty()) {
                    throw new IllegalArgumentException("At least one of 'title' or 'subtitle' must be provided for title action");
                }
                return createTitleAction(title, subtitle, fadeInSeconds, staySeconds, fadeOutSeconds);
            case "actionbar":
                String actionbarMessage = config.getString("message");
                if (actionbarMessage == null) {
                    throw new IllegalArgumentException("Missing 'message' for actionbar action");
                }
                return createActionbarAction(actionbarMessage);
            case "server_command":
                String command = config.getString("command");
                if (command == null) {
                    throw new IllegalArgumentException("Missing 'command' for server_command action");
                }
                return createServerCommandAction(command);
            default:
                throw new IllegalArgumentException("Unknown action type: " + type);
        }
    }

    private static Action createServerCommandAction(String command) {
        return (newbie, player) -> {
            String finalCommand = PlaceholderAPI.setPlaceholders(player, command);
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), finalCommand);
        };
    }

    private static Action createTitleAction(String title,
                                            String subtitle,
                                            double fadeInSeconds,
                                            double staySeconds,
                                            double fadeOutSeconds) {
        return (newbie, player) -> {
            Title.Times times = Title.Times.times(
                    Duration.ofMillis((long) (fadeInSeconds * 1000)),
                    Duration.ofMillis((long) (staySeconds * 1000)),
                    Duration.ofMillis((long) (fadeOutSeconds * 1000))
            );
            Component titleComponent = MiniMessage.miniMessage().deserialize(title);
            Component subtitleComponent = MiniMessage.miniMessage().deserialize(subtitle);
            Title adventureTitle = Title.title(titleComponent, subtitleComponent, times);
            player.showTitle(adventureTitle);
        };
    }

    private static Action createActionbarAction(String message) {
        return (newbie, player) -> {
            String playerMessageString = PlaceholderAPI.setPlaceholders(player, message);
            Component component = MiniMessage.miniMessage().deserialize(playerMessageString);
            player.sendActionBar(component);
        };
    }

    private static Action createSoundAction(String soundName, float volume, float pitch) {
        return (newbie, player) -> {
            @SuppressWarnings("PatternValidation")
            Sound sound = Sound.sound(Key.key(soundName), Sound.Source.MASTER, volume, pitch);
            player.playSound(sound);
        };
    }

    private static Action createSendMessageAction(String message) {
        return (newbie, player) -> {
            String playerMessageString = PlaceholderAPI.setPlaceholders(player, message);
            Component component = MiniMessage.miniMessage().deserialize(playerMessageString);
            player.sendMessage(component);
        };
    }
}
