package org.ithaca.ithacaAziende.guis;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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
import org.ithaca.ithacaAziende.models.DipendenteInfo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class NoteGUI implements Listener {

    private final IthacaAziende plugin;
    private final Azienda azienda;
    private final Player player;
    private final UUID dipendenteUUID;
    private final GestioneDipendenteGUI previousGUI;
    private final Inventory inventory;
    private boolean returnToPrevious = true;

    public NoteGUI(IthacaAziende plugin, Azienda azienda, Player player, UUID dipendenteUUID, GestioneDipendenteGUI previousGUI) {
        this.plugin = plugin;
        this.azienda = azienda;
        this.player = player;
        this.dipendenteUUID = dipendenteUUID;
        this.previousGUI = previousGUI;

        OfflinePlayer dipendente = Bukkit.getOfflinePlayer(dipendenteUUID);
        this.inventory = Bukkit.createInventory(null, 54, ChatColor.AQUA + "Note - " + dipendente.getName());

        setupGUI();
    }

    private void setupGUI() {
        DipendenteInfo info = azienda.getInfoDipendente(dipendenteUUID);
        List<DipendenteInfo.Nota> note = info.getNote();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        if (note.isEmpty()) {
            ItemStack noNotes = new ItemStack(Material.BARRIER);
            ItemMeta meta = noNotes.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "Nessuna nota");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Non ci sono note",
                    ChatColor.GRAY + "per questo dipendente"
            ));
            noNotes.setItemMeta(meta);
            inventory.setItem(22, noNotes);
        } else {
            int slot = 0;
            for (int i = 0; i < Math.min(45, note.size()); i++) {
                DipendenteInfo.Nota nota = note.get(i);

                ItemStack paper = new ItemStack(Material.PAPER);
                ItemMeta meta = paper.getItemMeta();
                meta.setDisplayName(ChatColor.AQUA + "Nota #" + (i + 1));

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Data: " + ChatColor.WHITE + nota.getData().format(formatter));
                lore.add("");

                String[] lines = nota.getTesto().split("(?<=\\G.{30})");
                for (String line : lines) {
                    lore.add(ChatColor.WHITE + line);
                }

                lore.add("");
                lore.add(ChatColor.RED + "Click destro per eliminare");

                meta.setLore(lore);
                paper.setItemMeta(meta);

                inventory.setItem(slot++, paper);
            }
        }

        ItemStack aggiungi = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta aggiungiMeta = aggiungi.getItemMeta();
        aggiungiMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Aggiungi Nota");
        aggiungiMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Clicca per aggiungere",
                ChatColor.GRAY + "una nuova nota"
        ));
        aggiungi.setItemMeta(aggiungiMeta);
        inventory.setItem(53, aggiungi);
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

        if (e.getSlot() == 53 && e.getCurrentItem().getType() == Material.WRITABLE_BOOK) {
            returnToPrevious = false;
            clicker.closeInventory();
            HandlerList.unregisterAll(this);

            startAggiungiNota();
        } else if (e.getCurrentItem().getType() == Material.PAPER && e.isRightClick()) {
            ItemMeta meta = e.getCurrentItem().getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String displayName = ChatColor.stripColor(meta.getDisplayName());
                if (displayName.startsWith("Nota #")) {
                    try {
                        int index = Integer.parseInt(displayName.substring(6)) - 1;
                        azienda.getInfoDipendente(dipendenteUUID).rimuoviNota(index);
                        plugin.getAziendaManager().saveAll();

                        clicker.sendMessage(ChatColor.GREEN + "Nota eliminata!");

                        returnToPrevious = false;
                        clicker.closeInventory();
                        HandlerList.unregisterAll(this);

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            NoteGUI nuovaGUI = new NoteGUI(plugin, azienda, clicker, dipendenteUUID, previousGUI);
                            nuovaGUI.open();
                        }, 1L);
                    } catch (NumberFormatException ignored) {}
                }
            }
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

    private void startAggiungiNota() {
        ConversationFactory factory = new ConversationFactory(plugin)
                .withFirstPrompt(new NotaPrompt())
                .withLocalEcho(false)
                .withTimeout(120)
                .withPrefix(context -> ChatColor.AQUA + "[Note] " + ChatColor.RESET)
                .addConversationAbandonedListener(event -> {
                    if (!event.gracefulExit()) {
                        player.sendMessage(ChatColor.AQUA + "[Note] " + ChatColor.RED + "Operazione annullata.");
                        Bukkit.getScheduler().runTaskLater(plugin, this::open, 1L);
                    }
                });

        factory.buildConversation(player).begin();
    }

    private class NotaPrompt extends StringPrompt {
        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.YELLOW + "Inserisci il testo della nota (o scrivi 'annulla'):";
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            if (input.equalsIgnoreCase("annulla")) {
                player.sendMessage(ChatColor.AQUA + "[Note] " + ChatColor.YELLOW + "Operazione annullata.");
                Bukkit.getScheduler().runTaskLater(plugin, () -> open(), 1L);
                return Prompt.END_OF_CONVERSATION;
            }

            if (input == null || input.trim().isEmpty()) {
                player.sendMessage(ChatColor.AQUA + "[Note] " + ChatColor.RED + "Devi inserire del testo!");
                return this;
            }

            azienda.aggiungiNotaDipendente(dipendenteUUID, input.trim(), LocalDate.now());
            plugin.getAziendaManager().saveAll();

            player.sendMessage(ChatColor.AQUA + "[Note] " + ChatColor.GREEN + "Nota aggiunta con successo!");

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                NoteGUI nuovaGUI = new NoteGUI(plugin, azienda, player, dipendenteUUID, previousGUI);
                nuovaGUI.open();
            }, 1L);

            return Prompt.END_OF_CONVERSATION;
        }
    }
}