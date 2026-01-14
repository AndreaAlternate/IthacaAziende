package org.ithaca.ithacaAziende.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.ithaca.ithacaAziende.IthacaAziende;
import org.ithaca.ithacaAziende.models.Azienda;
import org.ithaca.ithacaAziende.models.DipendenteInfo;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class PlayerJoinListener implements Listener {

    private final IthacaAziende plugin;

    public PlayerJoinListener(IthacaAziende plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Controlla dopo 2 secondi per dare tempo al player di caricare
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            checkPlayerNotifications(player);
        }, 40L); // 2 secondi
    }

    private void checkPlayerNotifications(Player player) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        boolean haNotifiche = false;

        // Cerca in tutte le aziende se il player è dipendente
        for (Azienda azienda : plugin.getAziendaManager().getAllAziende()) {
            if (!azienda.isDipendente(player.getUniqueId())) {
                continue;
            }

            DipendenteInfo info = azienda.getInfoDipendente(player.getUniqueId());

            // Controlla incarichi non completati
            List<DipendenteInfo.Incarico> incarichiNonCompletati = info.getIncarichiNonCompletati();

            // Controlla richiami
            List<DipendenteInfo.Richiamo> richiami = info.getRichiami();

            if (!incarichiNonCompletati.isEmpty() || !richiami.isEmpty()) {
                if (!haNotifiche) {
                    player.sendMessage("");
                    player.sendMessage(ChatColor.GOLD + "═══════════════════════════════════");
                    player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "    NOTIFICHE AZIENDALI");
                    player.sendMessage(ChatColor.GOLD + "═══════════════════════════════════");
                    haNotifiche = true;
                }

                player.sendMessage("");
                player.sendMessage(ChatColor.YELLOW + "Azienda: " + ChatColor.WHITE + ChatColor.BOLD + azienda.getNome());

                // Mostra incarichi non completati
                if (!incarichiNonCompletati.isEmpty()) {
                    player.sendMessage("");
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "➤ INCARICHI IN SOSPESO:");

                    for (int i = 0; i < Math.min(3, incarichiNonCompletati.size()); i++) {
                        DipendenteInfo.Incarico incarico = incarichiNonCompletati.get(i);
                        player.sendMessage(ChatColor.GRAY + "  " + (i + 1) + ". " + ChatColor.WHITE + incarico.getDescrizione());
                        player.sendMessage(ChatColor.GRAY + "     Data: " + ChatColor.YELLOW + incarico.getData().format(formatter));
                    }

                    if (incarichiNonCompletati.size() > 3) {
                        player.sendMessage(ChatColor.GRAY + "  ... e altri " + (incarichiNonCompletati.size() - 3) + " incarichi");
                    }
                }

                // Mostra richiami
                if (!richiami.isEmpty()) {
                    player.sendMessage("");
                    player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "⚠ RICHIAMI RICEVUTI:");

                    for (int i = 0; i < Math.min(3, richiami.size()); i++) {
                        DipendenteInfo.Richiamo richiamo = richiami.get(i);
                        player.sendMessage(ChatColor.GRAY + "  " + (i + 1) + ". " + ChatColor.WHITE + richiamo.getMotivo());
                        player.sendMessage(ChatColor.GRAY + "     Data: " + ChatColor.YELLOW + richiamo.getData().format(formatter));
                    }

                    if (richiami.size() > 3) {
                        player.sendMessage(ChatColor.GRAY + "  ... e altri " + (richiami.size() - 3) + " richiami");
                    }
                }
            }
        }

        if (haNotifiche) {
            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "═══════════════════════════════════");
            player.sendMessage("");
        }
    }
}