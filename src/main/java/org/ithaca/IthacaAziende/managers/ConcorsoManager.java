package org.ithaca.ithacaAziende.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.ithaca.ithacaAziende.IthacaAziende;
import org.ithaca.ithacaAziende.models.Concorso;
import org.ithaca.ithacaAziende.utils.LocalDateAdapter;
import org.ithaca.ithacaAziende.utils.LocalTimeAdapter;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class ConcorsoManager {

    public static final int MAX_CONCORSI_PER_AZIENDA = 1;

    private final IthacaAziende plugin;
    private final List<Concorso> concorsi;
    private final Map<String, Set<String>> tipiConcorsoPerAzienda;
    private final Gson gson;
    private final File dataFile;
    private final File tipiFile;

    public ConcorsoManager(IthacaAziende plugin) {
        this.plugin = plugin;
        this.concorsi = new ArrayList<>();
        this.tipiConcorsoPerAzienda = new HashMap<>();
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .registerTypeAdapter(LocalTime.class, new LocalTimeAdapter())
                .setPrettyPrinting()
                .create();

        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        this.dataFile = new File(dataFolder, "concorsi.json");
        this.tipiFile = new File(dataFolder, "tipi_concorso.json");

        loadAll();
        pulisciConcorsiScaduti();
    }

    public boolean puoCreareConcorso(String nomeAzienda) {
        pulisciConcorsiScaduti();
        long count = concorsi.stream()
                .filter(c -> c.getNomeAzienda().equalsIgnoreCase(nomeAzienda))
                .count();
        return count < MAX_CONCORSI_PER_AZIENDA;
    }

    public void aggiungiTipoConcorso(String nomeAzienda, String tipo) {
        tipiConcorsoPerAzienda.computeIfAbsent(nomeAzienda.toLowerCase(), k -> new HashSet<>()).add(tipo);
        saveTipi();
    }

    public Set<String> getTipiConcorso(String nomeAzienda) {
        return new HashSet<>(tipiConcorsoPerAzienda.getOrDefault(nomeAzienda.toLowerCase(), new HashSet<>()));
    }

    public void creaConcorso(String nomeAzienda, String tipo, LocalDate data, String luogo, int posti, LocalTime ora) {
        if (!puoCreareConcorso(nomeAzienda)) {
            return;
        }

        Concorso concorso = new Concorso(nomeAzienda, tipo, data, luogo, posti, ora);
        concorsi.add(concorso);
        riorganizzaConcorsi();
        saveConcorsi();
    }

    public List<Concorso> getConcorsiAttivi() {
        pulisciConcorsiScaduti();
        return new ArrayList<>(concorsi);
    }

    public List<Concorso> getConcorsiByAzienda(String nomeAzienda) {
        pulisciConcorsiScaduti();
        return concorsi.stream()
                .filter(c -> c.getNomeAzienda().equalsIgnoreCase(nomeAzienda))
                .collect(Collectors.toList());
    }

    public Concorso getConcorsoBySlot(int slot) {
        pulisciConcorsiScaduti();
        if (slot < 0 || slot >= concorsi.size()) {
            return null;
        }
        return concorsi.get(slot);
    }

    public boolean rimuoviConcorso(String nomeAzienda, String tipoRuolo) {
        boolean removed = false;
        for (int i = 0; i < concorsi.size(); i++) {
            Concorso c = concorsi.get(i);
            if (c.getNomeAzienda().equalsIgnoreCase(nomeAzienda) &&
                    c.getTipoConcorso().equalsIgnoreCase(tipoRuolo)) {
                concorsi.remove(i);
                removed = true;
                break;
            }
        }

        if (removed) {
            riorganizzaConcorsi();
            saveConcorsi();
        }

        return removed;
    }

    private void pulisciConcorsiScaduti() {
        boolean removed = concorsi.removeIf(Concorso::isScaduto);
        if (removed) {
            riorganizzaConcorsi();
            saveConcorsi();
        }
    }

    private void riorganizzaConcorsi() {
        concorsi.sort(Comparator.comparingLong(Concorso::getTimestamp));
    }

    private void saveConcorsi() {
        try (Writer writer = new FileWriter(dataFile)) {
            gson.toJson(concorsi, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Errore nel salvare i concorsi: " + e.getMessage());
        }
    }

    private void saveTipi() {
        try (Writer writer = new FileWriter(tipiFile)) {
            gson.toJson(tipiConcorsoPerAzienda, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Errore nel salvare i tipi concorso: " + e.getMessage());
        }
    }

    private void loadAll() {
        if (dataFile.exists()) {
            try (Reader reader = new FileReader(dataFile)) {
                String json = new String(java.nio.file.Files.readAllBytes(dataFile.toPath()));
                if (!json.trim().isEmpty() && !json.trim().equals("[]")) {
                    List<Concorso> loaded = gson.fromJson(json, new TypeToken<List<Concorso>>(){}.getType());
                    if (loaded != null) {
                        concorsi.addAll(loaded);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Impossibile caricare i concorsi: " + e.getMessage());
            }
        }

        if (tipiFile.exists()) {
            try (Reader reader = new FileReader(tipiFile)) {
                String json = new String(java.nio.file.Files.readAllBytes(tipiFile.toPath()));
                if (!json.trim().isEmpty() && !json.trim().equals("[]")) {
                    Map<String, Set<String>> loaded = gson.fromJson(json, new TypeToken<Map<String, Set<String>>>(){}.getType());
                    if (loaded != null) {
                        tipiConcorsoPerAzienda.putAll(loaded);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Impossibile caricare i tipi concorso: " + e.getMessage());
            }
        }

        pulisciConcorsiScaduti();
    }

    public void saveAll() {
        saveConcorsi();
        saveTipi();
    }
}