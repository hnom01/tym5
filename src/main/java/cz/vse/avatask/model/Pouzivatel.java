package cz.vse.avatask.model;

public class Pouzivatel {
    private final int id;
    private final String uzivatelskeMeno;
    private final String heslo;
    private final String email;
    private final int xp;
    private final int level;
    private final int herniMena;
    private final int pocetVolnychProdlouzeni;
    private final boolean notifikace;
    private final boolean notifikaceDeadline;
    private final boolean notifikaceMesic;
    private final boolean notifikaceTyden;
    private final boolean lightMode;

    public Pouzivatel(int id, String uzivatelskeMeno, String heslo, String email, int xp, int level, int herniMena,
                      int pocetVolnychProdlouzeni, boolean notifikace, boolean notifikaceDeadline,
                      boolean notifikaceMesic, boolean notifikaceTyden, boolean lightMode) {
        this.id = id;
        this.uzivatelskeMeno = uzivatelskeMeno;
        this.heslo = heslo;
        this.email = email;
        this.xp = xp;
        this.level = level;
        this.herniMena = herniMena;
        this.pocetVolnychProdlouzeni = pocetVolnychProdlouzeni;
        this.notifikace = notifikace;
        this.notifikaceDeadline = notifikaceDeadline;
        this.notifikaceMesic = notifikaceMesic;
        this.notifikaceTyden = notifikaceTyden;
        this.lightMode = lightMode;
    }

    public int getId() {
        return id;
    }

    public String getUzivatelskeMeno() {
        return uzivatelskeMeno;
    }

    public String getHeslo() {
        return heslo;
    }

    public String getEmail() {
        return email;
    }

    public int getXp() {
        return xp;
    }

    public int getLevel() {
        return level;
    }

    public int getHerniMena() {
        return herniMena;
    }

    public int getPocetVolnychProdlouzeni() {
        return pocetVolnychProdlouzeni;
    }

    public boolean isNotifikace() {
        return notifikace;
    }

    public boolean isNotifikaceDeadline() {
        return notifikaceDeadline;
    }

    public boolean isNotifikaceMesic() {
        return notifikaceMesic;
    }

    public boolean isNotifikaceTyden() {
        return notifikaceTyden;
    }

    public boolean isLightMode() {
        return lightMode;
    }
}
