package me.malo.timbermod.timber;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class CommandToggle implements CommandExecutor {

    // where the player status is stored
    public HashMap<Player, Boolean> playerStatus;

    /**
     * constructor
     *
     * @param playerStatus the hashmap where the status of toggle fpr each player is stored
     */
    public CommandToggle(HashMap<Player, Boolean> playerStatus) {
        this.playerStatus = playerStatus;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (sender instanceof Player && sender.hasPermission("timber.toggle")) {
            Player player = (Player) sender;
            setTimber(player, getTimber(player) ? Boolean.FALSE : Boolean.TRUE);
            player.sendMessage("[§7Toggled Timber:" + (getTimber(player) ? "§a on" : "§4 off") + "§r]");
        }
        return true;
    }

    /**
     * returns the toggle state for the given player
     * if the player is not in the map the player is set to true
     *
     * @param player player to look up
     * @return if the toggle is active or not
     */
    public boolean getTimber(Player player) {
        if (!inMap(player)) {
            setTimber(player, Boolean.TRUE);
        }
        return this.playerStatus.get(player);
    }

    /**
     * updates the state stored for the player
     *
     * @param player the player to change
     * @param state  the state to save
     */
    public void setTimber(Player player, Boolean state) {
        this.playerStatus.put(player, state);
    }

    /**
     * returns if the player is in the map
     *
     * @param player player to check
     * @return if the player is already in the map
     */
    public boolean inMap(Player player) {
        return this.playerStatus.containsKey(player);
    }
}
