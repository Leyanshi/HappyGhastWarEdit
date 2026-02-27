package me.wang.happyGhastWar.commands.gw.impl;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.arena.ArenaConfig;
import me.wang.happyGhastWar.commands.gw.GWCommand;
import org.bukkit.entity.Player;

import java.util.List;

public class Join extends GWCommand {
    public Join(){
        super("join", true, new String[0]);
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        if (ghastWar.getConfig().getBoolean("bungee.enable",false) && ghastWar.getConfig().getBoolean("bungee.can-select-game",false)){
            player.sendMessage(ghastWar.getLanguage(player).getContent("commands.cant-select-game"));
            return;
        }
        if (params.isEmpty()){
            player.sendMessage(ghastWar.getLanguage(player).getContent("commands.missRequireData"));
            return;
        }
        if (!ghastWar.arenas.containsKey(params.get(0))){
            player.sendMessage(ghastWar.getLanguage(player).getContent("commands.gameNotFound"));
            return;
        }

        Arena arena = ghastWar.arenas.get(params.get(0));
        ArenaConfig arenaConfig = arena.getArenaConfig();
        player.teleport(arenaConfig.getWait());
        arena.internalAddPlayer(player);
    }


}
