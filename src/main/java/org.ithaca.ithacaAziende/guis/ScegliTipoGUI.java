package org.ithaca.ithacaAziende.guis;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.conversations.*;
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

import java.util.Arrays;
import java.util.Set;

public class ScegliTipoGUI implements Listener {

    private final IthacaAziende plugin;
    private final Azienda azienda;
    private final Player player;
    private final ConcorsiGUI previousGUI;
    private final Inventory inventory;
    private boolean returnToPrevious = true;

    public ScegliTipoGUI(IthacaAziende plugin, Azienda azienda, Player player, ConcorsiGUI previousGUI) {
        this.plugin = plugin;
        this.azienda = azienda;
        this.player = player;
        this.previousGUI = previousGUI;
        this.inventory = Bukkit.createInventory(null, 54, ChatColor.GREEN + "" + ChatColor.BOLD + "Scegli Tipo Concorso");

        setupGUI();
    }

    private void setupGUI() {
        Set<String> tipi = azienda.getRuoli();

        if (tipi.isEmpty()) {
            ItemStack noRoles = new ItemStack(Material.BARRIER);
            ItemMeta meta = noRoles.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "Nessun ruolo disponibile");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Devi prima creare",
                    ChatColor.GRAY + "dei ruoli nella sezione Ruoli"
            ));
            noRoles.setItemMeta(meta);
            inventory.setItem(22, noRoles);
        } else {
            int slot = 0;
            for (String tipo : tipi) {
                if (slot >= 54) break;

                ItemStack paper = new ItemStack(Material.PAPER);
                ItemMeta meta = paper.getItemMeta();
                meta.setDisplayName(ChatColor.AQUA + tipo);
                meta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Clicca per selezionare",
                        ChatColor.GRAY + "questo tipo di concorso"
                ));
                paper.setItemMeta(meta);

                inventory.setItem(slot++, paper);
            }
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

        ItemMeta meta = e.getCurrentItem().getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        if (e.getCurrentItem().getType() == Material.PAPER) {
            String tipoSelezionato = ChatColor.stripColor(meta.getDisplayName());

            returnToPrevious = false;
            clicker.closeInventory();
            HandlerList.unregisterAll(this);

            CreaConcorsoInput input = new CreaConcorsoInput(plugin, azienda, clicker, tipoSelezionato, previousGUI);
            input.start();
        }
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