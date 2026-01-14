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
import org.ithaca.ithacaAziende.models.DipendenteInfo;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

public class AssegnaRuoloGUI implements Listener {

    private final IthacaAziende plugin;
    private final Azienda azienda;
    private final Player player;
    private final UUID dipendenteUUID;
    private final GestioneDipendenteGUI previousGUI;
    private final Inventory inventory;
    private boolean returnToPrevious = true;

    public AssegnaRuoloGUI(IthacaAziende plugin, Azienda azienda, Player player, UUID dipendenteUUID, GestioneDipendenteGUI previousGUI) {
        this.plugin = plugin;
        this.azienda = azienda;
        this.player = player;
        this.dipendenteUUID = dipendenteUUID;
        this.previousGUI = previousGUI;

        OfflinePlayer dipendente = Bukkit.getOfflinePlayer(dipendenteUUID);
        this.inventory = Bukkit.createInventory(null, 54, ChatColor.YELLOW + "Assegna Ruolo - " + dipendente.getName());

        setupGUI();
    }

    private void setupGUI() {
        Set<String> ruoli = azienda.getRuoli();

        if (ruoli.isEmpty()) {
            ItemStack noRoles = new ItemStack(Material.BARRIER);
            ItemMeta meta = noRoles.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "Nessun ruolo disponibile");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Crea prima dei ruoli",
                    ChatColor.GRAY + "nella sezione Ruoli"
            ));
            noRoles.setItemMeta(meta);
            inventory.setItem(22, noRoles);
            return;
        }

        String ruoloAttuale = azienda.getInfoDipendente(dipendenteUUID).getRuolo();

        int slot = 0;
        for (String ruolo : ruoli) {
            if (slot >= 54) break;

            ItemStack paper = new ItemStack(Material.NAME_TAG);
            ItemMeta meta = paper.getItemMeta();

            if (ruolo.equals(ruoloAttuale)) {
                meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + ruolo + " ✓");
                meta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Ruolo attualmente assegnato"
                ));
            } else {
                meta.setDisplayName(ChatColor.YELLOW + ruolo);
                meta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Clicca per assegnare"
                ));
            }

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
        if (!(e.getWhoClicked() instanceof Player)) return;

        Player clicker = (Player) e.getWhoClicked();
        e.setCancelled(true);

        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;
        if (e.getCurrentItem().getType() != Material.NAME_TAG) return;

        ItemMeta meta = e.getCurrentItem().getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String ruolo = ChatColor.stripColor(meta.getDisplayName()).replace(" ✓", "").trim();

        azienda.setRuoloDipendente(dipendenteUUID, ruolo);
        plugin.getAziendaManager().applicaTarghetta(azienda, dipendenteUUID);
        plugin.getAziendaManager().saveAll();

        OfflinePlayer dipendente = Bukkit.getOfflinePlayer(dipendenteUUID);
        clicker.sendMessage(ChatColor.GREEN + "Ruolo '" + ruolo + "' assegnato a " + dipendente.getName() + "!");

        returnToPrevious = false;
        clicker.closeInventory();
        HandlerList.unregisterAll(this);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            GestioneDipendenteGUI nuovaGUI = new GestioneDipendenteGUI(plugin, azienda, clicker, dipendenteUUID, previousGUI.previousGUI);
            nuovaGUI.open();
        }, 1L);
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