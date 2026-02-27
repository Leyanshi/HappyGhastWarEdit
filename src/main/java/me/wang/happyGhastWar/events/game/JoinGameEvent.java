package me.wang.happyGhastWar.events.game;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinGameEvent implements Listener {

    private HappyGhastWar ghastWar;

    public JoinGameEvent(HappyGhastWar ghastWar){
        this.ghastWar = ghastWar;
    }

    @EventHandler
    public void joinFromWorld(PlayerChangedWorldEvent e){
        World world = e.getPlayer().getWorld();
        if (!HappyGhastWar.arenas.containsKey(world.getName())){
            return;
        }

        Arena arena = HappyGhastWar.arenas.get(world.getName());
        //System.out.println(arena.getName());
        //arena.addPlayer(e.getPlayer());
    }

    @EventHandler
    public void joinFromServer(PlayerJoinEvent e){
        ghastWar.getLanguage(e.getPlayer());
        World world = e.getPlayer().getWorld();
        if (!HappyGhastWar.arenas.containsKey(world.getName())){
            return;
        }
        Arena arena = HappyGhastWar.arenas.get(world.getName());

        //arena.addPlayer(e.getPlayer());
    }
}
