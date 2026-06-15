package cz.vse.avatask.model;

import java.time.LocalDate;

public class Uloha {
    private final int id;
    private final String nazov;
    private final LocalDate deadline;
    private final String stav;
    private final String popis;
    private final int xpOdmena;

    public Uloha(int id, String nazov, LocalDate deadline, String stav, String popis, int xpOdmena) {
        this.id = id;
        this.nazov = nazov;
        this.deadline = deadline;
        this.stav = stav;
        this.popis = popis;
        this.xpOdmena = xpOdmena;
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

    public int getXpOdmena() { return xpOdmena;}

    public boolean jeDokoncena() {
        return "Dokoncena".equalsIgnoreCase(stav);
    }
    private int pocetProdlouzeni;

    public int getPocetProdlouzeni() {
        return pocetProdlouzeni;
    }

    public void setPocetProdlouzeni(int pocetProdlouzeni) {
        this.pocetProdlouzeni = pocetProdlouzeni;
    }
}
