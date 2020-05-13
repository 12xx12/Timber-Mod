package de.malo.timber;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class CommandToggle implements CommandExecutor {

    public HashMap<Player, Boolean> playerStatus;

    public CommandToggle(HashMap<Player, Boolean> map) {
        playerStatus = map;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            setTimber(player, getTimber(player) ? Boolean.FALSE : Boolean.TRUE);
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

    public HashMap<Player, Boolean> getPlayerStatus() {
        return playerStatus;
    }
}
