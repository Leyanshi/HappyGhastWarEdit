package me.wang.happyGhastWar.commands.gw.impl.admin;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.arena.ArenaConfig;
import me.wang.happyGhastWar.commands.gw.GWCommand;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

public class SetTargetRadius extends GWCommand {
    public SetTargetRadius(){
        super("settargetradius", new String[0]);
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        World world = player.getWorld();
        if (!ghastWar.arenas.containsKey(world.getName())){
            player.sendMessage(ghastWar.getLanguage(player).getContent("commands.gameNotFound"));
            return;
        }
        if (params.size() < 1){
            player.sendMessage(ghastWar.getLanguage(player).getContent("commands.missRequireData"));
            return;
        }

        Arena arena = ghastWar.arenas.get(world.getName());
        ArenaConfig arenaConfig = arena.getArenaConfig();

        arenaConfig.setTargetRadius(Integer.valueOf(params.get(0)));

        player.sendMessage(ghastWar.getLanguage(player).getContent("commands.setSuccess"));
    }
}
