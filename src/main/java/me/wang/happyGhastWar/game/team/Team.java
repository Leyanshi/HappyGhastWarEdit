package me.wang.happyGhastWar.game.team;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.ghast.GameGhast;
import org.bukkit.Bukkit;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.List;

public class Team {

    private List<Player> players = new ArrayList<>();

    private List<GameGhast> ghasts = new ArrayList<>();
    private final Arena.Teams teams;
    private final org.bukkit.scoreboard.Team team;
    private final Scoreboard scoreboard;
    private boolean canRespawn;
    private boolean isAlive;

    public Team(Arena.Teams teams, Scoreboard scoreboard){
        this.teams = teams;
        this.scoreboard = scoreboard;
        this.team = scoreboard.registerNewTeam(teams.getDisplayName());
        this.team.setColor(teams.getColor());
        this.team.setOption(org.bukkit.scoreboard.Team.Option.COLLISION_RULE, org.bukkit.scoreboard.Team.OptionStatus.FOR_OTHER_TEAMS);
        this.team.setAllowFriendlyFire(false);
        team.setPrefix(teams.getColor()+"["+teams.getDisplayName()+"]");
        this.canRespawn = true;
        this.isAlive = true;
    }

    public GameGhast getGhastByInteraction(Interaction interaction){
        for (GameGhast ghast : ghasts){
            if (ghast.getInteraction().equals(interaction)){
                return ghast;
            }
        }
        return null;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }

    public void addPlayer(Player player){
        players.add(player);
        team.addPlayer(player);
    }

    public void removePlayer(Player player){
        players.remove(player);
        team.removePlayer(player);
    }

    public Arena.Teams getTeams() {
        return teams;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public int getSize(){
        return players.size();
    }

    public void addGhast(GameGhast ghast){
        ghasts.add(ghast);
        team.addEntry(ghast.getHappyGhast().getUniqueId().toString());
    }

    public void removeGhast(GameGhast ghast){
        ghasts.remove(ghast);
        team.removeEntry(ghast.getHappyGhast().getUniqueId().toString());
    }

    public List<GameGhast> getGhasts() {
        return ghasts;
    }

    public int getGhastSize(){
        return ghasts.size();
    }

    public org.bukkit.scoreboard.Team getTeam() {
        return team;
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public void unRegister(){
        team.unregister();
        players.clear();
        ghasts.clear();
    }

    public boolean isCanRespawn() {
        return canRespawn;
    }

    public void setCanRespawn(boolean canRespawn) {
        this.canRespawn = canRespawn;
    }
}
