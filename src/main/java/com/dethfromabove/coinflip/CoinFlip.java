package com.dethfromabove.coinflip;

import org.bukkit.plugin.java.JavaPlugin;

public class CoinFlip extends JavaPlugin {

    @Override
    public void onEnable() {
        // Register the command
        this.getCommand("coinflip").setExecutor(new CoinFlipCommand());
        
        getLogger().info("CoinFlip plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("CoinFlip plugin has been disabled!");
    }
}