package me.wang.happyGhastWar.commands.gw.impl.admin;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.arena.ArenaConfig;
import me.wang.happyGhastWar.commands.gw.GWCommand;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;

public class RemoveChest extends GWCommand {
    public RemoveChest(){
        super("removechest", new String[0]);
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        World world = player.getWorld();
        if (!ghastWar.arenas.containsKey(world.getName())){
            player.sendMessage(ghastWar.language.getContent("commands.gameNotFound"));
            return;
        }

        Arena arena = ghastWar.arenas.get(world.getName());
        ArenaConfig arenaConfig = arena.getArenaConfig();

        Block block = player.getTargetBlockExact(5);
        if (block == null){
            player.sendMessage(ghastWar.language.getContent("commands.target-empty"));
            return;
        }
        if (block.getType() != Material.CHEST){
            player.sendMessage(ghastWar.language.getContent("commands.type-not-chest"));
            return;
        }

        arenaConfig.removeChest(block.getLocation());

        player.sendMessage(ghastWar.language.getContent("commands.setSuccess"));
    }
}
