package org.ithaca.ithacaAziende.guis;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
import org.ithaca.ithacaAziende.models.Concorso;

import java.util.Arrays;
import java.util.List;

public class ConcorsiGUI implements Listener {

    private final IthacaAziende plugin;
    private final Azienda azienda;
    private final Player player;
    public final PCMainGUI previousGUI;
    private final Inventory inventory;
    private boolean returnToPrevious = true;

    public ConcorsiGUI(IthacaAziende plugin, Azienda azienda, Player player, PCMainGUI previousGUI) {
        this.plugin = plugin;
        this.azienda = azienda;
        this.player = player;
        this.previousGUI = previousGUI;
        this.inventory = Bukkit.createInventory(null, 27, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Concorsi");

        setupGUI();
    }

    private void setupGUI() {
        ItemStack separator = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta separatorMeta = separator.getItemMeta();
        separatorMeta.setDisplayName(" ");
        separator.setItemMeta(separatorMeta);

        for (int i = 9; i < 18; i++) {
            inventory.setItem(i, separator);
        }

        // Mostra solo i concorsi della propria azienda
        List<Concorso> concorsi = plugin.getConcorsoManager().getConcorsiByAzienda(azienda.getNome());

        for (int i = 0; i < Math.min(9, concorsi.size()); i++) {
            Concorso concorso = concorsi.get(i);

            ItemStack paper = new ItemStack(Material.PAPER);
            ItemMeta meta = paper.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + concorso.getTipoConcorso());
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Data: " + ChatColor.WHITE + concorso.getDataFormatted(),
                    ChatColor.GRAY + "Ora: " + ChatColor.WHITE + concorso.getOraFormatted(),
                    ChatColor.GRAY + "Luogo: " + ChatColor.WHITE + concorso.getLuogo(),
                    ChatColor.GRAY + "Posti: " + ChatColor.WHITE + concorso.getPostiDisponibili()
            ));
            paper.setItemMeta(meta);

            inventory.setItem(i, paper);
        }

        ItemStack create = new ItemStack(Material.END_CRYSTAL);
        ItemMeta createMeta = create.getItemMeta();
        createMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Crea Concorso");

        if (!plugin.getConcorsoManager().puoCreareConcorso(azienda.getNome())) {
            createMeta.setLore(Arrays.asList(
                    ChatColor.RED + "Hai già un concorso attivo!",
                    ChatColor.GRAY + "Puoi avere massimo 1 concorso"
            ));
        } else {
            createMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Clicca per creare",
                    ChatColor.GRAY + "un nuovo concorso",
                    "",
                    ChatColor.GREEN + "Concorsi attivi: " + concorsi.size() + "/1"
            ));
        }

        create.setItemMeta(createMeta);
        inventory.setItem(22, create);
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

        if (slot == 22 && e.getCurrentItem().getType() == Material.END_CRYSTAL) {
            if (!azienda.getProprietario().equals(clicker.getUniqueId())) {
                clicker.sendMessage(ChatColor.RED + "Solo il proprietario può creare concorsi!");
                return;
            }

            if (!plugin.getConcorsoManager().puoCreareConcorso(azienda.getNome())) {
                clicker.sendMessage(ChatColor.RED + "Hai già un concorso attivo! Massimo 1 concorso per azienda.");
                return;
            }

            returnToPrevious = false;
            clicker.closeInventory();
            HandlerList.unregisterAll(this);

            ScegliTipoGUI scegliTipo = new ScegliTipoGUI(plugin, azienda, clicker, this);
            Bukkit.getScheduler().runTaskLater(plugin, scegliTipo::open, 1L);
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