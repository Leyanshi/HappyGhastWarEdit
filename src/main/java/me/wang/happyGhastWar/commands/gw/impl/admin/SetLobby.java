package me.wang.happyGhastWar.commands.gw.impl.admin;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.arena.ArenaConfig;
import me.wang.happyGhastWar.commands.gw.GWCommand;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

public class SetLobby extends GWCommand {
    public SetLobby(){
        super("setlobby", new String[0]);
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        World world = player.getWorld();
        if (ghastWar.arenas.containsKey(world.getName())){
            player.sendMessage(ghastWar.getLanguage(player).getContent("commands.lobby-not-allow"));
            return;
        }
        ghastWar.setLobby(player.getLocation());

        player.sendMessage(ghastWar.getLanguage(player).getContent("commands.setSuccess"));
    }
}
