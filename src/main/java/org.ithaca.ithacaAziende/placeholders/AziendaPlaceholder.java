package org.ithaca.ithacaAziende.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.ithaca.ithacaAziende.IthacaAziende;
import org.ithaca.ithacaAziende.models.Azienda;
import org.ithaca.ithacaAziende.models.Concorso;
import org.jetbrains.annotations.NotNull;

public class AziendaPlaceholder extends PlaceholderExpansion {

    private final IthacaAziende plugin;

    public AziendaPlaceholder(IthacaAziende plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "concorso";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        // %concorso_azienda<numero>_<info>%
        // Esempio: %concorso_azienda1_nome%

        if (identifier.matches("azienda[1-4]_nome")) {
            try {
                int index = Integer.parseInt(identifier.substring(7, 8)) - 1;
                Concorso concorso = plugin.getConcorsoManager().getConcorsoBySlot(index);
                if (concorso != null) {
                    return concorso.getNomeAzienda() + " - " + concorso.getTipoConcorso();
                }
                return "";
            } catch (Exception e) {
                return "";
            }
        }

        if (identifier.matches("azienda[1-4]_data")) {
            try {
                int index = Integer.parseInt(identifier.substring(7, 8)) - 1;
                Concorso concorso = plugin.getConcorsoManager().getConcorsoBySlot(index);
                if (concorso != null) {
                    return concorso.getDataFormatted();
                }
                return "";
            } catch (Exception e) {
                return "";
            }
        }

        if (identifier.matches("azienda[1-4]_luogo")) {
            try {
                int index = Integer.parseInt(identifier.substring(7, 8)) - 1;
                Concorso concorso = plugin.getConcorsoManager().getConcorsoBySlot(index);
                if (concorso != null) {
                    return concorso.getLuogo();
                }
                return "";
            } catch (Exception e) {
                return "";
            }
        }

        if (identifier.matches("azienda[1-4]_posti")) {
            try {
                int index = Integer.parseInt(identifier.substring(7, 8)) - 1;
                Concorso concorso = plugin.getConcorsoManager().getConcorsoBySlot(index);
                if (concorso != null) {
                    return String.valueOf(concorso.getPostiDisponibili());
                }
                return "";
            } catch (Exception e) {
                return "";
            }
        }

        if (identifier.matches("azienda[1-4]_orario")) {
            try {
                int index = Integer.parseInt(identifier.substring(7, 8)) - 1;
                Concorso concorso = plugin.getConcorsoManager().getConcorsoBySlot(index);
                if (concorso != null) {
                    return concorso.getOraFormatted();
                }
                return "";
            } catch (Exception e) {
                return "";
            }
        }

        return null;
    }
}