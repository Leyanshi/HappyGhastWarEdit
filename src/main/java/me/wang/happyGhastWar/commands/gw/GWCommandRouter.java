package me.wang.happyGhastWar.commands.gw;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.commands.gw.impl.GUI;
import me.wang.happyGhastWar.commands.gw.impl.Join;
import me.wang.happyGhastWar.commands.gw.impl.Leave;
import me.wang.happyGhastWar.commands.gw.impl.admin.Admin;
import me.wang.happyGhastWar.commands.gw.impl.debug.Debug;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Stream;

public class GWCommandRouter implements CommandExecutor, TabExecutor {
    private static final List<GWCommand> COMMANDS = ImmutableList.of(new Join(), new Admin(), new Debug(), new GUI(), new Leave());
    private final HappyGhastWar plugin;
    private final Map<String, GWCommand> commands;

    public GWCommandRouter(HappyGhastWar plugin){
        this.plugin = plugin;
        ImmutableMap.Builder<String, GWCommand> commands = ImmutableMap.builder();

        for(GWCommand command : COMMANDS) {
            command.getCommands().forEach((label) -> commands.put(label.toString(), command));
        }

        this.commands = commands.build();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (args.length == 0) {
            for (String help : plugin.getLanguage(sender instanceof Player ? (Player) sender : null).getTranslatedList("commands.main-list")){
                sender.sendMessage(help);
            }
            return true;
        } else {
            String search = args[0].toLowerCase(Locale.ROOT);
            GWCommand target = (GWCommand) this.commands.get(search);
            if (target == null) {
                sender.sendMessage("&cUnknown command &7" + search);
                return true;
            } else {
                String permission = target.getPermission();
                if (permission != null && !permission.isEmpty() && !sender.hasPermission(permission)) {
                    sender.sendMessage("&cYou do not have permission to do this!");
                    return true;
                } else {
                    if (sender instanceof Player){
                        target.evaluate(this.plugin, (Player) sender, search, Arrays.asList(Arrays.copyOfRange(args, 1, args.length)));
                        return true;
                    }else {
                        if (target.isPlayerSender()){
                            sender.sendMessage("该命令只能由玩家执行");
                            return true;
                        }
                        target.evaluate(this.plugin, sender, search, Arrays.asList(Arrays.copyOfRange(args, 1, args.length)));
                        return true;
                    }
                }
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args) {
        List<String> suggestions = new ArrayList();
        if (args.length > 1) {
            GWCommand target = (GWCommand) this.commands.get(args[0].toLowerCase(Locale.ROOT));
            if (target != null) {
                target.complete(this.plugin, sender, args[0].toLowerCase(Locale.ROOT), Arrays.asList(Arrays.copyOfRange(args, 1, args.length)), suggestions);
            }

            return suggestions;
        } else {
            Stream<String> targets = GWCommand.filterByPermission(sender, this.commands.values().stream()).map(GWCommand::getCommand).map(Object::toString);
            GWCommand.suggestByParameter(targets, suggestions, args.length == 0 ? null : args[0]);
            return suggestions;
        }
    }
}


