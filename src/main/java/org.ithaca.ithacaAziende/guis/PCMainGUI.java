package org.ithaca.ithacaAziende.guis;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.ithaca.ithacaAziende.IthacaAziende;
import org.ithaca.ithacaAziende.guis.ConcorsiGUI;
import org.ithaca.ithacaAziende.models.Azienda;

import java.util.Arrays;

public class PCMainGUI implements Listener {

    private final IthacaAziende plugin;
    private final Azienda azienda;
    private final Player player;
    private final Inventory inventory;

    public PCMainGUI(IthacaAziende plugin, Azienda azienda, Player player) {
        this.plugin = plugin;
        this.azienda = azienda;
        this.player = player;
        this.inventory = Bukkit.createInventory(null, 36, ChatColor.DARK_AQUA + "" + ChatColor.BOLD + azienda.getNome() + " - PC");

        setupGUI();
    }

    private void setupGUI() {
        // Riempimento vetri grigi
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, filler);
        }

        // Riga 2
        // Assumi
        ItemStack assumi = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta assumiMeta = assumi.getItemMeta();
        assumiMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Assumi");
        assumiMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Clicca per assumere",
                ChatColor.GRAY + "nuovi dipendenti"
        ));
        assumi.setItemMeta(assumiMeta);
        inventory.setItem(10, assumi);

        // Dipendenti
        ItemStack dipendenti = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta dipendentiMeta = dipendenti.getItemMeta();
        dipendentiMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Dipendenti");
        dipendentiMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Clicca per gestire",
                ChatColor.GRAY + "i dipendenti"
        ));
        dipendenti.setItemMeta(dipendentiMeta);
        inventory.setItem(11, dipendenti);

        // Transazioni
        ItemStack transazioni = new ItemStack(Material.PAPER);
        ItemMeta transazioniMeta = transazioni.getItemMeta();
        transazioniMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Transazioni");
        transazioniMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Clicca per visualizzare",
                ChatColor.GRAY + "le transazioni"
        ));
        transazioni.setItemMeta(transazioniMeta);
        inventory.setItem(13, transazioni);

        // Ruoli
        ItemStack ruoli = new ItemStack(Material.ANVIL);
        ItemMeta ruoliMeta = ruoli.getItemMeta();
        ruoliMeta.setDisplayName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Ruoli");
        ruoliMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Clicca per gestire",
                ChatColor.GRAY + "i ruoli aziendali"
        ));
        ruoli.setItemMeta(ruoliMeta);
        inventory.setItem(15, ruoli);

        // Concorsi
        ItemStack concorsi = new ItemStack(Material.SPRUCE_HANGING_SIGN);
        ItemMeta concorsiMeta = concorsi.getItemMeta();
        concorsiMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Concorsi");
        concorsiMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Clicca per gestire",
                ChatColor.GRAY + "i concorsi"
        ));
        concorsi.setItemMeta(concorsiMeta);
        inventory.setItem(16, concorsi);

        // Riga 3
        // Targhetta
        ItemStack targhetta = new ItemStack(Material.NAME_TAG);
        ItemMeta targhettaMeta = targhetta.getItemMeta();
        targhettaMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Targhetta");
        targhettaMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Gestisci prefisso azienda",
                ChatColor.GRAY + "e suffissi ruoli"
        ));
        targhetta.setItemMeta(targhettaMeta);
        inventory.setItem(19, targhetta);
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
            case 10: // Assumi
                if (!azienda.getProprietario().equals(clicker.getUniqueId())) {
                    clicker.sendMessage(ChatColor.RED + "Solo il proprietario può assumere dipendenti!");
                    return;
                }
                clicker.closeInventory();
                AssumiGUI assumiGUI = new AssumiGUI(plugin, azienda, clicker, this);
                Bukkit.getScheduler().runTaskLater(plugin, assumiGUI::open, 1L);
                break;

            case 11: // Dipendenti
                if (!azienda.getProprietario().equals(clicker.getUniqueId())) {
                    clicker.sendMessage(ChatColor.RED + "Solo il proprietario può gestire i dipendenti!");
                    return;
                }
                clicker.closeInventory();
                DipendentiGUI dipendentiGUI = new DipendentiGUI(plugin, azienda, clicker, this);
                Bukkit.getScheduler().runTaskLater(plugin, dipendentiGUI::open, 1L);
                break;

            case 13: // Transazioni
                clicker.closeInventory();
                TransazioniGUI transazioniGUI = new TransazioniGUI(plugin, azienda, clicker, this);
                Bukkit.getScheduler().runTaskLater(plugin, transazioniGUI::open, 1L);
                break;

            case 15: // Ruoli
                if (!azienda.getProprietario().equals(clicker.getUniqueId())) {
                    clicker.sendMessage(ChatColor.RED + "Solo il proprietario può gestire i ruoli!");
                    return;
                }
                clicker.closeInventory();
                RuoliGUI ruoliGUI = new RuoliGUI(plugin, azienda, clicker, this);
                Bukkit.getScheduler().runTaskLater(plugin, ruoliGUI::open, 1L);
                break;

            case 16: // Concorsi
                clicker.closeInventory();
                ConcorsiGUI concorsiGUI = new ConcorsiGUI(plugin, azienda, clicker, this);
                Bukkit.getScheduler().runTaskLater(plugin, concorsiGUI::open, 1L);
                break;

            case 19: // Targhetta
                if (!azienda.getProprietario().equals(clicker.getUniqueId())) {
                    clicker.sendMessage(ChatColor.RED + "Solo il proprietario può gestire la targhetta!");
                    return;
                }
                clicker.closeInventory();
                TarghettaGUI targhettaGUI = new TarghettaGUI(plugin, azienda, clicker, this);
                Bukkit.getScheduler().runTaskLater(plugin, targhettaGUI::open, 1L);
                break;
        }

        HandlerList.unregisterAll(this);
    }
}