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
import org.ithaca.ithacaAziende.IthacaAziende;
import org.ithaca.ithacaAziende.models.Azienda;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TransazioniGUI implements Listener {

    private final IthacaAziende plugin;
    private final Azienda azienda;
    private final Player player;
    private final PCMainGUI previousGUI;
    private final Inventory inventory;

    public TransazioniGUI(IthacaAziende plugin, Azienda azienda, Player player, PCMainGUI previousGUI) {
        this.plugin = plugin;
        this.azienda = azienda;
        this.player = player;
        this.previousGUI = previousGUI;
        this.inventory = Bukkit.createInventory(null, 54, ChatColor.YELLOW + "" + ChatColor.BOLD + "Transazioni");

        setupGUI();
    }

    private void setupGUI() {
        List<Azienda.Transazione> transazioni = azienda.getTransazioni();

        if (transazioni.isEmpty()) {
            ItemStack noTransactions = new ItemStack(Material.BARRIER);
            ItemMeta meta = noTransactions.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "Nessuna transazione");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Non ci sono ancora",
                    ChatColor.GRAY + "transazioni registrate"
            ));
            noTransactions.setItemMeta(meta);
            inventory.setItem(22, noTransactions);
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        int slot = 0;

        int start = Math.max(0, transazioni.size() - 54);
        for (int i = start; i < transazioni.size(); i++) {
            Azienda.Transazione t = transazioni.get(i);

            ItemStack paper = new ItemStack(Material.PAPER);
            ItemMeta meta = paper.getItemMeta();

            OfflinePlayer responsabile = Bukkit.getOfflinePlayer(t.getResponsabile());

            meta.setDisplayName(ChatColor.YELLOW + t.getDescrizione());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Importo: " + (t.getImporto() >= 0 ? ChatColor.GREEN : ChatColor.RED) + "€" + t.getImporto());
            lore.add(ChatColor.GRAY + "Data: " + ChatColor.WHITE + t.getData().format(formatter));
            lore.add(ChatColor.GRAY + "Responsabile: " + ChatColor.WHITE + responsabile.getName());

            meta.setLore(lore);
            paper.setItemMeta(meta);

            inventory.setItem(slot++, paper);
        }
    }

    public void open() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getInventory().equals(inventory)) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!e.getInventory().equals(inventory)) return;
        if (!(e.getPlayer() instanceof Player)) return;

        Player p = (Player) e.getPlayer();

        if (previousGUI != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> previousGUI.open(), 1L);
        }

        HandlerList.unregisterAll(this);
    }
}