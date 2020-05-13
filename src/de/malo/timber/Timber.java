package de.malo.timber;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.UnknownDependencyException;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

import static java.lang.Math.*;

public class Timber extends JavaPlugin implements Listener {

    static boolean withWorldGuard;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        // checks if WorldGuard is installed
        // Todo: make this work without WorldGuard
        WorldGuardPlugin worldGuard = getWorldGuard();
        if (worldGuard == null) {
            getLogger().info("using no WorldGuard");
            withWorldGuard = false;
        } else {
            getLogger().info("using WorldGuard");
            withWorldGuard = true;
        }

        // config stuff
        this.saveDefaultConfig();
        FileConfiguration config = this.getConfig();
        config.addDefault("maxChop", 8);
        config.addDefault("logging", false);
        config.options().copyDefaults(true);
        saveConfig();
    }

    @EventHandler
    private void onBlockBreak(BlockBreakEvent e) {
        boolean canBuild;
        // checks if targeted block is buildable
        if (withWorldGuard) {
            LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(e.getPlayer());
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(e.getBlock().getLocation());
            canBuild = query.testState(loc, localPlayer, Flags.BUILD);
        } else {
            // if no WorldGuard is installed set to permanent true
            canBuild = true;
        }
        if (!e.getPlayer().isSneaking() && canBuild)
            if (e.getPlayer().hasPermission("timber.use"))
                if (isAxe(e.getPlayer().getInventory().getItemInMainHand()))
                    if (isLog(e.getBlock().getType()))
                        dropTree(e.getBlock().getLocation(), e.getPlayer());

    }

    /**
     * Drops the tree at the given location
     *
     * @param location location of the block destroyed
     * @param player   player who chopped the tree
     * @author Marc
     * @since 2020-05-05
     */
    private void dropTree(final @NotNull Location location, final Player player) {
        List<Block> checkedBlocks = new LinkedList<>();
        Location origin = location.clone();
        Location leaveLocation = location.clone();

        List<Block> blocks = leaveDrop(leaveLocation, origin, checkedBlocks);

        for (Block block : blocks) {
            block.breakNaturally(player.getInventory().getItemInMainHand());
        }
        damageItem(player.getInventory().getItemInMainHand(), blocks.size());
        player.updateInventory();

        // logging section
        if (getConfig().getBoolean("logging")) {
            getLogger().info("Player " + player.getName() + " broke " + (blocks.size()) +
                    " blocks at [" + origin.getBlockX() + ", " + origin.getBlockY() + ", " + origin.getBlockZ() + "]");
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
    private @NotNull List<Block> leaveDrop(Location location, final Location origin, List<Block> checkedBlocks) {
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
     * returns the WorldGuard plugin to use
     *
     * @return the WorldGuard Plugin
     * @author Marc
     * @since 2020-05-05
     **/
    public WorldGuardPlugin getWorldGuard() {
        WorldGuardPlugin plugin;
        try {
            plugin = (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin("WorldGuard");
        } catch (UnknownDependencyException e) {
            plugin = null;
        }
        return plugin;
    }

    /**
     * determines if the given material is an axe
     *
     * @param material the material to check
     * @return whether it is or not
     * @author Marc
     * @since 2020-05-05
     */
    private boolean isAxe(@NotNull ItemStack material) {
        boolean isAxe;
        switch (material.getType()) {
            case WOODEN_AXE:
            case STONE_AXE:
            case IRON_AXE:
            case GOLDEN_AXE:
            case DIAMOND_AXE:
                isAxe = true;
                break;
            default:
                isAxe = false;
                break;
        }
        return isAxe;
    }

    /**
     * determine is the given material is a log
     *
     * @param material the material to check
     * @return whether it is or not
     * @author Marc
     * @since 2020-05-05
     */
    private boolean isLog(@NotNull Material material) {
        boolean isLog;
        switch (material) {
            case ACACIA_LOG:
            case BIRCH_LOG:
            case DARK_OAK_LOG:
            case JUNGLE_LOG:
            case OAK_LOG:
            case SPRUCE_LOG:
                isLog = true;
                break;
            default:
                isLog = false;
                break;
        }
        return isLog;
    }

    /**
     * returns the customised distance
     * the y - position is less influential to the distance
     *
     * @param pos1 starting position of the distance
     * @param pos2 ending position of the distance
     * @return the distance betweent pos1 and pos2
     * @author Marc
     * @since 2020-05-13
     */
    private int distance(@NotNull Location pos1, @NotNull Location pos2) {
        int dis = (int) round(sqrt(pow(pos1.getBlockX() - pos2.getBlockX(), 2) + pow(pos1.getBlockZ() - pos2.getBlockZ(), 2)));
        dis += round(abs(pos1.getBlockY() - pos2.getBlockY()) / 8.0);
        return dis;
    }

    /**
     * damages the given item by the given value, respecting unbreaking.
     *
     * @param item   - item to damage
     * @param damage - damage to deal to item
     * @return the damaged item
     * @author Marc
     * @since 2020-05-05
     */
    private @NotNull ItemStack damageItem(@NotNull ItemStack item, int damage) {
        // Todo: change damaging behaviour to iterative design to assure good destruction behaviour
        org.bukkit.inventory.meta.Damageable im = (org.bukkit.inventory.meta.Damageable) item.getItemMeta();
        // checks for unbreaking and respects it in the damage value
        if (item.containsEnchantment(Enchantment.DURABILITY))
            damage = (int) round(damage * (1.0 / (item.getEnchantmentLevel(Enchantment.DURABILITY) + 1)));
        im.setDamage(im.getDamage() + damage);
        if (item.getType().getMaxDurability() - im.getDamage() <= 0) {
            getLogger().info("Damage on tool: " + im.getDamage());
            // todo: add destruction behaviour
        } else {
            item.setItemMeta((org.bukkit.inventory.meta.ItemMeta) im);
        }
        return item;
    }
}
