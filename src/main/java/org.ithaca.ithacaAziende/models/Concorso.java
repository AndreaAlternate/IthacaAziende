package org.ithaca.ithacaAziende.models;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Concorso {

    public static final int MAX_CONCORSI_GLOBALI = 4;
    public static final int MAX_POSTI = 30;
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final String nomeAzienda;
    private final String tipoConcorso;
    private final LocalDate data;
    private final String luogo;
    private final int postiDisponibili;
    private final LocalTime ora;
    private final long timestamp; // Per ordinamento

    public Concorso(String nomeAzienda, String tipoConcorso, LocalDate data, String luogo, int postiDisponibili, LocalTime ora) {
        this.nomeAzienda = nomeAzienda;
        this.tipoConcorso = tipoConcorso;
        this.data = data;
        this.luogo = luogo;
        this.postiDisponibili = postiDisponibili;
        this.ora = ora;
        this.timestamp = System.currentTimeMillis();
    }

    public String getNomeAzienda() {
        return nomeAzienda;
    }

    public String getTipoConcorso() {
        return tipoConcorso;
    }

    public LocalDate getData() {
        return data;
    }

    public String getDataFormatted() {
        return data.format(DATE_FORMATTER);
    }

    public String getLuogo() {
        return luogo;
    }

    public int getPostiDisponibili() {
        return postiDisponibili;
    }

    public LocalTime getOra() {
        return ora;
    }

    public String getOraFormatted() {
        return ora.format(TIME_FORMATTER);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isScaduto() {
        LocalDate oggi = LocalDate.now();
        return data.isBefore(oggi);
    }
}