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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ImpostaStipendioGUI implements Listener {

    private final IthacaAziende plugin;
    private final Azienda azienda;
    private final Player player;
    private final UUID targetUUID;
    private final GestioneDipendenteGUI previousGUI;
    private final Inventory inventory;
    private boolean returnToPrevious = true;

    public ImpostaStipendioGUI(IthacaAziende plugin, Azienda azienda, Player player, UUID targetUUID, GestioneDipendenteGUI previousGUI) {
        this.plugin = plugin;
        this.azienda = azienda;
        this.player = player;
        this.targetUUID = targetUUID;
        this.previousGUI = previousGUI;
        this.inventory = Bukkit.createInventory(null, 27, ChatColor.GOLD + "" + ChatColor.BOLD + "Imposta Stipendio");

        setupGUI();
    }

    private void setupGUI() {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        // Info dipendente con stipendio attuale
        ItemStack info = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(ChatColor.AQUA + target.getName());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Imposta lo stipendio");
        lore.add(ChatColor.GRAY + "per questo dipendente");
        lore.add("");

        Azienda.StipendioInfo stipendioAttuale = azienda.getStipendio(targetUUID);
        if (stipendioAttuale != null) {
            lore.add(ChatColor.YELLOW + "Stipendio attuale:");
            lore.add(ChatColor.GREEN + "€" + stipendioAttuale.getImporto());
        } else {
            lore.add(ChatColor.RED + "Nessuno stipendio impostato");
        }

        infoMeta.setLore(lore);
        info.setItemMeta(infoMeta);
        inventory.setItem(4, info);

        // Bottone imposta stipendio
        ItemStack imposta = new ItemStack(Material.GOLD_INGOT);
        ItemMeta impostaMeta = imposta.getItemMeta();
        impostaMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Imposta Stipendio");
        impostaMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Clicca per impostare",
                ChatColor.GRAY + "l'importo tramite chat"
        ));
        imposta.setItemMeta(impostaMeta);
        inventory.setItem(13, imposta);
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

        if (e.getSlot() == 13 && e.getCurrentItem().getType() == Material.GOLD_INGOT) {
            returnToPrevious = false;
            clicker.closeInventory();
            HandlerList.unregisterAll(this);

            startStipendioInput();
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

    private void startStipendioInput() {
        ConversationFactory factory = new ConversationFactory(plugin)
                .withFirstPrompt(new ImportoPrompt())
                .withLocalEcho(false)
                .withTimeout(120)
                .withPrefix(context -> ChatColor.GOLD + "[Stipendio] " + ChatColor.RESET)
                .addConversationAbandonedListener(event -> {
                    if (!event.gracefulExit()) {
                        player.sendMessage(ChatColor.GOLD + "[Stipendio] " + ChatColor.RED + "Operazione annullata.");
                        if (previousGUI != null) {
                            Bukkit.getScheduler().runTaskLater(plugin, () -> previousGUI.open(), 1L);
                        }
                    }
                });

        factory.buildConversation(player).begin();
    }

    private class ImportoPrompt extends NumericPrompt {
        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.YELLOW + "Inserisci l'importo dello stipendio (es: 1000 o 1000.50) o scrivi 'annulla':";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, Number input) {
            double importo = input.doubleValue();

            // Imposta lo stipendio con data null (nessuna scadenza)
            azienda.setStipendio(targetUUID, importo, null);
            plugin.getAziendaManager().saveAll();

            OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);

            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "✔ Stipendio Impostato!");
            player.sendMessage("");
            player.sendMessage(ChatColor.GRAY + "Dipendente: " + ChatColor.WHITE + target.getName());
            player.sendMessage(ChatColor.GRAY + "Importo: " + ChatColor.GREEN + "€" + importo);
            player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("");

            if (previousGUI != null) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> previousGUI.open(), 1L);
            }

            return Prompt.END_OF_CONVERSATION;
        }

        @Override
        protected boolean isNumberValid(ConversationContext context, Number input) {
            return input.doubleValue() > 0;
        }

        @Override
        protected String getInputNotNumericText(ConversationContext context, String invalidInput) {
            if (invalidInput.equalsIgnoreCase("annulla")) {
                player.sendMessage(ChatColor.GOLD + "[Stipendio] " + ChatColor.YELLOW + "Operazione annullata.");
                if (previousGUI != null) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> previousGUI.open(), 1L);
                }
                return null;
            }
            return ChatColor.GOLD + "[Stipendio] " + ChatColor.RED + "Devi inserire un numero valido!";
        }

        @Override
        protected String getFailedValidationText(ConversationContext context, Number invalidInput) {
            return ChatColor.GOLD + "[Stipendio] " + ChatColor.RED + "L'importo deve essere maggiore di 0!";
        }
    }
}