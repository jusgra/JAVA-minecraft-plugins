package me.jstl05.tagplugin;

import me.jstl05.tagplugin.commands.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class TagPlugin extends JavaPlugin {

    private static TagPlugin plugin;

    public static TagPlugin getPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;

        tagCommand tagCom = new tagCommand();
        getServer().getPluginManager().registerEvents(tagCom, this);
        getCommand("tag").setExecutor(tagCom);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
