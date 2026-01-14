package org.ithaca.ithacaAziende.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.ithaca.ithacaAziende.IthacaAziende;
import org.ithaca.ithacaAziende.models.Azienda;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AziendaTabCompleter implements TabCompleter {

    private final IthacaAziende plugin;

    public AziendaTabCompleter(IthacaAziende plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("crea", "elimina", "give", "concorso"));
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("concorso")) {
            completions.add("rimuovi");
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            completions.add("pc");
            return completions;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give") && args[1].equalsIgnoreCase("pc")) {
            for (Azienda azienda : plugin.getAziendaManager().getAllAziende()) {
                completions.add(azienda.getNome());
            }
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("elimina")) {
            for (Azienda azienda : plugin.getAziendaManager().getAllAziende()) {
                completions.add(azienda.getNome());
            }
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}