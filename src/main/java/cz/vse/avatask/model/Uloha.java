package cz.vse.avatask.model;

import java.time.LocalDate;

public class Uloha {
    private final int id;
    private final String nazov;
    private final LocalDate deadline;
    private final String stav;
    private final String popis;

    public Uloha(int id, String nazov, LocalDate deadline, String stav, String popis) {
        this.id = id;
        this.nazov = nazov;
        this.deadline = deadline;
        this.stav = stav;
        this.popis = popis;
    }

    public int getId() {
        return id;
    }

    public String getNazov() {
        return nazov;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public String getStav() {
        return stav;
    }

    public String getPopis() {
        return popis;
    }

    public boolean jeDokoncena() {
        return "Dokoncena".equalsIgnoreCase(stav);
    }
}
