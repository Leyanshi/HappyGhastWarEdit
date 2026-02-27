package me.wang.happyGhastWar.scoreboard.old;

import me.wang.happyGhastWar.HappyGhastWar;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class GameScoreboard {
    private final HappyGhastWar plugin;
    private final String roomName;
    private final Scoreboard scoreboard;

    // 游戏状态
    private GameState gameState = GameState.WAITING;
    private int countdownSeconds = 0;
    private int requiredPlayers = 0;
    private int currentPlayers = 0;
    private String serverName = "";

    private String serverIp = "By lao_wang";

    // 队伍数据 - 存储恶魂（床）状态
    private final Map<String, me.wang.happyGhastWar.game.team.Team> teams = new HashMap<>();


    // 游戏状态枚举
    public enum GameState {
        WAITING, COUNTDOWN, PLAYING, ENDING
    }


    public GameScoreboard(HappyGhastWar plugin, String roomName, int teamCount, String serverIp) {
        this.plugin = plugin;
        this.roomName = roomName;
        this.requiredPlayers = teamCount; // 每队1人
        this.serverIp = serverIp;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("gw", "dummy",
                ChatColor.YELLOW.toString() + ChatColor.BOLD + "乐魂战");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    public GameScoreboard(HappyGhastWar plugin, String roomName, int teamCount, String serverIp, String serverName) {
        this.plugin = plugin;
        this.roomName = roomName;
        this.requiredPlayers = teamCount; // 每队1人
        this.serverName = serverName;
        this.serverIp = serverIp;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("gw", "dummy",
                ChatColor.YELLOW.toString() + ChatColor.BOLD + "乐魂战");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    public void init(){

    }

    public void addTeam(me.wang.happyGhastWar.game.team.Team team){
        teams.put(team.getTeams().getDisplayName(),team);
        updateAllScoreboards();
    }

    /**
     * 获取BedWars标准队伍名称
     */

    /**
     * 为玩家创建BedWars风格计分板
     */
    public void createScoreboard(Player player) {


        player.setScoreboard(scoreboard);
        //playerScoreboards.put(player.getUniqueId(), scoreboard);


        updateScoreboard();
    }

    /**
     * 移除玩家计分板
     */
    public void removeScoreboard(Player player) {
        //playerScoreboards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    /**
     * 更新所有玩家的计分板
     */
    public void updateAllScoreboards() {
        updateScoreboard();

    }



    /**
     * 更新单个玩家的计分板
     */
    public void updateScoreboard() {

        if (scoreboard == null) return;

        Objective objective = scoreboard.getObjective("gw");
        if (objective == null) return;

        // 清空现有行
        clearScoreboard(scoreboard);


        // 根据状态显示不同内容
        switch (gameState) {
            case WAITING:
                updateWaitingScoreboard(objective);
                break;
            case COUNTDOWN:
                updateCountdownScoreboard(objective);
                break;
            case PLAYING:
            case ENDING:
                updatePlayingScoreboard(objective);
                break;
        }
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    /**
     * 等待状态计分板 (BedWars1058风格)
     */
    private void updateWaitingScoreboard(Objective objective) {


        List<String> lines = plugin.language.getTranslatedList("scoreboard.waiting");
        String date = getDate("yy/MM/dd");
        lines = lines.stream().map(s -> s = s.replace("{date}",date)
                .replace("{server}",serverName)
                .replace("{map}",roomName)
                .replace("{on}",String.valueOf(currentPlayers))
                .replace("{max}",String.valueOf(requiredPlayers))
                .replace("{serverIp}",serverIp)
        ).collect(Collectors.toList());
        setScoreboardLines(objective, lines);
    }

    /**
     * 倒计时状态计分板 (BedWars1058风格)
     */
    private void updateCountdownScoreboard(Objective objective) {

        List<String> lines = plugin.language.getTranslatedList("scoreboard.starting");
        String date = getDate("yy/MM/dd");
        lines = lines.stream().map(s -> s = s.replace("{date}",date)
                .replace("{server}",serverName)
                .replace("{map}",roomName)
                .replace("{on}",String.valueOf(currentPlayers))
                .replace("{max}",String.valueOf(requiredPlayers))
                .replace("{time}",String.valueOf(countdownSeconds))
                .replace("{serverIp}",serverIp)
        ).collect(Collectors.toList());
        setScoreboardLines(objective, lines);
    }

    private String getDate(String f){
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat(f);
        return sdf.format(date);
    }

    private String getTeam(int index){
        if (index <= teams.size()) {
            me.wang.happyGhastWar.game.team.Team team = teams.values().stream().toList().get(index-1);
            String ghastStatus = team.isCanRespawn() ?
                    ChatColor.GREEN + ChatColor.BOLD.toString() + "✓" :
                    ChatColor.GRAY + "" + team.getSize();
            String teamStatus = team.isAlive() ?
                    "":
                    ChatColor.RED + ChatColor.BOLD.toString() + "✗";
            String teamLine = team.getTeam().getColor() + team.getTeams().getDisplayName() + "队" + " " +
                    (team.isAlive() ? ghastStatus : "") +
                    teamStatus;
            return teamLine;
        }
        return "Unknown Team";
    }

    /**
     * 游戏中状态计分板 (BedWars1058风格)
     */
    private void updatePlayingScoreboard(Objective objective) {


        List<String> lines = plugin.language.getTranslatedList("scoreboard.playing");
        String date = getDate("yy/MM/dd");
        lines = lines.stream().map(s -> s = s.replace("{date}",date)
                .replace("{server}",serverName)
                .replace("{map}",roomName)
                .replace("{on}",String.valueOf(currentPlayers))
                .replace("{max}",String.valueOf(requiredPlayers))
                .replace("{time}",String.valueOf(countdownSeconds))
                .replace("{serverIp}",serverIp)
                .replace("{team1}",getTeam(1))
                .replace("{team2}",getTeam(2))
                .replace("{team3}",getTeam(3))
                .replace("{team4}",getTeam(4))
                .replace("{team5}",getTeam(5))
                .replace("{team6}",getTeam(6))
                .replace("{team7}",getTeam(7))
                .replace("{team8}",getTeam(8))
        ).collect(Collectors.toList());
        lines = lines.stream().filter(line -> !Objects.equals(line, "Unknown Team")).toList();
        setScoreboardLines(objective, lines);
    }

    /**
     * 设置计分板行内容
     */
    private void setScoreboardLines(Objective objective, List<String> lines) {
        int score = lines.size(); // 从最高分开始
        for (String line : lines) {

            String entry = getUniqueEntry(line, score);
            objective.getScore(entry).setScore(score);
            score--;
        }
    }

    /**
     * 生成唯一的计分板条目
     */
    private String getUniqueEntry(String text, int index) {
        return ChatColor.RESET.toString().repeat(index+1) + text;
    }

    /**
     * 清空计分板
     */
    private void clearScoreboard(Scoreboard scoreboard) {
        scoreboard.getEntries().forEach(scoreboard::resetScores);
    }

    /**
     * 格式化时间 (MM:SS)
     */
    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    // ========== 状态更新方法 ==========

    /**
     * 更新倒计时 (每秒调用)
     */
    public void updateCountdown(int seconds) {
        this.countdownSeconds = seconds;
        this.gameState = GameState.COUNTDOWN;
        updateAllScoreboards();
    }


    /**
     * 更新玩家数量
     */
    public void updatePlayerCount(int current, int required) {
        this.currentPlayers = current;
        this.requiredPlayers = required;
        updateAllScoreboards();
    }

    /**
     * 设置游戏状态
     */
    public void setGameState(GameState state) {
        this.gameState = state;
        updateAllScoreboards();
    }

    /**
     * 更新队伍玩家数量
     */
    public void updateTeam(me.wang.happyGhastWar.game.team.Team team) {
        teams.put(team.getTeams().getDisplayName(),team);
        updateAllScoreboards();
    }



    /**
     * 获取队伍数据
     */
    public me.wang.happyGhastWar.game.team.Team getTeam(String teamName) {
        return teams.get(teamName);
    }

    /**
     * 获取所有队伍
     */
    public Collection<me.wang.happyGhastWar.game.team.Team> getAllTeams() {
        return teams.values();
    }

    /**
     * 获取当前状态
     */
    public GameState getGameState() {
        return gameState;
    }
}