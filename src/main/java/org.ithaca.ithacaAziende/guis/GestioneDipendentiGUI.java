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
import java.util.UUID;

public class GestioneDipendenteGUI implements Listener {

    private final IthacaAziende plugin;
    private final Azienda azienda;
    private final Player player;
    private final UUID dipendenteUUID;
    public final DipendentiGUI previousGUI;
    private final Inventory inventory;
    private boolean returnToPrevious = true;

    public GestioneDipendenteGUI(IthacaAziende plugin, Azienda azienda, Player player, UUID dipendenteUUID, DipendentiGUI previousGUI) {
        this.plugin = plugin;
        this.azienda = azienda;
        this.player = player;
        this.dipendenteUUID = dipendenteUUID;
        this.previousGUI = previousGUI;

        OfflinePlayer dipendente = Bukkit.getOfflinePlayer(dipendenteUUID);
        this.inventory = Bukkit.createInventory(null, 27, ChatColor.AQUA + dipendente.getName());

        setupGUI();
    }

    private void setupGUI() {
        // Vetri di riempimento
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        // Stipendi
        ItemStack stipendi = new ItemStack(Material.DIAMOND);
        ItemMeta stipendiMeta = stipendi.getItemMeta();
        stipendiMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Stipendi");
        stipendiMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Gestisci lo stipendio",
                ChatColor.GRAY + "di questo dipendente"
        ));
        stipendi.setItemMeta(stipendiMeta);
        inventory.setItem(10, stipendi);

        // Pex/Ruoli
        ItemStack pex = new ItemStack(Material.NAME_TAG);
        ItemMeta pexMeta = pex.getItemMeta();
        pexMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Ruolo");

        DipendenteInfo info = azienda.getInfoDipendente(dipendenteUUID);
        if (info.getRuolo() != null) {
            pexMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Ruolo attuale:",
                    ChatColor.WHITE + info.getRuolo(),
                    "",
                    ChatColor.GRAY + "Clicca per cambiare"
            ));
        } else {
            pexMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Nessun ruolo assegnato",
                    "",
                    ChatColor.GRAY + "Clicca per assegnare"
            ));
        }
        pex.setItemMeta(pexMeta);
        inventory.setItem(11, pex);

        // Riga 3: Azioni e info
        // Licenzia
        ItemStack licenzia = new ItemStack(Material.OMINOUS_BOTTLE);
        ItemMeta licenziaMeta = licenzia.getItemMeta();
        licenziaMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Licenzia");
        licenziaMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Licenzia questo",
                ChatColor.GRAY + "dipendente"
        ));
        licenzia.setItemMeta(licenziaMeta);
        inventory.setItem(19, licenzia);

        // Incarichi
        ItemStack incarichi = new ItemStack(Material.BOOK);
        ItemMeta incarichiMeta = incarichi.getItemMeta();
        incarichiMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Incarichi");

        DipendenteInfo infoIncarichi = azienda.getInfoDipendente(dipendenteUUID);
        int numIncarichi = infoIncarichi.getIncarichi().size();
        int nonCompletati = infoIncarichi.getIncarichiNonCompletati().size();
        incarichiMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Incarichi totali: " + ChatColor.WHITE + numIncarichi,
                ChatColor.GRAY + "Non completati: " + ChatColor.YELLOW + nonCompletati,
                "",
                ChatColor.GRAY + "Clicca per gestire"
        ));
        incarichi.setItemMeta(incarichiMeta);
        inventory.setItem(20, incarichi);

        // Note
        ItemStack note = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta noteMeta = note.getItemMeta();
        noteMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Note");

        DipendenteInfo infoNote = azienda.getInfoDipendente(dipendenteUUID);
        int numNote = infoNote.getNote().size();
        noteMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Note totali: " + ChatColor.WHITE + numNote,
                "",
                ChatColor.GRAY + "Clicca per gestire le note"
        ));
        note.setItemMeta(noteMeta);
        inventory.setItem(21, note);

        // Richiami
        ItemStack richiami = new ItemStack(Material.BELL);
        ItemMeta richiamiMeta = richiami.getItemMeta();
        richiamiMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Richiami");

        DipendenteInfo infoRichiami = azienda.getInfoDipendente(dipendenteUUID);
        int numRichiami = infoRichiami.getRichiami().size();
        richiamiMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Richiami totali: " + ChatColor.WHITE + numRichiami,
                "",
                ChatColor.GRAY + "Clicca per gestire i richiami"
        ));
        richiami.setItemMeta(richiamiMeta);
        inventory.setItem(22, richiami);
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

        int slot = e.getSlot();

        switch (slot) {
            case 10: // Stipendi
                returnToPrevious = false;
                clicker.closeInventory();
                HandlerList.unregisterAll(this);
                ImpostaStipendioGUI stipendioGUI = new ImpostaStipendioGUI(plugin, azienda, clicker, dipendenteUUID, this);
                Bukkit.getScheduler().runTaskLater(plugin, stipendioGUI::open, 1L);
                break;

            case 11: // Ruolo
                returnToPrevious = false;
                clicker.closeInventory();
                HandlerList.unregisterAll(this);
                AssegnaRuoloGUI ruoloGUI = new AssegnaRuoloGUI(plugin, azienda, clicker, dipendenteUUID, this);
                Bukkit.getScheduler().runTaskLater(plugin, ruoloGUI::open, 1L);
                break;

            case 19: // Licenzia
                plugin.getAziendaManager().licenziaDipendente(azienda.getNome(), dipendenteUUID);
                OfflinePlayer dipendente = Bukkit.getOfflinePlayer(dipendenteUUID);
                clicker.sendMessage(ChatColor.GREEN + dipendente.getName() + " è stato licenziato!");

                Player targetOnline = Bukkit.getPlayer(dipendenteUUID);
                if (targetOnline != null && targetOnline.isOnline()) {
                    targetOnline.sendMessage(ChatColor.RED + "Sei stato licenziato da " + azienda.getNome() + "!");
                }

                returnToPrevious = false;
                clicker.closeInventory();
                HandlerList.unregisterAll(this);
                Bukkit.getScheduler().runTaskLater(plugin, () -> previousGUI.open(), 1L);
                break;

            case 20: // Incarichi
                returnToPrevious = false;
                clicker.closeInventory();
                HandlerList.unregisterAll(this);
                IncarichiGUI incarichiGUI = new IncarichiGUI(plugin, azienda, clicker, dipendenteUUID, this);
                Bukkit.getScheduler().runTaskLater(plugin, incarichiGUI::open, 1L);
                break;

            case 21: // Note
                returnToPrevious = false;
                clicker.closeInventory();
                HandlerList.unregisterAll(this);
                NoteGUI noteGUI = new NoteGUI(plugin, azienda, clicker, dipendenteUUID, this);
                Bukkit.getScheduler().runTaskLater(plugin, noteGUI::open, 1L);
                break;

            case 22: // Richiami
                returnToPrevious = false;
                clicker.closeInventory();
                HandlerList.unregisterAll(this);
                RichiamiGUI richiamiGUI = new RichiamiGUI(plugin, azienda, clicker, dipendenteUUID, this);
                Bukkit.getScheduler().runTaskLater(plugin, richiamiGUI::open, 1L);
                break;
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