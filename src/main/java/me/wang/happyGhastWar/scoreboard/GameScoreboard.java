package me.wang.happyGhastWar.scoreboard;

import me.wang.happyGhastWar.HappyGhastWar;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class GameScoreboard {
    private final HappyGhastWar plugin;
    private final String roomName;
    private final Scoreboard scoreboard;
    private final Map<Player,Scoreboard> playerScoreboard = new HashMap<>();

    // 游戏状态
    private GameState gameState = GameState.WAITING;
    private int countdownSeconds = 0;
    private int requiredPlayers = 0;
    private int currentPlayers = 0;
    private String serverName = "";

    private String serverIp = "By lao_wang";

    private final Map<String, me.wang.happyGhastWar.game.team.Team> teams = new HashMap<>();

    private final Map<Integer, Team> lineTeams = new HashMap<>();

    private int currentDisplayedLines = 0;

    public enum GameState {
        WAITING, COUNTDOWN, PLAYING, ENDING
    }

    public GameScoreboard(HappyGhastWar plugin, String roomName, int teamCount, String serverIp) {
        this.plugin = plugin;
        this.roomName = roomName;
        this.requiredPlayers = teamCount;
        this.serverIp = serverIp;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("gw", "dummy",
                ChatColor.YELLOW.toString() + ChatColor.BOLD + "乐魂战");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

    }

    public GameScoreboard(HappyGhastWar plugin, String roomName, int teamCount, String serverIp, String serverName) {
        this.plugin = plugin;
        this.roomName = roomName;
        this.requiredPlayers = teamCount;
        this.serverName = serverName;
        this.serverIp = serverIp;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("gw", "dummy",
                ChatColor.YELLOW.toString() + ChatColor.BOLD + "乐魂战");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

    }

    /**
     * 初始化计分板行的Team对象
     */
    private void initializeLineTeams(Scoreboard s) {
        // 最多支持15行（计分板最大行数）
        for (int i = 1; i <= 15; i++) {
            String teamName = "line_" + i;
            Team team = s.registerNewTeam(teamName);

            // 使用唯一的条目标识符
            String entry = getColorCode(i);
            team.addEntry(entry);

            lineTeams.put(i, team);
        }
    }

    /**
     * 获取颜色代码，确保每个条目唯一
     */
    private String getColorCode(int index) {
        // 使用不同的颜色代码确保条目唯一
        ChatColor[] colors = {
                ChatColor.BLACK, ChatColor.DARK_BLUE, ChatColor.DARK_GREEN,
                ChatColor.DARK_AQUA, ChatColor.DARK_RED, ChatColor.DARK_PURPLE,
                ChatColor.GOLD, ChatColor.GRAY, ChatColor.DARK_GRAY,
                ChatColor.BLUE, ChatColor.GREEN, ChatColor.AQUA,
                ChatColor.RED, ChatColor.LIGHT_PURPLE, ChatColor.YELLOW,
                ChatColor.WHITE, ChatColor.BOLD, ChatColor.MAGIC
        };

        int colorIndex = (index - 1) % colors.length;
        return colors[colorIndex].toString() + ChatColor.RESET;
    }

    public void init() {
        // 初始化方法
    }

    public void addTeam(me.wang.happyGhastWar.game.team.Team team) {
        teams.put(team.getTeams().getDisplayName(), team);
        updateAllScoreboards();
    }

    /**
     * 为玩家创建BedWars风格计分板
     */
    public void createScoreboard(Player player) {
        Scoreboard s = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = s.registerNewObjective("gw", "dummy",
                plugin.getLanguage(player).getContent("scoreboard.title"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        initializeLineTeams(s);
        player.setScoreboard(s);
        playerScoreboard.put(player,s);
        updateScoreboard();
    }

    /**
     * 移除玩家计分板
     */
    public void removeScoreboard(Player player) {

        playerScoreboard.remove(player);
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    /**
     * 更新所有玩家的计分板
     */
    public void updateAllScoreboards() {
        updateScoreboard();
    }

    /**
     * 更新计分板
     */
    public void updateScoreboard() {
        playerScoreboard.forEach(((player, s) -> {
            if (s == null) return;

            Objective objective = s.getObjective("gw");
            if (objective == null) return;

            // 获取当前应该显示的内容
            List<String> currentLines = getCurrentLines(player);

            updateScoreboardLines(objective, currentLines, s);
        }));

    }

    /**
     * 获取当前应该显示的计分板行
     */
    private List<String> getCurrentLines(Player player) {
        return switch (gameState) {
            case WAITING -> getWaitingLines(player);
            case COUNTDOWN -> getCountdownLines(player);
            case PLAYING, ENDING -> getPlayingLines(player);
            default -> new ArrayList<>();
        };
    }

    /**
     * 等待状态计分板内容
     */
    private List<String> getWaitingLines(Player player) {
        List<String> lines = plugin.getLanguage(player).getTranslatedList("scoreboard.waiting");
        String date = getDate("yy/MM/dd");
        List<String> processedLines = new ArrayList<>();

        for (String line : lines) {
            // 保留空行
            if (line.isEmpty()) {
                processedLines.add("");
                continue;
            }

            // 处理带颜色的行
            String processedLine = line.replace("{date}", date)
                    .replace("{server}", serverName)
                    .replace("{map}", roomName)
                    .replace("{on}", String.valueOf(currentPlayers))
                    .replace("{max}", String.valueOf(requiredPlayers))
                    .replace("{serverIp}", serverIp);

            // 转换颜色代码
            processedLine = ChatColor.translateAlternateColorCodes('&', processedLine);
            processedLines.add(processedLine);
        }

        return processedLines;
    }

    /**
     * 倒计时状态计分板内容
     */
    private List<String> getCountdownLines(Player player) {
        List<String> lines = plugin.getLanguage(player).getTranslatedList("scoreboard.starting");
        String date = getDate("yy/MM/dd");
        List<String> processedLines = new ArrayList<>();

        for (String line : lines) {
            // 保留空行
            if (line.isEmpty()) {
                processedLines.add("");
                continue;
            }

            // 处理带颜色的行
            String processedLine = line.replace("{date}", date)
                    .replace("{server}", serverName)
                    .replace("{map}", roomName)
                    .replace("{on}", String.valueOf(currentPlayers))
                    .replace("{max}", String.valueOf(requiredPlayers))
                    .replace("{time}", String.valueOf(countdownSeconds))
                    .replace("{serverIp}", serverIp);

            // 转换颜色代码
            processedLine = ChatColor.translateAlternateColorCodes('&', processedLine);
            processedLines.add(processedLine);
        }

        return processedLines;
    }

    /**
     * 游戏中状态计分板内容
     */
    private List<String> getPlayingLines(Player player) {
        List<String> lines = plugin.getLanguage(player).getTranslatedList("scoreboard.playing");
        String date = getDate("yy/MM/dd");
        List<String> processedLines = new ArrayList<>();

        // 生成所有可能的队伍替换
        Map<String, String> replacements = new HashMap<>();
        replacements.put("{date}", date);
        replacements.put("{server}", serverName);
        replacements.put("{map}", roomName);
        replacements.put("{on}", String.valueOf(currentPlayers));
        replacements.put("{max}", String.valueOf(requiredPlayers));
        replacements.put("{time}", String.valueOf(countdownSeconds));
        replacements.put("{serverIp}", serverIp);

        // 为每个可能的队伍位置生成显示内容
        for (int i = 1; i <= 8; i++) {
            String teamKey = "{team" + i + "}";
            String teamDisplay = getTeamDisplay(i,player);

            // 如果队伍不存在，标记为要删除
            if (teamDisplay.equals("Unknown Team")) {
                // 设置为特殊标记，稍后删除整行
                replacements.put(teamKey, "__REMOVE_LINE__");
            } else {
                replacements.put(teamKey, teamDisplay);
            }
        }

        for (String line : lines) {
            // 如果这行是team行且标记为要删除，跳过不添加
            for (int i = 1; i <= 8; i++) {
                if (line.contains("{team" + i + "}")) {
                    if (replacements.get("{team" + i + "}").equals("__REMOVE_LINE__")) {
                        // 跳过这一行
                        continue;
                    }
                }
            }

            // 应用所有替换
            String processedLine = line;
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                processedLine = processedLine.replace(entry.getKey(), entry.getValue());
            }

            // 如果替换后包含删除标记，也跳过（可能是team行和其他内容的组合）
            if (processedLine.contains("__REMOVE_LINE__")) {
                continue;
            }

            // 转换颜色代码
            processedLine = ChatColor.translateAlternateColorCodes('&', processedLine);

            // 保留空行
            processedLines.add(processedLine);
        }

        return processedLines;
    }

    /**
     * 更新计分板行（使用Team避免闪烁）
     */
    private void updateScoreboardLines(Objective objective, List<String> lines, Scoreboard s) {
        int totalLines = lines.size();

        // 先清除上一次所有行的分数
        clearPreviousScores(s);

        // 从下往上设置行（计分板分数越高显示越靠上）
        for (int i = 0; i < totalLines; i++) {
            String line = lines.get(i);
            int lineNumber = totalLines - i; // 分数从高到低，第一行分数最高

            Team team = lineTeams.get(lineNumber);
            if (team != null) {
                // 处理空行 - 使用空格而不是空字符串
                if (line.isEmpty()) {
                    team.setPrefix(" ");
                    team.setSuffix("");
                } else if (line.length() > 16) {
                    // 分割为前缀和后缀
                    String prefix = line.substring(0, Math.min(16, line.length()));
                    String suffix = "";

                    // 获取前缀的最后颜色代码
                    String lastColors = ChatColor.getLastColors(prefix);

                    // 计算剩余部分
                    if (line.length() > 16) {
                        int suffixStart = 16;
                        int suffixEnd = Math.min(32, line.length());
                        suffix = lastColors + line.substring(suffixStart, suffixEnd);
                    }

                    team.setPrefix(prefix);
                    team.setSuffix(suffix);
                } else {
                    team.setPrefix(line);
                    team.setSuffix("");
                }

                // 设置分数（只有这一行需要显示）
                objective.getScore(getColorCode(lineNumber)).setScore(lineNumber);
            }
        }

        // 记录当前显示的行数
        currentDisplayedLines = totalLines;
    }

    /**
     * 清除上一次设置的分数
     */
    private void clearPreviousScores(Scoreboard s) {
        Objective objective = s.getObjective("gw");
        if (objective == null) return;

        // 只清除之前显示的行
        for (int i = 1; i <= currentDisplayedLines; i++) {
            String entry = getColorCode(i);
            s.resetScores(entry);
        }
    }

    private String getDate(String f) {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat(f);
        return sdf.format(date);
    }

    private String getTeamDisplay(int index,Player player) {
        List<me.wang.happyGhastWar.game.team.Team> teamList = new ArrayList<>(teams.values());

        if (index <= teamList.size()) {
            me.wang.happyGhastWar.game.team.Team team = teamList.get(index - 1);
            String ghastStatus = team.isCanRespawn() ?
                    ChatColor.GREEN + ChatColor.BOLD.toString() + "✓" :
                    ChatColor.GRAY + "" + team.getSize();
            String teamStatus = team.isAlive() ?
                    "" :
                    ChatColor.RED + ChatColor.BOLD.toString() + "✗";
            String teamLine = team.getTeam().getColor() + team.getTeams().getDisplayName() + plugin.getLanguage(player).getContent("scoreboard.team") + " " +
                    (team.isAlive() ? ghastStatus : "") +
                    teamStatus;
            return teamLine;
        }
        return "Unknown Team";
    }


    /**
     * 更新倒计时 (每秒调用)
     */
    public void updateCountdown(int seconds) {
        // 只有秒数变化时才更新
        if (this.countdownSeconds != seconds || this.gameState != GameState.COUNTDOWN) {
            this.countdownSeconds = seconds;
            this.gameState = GameState.COUNTDOWN;
            updateAllScoreboards();
        }
    }

    /**
     * 更新玩家数量
     */
    public void updatePlayerCount(int current, int required) {
        // 只有玩家数量变化时才更新
        if (this.currentPlayers != current || this.requiredPlayers != required) {
            this.currentPlayers = current;
            this.requiredPlayers = required;
            updateAllScoreboards();
        }
    }

    /**
     * 设置游戏状态
     */
    public void setGameState(GameState state) {
        // 只有状态变化时才更新
        if (this.gameState != state) {
            this.gameState = state;
            updateAllScoreboards();
        }
    }

    /**
     * 更新队伍
     */
    public void updateTeam(me.wang.happyGhastWar.game.team.Team team) {
        me.wang.happyGhastWar.game.team.Team existingTeam = teams.get(team.getTeams().getDisplayName());

        // 只有队伍数据变化时才更新
        if (existingTeam == null || !isTeamDataEqual(existingTeam, team)) {
            teams.put(team.getTeams().getDisplayName(), team);
            updateAllScoreboards();
        }
    }

    /**
     * 比较两个队伍数据是否相同
     */
    private boolean isTeamDataEqual(me.wang.happyGhastWar.game.team.Team team1, me.wang.happyGhastWar.game.team.Team team2) {
        return team1.isAlive() == team2.isAlive() &&
                team1.isCanRespawn() == team2.isCanRespawn() &&
                team1.getSize() == team2.getSize();
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public void update(){
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