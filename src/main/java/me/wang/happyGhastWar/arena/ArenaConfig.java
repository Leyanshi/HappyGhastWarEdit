package me.wang.happyGhastWar.arena;

import me.wang.happyGhastWar.HappyGhastWar;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public class ArenaConfig {
    private final HappyGhastWar main;
    private File file;
    private YamlConfiguration config;
    private final Logger logger;
    private String name;

    public ArenaConfig(HappyGhastWar ghastWar){
        this.main = ghastWar;
        this.logger = ghastWar.getLogger();
    }

    public void loadArena(String name){
        this.name = name;
        try{
            file = new File(main.getDataFolder()+"/arenas",name);
            config = YamlConfiguration.loadConfiguration(file);
            config.save(file);
            logger.info(ChatColor.GREEN+"Successfully loaded Arena "+name);
        } catch (IOException e) {
            logger.severe("Failed to load Arena "+name);
            throw new RuntimeException(e);
        }

    }

    public File getFile() {
        return file;
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public void reload(){
        if (config == null){
            return;
        }
        try {
            config.save(file);
            config.load(file);
            logger.info(ChatColor.GREEN+"Successfully reloaded Arena "+name);
        } catch (InvalidConfigurationException | IOException e) {
            logger.severe("Failed to reload Arena "+name);
            throw new RuntimeException(e);
        }
    }

    public void init(String name, World world){
        config.set("name",name);
        config.set("worldName",world.getName());
        config.set("game.teams",4);
        config.set("game.teamSize",4);
        config.set("game.timeCount",60);
        config.set("bossbar.develop",300);
        config.set("bossbar.battle",300);
        config.set("bossbar.reduce",700);
        config.set("bossbar.ultimate",300);
        config.set("chests",new ArrayList<>());

        reload();
    }

    public int getTimeCount(){
        return config.getInt("game.timeCount");
    }

    public String getName(){
        return config.getString("name");
    }

    public int getTeamCount(){
        return config.getInt("game.teams");
    }

    public int getTeamSize(){
        return config.getInt("game.teamSize");
    }

    public String getWorldName(){
        return config.getString("worldName");
    }

    public int getGhastAmount(){
        return config.getInt("game.ghast-amount");
    }

    public int getDevelop(){
        return config.getInt("bossbar.develop");
    }

    public int getBattle(){
        return config.getInt("bossbar.battle");
    }

    public int getReduce(){
        return config.getInt("bossbar.reduce");
    }

    public int getUltimate(){
        return config.getInt("bossbar.ultimate");
    }

    public void addChest(Location location){
        String loc = String.format("%s,%s,%s,%s",location.getWorld().getName(),location.getX(),location.getY(),location.getZ());
        List<String> list = config.getStringList("chests");
        list.add(loc);
        config.set("chests",list);

        reload();
    }

    public void removeChest(Location location){
        String loc = String.format("%s,%s,%s,%s",location.getWorld().getName(),location.getX(),location.getY(),location.getZ());
        List<String> list = config.getStringList("chests");
        list.remove(loc);
        config.set("chests",list);

        reload();
    }

    public List<Location> getChests(){
        List<String> list = config.getStringList("chests");
        List<Location> locations = new ArrayList<>();
        list.forEach(s -> {
            locations.add(translateLocation(s));
        });
        return locations;
    }

    public Location translateLocation(String raw){
        String[] data = raw.split(",");
        World world = Bukkit.getWorld(data[0]);
        double x = Double.valueOf(data[1]);
        double y = Double.valueOf(data[2]);
        double z = Double.valueOf(data[3]);
        return new Location(world,x,y,z);
    }

    public Location getGhastSpawn(Arena.Teams teams){
        return translateLocation(config.getString("ghast."+teams.name()));
    }

    public void setGhastAmount(int amount){
        config.set("game.ghast-amount",amount);

        reload();
    }

    public Location getSpawn(Arena.Teams teams){
        return translateLocation(config.getString("team."+teams.name()));
    }

    public int getRadius(){
        return config.getInt("game.radius");
    }

    public int getTargetRadius(){
        return config.getInt("game.target-radius");
    }

    public Location getCenter(){
        return translateLocation(config.getString("game.center"));
    }

    public void setRadius(int r){
        config.set("game.radius",r);

        reload();
    }

    public void setTargetRadius(int r){
        config.set("game.target-radius",r);

        reload();
    }

    public void setCenter(Location location){
        String loc = String.format("%s,%s,%s,%s",location.getWorld().getName(),location.getX(),location.getY(),location.getZ());
        config.set("game.center",loc);

        reload();
    }

    public void setTeamSpawn(Arena.Teams teams, Location location){
        String loc = String.format("%s,%s,%s,%s",location.getWorld().getName(),location.getX(),location.getY(),location.getZ());
        config.set("team."+teams.name(),loc);

        reload();
    }

    public Location getWait(){
        return translateLocation(config.getString("wait"));
    }

    public void setWait(Location location){
        String loc = String.format("%s,%s,%s,%s",location.getWorld().getName(),location.getX(),location.getY(),location.getZ());
        config.set("wait",loc);

        reload();
    }

    public void setGhastSpawn(Arena.Teams teams, Location location){
        String loc = String.format("%s,%s,%s,%s",location.getWorld().getName(),location.getX(),location.getY(),location.getZ());
        config.set("ghast."+teams.name(),loc);

        reload();
    }
}
