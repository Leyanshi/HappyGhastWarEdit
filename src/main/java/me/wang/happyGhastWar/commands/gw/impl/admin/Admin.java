package me.wang.happyGhastWar.commands.gw.impl.admin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.commands.gw.GWCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Stream;

public class Admin extends GWCommand {
    private static final List<GWCommand> COMMANDS = ImmutableList.of(new Admin() ,new Create(), new SetSpawn(), new SetGhastSpawn(), new SetGhastAmount(), new SetWait(), new SetCenter(),new SetRadius(), new SetTargetRadius(), new BackUp(), new Disable(),new Enable(),new AddChest(),new RemoveChest());
    private final Map<String,GWCommand> commands;
    public Admin(){
        super("admin",true, new String[0]);

        ImmutableMap.Builder<String, GWCommand> commands = ImmutableMap.builder();

        for(GWCommand command : COMMANDS) {
            command.getCommands().forEach((label) -> commands.put((String) label, command));
        }

        this.commands = commands.build();
    }

    public void evaluate(HappyGhastWar happyGhastWar, Player commandSender, String s, List<String> params) {

        if (params.isEmpty()) {
            for (String help : happyGhastWar.getLanguage(commandSender).getTranslatedList("commands.admin-list")){
                commandSender.sendMessage(help);
            }
        } else {
            String search = ((String)params.get(0)).toLowerCase(Locale.ROOT);
            GWCommand target = (GWCommand) this.commands.get(search);
            if (target == null) {
                commandSender.sendMessage("&cUnknown command &7admin " + search);
            } else {
                String permission = target.getPermission();
                if (permission != null && !permission.isEmpty() && !commandSender.hasPermission(permission)) {
                    commandSender.sendMessage("&cYou do not have permission to do this!");
                }else {
                    target.evaluate(happyGhastWar,(Player) commandSender, search, params.subList(1, params.size()));
                }
            }
        }
    }

    public void complete(HappyGhastWar plugin, CommandSender sender, String alias, List<String> params, List<String> suggestions) {
        if (params.size() <= 1) {
            Stream<String> targets = filterByPermission(sender, this.commands.values().stream()).map(GWCommand::getCommands).flatMap(Collection::stream);
            suggestByParameter(targets, suggestions, params.isEmpty() ? null : (String)params.get(0));
        } else {
            String search = ((String)params.get(0)).toLowerCase(Locale.ROOT);
            GWCommand target = (GWCommand) this.commands.get(search);
            if (target != null) {
                target.complete(plugin, sender, search, params.subList(1, params.size()), suggestions);
            }
        }
    }

    static {
        COMMANDS.forEach((command) -> command.setPermission("gw.admin." + command.getCommand()));
    }
}
