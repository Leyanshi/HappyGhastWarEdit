package me.wang.happyGhastWar.arena;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.game.party.Party;
import me.wang.happyGhastWar.game.party.PartyManager;
import me.wang.happyGhastWar.game.team.TeamDivider;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ArenaSelector implements Listener {

    private final HappyGhastWar plugin;
    private final Map<UUID, Inventory> openInventories;
    private final Map<UUID, Integer> refreshTasks;

    public ArenaSelector(HappyGhastWar plugin) {
        this.plugin = plugin;
        this.openInventories = new HashMap<>();
        this.refreshTasks = new HashMap<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openSelector(Player player) {
        // 获取所有可用的竞技场
        List<Arena> availableArenas = getAvailableArenas();

        // 计算GUI大小 (每行9个，最多6行)
        int rows = Math.max(1, (int) Math.ceil(availableArenas.size() / 9.0));
        int size = Math.min(rows * 9, 54);

        // 创建GUI
        Inventory gui = Bukkit.createInventory(
                null,
                size,
                ChatColor.translateAlternateColorCodes('&', "&6&l选择游戏 &7(点击加入)")
        );

        // 添加竞技场物品
        for (int i = 0; i < Math.min(availableArenas.size(), size); i++) {
            Arena arena = availableArenas.get(i);
            gui.setItem(i, createArenaItem(arena));
        }

        // 如果还有空间，添加刷新物品
        if (size > availableArenas.size()) {
            ItemStack refreshItem = new ItemStack(Material.COMPASS);
            ItemMeta meta = refreshItem.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "刷新列表");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "点击刷新可用游戏列表"
            ));
            refreshItem.setItemMeta(meta);
            gui.setItem(size - 1, refreshItem);
        }

        // 打开GUI
        player.openInventory(gui);
        openInventories.put(player.getUniqueId(), gui);

        // 启动刷新任务
        startRefreshTask(player);
    }


    private List<Arena> getAvailableArenas() {
        // 这里需要根据你的HappyGhastWar类结构来获取竞技场
        return HappyGhastWar.getArenas().values().stream()
                .filter(Arena::isEnable) // 启用状态
                .filter(arena -> arena.status != Arena.GameStatus.PLAYING) // 等待状态
                .filter(arena -> arena.getPlayers().size() <
                        arena.getArenaConfig().getTeamCount() * arena.getArenaConfig().getTeamSize()) // 未满员
                .sorted(Comparator.comparingInt(a -> a.getPlayers().size())) // 按玩家人数排序
                .collect(java.util.stream.Collectors.toList());
    }

    private ItemStack createArenaItem(Arena arena) {
        ItemStack item = new ItemStack(Material.GRASS_BLOCK);

        // 根据玩家人数选择不同的材质
        int playerCount = arena.getPlayers().size();
        int maxPlayers = arena.getArenaConfig().getTeamCount() * arena.getArenaConfig().getTeamSize();
        double fillPercentage = (double) playerCount / maxPlayers;

        // 根据填充率选择材质颜色
        if (fillPercentage >= 0.75) {
            item.setType(Material.RED_CONCRETE);
        } else if (fillPercentage >= 0.5) {
            item.setType(Material.YELLOW_CONCRETE);
        } else if (fillPercentage >= 0.25) {
            item.setType(Material.GREEN_CONCRETE);
        } else {
            item.setType(Material.BLUE_CONCRETE);
        }

        ItemMeta meta = item.getItemMeta();

        // 设置显示名称
        String statusColor = (fillPercentage >= 0.75) ?
                ChatColor.RED.toString() : (fillPercentage >= 0.5) ?
                ChatColor.YELLOW.toString() : ChatColor.GREEN.toString();

        meta.setDisplayName(ChatColor.GOLD + arena.getName() +
                statusColor + " [" + playerCount + "/" + maxPlayers + "]");

        // 设置Lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "状态: " + getStatusText(arena, fillPercentage));
        lore.add(ChatColor.GRAY + "队伍数: " + ChatColor.WHITE + arena.getArenaConfig().getTeamCount());
        lore.add(ChatColor.GRAY + "队伍大小: " + ChatColor.WHITE + arena.getArenaConfig().getTeamSize());
        lore.add("");
        lore.add(ChatColor.YELLOW + "点击加入游戏!");

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    private String getStatusText(Arena arena, double fillPercentage) {
        int playerCount = arena.getPlayers().size();
        int maxPlayers = arena.getArenaConfig().getTeamCount() * arena.getArenaConfig().getTeamSize();

        if (playerCount == 0) {
            return ChatColor.GREEN + "等待玩家";
        } else if (playerCount < maxPlayers / 2) {
            return ChatColor.YELLOW + "等待更多玩家";
        } else if (playerCount < maxPlayers) {
            return ChatColor.GOLD + "即将开始";
        } else {
            return ChatColor.RED + "已满员";
        }
    }

    private void startRefreshTask(Player player) {
        // 取消之前的任务
        if (refreshTasks.containsKey(player.getUniqueId())) {
            Bukkit.getScheduler().cancelTask(refreshTasks.get(player.getUniqueId()));
        }

        // 创建新任务
        int taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (!player.isOnline() || !openInventories.containsKey(player.getUniqueId())) {
                return;
            }

            // 刷新GUI
            Inventory gui = openInventories.get(player.getUniqueId());
            List<Arena> arenas = getAvailableArenas();

            // 清除旧物品
            gui.clear();

            // 添加新物品
            for (int i = 0; i < Math.min(arenas.size(), gui.getSize() - 1); i++) {
                Arena arena = arenas.get(i);
                gui.setItem(i, createArenaItem(arena));
            }

            // 添加刷新按钮
            ItemStack refreshItem = new ItemStack(Material.COMPASS);
            ItemMeta meta = refreshItem.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "刷新列表");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "点击刷新可用游戏列表"
            ));
            refreshItem.setItemMeta(meta);
            gui.setItem(gui.getSize() - 1, refreshItem);

        }, 0L, 100L).getTaskId();

        refreshTasks.put(player.getUniqueId(), taskId);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();

        // 检查是否是我们的GUI
        if (!openInventories.containsKey(playerId) ||
                !event.getInventory().equals(openInventories.get(playerId))) {
            return;
        }

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();

        // 处理刷新按钮
        if (clicked.getType() == Material.COMPASS &&
                clicked.hasItemMeta() &&
                clicked.getItemMeta().getDisplayName().contains("刷新列表")) {

            // 刷新GUI
            openInventories.remove(playerId);
            openSelector(player);
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return;
        }

        // 处理竞技场物品
        if (clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
            String displayName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

            //提取竞技场名称 (格式为 "竞技场名称 [x/y]")
            //注意：由于名称可能包含空格，我们按 " [" 分割，取第一部分
            if (displayName.contains(" [")) {
                String arenaName = displayName.split(" \\[")[0];

                // 找到对应的竞技场
                Optional<Arena> arenaOpt = HappyGhastWar.getArenas().values().stream()
                        .filter(a -> a.getName().equals(arenaName))
                        .findFirst();

                if (arenaOpt.isPresent()) {
                    Arena arena = arenaOpt.get();

                    // 检查是否满足加入条件
                    if (!arena.isEnable()) {
                        player.sendMessage(ChatColor.RED + "该游戏已禁用!");
                        return;
                    }

                    if (arena.status == Arena.GameStatus.PLAYING) {
                        player.sendMessage(ChatColor.RED + "游戏已开始，无法加入!");
                        return;
                    }

                    if (arena.getPlayers().contains(player)) {
                        player.sendMessage(ChatColor.RED + "你已在此游戏中!");
                        return;
                    }

                    // 检查是否满员
                    int maxPlayers = arena.getArenaConfig().getTeamCount() * arena.getArenaConfig().getTeamSize();
                    if (arena.getPlayers().size() >= maxPlayers) {
                        player.sendMessage(ChatColor.RED + "游戏已满员!");
                        return;
                    }

                    // 加入游戏
                    player.closeInventory();

                    Party party = HappyGhastWar.getInstance().getPartyManager().getParty(player);
                    PartyManager partyManager = HappyGhastWar.getInstance().getPartyManager();

                    if (party != null && party.isLeader(player)) {
                        List<Player> partyMembers = party.getOnlineMembers();
                        int partySize = partyMembers.size();

                        //检查Party人数是否超过队伍最大人数
                        if (partySize > arena.getArenaConfig().getTeamSize()) {
                            player.sendMessage(HappyGhastWar.language.getContent("game.party-too-large")
                                    .replace("{0}", String.valueOf(partySize))
                                    .replace("{1}", String.valueOf(arena.getArenaConfig().getTeamSize())));
                            return;
                        }

                        //检查Party成员是否都可以加入
                        for (Player member : partyMembers) {
                            if (arena.getPlayers().contains(member)) {
                                player.sendMessage(ChatColor.RED + member.getName() + " 已经在游戏中!");
                                return;
                            }
                        }

                        //检查加入Party后是否还能保证每个队伍都有玩家
                        List<Player> allPlayersAfterJoin = new ArrayList<>(arena.getPlayers());
                        allPlayersAfterJoin.addAll(partyMembers);

                        if (allPlayersAfterJoin.size() >= arena.getArenaConfig().getTeamCount()){
                            if (!TeamDivider.canStartCountdown(allPlayersAfterJoin, arena.getArenaConfig().getTeamCount(), partyManager)) {
                                if (arena.getPlayers().isEmpty()){
                                    player.sendMessage(HappyGhastWar.language.getContent("game.party-not-enough-space"));
                                    return;
                                }
                            }
                        }

                        for (Player member : partyMembers) {
                            member.teleport(arena.getArenaConfig().getWait());
                            arena.internalAddPlayer(member);
                        }
                    } else {
                        List<Player> allPlayersAfterJoin = new ArrayList<>(arena.getPlayers());
                        allPlayersAfterJoin.add(player);

                        if (party != null && !party.isLeader(player)) {
                            if (!arena.getPlayers().contains(party.getLeader())) {
                                player.sendMessage(ChatColor.RED + "请等待队长 " + party.getLeader().getName() + " 先加入游戏!");
                                return;
                            }
                        }
                        player.teleport(arena.getArenaConfig().getWait());
                        arena.internalAddPlayer(player);
                    }

                    player.sendMessage(ChatColor.GREEN + "已加入游戏: " + ChatColor.GOLD + arenaName);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                    // 从列表中移除
                    openInventories.remove(playerId);
                    if (refreshTasks.containsKey(playerId)) {
                        Bukkit.getScheduler().cancelTask(refreshTasks.get(playerId));
                        refreshTasks.remove(playerId);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (openInventories.containsKey(playerId)) {
            openInventories.remove(playerId);

            // 取消刷新任务
            if (refreshTasks.containsKey(playerId)) {
                Bukkit.getScheduler().cancelTask(refreshTasks.get(playerId));
                refreshTasks.remove(playerId);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // 清理资源
        if (openInventories.containsKey(playerId)) {
            openInventories.remove(playerId);
        }

        if (refreshTasks.containsKey(playerId)) {
            Bukkit.getScheduler().cancelTask(refreshTasks.get(playerId));
            refreshTasks.remove(playerId);
        }
    }
}