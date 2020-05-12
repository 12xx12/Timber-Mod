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

import java.util.LinkedList;
import java.util.List;

import static java.lang.Math.*;

public class Timber extends JavaPlugin implements Listener {

    static boolean withWorldGuard;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
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
            if (isAxe(e.getPlayer().getInventory().getItemInMainHand()))
                if (isLog(e.getBlock().getType()))
                    dropTree(e.getBlock().getLocation(), e.getPlayer());

    }

    private void dropTree(final Location location, final Player player) {
        /**
         * Drops the tree at the given location
         *
         * @author Marc
         * @version 2.0
         * @since 2020-05-05
         */
        List<Block> blocks = new LinkedList<>();
        List<Block> checkedLeaves = new LinkedList<>();

        Location origin = location.clone();
        Location leaveLocation = location.clone();
        blocks.addAll(leaveDrop(leaveLocation, origin, checkedLeaves));

        // org.bukkit.inventory.meta.Damageable tool = (org.bukkit.inventory.meta.Damageable) player.getInventory().getItemInMainHand().getItemMeta();

        for (Block block : blocks) {
            block.breakNaturally(player.getInventory().getItemInMainHand());
        }
        damageItem(player.getInventory().getItemInMainHand(), blocks.size());

        player.updateInventory();
}

    private List<Block> leaveDrop(Location location, final Location origin, List<Block> checkedLeaves) {
        /**
         * adds connected blocks to the removal list.
         * checks surroundings for leaves and Logs and adds them
         * Logs directly if connected to the original block and
         * for leaves checks if is not part of another tree
         *
         * @author Marc
         * @version 2.0
         * @since 2020-05-05
         *
         * @param location location of the
         * @param origin block which was already mined
         * @param checkedLeaves leaves already checked
         * @return list of blocks to break
         */
        List<Block> breakBlogs = new LinkedList<>(); // list returned with the blocks to break
        final float border = getConfig().getInt("maxChop"); // maximum distance to original broken block
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Location dest = location.getBlock().getRelative(x, y, z).getLocation();
                    if (!(checkedLeaves.contains(dest.getBlock()))) {
                        checkedLeaves.add(dest.clone().getBlock()); // adds to already checked blocks
                        if (isLog(dest.getBlock().getType()) && distance(location, origin) < border) {
                            breakBlogs.add(dest.clone().getBlock()); // adds to blocks to break
                            breakBlogs.addAll(leaveDrop(dest, origin, checkedLeaves));
                        }
                    }
                }
            }
        }
        return breakBlogs;
    }

    public WorldGuardPlugin getWorldGuard() {
        /**
         * returns the WorldGuard plugin to use
         *
         * @author MaLo
         * @version 1.0
         * @since 2020-05-05
         * */
        WorldGuardPlugin plugin;
        try {
            plugin = (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin("WorldGuard");
        } catch (UnknownDependencyException e) {
            plugin = null;
        }
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return null;
        }
        return plugin;
    }

    private boolean isAxe(ItemStack material) {
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

    private boolean isLog(Material material) {
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

    private int distance(Location location, Location origin) {
        int dis;
        dis = (int) round(sqrt(pow(location.getBlockX() - origin.getBlockX(), 2) + pow(location.getBlockZ() - origin.getBlockZ(), 2)));
        dis += round(abs(location.getBlockY() - origin.getBlockY()) / 8.0);
        return dis;
    }

    private ItemStack damageItem(ItemStack item, int damage) {
        /**
         * damages the given item by the given value, respecting unbreaking.
         *
         * @author Marc
         * @version 1.0
         * @since 2020-05-05
         *
         * @param item - item to damage
         * @param damage - damage to deal to item
         *
         * @return the damaged item
         */
        org.bukkit.inventory.meta.Damageable im = (org.bukkit.inventory.meta.Damageable) item.getItemMeta();
        // checks for unbraking and respects it in the damage value
        if (item.containsEnchantment(Enchantment.DURABILITY)) {
            damage = (int) round(damage * (1.0 / (item.getEnchantmentLevel(Enchantment.DURABILITY) + 1)));
        }
        im.setDamage(im.getDamage() + damage);
        if (im.getDamage() <= item.getType().getMaxDurability()) {
            item = null;
        } else {
            item.setItemMeta((org.bukkit.inventory.meta.ItemMeta) im);
        }
        return item;
    }
}