package com.minecraftly.modules.spawn;

import com.minecraftly.core.bukkit.MinecraftlyCore;
import com.minecraftly.core.bukkit.module.Module;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Iterator;
import java.util.Set;

public class SpawnModule extends Module implements Listener {

    private MinecraftlyCore plugin;

    private World spawnWorld = null;

    @Override
    protected void onEnable(MinecraftlyCore plugin) {
        this.plugin = plugin;
        registerListener(this);
    }

    public World getSpawnWorld() {
        if (spawnWorld == null) {
            spawnWorld = Bukkit.getWorlds().get(0);
        }

        return spawnWorld;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        player.teleport(getSpawnWorld().getSpawnLocation());
        makePlayerDisabled(player, player.getWorld());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        Player player = e.getPlayer();
        World to = e.getTo().getWorld();
        World from = e.getFrom().getWorld();

        World spawnWorld = getSpawnWorld();

        if (!to.equals(from)) {
            if (to.equals(spawnWorld)) {
                makePlayerDisabled(player, spawnWorld);
            } else if (from.equals(spawnWorld)) {
                player.removePotionEffect(PotionEffectType.BLINDNESS);
                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                player.removePotionEffect(PotionEffectType.JUMP);
                player.setWalkSpeed(0.2F);
                player.setGameMode(GameMode.SURVIVAL); // todo survival worlds?
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        Set<Player> recipients = e.getRecipients();
        Player player = e.getPlayer();
        World spawnWorld = Bukkit.getWorlds().get(0);

        if (player.getWorld() == spawnWorld) {
            recipients.clear();
        } else {
            Iterator<Player> iterator = recipients.iterator();

            while (iterator.hasNext()) {
                Player recipient = iterator.next();
                if (recipient.getWorld() == spawnWorld) {
                    iterator.remove();
                }
            }
        }
    }

    private void makePlayerDisabled(final Player player, final World world) { // lol
        for (Player player1 : world.getPlayers()) {
            if (player != player1) {
                player.hidePlayer(player1);
                player1.hidePlayer(player);
            }
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 200, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 200, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 250, true, false));
        player.setWalkSpeed(0);
        player.setGameMode(GameMode.ADVENTURE);

        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (player.getWorld() == world) { // check player hasn't changed world
                    player.getInventory().clear();
                    player.getEnderChest().clear();
                }
            }
        }, 10L);
    }

    @Override
    public ChunkGenerator getWorldGenerator(String worldName, String id) {
        Validate.notEmpty(id, "Generator settings missing.");

        String[] parts = id.split(",");
        String materialName = parts.length > 0 ? parts[0] : id;
        Material material = Material.matchMaterial(materialName);
        Validate.notNull(material, "Invalid material type: " + materialName);
        Validate.isTrue(material.isBlock(), material + " is not a block (likely an item).");
        Validate.isTrue(material.isSolid(), material + " is not a solid block (players will fall through this).");

        return new VoidGenerator(material);
    }
}