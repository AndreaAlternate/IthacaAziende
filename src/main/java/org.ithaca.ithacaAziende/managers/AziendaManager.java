package org.ithaca.ithacaAziende.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.ithaca.ithacaAziende.IthacaAziende;
import org.ithaca.ithacaAziende.models.Azienda;
import org.ithaca.ithacaAziende.utils.LocalDateAdapter;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AziendaManager {

    private final IthacaAziende plugin;
    private final Map<String, Azienda> aziende;
    private final Map<Location, String> pcLocations;
    private final Gson gson;
    private final File dataFolder;

    public AziendaManager(IthacaAziende plugin) {
        this.plugin = plugin;
        this.aziende = new HashMap<>();
        this.pcLocations = new HashMap<>();
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .enableComplexMapKeySerialization()
                .setPrettyPrinting()
                .create();
        this.dataFolder = new File(plugin.getDataFolder(), "data");

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        loadAll();
    }

    public void creaAzienda(String nome, UUID proprietario) {
        if (aziende.containsKey(nome.toLowerCase())) {
            return;
        }

        // Controlla se il proprietario ha già un'azienda
        for (Azienda a : aziende.values()) {
            if (a.getProprietario().equals(proprietario)) {
                return;
            }
        }

        Azienda azienda = new Azienda(nome, proprietario);
        aziende.put(nome.toLowerCase(), azienda);

        // Crea gruppo LuckPerms
        creaGruppoLuckPerms(nome);
        aggiungiMembroGruppo(proprietario, nome);

        // Assegna ruolo "Capo" al proprietario
        azienda.setRuoloDipendente(proprietario, "Capo");
        applicaTarghetta(azienda, proprietario);

        saveAzienda(nome);
    }

    public void eliminaAzienda(String nome) {
        Azienda azienda = aziende.remove(nome.toLowerCase());
        if (azienda == null) return;

        // Rimuovi targhetta e gruppo dal proprietario
        rimuoviTarghetta(azienda.getProprietario());
        rimuoviMembroGruppo(azienda.getProprietario(), nome);

        // Rimuovi targhetta e gruppo da tutti i dipendenti
        for (UUID dipendente : azienda.getDipendenti()) {
            rimuoviTarghetta(dipendente);
            rimuoviMembroGruppo(dipendente, nome);
        }

        // Elimina gruppo LuckPerms
        eliminaGruppoLuckPerms(nome);

        // Rimuovi PC locations
        pcLocations.entrySet().removeIf(entry -> entry.getValue().equalsIgnoreCase(nome));

        // Elimina file
        File file = new File(dataFolder, nome.toLowerCase() + ".json");
        if (file.exists()) {
            file.delete();
        }

        savePCLocations();
    }

    public Azienda getAzienda(String nome) {
        return aziende.get(nome.toLowerCase());
    }

    public Collection<Azienda> getAllAziende() {
        return aziende.values();
    }

    public void registraPC(Location location, String nomeAzienda) {
        pcLocations.put(location, nomeAzienda);
        savePCLocations();
    }

    public void rimuoviPC(Location location) {
        pcLocations.remove(location);
        savePCLocations();
    }

    public String getAziendaByPC(Location location) {
        return pcLocations.get(location);
    }

    public boolean hasAzienda(UUID proprietario) {
        for (Azienda azienda : aziende.values()) {
            if (azienda.getProprietario().equals(proprietario)) {
                return true;
            }
        }
        return false;
    }

    public Azienda getAziendaByProprietario(UUID proprietario) {
        for (Azienda azienda : aziende.values()) {
            if (azienda.getProprietario().equals(proprietario)) {
                return azienda;
            }
        }
        return null;
    }

    private void creaGruppoLuckPerms(String nomeAzienda) {
        LuckPerms lp = plugin.getLuckPerms();
        String groupName = "azienda_" + nomeAzienda.toLowerCase().replace(" ", "_");

        lp.getGroupManager().createAndLoadGroup(groupName).thenAccept(group -> {
            if (group != null) {
                plugin.getLogger().info("Gruppo LuckPerms creato: " + groupName);
            }
        });
    }

    private void eliminaGruppoLuckPerms(String nomeAzienda) {
        LuckPerms lp = plugin.getLuckPerms();
        String groupName = "azienda_" + nomeAzienda.toLowerCase().replace(" ", "_");

        lp.getGroupManager().loadGroup(groupName).thenAccept(group -> {
            if (group.isPresent()) {
                lp.getGroupManager().deleteGroup(group.get());
                plugin.getLogger().info("Gruppo LuckPerms eliminato: " + groupName);
            }
        });
    }

    private void aggiungiMembroGruppo(UUID uuid, String nomeAzienda) {
        LuckPerms lp = plugin.getLuckPerms();
        String groupName = "azienda_" + nomeAzienda.toLowerCase().replace(" ", "_");

        CompletableFuture<User> userFuture = lp.getUserManager().loadUser(uuid);
        userFuture.thenAccept(user -> {
            InheritanceNode node = InheritanceNode.builder(groupName).build();
            user.data().add(node);
            lp.getUserManager().saveUser(user);
        });
    }

    private void rimuoviMembroGruppo(UUID uuid, String nomeAzienda) {
        LuckPerms lp = plugin.getLuckPerms();
        String groupName = "azienda_" + nomeAzienda.toLowerCase().replace(" ", "_");

        CompletableFuture<User> userFuture = lp.getUserManager().loadUser(uuid);
        userFuture.thenAccept(user -> {
            InheritanceNode node = InheritanceNode.builder(groupName).build();
            user.data().remove(node);
            lp.getUserManager().saveUser(user);
        });
    }

    public void assumiDipendente(String nomeAzienda, UUID dipendente) {
        Azienda azienda = getAzienda(nomeAzienda);
        if (azienda == null) return;

        azienda.aggiungiDipendente(dipendente);
        aggiungiMembroGruppo(dipendente, nomeAzienda);

        // Assegna ruolo predefinito "Dipendente"
        azienda.setRuoloDipendente(dipendente, "Dipendente");
        applicaTarghetta(azienda, dipendente);

        saveAzienda(nomeAzienda);
    }

    public void licenziaDipendente(String nomeAzienda, UUID dipendente) {
        Azienda azienda = getAzienda(nomeAzienda);
        if (azienda == null) return;

        // Rimuovi targhetta prima di rimuovere il dipendente
        rimuoviTarghetta(dipendente);

        azienda.rimuoviDipendente(dipendente);
        rimuoviMembroGruppo(dipendente, nomeAzienda);
        saveAzienda(nomeAzienda);
    }

    private void saveAzienda(String nome) {
        Azienda azienda = aziende.get(nome.toLowerCase());
        if (azienda == null) return;

        File file = new File(dataFolder, nome.toLowerCase() + ".json");
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(azienda, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Errore nel salvare l'azienda " + nome + ": " + e.getMessage());
        }
    }

    private void savePCLocations() {
        File file = new File(dataFolder, "pc_locations.json");
        try (Writer writer = new FileWriter(file)) {
            Map<String, String> serializable = new HashMap<>();
            for (Map.Entry<Location, String> entry : pcLocations.entrySet()) {
                Location loc = entry.getKey();
                String key = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
                serializable.put(key, entry.getValue());
            }
            gson.toJson(serializable, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Errore nel salvare le posizioni PC: " + e.getMessage());
        }
    }

    private void loadAll() {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".json") && !name.equals("pc_locations.json"));
        if (files != null) {
            for (File file : files) {
                try (Reader reader = new FileReader(file)) {
                    String json = new String(java.nio.file.Files.readAllBytes(file.toPath()));
                    if (json.trim().isEmpty() || json.trim().equals("[]")) {
                        plugin.getLogger().warning("File " + file.getName() + " vuoto o invalido, saltato.");
                        continue;
                    }

                    Azienda azienda = gson.fromJson(json, Azienda.class);
                    if (azienda != null && azienda.getNome() != null) {
                        aziende.put(azienda.getNome().toLowerCase(), azienda);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Impossibile caricare " + file.getName() + ": " + e.getMessage());
                    plugin.getLogger().warning("Il file potrebbe essere corrotto. Eliminalo manualmente se il problema persiste.");
                }
            }
        }

        File pcFile = new File(dataFolder, "pc_locations.json");
        if (pcFile.exists()) {
            try (Reader reader = new FileReader(pcFile)) {
                String json = new String(java.nio.file.Files.readAllBytes(pcFile.toPath()));
                if (json.trim().isEmpty() || json.trim().equals("[]")) {
                    return;
                }

                Map<String, String> serializable = gson.fromJson(json, new TypeToken<Map<String, String>>(){}.getType());
                if (serializable != null) {
                    for (Map.Entry<String, String> entry : serializable.entrySet()) {
                        try {
                            String[] parts = entry.getKey().split(",");
                            if (parts.length == 4) {
                                Location loc = new Location(
                                        Bukkit.getWorld(parts[0]),
                                        Integer.parseInt(parts[1]),
                                        Integer.parseInt(parts[2]),
                                        Integer.parseInt(parts[3])
                                );
                                pcLocations.put(loc, entry.getValue());
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Errore nel caricare una posizione PC: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Impossibile caricare le posizioni PC: " + e.getMessage());
            }
        }
    }

    public void saveAll() {
        for (String nome : aziende.keySet()) {
            saveAzienda(nome);
        }
        savePCLocations();
    }

    public void applicaTarghetta(Azienda azienda, UUID uuid) {
        LuckPerms lp = plugin.getLuckPerms();

        CompletableFuture<User> userFuture = lp.getUserManager().loadUser(uuid);
        userFuture.thenAccept(user -> {
            String prefisso = azienda.getPrefissoFormattato();
            String ruolo = azienda.getInfoDipendente(uuid).getRuolo();
            String suffisso = azienda.getSuffissoFormattato(ruolo);

            // Applica prefisso
            user.data().clear(net.luckperms.api.node.NodeType.PREFIX::matches);
            user.data().add(net.luckperms.api.node.types.PrefixNode.builder(prefisso, 100).build());

            // Applica suffisso
            user.data().clear(net.luckperms.api.node.NodeType.SUFFIX::matches);
            user.data().add(net.luckperms.api.node.types.SuffixNode.builder(suffisso, 100).build());

            lp.getUserManager().saveUser(user);
        });
    }

    public void rimuoviTarghetta(UUID uuid) {
        LuckPerms lp = plugin.getLuckPerms();

        CompletableFuture<User> userFuture = lp.getUserManager().loadUser(uuid);
        userFuture.thenAccept(user -> {
            // Rimuovi prefisso
            user.data().clear(net.luckperms.api.node.NodeType.PREFIX::matches);

            // Rimuovi suffisso
            user.data().clear(net.luckperms.api.node.NodeType.SUFFIX::matches);

            lp.getUserManager().saveUser(user);
            plugin.getLogger().info("Targhetta rimossa per il giocatore: " + uuid);
        });
    }

    public void applicaTarghettaTutti(Azienda azienda) {
        applicaTarghetta(azienda, azienda.getProprietario());
        for (UUID dipendente : azienda.getDipendenti()) {
            applicaTarghetta(azienda, dipendente);
        }
    }
}