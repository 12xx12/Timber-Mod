package me.malo.timbermod.timber;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static java.lang.Math.*;

public class Timber extends JavaPlugin implements Listener {

    public static StateFlag TIMBER_FLAG;
    static boolean withWorldGuard;
    static CommandToggle toggle;
    private HashMap<Player, Boolean> playerStatus;

    @Override
    public void onLoad() {
        // checks if WorldGuard is installed and disables functionality if installed
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
            getLogger().info("using no WorldGuard");
            withWorldGuard = false;
        } else {
            getLogger().info("using WorldGuard");
            withWorldGuard = true;
        }

        if (withWorldGuard) {
            // custom flag for timber usage
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            try {
                StateFlag flag = new StateFlag("Timber", true);
                registry.register(flag);
                TIMBER_FLAG = flag;
            } catch (FlagConflictException e) {
                Flag<?> existing = registry.get("Timber");
                if (existing instanceof StateFlag) {
                    TIMBER_FLAG = (StateFlag) existing;
                } else {
                    getLogger().info("FATAL ERROR WITH FLAG REGISTRATION");
                }
            }
        }
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        // config stuff
        this.saveDefaultConfig();
        FileConfiguration config = this.getConfig();
        config.addDefault("maxChop", 8);
        config.addDefault("logging", false);
        config.options().copyDefaults(true);
        saveConfig();

        // add player tracking
        playerStatus = new HashMap<>();
        toggle = new CommandToggle(playerStatus);

