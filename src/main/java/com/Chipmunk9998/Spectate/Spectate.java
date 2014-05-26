package com.Chipmunk9998.Spectate;

import java.io.File;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.Chipmunk9998.Spectate.api.SpectateManager;

public class Spectate extends JavaPlugin {

    // TODO: (IN THE FUTURE - DON'T HAVE TIME FOR THIS RIGHT NOW) Control
    // command

    private static SpectateManager Manager;

    public boolean                 cantspectate_permission_enabled = false;
    public boolean                 disable_commands                = false;

    @Override
    public void onEnable() {

        Spectate.Manager = new SpectateManager(this);

        boolean convertcantspectate = false;
        boolean convertdisable = false;

        final File configFile = new File(getDataFolder(), "config.yml");

        if (configFile.exists()) {

            final FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            if (config.get("cantspectate Permission Enabled?") != null) {

                convertcantspectate = config.getBoolean("cantspectate Permission Enabled?");
                convertdisable = config.getBoolean("Disable commands while spectating?");

                config.set("cantspectate Permission Enabled?", null);
                config.set("Disable commands while spectating?", null);

                try {

                    config.save(configFile);

                } catch (final IOException e) {

                    e.printStackTrace();

                }

            }

        }

        loadConfig();

        if (convertcantspectate || convertdisable) {

            final File configFile1 = new File(getDataFolder(), "config.yml");
            final FileConfiguration config = YamlConfiguration.loadConfiguration(configFile1);
            config.set("cantspectate-permission-enabled", convertcantspectate);
            config.set("disable-commands-while-spectating", convertdisable);

            try {

                config.save(configFile1);

            } catch (final IOException e) {

                e.printStackTrace();

            }

            loadConfig();

        }

        getServer().getPluginManager().registerEvents(new SpectateListener(this), this);
        getCommand("spectate").setExecutor(new SpectateCommandExecutor(this));
        Spectate.getAPI().startSpectateTask();

    }

    @Override
    public void onDisable() {

        for (final Player p: Spectate.getAPI().getSpectatingPlayers()) {

            Spectate.getAPI().stopSpectating(p, true);
            p.sendMessage(ChatColor.GRAY + "You were forced to stop spectating because of a server reload.");

        }

        Spectate.getAPI().stopSpectateTask();

    }

    public static SpectateManager getAPI() {

        return Spectate.Manager;

    }

    public boolean multiverseInvEnabled() {

        return getServer().getPluginManager().getPlugin("Multiverse-Inventories") != null
                && getServer().getPluginManager().getPlugin("Multiverse-Inventories").isEnabled();

    }

    public void loadConfig() {

        saveDefaultConfig();

        final File configFile = new File(getDataFolder(), "config.yml");
        final FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        cantspectate_permission_enabled = config.getBoolean("cantspectate-permission-enabled");
        disable_commands = config.getBoolean("disable-commands-while-spectating");

    }

}
