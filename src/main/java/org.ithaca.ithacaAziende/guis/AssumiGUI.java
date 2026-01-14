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
import org.bukkit.inventory.meta.SkullMeta;
import org.ithaca.ithacaAziende.IthacaAziende;
import org.ithaca.ithacaAziende.models.Azienda;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AssumiGUI implements Listener {

    private final IthacaAziende plugin;
    private final Azienda azienda;
    private final Player player;
    private final PCMainGUI previousGUI;
    private final Inventory inventory;
    private boolean returnToPrevious = true;

    public AssumiGUI(IthacaAziende plugin, Azienda azienda, Player player, PCMainGUI previousGUI) {
        this.plugin = plugin;
        this.azienda = azienda;
        this.player = player;
        this.previousGUI = previousGUI;
        this.inventory = Bukkit.createInventory(null, 54, ChatColor.GREEN + "" + ChatColor.BOLD + "Assumi Dipendente");

        setupGUI();
    }

    private void setupGUI() {
        List<Player> nearbyPlayers = new ArrayList<>();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(player)) continue;
            if (p.getWorld().equals(player.getWorld()) &&
                    p.getLocation().distance(player.getLocation()) <= 50) {
                if (!azienda.isDipendente(p.getUniqueId())) {
                    nearbyPlayers.add(p);
                }
            }
        }

        if (nearbyPlayers.isEmpty()) {
            ItemStack noPlayers = new ItemStack(Material.BARRIER);
            ItemMeta meta = noPlayers.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "Nessun giocatore vicino");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Non ci sono giocatori",
                    ChatColor.GRAY + "nelle vicinanze da assumere"
            ));
            noPlayers.setItemMeta(meta);
            inventory.setItem(22, noPlayers);
            return;
        }

        int slot = 0;
        for (Player p : nearbyPlayers) {
            if (slot >= 54) break;

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.setOwningPlayer(p);
            meta.setDisplayName(ChatColor.GREEN + p.getName());
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Clicca per assumere",
                    ChatColor.GRAY + "questo giocatore"
            ));
            skull.setItemMeta(meta);

            inventory.setItem(slot++, skull);
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
        if (e.getCurrentItem().getType() != Material.PLAYER_HEAD) return;

        SkullMeta meta = (SkullMeta) e.getCurrentItem().getItemMeta();
        if (meta.getOwningPlayer() == null) return;

        Player target = meta.getOwningPlayer().getPlayer();
        if (target == null || !target.isOnline()) {
            clicker.sendMessage(ChatColor.RED + "Giocatore non più online!");
            clicker.closeInventory();
            HandlerList.unregisterAll(this);
            return;
        }

        plugin.getAziendaManager().assumiDipendente(azienda.getNome(), target.getUniqueId());
        clicker.sendMessage(ChatColor.GREEN + target.getName() + " è stato assunto!");

        if (target.isOnline()) {
            target.sendMessage("");
            target.sendMessage(ChatColor.GREEN + "═════════════════════════════");
            target.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "SEI STATO ASSUNTO!");
            target.sendMessage(ChatColor.GRAY + "Azienda: " + ChatColor.WHITE + azienda.getNome());
            target.sendMessage(ChatColor.GREEN + "═════════════════════════════");
            target.sendMessage("");
        }

        returnToPrevious = false;
        clicker.closeInventory();
        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().runTaskLater(plugin, () -> previousGUI.open(), 1L);
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