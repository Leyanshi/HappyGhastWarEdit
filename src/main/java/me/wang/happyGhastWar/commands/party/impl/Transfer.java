package me.wang.happyGhastWar.commands.party.impl;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.commands.party.PartyCommand;
import me.wang.happyGhastWar.game.party.Party;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

public class Transfer extends PartyCommand {
    public Transfer(){
        super("transfer",true, new String[0]);
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        if (HappyGhastWar.arenas.containsKey(player.getWorld().getName())){
            player.sendMessage(HappyGhastWar.language.getContent("party.unable-use-in-game"));
            return;
        }
        if (params.isEmpty()) return;
        Party party = ghastWar.getPartyManager().getParty(player);
        if (party == null) {
            player.sendMessage(ChatColor.RED + "你没有队伍!");
            return;
        }

        party.transferLeadership(player, params.get(0));
    }
}
