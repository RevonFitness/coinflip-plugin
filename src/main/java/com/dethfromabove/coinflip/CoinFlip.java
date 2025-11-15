package com.dethfromabove.coinflip;

import com.dethfromabove.coinflip.commands.CoinFlipCommand;
import com.dethfromabove.coinflip.managers.GameManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class CoinFlip extends JavaPlugin {

    private Economy economy;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        if (!setupEconomy()) {
            getLogger().severe("Vault not found! This plugin requires Vault and an economy plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        gameManager = new GameManager(this);
        
        getCommand("coinflip").setExecutor(new CoinFlipCommand(this));
        
        getLogger().info("CoinFlip plugin v2.0.0 has been enabled!");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.cancelAllGames();
        }
        getLogger().info("CoinFlip plugin has been disabled!");
    }
    
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }
    
    public Economy getEconomy() {
        return economy;
    }
    
    public GameManager getGameManager() {
        return gameManager;
    }
}