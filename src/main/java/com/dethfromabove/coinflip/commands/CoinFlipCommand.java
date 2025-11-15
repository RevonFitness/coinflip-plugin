package com.dethfromabove.coinflip.commands;

import com.dethfromabove.coinflip.CoinFlip;
import com.dethfromabove.coinflip.game.CoinFlipGame;
import com.dethfromabove.coinflip.util.CoinFlipAnimator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CoinFlipCommand implements CommandExecutor, TabCompleter {

    private final CoinFlip plugin;

    public CoinFlipCommand(CoinFlip plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("coinflip.use")) {
            player.sendMessage(colorize("&cYou don't have permission to use this!"));
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
            case "c":
                handleCreate(player, args);
                break;
            case "accept":
            case "a":
                handleAccept(player, args);
                break;
            case "list":
            case "l":
                handleList(player);
                break;
            case "cancel":
                handleCancel(player);
                break;
            default:
                sendHelpMessage(player);
                break;
        }

        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                "&cUsage: /cf create <amount>"));
            return;
        }

        if (plugin.getGameManager().hasActiveGame(player)) {
            player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                plugin.getConfig().getString("messages.already-has-game")));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + "&cInvalid amount!"));
            return;
        }

        double minBet = plugin.getConfig().getDouble("min-bet");
        double maxBet = plugin.getConfig().getDouble("max-bet");

        if (amount < minBet) {
            player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                plugin.getConfig().getString("messages.bet-too-low").replace("{min}", String.valueOf(minBet))));
            return;
        }

        if (amount > maxBet) {
            player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                plugin.getConfig().getString("messages.bet-too-high").replace("{max}", String.valueOf(maxBet))));
            return;
        }

        if (plugin.getEconomy().getBalance(player) < amount) {
            player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                plugin.getConfig().getString("messages.insufficient-funds").replace("{amount}", String.valueOf(amount))));
            return;
        }

        plugin.getEconomy().withdrawPlayer(player, amount);

        plugin.getGameManager().createGame(player, amount);

        player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
            plugin.getConfig().getString("messages.game-created").replace("{amount}", String.valueOf(amount))));
    }

    private void handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                "&cUsage: /cf accept <player>"));
            return;
        }

        if (plugin.getGameManager().hasActiveGame(player)) {
            player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                plugin.getConfig().getString("messages.already-has-game")));
            return;
        }

        String targetName = args[1];
        Player target = plugin.getServer().getPlayer(targetName);

        if (target == null || !target.isOnline()) {
            player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + "&cPlayer not found!"));
            return;
        }

        CoinFlipGame game = plugin.getGameManager().getGame(target);
        if (game == null) {
            player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                "&cThat player doesn't have an active coinflip!"));
            return;
        }

        long expireTime = plugin.getConfig().getLong("game-expire-time", 300);
        if (game.isExpired(expireTime)) {
            player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                plugin.getConfig().getString("messages.game-expired")));
            plugin.getGameManager().removeGame(game);
            return;
        }

        double amount = game.getAmount();

        if (plugin.getEconomy().getBalance(player) < amount) {
            player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                plugin.getConfig().getString("messages.insufficient-funds").replace("{amount}", String.valueOf(amount))));
            return;
        }

        plugin.getEconomy().withdrawPlayer(player, amount);

        game.setAcceptor(player);

        player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
            plugin.getConfig().getString("messages.game-accepted")
                .replace("{player}", target.getName())
                .replace("{amount}", String.valueOf(amount))));

        target.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
            "&e" + player.getName() + " accepted your coinflip for &6$" + amount + "&e!"));

        CoinFlipAnimator.startGame(plugin, game, target, player);
    }

    private void handleList(Player player) {
        List<CoinFlipGame> games = plugin.getGameManager().getAvailableGames(player);

        if (games.isEmpty()) {
            player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                plugin.getConfig().getString("messages.no-games-available")));
            return;
        }

        player.sendMessage(Component.text("━━━━━━ ", NamedTextColor.DARK_GRAY)
                .append(Component.text("Active CoinFlips", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" ━━━━━━", NamedTextColor.DARK_GRAY)));

        for (CoinFlipGame game : games) {
            Component message = Component.text("🪙 ", NamedTextColor.YELLOW)
                    .append(Component.text(game.getCreatorName(), NamedTextColor.WHITE))
                    .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("$" + game.getAmount(), NamedTextColor.GOLD))
                    .append(Component.text(" [ACCEPT]", NamedTextColor.GREEN, TextDecoration.BOLD)
                            .hoverEvent(HoverEvent.showText(Component.text("Click to accept!", NamedTextColor.GREEN)))
                            .clickEvent(ClickEvent.runCommand("/cf accept " + game.getCreatorName())));

            player.sendMessage(message);
        }
    }

    private void handleCancel(Player player) {
        if (!plugin.getGameManager().hasActiveGame(player)) {
            player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                plugin.getConfig().getString("messages.no-active-game")));
            return;
        }

        CoinFlipGame game = plugin.getGameManager().getGame(player);
        plugin.getEconomy().depositPlayer(player, game.getAmount());
        plugin.getGameManager().cancelGame(player);

        player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
            plugin.getConfig().getString("messages.game-cancelled")));
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(Component.text("━━━━━━ ", NamedTextColor.DARK_GRAY)
                .append(Component.text("CoinFlip Commands", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" ━━━━━━", NamedTextColor.DARK_GRAY)));
        player.sendMessage(Component.text("/cf create <amount>", NamedTextColor.YELLOW)
                .append(Component.text(" - Create a coinflip", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/cf accept <player>", NamedTextColor.YELLOW)
                .append(Component.text(" - Accept a coinflip", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/cf list", NamedTextColor.YELLOW)
                .append(Component.text(" - List active coinflips", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/cf cancel", NamedTextColor.YELLOW)
                .append(Component.text(" - Cancel your coinflip", NamedTextColor.GRAY)));
    }

    private String colorize(String message) {
        return message.replace("&", "§");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "accept", "list", "cancel"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("accept")) {
            plugin.getServer().getOnlinePlayers().forEach(p -> completions.add(p.getName()));
        }

        return completions;
    }
}