package cz.vse.avatask.model;

import java.time.LocalDate;

public class Uloha {
    private final int id;
    private final String nazov;
    private final LocalDate deadline;
    private final String stav;
    private final String popis;
    private final int xp;

    public Uloha(int id, String nazov, LocalDate deadline, String stav, String popis, int xp) {
        this.id = id;
        this.nazov = nazov;
        this.deadline = deadline;
        this.stav = stav;
        this.popis = popis;
        this.xp = xp;
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

    public int getXp() {
        return xp;
    }

    public boolean jeDokoncena() {
        return "Dokoncena".equalsIgnoreCase(stav);
    }
}