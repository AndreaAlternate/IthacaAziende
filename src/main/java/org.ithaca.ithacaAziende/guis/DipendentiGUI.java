package org.ithaca.ithacaAziende.guis;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.ithaca.ithacaAziende.IthacaAziende;
import org.ithaca.ithacaAziende.models.Azienda;

import java.util.Arrays;
import java.util.UUID;

public class DipendentiGUI implements Listener {

    private final IthacaAziende plugin;
    private final Azienda azienda;
    private final Player player;
    private final PCMainGUI previousGUI;
    private final Inventory inventory;
    private boolean returnToPrevious = true;

    public DipendentiGUI(IthacaAziende plugin, Azienda azienda, Player player, PCMainGUI previousGUI) {
        this.plugin = plugin;
        this.azienda = azienda;
        this.player = player;
        this.previousGUI = previousGUI;
        this.inventory = Bukkit.createInventory(null, 54, ChatColor.AQUA + "" + ChatColor.BOLD + "Gestione Dipendenti");

        setupGUI();
    }

    private void setupGUI() {
        if (azienda.getDipendenti().isEmpty()) {
            ItemStack noEmployees = new ItemStack(Material.BARRIER);
            ItemMeta meta = noEmployees.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "Nessun dipendente");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Non ci sono dipendenti",
                    ChatColor.GRAY + "nell'azienda"
            ));
            noEmployees.setItemMeta(meta);
            inventory.setItem(22, noEmployees);
            return;
        }

        int slot = 0;
        for (UUID uuid : azienda.getDipendenti()) {
            if (slot >= 54) break;

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.setOwningPlayer(offlinePlayer);
            meta.setDisplayName(ChatColor.AQUA + offlinePlayer.getName());
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Clicca per gestire",
                    ChatColor.GRAY + "questo dipendente"
            ));
            skull.setItemMeta(meta);

            inventory.setItem(slot++, skull);
        }
    }

    public void open() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getInventory().equals(inventory)) return;
        if (!(e.getWhoClicked() instanceof Player)) return;

        Player clicker = (Player) e.getWhoClicked();
        e.setCancelled(true);

        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;
        if (e.getCurrentItem().getType() != Material.PLAYER_HEAD) return;

        SkullMeta meta = (SkullMeta) e.getCurrentItem().getItemMeta();
        if (meta.getOwningPlayer() == null) return;

        UUID targetUUID = meta.getOwningPlayer().getUniqueId();

        returnToPrevious = false;
        clicker.closeInventory();
        HandlerList.unregisterAll(this);

        GestioneDipendenteGUI gestioneGUI = new GestioneDipendenteGUI(plugin, azienda, clicker, targetUUID, this);
        Bukkit.getScheduler().runTaskLater(plugin, gestioneGUI::open, 1L);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!e.getInventory().equals(inventory)) return;
        if (!(e.getPlayer() instanceof Player)) return;

        Player p = (Player) e.getPlayer();

        if (returnToPrevious && previousGUI != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> previousGUI.open(), 1L);
        }

        HandlerList.unregisterAll(this);
    }
}