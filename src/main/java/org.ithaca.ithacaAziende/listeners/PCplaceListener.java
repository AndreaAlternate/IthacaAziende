package org.ithaca.ithacaAziende.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.ithaca.ithacaAziende.IthacaAziende;
import org.ithaca.ithacaAziende.guis.PCMainGUI;
import org.ithaca.ithacaAziende.models.Azienda;

public class PCPlaceListener implements Listener {

    private final IthacaAziende plugin;

    public PCPlaceListener(IthacaAziende plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        ItemStack item = e.getItemInHand();

        if (item.getType() != Material.PURPUR_BLOCK) return;
        if (!item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) return;
        if (!meta.hasCustomModelData()) return;

        String displayName = meta.getDisplayName();
        String stripped = ChatColor.stripColor(displayName);

        if (!stripped.endsWith(" PC")) return;

        String nomeAzienda = stripped.substring(0, stripped.length() - 3).trim();
        Azienda azienda = plugin.getAziendaManager().getAzienda(nomeAzienda);

        if (azienda == null) {
            player.sendMessage(ChatColor.RED + "Azienda non trovata!");
            e.setCancelled(true);
            return;
        }

        plugin.getAziendaManager().registraPC(e.getBlock().getLocation(), nomeAzienda);
        player.sendMessage(ChatColor.GREEN + "PC aziendale piazzato con successo!");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Block block = e.getBlock();

        if (block.getType() != Material.PURPUR_BLOCK) return;

        String nomeAzienda = plugin.getAziendaManager().getAziendaByPC(block.getLocation());
        if (nomeAzienda == null) return;

        Player player = e.getPlayer();
        Azienda azienda = plugin.getAziendaManager().getAzienda(nomeAzienda);

        if (azienda == null) return;

        if (!player.hasPermission("ithacaaziende.give") &&
                !azienda.getProprietario().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Non puoi rompere questo PC!");
            e.setCancelled(true);
            return;
        }

        plugin.getAziendaManager().rimuoviPC(block.getLocation());

        ItemStack pc = new ItemStack(Material.PURPUR_BLOCK);
        ItemMeta meta = pc.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&3&l" + nomeAzienda + " PC"));
        meta.setCustomModelData(1);
        pc.setItemMeta(meta);

        e.setDropItems(false);
        block.getWorld().dropItemNaturally(block.getLocation(), pc);

        player.sendMessage(ChatColor.GREEN + "PC aziendale rimosso!");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;
        if (e.getClickedBlock().getType() != Material.PURPUR_BLOCK) return;

        String nomeAzienda = plugin.getAziendaManager().getAziendaByPC(e.getClickedBlock().getLocation());
        if (nomeAzienda == null) return;

        Player player = e.getPlayer();
        Azienda azienda = plugin.getAziendaManager().getAzienda(nomeAzienda);

        if (azienda == null) {
            player.sendMessage(ChatColor.RED + "Errore: Azienda non trovata!");
            return;
        }

        if (!azienda.isDipendente(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Non fai parte di questa azienda!");
            return;
        }

        e.setCancelled(true);
        PCMainGUI gui = new PCMainGUI(plugin, azienda, player);
        gui.open();
    }
}