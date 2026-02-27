package me.wang.happyGhastWar.commands.gw.impl;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.commands.gw.GWCommand;
import org.bukkit.entity.Player;

import java.util.List;

public class GUI extends GWCommand {
    public GUI(){
        super("gui", true, new String[0]);
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        if (ghastWar.getConfig().getBoolean("bungee.enable",false) && ghastWar.getConfig().getBoolean("bungee.can-select-game",false)){
            player.sendMessage(HappyGhastWar.language.getContent("commands.cant-select-game"));
            return;
        }
        ghastWar.getArenaSelector().openSelector(player);
    }
}
