package me.wang.happyGhastWar.events.game;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Resource implements Listener {

    private HappyGhastWar ghastWar;

    public Resource(HappyGhastWar ghastWar){
        this.ghastWar = ghastWar;
    }

    private List<Material> logs = new ArrayList<>(Arrays.asList(
            Material.OAK_LOG,
            Material.SPRUCE_LOG,
            Material.BIRCH_LOG,
            Material.JUNGLE_LOG,
            Material.ACACIA_LOG,
            Material.DARK_OAK_LOG,
            Material.MANGROVE_LOG,
            Material.CHERRY_LOG,
            Material.PALE_OAK_LOG
    ));

    private List<Material> coals = new ArrayList<>(Arrays.asList(
            Material.COAL_ORE,
            Material.DEEPSLATE_COAL_ORE
    ));

    private List<Material> irons = new ArrayList<>(Arrays.asList(
            Material.IRON_ORE,
            Material.DEEPSLATE_IRON_ORE
    ));

    private List<Material> coppers = new ArrayList<>(Arrays.asList(
            Material.COPPER_ORE,
            Material.DEEPSLATE_COPPER_ORE
    ));

    @EventHandler
    public void destroy(BlockBreakEvent e){
        if (!HappyGhastWar.arenas.containsKey(e.getBlock().getWorld().getName())) return;
        Arena arena = HappyGhastWar.arenas.get(e.getBlock().getWorld().getName());
        if (!arena.isEnable()) return;
        if (arena.status != Arena.GameStatus.PLAYING) return;
        Block block = e.getBlock();
        Player player = e.getPlayer();

        if (block.getType() == Material.FURNACE || block.getType() == Material.BLAST_FURNACE) return;

        boolean isGetAble = false;

        if (block.getType() == Material.CHEST){
            Chest chest = (Chest) block.getState();
            for (ItemStack itemStack : Arrays.stream(chest.getBlockInventory().getContents()).toList()){
                if (itemStack != null){
                    player.getWorld().dropItem(chest.getLocation(),itemStack);
                }
            }
            e.setDropItems(false);
            return;
        }

        if (logs.contains(block.getType())){
            player.getInventory().addItem(new ItemStack(Material.OAK_LOG,2));
            isGetAble = true;
        }

        if (irons.contains(block.getType())){
            player.getInventory().addItem(new ItemStack(Material.IRON_INGOT,2));
            isGetAble = true;
        }

        if (block.getType() == Material.RAW_IRON_BLOCK){
            player.getInventory().addItem(new ItemStack(Material.IRON_INGOT,6));
            isGetAble = true;
            arena.rawBlocks.add(block.getLocation());
            e.setCancelled(true);
            block.setType(Material.IRON_ORE);
        }

        if (coals.contains(block.getType())){
            player.getInventory().addItem(new ItemStack(Material.COAL,3));
            isGetAble = true;
        }

        if (coppers.contains(block.getType())){
            player.getInventory().addItem(new ItemStack(Material.COPPER_INGOT,2));
            isGetAble = true;
        }
        e.setDropItems(false);
        Material type = block.getType();
        if (isGetAble){
            arena.resources.put(block.getLocation(),block.getType());

            if (block.getType() == Material.RAW_IRON_BLOCK) return;
            Bukkit.getServer().getScheduler().runTaskLater(HappyGhastWar.getPlugin(HappyGhastWar.class),() -> {
                if (arena.resources.isEmpty()){
                    return;
                }
                if (arena.rawBlocks.contains(block.getLocation())){
                    block.getLocation().getBlock().setType(Material.RAW_IRON_BLOCK);
                    return;
                }
                block.getLocation().getBlock().setType(type);
            },60 * 20);
        }

    }
}
