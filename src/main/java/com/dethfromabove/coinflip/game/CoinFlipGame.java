package com.dethfromabove.coinflip.game;

import org.bukkit.entity.Player;

import java.util.UUID;

public class CoinFlipGame {
    
    private final UUID gameId;
    private final UUID creatorId;
    private final String creatorName;
    private final double amount;
    private final long createdTime;
    private UUID acceptorId;
    private String acceptorName;
    
    public CoinFlipGame(Player creator, double amount) {
        this.gameId = UUID.randomUUID();
        this.creatorId = creator.getUniqueId();
        this.creatorName = creator.getName();
        this.amount = amount;
        this.createdTime = System.currentTimeMillis();
    }
    
    public UUID getGameId() {
        return gameId;
    }
    
    public UUID getCreatorId() {
        return creatorId;
    }
    
    public String getCreatorName() {
        return creatorName;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public long getCreatedTime() {
        return createdTime;
    }
    
    public UUID getAcceptorId() {
        return acceptorId;
    }
    
    public String getAcceptorName() {
        return acceptorName;
    }
    
    public void setAcceptor(Player acceptor) {
        this.acceptorId = acceptor.getUniqueId();
        this.acceptorName = acceptor.getName();
    }
    
    public boolean hasAcceptor() {
        return acceptorId != null;
    }
    
    public boolean isExpired(long expireTime) {
        return System.currentTimeMillis() - createdTime > expireTime * 1000;
    }
}