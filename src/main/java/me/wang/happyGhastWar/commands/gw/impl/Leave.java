package me.wang.happyGhastWar.commands.gw.impl;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.arena.ArenaConfig;
import me.wang.happyGhastWar.commands.gw.GWCommand;
import me.wang.happyGhastWar.game.party.Party;
import me.wang.happyGhastWar.game.party.PartyManager;
import me.wang.happyGhastWar.game.team.TeamDivider;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Leave extends GWCommand {
    public Leave(){
        super("leave", true, new String[0]);
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        World world = player.getWorld();
        if (!ghastWar.arenas.containsKey(world.getName())){
            player.sendMessage(ghastWar.language.getContent("commands.gameNotFound"));
            return;
        }

        Arena arena = ghastWar.arenas.get(world.getName());
        ArenaConfig arenaConfig = arena.getArenaConfig();

        Party party = HappyGhastWar.getInstance().getPartyManager().getParty(player);
        PartyManager partyManager = HappyGhastWar.getInstance().getPartyManager();

        if (party != null && party.isLeader(player)) {
            // 队长加入，检查整个Party
            List<Player> partyMembers = party.getOnlineMembers();

            for (Player member : partyMembers) {
                tpToLobby(member,ghastWar);
                arena.removePlayer(member);
            }
        } else {

            // 如果是Party成员但不是队长，检查队长是否已经加入
            if (party != null && !party.isLeader(player)) {
                if (arena.getPlayers().contains(party.getLeader())) {
                    player.sendMessage(ChatColor.RED + "请等待队长 " + party.getLeader().getName() + " 先离开游戏!");
                    return;
                }
            }
            tpToLobby(player,ghastWar);
            arena.removePlayer(player);
        }
    }

    public void tpToLobby(Player player,HappyGhastWar ghastWar){
        if (!ghastWar.getConfig().getBoolean("bungee.enable",false)){
            player.teleport(ghastWar.getLobby());
        }else {
            player.teleport(ghastWar.getLobby());
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(ghastWar.getConfig().getString("bungee.lobby","hub"));
            player.sendPluginMessage(ghastWar, "BungeeCord", out.toByteArray());
        }
    }

}
