package com.Chipmunk9998.Spectate.api;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import com.Chipmunk9998.Spectate.PlayerState;
import com.Chipmunk9998.Spectate.Spectate;

public class SpectateManager {

    private final Spectate                           plugin;
    private int                                      spectateTask     = -1;

    private final ArrayList<Player>                  isSpectating     = new ArrayList<Player>();
    private final ArrayList<Player>                  isBeingSpectated = new ArrayList<Player>();
    private final HashMap<Player, ArrayList<Player>> spectators       = new HashMap<Player, ArrayList<Player>>();
    private final HashMap<Player, Player>            target           = new HashMap<Player, Player>();

    private final ArrayList<String>                  isClick          = new ArrayList<String>();

    private final HashMap<String, SpectateMode>      playerMode       = new HashMap<String, SpectateMode>();
    private final HashMap<String, SpectateAngle>     playerAngle      = new HashMap<String, SpectateAngle>();

    private final ArrayList<String>                  isScanning       = new ArrayList<String>();
    private final HashMap<String, Integer>           scanTask         = new HashMap<String, Integer>();

    private final HashMap<Player, PlayerState>       states           = new HashMap<Player, PlayerState>();
    private final HashMap<Player, PlayerState>       multiInvStates   = new HashMap<Player, PlayerState>();

    private final ArrayList<String>                  inventoryOff     = new ArrayList<String>();

    public SpectateManager(Spectate plugin) {

        this.plugin = plugin;

    }

