package me.wang.happyGhastWar.util;

import me.wang.happyGhastWar.HappyGhastWar;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Language {
    private final HappyGhastWar main;
    private File file;
    private YamlConfiguration config;
    private final Logger logger;
    private String name;

    public Language(HappyGhastWar ghastWar){
        this.main = ghastWar;
        this.logger = ghastWar.getLogger();
    }

    public void loadLanguage(String name){
        this.name = name;
        try{
            file = new File(main.getDataFolder()+"/languages",name);
            if (!file.exists()){
                main.saveResource("languages/"+name,false);
            }
            config = YamlConfiguration.loadConfiguration(file);
            config.save(file);
            logger.info(ChatColor.GREEN+"Successfully loaded language "+name);
        } catch (IOException e) {
            logger.severe("Failed to load language "+name);
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
            config.load(file);
            logger.info(ChatColor.GREEN+"Successfully reloaded language "+name);
        } catch (InvalidConfigurationException | IOException e) {
            logger.severe("Failed to reload language "+name);
            throw new RuntimeException(e);
        }
    }

    public String getRawContent(String key){
        return config.getString(key);
    }

    public String getContent(String key){
        String raw = config.getString(key);
        return ChatColor.translateAlternateColorCodes('&',raw);
    }

    public List<String> getList(String key){
        return config.getStringList(key);
    }

    public List<String> getTranslatedList(String key){
        List<String> list = config.getStringList(key);
        return list.stream().map(s -> s = ChatColor.translateAlternateColorCodes('&',s)).collect(Collectors.toList());
    }
}
