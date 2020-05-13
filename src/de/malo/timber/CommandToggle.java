package de.malo.timber;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class CommandToggle implements CommandExecutor {
    private HashMap<String, Boolean> playerStatus;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            setTimber(player, !getTimber(player));
        }
        return true;
    }

    public CommandToggle () {
        playerStatus = new HashMap<>();
    }

    public boolean getTimber(Player player) {
        return this.playerStatus.get(player.getName());
    }
    public void setTimber(Player player, boolean enabled) {
        this.playerStatus.put(player.getName(), enabled);
    }
    public boolean inMap(Player player) {
        return this.playerStatus.containsValue(player.getName());
    }
}
