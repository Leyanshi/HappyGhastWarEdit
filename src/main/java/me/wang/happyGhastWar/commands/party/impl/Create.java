package me.wang.happyGhastWar.commands.party.impl;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.commands.party.PartyCommand;
import me.wang.happyGhastWar.game.party.Party;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

public class Create extends PartyCommand {
    public Create(){
        super("create",true, new String[0]);
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        if (HappyGhastWar.arenas.containsKey(player.getWorld().getName())){
            player.sendMessage(ghastWar.getLanguage(player).getContent("party.unable-use-in-game"));
            return;
        }
        Party party = ghastWar.getPartyManager().createParty(player);
        if (party == null) {
            player.sendMessage(ChatColor.RED + "你已经在队伍中!");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "已创建队伍!");
        player.sendMessage(ChatColor.YELLOW + "使用 /party invite <玩家名> 邀请其他玩家");
    }
}
