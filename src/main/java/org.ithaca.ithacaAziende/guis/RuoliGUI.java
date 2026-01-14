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

public class RuoliGUI implements Listener {

    private final IthacaAziende plugin;
    private final Azienda azienda;
    private final Player player;
    private final PCMainGUI previousGUI;
    private final Inventory inventory;
    private boolean returnToPrevious = true;

    public RuoliGUI(IthacaAziende plugin, Azienda azienda, Player player, PCMainGUI previousGUI) {
        this.plugin = plugin;
        this.azienda = azienda;
        this.player = player;
        this.previousGUI = previousGUI;
        this.inventory = Bukkit.createInventory(null, 54, ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Gestione Ruoli");

        setupGUI();
    }

    private void setupGUI() {
        if (azienda.getRuoli().isEmpty()) {
            ItemStack noRoles = new ItemStack(Material.BARRIER);
            ItemMeta meta = noRoles.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "Nessun ruolo");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Non ci sono ruoli",
                    ChatColor.GRAY + "creati nell'azienda"
            ));
            noRoles.setItemMeta(meta);
            inventory.setItem(22, noRoles);
        } else {
            int slot = 0;
            for (String ruolo : azienda.getRuoli()) {
                if (slot >= 45) break;

                ItemStack paper = new ItemStack(Material.PAPER);
                ItemMeta meta = paper.getItemMeta();
                meta.setDisplayName(ChatColor.AQUA + ruolo);
                meta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Ruolo aziendale"
                ));
                paper.setItemMeta(meta);

                inventory.setItem(slot++, paper);
            }
        }

        // Pulsante per creare nuovo ruolo
        ItemStack create = new ItemStack(Material.ANVIL);
        ItemMeta meta = create.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Crea Nuovo Ruolo");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Clicca per creare",
                ChatColor.GRAY + "un nuovo ruolo aziendale"
        ));
        create.setItemMeta(meta);
        inventory.setItem(53, create);
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

        if (e.getSlot() == 53 && e.getCurrentItem().getType() == Material.ANVIL) {
            // Crea nuovo ruolo
            returnToPrevious = false;
            clicker.closeInventory();
            HandlerList.unregisterAll(this);

            ConversationFactory factory = new ConversationFactory(plugin)
                    .withFirstPrompt(new NomeRuoloPrompt())
                    .withLocalEcho(false)
                    .withTimeout(60)
                    .withPrefix(context -> ChatColor.DARK_PURPLE + "[Ruoli] " + ChatColor.RESET)
                    .addConversationAbandonedListener(event -> {
                        if (!event.gracefulExit()) {
                            clicker.sendMessage(ChatColor.DARK_PURPLE + "[Ruoli] " + ChatColor.RED + "Creazione ruolo annullata.");
                            Bukkit.getScheduler().runTaskLater(plugin, this::open, 1L);
                        }
                    });

            factory.buildConversation(clicker).begin();
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

    private class NomeRuoloPrompt extends StringPrompt {
        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.YELLOW + "Inserisci il nome del nuovo ruolo (o scrivi 'annulla' per annullare):";
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            if (input == null || input.trim().isEmpty()) {
                player.sendMessage(ChatColor.DARK_PURPLE + "[Ruoli] " + ChatColor.RED + "Devi inserire un nome!");
                Bukkit.getScheduler().runTaskLater(plugin, () -> open(), 1L);
                return Prompt.END_OF_CONVERSATION;
            }

            if (input.equalsIgnoreCase("annulla")) {
                player.sendMessage(ChatColor.DARK_PURPLE + "[Ruoli] " + ChatColor.YELLOW + "Creazione ruolo annullata.");
                Bukkit.getScheduler().runTaskLater(plugin, () -> open(), 1L);
                return Prompt.END_OF_CONVERSATION;
            }

            String nomeRuolo = input.trim();
            azienda.aggiungiRuolo(nomeRuolo);
            plugin.getAziendaManager().saveAll();

            player.sendMessage(ChatColor.DARK_PURPLE + "[Ruoli] " + ChatColor.GREEN + "Ruolo '" + nomeRuolo + "' creato con successo!");

            // Riapri la GUI aggiornata
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                returnToPrevious = false;
                RuoliGUI nuovaGUI = new RuoliGUI(plugin, azienda, player, previousGUI);
                nuovaGUI.open();
            }, 1L);

            return Prompt.END_OF_CONVERSATION;
        }
    }
}