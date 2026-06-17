package cz.vse.avatask.model;

import java.time.LocalDate;

public class Uloha {
    private final int id;
    private final String nazov;
    private final LocalDate deadline;
    private final String stav;
    private final String popis;
    private final int xpOdmena;
    private LocalDate datumDokonceni;
    private final String obtiaznost;


    public Uloha(int id, String nazov, LocalDate deadline, String stav, String popis, int xpOdmena, LocalDate datumDokonceni, String obtiaznost) {
        this.id = id;
        this.nazov = nazov;
        this.deadline = deadline;
        this.stav = stav;
        this.popis = popis;
        this.xpOdmena = xpOdmena;
        this.datumDokonceni = datumDokonceni;
        this.obtiaznost = obtiaznost;
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

    public String getObtiaznost() { return obtiaznost; }

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



    public LocalDate getDatumDokonceni() {
        return datumDokonceni;
    }

}
