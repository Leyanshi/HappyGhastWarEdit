package me.wang.happyGhastWar;

import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.arena.ArenaConfig;
import me.wang.happyGhastWar.arena.ArenaSelector;
import me.wang.happyGhastWar.commands.gw.GWCommandRouter;
import me.wang.happyGhastWar.commands.party.PartyCommandRouter;
import me.wang.happyGhastWar.events.ProxyDataEvent;
import me.wang.happyGhastWar.events.game.*;
import me.wang.happyGhastWar.events.ghast.GhastDeath;
import me.wang.happyGhastWar.events.player.PlayerDeath;
import me.wang.happyGhastWar.events.player.PlayerKill;
import me.wang.happyGhastWar.game.party.PartyManager;
import me.wang.happyGhastWar.game.upgrade.UpgradeGUI;
import me.wang.happyGhastWar.game.prop.ItemFunctions;
import me.wang.happyGhastWar.util.Language;
import me.wang.happyGhastWar.util.Metrics;
import me.wang.happyGhastWar.util.SocketClient;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.mvplugins.multiverse.core.MultiverseCoreApi;

import java.io.File;
import java.util.*;

public final class HappyGhastWar extends JavaPlugin {

    public MultiverseCoreApi coreApi;

    public static FileConfiguration config;

    public static Language language;

    public static Map<String , Arena> arenas = new HashMap<>();


    public static ItemFunctions itemFunctions;

    private PartyManager partyManager;

    private ArenaSelector arenaSelector;

    public List<ItemStack> chest_items = new ArrayList<>(Arrays.asList(
            new ItemStack(Material.WIND_CHARGE),
            new ItemStack(Material.FIREWORK_ROCKET),
            new ItemStack(Material.GOLDEN_APPLE),
            new ItemStack(Material.ARROW),
            new ItemStack(Material.SNOW_BLOCK),
            new ItemStack(Material.TOTEM_OF_UNDYING),
            new ItemStack(Material.IRON_INGOT),
            new ItemStack(Material.COAL),
            new ItemStack(Material.COPPER_INGOT)
    ));

    public SocketClient socketClient;

    public Map<String,Language> languageMap = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        int pluginId = 29369;
        Metrics metrics = new Metrics(this, pluginId);

        coreApi = MultiverseCoreApi.get();

        socketClient = new SocketClient(this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        getLogger().info("Loading configs...");
        saveDefaultConfig();
        config = getConfig();
        language = loadLanguage("default",getConfig().getString("language"));
        loadLanguage("zh_cn","zh_cn.yml");
        loadLanguage("en_us","en_us.yml");
        loadLanguage("zh_tw","zh_tw.yml");
        loadLanguage("de_de","de_de.yml");
        loadLanguage("es_es","es_es.yml");
        loadLanguage("fr_fr","fr_fr.yml");
        loadLanguage("ru_ru","ru_ru.yml");

        if (config.getBoolean("bungee.enable",false)){
            getLogger().info("Trying to connect to the Proxy Server...");
            socketClient.connect(config.getString("bungee.host"),config.getInt("bungee.port"),config.getString("bungee.serverName"));
        }

        getLogger().info("Loading commands...");
        registerCommands();

        getLogger().info("Loading Arenas...");
        File arenaFolder = new File(getDataFolder(),"arenas");
        if (!arenaFolder.exists()){
            arenaFolder.mkdirs();
        }
        for (File file : arenaFolder.listFiles()){
            ArenaConfig arenaConfig = new ArenaConfig(this);
            arenaConfig.loadArena(file.getName());
            World world = getServer().getWorld(arenaConfig.getWorldName());
            if (world == null){
                getLogger().severe("Failed to load arena "+file.getName()+":World not found!");
                continue;
            }
            Arena arena = new Arena(this,world,arenaConfig);

            arenas.put(world.getName(), arena);
            getLogger().info("Loaded arena "+file.getName());
        }

        getServer().getPluginManager().registerEvents(new ClickFurnaceEvent(this),this);
        getServer().getPluginManager().registerEvents(new JoinGameEvent(this),this);
        getServer().getPluginManager().registerEvents(new LeaveGameEvent(this),this);
        getServer().getPluginManager().registerEvents(new GhastDeath(this),this);
        getServer().getPluginManager().registerEvents(new PlayerDeath(this),this);
        //getServer().getPluginManager().registerEvents(new PlayerKill(),this);
        getServer().getPluginManager().registerEvents(new Protection(this),this);
        getServer().getPluginManager().registerEvents(new Resource(this),this);
        getServer().getPluginManager().registerEvents(new ProxyDataEvent(this),this);
        getServer().getPluginManager().registerEvents(new Waiting(this),this);

        itemFunctions = new ItemFunctions(this);
        itemFunctions.startAllTasks();
        partyManager = new PartyManager();
        arenaSelector = new ArenaSelector(this);

    }

    public Language loadLanguage(String name,String filename){
        Language lang = new Language(this);
        lang.loadLanguage(filename);
        if (name.equals("default")){
            languageMap.put(name,lang);
            getLogger().info("Loaded language "+name);
        }
        return lang;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Unload arenas...");
        arenas.forEach((s, arena) -> {
            arena.stop();
            getLogger().info("Unloaded "+s);
        });
        socketClient.disconnect();
    }

    public void registerCommands(){
        PluginCommand pluginCommand = this.getCommand("ghastwar");
        if (pluginCommand != null) {
            GWCommandRouter router = new GWCommandRouter(this);
            pluginCommand.setExecutor(router);
            pluginCommand.setTabCompleter(router);
        }
        PluginCommand partyCommand = this.getCommand("party");
        if (partyCommand != null) {
            PartyCommandRouter router = new PartyCommandRouter(this);
            partyCommand.setExecutor(router);
            partyCommand.setTabCompleter(router);
        }
    }

    public static HappyGhastWar getInstance(){
        return HappyGhastWar.getPlugin(HappyGhastWar.class);
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public static Map<String, Arena> getArenas() {
        return arenas;
    }

    public ArenaSelector getArenaSelector() {
        return arenaSelector;
    }

    public Location translateLocation(String raw){
        String[] data = raw.split(",");
        World world = Bukkit.getWorld(data[0]);
        double x = Double.valueOf(data[1]);
        double y = Double.valueOf(data[2]);
        double z = Double.valueOf(data[3]);
        return new Location(world,x,y,z);
    }

    public Location getLobby(){
        String s = config.getString("lobby","unknown");
        if (s.equals("unknown")){
            return Bukkit.getWorld("world").getSpawnLocation();
        }
        return translateLocation(s);
    }

    public void setLobby(Location location){
        String loc = String.format("%s,%s,%s,%s",location.getWorld().getName(),location.getX(),location.getY(),location.getZ());
        config.set("lobby",loc);
        saveConfig();
        reloadConfig();
    }

    public Language getLanguage(Player player){
        if (player == null){
            return language;
        }
        if (!languageMap.containsKey(player.getLocale())){
            return language;
        }
        return languageMap.get(player.getLocale());
    }

    public SocketClient getSocketClient() {
        return socketClient;
    }
}
