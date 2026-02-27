package me.wang.happyGhastWar.game.upgrade;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.*;

/*
 * 熔炉熔炼功能
 */
public class AlloyMaker {
    private final JavaPlugin plugin;
    private final List<Player> playersToCheck;
    private final Map<UUID, Integer> progressMap = new HashMap<>();
    private final Map<UUID, Object> targetMap = new HashMap<>(); // 存储目标对象（Block或Entity）
    private final Map<UUID, Location> targetLocationMap = new HashMap<>();
    private final int requiredTicks = 100;
    private int taskId = -1;
    private double blockDisplayCheckDistance = 5.0; // BlockDisplay检查距离

    public AlloyMaker(JavaPlugin plugin, List<Player> playersToCheck) {
        this.plugin = plugin;
        this.playersToCheck = new ArrayList<>(playersToCheck);
    }

    public void setBlockDisplayCheckDistance(double distance) {
        this.blockDisplayCheckDistance = distance;
    }


    public double getBlockDisplayCheckDistance() {
        return blockDisplayCheckDistance;
    }

    public void startChecking() {
        if (taskId != -1) {
            return;
        }

        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                checkAllPlayers();
            }
        }.runTaskTimer(plugin, 0L, 1L).getTaskId(); // 每 tick 检查一次
    }

    public void stopChecking() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
            progressMap.clear();
            targetMap.clear();
            targetLocationMap.clear();
        }
    }


    public void addPlayer(Player player) {
        if (!playersToCheck.contains(player)) {
            playersToCheck.add(player);
        }
    }

    public void removePlayer(Player player) {
        if (playersToCheck.remove(player)) {
            progressMap.remove(player.getUniqueId());
            targetMap.remove(player.getUniqueId());
            targetLocationMap.remove(player.getUniqueId());
        }
    }


    public List<Player> getPlayersToCheck() {
        return new ArrayList<>(playersToCheck);
    }


    public int getPlayerProgress(Player player) {
        return progressMap.getOrDefault(player.getUniqueId(), 0);
    }


    private void checkAllPlayers() {
        List<Player> playersCopy = new ArrayList<>(playersToCheck);

        for (Player player : playersCopy) {
            if (!player.isOnline()) {
                // 玩家离线，移除并清理数据
                playersToCheck.remove(player);
                progressMap.remove(player.getUniqueId());
                targetMap.remove(player.getUniqueId());
                targetLocationMap.remove(player.getUniqueId());
                continue;
            }

            checkPlayer(player);
        }
    }


    private LookResult isLookingAtValidTarget(Player player) {
        UUID uuid = player.getUniqueId();
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        //检查方块
        Block targetBlock = player.getTargetBlockExact(5); // 5格范围内
        if (targetBlock != null && isFurnaceBlock(targetBlock)) {
            return new LookResult(true, targetBlock, targetBlock.getLocation());
        }

        //检查BlockDisplay实体
        List<Entity> nearbyEntities = player.getNearbyEntities(
                blockDisplayCheckDistance,
                blockDisplayCheckDistance,
                blockDisplayCheckDistance
        );

        // 按距离排序，优先检查近的实体
        nearbyEntities.sort(Comparator.comparingDouble(e -> e.getLocation().distance(eyeLocation)));

        for (Entity entity : nearbyEntities) {
            if (entity instanceof BlockDisplay) {
                BlockDisplay blockDisplay = (BlockDisplay) entity;

                // 检查BlockDisplay显示的是否为熔炉
                if (isFurnaceBlockDisplay(blockDisplay)) {
                    // 检查玩家是否真的看着这个BlockDisplay
                    if (isLookingAtEntity(player, blockDisplay, 5.0)) {
                        return new LookResult(true, blockDisplay, blockDisplay.getLocation());
                    }
                }
            }
        }

        return new LookResult(false, null, null);
    }


    private boolean isFurnaceBlock(Block block) {
        Material type = block.getType();
        return type == Material.FURNACE ||
                type == Material.BLAST_FURNACE ||
                type == Material.SMOKER;
    }

    private boolean isFurnaceBlockDisplay(BlockDisplay blockDisplay) {
        BlockData blockData = blockDisplay.getBlock();
        if (blockData == null) return false;

        Material material = blockData.getMaterial();
        return material == Material.FURNACE ||
                material == Material.BLAST_FURNACE ||
                material == Material.SMOKER;
    }


    private boolean isLookingAtEntity(Player player, Entity entity, double maxDistance) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        // 获取实体的边界框
        BoundingBox entityBox = entity.getBoundingBox();

        // 扩展一点点以避免精度问题
        entityBox.expand(0.1, 0.1, 0.1);

        // 创建从玩家眼睛位置到视线方向的射线
        Location rayStart = eyeLocation.clone();
        Vector rayDirection = direction.clone();

        // 检查射线是否与实体边界框相交
        Vector min = entityBox.getMin();
        Vector max = entityBox.getMax();

        // 简单的射线-包围盒相交测试
        double tMin = (min.getX() - rayStart.getX()) / rayDirection.getX();
        double tMax = (max.getX() - rayStart.getX()) / rayDirection.getX();

        if (tMin > tMax) {
            double temp = tMin;
            tMin = tMax;
            tMax = temp;
        }

        double tyMin = (min.getY() - rayStart.getY()) / rayDirection.getY();
        double tyMax = (max.getY() - rayStart.getY()) / rayDirection.getY();

        if (tyMin > tyMax) {
            double temp = tyMin;
            tyMin = tyMax;
            tyMax = temp;
        }

        if ((tMin > tyMax) || (tyMin > tMax)) return false;

        if (tyMin > tMin) tMin = tyMin;
        if (tyMax < tMax) tMax = tyMax;

        double tzMin = (min.getZ() - rayStart.getZ()) / rayDirection.getZ();
        double tzMax = (max.getZ() - rayStart.getZ()) / rayDirection.getZ();

        if (tzMin > tzMax) {
            double temp = tzMin;
            tzMin = tzMax;
            tzMax = temp;
        }

        if ((tMin > tzMax) || (tzMin > tMax)) return false;

        if (tzMin > tMin) tMin = tzMin;
        if (tzMax < tMax) tMax = tzMax;

        // 检查距离是否在范围内
        return tMin > 0 && tMin <= maxDistance;
    }


    private void checkPlayer(Player player) {
        UUID uuid = player.getUniqueId();

        // 检查主手是否持有铁锭
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        boolean hasIronInMainHand = mainHand != null &&
                mainHand.getType() == Material.IRON_INGOT &&
                mainHand.getAmount() > 0;

        // 检查副手是否持有铜锭
        ItemStack offHand = player.getInventory().getItemInOffHand();
        boolean hasCopperInOffHand = offHand != null &&
                offHand.getType() == Material.COPPER_INGOT &&
                offHand.getAmount() > 0;

        // 检查视线是否看着熔炉（方块或BlockDisplay实体）
        LookResult lookResult = isLookingAtValidTarget(player);
        boolean isLookingAtFurnace = lookResult.isValid;

        // 如果所有条件都满足
        if (hasIronInMainHand && hasCopperInOffHand && isLookingAtFurnace) {
            // 检查是否看着同一个目标
            Object currentTarget = lookResult.target;
            Location currentLocation = lookResult.location;

            Object lastTarget = targetMap.get(uuid);
            Location lastLocation = targetLocationMap.get(uuid);

            boolean isSameTarget = false;

            if (lastTarget != null && currentTarget != null) {
                // 如果都是方块，比较位置
                if (lastTarget instanceof Block && currentTarget instanceof Block) {
                    Block lastBlock = (Block) lastTarget;
                    Block currentBlock = (Block) currentTarget;
                    isSameTarget = lastBlock.getLocation().equals(currentBlock.getLocation());
                }
                // 如果都是实体，比较UUID
                else if (lastTarget instanceof Entity && currentTarget instanceof Entity) {
                    Entity lastEntity = (Entity) lastTarget;
                    Entity currentEntity = (Entity) currentTarget;
                    isSameTarget = lastEntity.getUniqueId().equals(currentEntity.getUniqueId());
                }
                // 如果都是位置，比较位置
                else if (lastLocation != null && currentLocation != null) {
                    isSameTarget = lastLocation.distanceSquared(currentLocation) < 0.01; // 0.1格内的误差
                }
            }

            if (!isSameTarget) {
                // 看着不同的目标，重置进度
                progressMap.put(uuid, 1);
                targetMap.put(uuid, currentTarget);
                targetLocationMap.put(uuid, currentLocation);
                sendProgressMessage(player, 1);
            } else {
                // 看着同一个目标，增加进度
                int currentProgress = progressMap.getOrDefault(uuid, 0) + 1;
                progressMap.put(uuid, currentProgress);

                // 每20ticks发送一次进度消息
                if (currentProgress % 20 == 0) {
                    sendProgressMessage(player, currentProgress);
                }

                // 检查是否完成
                if (currentProgress >= requiredTicks) {
                    giveAlloyIngot(player);
                    resetPlayerProgress(player);
                }
            }
        } else {
            // 条件不满足，重置进度
            if (progressMap.containsKey(uuid)) {
                sendInterruptMessage(player);
                resetPlayerProgress(player);
            }
        }
    }

    private void giveAlloyIngot(Player player) {
        // 消耗主手铁锭
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && mainHand.getType() == Material.IRON_INGOT) {
            if (mainHand.getAmount() > 1) {
                mainHand.setAmount(mainHand.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        }

        // 消耗副手铜锭
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand != null && offHand.getType() == Material.COPPER_INGOT) {
            if (offHand.getAmount() > 1) {
                offHand.setAmount(offHand.getAmount() - 1);
            } else {
                player.getInventory().setItemInOffHand(null);
            }
        }

        // 给予下界合金锭
        ItemStack netheriteIngot = new ItemStack(Material.NETHERITE_INGOT, 1);

        // 尝试添加到背包
        Map<Integer, ItemStack> remaining = player.getInventory().addItem(netheriteIngot);

        // 如果背包满了，掉落在地上
        if (!remaining.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), netheriteIngot);
            player.sendMessage("§e背包已满，下界合金锭已掉落在地上！");
        }

        // 播放合成成功音效
        player.playSound(player.getLocation(), "block.anvil.use", 1.0f, 1.0f);

        // 如果是通过BlockDisplay合成，可以额外播放粒子效果
        UUID uuid = player.getUniqueId();
        Object target = targetMap.get(uuid);

    }


    private void resetPlayerProgress(Player player) {
        UUID uuid = player.getUniqueId();
        progressMap.remove(uuid);
        targetMap.remove(uuid);
        targetLocationMap.remove(uuid);
    }


    private void sendProgressMessage(Player player, int currentProgress) {
        int seconds = currentProgress / 20;
        int totalSeconds = requiredTicks / 20;

        // 显示进度条
        StringBuilder progressBar = new StringBuilder(ChatColor.WHITE+"[");
        int barLength = 20;
        int progressPercent = (currentProgress * 100) / requiredTicks;
        int filledLength = (progressPercent * barLength) / 100;

        for (int i = 0; i < barLength; i++) {
            if (i < filledLength) {
                progressBar.append(ChatColor.GREEN+"|");
            } else {
                progressBar.append(ChatColor.GRAY+"|");
            }
        }
        progressBar.append(ChatColor.WHITE+"]");

        // 发送到动作栏
        player.sendTitle("",progressBar.toString(),0,30,0);
    }

    private void sendInterruptMessage(Player player) {

    }


    public boolean isRunning() {
        return taskId != -1;
    }

    public int getTaskId() {
        return taskId;
    }


    private static class LookResult {
        private final boolean isValid;
        private final Object target; // Block 或 Entity
        private final Location location;

        public LookResult(boolean isValid, Object target, Location location) {
            this.isValid = isValid;
            this.target = target;
            this.location = location;
        }

        public boolean isValid() {
            return isValid;
        }

        public Object getTarget() {
            return target;
        }

        public Location getLocation() {
            return location;
        }
    }
}