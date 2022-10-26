package me.jstl05.blockshuffle;

import me.jstl05.blockshuffle.commands.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlockShuffle extends JavaPlugin {

    private static BlockShuffle plugin;

    public static BlockShuffle getPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;

        blockShuffleCommand bsc = new blockShuffleCommand();
        getServer().getPluginManager().registerEvents(bsc, this);
        getCommand("blockshuffle").setExecutor(bsc);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
