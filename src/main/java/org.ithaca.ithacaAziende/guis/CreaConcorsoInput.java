package org.ithaca.ithacaAziende.guis;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.conversations.*;
import org.bukkit.entity.Player;
import org.ithaca.ithacaAziende.IthacaAziende;
import org.ithaca.ithacaAziende.models.Azienda;
import org.ithaca.ithacaAziende.models.Concorso;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public class CreaConcorsoInput {

    private final IthacaAziende plugin;
    private final Azienda azienda;
    private final Player player;
    private final String tipoConcorso;
    private final ConcorsiGUI previousGUI;

    private LocalDate data;
    private String luogo;
    private int posti;
    private LocalTime ora;

    public CreaConcorsoInput(IthacaAziende plugin, Azienda azienda, Player player, String tipoConcorso, ConcorsiGUI previousGUI) {
        this.plugin = plugin;
        this.azienda = azienda;
        this.player = player;
        this.tipoConcorso = tipoConcorso;
        this.previousGUI = previousGUI;
    }

    public void start() {
        ConversationFactory factory = new ConversationFactory(plugin)
                .withFirstPrompt(new DataPrompt())
                .withLocalEcho(false)
                .withTimeout(120)
                .withPrefix(context -> ChatColor.DARK_PURPLE + "[Concorso] " + ChatColor.RESET)
                .addConversationAbandonedListener(event -> {
                    if (!event.gracefulExit()) {
                        player.sendMessage(ChatColor.DARK_PURPLE + "[Concorso] " + ChatColor.RED + "Creazione concorso annullata.");
                        if (previousGUI != null) {
                            Bukkit.getScheduler().runTaskLater(plugin, () -> previousGUI.open(), 1L);
                        }
                    }
                });

        factory.buildConversation(player).begin();
    }

    private class DataPrompt extends StringPrompt {
        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.YELLOW + "Inserisci la data del concorso (formato: GG/MM/AA, es: 31/12/26) o scrivi 'annulla':";
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            if (input.equalsIgnoreCase("annulla")) {
                player.sendMessage(ChatColor.DARK_PURPLE + "[Concorso] " + ChatColor.YELLOW + "Creazione concorso annullata.");
                if (previousGUI != null) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> previousGUI.open(), 1L);
                }
                return Prompt.END_OF_CONVERSATION;
            }

            try {
                data = LocalDate.parse(input, Concorso.DATE_FORMATTER);

                if (data.isBefore(LocalDate.now())) {
                    player.sendMessage(ChatColor.DARK_PURPLE + "[Concorso] " + ChatColor.RED + "La data non può essere nel passato!");
                    return this;
                }

                return new LuogoPrompt();

            } catch (DateTimeParseException e) {
                player.sendMessage(ChatColor.DARK_PURPLE + "[Concorso] " + ChatColor.RED + "Formato data non valido! Usa: GG/MM/AA (es: 31/12/26)");
                return this;
            }
        }
    }

    private class LuogoPrompt extends StringPrompt {
        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.YELLOW + "Inserisci il luogo del concorso o scrivi 'annulla':";
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            if (input.equalsIgnoreCase("annulla")) {
                player.sendMessage(ChatColor.DARK_PURPLE + "[Concorso] " + ChatColor.YELLOW + "Creazione concorso annullata.");
                if (previousGUI != null) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> previousGUI.open(), 1L);
                }
                return Prompt.END_OF_CONVERSATION;
            }

            if (input == null || input.trim().isEmpty()) {
                player.sendMessage(ChatColor.DARK_PURPLE + "[Concorso] " + ChatColor.RED + "Devi inserire un luogo!");
                return this;
            }

            luogo = input.trim();
            return new PostiPrompt();
        }
    }

    private class PostiPrompt extends NumericPrompt {
        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.YELLOW + "Inserisci il numero di posti disponibili (1-" + Concorso.MAX_POSTI + ") o scrivi 'annulla':";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, Number input) {
            posti = input.intValue();

            if (posti <= 0) {
                player.sendMessage(ChatColor.DARK_PURPLE + "[Concorso] " + ChatColor.RED + "I posti devono essere maggiori di 0!");
                return this;
            }

            if (posti > Concorso.MAX_POSTI) {
                player.sendMessage(ChatColor.DARK_PURPLE + "[Concorso] " + ChatColor.RED + "Massimo " + Concorso.MAX_POSTI + " posti disponibili!");
                return this;
            }

            return new OraPrompt();
        }

        @Override
        protected boolean isNumberValid(ConversationContext context, Number input) {
            return input.intValue() > 0 && input.intValue() <= Concorso.MAX_POSTI;
        }

        @Override
        protected String getInputNotNumericText(ConversationContext context, String invalidInput) {
            if (invalidInput.equalsIgnoreCase("annulla")) {
                player.sendMessage(ChatColor.DARK_PURPLE + "[Concorso] " + ChatColor.YELLOW + "Creazione concorso annullata.");
                if (previousGUI != null) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> previousGUI.open(), 1L);
                }
                return null;
            }
            return ChatColor.DARK_PURPLE + "[Concorso] " + ChatColor.RED + "Devi inserire un numero valido!";
        }

        @Override
        protected String getFailedValidationText(ConversationContext context, Number invalidInput) {
            return ChatColor.DARK_PURPLE + "[Concorso] " + ChatColor.RED + "Il numero deve essere tra 1 e " + Concorso.MAX_POSTI + "!";
        }
    }

    private class OraPrompt extends StringPrompt {
        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.YELLOW + "Inserisci l'ora del concorso (formato: HH:MM, es: 14:30) o scrivi 'annulla':";
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            if (input.equalsIgnoreCase("annulla")) {
                player.sendMessage(ChatColor.DARK_PURPLE + "[Concorso] " + ChatColor.YELLOW + "Creazione concorso annullata.");
                if (previousGUI != null) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> previousGUI.open(), 1L);
                }
                return Prompt.END_OF_CONVERSATION;
            }

            try {
                ora = LocalTime.parse(input, Concorso.TIME_FORMATTER);

                plugin.getConcorsoManager().creaConcorso(
                        azienda.getNome(),
                        tipoConcorso,
                        data,
                        luogo,
                        posti,
                        ora
                );

                player.sendMessage(ChatColor.LIGHT_PURPLE + "[Concorso] " + ChatColor.GREEN + "Concorso creato con successo!");
                player.sendMessage(ChatColor.YELLOW + "Nome: " + ChatColor.WHITE + azienda.getNome() + " - " + tipoConcorso);
                player.sendMessage(ChatColor.YELLOW + "Data: " + ChatColor.WHITE + data.format(Concorso.DATE_FORMATTER));
                player.sendMessage(ChatColor.YELLOW + "Luogo: " + ChatColor.WHITE + luogo);
                player.sendMessage(ChatColor.YELLOW + "Posti: " + ChatColor.WHITE + posti);
                player.sendMessage(ChatColor.YELLOW + "Ora: " + ChatColor.WHITE + ora.format(Concorso.TIME_FORMATTER));

                if (previousGUI != null) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        ConcorsiGUI nuovaGUI = new ConcorsiGUI(plugin, azienda, player, previousGUI.previousGUI);
                        nuovaGUI.open();
                    }, 1L);
                }

                return Prompt.END_OF_CONVERSATION;

            } catch (DateTimeParseException e) {
                player.sendMessage(ChatColor.DARK_PURPLE + "[Concorso] " + ChatColor.YELLOW + "Formato ora non valido! Usa: HH:MM (es: 14:30)");
                return this;
            }
        }
    }
}