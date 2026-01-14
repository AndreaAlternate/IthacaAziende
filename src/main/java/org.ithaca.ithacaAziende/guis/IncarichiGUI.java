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

public class IncarichiGUI implements Listener {

    private final IthacaAziende plugin;
    private final Azienda azienda;
    private final Player player;
    private final UUID dipendenteUUID;
    private final GestioneDipendenteGUI previousGUI;
    private final Inventory inventory;
    private boolean returnToPrevious = true;

    public IncarichiGUI(IthacaAziende plugin, Azienda azienda, Player player, UUID dipendenteUUID, GestioneDipendenteGUI previousGUI) {
        this.plugin = plugin;
        this.azienda = azienda;
        this.player = player;
        this.dipendenteUUID = dipendenteUUID;
        this.previousGUI = previousGUI;

        OfflinePlayer dipendente = Bukkit.getOfflinePlayer(dipendenteUUID);
        this.inventory = Bukkit.createInventory(null, 54, ChatColor.LIGHT_PURPLE + "Incarichi - " + dipendente.getName());

        setupGUI();
    }

    private void setupGUI() {
        DipendenteInfo info = azienda.getInfoDipendente(dipendenteUUID);
        List<DipendenteInfo.Incarico> incarichi = info.getIncarichi();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        if (incarichi.isEmpty()) {
            ItemStack noIncarichi = new ItemStack(Material.BARRIER);
            ItemMeta meta = noIncarichi.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "Nessun incarico");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Non ci sono incarichi",
                    ChatColor.GRAY + "per questo dipendente"
            ));
            noIncarichi.setItemMeta(meta);
            inventory.setItem(22, noIncarichi);
        } else {
            int slot = 0;
            for (int i = 0; i < Math.min(45, incarichi.size()); i++) {
                DipendenteInfo.Incarico incarico = incarichi.get(i);

                ItemStack book = new ItemStack(incarico.isCompletato() ? Material.WRITTEN_BOOK : Material.BOOK);
                ItemMeta meta = book.getItemMeta();
                meta.setDisplayName((incarico.isCompletato() ? ChatColor.GREEN : ChatColor.YELLOW) + "Incarico #" + (i + 1));

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Data: " + ChatColor.WHITE + incarico.getData().format(formatter));
                lore.add(ChatColor.GRAY + "Stato: " + (incarico.isCompletato() ? ChatColor.GREEN + "✓ Completato" : ChatColor.YELLOW + "✗ In corso"));
                lore.add("");

                String[] lines = incarico.getDescrizione().split("(?<=\\G.{30})");
                for (String line : lines) {
                    lore.add(ChatColor.WHITE + line);
                }

                lore.add("");
                if (!incarico.isCompletato()) {
                    lore.add(ChatColor.GREEN + "Click sinistro: Segna completato");
                } else {
                    lore.add(ChatColor.YELLOW + "Click sinistro: Segna non completato");
                }
                lore.add(ChatColor.RED + "Click destro: Elimina");

                meta.setLore(lore);
                book.setItemMeta(meta);

                inventory.setItem(slot++, book);
            }
        }

        ItemStack aggiungi = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta aggiungiMeta = aggiungi.getItemMeta();
        aggiungiMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Aggiungi Incarico");
        aggiungiMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Clicca per aggiungere",
                ChatColor.GRAY + "un nuovo incarico"
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

            startAggiungiIncarico();
        } else if ((e.getCurrentItem().getType() == Material.BOOK || e.getCurrentItem().getType() == Material.WRITTEN_BOOK)) {
            ItemMeta meta = e.getCurrentItem().getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String displayName = ChatColor.stripColor(meta.getDisplayName());
                if (displayName.startsWith("Incarico #")) {
                    try {
                        int index = Integer.parseInt(displayName.substring(10)) - 1;

                        if (e.isRightClick()) {
                            // Elimina
                            azienda.getInfoDipendente(dipendenteUUID).rimuoviIncarico(index);
                            plugin.getAziendaManager().saveAll();

                            clicker.sendMessage(ChatColor.GREEN + "Incarico eliminato!");
                        } else if (e.isLeftClick()) {
                            // Toggle completato
                            DipendenteInfo.Incarico incarico = azienda.getInfoDipendente(dipendenteUUID).getIncarichi().get(index);
                            boolean vecchioStato = incarico.isCompletato();
                            azienda.setIncaricoCompletato(dipendenteUUID, index, !vecchioStato);
                            plugin.getAziendaManager().saveAll();

                            Player target = Bukkit.getPlayer(dipendenteUUID);
                            if (target != null && target.isOnline()) {
                                if (!vecchioStato) {
                                    // Era non completato, ora è completato
                                    target.sendMessage(ChatColor.GREEN + "Il tuo incarico è stato segnato come completato!");
                                } else {
                                    // Era completato, ora è non completato
                                    target.sendMessage(ChatColor.YELLOW + "Il tuo incarico è stato segnato come non completato.");
                                }
                            }

                            clicker.sendMessage(ChatColor.GREEN + "Stato incarico aggiornato!");
                        }

                        returnToPrevious = false;
                        clicker.closeInventory();
                        HandlerList.unregisterAll(this);

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            IncarichiGUI nuovaGUI = new IncarichiGUI(plugin, azienda, clicker, dipendenteUUID, previousGUI);
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

    private void startAggiungiIncarico() {
        ConversationFactory factory = new ConversationFactory(plugin)
                .withFirstPrompt(new IncaricoPrompt())
                .withLocalEcho(false)
                .withTimeout(120)
                .withPrefix(context -> ChatColor.LIGHT_PURPLE + "[Incarichi] " + ChatColor.RESET)
                .addConversationAbandonedListener(event -> {
                    if (!event.gracefulExit()) {
                        player.sendMessage(ChatColor.LIGHT_PURPLE + "[Incarichi] " + ChatColor.RED + "Operazione annullata.");
                        Bukkit.getScheduler().runTaskLater(plugin, this::open, 1L);
                    }
                });

        factory.buildConversation(player).begin();
    }

    private class IncaricoPrompt extends StringPrompt {
        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.YELLOW + "Inserisci la descrizione dell'incarico (o scrivi 'annulla'):";
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            if (input.equalsIgnoreCase("annulla")) {
                player.sendMessage(ChatColor.LIGHT_PURPLE + "[Incarichi] " + ChatColor.YELLOW + "Operazione annullata.");
                Bukkit.getScheduler().runTaskLater(plugin, () -> open(), 1L);
                return Prompt.END_OF_CONVERSATION;
            }

            if (input == null || input.trim().isEmpty()) {
                player.sendMessage(ChatColor.LIGHT_PURPLE + "[Incarichi] " + ChatColor.RED + "Devi inserire una descrizione!");
                return this;
            }

            azienda.aggiungiIncaricoDipendente(dipendenteUUID, input.trim(), LocalDate.now());
            plugin.getAziendaManager().saveAll();

            Player target = Bukkit.getPlayer(dipendenteUUID);
            if (target != null && target.isOnline()) {
                target.sendMessage("");
                target.sendMessage(ChatColor.LIGHT_PURPLE + "═════════════════════════════");
                target.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "NUOVO INCARICO");
                target.sendMessage(ChatColor.GRAY + "Azienda: " + ChatColor.WHITE + azienda.getNome());
                target.sendMessage("");
                target.sendMessage(ChatColor.YELLOW + input.trim());
                target.sendMessage(ChatColor.LIGHT_PURPLE + "═════════════════════════════");
                target.sendMessage("");
            }

            player.sendMessage(ChatColor.LIGHT_PURPLE + "[Incarichi] " + ChatColor.GREEN + "Incarico assegnato con successo!");

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                IncarichiGUI nuovaGUI = new IncarichiGUI(plugin, azienda, player, dipendenteUUID, previousGUI);
                nuovaGUI.open();
            }, 1L);

            return Prompt.END_OF_CONVERSATION;
        }
    }
}