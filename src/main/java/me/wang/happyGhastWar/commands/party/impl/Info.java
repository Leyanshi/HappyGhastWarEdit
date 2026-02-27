package me.wang.happyGhastWar.commands.party.impl;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.commands.party.PartyCommand;
import me.wang.happyGhastWar.game.party.Party;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

public class Info extends PartyCommand {
    public Info(){
        super("info",true, new String[0]);
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        Party party = ghastWar.getPartyManager().getParty(player);
        if (party == null) {
            player.sendMessage(ChatColor.YELLOW + "你不在任何队伍中");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== 队伍信息 ===");
        player.sendMessage(ChatColor.YELLOW + "队长: " + ChatColor.GREEN + party.getLeader().getName());
        player.sendMessage(ChatColor.YELLOW + "成员 (" + party.getMemberCount() + "/" + party.getMaxSize() + "):");

        for (java.util.Map.Entry<java.util.UUID, Party.PartyRole> entry : party.getMembers().entrySet()) {
            Player member = Bukkit.getPlayer(entry.getKey());
            if (member != null) {
                String role = entry.getValue() == Party.PartyRole.LEADER ?
                        ChatColor.RED + "[队长]" : ChatColor.GRAY + "[队员]";
                player.sendMessage("  " + role + " " + ChatColor.WHITE + member.getName());
            }
        }
    }
}
