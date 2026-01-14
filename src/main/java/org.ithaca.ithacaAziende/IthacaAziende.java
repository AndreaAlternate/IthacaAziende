package org.ithaca.ithacaAziende;

import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.ithaca.ithacaAziende.commands.AziendaCommand;
import org.ithaca.ithacaAziende.commands.AziendaTabCompleter;
import org.ithaca.ithacaAziende.listeners.PCPlaceListener;
import org.ithaca.ithacaAziende.listeners.PlayerJoinListener;
import org.ithaca.ithacaAziende.managers.AziendaManager;
import org.ithaca.ithacaAziende.managers.ConcorsoManager;
import org.ithaca.ithacaAziende.placeholders.AziendaPlaceholder;

public class IthacaAziende extends JavaPlugin {

    private static IthacaAziende instance;
    private LuckPerms luckPerms;
    private AziendaManager aziendaManager;
    private ConcorsoManager concorsoManager;

    @Override
    public void onEnable() {
        instance = this;

        // Setup LuckPerms
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPerms = provider.getProvider();
            getLogger().info("LuckPerms hooked successfully!");
        } else {
            getLogger().severe("LuckPerms not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize managers
        aziendaManager = new AziendaManager(this);
        concorsoManager = new ConcorsoManager(this);

        // Register commands
        getCommand("azienda").setExecutor(new AziendaCommand(this));
        getCommand("azienda").setTabCompleter(new AziendaTabCompleter(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(new PCPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        // Register PlaceholderAPI expansion
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new AziendaPlaceholder(this).register();
            getLogger().info("PlaceholderAPI hooked successfully!");
        }

        getLogger().info("IthacaAziende enabled successfully!");


    }

    @Override
    public void onDisable() {
        if (aziendaManager != null) {
            aziendaManager.saveAll();
        }
        if (concorsoManager != null) {
            concorsoManager.saveAll();
        }
        getLogger().info("IthacaAziende disabled!");
    }

    public static IthacaAziende getInstance() {
        return instance;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public AziendaManager getAziendaManager() {
        return aziendaManager;
    }

    public ConcorsoManager getConcorsoManager() {
        return concorsoManager;
    }
}