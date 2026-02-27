package me.wang.happyGhastWar.commands.party.impl;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.commands.party.PartyCommand;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

public class Disband extends PartyCommand {
    public Disband(){
        super("disband",true, new String[0]);
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        if (ghastWar.getPartyManager().disbandParty(player)) {
            player.sendMessage(ChatColor.RED + "已解散队伍!");
        } else {
            player.sendMessage(ChatColor.RED + "你不是队长或没有队伍!");
        }
    }
}
