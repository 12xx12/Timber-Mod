package de.malo.timber;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class CommandToggle implements CommandExecutor {

    private HashMap<Player, Boolean> playerStatus;

    public CommandToggle() {
        this.playerStatus = new HashMap<>();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            setTimber(player, !getTimber(player));
            player.sendMessage("[§7Toggled Timber:" + (getTimber(player) ? "§a on" : "§4 off") + "§r]");
        }
        return true;
    }

    public boolean getTimber(Player player) {
        if (!inMap(player)) {
            setTimber(player, Boolean.TRUE);
        }
        return this.playerStatus.get(player);
    }

    public void setTimber(Player player, Boolean enabled) {
        this.playerStatus.put(player, enabled);
    }

    public boolean inMap(Player player) {
        return this.playerStatus.containsKey(player);
    }
}
