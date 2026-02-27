package me.wang.happyGhastWar.commands.gw.impl.admin;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.arena.ArenaConfig;
import me.wang.happyGhastWar.commands.gw.GWCommand;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SetSpawn extends GWCommand {
    public SetSpawn(){
        super("setspawn", new String[0]);
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

        List<String> teams = new ArrayList<>();
        Arrays.stream(Arena.Teams.values()).forEach(teams1 -> {
            teams.add(teams1.name());
        });

        if (!teams.contains(params.get(0))){
            player.sendMessage(ghastWar.getLanguage(player).getContent("commands.teamNotFound"));
            player.sendMessage(ghastWar.getLanguage(player).getContent("commands.availableTeams"));
            return;
        }
        Arena arena = ghastWar.arenas.get(world.getName());
        ArenaConfig arenaConfig = arena.getArenaConfig();

        arenaConfig.setTeamSpawn(Arena.Teams.valueOf(params.get(0)),player.getLocation());

        player.sendMessage(ghastWar.getLanguage(player).getContent("commands.setSuccess"));
    }
}
