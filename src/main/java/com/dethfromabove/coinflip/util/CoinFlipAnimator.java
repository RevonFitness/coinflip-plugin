package com.dethfromabove.coinflip.util;

import com.dethfromabove.coinflip.CoinFlip;
import com.dethfromabove.coinflip.game.CoinFlipGame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Random;

public class CoinFlipAnimator {

    private static final Random random = new Random();

    public static void startGame(CoinFlip plugin, CoinFlipGame game, Player creator, Player acceptor) {
        boolean animationEnabled = plugin.getConfig().getBoolean("animation-enabled", true);
        int animationDuration = plugin.getConfig().getInt("animation-duration", 40);

        if (!animationEnabled) {
            // Skip animation, decide winner immediately
            decideWinner(plugin, game, creator, acceptor);
            return;
        }

        // Send initial message
        creator.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + "&eFlipping coin..."));
        acceptor.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + "&eFlipping coin..."));

        // Animation frames
        String[] frames = {"◐", "◓", "◑", "◒", "◐", "◓", "◑", "◒"};

        // Show spinning animation
        for (int i = 0; i < frames.length; i++) {
            final int index = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Component title = Component.text(frames[index], NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD);
                Component subtitle = Component.text("Flipping...", NamedTextColor.YELLOW);

                Title coinTitle = Title.title(
                        title,
                        subtitle,
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(250), Duration.ofMillis(100))
                );

                creator.showTitle(coinTitle);
                acceptor.showTitle(coinTitle);

                creator.playSound(creator.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.0f + (index * 0.1f));
                acceptor.playSound(acceptor.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.0f + (index * 0.1f));
            }, i * 5L);
        }

        // Decide winner after animation
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            decideWinner(plugin, game, creator, acceptor);
        }, animationDuration);
    }

    private static void decideWinner(CoinFlip plugin, CoinFlipGame game, Player creator, Player acceptor) {
        boolean creatorWins = random.nextBoolean();
        Player winner = creatorWins ? creator : acceptor;
        Player loser = creatorWins ? acceptor : creator;

        double amount = game.getAmount();
        double totalPot = amount * 2;

        // Calculate tax if enabled
        boolean taxEnabled = plugin.getConfig().getBoolean("tax-enabled", false);
        double taxPercentage = plugin.getConfig().getDouble("tax-percentage", 5.0);
        double taxAmount = 0;
        double payout = totalPot;

        if (taxEnabled) {
            taxAmount = totalPot * (taxPercentage / 100);
            payout = totalPot - taxAmount;
        }

        // Give winnings to winner
        plugin.getEconomy().depositPlayer(winner, payout);

        // Show results
        showResult(plugin, winner, loser, amount, payout, taxAmount, creatorWins);

        // Remove game
        plugin.getGameManager().removeGame(game);
    }

    private static void showResult(CoinFlip plugin, Player winner, Player loser, double betAmount, double payout, double tax, boolean creatorWon) {
        String result = creatorWon ? "HEADS" : "TAILS";
        NamedTextColor color = creatorWon ? NamedTextColor.GOLD : NamedTextColor.GRAY;
        String emoji = creatorWon ? "👑" : "⚪";

        // Winner messages
        Component winTitle = Component.text("YOU WON!", NamedTextColor.GREEN, TextDecoration.BOLD);
        Component winSubtitle = Component.text(emoji + " " + result + " " + emoji, color)
                .append(Component.text(" +$" + String.format("%.2f", payout), NamedTextColor.GOLD));

        winner.showTitle(Title.title(
                winTitle,
                winSubtitle,
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
        ));

        winner.playSound(winner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        String winMessage = plugin.getConfig().getString("messages.you-won")
                .replace("{amount}", String.format("%.2f", payout));
        winner.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + winMessage));

        if (tax > 0) {
            winner.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                "&7(Tax: &c-$" + String.format("%.2f", tax) + "&7)"));
        }

        // Loser messages
        Component loseTitle = Component.text("YOU LOST!", NamedTextColor.RED, TextDecoration.BOLD);
        Component loseSubtitle = Component.text(emoji + " " + result + " " + emoji, color)
                .append(Component.text(" -$" + String.format("%.2f", betAmount), NamedTextColor.RED));

        loser.showTitle(Title.title(
                loseTitle,
                loseSubtitle,
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
        ));

        loser.playSound(loser.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        
        String loseMessage = plugin.getConfig().getString("messages.you-lost")
                .replace("{amount}", String.format("%.2f", betAmount));
        loser.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + loseMessage));
    }

    private static String colorize(String message) {
        return message.replace("&", "§");
    }
}