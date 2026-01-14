package org.ithaca.ithacaAziende.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.ithaca.ithacaAziende.IthacaAziende;
import org.ithaca.ithacaAziende.models.Azienda;

public class AziendaCommand implements CommandExecutor {

    private final IthacaAziende plugin;

    public AziendaCommand(IthacaAziende plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Solo i giocatori possono usare questo comando!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "crea":
                handleCrea(player, args);
                break;
            case "elimina":
                handleElimina(player, args);
                break;
            case "give":
                handleGive(player, args);
                break;
            case "concorso":
                handleConcorso(player, args);
                break;
            default:
                sendUsage(player);
                break;
        }

        return true;
    }

    private void handleCrea(Player player, String[] args) {
        if (!player.hasPermission("ithacaaziende.crea")) {
            player.sendMessage(ChatColor.RED + "Non hai il permesso per creare aziende!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso: /azienda crea <nome>");
            return;
        }

        // Controlla se ha già un'azienda
        if (plugin.getAziendaManager().hasAzienda(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Hai già un'azienda! Puoi crearne solo una.");
            return;
        }

        StringBuilder nomeBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) nomeBuilder.append(" ");
            nomeBuilder.append(args[i]);
        }
        String nome = nomeBuilder.toString();

        if (plugin.getAziendaManager().getAzienda(nome) != null) {
            player.sendMessage(ChatColor.RED + "Un'azienda con questo nome esiste già!");
            return;
        }

        plugin.getAziendaManager().creaAzienda(nome, player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Azienda '" + nome + "' creata con successo!");
    }

    private void handleElimina(Player player, String[] args) {
        if (!player.hasPermission("ithacaaziende.elimina")) {
            player.sendMessage(ChatColor.RED + "Non hai il permesso per eliminare aziende!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso: /azienda elimina <nome>");
            return;
        }

        StringBuilder nomeBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) nomeBuilder.append(" ");
            nomeBuilder.append(args[i]);
        }
        String nome = nomeBuilder.toString();

        Azienda azienda = plugin.getAziendaManager().getAzienda(nome);
        if (azienda == null) {
            player.sendMessage(ChatColor.RED + "Azienda non trovata!");
            return;
        }

        // Solo il proprietario o un admin può eliminare
        if (!azienda.getProprietario().equals(player.getUniqueId()) && !player.hasPermission("ithacaaziende.elimina")) {
            player.sendMessage(ChatColor.RED + "Solo il proprietario può eliminare questa azienda!");
            return;
        }

        plugin.getAziendaManager().eliminaAzienda(nome);
        player.sendMessage(ChatColor.GREEN + "Azienda '" + nome + "' eliminata con successo!");
    }

    private void handleGive(Player player, String[] args) {
        if (!player.hasPermission("ithacaaziende.give")) {
            player.sendMessage(ChatColor.RED + "Non hai il permesso per dare PC aziendali!");
            return;
        }

        if (args.length < 3 || !args[1].equalsIgnoreCase("pc")) {
            player.sendMessage(ChatColor.RED + "Uso: /azienda give pc <nome azienda>");
            return;
        }

        StringBuilder nomeBuilder = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (i > 2) nomeBuilder.append(" ");
            nomeBuilder.append(args[i]);
        }
        String nome = nomeBuilder.toString();

        Azienda azienda = plugin.getAziendaManager().getAzienda(nome);
        if (azienda == null) {
            player.sendMessage(ChatColor.RED + "Azienda non trovata!");
            return;
        }

        ItemStack pc = new ItemStack(Material.PURPUR_BLOCK);
        ItemMeta meta = pc.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&3&l" + nome + " PC"));
        meta.setCustomModelData(1);
        pc.setItemMeta(meta);

        player.getInventory().addItem(pc);
        player.sendMessage(ChatColor.GREEN + "Ti è stato dato il PC dell'azienda '" + nome + "'!");
    }

    private void sendUsage(Player player) {
        player.sendMessage(ChatColor.YELLOW + "Uso comandi azienda:");
        player.sendMessage(ChatColor.YELLOW + "/azienda crea <nome> - Crea una nuova azienda");
        player.sendMessage(ChatColor.YELLOW + "/azienda elimina <nome> - Elimina un'azienda");
        player.sendMessage(ChatColor.YELLOW + "/azienda give pc <nome> - Ottieni un PC aziendale");
        player.sendMessage(ChatColor.YELLOW + "/azienda concorso rimuovi <nome> - Rimuovi un concorso");
    }

    private void handleConcorso(Player player, String[] args) {
        if (!player.hasPermission("ithacaaziende.elimina")) {
            player.sendMessage(ChatColor.RED + "Non hai il permesso per gestire i concorsi!");
            return;
        }

        if (args.length < 3 || !args[1].equalsIgnoreCase("rimuovi")) {
            player.sendMessage(ChatColor.RED + "Uso: /azienda concorso rimuovi <nome concorso completo>");
            player.sendMessage(ChatColor.YELLOW + "Esempio: /azienda concorso rimuovi NomeAzienda - NomeRuolo");
            return;
        }

        StringBuilder nomeCompleto = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (i > 2) nomeCompleto.append(" ");
            nomeCompleto.append(args[i]);
        }

        String nome = nomeCompleto.toString();
        String[] parti = nome.split(" - ");

        if (parti.length != 2) {
            player.sendMessage(ChatColor.RED + "Formato nome non valido!");
            player.sendMessage(ChatColor.YELLOW + "Usa: NomeAzienda - NomeRuolo");
            return;
        }

        String nomeAzienda = parti[0].trim();
        String tipoRuolo = parti[1].trim();

        if (plugin.getConcorsoManager().rimuoviConcorso(nomeAzienda, tipoRuolo)) {
            player.sendMessage(ChatColor.GREEN + "Concorso '" + nome + "' rimosso con successo!");
        } else {
            player.sendMessage(ChatColor.RED + "Concorso non trovato!");
        }
    }
}