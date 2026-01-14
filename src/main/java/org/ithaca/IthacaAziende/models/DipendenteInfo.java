package org.ithaca.ithacaAziende.models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DipendenteInfo {

    private String ruolo;
    private final List<Nota> note;
    private final List<Richiamo> richiami;
    private final List<Incarico> incarichi;

    public DipendenteInfo() {
        this.ruolo = null;
        this.note = new ArrayList<>();
        this.richiami = new ArrayList<>();
        this.incarichi = new ArrayList<>();
    }

    public String getRuolo() {
        return ruolo;
    }

    public void setRuolo(String ruolo) {
        this.ruolo = ruolo;
    }

    public List<Nota> getNote() {
        return new ArrayList<>(note);
    }

    public void aggiungiNota(String testo, LocalDate data) {
        note.add(new Nota(testo, data));
    }

    public void rimuoviNota(int index) {
        if (index >= 0 && index < note.size()) {
            note.remove(index);
        }
    }

    public List<Richiamo> getRichiami() {
        return new ArrayList<>(richiami);
    }

    public void aggiungiRichiamo(String motivo, LocalDate data) {
        richiami.add(new Richiamo(motivo, data));
    }

    public void rimuoviRichiamo(int index) {
        if (index >= 0 && index < richiami.size()) {
            richiami.remove(index);
        }
    }

    public List<Incarico> getIncarichi() {
        return new ArrayList<>(incarichi);
    }

    public List<Incarico> getIncarichiNonCompletati() {
        List<Incarico> nonCompletati = new ArrayList<>();
        for (Incarico i : incarichi) {
            if (!i.isCompletato()) {
                nonCompletati.add(i);
            }
        }
        return nonCompletati;
    }

    public void aggiungiIncarico(String descrizione, LocalDate data) {
        incarichi.add(new Incarico(descrizione, data));
    }

    public void setIncaricoCompletato(int index, boolean completato) {
        if (index >= 0 && index < incarichi.size()) {
            incarichi.get(index).setCompletato(completato);
        }
    }

    public void rimuoviIncarico(int index) {
        if (index >= 0 && index < incarichi.size()) {
            incarichi.remove(index);
        }
    }

    public static class Nota {
        private final String testo;
        private final LocalDate data;

        public Nota(String testo, LocalDate data) {
            this.testo = testo;
            this.data = data;
        }

        public String getTesto() {
            return testo;
        }

        public LocalDate getData() {
            return data;
        }
    }

    public static class Richiamo {
        private final String motivo;
        private final LocalDate data;

        public Richiamo(String motivo, LocalDate data) {
            this.motivo = motivo;
            this.data = data;
        }

        public String getMotivo() {
            return motivo;
        }

        public LocalDate getData() {
            return data;
        }
    }

    public static class Incarico {
        private final String descrizione;
        private final LocalDate data;
        private boolean completato;

        public Incarico(String descrizione, LocalDate data) {
            this.descrizione = descrizione;
            this.data = data;
            this.completato = false;
        }

        public String getDescrizione() {
            return descrizione;
        }

        public LocalDate getData() {
            return data;
        }

        public boolean isCompletato() {
            return completato;
        }

        public void setCompletato(boolean completato) {
            this.completato = completato;
        }
    }
}