package me.wang.happyGhastWar.events.player;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.game.player.PlayerData;
import me.wang.happyGhastWar.game.team.Team;
import me.wang.happyGhastWar.util.Language;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerKill implements Listener {

    private HappyGhastWar ghastWar;

    public PlayerKill(HappyGhastWar ghastWar){
        this.ghastWar = ghastWar;
    }

    @Deprecated
    @EventHandler
    public void death(PlayerDeathEvent e){
        if (!HappyGhastWar.arenas.containsKey(e.getEntity().getLocation().getWorld().getName())) return;
        Arena arena = HappyGhastWar.arenas.get(e.getEntity().getLocation().getWorld().getName());
        if (!arena.isEnable()) return;
        if (arena.status != Arena.GameStatus.PLAYING) return;
        if (!arena.getPlayers().contains(e.getEntity())) return;
        Team team = null;
        Player killer = e.getEntity().getKiller();
        Player victim = e.getEntity();
        String killer_color = "";
        String killer_name = "";
        for (Team team1 : arena.getTeams()){
            if (team1.getPlayers().contains(e.getEntity())){
                team = team1;
            }
            if (killer != null){
                if (team1.getPlayers().contains(killer)){
                    killer_color = team1.getTeams().getColor().toString();
                    killer_name = killer.getDisplayName();
                }
            }
        }
        if (team == null) return;



        Bukkit.getServer().getLogger().info((killer != null)+"");

        if (killer != null){
            PlayerData data = arena.getPlayerDatas().get(killer);
            if (data != null){
                data.addKill(1);
                arena.getPlayerDatas().put(killer,data);
            }
        }

        Language language = HappyGhastWar.language;
        String message = !team.isCanRespawn() ? language.getContent("game.player-die-unknown-final") : language.getContent("game.player-die-unknown-regular");
        EntityDamageEvent damageEvent = e.getEntity().getLastDamageCause();

        if (damageEvent != null) {
            if (damageEvent.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {

                if (killer == null) {
                    message = !team.isCanRespawn() ? language.getContent("game.player-die-bomb2-final") : language.getContent("game.player-die-bomb2-regular");
                } else {
                    if (killer != victim) {
                        message = !team.isCanRespawn() ? language.getContent("game.player-die-bomb-final") : language.getContent("game.player-die-bomb-regular");
                    } else {
                        message = !team.isCanRespawn() ? language.getContent("game.player-die-bomb-final") : language.getContent("game.player-die-bomb-regular");
                    }
                }

            } else if (damageEvent.getCause() == EntityDamageEvent.DamageCause.VOID) {

                if (killer == null) {
                    message = !team.isCanRespawn() ? language.getContent("game.player-die-void-final") : language.getContent("game.player-die-void-regular");
                } else {
                    if (killer != victim) {
                        message = !team.isCanRespawn() ? language.getContent("game.player-die-knocked-void-final") : language.getContent("game.player-die-knocked-void-regular");
                    } else {
                        message = !team.isCanRespawn() ? language.getContent("game.player-die-void-final") : language.getContent("game.player-die-void-regular");
                    }
                }
            } else if (damageEvent.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                if (killer != null) {
                    message = !team.isCanRespawn() ? language.getContent("game.player-die-attack-final") : language.getContent("game.player-die-attack-regular");
                }
            } else if (damageEvent.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
                if (killer != null) {
                    message = !team.isCanRespawn() ? language.getContent("game.player-die-shoot-final") : language.getContent("game.player-die-shoot-regular");
                }
            } else if (damageEvent.getCause() == EntityDamageEvent.DamageCause.FALL) {

                if (killer != null && killer.getUniqueId().equals(victim.getUniqueId())) killer = null;
                if (killer != null) {
                    if (killer != victim) {
                        message = !team.isCanRespawn() ? language.getContent("game.player-die-knocked-fall-final") : language.getContent("game.player-die-knocked-fall-regular");
                    } else {
                        message = !team.isCanRespawn() ? language.getContent("game.player-die-void-final") : language.getContent("game.player-die-void-regular");
                    }
                }
            }
        }

        e.setDeathMessage(null);
        message = message
                .replace("{0}",team.getTeams().getColor().toString())
                .replace("{1}",victim.getDisplayName())
                .replace("{2}",killer_color)
                .replace("{3}",killer_name);
        for (Player player : arena.getPlayers()){
            player.sendMessage(message);
        }

    }
}
