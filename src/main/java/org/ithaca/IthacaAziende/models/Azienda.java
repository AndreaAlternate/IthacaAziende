package org.ithaca.ithacaAziende.models;

import java.time.LocalDate;
import java.util.*;

public class Azienda {

    private final String nome;
    private final UUID proprietario;
    private final Set<UUID> dipendenti;
    private final Map<UUID, StipendioInfo> stipendi;
    private final List<Transazione> transazioni;
    private final Set<String> ruoli;
    private final Map<UUID, DipendenteInfo> infoDipendenti;
    private String colorePrefisso;

    public Azienda(String nome, UUID proprietario) {
        this.nome = nome;
        this.proprietario = proprietario;
        this.dipendenti = new HashSet<>();
        this.stipendi = new HashMap<>();
        this.transazioni = new ArrayList<>();
        this.ruoli = new HashSet<>();
        this.infoDipendenti = new HashMap<>();
        this.colorePrefisso = "&6"; // Colore default (oro)
    }

    public String getNome() {
        return nome;
    }

    public UUID getProprietario() {
        return proprietario;
    }

    public Set<UUID> getDipendenti() {
        return new HashSet<>(dipendenti);
    }

    public void aggiungiDipendente(UUID uuid) {
        dipendenti.add(uuid);
    }

    public void rimuoviDipendente(UUID uuid) {
        dipendenti.remove(uuid);
        stipendi.remove(uuid);
    }

    public boolean isDipendente(UUID uuid) {
        return dipendenti.contains(uuid) || proprietario.equals(uuid);
    }

    public void setStipendio(UUID uuid, double importo, LocalDate dataScadenza) {
        stipendi.put(uuid, new StipendioInfo(importo, dataScadenza));
    }

    public StipendioInfo getStipendio(UUID uuid) {
        return stipendi.get(uuid);
    }

    public Map<UUID, StipendioInfo> getTuttiStipendi() {
        return new HashMap<>(stipendi);
    }

    public void aggiungiTransazione(Transazione transazione) {
        transazioni.add(transazione);
    }

    public List<Transazione> getTransazioni() {
        return new ArrayList<>(transazioni);
    }

    public void aggiungiRuolo(String ruolo) {
        ruoli.add(ruolo);
    }

    public void rimuoviRuolo(String ruolo) {
        ruoli.remove(ruolo);
    }

    public Set<String> getRuoli() {
        return new HashSet<>(ruoli);
    }

    public DipendenteInfo getInfoDipendente(UUID uuid) {
        return infoDipendenti.computeIfAbsent(uuid, k -> new DipendenteInfo());
    }

    public void setRuoloDipendente(UUID uuid, String ruolo) {
        getInfoDipendente(uuid).setRuolo(ruolo);
    }

    public void aggiungiNotaDipendente(UUID uuid, String nota, LocalDate data) {
        getInfoDipendente(uuid).aggiungiNota(nota, data);
    }

    public void aggiungiRichiamoDipendente(UUID uuid, String motivo, LocalDate data) {
        getInfoDipendente(uuid).aggiungiRichiamo(motivo, data);
    }

    public void aggiungiIncaricoDipendente(UUID uuid, String descrizione, LocalDate data) {
        getInfoDipendente(uuid).aggiungiIncarico(descrizione, data);
    }

    public void setIncaricoCompletato(UUID uuid, int index, boolean completato) {
        getInfoDipendente(uuid).setIncaricoCompletato(index, completato);
    }

    public void setIncaricoDipendente(UUID uuid, String incarico) {
        getInfoDipendente(uuid).aggiungiIncarico(incarico, LocalDate.now());
    }

    // Metodi per la targhetta aziendale
    public String getColorePrefisso() {
        return colorePrefisso != null ? colorePrefisso : "&6";
    }

    public void setColorePrefisso(String colorePrefisso) {
        this.colorePrefisso = colorePrefisso;
    }

    public String getPrefissoFormattato() {
        return getColorePrefisso() + "&l" + nome + "&r ";
    }

    public String getSuffissoFormattato(String ruolo) {
        if (ruolo == null || ruolo.isEmpty()) {
            return " &7[Dipendente]";
        }
        if (ruolo.equalsIgnoreCase("Capo")) {
            return " &4[Capo]";
        }
        return " &7[" + ruolo + "]";
    }

    public static class StipendioInfo {
        private final double importo;
        private final LocalDate dataScadenza;

        public StipendioInfo(double importo, LocalDate dataScadenza) {
            this.importo = importo;
            this.dataScadenza = dataScadenza;
        }

        public double getImporto() {
            return importo;
        }

        public LocalDate getDataScadenza() {
            return dataScadenza;
        }
    }

    public static class Transazione {
        private final String descrizione;
        private final double importo;
        private final LocalDate data;
        private final UUID responsabile;

        public Transazione(String descrizione, double importo, UUID responsabile) {
            this.descrizione = descrizione;
            this.importo = importo;
            this.data = LocalDate.now();
            this.responsabile = responsabile;
        }

        public String getDescrizione() {
            return descrizione;
        }

        public double getImporto() {
            return importo;
        }

        public LocalDate getData() {
            return data;
        }

        public UUID getResponsabile() {
            return responsabile;
        }
    }
}