        // regestering command
        try {
            this.getCommand("toggletimber").setExecutor(new CommandToggle(playerStatus));
        } catch (NullPointerException e) {
            getLogger().info(e.getMessage());
        }
    }

    /**
     * Edit: 2020-07-16     Added support for the region flag
     */
    @EventHandler
    private void onBlockBreak(BlockBreakEvent e) {
        boolean canBuild;
        // checks if targeted block is buildable
        if (withWorldGuard) {
            LocalPlayer localPlayer = com.sk89q.worldguard.bukkit.WorldGuardPlugin.inst().wrapPlayer(e.getPlayer());
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            canBuild = query.testState(BukkitAdapter.adapt(e.getBlock().getLocation()), localPlayer, Flags.BUILD);
            canBuild = canBuild && query.testState(BukkitAdapter.adapt(e.getBlock().getLocation()), localPlayer, TIMBER_FLAG);
        } else {
            // if no WorldGuard is installed set to permanent true
            canBuild = true;
        }
        if (!e.getPlayer().isSneaking() && canBuild)
            if (e.getPlayer().hasPermission("timber.use"))
                if (toggle.getTimber(e.getPlayer()) == Boolean.TRUE)
                    if (isAxe(e.getPlayer().getInventory().getItemInMainHand()))
                        if (isLog(e.getBlock().getType()))
                            dropTree(e.getBlock().getLocation(), e.getPlayer());

    }

    /**
     * Drops the tree at the given location
     *
     * Edit 2020-07-16:     Removed unnecessary operation
     *
     * @param location location of the block destroyed
     * @param player   player who chopped the tree
     * @author Marc
     * @since 2020-05-05
     *
     */
    private void dropTree(final Location location, final Player player) {
        List<Block> checkedBlocks = new LinkedList<>();
        Location leaveLocation = location.clone();

        List<Block> blocks = leaveDrop(leaveLocation, location, checkedBlocks);

        for (Block block : blocks) {
            block.breakNaturally(player.getInventory().getItemInMainHand());
        }

        player.getInventory().setItemInMainHand(damageItem(player.getInventory().getItemInMainHand(), player, blocks.size()));
        player.updateInventory();

        // logging section
        if (getConfig().getBoolean("logging")) {
            getLogger().info("Player " + player.getName() + " broke " + (blocks.size()) +
                    " blocks at [" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + "]");
        }
    }

    /**
     * adds connected blocks to the removal list.
     * checks if the surrounding 18 block at y = ~ and y = ~+1 are a log and adds them
     *
     * @param location      location of the
     * @param origin        block which was already mined
     * @param checkedBlocks blocks already checked
     * @return list of blocks to break
     * @author Marc
     * @since 2020-05-05
     */
    private List<Block> leaveDrop(Location location, final Location origin, List<Block> checkedBlocks) {
        List<Block> breakBlogs = new LinkedList<>(); // list returned with the blocks to break
        final float border = getConfig().getInt("maxChop"); // maximum distance to original broken block

        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Location dest = location.getBlock().getRelative(x, y, z).getLocation();
                    if (!(checkedBlocks.contains(dest.getBlock()))) {
                        checkedBlocks.add(dest.clone().getBlock()); // adds to already checked blocks
                        if (isLog(dest.getBlock().getType()) && distance(location, origin) < border) {
                            if (!(breakBlogs.contains(dest.getBlock())))
                                breakBlogs.add(dest.clone().getBlock()); // adds to blocks to break if not already added
                            breakBlogs.addAll(leaveDrop(dest, origin, checkedBlocks));
                        }
                    }
                }
            }
        }
        return breakBlogs;
    }

    /**
     * determines if the given material is an axe
     *
     * Edit:    2020-07-16      Added netherite Axe
     * Edit:    2020-07-16      changed code to support future axe types
     *
     * @param material the material to check
     * @return whether it is or not
     * @author Marc
     * @since 2020-05-05
     */
    private boolean isAxe(ItemStack material) {
        return material.toString().contains("AXE");
    }

    /**
     * determine is the given material is a log
     *
     * Edit:    2020-07-16      Added warped and crimson stem Wood types
     * Edit:    2020-07-16      changed logic to support future Wood types
     *
     * @param material the material to check
     * @return whether it is or not
     * @author Marc
     * @since 2020-05-05
     */
    private boolean isLog||isStem(Material material) {
        return material.toString().contains("LOG") || material.toString().contains("STEM");
    }

    /**
     * returns the customised distance
     * the y - position is less influential to the distance
     *
     * Edit:    2020-07-16     some readability and logic cleanup
     *
     * @param pos1 starting position of the distance
     * @param pos2 ending position of the distance
     * @return the distance between pos1 and pos2
     * @author Marc
     * @since 2020-05-13
     */
    private int distance(Location pos1, Location pos2) {
        int dis = (int) round(sqrt(pow(pos2.getBlockX() - pos1.getBlockX(), 2) +
                                   pow(pos2.getBlockZ() - pos1.getBlockZ(), 2)));
        return (int) round(sqrt(pow((pos2.getBlockY() - pos1.getBlockY()) / 8.0, 2) + pow(dis, 2)));
    }

    /**
     * damages the given item by the given value, respecting the enchantment unbreaking.
     *
     * Edit:    2020-07-16     Added comments and removed unnecessary for loop
     *
     * @param item   - item to damage
     * @param damage - damage to deal to item
     * @return the damaged item
     * @author Marc
     * @since 2020-05-05
     */
    private ItemStack damageItem(ItemStack item, Player player, int damage) {
        org.bukkit.inventory.meta.Damageable im = (org.bukkit.inventory.meta.Damageable) item.getItemMeta();
        // checks for unbreaking and respects it in the damage value
        if (item.containsEnchantment(Enchantment.DURABILITY))
            damage = (int) round(damage * (1.0 / (item.getEnchantmentLevel(Enchantment.DURABILITY) + 1)));

        im.setDamage(im.getDamage() + damage);
        item.setItemMeta((org.bukkit.inventory.meta.ItemMeta) im);

        // removes item if item would break
        if (item.getType().getMaxDurability() - im.getDamage() <= 0) {
            item.setType(Material.AIR);

            // some special effects
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1F, 1F);
            player.spawnParticle(Particle.ITEM_CRACK, player.getLocation(), 1, 0, 0, 0,
                    player.getInventory().getItemInMainHand());
        }
        return item;
    }
}
