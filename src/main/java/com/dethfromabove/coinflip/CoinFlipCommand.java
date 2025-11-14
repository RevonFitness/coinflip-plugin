package com.dethfromabove.coinflip;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Random;

public class CoinFlipCommand implements CommandExecutor {

    private final Random random = new Random();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        // Flip the coin
        boolean isHeads = random.nextBoolean();
        String result = isHeads ? "HEADS" : "TAILS";
        
        // Create styled message
        Component message = Component.text("🪙 Flipping coin... ", NamedTextColor.YELLOW)
                .append(Component.text(result, isHeads ? NamedTextColor.GOLD : NamedTextColor.GRAY)
                        .decorate(TextDecoration.BOLD));

        // Send message and play sound
        player.sendMessage(message);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        return true;
    }
}