    private void updateSpectators() {

        spectateTask = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {

            @Override
            public void run() {

                for (final Player p: plugin.getServer().getOnlinePlayers()) {

                    if (isSpectating(p)) {

                        if (plugin.multiverseInvEnabled()) {

                            if (!p.getWorld().getName().equals(getTarget(p).getWorld().getName())) {

                                p.sendMessage(ChatColor.GRAY
                                        + "You were forced to stop spectating because the person you were spectating switched worlds.");
                                stopSpectating(p, true);
                                continue;

                            }

                        }

                        if (getSpectateAngle(p) == SpectateAngle.FIRST_PERSON) {

                            if (roundTwoDecimals(p.getLocation().getX()) != roundTwoDecimals(getTarget(p).getLocation()
                                    .getX())
                                    || roundTwoDecimals(p.getLocation().getY()) != roundTwoDecimals(getTarget(p)
                                            .getLocation().getY())
                                    || roundTwoDecimals(p.getLocation().getZ()) != roundTwoDecimals(getTarget(p)
                                            .getLocation().getZ())
                                    || roundTwoDecimals(p.getLocation().getYaw()) != roundTwoDecimals(getTarget(p)
                                            .getLocation().getYaw())
                                    || roundTwoDecimals(p.getLocation().getPitch()) != roundTwoDecimals(getTarget(p)
                                            .getLocation().getPitch())) {

                                p.teleport(getTarget(p));

                            }

                        } else {

                            if (getSpectateAngle(p) != SpectateAngle.FREEROAM) {

                                p.teleport(getSpectateLocation(p));

                            }

                        }

                        if (!inventoryOff.contains(p.getName())) {
                            p.getInventory().setContents(getTarget(p).getInventory().getContents());
                            p.getInventory().setArmorContents(getTarget(p).getInventory().getArmorContents());
                        }

                        if (getTarget(p).getHealth() == 0) {

                            p.setHealth(1);

                        } else {

                            if (getTarget(p).getHealth() < p.getHealth()) {

                                final double difference = p.getHealth() - getTarget(p).getHealth();
                                p.damage(difference);

                            } else if (getTarget(p).getHealth() > p.getHealth()) {

                                p.setHealth(getTarget(p).getHealth());

                            }

                        }

                        p.setLevel(getTarget(p).getLevel());
                        p.setExp(getTarget(p).getExp());

                        for (final PotionEffect e: p.getActivePotionEffects()) {

                            boolean foundPotion = false;

                            for (final PotionEffect e1: getTarget(p).getActivePotionEffects()) {

                                if (e1.getType() == e.getType()) {

                                    foundPotion = true;
                                    break;

                                }

                            }

                            if (!foundPotion) {

                                p.removePotionEffect(e.getType());

                            }

                        }

                        for (final PotionEffect e: getTarget(p).getActivePotionEffects()) {

                            p.addPotionEffect(e);

                        }

                        if (!inventoryOff.contains(p.getName())) {
                            p.getInventory().setHeldItemSlot(getTarget(p).getInventory().getHeldItemSlot());
                        }

                        if (getTarget(p).isFlying()) {

                            if (!p.isFlying()) {

                                p.setFlying(true);

                            }

                        }

                    }

                }

            }

        }, 0L, 1L);

    }

    public void startSpectateTask() {

        if (spectateTask == -1) {

            updateSpectators();

        }

    }

    public void stopSpectateTask() {

        if (spectateTask != -1) {

            plugin.getServer().getScheduler().cancelTask(spectateTask);
            spectateTask = -1;

        }

    }

    public void startSpectating(Player p, Player target, boolean saveState) {

        if (!isSpectating(p)) {

            if (saveState) {

                savePlayerState(p);

            }

        }

        boolean saveMultiInvState = false;

        if (plugin.multiverseInvEnabled()) {

            if (!p.getWorld().getName().equals(target.getWorld().getName())) {

                saveMultiInvState = true;

            }

        }

        for (final Player player1: plugin.getServer().getOnlinePlayers()) {

            player1.hidePlayer(p);

        }

        if (saveMultiInvState) {

            p.teleport(target.getWorld().getSpawnLocation());
            multiInvStates.put(p, new PlayerState(p));

        }

        final String playerListName = p.getPlayerListName();

        if (getSpectateAngle(p) == SpectateAngle.FIRST_PERSON) {

            p.hidePlayer(target);

        } else {

            p.showPlayer(target);

        }

        p.setPlayerListName(playerListName);

        p.setHealth(target.getHealth());

        p.teleport(target);

        if (isSpectating(p)) {

            setBeingSpectated(getTarget(p), false);
            p.showPlayer(getTarget(p));
            removeSpectator(getTarget(p), p);

        }

        for (final PotionEffect e: p.getActivePotionEffects()) {

            p.removePotionEffect(e.getType());

        }

        setTarget(p, target);
        addSpectator(target, p);

        p.setGameMode(target.getGameMode());
        p.setFoodLevel(target.getFoodLevel());

        setExperienceCooldown(p, Integer.MAX_VALUE);
        p.setAllowFlight(true);

        setSpectating(p, true);
        setBeingSpectated(target, true);

        p.sendMessage(ChatColor.GRAY + "You are now spectating " + target.getName() + ".");

    }

    public void stopSpectating(Player p, boolean loadState) {

        setSpectating(p, false);
        setBeingSpectated(getTarget(p), false);

        removeSpectator(getTarget(p), p);

        if (isScanning(p)) {

            stopScanning(p);

        }

        for (final PotionEffect e: p.getActivePotionEffects()) {

            p.removePotionEffect(e.getType());

        }

        if (loadState) {

            loadPlayerState(p);

        }

        setExperienceCooldown(p, 0);

        p.showPlayer(getTarget(p));

    }

    public boolean scrollRight(Player p, ArrayList<Player> playerList) {

        final SpectateScrollEvent event = new SpectateScrollEvent(p, playerList, ScrollDirection.RIGHT);
        plugin.getServer().getPluginManager().callEvent(event);

        playerList = new ArrayList<Player>(event.getSpectateList());

        playerList.remove(p);

        if (playerList.size() == 0) {

            return false;

        }

        if (plugin.multiverseInvEnabled()) {

            if (isScanning(p)) {

                for (final Player players: event.getSpectateList()) {

                    if (!players.getWorld().getName().equals(p.getWorld().getName())) {

                        playerList.remove(players);

                    }

                }

            }

        }

        int scrollToIndex;

        if (getScrollNumber(p, playerList) == playerList.size()) {

            scrollToIndex = 1;

        } else {

            scrollToIndex = getScrollNumber(p, playerList) + 1;

        }

        startSpectating(p, playerList.get(scrollToIndex - 1), false);

        return true;

    }

    public boolean scrollLeft(Player p, ArrayList<Player> playerList) {

        final SpectateScrollEvent event = new SpectateScrollEvent(p, playerList, ScrollDirection.LEFT);
        plugin.getServer().getPluginManager().callEvent(event);

        playerList = new ArrayList<Player>(event.getSpectateList());

        playerList.remove(p);

        if (playerList.size() == 0) {

            return false;

        }

        if (plugin.multiverseInvEnabled()) {

            if (isScanning(p)) {

                for (final Player players: event.getSpectateList()) {

                    if (!players.getWorld().getName().equals(p.getWorld().getName())) {

                        playerList.remove(players);

                    }

                }

            }

        }

        int scrollToIndex;

        if (getScrollNumber(p, playerList) == 1) {

            scrollToIndex = playerList.size();

        } else {

            scrollToIndex = getScrollNumber(p, playerList) - 1;

        }

        startSpectating(p, playerList.get(scrollToIndex - 1), false);

        return true;

    }

    public int getScrollNumber(Player p, ArrayList<Player> playerList) {

        if (!isSpectating(p)) {

            return 1;

        }

        if (!playerList.contains(getTarget(p))) {

            return 1;

        }

        playerList.remove(p);

        return playerList.indexOf(getTarget(p)) + 1;

    }

    public void setSpectateMode(Player p, SpectateMode newMode) {

        if (newMode == SpectateMode.DEFAULT) {

            playerMode.remove(p.getName());

        } else {

            playerMode.put(p.getName(), newMode);

        }

    }

    public SpectateMode getSpectateMode(Player p) {

        if (playerMode.get(p.getName()) == null) {

            return SpectateMode.DEFAULT;

        }

        return playerMode.get(p.getName());

    }

    public void setSpectateAngle(Player p, SpectateAngle newAngle) {

        if (isSpectating(p)) {

            if (newAngle == SpectateAngle.FIRST_PERSON) {

                p.hidePlayer(getTarget(p));

            } else {

                p.showPlayer(getTarget(p));

            }

            if (newAngle == SpectateAngle.FREEROAM) {

                p.teleport(getTarget(p));

            }

        }

        if (newAngle == SpectateAngle.FIRST_PERSON) {

            playerAngle.remove(p.getName());

        } else {

            playerAngle.put(p.getName(), newAngle);

        }

    }

    public SpectateAngle getSpectateAngle(Player p) {

        if (playerAngle.get(p.getName()) == null) {

            return SpectateAngle.FIRST_PERSON;

        }

        return playerAngle.get(p.getName());

    }

    public void startScanning(final Player p, int interval) {

        isScanning.add(p.getName());

        scanTask.put(p.getName(), plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {

            @Override
            public void run() {

                scrollRight(p, getSpectateablePlayers());

            }

        }, 0, 20 * interval));

    }

    public void stopScanning(Player p) {

        plugin.getServer().getScheduler().cancelTask(scanTask.get(p.getName()));
        isScanning.remove(p.getName());

    }

    public boolean isScanning(Player p) {

        return isScanning.contains(p.getName());

    }

    public ArrayList<Player> getSpectateablePlayers() {

        final ArrayList<Player> spectateablePlayers = new ArrayList<Player>();

        for (final Player onlinePlayers: plugin.getServer().getOnlinePlayers()) {

            if (onlinePlayers.isDead()) {

                continue;

            }

            if (isSpectating.contains(onlinePlayers)) {

                continue;

            }

            if (plugin.cantspectate_permission_enabled) {

                if (onlinePlayers.hasPermission("spectate.cantspectate")) {

                    continue;

                }

            }

            spectateablePlayers.add(onlinePlayers);

        }

        return spectateablePlayers;

    }

    private void setTarget(Player p, Player ptarget) {

        target.put(p, ptarget);

    }

    public Player getTarget(Player p) {

        return target.get(p);

    }

    public boolean isSpectating(Player p) {

        return isSpectating.contains(p);

    }

    public boolean isBeingSpectated(Player p) {

        return isBeingSpectated.contains(p);

    }

    private void setBeingSpectated(Player p, boolean beingSpectated) {

        if (beingSpectated) {

            if (isBeingSpectated.contains(p)) {

                return;

            }

            isBeingSpectated.add(p);

        } else {

            isBeingSpectated.remove(p);

        }

    }

    private void addSpectator(Player p, Player spectator) {

        if (spectators.get(p) == null) {

            final ArrayList<Player> newSpectators = new ArrayList<Player>();

            newSpectators.add(spectator);

            spectators.put(p, newSpectators);

        } else {

            spectators.get(p).add(spectator);

        }

    }

    private void removeSpectator(Player p, Player spectator) {

        if (spectators.get(p) != null) {

            if (spectators.get(p).size() == 1) {

                spectators.remove(p);

            } else {

                spectators.get(p).remove(spectator);

            }

        }

    }

    public ArrayList<Player> getSpectators(Player p) {

        return (spectators.get(p) == null ? new ArrayList<Player>() : spectators.get(p));

    }

    public ArrayList<Player> getSpectatingPlayers() {

        final ArrayList<Player> spectatingPlayers = new ArrayList<Player>();

        for (final Player p: plugin.getServer().getOnlinePlayers()) {

            if (isSpectating(p)) {

                spectatingPlayers.add(p);

            }

        }

        return spectatingPlayers;

    }

    private void setSpectating(Player p, boolean spectating) {

        if (spectating) {

            if (isSpectating.contains(p)) {

                return;

            }

            isSpectating.add(p);

        } else {

            isSpectating.remove(p);

        }

    }

    public void setModifyInventory(Player p, boolean modify) {
        if (modify) {
            if (inventoryOff.contains(p.getName())) {
                inventoryOff.remove(p.getName());
            }
        } else {
            if (!inventoryOff.contains(p.getName())) {
                inventoryOff.add(p.getName());
            }
        }
    }

    public void disableScroll(final Player player, long ticks) {

        if (!isClick.contains(player.getName())) {

            isClick.add(player.getName());

            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

                @Override
                public void run() {

                    isClick.remove(player.getName());

                }

            }, ticks);

        }

    }

    public Location getSpectateLocation(Player p) {

        if (getSpectateAngle(p) == SpectateAngle.FIRST_PERSON) {

            return (getTarget(p).getLocation());

        }

        Location playerLoc = getTarget(p).getLocation();

        double currentSubtraction = 0;
        Location previousLoc = playerLoc;

        while (currentSubtraction <= 5) {

            playerLoc = getTarget(p).getLocation();

            final Vector v = getTarget(p).getLocation().getDirection().normalize();
            v.multiply(currentSubtraction);

            if (getSpectateAngle(p) == SpectateAngle.THIRD_PERSON) {

                playerLoc.subtract(v);

            } else if (getSpectateAngle(p) == SpectateAngle.THIRD_PERSON_FRONT) {

                playerLoc.add(v);

                if (playerLoc.getYaw() < -180) {

                    playerLoc.setYaw(playerLoc.getYaw() + 180);

                } else {

                    playerLoc.setYaw(playerLoc.getYaw() - 180);

                }

                playerLoc.setPitch(-playerLoc.getPitch());

            }

            final Material tempMat = new Location(playerLoc.getWorld(), playerLoc.getX(), playerLoc.getY() + 1.5,
                    playerLoc.getZ()).getBlock().getType();

            if (tempMat != Material.AIR && tempMat != Material.WATER && tempMat != Material.STATIONARY_WATER) {

                return previousLoc;

            }

            previousLoc = playerLoc;

            currentSubtraction += 0.5;

        }

        return playerLoc;

    }

    public PlayerState getPlayerState(Player p) {

        return states.get(p);

    }

    public void savePlayerState(Player p) {

        final PlayerState playerstate = new PlayerState(p);
        states.put(p, playerstate);

    }

    public void loadPlayerState(Player toPlayer) {

        loadPlayerState(toPlayer, toPlayer);

    }

    public void loadPlayerState(Player fromState, Player toPlayer) {

        if (plugin.multiverseInvEnabled() && multiInvStates.get(fromState) != null) {

            loadFinalState(multiInvStates.get(fromState), toPlayer);
            multiInvStates.remove(fromState);

        }

        loadFinalState(getPlayerState(fromState), toPlayer);
        states.remove(fromState);

    }

    private void loadFinalState(PlayerState state, Player toPlayer) {

        toPlayer.teleport(state.location);

        toPlayer.getInventory().setContents(state.inventory);
        toPlayer.getInventory().setArmorContents(state.armor);
        toPlayer.setFoodLevel(state.hunger);
        toPlayer.setHealth(state.health);
        toPlayer.setLevel(state.level);
        toPlayer.setExp(state.exp);
        toPlayer.getInventory().setHeldItemSlot(state.slot);
        toPlayer.setAllowFlight(state.allowFlight);
        toPlayer.setFlying(state.isFlying);
        toPlayer.setGameMode(state.mode);

        for (final Player onlinePlayers: plugin.getServer().getOnlinePlayers()) {

            if (!state.vanishedFrom.contains(onlinePlayers)) {

                onlinePlayers.showPlayer(toPlayer);

            }

        }

        for (final PotionEffect e: state.potions) {

            toPlayer.addPotionEffect(e);

        }

    }

    public ArrayList<Player> getVanishedFromList(Player p) {

        return getPlayerState(p).vanishedFrom;

    }

    public void setExperienceCooldown(Player p, int cooldown) {

        try {

            final Method handle = p.getClass().getDeclaredMethod("getHandle");
            final Object entityPlayer = handle.invoke(p);
            final Field cooldownField = entityPlayer.getClass().getSuperclass().getDeclaredField("bu");
            cooldownField.setAccessible(true);
            cooldownField.setInt(entityPlayer, cooldown);

        } catch (final Exception e) {

            e.printStackTrace();

        }

    }

    public boolean isReadyForNextScroll(Player p) {

        return !isClick.contains(p.getName());

    }

    public double roundTwoDecimals(double d) {

        try {

            final DecimalFormat twoDForm = new DecimalFormat("#.##");
            return Double.valueOf(twoDForm.format(d));

        } catch (final NumberFormatException e) {

            return d;

        }

    }

}
