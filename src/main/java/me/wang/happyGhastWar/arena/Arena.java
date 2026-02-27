package me.wang.happyGhastWar.arena;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.game.chest.ChestRandomFiller;
import me.wang.happyGhastWar.game.upgrade.AlloyMaker;
import me.wang.happyGhastWar.game.party.Party;
import me.wang.happyGhastWar.game.party.PartyManager;
import me.wang.happyGhastWar.game.player.PlayerData;
import me.wang.happyGhastWar.game.team.TeamChest;
import me.wang.happyGhastWar.game.team.TeamDivider;
import me.wang.happyGhastWar.game.upgrade.UpgradeGUI;
import me.wang.happyGhastWar.scoreboard.GameScoreboard;
import me.wang.happyGhastWar.game.team.Team;
import me.wang.happyGhastWar.ghast.GameGhast;
import me.wang.happyGhastWar.util.MessageTranslate;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.core.world.options.DeleteWorldOptions;
import org.mvplugins.multiverse.core.world.options.ImportWorldOptions;
import org.mvplugins.multiverse.core.world.options.UnloadWorldOptions;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public class Arena extends BukkitRunnable {
    public enum GameStatus{
        WAIT,
        STARTING,
        PLAYING,
        ENDING,
        PROCESSING,
        COUNTING
    }

    public enum Teams{
        RED("红",ChatColor.RED),
        BLUE("蓝",ChatColor.BLUE),
        GREEN("绿",ChatColor.GREEN),
        YELLOW("黄",ChatColor.YELLOW),
        PURPLE("紫",ChatColor.LIGHT_PURPLE),
        WHITE("白",ChatColor.WHITE),
        GOLD("橙",ChatColor.GOLD),
        AQUA("青蓝",ChatColor.AQUA);

        private final String displayName;

        private final ChatColor color;

        Teams(String displayName, ChatColor color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public ChatColor getColor() {
            return color;
        }
    }

    private List<Player> players = new ArrayList<>();

    private Map<Player, PlayerData> playerDatas = new HashMap<>();

    private final String name;
    private World world;
    public GameStatus status;
    private YamlConfiguration config;
    private HappyGhastWar ghastWar;
    private WorldManager worldManager;
    private Path worldPath;

    private final ArenaConfig arenaConfig;

    private List<Team> teams = new ArrayList<>();

    private Map<HappyGhast, GameGhast> ghasts = new HashMap<>();

    private final GameScoreboard gameScoreboard;

    private final Scoreboard scoreboard;

    private int timeCount = -1;

    private BukkitTask countTask;

    public Map<Location,Material> resources = new HashMap<>();

    public List<Location> rawBlocks = new ArrayList<>();

    public AlloyMaker alloyMaker;

    public TeamChest teamChest;

    public BossBar bossBar;

    public BukkitTask bossBarCount;

    public boolean enable = true;

    public int bossBarTime = -1;

    public NamespacedKey bossBarKey;

    public AdvancedCircleShrinker circleShrinker;

    public UpgradeGUI upgradeGUI;

    public UpgradeGUI getUpgradeGUI() {
        return upgradeGUI;
    }

    public boolean isEnable() {
        return enable;
    }

    public World getWorld() {
        return world;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getName() {
        return name;
    }

    public Map<Location, Material> getResources() {
        return resources;
    }

    public List<Location> getRawBlocks() {
        return rawBlocks;
    }

    public Map<HappyGhast, GameGhast> getGhasts(){
        return ghasts;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public enum gameStage{
        Development,
        Battle,
        Reduce,
        Ultimate,
        WAIT,
        END,
        COUNT
    }

    public gameStage stage;


    @Override
    public void run() {
        if (config == null){
            return;
        }
        teleportBlockDisplays();
        updateBossBar();
        switch (status){
            case WAIT -> handleWait();
            case STARTING -> handleStarting();
            case PLAYING -> handlePlaying();
            case ENDING -> handleEnding();
        }
    }

    public Team getPlayerTeam(Player player){
        for (Team team : teams){
            if (team.getPlayers().contains(player)){
                return team;
            }
        }
        return null;
    }

    public void updateBossBar(){
        switch (stage){
            case Development -> onDevelopment();
            case Battle -> onBattle();
            case Reduce -> onReduce();
            case Ultimate -> onUltimate();
        }
    }

    public void checkGhast(Team team,Arena arena){
        if (!team.getGhasts().isEmpty()) return;
        for (Player player : team.getPlayers()){
            player.sendTitle(ghastWar.getLanguage(player).getContent("game.respawn-unavailable-title"),HappyGhastWar.language.getContent("game.respawn-unavailable-subtitle"));
        }
        team.setCanRespawn(false);
        arena.getGameScoreboard().updateTeam(team);
    }

    public void onUltimate(){
        stage = gameStage.COUNT;
        bossBarTime = arenaConfig.getUltimate();
        bossBarCount = Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(ghastWar,()->{
            bossBarTime--;
            if (bossBarTime < 1){
                stage = gameStage.END;
                Bukkit.getServer().getScheduler().runTask(ghastWar,()->{
                    teams.forEach(team -> {
                        team.getGhasts().forEach(ghast -> {
                            team.removeGhast(ghast);
                            ghasts.remove(ghast);
                            ghast.unregister();
                            checkGhast(team,this);
                        });
                    });
                });
                bossBar.setProgress(1);
                bossBar.setTitle("");
                bossBarCount.cancel();
                return;
            }
            bossBar.setTitle(ghastWar.getLanguage(null).getContent("bossbar.ultimate").replace("{0}",String.valueOf(bossBarTime)));
            bossBar.setProgress(calculateDecimalPercentage(bossBarTime,arenaConfig.getUltimate()));
        },0,20);
    }

    public void onReduce(){
        stage = gameStage.COUNT;
        bossBarTime = arenaConfig.getReduce();
        bossBar.setColor(BarColor.RED);
        circleShrinker.startShrinking(arenaConfig.getTargetRadius(),arenaConfig.getReduce(),1);
        bossBarCount = Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(ghastWar,()->{
            bossBarTime--;
            if (bossBarTime < 1){
                stage = gameStage.Ultimate;
                bossBarCount.cancel();
                return;
            }
            bossBar.setTitle(ghastWar.getLanguage(null).getContent("bossbar.reduce").replace("{0}",String.valueOf(bossBarTime)));
            bossBar.setProgress(calculateDecimalPercentage(bossBarTime,arenaConfig.getReduce()));
        },0,20);
    }

    public void onBattle(){
        stage = gameStage.COUNT;
        bossBarTime = arenaConfig.getBattle();
        bossBar.setColor(BarColor.YELLOW);
        bossBarCount = Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(ghastWar,()->{
            bossBarTime--;
            if (bossBarTime < 1){
                stage = gameStage.Reduce;
                bossBarCount.cancel();
                return;
            }
            bossBar.setTitle(ghastWar.getLanguage(null).getContent("bossbar.battle").replace("{0}",String.valueOf(bossBarTime)));
            bossBar.setProgress(calculateDecimalPercentage(bossBarTime,arenaConfig.getBattle()));
        },0,20);
    }

    public void onDevelopment(){
        stage = gameStage.COUNT;
        bossBarTime = arenaConfig.getDevelop();
        bossBar.setColor(BarColor.BLUE);
        bossBarCount = Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(ghastWar,()->{
            bossBarTime--;
            if (bossBarTime < 1){
                stage = gameStage.Battle;
                bossBarCount.cancel();
                return;
            }
            bossBar.setTitle(ghastWar.getLanguage(null).getContent("bossbar.develop").replace("{0}",String.valueOf(bossBarTime)));
            bossBar.setProgress(calculateDecimalPercentage(bossBarTime,arenaConfig.getDevelop()));
        },0,20);
    }

    public double calculateDecimalPercentage(double current, double max) {
        if (max <= 0) return 0.0;

        double percentage = current / max;

        // 限制在0.0-1.0之间
        if (percentage < 0) return 0.0;
        if (percentage > 1) return 1.0;

        return percentage;
    }

    public void teleportBlockDisplays(){
        ghasts.forEach((happyGhast, gameGhast) -> {
            BlockDisplay blockDisplay = gameGhast.getBlockDisplay();

            ghastWar.getServer().getScheduler().runTask(ghastWar, () -> {
                if (!happyGhast.isValid() || !blockDisplay.isValid()) return;

                BoundingBox ghastBox = happyGhast.getBoundingBox();
                Vector3f ghastWorldCenter = new Vector3f(
                        (float)(ghastBox.getMinX() + ghastBox.getWidthX() / 2),
                        (float)(ghastBox.getMinY() + ghastBox.getHeight() / 2),
                        (float)(ghastBox.getMinZ() + ghastBox.getWidthZ() / 2)
                );

                Vector3f modelCenterOffset = new Vector3f(0.5f, 0.5f, 0.5f);

                float heightAbove = 2.5f;
                Location targetLocation = new Location(
                        happyGhast.getWorld(),
                        ghastWorldCenter.x,
                        ghastWorldCenter.y + heightAbove,
                        ghastWorldCenter.z
                );

                blockDisplay.teleport(targetLocation);

                TextDisplay textDisplay = gameGhast.getTextDisplay();
                textDisplay.teleport(targetLocation.clone().add(0,1,0));

                Interaction interaction = gameGhast.getInteraction();
                interaction.teleport(targetLocation.clone().add(0,-0.5,0));

                Location ghastLoc = happyGhast.getLocation();
                float yaw = ghastLoc.getYaw();
                float pitch = 0;

                float yawRad = (float) Math.toRadians(-yaw);
                float pitchRad = (float) Math.toRadians(pitch);
                org.joml.Quaternionf rotation = new org.joml.Quaternionf()
                        .rotateYXZ(yawRad, pitchRad, 0);

                org.joml.Vector3f rotatedOffset = rotation.transform(new org.joml.Vector3f(
                        modelCenterOffset.x, modelCenterOffset.y, modelCenterOffset.z
                ));
                org.joml.Vector3f translation = new org.joml.Vector3f(
                        -rotatedOffset.x, -rotatedOffset.y, -rotatedOffset.z
                );

                Transformation currentTrans = blockDisplay.getTransformation();
                Transformation newTransformation = new Transformation(
                        translation, // 应用补偿偏移
                        rotation,    // 应用乐魂的旋转
                        currentTrans.getScale(),
                        new org.joml.Quaternionf()
                );
                blockDisplay.setTransformation(newTransformation);

                World world = happyGhast.getWorld();

                world.spawnParticle(Particle.DUST,
                        targetLocation,
                        2, 0.05, 0.05, 0.05, 0,
                        new Particle.DustOptions(Color.BLACK, 1.0f));
            });
        });
    }

    public void stop(){
        sendPlayingMessage();
        this.cancel();
        this.alloyMaker.stopChecking();
        if (this.bossBarCount != null){
            this.bossBarCount.cancel();
        }
        Bukkit.removeBossBar(bossBarKey);

    }

    public void handleEnding(){
        status = GameStatus.PROCESSING;

        for (Player player : world.getPlayers()){
            player.sendMessage(ghastWar.getLanguage(player).getContent("game.arena-close"));
        }
        ghastWar.getServer().getScheduler().runTaskLater(ghastWar, () -> {
            for (Player player : world.getPlayers()){
                ghastWar.getLogger().info(player.getName());
                tpToLobby(player,ghastWar);
                removePlayer(player);
            }
            ghastWar.getLogger().info(world.getPlayers().toString());
            players.clear();
            playerDatas.clear();

            for (GameGhast ghast : ghasts.values()){
                ghast.unregister();
            }

            ghasts.clear();
            teams.clear();

            //while (!world.getPlayers().isEmpty());
            ghastWar.getServer().getScheduler().runTaskLater(ghastWar, () -> {
                resetWorld();
                init();
            },20L);
        },15 * 20);

    }

    public GameScoreboard getGameScoreboard() {
        return gameScoreboard;
    }

    public Map<Player, PlayerData> getPlayerDatas() {
        return playerDatas;
    }

    public void handlePlaying(){

        for (Team team : teams){
            if (team.getPlayers().isEmpty()){
                if (!team.isAlive()){
                    continue;
                }

                for (Player player : players){
                    String message = ghastWar.getLanguage(player).getContent("game.team-eliminated")
                            .replace("{0}",team.getTeams().getColor().toString())
                            .replace("{1}",team.getTeams().displayName);
                    player.sendMessage(message);
                }
                team.setAlive(false);
                gameScoreboard.updateTeam(team);
                teams.remove(team);

            }
        }

        gameScoreboard.update();

        if (teams.size() == 1){
            Team winTeam = teams.get(0);


            // 获取前3名
            List<Map.Entry<Player, PlayerData>> topThree = playerDatas.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().getKills() - e1.getValue().getKills())
                    .limit(3)
                    .toList();


            for (Player player : players){
                String winMessage = ghastWar.getLanguage(player).getContent("game.end-winner-team")
                        .replace("{0}",winTeam.getTeams().getColor().toString())
                        .replace("{1}",winTeam.getTeams().getDisplayName());


                String names = winTeam.getPlayers().stream().map(Player::getDisplayName).collect(Collectors.joining(", "));

                String first = ghastWar.getLanguage(player).getContent("game.null-player"), firstKills = "0";
                String second = ghastWar.getLanguage(player).getContent("game.null-player"), secondKills = "0";
                String third = ghastWar.getLanguage(player).getContent("game.null-player"), thirdKills = "0";

                // 赋值
                if (!topThree.isEmpty()) {
                    first = topThree.getFirst().getKey().getName();
                    firstKills = String.valueOf(topThree.getFirst().getValue().getKills());
                    Bukkit.getServer().getLogger().info(topThree.getFirst().getValue().getKills()+"");
                }
                if (topThree.size() > 1) {
                    second = topThree.get(1).getKey().getName();
                    secondKills = String.valueOf(topThree.get(1).getValue().getKills());
                }
                if (topThree.size() > 2) {
                    third = topThree.get(2).getKey().getName();
                    thirdKills = String.valueOf(topThree.get(2).getValue().getKills());
                }

                String topMessage = ghastWar.getLanguage(player).getContent("game.end-top-chat")
                        .replace("{0}",winTeam.getTeams().getColor().toString())
                        .replace("{1}",winTeam.getTeams().getDisplayName())
                        .replace("{2}",names)
                        .replace("{firstName}",first)
                        .replace("{firstKills}",firstKills)
                        .replace("{secondName}",second)
                        .replace("{secondKills}",secondKills)
                        .replace("{thirdName}",third)
                        .replace("{thirdKills}",thirdKills);

                if (winTeam.getPlayers().contains(player)){
                    player.sendTitle(ghastWar.getLanguage(player).getContent("game.end-victory-title"),"");
                }else {
                    player.sendTitle(ghastWar.getLanguage(player).getContent("game.end-game-over-title"),"");
                }
                player.sendMessage(winMessage);
                player.sendMessage(topMessage);
            }

            status = GameStatus.ENDING;
        }
    }

    public Color getColor(Teams teams){
        switch (teams){
            case RED -> {
                return Color.RED;
            }
            case BLUE -> {
                return Color.BLUE;
            }
            case GREEN -> {
                return Color.GREEN;
            }
            case YELLOW -> {
                return Color.YELLOW;
            }
            case AQUA -> {
                return Color.AQUA;
            }
            case WHITE -> {
                return Color.WHITE;
            }
            case GOLD -> {
                return Color.ORANGE;
            }
            case PURPLE -> {
                return Color.PURPLE;
            }
            default -> {
                return Color.BLACK;
            }
        }
    }

    public void giveWaitEquipment(Player player){
        ItemStack lobby = new ItemStack(Material.RED_BED);
        ItemMeta lobbyMeta = lobby.getItemMeta();
        lobbyMeta.setDisplayName(ghastWar.getLanguage(player).getContent("item.lobby-bed-name"));
        lobbyMeta.setLore(ghastWar.getLanguage(player).getTranslatedList("item.lobby-bed-lore"));
        lobby.setItemMeta(lobbyMeta);

        player.getInventory().setItem(0,lobby);
    }

    public void giveEquipment(Player player,Team team){
        player.getInventory().clear();
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta helmetMeta = (LeatherArmorMeta) helmet.getItemMeta();
        helmetMeta.setColor(getColor(team.getTeams()));
        helmet.setItemMeta(helmetMeta);

        ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta chestMeta = (LeatherArmorMeta) chest.getItemMeta();
        chestMeta.setColor(getColor(team.getTeams()));
        chest.setItemMeta(chestMeta);

        ItemStack leg = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta legMeta = (LeatherArmorMeta) leg.getItemMeta();
        legMeta.setColor(getColor(team.getTeams()));
        leg.setItemMeta(legMeta);

        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta bootsMeta = (LeatherArmorMeta) boots.getItemMeta();
        bootsMeta.setColor(getColor(team.getTeams()));
        boots.setItemMeta(bootsMeta);

        PlayerInventory inv = player.getInventory();
        inv.setHelmet(helmet);
        inv.setChestplate(chest);
        inv.setLeggings(leg);
        inv.setBoots(boots);

        inv.addItem(new ItemStack(Material.STONE_PICKAXE));
        inv.addItem(new ItemStack(Material.STONE_SWORD));
        inv.addItem(new ItemStack(Material.STONE_AXE));
        inv.addItem(new ItemStack(Material.WOODEN_SHOVEL));
        inv.addItem(new ItemStack(Material.SPYGLASS));

        upgradeGUI.giveCatapultToPlayer(player,0);

        inv.addItem(new ItemStack(Material.OAK_LOG,64 * 4));
        inv.addItem(new ItemStack(Material.IRON_INGOT, 64));
        inv.addItem(new ItemStack(Material.COPPER_INGOT, 50));
        inv.addItem(new ItemStack(Material.COAL, 50));
    }

    public void handleStarting(){
        if (timeCount > 0 || timeCount == -1){
            return;
        }
        status = GameStatus.PROCESSING;
        sendPlayingMessage();
        //List<Team> gteams = TeamDivider.dividePlayers(players,arenaConfig.getTeamCount(),1, scoreboard);
        List<Team> gteams = TeamDivider.dividePlayers(
                players,
                arenaConfig.getTeamCount(),
                1,
                scoreboard,
                HappyGhastWar.getInstance().getPartyManager()
        );
        this.teams = gteams;


        gameScoreboard.setGameState(GameScoreboard.GameState.PLAYING);


        Bukkit.getServer().getScheduler().runTask(ghastWar, ()->{
            world.setGameRule(GameRule.KEEP_INVENTORY,true);
            world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN,true);
            world.setDifficulty(Difficulty.EASY);

            ChestRandomFiller chestRandomFiller = new ChestRandomFiller();
            chestRandomFiller.fillChestsRandomly(arenaConfig.getChests(),ghastWar.chest_items);

            this.circleShrinker = new AdvancedCircleShrinker(ghastWar,arenaConfig.getCenter(),arenaConfig.getRadius());
            for (Team team : teams){
                gameScoreboard.addTeam(team);
                Location spawn = arenaConfig.getSpawn(team.getTeams());

                ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
                LeatherArmorMeta helmetMeta = (LeatherArmorMeta) helmet.getItemMeta();
                helmetMeta.setColor(getColor(team.getTeams()));
                helmet.setItemMeta(helmetMeta);

                ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE);
                LeatherArmorMeta chestMeta = (LeatherArmorMeta) chest.getItemMeta();
                chestMeta.setColor(getColor(team.getTeams()));
                chest.setItemMeta(chestMeta);

                ItemStack leg = new ItemStack(Material.LEATHER_LEGGINGS);
                LeatherArmorMeta legMeta = (LeatherArmorMeta) leg.getItemMeta();
                legMeta.setColor(getColor(team.getTeams()));
                leg.setItemMeta(legMeta);

                ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
                LeatherArmorMeta bootsMeta = (LeatherArmorMeta) boots.getItemMeta();
                bootsMeta.setColor(getColor(team.getTeams()));
                boots.setItemMeta(bootsMeta);




                for (int i = 0;i < arenaConfig.getGhastAmount(); i++){
                    Location ghastSpawn = arenaConfig.getGhastSpawn(team.getTeams());
                    GameGhast ghast = spawnGhast(team.getTeams(),ghastSpawn);
                    ghast.getHappyGhast().setAI(false);
                    team.addGhast(ghast);
                }


                for (Player player : team.getPlayers()){
                    player.teleport(spawn);
                    player.setPlayerTime(1000,false);
                    player.setPlayerWeather(WeatherType.CLEAR);
                    PlayerData playerData = new PlayerData(player,team);
                    playerDatas.put(player,playerData);
                    player.setGlowing(true);
                    giveEquipment(player,team);
                    alloyMaker.addPlayer(player);
                    bossBar.addPlayer(player);
                    upgradeGUI.upgradeManager.removePlayer(player);
                }


            }

            for (Player player : players){
                String startTitle = ghastWar.getLanguage(player).getContent("game.game-start-title");
                String startSubTitle = ghastWar.getLanguage(player).getContent("game.game-start-subtitle");
                player.sendTitle(startTitle,startSubTitle);
                player.sendMessage(ghastWar.getLanguage(player).getContent("game.game-start-message"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
            this.stage = gameStage.Development;
            status = GameStatus.PLAYING;
        });

    }

    public void handleWait(){

        int size = players.size();
        int teams = arenaConfig.getTeamCount();
        int teamSize = arenaConfig.getTeamSize();
        timeCount = arenaConfig.getTimeCount();
        int minPlayers = teams * 1;


        gameScoreboard.updatePlayerCount(size,teams * teamSize);

        if (size >= minPlayers){
            status = GameStatus.COUNTING;
            boolean canStart = TeamDivider.canStartCountdown(
                    players,
                    teams,
                    HappyGhastWar.getInstance().getPartyManager()
            );
            if (!canStart){
                status = GameStatus.WAIT;
                return;
            }

            for (Player player : players){
                player.sendMessage(ghastWar.getLanguage(player).getContent("game.getMinPlayers").replace("{time}",timeCount+""));
            }
            gameScoreboard.setGameState(GameScoreboard.GameState.COUNTDOWN);
            countTask = ghastWar.getServer().getScheduler().runTaskTimerAsynchronously(ghastWar,() -> {
                gameScoreboard.updateCountdown(timeCount);
                if (players.size() < minPlayers){
                    for (Player player : players){
                        player.sendMessage(ghastWar.getLanguage(player).getContent("game.game-cancel-message"));
                    }
                    countTask.cancel();
                    status = GameStatus.WAIT;
                    gameScoreboard.setGameState(GameScoreboard.GameState.WAITING);
                    timeCount = -1;
                    return;
                }
                if (timeCount > 0 && timeCount <= 5){
                    String subtitle = translateCount(timeCount);

                    for (Player player : players){
                        String title = ghastWar.getLanguage(player).getContent("game.arena-start-countdown-title");
                        player.sendTitle(title,subtitle);
                        player.playSound(player,Sound.UI_BUTTON_CLICK,1.0f,1.0f);
                        player.sendMessage(ghastWar.getLanguage(player).getContent("game.getMinPlayers").replace("{time}",timeCount+""));
                    }
                }
                if (timeCount < 1){
                    timeCount = 0;
                    countTask.cancel();
                    status = GameStatus.STARTING;
                    return;
                }
                timeCount--;
            },0,20);
        }
    }

    public String translateCount(int c){
        return switch (c) {
            case 5 -> HappyGhastWar.language.getContent("game.arena-start-countdown-subtitle-5");
            case 4 -> HappyGhastWar.language.getContent("game.arena-start-countdown-subtitle-4");
            case 3 -> HappyGhastWar.language.getContent("game.arena-start-countdown-subtitle-3");
            case 2 -> HappyGhastWar.language.getContent("game.arena-start-countdown-subtitle-2");
            case 1 -> HappyGhastWar.language.getContent("game.arena-start-countdown-subtitle-1");
            default -> "";
        };
    }

    public Material getHarness(Teams teams){
        return switch (teams) {
            case RED -> Material.RED_HARNESS;
            case BLUE -> Material.BLUE_HARNESS;
            case GREEN -> Material.GREEN_HARNESS;
            case YELLOW -> Material.YELLOW_HARNESS;
            case AQUA -> Material.CYAN_HARNESS;
            case WHITE -> Material.WHITE_HARNESS;
            case GOLD -> Material.ORANGE_HARNESS;
            case PURPLE -> Material.PURPLE_HARNESS;
        };
    }

    public ArenaConfig getArenaConfig(){
        return this.arenaConfig;
    }

    public GameGhast spawnGhast(Teams team, Location location){
        HappyGhast happyGhast = location.getWorld().spawn(location, HappyGhast.class);
        happyGhast.getEquipment().setItem(EquipmentSlot.BODY,new ItemStack(getHarness(team)));
        happyGhast.setGlowing(true);
        happyGhast.setMaxHealth(100);
        happyGhast.setHealth(100);

        BlockDisplay display = location.getWorld().spawn(happyGhast.getLocation(), BlockDisplay.class);
        display.setBlock((Material.FURNACE.createBlockData()));

        TextDisplay textDisplay = location.getWorld().spawn(happyGhast.getLocation(),TextDisplay.class);
        textDisplay.setText(team.color + team.displayName + "队" + ChatColor.WHITE + "乐魂");

        textDisplay.setBillboard(Display.Billboard.CENTER);  // 始终面向玩家
        textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
        textDisplay.setSeeThrough(true); // 可穿透看到后面
        textDisplay.setShadowed(true);   // 有阴影

        Interaction interaction = (Interaction) world.spawnEntity(location, EntityType.INTERACTION);

        float visualScale = 1f;
        interaction.setInteractionWidth(visualScale); // 宽度 (X/Z轴)
        interaction.setInteractionHeight(visualScale); // 高度 (Y轴)

        GameGhast gameGhast = new GameGhast(happyGhast,display,textDisplay,interaction);
        ghasts.put(happyGhast,gameGhast);
        return gameGhast;
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




    public Arena(HappyGhastWar ghastWar, World world, ArenaConfig arenaConfig){
        this.world = world;
        world.setGameRule(GameRule.KEEP_INVENTORY,true);
        this.status = GameStatus.WAIT;
        this.ghastWar = ghastWar;
        this.worldManager = ghastWar.coreApi.getWorldManager();
        this.worldPath = world.getWorldFolder().toPath();
        this.arenaConfig = arenaConfig;

        this.name = arenaConfig.getName();

        if (ghastWar.getConfig().getBoolean("bungee.enable")){
            this.gameScoreboard = new GameScoreboard(ghastWar,name,arenaConfig.getTeamCount(),ghastWar.getConfig().getString("serverIp"),ghastWar.getConfig().getString("bungee.serverName"));
        }else {
            this.gameScoreboard = new GameScoreboard(ghastWar,name,arenaConfig.getTeamCount(),ghastWar.getConfig().getString("serverIp"));
        }

        gameScoreboard.setGameState(GameScoreboard.GameState.WAITING);
        this.scoreboard = this.gameScoreboard.getScoreboard();
        this.alloyMaker = new AlloyMaker(ghastWar, new ArrayList<>());
        alloyMaker.startChecking();
        this.teamChest = new TeamChest();
        this.stage = gameStage.WAIT;
        this.bossBarKey = new NamespacedKey(ghastWar,world.getName());
        this.bossBar = ghastWar.getServer().createBossBar(bossBarKey,"Waiting", BarColor.WHITE, BarStyle.SOLID);
        this.upgradeGUI = new UpgradeGUI(ghastWar,this);
        loadConfig();
        startSchedule();
        sendReadyMessage();
    }



    public void sendReadyMessage(){
        if (!ghastWar.getConfig().getBoolean("bungee.enable",false)) return;
        JsonObject jsonObject = new MessageTranslate(ghastWar).FromArena(this);
        jsonObject.addProperty("action","ready");
        ghastWar.socketClient.sendMessage(jsonObject.toString());
    }

    public void sendRefreshMessage(){
        if (!ghastWar.getConfig().getBoolean("bungee.enable",false)) return;
        JsonObject jsonObject = new MessageTranslate(ghastWar).FromArena(this);
        jsonObject.addProperty("action","refresh");
        ghastWar.socketClient.sendMessage(jsonObject.toString());
    }

    public void sendPlayingMessage(){
        if (!ghastWar.getConfig().getBoolean("bungee.enable",false)) return;
        JsonObject jsonObject = new MessageTranslate(ghastWar).FromArena(this);
        jsonObject.addProperty("action","playing");
        ghastWar.socketClient.sendMessage(jsonObject.toString());
    }

    public void startSchedule(){
        this.runTaskTimerAsynchronously(this.ghastWar,0,0);
    }

    public void backupWorld(){
        ghastWar.getLogger().info("Backing up game: "+name);

        File folder = new File(this.ghastWar.getDataFolder(),"backups");
        if (!folder.exists()){
            folder.mkdirs();
        }
        this.worldManager.getWorld(this.world).peek(world -> {
            this.worldManager.unloadWorld(UnloadWorldOptions.world(worldManager.getLoadedWorld(world).get()));
        });
        Path backup = new File(this.ghastWar.getDataFolder()+"/backups",this.world.getName()).toPath();
        try {
            copyDirectory(this.worldPath,backup);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.worldManager.getWorld(this.world).peek(world -> {
            this.worldManager.loadWorld(world);
            this.world = ghastWar.getServer().getWorld(world.getName());
        });
    }

    public void resetWorld(){
        /*
        String worldName = world.getName();
        Path backup = new File(this.ghastWar.getDataFolder()+"/backups",this.world.getName()).toPath();
        this.worldManager.getWorld(this.world).peek(world -> {
            this.worldManager.deleteWorld(DeleteWorldOptions.world(world));
            try{
                copyDirectory(backup,this.worldPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            this.worldManager.importWorld(ImportWorldOptions.worldName(this.world.getName()));
            this.world = ghastWar.getServer().getWorld(worldName);
            ghastWar.getLogger().info("Game reset successful: "+name);
            gameScoreboard.setGameState(GameScoreboard.GameState.WAITING);
            status = GameStatus.WAIT;
        });

         */
    }

    public void init(){
        status = GameStatus.PROCESSING;
        String worldName = world.getName();
        ghastWar.getLogger().info("Resetting game: "+name);
        this.stage = gameStage.WAIT;
        this.bossBarCount.cancel();
        this.bossBarTime = -1;
        upgradeGUI.unregister();
        circleShrinker.stopShrinking();
        Path backup = new File(this.ghastWar.getDataFolder()+"/backups",this.world.getName()).toPath();
        this.worldManager.getWorld(this.world).peek(world -> {
            rawBlocks.clear();
            teamChest.reset();
            this.worldManager.deleteWorld(DeleteWorldOptions.world(world));
            try{
                copyDirectory(backup,this.worldPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            this.worldManager.importWorld(ImportWorldOptions.worldName(this.world.getName()));
            this.world = ghastWar.getServer().getWorld(worldName);

            gameScoreboard.setGameState(GameScoreboard.GameState.WAITING);
            timeCount = -1;

            players.clear();
            playerDatas.clear();
            ghasts.clear();
            teams.clear();
            for (Team team : gameScoreboard.getAllTeams()){
                team.unRegister();
            }
            gameScoreboard.init();
            resources.clear();
            upgradeGUI = new UpgradeGUI(ghastWar,this);
            ghastWar.getLogger().info("Game reset successful: "+name);
            status = GameStatus.WAIT;
            sendReadyMessage();
        });
        //this.status = GameStatus.WAIT;
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    if (!Files.exists(targetPath)) {
                        Files.createDirectories(targetPath);
                    }
                } else {
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                //throw new RuntimeException("复制文件失败: " + sourcePath, e);
            }
        });
    }

    private void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted((a, b) -> -a.compareTo(b)) // 先删除文件，后删除目录
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException ignored) {
                            // 记录但继续执行
                            this.ghastWar.getLogger().warning("无法删除文件: " + p);
                        }
                    });
        }
    }

    private void loadConfig(){
        File file = new File(this.ghastWar.getDataFolder()+"/maps/"+this.world.getName()+".yml");
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public List<Player> getPlayers() {
        return this.players;
    }

    public void addPlayer(Player player){
        Party party = HappyGhastWar.getInstance().getPartyManager().getParty(player);
        PartyManager partyManager = HappyGhastWar.getInstance().getPartyManager();

        if (party != null && party.isLeader(player)) {
            // 队长加入，检查整个Party
            List<Player> partyMembers = party.getOnlineMembers();
            int partySize = partyMembers.size();

            // 1. 检查Party人数是否超过队伍最大人数
            if (partySize > arenaConfig.getTeamSize()) {
                player.sendMessage(ghastWar.getLanguage(player).getContent("game.party-too-large")
                        .replace("{0}", String.valueOf(partySize))
                        .replace("{1}", String.valueOf(arenaConfig.getTeamSize())));
                return;
            }

            // 2. 检查Party成员是否都可以加入
            for (Player member : partyMembers) {
                if (players.contains(member)) {
                    player.sendMessage(ChatColor.RED + member.getName() + " 已经在游戏中!");
                    return;
                }
            }

            // 3. 快速检查：加入Party后是否还能保证每个队伍都有玩家
            List<Player> allPlayersAfterJoin = new ArrayList<>(players);
            allPlayersAfterJoin.addAll(partyMembers);

            if (!TeamDivider.canStartCountdown(allPlayersAfterJoin, arenaConfig.getTeamCount(), partyManager)) {
                player.sendMessage(ghastWar.getLanguage(player).getContent("game.party-not-enough-space"));
                return;
            }

            // 所有检查通过，加入所有Party成员
            for (Player member : partyMembers) {
                internalAddPlayer(member);
            }
        } else {
            // 独立玩家或非队长成员加入
            // 检查是否有空间（简单检查）
            List<Player> allPlayersAfterJoin = new ArrayList<>(players);
            allPlayersAfterJoin.add(player);

            if (!TeamDivider.canStartCountdown(allPlayersAfterJoin, arenaConfig.getTeamCount(), partyManager)) {
                player.sendMessage(ghastWar.getLanguage(player).getContent("game.party-not-enough-space"));
                return;
            }

            // 如果是Party成员但不是队长，检查队长是否已经加入
            if (party != null && !party.isLeader(player)) {
                if (!players.contains(party.getLeader())) {
                    player.sendMessage(ChatColor.RED + "请等待队长 " + party.getLeader().getName() + " 先加入游戏!");
                    return;
                }
            }

            internalAddPlayer(player);
        }

        //internalAddPlayer(player);
    }


    public void internalAddPlayer(Player player) {
        if (status != GameStatus.WAIT && status != GameStatus.COUNTING){

            joinAsSpectator(player);
            return;
        }
        for (Player player1 : players){
            player1.sendMessage(ghastWar.getLanguage(player1).getContent("game.player-join-game").replace("{name}", player.getName()));
        }
        player.setPlayerTime(1000,false);
        player.setPlayerWeather(WeatherType.CLEAR);
        player.getInventory().clear();
        giveWaitEquipment(player);
        gameScoreboard.createScoreboard(player);
        players.add(player);
        player.teleport(arenaConfig.getWait());
        sendRefreshMessage();
    }

    public void joinAsSpectator(Player player){
        player.setPlayerTime(1000,false);
        player.setPlayerWeather(WeatherType.CLEAR);
        player.getInventory().clear();
        giveWaitEquipment(player);
        gameScoreboard.createScoreboard(player);
        player.teleport(arenaConfig.getWait());
        player.sendMessage(ghastWar.getLanguage(player).getContent("commands.join-spectator"));
    }

    public void removePlayer(Player player){
        for (Player player1 : players){
            player1.sendMessage(ghastWar.getLanguage(player1).getContent("game.player-leave-game").replace("{name}", player.getName()));
        }
        player.getInventory().clear();
        player.setGlowing(false);
        gameScoreboard.removeScoreboard(player);
        players.remove(player);
        alloyMaker.removePlayer(player);
        bossBar.removePlayer(player);
        teams.forEach(team -> {
            if (team.getPlayers().contains(player)){
                team.removePlayer(player);
            }
        });
        sendRefreshMessage();
    }

    public void removeGhast(GameGhast ghast){
        ghasts.remove(ghast);
    }
}
