package me.wang.happyGhastWar.ghast;

import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.TextDisplay;

public class GameGhast {
    private final HappyGhast happyGhast;
    private final BlockDisplay blockDisplay;
    private final TextDisplay textDisplay;
    private final Interaction interaction;

    private int healthLevel = 0;
    private int armorLevel = 0;

    private int snowAmount = 0;

    public GameGhast(HappyGhast happyGhast,BlockDisplay blockDisplay,TextDisplay textDisplay,Interaction interaction){
        this.blockDisplay = blockDisplay;
        this.interaction = interaction;
        this.happyGhast = happyGhast;
        this.textDisplay = textDisplay;
    }

    public int getSnowAmount() {
        return snowAmount;
    }

    public void setSnowAmount(int snowAmount) {
        this.snowAmount = snowAmount;
    }

    public HappyGhast getHappyGhast() {
        return happyGhast;
    }

    public BlockDisplay getBlockDisplay() {
        return blockDisplay;
    }

    public Interaction getInteraction() {
        return interaction;
    }

    public TextDisplay getTextDisplay() {
        return textDisplay;
    }

    public int getArmorLevel() {
        return armorLevel;
    }

    public int getHealthLevel() {
        return healthLevel;
    }

    public void setArmorLevel(int armorLevel) {
        this.armorLevel = armorLevel;
    }

    public void setHealthLevel(int healthLevel) {
        this.healthLevel = healthLevel;
    }

    public void addMaxHealth(double h){
        if (happyGhast == null) return;
        double old = happyGhast.getMaxHealth();
        happyGhast.setMaxHealth(old+h);
    }

    public void addHealth(double h){
        if (happyGhast == null) return;
        double old = happyGhast.getHealth();
        happyGhast.setHealth(old+h);
    }

    public void unregister(){
        if (!happyGhast.isDead()){
            happyGhast.remove();
        }
        if (blockDisplay != null){
            blockDisplay.remove();
        }
        if (textDisplay != null){
            textDisplay.remove();
        }
        if (interaction != null){
            interaction.remove();
        }
    }
}
