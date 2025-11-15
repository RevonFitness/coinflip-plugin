package com.dethfromabove.coinflip.managers;

import com.dethfromabove.coinflip.CoinFlip;
import com.dethfromabove.coinflip.game.CoinFlipGame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class GameManager {
    
    private final CoinFlip plugin;
    private final Map<UUID, CoinFlipGame> activeGames;
    private final Map<UUID, CoinFlipGame> allGames;
    
    public GameManager(CoinFlip plugin) {
        this.plugin = plugin;
        this.activeGames = new HashMap<>();
        this.allGames = new HashMap<>();
        
        startCleanupTask();
    }
    
    public boolean createGame(Player player, double amount) {
        if (hasActiveGame(player)) {
            return false;
        }
        
        CoinFlipGame game = new CoinFlipGame(player, amount);
        activeGames.put(player.getUniqueId(), game);
        allGames.put(game.getGameId(), game);
        return true;
    }
    
    public boolean cancelGame(Player player) {
        CoinFlipGame game = activeGames.remove(player.getUniqueId());
        if (game != null) {
            allGames.remove(game.getGameId());
            return true;
        }
        return false;
    }
    
    public CoinFlipGame getGame(Player player) {
        return activeGames.get(player.getUniqueId());
    }
    
    public CoinFlipGame getGameById(UUID gameId) {
        return allGames.get(gameId);
    }
    
    public boolean hasActiveGame(Player player) {
        return activeGames.containsKey(player.getUniqueId());
    }
    
    public List<CoinFlipGame> getAvailableGames(Player excludePlayer) {
        long expireTime = plugin.getConfig().getLong("game-expire-time", 300);
        return activeGames.values().stream()
                .filter(game -> !game.getCreatorId().equals(excludePlayer.getUniqueId()))
                .filter(game -> !game.hasAcceptor())
                .filter(game -> !game.isExpired(expireTime))
                .collect(Collectors.toList());
    }
    
    public void removeGame(CoinFlipGame game) {
        activeGames.remove(game.getCreatorId());
        if (game.hasAcceptor()) {
            activeGames.remove(game.getAcceptorId());
        }
        allGames.remove(game.getGameId());
    }
    
    public void cancelAllGames() {
        for (CoinFlipGame game : new ArrayList<>(activeGames.values())) {
            Player creator = Bukkit.getPlayer(game.getCreatorId());
            if (creator != null && creator.isOnline()) {
                plugin.getEconomy().depositPlayer(creator, game.getAmount());
                creator.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.game-cancelled"));
            }
        }
        activeGames.clear();
        allGames.clear();
    }
    
    private void startCleanupTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long expireTime = plugin.getConfig().getLong("game-expire-time", 300);
            List<CoinFlipGame> expiredGames = activeGames.values().stream()
                    .filter(game -> !game.hasAcceptor() && game.isExpired(expireTime))
                    .collect(Collectors.toList());
            
            for (CoinFlipGame game : expiredGames) {
                Player creator = Bukkit.getPlayer(game.getCreatorId());
                if (creator != null && creator.isOnline()) {
                    plugin.getEconomy().depositPlayer(creator, game.getAmount());
                    creator.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                        plugin.getConfig().getString("messages.game-expired"));
                }
                removeGame(game);
            }
        }, 20L * 60L, 20L * 60L);
    }
}