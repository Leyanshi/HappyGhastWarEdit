package me.wang.happyGhastWar.commands.gw.impl.admin;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.arena.ArenaConfig;
import me.wang.happyGhastWar.commands.gw.GWCommand;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.mvplugins.multiverse.core.world.options.ImportWorldOptions;

import java.util.List;

public class Create extends GWCommand {
    public Create(){
        super("create", new String[0]);
    }

    public void evaluate(HappyGhastWar ghastWar, Player sender, String s, List<String> params) {
        sender.sendMessage("Creating...");
        Player player = (Player) sender;
        String worldName = params.get(0);
        World world = ghastWar.getServer().getWorld(worldName);
        if (world == null){
            player.sendMessage(ghastWar.getLanguage(player).getContent("commands.importWorld"));
            boolean result = ghastWar.coreApi.getWorldManager().importWorld(ImportWorldOptions.worldName(worldName)).isSuccess();
            if (!result){
                player.sendMessage(ghastWar.getLanguage(player).getContent("commands.importFailed"));
            }
        }
        player.teleport(world.getSpawnLocation());
        ghastWar.coreApi.getWorldManager().getLoadedWorld(worldName).peek(mvworld -> {
            mvworld.setHunger(false);
            world.setGameRule(GameRule.DO_MOB_SPAWNING,false);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);
            world.setGameRule(GameRule.DO_WEATHER_CYCLE,false);
            world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN,true);
            world.setGameRule(GameRule.DO_PATROL_SPAWNING,false);
            world.setGameRule(GameRule.DO_TRADER_SPAWNING,false);
            world.setGameRule(GameRule.DO_WARDEN_SPAWNING,false);
            world.setGameRule(GameRule.DO_FIRE_TICK,false);
        });
        player.setGameMode(GameMode.CREATIVE);

        ArenaConfig arenaConfig = new ArenaConfig(ghastWar);
        arenaConfig.loadArena(worldName+".yml");
        arenaConfig.init(params.get(1),world);

        Arena arena = new Arena(ghastWar,world,arenaConfig);
        arena.backupWorld();
        ghastWar.arenas.put(worldName,arena);

        player.sendMessage(ghastWar.getLanguage(player).getContent("commands.gameCreateSuccess"));
    }
}
