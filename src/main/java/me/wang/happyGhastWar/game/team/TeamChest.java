package me.wang.happyGhastWar.game.team;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeamChest implements Listener {

    // 存储所有队伍的箱子数据
    private final Map<Team, Inventory> teamChests = new ConcurrentHashMap<>();

    // 存储打开中的GUI，便于清理
    private final Map<UUID, Team> openInventories = new ConcurrentHashMap<>();

    /**
     * 获取或创建队伍的箱子
     * @param team 队伍对象
     * @return 队伍的箱子Inventory
     */
    public Inventory getTeamChest(Team team) {
        if (team == null) return null;

        if (teamChests.containsKey(team)) {
            return teamChests.get(team);
        }

        // 创建新的团队箱子
        Inventory inventory = Bukkit.createInventory(
                new TeamChestHolder(team),
                6 * 9,
                "§6团队箱子 §7- §e" + team.getTeams().getDisplayName()
        );

        teamChests.put(team, inventory);
        return inventory;
    }

    /**
     * 为玩家打开团队箱子
     * @param player 玩家
     * @param team 玩家所在的队伍
     * @return 是否成功打开
     */
    public boolean openTeamChest(Player player, Team team) {
        if (team == null || !team.getPlayers().contains(player)) {
            player.sendMessage("§c你必须加入队伍才能使用团队箱子！");
            return false;
        }

        Inventory chest = getTeamChest(team);
        if (chest == null) return false;

        player.openInventory(chest);
        openInventories.put(player.getUniqueId(), team);
        return true;
    }

    /**
     * 装饰箱子界面（可选）
     * @param inventory 箱子界面
     */
    private void decorateInventory(Inventory inventory) {
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta meta = border.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§7 ");
            border.setItemMeta(meta);
        }

        // 在四周添加边框
        for (int i = 0; i < 9; i++) {
            if (i < 54) inventory.setItem(i, border);
        }

        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, border);
        }

        for (int i = 9; i < 54; i += 9) {
            inventory.setItem(i, border);
            if (i + 8 < 54) {
                inventory.setItem(i + 8, border);
            }
        }
    }

    /**
     * 检查玩家是否有权限访问这个箱子
     */
    private boolean canAccessChest(Player player, Team team) {
        return team != null && team.getPlayers().contains(player);
    }

    // 事件监听器部分

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();

        // 检查是否点击的是团队箱子
        if (!openInventories.containsKey(playerId)) return;

        Team team = openInventories.get(playerId);

        // 检查权限
        if (!canAccessChest(player, team)) {
            event.setCancelled(true);
            player.closeInventory();
            player.sendMessage("§c你已不在该队伍，无法使用团队箱子！");
            openInventories.remove(playerId);
            return;
        }

        // 防止玩家移动边框物品
        if (event.getCurrentItem() != null &&
                event.getCurrentItem().getType() == Material.BLACK_STAINED_GLASS_PANE) {
            org.bukkit.inventory.meta.ItemMeta meta = event.getCurrentItem().getItemMeta();
            if (meta != null && meta.getDisplayName().equals("§7 ")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();

        if (!openInventories.containsKey(playerId)) return;

        Team team = openInventories.get(playerId);

        if (!canAccessChest(player, team)) {
            event.setCancelled(true);
            player.closeInventory();
            player.sendMessage("§c你已不在该队伍，无法使用团队箱子！");
            openInventories.remove(playerId);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        openInventories.remove(player.getUniqueId());
    }

    /**
     * 获取指定队伍箱子的物品内容
     * @param teamName 队伍名称
     * @return 物品数组
     */
    public ItemStack[] getTeamChestContents(String teamName) {
        Inventory chest = teamChests.get(teamName);
        if (chest == null) return new ItemStack[0];
        return chest.getContents();
    }

    /**
     * 设置队伍箱子的物品内容
     * @param team 队伍名称
     * @param contents 物品数组
     */
    public void setTeamChestContents(Team team, ItemStack[] contents) {
        Inventory chest = getTeamChest(
                team
        );
        if (chest != null && contents.length <= 54) {
            chest.setContents(contents);
        }
    }

    /**
     * 删除队伍的箱子（当队伍解散时）
     * @param team 队伍名称
     * @return 是否成功删除
     */
    public boolean removeTeamChest(Team team) {
        // 先保存数据
        Inventory chest = teamChests.get(team);

        // 关闭所有正在查看这个箱子的玩家
        for (Map.Entry<UUID, Team> entry : openInventories.entrySet()) {
            if (entry.getValue().equals(team)) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    player.closeInventory();
                }
            }
        }

        return teamChests.remove(team) != null;
    }

    /**
     * 获取所有队伍箱子的映射
     * @return 队伍名称 -> 箱子的映射
     */
    public Map<Team, Inventory> getAllTeamChests() {
        return new HashMap<>(teamChests);
    }

    public void reset(){
        teamChests.forEach((team, itemStacks) -> {
            removeTeamChest(team);
        });
        teamChests.clear();
        openInventories.clear();
    }
}

class TeamChestHolder implements org.bukkit.inventory.InventoryHolder {
    private final String teamName;
    private final Team team;

    public TeamChestHolder(Team team) {
        this.teamName = team.getTeams().getDisplayName();
        this.team = team;
    }

    public String getTeamName() {
        return teamName;
    }

    @Override
    public Inventory getInventory() {
        return null; // 这个Holder只用于标识，实际箱子由TeamChest管理
    }
}