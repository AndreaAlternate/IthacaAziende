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

public class RichiamiGUI implements Listener {

    private final IthacaAziende plugin;
    private final Azienda azienda;
    private final Player player;
    private final UUID dipendenteUUID;
    private final GestioneDipendenteGUI previousGUI;
    private final Inventory inventory;
    private boolean returnToPrevious = true;

    public RichiamiGUI(IthacaAziende plugin, Azienda azienda, Player player, UUID dipendenteUUID, GestioneDipendenteGUI previousGUI) {
        this.plugin = plugin;
        this.azienda = azienda;
        this.player = player;
        this.dipendenteUUID = dipendenteUUID;
        this.previousGUI = previousGUI;

        OfflinePlayer dipendente = Bukkit.getOfflinePlayer(dipendenteUUID);
        this.inventory = Bukkit.createInventory(null, 54, ChatColor.GOLD + "Richiami - " + dipendente.getName());

        setupGUI();
    }

    private void setupGUI() {
        DipendenteInfo info = azienda.getInfoDipendente(dipendenteUUID);
        List<DipendenteInfo.Richiamo> richiami = info.getRichiami();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        if (richiami.isEmpty()) {
            ItemStack noRichiami = new ItemStack(Material.BARRIER);
            ItemMeta meta = noRichiami.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "Nessun richiamo");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Non ci sono richiami",
                    ChatColor.GRAY + "per questo dipendente"
            ));
            noRichiami.setItemMeta(meta);
            inventory.setItem(22, noRichiami);
        } else {
            int slot = 0;
            for (int i = 0; i < Math.min(45, richiami.size()); i++) {
                DipendenteInfo.Richiamo richiamo = richiami.get(i);

                ItemStack bell = new ItemStack(Material.BELL);
                ItemMeta meta = bell.getItemMeta();
                meta.setDisplayName(ChatColor.GOLD + "Richiamo #" + (i + 1));

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Data: " + ChatColor.WHITE + richiamo.getData().format(formatter));
                lore.add("");
                lore.add(ChatColor.GRAY + "Motivo:");

                String[] lines = richiamo.getMotivo().split("(?<=\\G.{30})");
                for (String line : lines) {
                    lore.add(ChatColor.WHITE + line);
                }

                lore.add("");
                lore.add(ChatColor.RED + "Click destro per eliminare");

                meta.setLore(lore);
                bell.setItemMeta(meta);

                inventory.setItem(slot++, bell);
            }
        }

        ItemStack aggiungi = new ItemStack(Material.OMINOUS_BOTTLE);
        ItemMeta aggiungiMeta = aggiungi.getItemMeta();
        aggiungiMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Aggiungi Richiamo");
        aggiungiMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Clicca per aggiungere",
                ChatColor.GRAY + "un nuovo richiamo"
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

        if (e.getSlot() == 53 && e.getCurrentItem().getType() == Material.OMINOUS_BOTTLE) {
            returnToPrevious = false;
            clicker.closeInventory();
            HandlerList.unregisterAll(this);

            startAggiungiRichiamo();
        } else if (e.getCurrentItem().getType() == Material.BELL && e.isRightClick()) {
            ItemMeta meta = e.getCurrentItem().getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String displayName = ChatColor.stripColor(meta.getDisplayName());
                if (displayName.startsWith("Richiamo #")) {
                    try {
                        int index = Integer.parseInt(displayName.substring(10)) - 1;
                        azienda.getInfoDipendente(dipendenteUUID).rimuoviRichiamo(index);
                        plugin.getAziendaManager().saveAll();

                        clicker.sendMessage(ChatColor.GREEN + "Richiamo eliminato!");

                        returnToPrevious = false;
                        clicker.closeInventory();
                        HandlerList.unregisterAll(this);

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            RichiamiGUI nuovaGUI = new RichiamiGUI(plugin, azienda, clicker, dipendenteUUID, previousGUI);
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

    private void startAggiungiRichiamo() {
        ConversationFactory factory = new ConversationFactory(plugin)
                .withFirstPrompt(new RichiamoPrompt())
                .withLocalEcho(false)
                .withTimeout(120)
                .withPrefix(context -> ChatColor.GOLD + "[Richiami] " + ChatColor.RESET)
                .addConversationAbandonedListener(event -> {
                    if (!event.gracefulExit()) {
                        player.sendMessage(ChatColor.GOLD + "[Richiami] " + ChatColor.RED + "Operazione annullata.");
                        Bukkit.getScheduler().runTaskLater(plugin, this::open, 1L);
                    }
                });

        factory.buildConversation(player).begin();
    }

    private class RichiamoPrompt extends StringPrompt {
        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.YELLOW + "Inserisci il motivo del richiamo (o scrivi 'annulla'):";
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            if (input.equalsIgnoreCase("annulla")) {
                player.sendMessage(ChatColor.GOLD + "[Richiami] " + ChatColor.YELLOW + "Operazione annullata.");
                Bukkit.getScheduler().runTaskLater(plugin, () -> open(), 1L);
                return Prompt.END_OF_CONVERSATION;
            }

            if (input == null || input.trim().isEmpty()) {
                player.sendMessage(ChatColor.GOLD + "[Richiami] " + ChatColor.RED + "Devi inserire un motivo!");
                return this;
            }

            azienda.aggiungiRichiamoDipendente(dipendenteUUID, input.trim(), LocalDate.now());
            plugin.getAziendaManager().saveAll();

            Player target = Bukkit.getPlayer(dipendenteUUID);
            if (target != null && target.isOnline()) {
                target.sendMessage("");
                target.sendMessage(ChatColor.RED + "═════════════════════════════");
                target.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "HAI RICEVUTO UN RICHIAMO");
                target.sendMessage(ChatColor.GRAY + "Azienda: " + ChatColor.WHITE + azienda.getNome());
                target.sendMessage("");
                target.sendMessage(ChatColor.YELLOW + "Motivo: " + ChatColor.WHITE + input.trim());
                target.sendMessage(ChatColor.RED + "═════════════════════════════");
                target.sendMessage("");
            }

            player.sendMessage(ChatColor.GOLD + "[Richiami] " + ChatColor.GREEN + "Richiamo aggiunto con successo!");

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                RichiamiGUI nuovaGUI = new RichiamiGUI(plugin, azienda, player, dipendenteUUID, previousGUI);
                nuovaGUI.open();
            }, 1L);

            return Prompt.END_OF_CONVERSATION;
        }
    }
}