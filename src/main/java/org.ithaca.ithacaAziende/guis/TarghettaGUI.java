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

public class TarghettaGUI implements Listener {

    private final IthacaAziende plugin;
    private final Azienda azienda;
    private final Player player;
    private final PCMainGUI previousGUI;
    private final Inventory inventory;
    private boolean returnToPrevious = true;

    public TarghettaGUI(IthacaAziende plugin, Azienda azienda, Player player, PCMainGUI previousGUI) {
        this.plugin = plugin;
        this.azienda = azienda;
        this.player = player;
        this.previousGUI = previousGUI;
        this.inventory = Bukkit.createInventory(null, 27, ChatColor.AQUA + "" + ChatColor.BOLD + "Targhetta Aziendale");

        setupGUI();
    }

    private void setupGUI() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        // Prefisso
        ItemStack prefisso = new ItemStack(Material.NAME_TAG);
        ItemMeta prefissoMeta = prefisso.getItemMeta();
        prefissoMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Colore Prefisso");
        prefissoMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Attuale: " + ChatColor.WHITE + azienda.getColorePrefisso(),
                ChatColor.GRAY + "Anteprima: " + ChatColor.WHITE + org.bukkit.ChatColor.translateAlternateColorCodes('&', azienda.getPrefissoFormattato()),
                "",
                ChatColor.GRAY + "Il prefisso usa il nome",
                ChatColor.GRAY + "dell'azienda: " + ChatColor.WHITE + azienda.getNome(),
                "",
                ChatColor.YELLOW + "Clicca per cambiare colore"
        ));
        prefisso.setItemMeta(prefissoMeta);
        inventory.setItem(11, prefisso);

        // Info suffissi
        ItemStack suffissi = new ItemStack(Material.PAPER);
        ItemMeta suffissiMeta = suffissi.getItemMeta();
        suffissiMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Suffissi Ruoli");
        suffissiMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "I suffissi sono automatici",
                ChatColor.GRAY + "basati sui ruoli:",
                "",
                ChatColor.WHITE + "Capo " + ChatColor.DARK_RED + "→ " + ChatColor.GRAY + "[Capo]",
                ChatColor.WHITE + "Dipendente " + ChatColor.DARK_RED + "→ " + ChatColor.GRAY + "[Dipendente]",
                ChatColor.WHITE + "Altri ruoli " + ChatColor.DARK_RED + "→ " + ChatColor.GRAY + "[Nome Ruolo]",
                "",
                ChatColor.YELLOW + "Gestisci i ruoli nella sezione Ruoli"
        ));
        suffissi.setItemMeta(suffissiMeta);
        inventory.setItem(13, suffissi);

        // Applica
        ItemStack applica = new ItemStack(Material.EMERALD);
        ItemMeta applicaMeta = applica.getItemMeta();
        applicaMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Applica Modifiche");
        applicaMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Applica le modifiche",
                ChatColor.GRAY + "a tutti i membri"
        ));
        applica.setItemMeta(applicaMeta);
        inventory.setItem(15, applica);
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

        if (slot == 11 && e.getCurrentItem().getType() == Material.NAME_TAG) {
            // Cambia colore prefisso
            returnToPrevious = false;
            clicker.closeInventory();
            HandlerList.unregisterAll(this);

            startCambiaColore();
        } else if (slot == 15 && e.getCurrentItem().getType() == Material.EMERALD) {
            // Applica modifiche
            plugin.getAziendaManager().applicaTarghettaTutti(azienda);
            plugin.getAziendaManager().saveAll();

            clicker.sendMessage(ChatColor.GREEN + "Targhetta applicata a tutti i membri!");

            returnToPrevious = false;
            clicker.closeInventory();
            HandlerList.unregisterAll(this);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                TarghettaGUI nuovaGUI = new TarghettaGUI(plugin, azienda, clicker, previousGUI);
                nuovaGUI.open();
            }, 1L);
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

    private void startCambiaColore() {
        ConversationFactory factory = new ConversationFactory(plugin)
                .withFirstPrompt(new ColorePrompt())
                .withLocalEcho(false)
                .withTimeout(120)
                .withPrefix(context -> ChatColor.AQUA + "[Targhetta] " + ChatColor.RESET)
                .addConversationAbandonedListener(event -> {
                    if (!event.gracefulExit()) {
                        player.sendMessage(ChatColor.AQUA + "[Targhetta] " + ChatColor.RED + "Operazione annullata.");
                        Bukkit.getScheduler().runTaskLater(plugin, this::open, 1L);
                    }
                });

        factory.buildConversation(player).begin();
    }

    private class ColorePrompt extends StringPrompt {
        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.YELLOW + "Inserisci il colore del prefisso (es: &6, &c, #FF5733) o 'annulla':\n" +
                    ChatColor.GRAY + "Colori disponibili: &0-9, &a-f o HEX (#RRGGBB)";
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            if (input.equalsIgnoreCase("annulla")) {
                player.sendMessage(ChatColor.AQUA + "[Targhetta] " + ChatColor.YELLOW + "Operazione annullata.");
                Bukkit.getScheduler().runTaskLater(plugin, () -> open(), 1L);
                return Prompt.END_OF_CONVERSATION;
            }

            if (input == null || input.trim().isEmpty()) {
                player.sendMessage(ChatColor.AQUA + "[Targhetta] " + ChatColor.RED + "Devi inserire un colore!");
                return this;
            }

            String colore = input.trim();

            // Valida il formato
            if (!colore.matches("&[0-9a-fA-Fk-oK-OrR]") && !colore.matches("#[0-9A-Fa-f]{6}")) {
                player.sendMessage(ChatColor.AQUA + "[Targhetta] " + ChatColor.RED + "Formato colore non valido!");
                player.sendMessage(ChatColor.YELLOW + "Usa: &6, &c, oppure #FF5733");
                return this;
            }

            azienda.setColorePrefisso(colore);
            plugin.getAziendaManager().saveAll();

            player.sendMessage(ChatColor.AQUA + "[Targhetta] " + ChatColor.GREEN + "Colore prefisso impostato!");
            player.sendMessage(ChatColor.GRAY + "Anteprima: " + org.bukkit.ChatColor.translateAlternateColorCodes('&', azienda.getPrefissoFormattato()));
            player.sendMessage(ChatColor.YELLOW + "Ricorda di cliccare 'Applica Modifiche' per aggiornare tutti!");

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                TarghettaGUI nuovaGUI = new TarghettaGUI(plugin, azienda, player, previousGUI);
                nuovaGUI.open();
            }, 1L);

            return Prompt.END_OF_CONVERSATION;
        }
    }
}