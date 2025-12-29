package at.haha007.edennewbies;

import at.haha007.edennewbies.sql.SQLiteNewbiePlayerDAO;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Map;

import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

public final class EdenNewbiesPlugin extends JavaPlugin {

    public static final String PERMISSION_MESSAGE = "You don't have permission to use this command.";
    private NewbiePlayerDAO newbieDao;
    private PlayerTimeActionRunner playerTimeActionRunner;
    private Map<Integer, List<Action>> actions;

    @Override
    public void onEnable() {
        // Create data folder if it doesn't exist
        if (!getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            getDataFolder().mkdirs();
        }
        // Initialize the SQLiteNewbiePlayerDAO
        File dbFile = new File(getDataFolder(), "players.db");
        this.newbieDao = new SQLiteNewbiePlayerDAO(dbFile);
        getLogger().info("Initialized NewbiePlayerDAO with DB: " + dbFile.getAbsolutePath());

        //load config and start runner
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        reloadConfig();
        actions = ActionFactory.createActionsMapFromConfig(getConfig());
        playerTimeActionRunner = new PlayerTimeActionRunner(this, actions);

        //register commands
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
                this::registerCommands);
    }

    private void registerCommands(ReloadableRegistrarEvent<Commands> commands) {
        //reload command
        LiteralArgumentBuilder<CommandSourceStack> rootCommand = literal("edennewbies");
        rootCommand.then(literal("reload").executes(c -> {
            CommandSender sender = c.getSource().getSender();
            if (!sender.hasPermission("edennewbies.reload")) {
                sender.sendMessage(Component.text(PERMISSION_MESSAGE, NamedTextColor.RED));
                return 1;
            }
            reloadConfig();
            actions = ActionFactory.createActionsMapFromConfig(getConfig());
            playerTimeActionRunner.setActionsMap(actions);
            sender.sendMessage(Component.text("EdenNewbies configuration reloaded.", NamedTextColor.GREEN));
            return 0;
        }));

        //reset command, which resets the player's newbie status
        rootCommand.then(literal("reset").executes(c -> {
            CommandSender sender = c.getSource().getSender();
            if (!sender.hasPermission("edennewbies.reset")) {
                sender.sendMessage(Component.text(PERMISSION_MESSAGE, NamedTextColor.RED));
                return 1;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
                return 1;
            }
            playerTimeActionRunner.resetPlayer(player.getUniqueId());
            sender.sendMessage(Component.text("Your newbie status has been reset.", NamedTextColor.GREEN));
            return 0;
        }));

        //test command to test actions
        rootCommand.then(literal("test").then(argument("delay", IntegerArgumentType.integer())
                .suggests((ctx, builder) -> {
                    for (Integer delay : actions.keySet()) {
                        builder.suggest(delay.toString());
                    }
                    return builder.buildFuture();
                })
                .executes(c -> {
                    CommandSender sender = c.getSource().getSender();
                    if (!sender.hasPermission("edennewbies.test")) {
                        sender.sendMessage(Component.text(PERMISSION_MESSAGE, NamedTextColor.RED));
                        return 1;
                    }
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
                        return 1;
                    }
                    int delay = c.getArgument("delay", int.class);
                    List<Action> actionList = actions.get(delay);
                    if (actionList == null || actionList.isEmpty()) {
                        sender.sendMessage(Component.text("No actions found for delay: " + delay, NamedTextColor.RED));
                        return 1;
                    }
                    NewbiePlayer tempNewbiePlayer = new NewbiePlayer(player.getUniqueId());
                    for (Action action : actionList) {
                        action.execute(tempNewbiePlayer, player);
                    }
                    Bukkit.getScheduler().runTask(this, () ->
                            sender.sendMessage(Component.text("Executed test actions for delay: " + delay, NamedTextColor.GREEN)));
                    return 0;
                })
        ));

        commands.registrar().register(rootCommand.build());
    }

    @Override
    public void onDisable() {
        playerTimeActionRunner.saveNow();
    }

    public NewbiePlayerDAO getNewbieDao() {
        return newbieDao;
    }
}
