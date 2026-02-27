package me.wang.happyGhastWar.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.api.ClientDataReceiveEvent;
import me.wang.happyGhastWar.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProxyDataEvent implements Listener {

    private static Map<String,JsonObject> joinMap = new HashMap<>();

    private HappyGhastWar ghastWar;

    public ProxyDataEvent(HappyGhastWar ghastWar){
        this.ghastWar = ghastWar;
    }

    @EventHandler
    public void proxydata(ClientDataReceiveEvent e){
        JsonObject jsonObject = JsonParser.parseString(e.getData()).getAsJsonObject();
        switch (jsonObject.get("type").getAsString()){
            case "join" -> joinMap.put(jsonObject.get("player").getAsString(),jsonObject);
        }
    }

    @EventHandler
    public void join(PlayerJoinEvent e){
        if (joinMap.containsKey(e.getPlayer().getUniqueId().toString())){
            handleJoin(joinMap.get(e.getPlayer().getUniqueId().toString()));
            joinMap.remove(e.getPlayer().getUniqueId().toString());
        }
    }

    public void handleJoin(JsonObject jsonObject){
        Bukkit.getServer().getLogger().info(Bukkit.getServer().getOnlinePlayers().toString());
        Player player = Bukkit.getPlayer(UUID.fromString(jsonObject.get("player").getAsString()));
        if (!HappyGhastWar.arenas.containsKey(jsonObject.get("arena").getAsString())) return;
        Arena arena = HappyGhastWar.arenas.get(jsonObject.get("arena").getAsString());
        player.teleport(arena.getArenaConfig().getWait());
        arena.internalAddPlayer(player);
    }
}
