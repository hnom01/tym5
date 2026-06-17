package cz.vse.avatask.dao;

import cz.vse.avatask.CurrentUserSession;
import cz.vse.avatask.model.AkciaVysledok;
import cz.vse.avatask.model.AvatarVyzor;
import cz.vse.avatask.model.Pouzivatel;
import cz.vse.avatask.model.SatnikItem;
import cz.vse.avatask.model.Uloha;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.temporal.ChronoUnit;
import java.time.YearMonth;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:avatask.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void inicializujDatabazi() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = ON;");

            String sqlZakaznik = "CREATE TABLE IF NOT EXISTS ZAKAZNIK (" +
                    "id_zakaznik INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "uzivatelskeJmeno TEXT NOT NULL," +
                    "heslo TEXT NOT NULL," +
                    "email TEXT," +
                    "xp INTEGER DEFAULT 0," +
                    "level INTEGER DEFAULT 1," +
                    "herniMena INTEGER DEFAULT 0," +
                    "pocetVolnychProdlouzeni INTEGER DEFAULT 3," +
                    "pohlavie TEXT DEFAULT 'MUZ'" +
                    ");";
            stmt.execute(sqlZakaznik);

            String sqlNastaveni = "CREATE TABLE IF NOT EXISTS NASTAVENI (" +
                    "id_zakaznik INTEGER PRIMARY KEY," +
                    "notifikace BOOLEAN DEFAULT 1," +
                    "notifikaceDeadline BOOLEAN DEFAULT 1," +
                    "notifikaceMesic BOOLEAN DEFAULT 1," +
                    "notifikaceTyden BOOLEAN DEFAULT 1," +
                    "lightMode BOOLEAN DEFAULT 0," +
                    "FOREIGN KEY(id_zakaznik) REFERENCES ZAKAZNIK(id_zakaznik)" +
                    ");";
            stmt.execute(sqlNastaveni);


            String sqlUkol = "CREATE TABLE IF NOT EXISTS UKOL (" +
                    "id_ukol INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "id_zakaznik INTEGER," +
                    "nazev TEXT NOT NULL," +
                    "deadline DATE," +
                    "datumVytvoreni DATE," +
                    "popis TEXT," +
                    "stav TEXT DEFAULT 'Aktivna'," +
                    "xp_odmena INTEGER DEFAULT 20," +
                    "pocet_prodlouzeni INTEGER DEFAULT 0," +
                    "FOREIGN KEY(id_zakaznik) REFERENCES ZAKAZNIK(id_zakaznik)" +
                    ");";
            stmt.execute(sqlUkol);

            String sqlObleceni = "CREATE TABLE IF NOT EXISTS OBLECENI (" +
                    "id_obleceni INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "nazev TEXT NOT NULL," +
                    "popis TEXT," +
                    "cena INTEGER NOT NULL," +
                    "castTela TEXT NOT NULL" +
                    ");";
            stmt.execute(sqlObleceni);

            String sqlSatnikItem = "CREATE TABLE IF NOT EXISTS SATNIK_ITEM (" +
                    "id_satnik_item INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "id_zakaznik INTEGER," +
                    "id_obleceni INTEGER," +
                    "FOREIGN KEY(id_zakaznik) REFERENCES ZAKAZNIK(id_zakaznik)," +
                    "FOREIGN KEY(id_obleceni) REFERENCES OBLECENI(id_obleceni)" +
                    ");";
            stmt.execute(sqlSatnikItem);

            String sqlAvatar = "CREATE TABLE IF NOT EXISTS AVATAR (" +
                    "id_zakaznik INTEGER PRIMARY KEY," +
                    "id_hlava INTEGER," +
                    "id_telo INTEGER," +
                    "id_nohy INTEGER," +
                    "id_boty INTEGER," +
                    "FOREIGN KEY(id_zakaznik) REFERENCES ZAKAZNIK(id_zakaznik)," +
                    "FOREIGN KEY(id_hlava) REFERENCES SATNIK_ITEM(id_satnik_item)," +
                    "FOREIGN KEY(id_telo) REFERENCES SATNIK_ITEM(id_satnik_item)," +
                    "FOREIGN KEY(id_nohy) REFERENCES SATNIK_ITEM(id_satnik_item)," +
                    "FOREIGN KEY(id_boty) REFERENCES SATNIK_ITEM(id_satnik_item)" +
                    ");";
            stmt.execute(sqlAvatar);

            String sqlSystemConfig = "CREATE TABLE IF NOT EXISTS SYSTEM_CONFIG (" +
                    "klic TEXT PRIMARY KEY," +
                    "hodnota TEXT" +
                    ");";
            stmt.execute(sqlSystemConfig);

            pridajStlpecAkChyba(conn, "ZAKAZNIK", "pohlavie", "TEXT DEFAULT 'MUZ'");
            pridajStlpecAkChyba(conn, "UKOL", "datumDokonceni", "DATE");


            pridajStlpecAkChyba(conn, "UKOL", "pocet_prodlouzeni", "INTEGER DEFAULT 0");

            pridajStlpecAkChyba(conn, "UKOL", "obtiaznost", "TEXT DEFAULT 'Ľahká'");

            zaktivujZakladnyShop(conn);
            stmt.executeUpdate("UPDATE UKOL SET deadline = NULL WHERE deadline = '' OR TRIM(deadline) = ''");
            kontrolujAResetujVolnaProdlouzeni(conn);
        } catch (SQLException e) {
            System.out.println("Chyba pri praci s databazou: " + e.getMessage());
        }
    }

    private static void kontrolujAResetujVolnaProdlouzeni(Connection conn) throws SQLException {
        // Získání aktuálního roku a měsíce ve formátu "YYYY-MM" (např. "2026-06")
        String aktualniMesic = YearMonth.now().toString();
        String posledniResetMesic = null;

        // Načtení hodnoty, kdy byl reset proveden naposledy
        String selectSql = "SELECT hodnota FROM SYSTEM_CONFIG WHERE klic = 'posledni_reset_mesic'";
        try (PreparedStatement pstmt = conn.prepareStatement(selectSql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                posledniResetMesic = rs.getString("hodnota");
            }
        }

        // Pokud záznam neexistuje (první spuštění) nebo se aktuální měsíc liší od uloženého
        if (posledniResetMesic == null || !posledniResetMesic.equals(aktualniMesic)) {

            // 1. Resetování počtu volných prodloužení všem uživatelům na 3
            String updateUsersSql = "UPDATE ZAKAZNIK SET pocetVolnychProdlouzeni = 3";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(updateUsersSql);
            }

            // 2. Uložení/Aktualizace aktuálního měsíce jako měsíce posledního resetu
            String saveConfigSql = "INSERT INTO SYSTEM_CONFIG (klic, hodnota) VALUES ('posledni_reset_mesic', ?) " +
                    "ON CONFLICT(klic) DO UPDATE SET hodnota = excluded.hodnota";
            try (PreparedStatement pstmt = conn.prepareStatement(saveConfigSql)) {
                pstmt.setString(1, aktualniMesic);
                pstmt.executeUpdate();
            }

            System.out.println("Byl úspěšně proveden měsíční reset volných prodloužení deadlinu.");
        }
    }

    public static Optional<Pouzivatel> prihlasPouzivatela(String uzivatelskeJmeno, String heslo) {
        if (uzivatelskeJmeno == null || uzivatelskeJmeno.isBlank() || heslo == null) {
            return Optional.empty();
        }

        try {
            return nacitajPouzivatela(uzivatelskeJmeno.trim(), heslo);
        } catch (SQLException e) {
            System.out.println("Chyba pri prihlaseni: " + e.getMessage());
            return Optional.empty();
        }
    }

    public static Optional<Pouzivatel> registrujPouzivatela(String uzivatelskeJmeno, String heslo, String email) {
        if (uzivatelskeJmeno == null || uzivatelskeJmeno.isBlank() || heslo == null || heslo.isBlank()) {
            return Optional.empty();
        }

        String meno = uzivatelskeJmeno.trim();
        String emailHodnota = email == null ? null : email.trim();

        try {
            if (existujePouzivatel(meno)) {
                return Optional.empty();
            }

            int idZakaznika = vytvorPouzivatela(meno, heslo, emailHodnota);
            return nacitajPouzivatelaPodlaId(idZakaznika);
        } catch (SQLException e) {
            System.out.println("Chyba pri registracii: " + e.getMessage());
            return Optional.empty();
        }
    }

    public static Optional<Pouzivatel> nacitajAktualnehoPouzivatela() {
        return Optional.ofNullable(CurrentUserSession.getCurrentUser());
    }

    public static Optional<Pouzivatel> nacitajPouzivatelaPodlaId(int idZakaznika) {
        String sql = "SELECT z.id_zakaznik, z.uzivatelskeJmeno, z.heslo, z.email, z.xp, z.level, z.herniMena, " +
                "z.pocetVolnychProdlouzeni, z.pohlavie, COALESCE(n.notifikace, 1), COALESCE(n.notifikaceDeadline, 1), " +
                "COALESCE(n.notifikaceMesic, 1), COALESCE(n.notifikaceTyden, 1), COALESCE(n.lightMode, 0) " +
                "FROM ZAKAZNIK z LEFT JOIN NASTAVENI n ON n.id_zakaznik = z.id_zakaznik " +
                "WHERE z.id_zakaznik = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idZakaznika);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(nacitajZResultSetu(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Chyba pri nacitani pouzivatela: " + e.getMessage());
        }

        return Optional.empty();
    }

    private static Optional<Pouzivatel> nacitajPouzivatela(String uzivatelskeJmeno, String heslo) throws SQLException {
        String sql = "SELECT z.id_zakaznik, z.uzivatelskeJmeno, z.heslo, z.email, z.xp, z.level, z.herniMena, " +
                "z.pocetVolnychProdlouzeni, z.pohlavie, COALESCE(n.notifikace, 1), COALESCE(n.notifikaceDeadline, 1), " +
                "COALESCE(n.notifikaceMesic, 1), COALESCE(n.notifikaceTyden, 1), COALESCE(n.lightMode, 0) " +
                "FROM ZAKAZNIK z LEFT JOIN NASTAVENI n ON n.id_zakaznik = z.id_zakaznik " +
                "WHERE lower(z.uzivatelskeJmeno) = lower(?) AND z.heslo = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uzivatelskeJmeno);
            pstmt.setString(2, heslo);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(nacitajZResultSetu(rs));
                }
            }
        }

        return Optional.empty();
    }

    public static boolean ulozEmail(int idZakaznika, String email) {
        String sql = "UPDATE ZAKAZNIK SET email = ? WHERE id_zakaznik = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (email == null || email.isBlank()) {
                pstmt.setNull(1, java.sql.Types.VARCHAR);
            } else {
                pstmt.setString(1, email.trim());
            }
            pstmt.setInt(2, idZakaznika);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Chyba pri ukladaní emailu: " + e.getMessage());
            return false;
        }
    }

    public static AkciaVysledok ulozPohlavie(int idZakaznika, String pohlavie) {
        String hodnota = normalizujPohlavie(pohlavie);
        if (hodnota == null) {
            return new AkciaVysledok(false, "Vyber muz alebo zena.");
        }

        String sql = "UPDATE ZAKAZNIK SET pohlavie = ? WHERE id_zakaznik = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, hodnota);
            pstmt.setInt(2, idZakaznika);
            if (pstmt.executeUpdate() > 0) {
                prihlasAktualnehoPouzivatela(idZakaznika);
                return new AkciaVysledok(true, "Pohlavie bolo ulozene.");
            }
            return new AkciaVysledok(false, "Pohlavie sa nepodarilo ulozit.");
        } catch (SQLException e) {
            System.out.println("Chyba pri ukladani pohlavia: " + e.getMessage());
            return new AkciaVysledok(false, "Pohlavie sa nepodarilo ulozit.");
        }
    }

    public static boolean zmenHeslo(int idZakaznika, String stareHeslo, String noveHeslo) {
        if (noveHeslo == null || noveHeslo.isBlank()) {
            return false;
        }

        String sql = "UPDATE ZAKAZNIK SET heslo = ? WHERE id_zakaznik = ? AND heslo = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, noveHeslo);
            pstmt.setInt(2, idZakaznika);
            pstmt.setString(3, stareHeslo);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Chyba pri zmene hesla: " + e.getMessage());
            return false;
        }
    }

    public static boolean ulozNastavenia(int idZakaznika, boolean notifikace, boolean notifikaceDeadline,
                                         boolean notifikaceMesic, boolean notifikaceTyden, boolean lightMode) {
        String sql = "INSERT INTO NASTAVENI(id_zakaznik, notifikace, notifikaceDeadline, notifikaceMesic, notifikaceTyden, lightMode) " +
                "VALUES(?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(id_zakaznik) DO UPDATE SET " +
                "notifikace = excluded.notifikace, " +
                "notifikaceDeadline = excluded.notifikaceDeadline, " +
                "notifikaceMesic = excluded.notifikaceMesic, " +
                "notifikaceTyden = excluded.notifikaceTyden, " +
                "lightMode = excluded.lightMode";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idZakaznika);
            pstmt.setBoolean(2, notifikace);
            pstmt.setBoolean(3, notifikaceDeadline);
            pstmt.setBoolean(4, notifikaceMesic);
            pstmt.setBoolean(5, notifikaceTyden);
            pstmt.setBoolean(6, lightMode);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Chyba pri ukladaní nastavení: " + e.getMessage());
            return false;
        }
    }

    public static void pridejUkol(String nazev, String deadline, String popis, int xpOdmena, String obtiaznost) {
        String sql = "INSERT INTO UKOL(nazev, deadline, datumVytvoreni, popis, id_zakaznik, xp_odmena, obtiaznost) VALUES(?,?,date('now'),?,?,?,?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nazev);
            if (deadline == null || deadline.isBlank()) {
                pstmt.setNull(2, java.sql.Types.DATE);
            } else {
                pstmt.setString(2, deadline);
            }
            pstmt.setString(3, popis);
            pstmt.setInt(4, aktualneIdZakaznika());
            pstmt.setInt(5, xpOdmena);
            pstmt.setString(6, obtiaznost);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Chyba pri ukladani: " + e.getMessage());
        }
    }

    public static List<Uloha> nacitajUlohy() {
        List<Uloha> ulohy = new ArrayList<>();

        String sql = "SELECT id_ukol, nazev, deadline, stav, popis, xp_odmena, pocet_prodlouzeni, datumDokonceni, obtiaznost FROM UKOL WHERE id_zakaznik = ? ORDER BY " +
                "CASE WHEN stav = 'Dokoncena' THEN 1 ELSE 0 END, " +
                "CASE WHEN deadline IS NULL THEN 1 ELSE 0 END, deadline, id_ukol DESC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, aktualneIdZakaznika());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    LocalDate deadline = parseDeadline(rs.getString("deadline"));
                    LocalDate datumDokonceni = parseDeadline(rs.getString("datumDokonceni"));
                    Uloha nactenaUloha = new Uloha(
                            rs.getInt("id_ukol"),
                            rs.getString("nazev"),
                            deadline,
                            rs.getString("stav"),
                            rs.getString("popis"),
                            rs.getInt("xp_odmena"),
                            datumDokonceni,
                            rs.getString("obtiaznost") != null ? rs.getString("obtiaznost") : "Ľahká"
                    );
                    nactenaUloha.setPocetProdlouzeni(rs.getInt("pocet_prodlouzeni"));
                    ulohy.add(nactenaUloha);
                }
            }
        } catch (SQLException e) {
            System.out.println("Chyba pri nacitani uloh: " + e.getMessage());
        }

        return ulohy;
    }

    public static boolean oznacAkoDokoncenu(int idUlohy) {
        // 1. Získání údajů (tento blok máš správně)
        String selectSql = "SELECT xp_odmena, id_zakaznik, deadline FROM UKOL WHERE id_ukol = ?";
        int xpNaPripisanie = 0;
        int idZakaznika = -1;
        LocalDate deadline = null;

        try (Connection conn = getConnection();
             PreparedStatement selectPstmt = conn.prepareStatement(selectSql)) {
            selectPstmt.setInt(1, idUlohy);
            try (ResultSet rs = selectPstmt.executeQuery()) {
                if (rs.next()) {
                    xpNaPripisanie = rs.getInt("xp_odmena");
                    idZakaznika = rs.getInt("id_zakaznik");
                    deadline = parseDeadline(rs.getString("deadline"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Chyba pri zistovani dat: " + e.getMessage());
            return false;
        }

        if (idZakaznika == -1) return false;

        // 2. OPRAVA ZDE: Správná SQL syntaxe s WHERE
        String sql = "UPDATE UKOL SET stav = 'Dokoncena', datumDokonceni = date('now') WHERE id_ukol = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUlohy); // Nastavíme ID do otazníku
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Chyba pri dokonceni ulohy: " + e.getMessage());
            return false; // Přidáno pro lepší bezpečnost
        }

        return pripisXpPouzivatelovi(idZakaznika, xpNaPripisanie, deadline);
    }

    public static void predlzTermin(int idUlohy, LocalDate novyTermin) {
        String sql = "UPDATE UKOL SET deadline = ?, stav = CASE WHEN stav = 'Dokoncena' THEN stav ELSE 'Aktivna' END, xp_odmena = MAX(0, xp_odmena - 5) WHERE id_ukol = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, novyTermin.toString());
            pstmt.setInt(2, idUlohy);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Chyba pri predlzeni terminu: " + e.getMessage());
        }
    }


    public static AkciaVysledok plateneVykonajPredlzenie(int idZakaznika, int idUlohy, LocalDate novyTermin, int cena) {
        String sqlOdcitaj = "UPDATE ZAKAZNIK SET herniMena = herniMena - ? WHERE id_zakaznik = ? AND herniMena >= ?";
        String sqlPredlz  = "UPDATE UKOL SET deadline = ?, stav = CASE WHEN stav = 'Dokoncena' THEN stav ELSE 'Aktivna' END, xp_odmena = MAX(0, xp_odmena - 5) WHERE id_ukol = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps1 = conn.prepareStatement(sqlOdcitaj)) {
                ps1.setInt(1, cena);
                ps1.setInt(2, idZakaznika);
                ps1.setInt(3, cena);
                if (ps1.executeUpdate() == 0) {
                    conn.rollback();
                    return new AkciaVysledok(false, "Nemas dost hernej meny.");
                }
            }

            try (PreparedStatement ps2 = conn.prepareStatement(sqlPredlz)) {
                ps2.setString(1, novyTermin.toString());
                ps2.setInt(2, idUlohy);
                ps2.executeUpdate();
            }

            conn.commit();
            prihlasAktualnehoPouzivatela(idZakaznika);
            return new AkciaVysledok(true, "Termin predlzeny.");

        } catch (SQLException e) {
            return new AkciaVysledok(false, "Chyba: " + e.getMessage());
        }
    }

    public static List<SatnikItem> nacitajPredmetyObchodu() {
        List<SatnikItem> predmety = new ArrayList<>();
        String sql = "SELECT id_obleceni, nazev, COALESCE(popis, '') AS popis, cena, castTela FROM OBLECENI ORDER BY id_obleceni";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                predmety.add(new SatnikItem(
                        rs.getInt("id_obleceni"),
                        rs.getString("nazev"),
                        rs.getString("popis"),
                        rs.getInt("cena"),
                        rs.getString("castTela"),
                        obrazokPrePredmet(rs.getString("nazev"))
                ));
            }
        } catch (SQLException e) {
            System.out.println("Chyba pri nacitani predmetov obchodu: " + e.getMessage());
        }

        return predmety;
    }

    public static List<SatnikItem> nacitajPredmetyPouzivatela(int idZakaznika) {
        List<SatnikItem> predmety = new ArrayList<>();
        String sql = "SELECT o.id_obleceni, o.nazev, COALESCE(o.popis, '') AS popis, o.cena, o.castTela " +
                "FROM SATNIK_ITEM si " +
                "JOIN OBLECENI o ON o.id_obleceni = si.id_obleceni " +
                "WHERE si.id_zakaznik = ? ORDER BY o.id_obleceni";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idZakaznika);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    predmety.add(new SatnikItem(
                            rs.getInt("id_obleceni"),
                            rs.getString("nazev"),
                            rs.getString("popis"),
                            rs.getInt("cena"),
                            rs.getString("castTela"),
                            obrazokPrePredmet(rs.getString("nazev"))
                    ));
                }
            }
        } catch (SQLException e) {
            System.out.println("Chyba pri nacitani predmetov pouzivatela: " + e.getMessage());
        }

        return predmety;
    }

    public static Optional<SatnikItem> nacitajPredmetPodlaId(int idPredmetu) {
        String sql = "SELECT id_obleceni, nazev, COALESCE(popis, '') AS popis, cena, castTela FROM OBLECENI WHERE id_obleceni = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idPredmetu);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new SatnikItem(
                            rs.getInt("id_obleceni"),
                            rs.getString("nazev"),
                            rs.getString("popis"),
                            rs.getInt("cena"),
                            rs.getString("castTela"),
                            obrazokPrePredmet(rs.getString("nazev"))
                    ));
                }
            }
        } catch (SQLException e) {
            System.out.println("Chyba pri nacitani predmetu: " + e.getMessage());
        }

        return Optional.empty();
    }

    public static Optional<AvatarVyzor> nacitajAvatar(int idZakaznika) {
        String sql = "SELECT z.pohlavie, a.id_hlava, a.id_telo, a.id_nohy, a.id_boty " +
                "FROM ZAKAZNIK z LEFT JOIN AVATAR a ON a.id_zakaznik = z.id_zakaznik " +
                "WHERE z.id_zakaznik = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idZakaznika);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new AvatarVyzor(
                            rs.getString("pohlavie"),
                            rs.getObject("id_hlava") == null ? null : rs.getInt("id_hlava"),
                            rs.getObject("id_telo") == null ? null : rs.getInt("id_telo"),
                            rs.getObject("id_nohy") == null ? null : rs.getInt("id_nohy"),
                            rs.getObject("id_boty") == null ? null : rs.getInt("id_boty")
                    ));
                }
            }
        } catch (SQLException e) {
            System.out.println("Chyba pri nacitani avatara: " + e.getMessage());
        }

        return Optional.empty();
    }

    public static AkciaVysledok nakupPredmet(int idZakaznika, int idPredmetu) {
        String sqlUser = "SELECT herniMena FROM ZAKAZNIK WHERE id_zakaznik = ?";
        String sqlItem = "SELECT id_obleceni, nazev, cena FROM OBLECENI WHERE id_obleceni = ?";
        String sqlOwned = "SELECT 1 FROM SATNIK_ITEM WHERE id_zakaznik = ? AND id_obleceni = ? LIMIT 1";
        String sqlInsert = "INSERT INTO SATNIK_ITEM(id_zakaznik, id_obleceni) VALUES(?, ?)";
        String sqlSpend = "UPDATE ZAKAZNIK SET herniMena = herniMena - ? WHERE id_zakaznik = ?";
        String sqlEnsureAvatar = "INSERT OR IGNORE INTO AVATAR(id_zakaznik) VALUES(?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            int herniMena;
            try (PreparedStatement pstmt = conn.prepareStatement(sqlUser)) {
                pstmt.setInt(1, idZakaznika);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return new AkciaVysledok(false, "Pouzivatel sa nenasiel.");
                    }
                    herniMena = rs.getInt("herniMena");
                }
            }

            int cena;
            String nazov;
            try (PreparedStatement pstmt = conn.prepareStatement(sqlItem)) {
                pstmt.setInt(1, idPredmetu);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return new AkciaVysledok(false, "Predmet sa nenasiel.");
                    }
                    nazov = rs.getString("nazev");
                    cena = rs.getInt("cena");
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sqlOwned)) {
                pstmt.setInt(1, idZakaznika);
                pstmt.setInt(2, idPredmetu);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        conn.rollback();
                        return new AkciaVysledok(false, "Predmet uz mas kupeny.");
                    }
                }
            }

            if (herniMena < cena) {
                conn.rollback();
                return new AkciaVysledok(false, "Nemas dost hernej meny.");
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sqlEnsureAvatar)) {
                pstmt.setInt(1, idZakaznika);
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sqlSpend)) {
                pstmt.setInt(1, cena);
                pstmt.setInt(2, idZakaznika);
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {
                pstmt.setInt(1, idZakaznika);
                pstmt.setInt(2, idPredmetu);
                pstmt.executeUpdate();
            }

            conn.commit();
            prihlasAktualnehoPouzivatela(idZakaznika);
            return new AkciaVysledok(true, "Predmet '" + nazov + "' bol kupeny.");
        } catch (SQLException e) {
            System.out.println("Chyba pri kupe predmetu: " + e.getMessage());
            return new AkciaVysledok(false, "Predmet sa nepodarilo kupit.");
        }
    }

    public static AkciaVysledok vybavPredmet(int idZakaznika, int idPredmetu) {
        Optional<SatnikItem> predmetOpt = nacitajPredmetPodlaId(idPredmetu);
        if (predmetOpt.isEmpty()) {
            return new AkciaVysledok(false, "Predmet sa nenasiel.");
        }

        SatnikItem predmet = predmetOpt.get();
        if (!maPouzivatelPredmet(idZakaznika, idPredmetu)) {
            return new AkciaVysledok(false, "Najskor si predmet kup.");
        }

        String stlpec = mapujSlot(predmet.castTela());
        if (stlpec == null) {
            return new AkciaVysledok(false, "Predmet sa neda obliect.");
        }

        String konfliktnýStlpec = mapujKonfliktnýSlot(predmet.castTela());

        String sqlEnsureAvatar = "INSERT OR IGNORE INTO AVATAR(id_zakaznik) VALUES(?)";
        String sqlUpdate = "UPDATE AVATAR SET " + stlpec + " = ? WHERE id_zakaznik = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sqlEnsureAvatar)) {
                pstmt.setInt(1, idZakaznika);
                pstmt.executeUpdate();
            }

            if (konfliktnýStlpec != null) {
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE AVATAR SET " + konfliktnýStlpec + " = NULL WHERE id_zakaznik = ?")) {
                    pstmt.setInt(1, idZakaznika);
                    pstmt.executeUpdate();
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
                pstmt.setInt(1, idPredmetu);
                pstmt.setInt(2, idZakaznika);
                pstmt.executeUpdate();
            }

            conn.commit();
            prihlasAktualnehoPouzivatela(idZakaznika);
            return new AkciaVysledok(true, "Predmet '" + predmet.nazov() + "' bol obleceny.");
        } catch (SQLException e) {
            System.out.println("Chyba pri obliekani predmetu: " + e.getMessage());
            return new AkciaVysledok(false, "Predmet sa nepodarilo obliect.");
        }
    }

    public static AkciaVysledok vyzlecPredmet(int idZakaznika, int idPredmetu) {
        Optional<SatnikItem> predmetOpt = nacitajPredmetPodlaId(idPredmetu);
        if (predmetOpt.isEmpty()) {
            return new AkciaVysledok(false, "Predmet sa nenasiel.");
        }

        SatnikItem predmet = predmetOpt.get();
        String stlpec = mapujSlot(predmet.castTela());
        if (stlpec == null) {
            return new AkciaVysledok(false, "Predmet sa neda vyzliect.");
        }

        String sql = "UPDATE AVATAR SET " + stlpec + " = NULL WHERE id_zakaznik = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idZakaznika);
            pstmt.executeUpdate();
            prihlasAktualnehoPouzivatela(idZakaznika);
            return new AkciaVysledok(true, "Predmet '" + predmet.nazov() + "' bol vyzleceny.");
        } catch (SQLException e) {
            System.out.println("Chyba pri vyzliecani predmetu: " + e.getMessage());
            return new AkciaVysledok(false, "Predmet sa nepodarilo vyzliect.");
        }
    }

    private static boolean existujePouzivatel(String uzivatelskeJmeno) throws SQLException {
        String sql = "SELECT 1 FROM ZAKAZNIK WHERE lower(uzivatelskeJmeno) = lower(?) LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uzivatelskeJmeno);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static int vytvorPouzivatela(String uzivatelskeJmeno, String heslo, String email) throws SQLException {
        String insertZakaznik = "INSERT INTO ZAKAZNIK(uzivatelskeJmeno, heslo, email, xp, level, herniMena, pocetVolnychProdlouzeni) " +
                "VALUES(?, ?, ?, 0, 1, 0, 3)";
        String insertNastaveni = "INSERT INTO NASTAVENI(id_zakaznik, notifikace, notifikaceDeadline, notifikaceMesic, notifikaceTyden, lightMode) " +
                "VALUES(?, 1, 1, 1, 1, 0)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement insertZakaznikStatement = conn.prepareStatement(insertZakaznik, Statement.RETURN_GENERATED_KEYS)) {
                insertZakaznikStatement.setString(1, uzivatelskeJmeno);
                insertZakaznikStatement.setString(2, heslo);
                if (email == null || email.isBlank()) {
                    insertZakaznikStatement.setNull(3, java.sql.Types.VARCHAR);
                } else {
                    insertZakaznikStatement.setString(3, email);
                }
                insertZakaznikStatement.executeUpdate();

                int generatedId;
                try (ResultSet generatedKeys = insertZakaznikStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        generatedId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Nepodarilo sa ziskat ID noveho pouzivatela.");
                    }
                }

                try (PreparedStatement insertNastaveniStatement = conn.prepareStatement(insertNastaveni)) {
                    insertNastaveniStatement.setInt(1, generatedId);
                    insertNastaveniStatement.executeUpdate();
                }

                conn.commit();
                return generatedId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private static Pouzivatel nacitajZResultSetu(ResultSet rs) throws SQLException {
        return new Pouzivatel(
                rs.getInt(1),
                rs.getString(2),
                rs.getString(3),
                rs.getString(4),
                rs.getInt(5),
                rs.getInt(6),
                rs.getInt(7),
                rs.getInt(8),
                rs.getString(9),
                rs.getInt(10) != 0,
                rs.getInt(11) != 0,
                rs.getInt(12) != 0,
                rs.getInt(13) != 0,
                rs.getInt(14) != 0
        );
    }

    private static LocalDate parseDeadline(String rawDeadline) {
        if (rawDeadline == null || rawDeadline.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(rawDeadline.trim());
        } catch (DateTimeParseException e) {
            System.out.println("Neplatny deadline v databaze: " + rawDeadline);
            return null;
        }
    }

    private static int aktualneIdZakaznika() {
        Pouzivatel aktualny = CurrentUserSession.getCurrentUser();
        if (aktualny != null) {
            return aktualny.getId();
        }
        return 1;
    }

    private static void pridajStlpecAkChyba(Connection conn, String tabulka, String stlpec, String definicia) {
        try {
            if (maStlpec(conn, tabulka, stlpec)) {
                return;
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE " + tabulka + " ADD COLUMN " + stlpec + " " + definicia);
            }
        } catch (SQLException e) {
            System.out.println("Nepodarilo sa doplnit stlpec " + stlpec + " v tabulke " + tabulka + ": " + e.getMessage());
        }
    }

    private static boolean maStlpec(Connection conn, String tabulka, String stlpec) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, tabulka, stlpec)) {
            return rs.next();
        }
    }

    private static void zaktivujZakladnyShop(Connection conn) throws SQLException {
        String[][] predmety = {
                {"Klobúk", "Klasicky elegantny klobuk.", "120", "HLAVA_KLOBUK"},
                {"Okuliare", "Stylove okuliare do kazdej situacie.", "90", "TVAR_OKULIARE"},
                {"Šiltovka", "Cervena siltovka pre sportovy look.", "110", "HLAVA_SILTOVKA"},
                {"Náhrdelník", "Elegantny nahrdelnik, ktory doladi kazdy outfit.", "160", "KRK_NAHRDELNIK"}
        };

        for (String[] predmet : predmety) {
            zaktivujPredmetAkChyba(conn, predmet[0], predmet[1], Integer.parseInt(predmet[2]), predmet[3]);
        }
    }

    private static void zaktivujPredmetAkChyba(Connection conn, String nazev, String popis, int cena, String castTela)
            throws SQLException {
        if (maPredmet(conn, nazev)) {
            return;
        }

        String sql = "INSERT INTO OBLECENI(nazev, popis, cena, castTela) " +
                "SELECT ?, ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM OBLECENI WHERE lower(nazev) = lower(?))";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nazev);
            pstmt.setString(2, popis);
            pstmt.setInt(3, cena);
            pstmt.setString(4, castTela);
            pstmt.setString(5, nazev);
            pstmt.executeUpdate();
        }
    }

    private static boolean maPredmet(Connection conn, String nazev) throws SQLException {
        String normalizovanyCiel = nazovBezDiakritiky(nazev).trim().toLowerCase();
        String sql = "SELECT nazev FROM OBLECENI";

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String existujuci = rs.getString("nazev");
                if (nazovBezDiakritiky(existujuci).trim().toLowerCase().equals(normalizovanyCiel)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static String obrazokPrePredmet(String nazev) {
        String hladanyNazov = nazovBezDiakritiky(nazev).trim().toLowerCase();

        if (hladanyNazov.contains("klobuk")) {
            return "/FXML_files/klobuk.png";
        }
        if (hladanyNazov.contains("okuliare")) {
            return "/FXML_files/okuliare.png";
        }
        if (hladanyNazov.contains("siltovka")) {
            return "/FXML_files/siltovka_cervena.png";
        }
        if (hladanyNazov.contains("nahrdelnik")) {
            return "/FXML_files/nahrdelnik.png";
        }
        return "/FXML_files/muz_basic.png";
    }

    private static String mapujSlot(String castTela) {
        if (castTela == null) {
            return null;
        }

        return switch (castTela.trim().toUpperCase()) {
            case "HLAVA_KLOBUK" -> "id_hlava";
            case "TVAR_OKULIARE" -> "id_telo";
            case "HLAVA_SILTOVKA" -> "id_nohy";
            case "KRK_NAHRDELNIK" -> "id_boty";
            default -> null;
        };
    }

    private static String mapujKonfliktnýSlot(String castTela) {
        if (castTela == null) return null;
        return switch (castTela.trim().toUpperCase()) {
            case "HLAVA_KLOBUK" -> "id_nohy";   // vyzlece siltovku
            case "HLAVA_SILTOVKA" -> "id_hlava"; // vyzlece klobuk
            default -> null;
        };
    }

    private static boolean maPouzivatelPredmet(int idZakaznika, int idPredmetu) {
        String sql = "SELECT 1 FROM SATNIK_ITEM WHERE id_zakaznik = ? AND id_obleceni = ? LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idZakaznika);
            pstmt.setInt(2, idPredmetu);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Chyba pri overovani vlastnictva predmetu: " + e.getMessage());
            return false;
        }
    }

    private static void prihlasAktualnehoPouzivatela(int idZakaznika) {
        nacitajPouzivatelaPodlaId(idZakaznika).ifPresent(CurrentUserSession::setCurrentUser);
    }

    private static String normalizujPohlavie(String pohlavie) {
        if (pohlavie == null) {
            return null;
        }

        String normalized = nazovBezDiakritiky(pohlavie).trim().toLowerCase();
        if (normalized.equals("muz") || normalized.equals("m")) {
            return "MUZ";
        }
        if (normalized.equals("zena") || normalized.equals("zenske") || normalized.equals("z")) {
            return "ZENA";
        }
        return null;
    }

    private static String nazovBezDiakritiky(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace('á', 'a')
                .replace('ä', 'a')
                .replace('č', 'c')
                .replace('ď', 'd')
                .replace('é', 'e')
                .replace('ě', 'e')
                .replace('í', 'i')
                .replace('ĺ', 'l')
                .replace('ľ', 'l')
                .replace('ň', 'n')
                .replace('ó', 'o')
                .replace('ô', 'o')
                .replace('ŕ', 'r')
                .replace('š', 's')
                .replace('ť', 't')
                .replace('ú', 'u')
                .replace('ý', 'y')
                .replace('ž', 'z')
                .replace('Á', 'A')
                .replace('Ä', 'A')
                .replace('Č', 'C')
                .replace('Ď', 'D')
                .replace('É', 'E')
                .replace('Í', 'I')
                .replace('Ĺ', 'L')
                .replace('Ľ', 'L')
                .replace('Ň', 'N')
                .replace('Ó', 'O')
                .replace('Ô', 'O')
                .replace('Ŕ', 'R')
                .replace('Š', 'S')
                .replace('Ť', 'T')
                .replace('Ú', 'U')
                .replace('Ý', 'Y')
                .replace('Ž', 'Z');
    }
    public static void snizPocetVolnychProdlouzeni(int idZakaznika) {
        String sql = "UPDATE ZAKAZNIK SET pocetVolnychProdlouzeni = pocetVolnychProdlouzeni - 1 WHERE id_zakaznik = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idZakaznika);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Chyba při snižování volných prodloužení: " + e.getMessage());
        }
    }


    private static boolean pripisXpPouzivatelovi(int idZakaznika, int pridaneXp, LocalDate deadline) {
        long dnyZpozdeni = 0;
        if (deadline != null && LocalDate.now().isAfter(deadline)) {
            dnyZpozdeni = ChronoUnit.DAYS.between(deadline, LocalDate.now());
        }

        int penalizaceXp = (int) (dnyZpozdeni * 2);
        int finalniPridaneXp = Math.max(0, pridaneXp - penalizaceXp);

        int penalizaceMena = (int) Math.min(10, dnyZpozdeni * 2);

        String selectSql = "SELECT xp, level, herniMena FROM ZAKAZNIK WHERE id_zakaznik = ?";
        int aktualneXp = 0;
        int aktualnyLevel = 1;
        int aktualnaMena = 0;

        try (Connection conn = getConnection();
             PreparedStatement selectPstmt = conn.prepareStatement(selectSql)) {
            selectPstmt.setInt(1, idZakaznika);
            try (ResultSet rs = selectPstmt.executeQuery()) {
                if (rs.next()) {
                    aktualneXp = rs.getInt("xp");
                    aktualnyLevel = rs.getInt("level");
                    aktualnaMena = rs.getInt("herniMena");
                }
            }
        } catch (SQLException e) {
            System.out.println("Chyba pri nacitani zakaznika: " + e.getMessage());
            return false;
        }

        aktualnaMena = Math.max(0, aktualnaMena - penalizaceMena);

        int noveXp = aktualneXp + finalniPridaneXp;
        boolean levelUp = false;


        while (noveXp >= aktualnyLevel * 100) {
            aktualnyLevel++;
            aktualnaMena += aktualnyLevel * 50;
            levelUp = true;
        }

        String updateSql = "UPDATE ZAKAZNIK SET xp = ?, level = ?, herniMena = ? WHERE id_zakaznik = ?";
        try (Connection conn = getConnection();
             PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {
            updatePstmt.setInt(1, noveXp);
            updatePstmt.setInt(2, aktualnyLevel);
            updatePstmt.setInt(3, aktualnaMena);
            updatePstmt.setInt(4, idZakaznika);
            updatePstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Chyba pri ukladani xp a levelu: " + e.getMessage());
        }

        Optional<Pouzivatel> obnoveny = nacitajPouzivatelaPodlaId(idZakaznika);
        obnoveny.ifPresent(CurrentUserSession::setCurrentUser);

        return levelUp;
    }

    public static int[] ziskajStatistikyUspesnosti(int idZakaznika) {
        String sql = "SELECT " +
                "SUM(CASE WHEN stav = 'Dokoncena' AND datumDokonceni <= deadline THEN 1 ELSE 0 END) AS hotovo, " +
                "SUM(CASE WHEN stav = 'Dokoncena' OR (deadline IS NOT NULL AND deadline < date('now')) THEN 1 ELSE 0 END) AS celkem " +
                "FROM UKOL WHERE id_zakaznik = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idZakaznika);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new int[]{rs.getInt("hotovo"), rs.getInt("celkem")};
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new int[]{0, 0};
    }
    public static List<Map<String, Object>> ziskajStatistikyProGraf(int idZakaznika) {
        List<Map<String, Object>> data = new ArrayList<>();


        String sql = "SELECT datumDokonceni, " +
                "SUM(CASE WHEN datumDokonceni <= deadline THEN 1 ELSE 0 END) as vcas, " +
                "SUM(CASE WHEN datumDokonceni > deadline THEN 1 ELSE 0 END) as pozde " +
                "FROM UKOL " +
                "WHERE id_zakaznik = ? AND stav = 'Dokoncena' " +
                "AND datumDokonceni >= date('now', '-10 days') " +
                "GROUP BY datumDokonceni " +
                "ORDER BY datumDokonceni ASC LIMIT 10";

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idZakaznika);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> entry = new java.util.HashMap<>();
                entry.put("datum", rs.getString("datumDokonceni"));
                entry.put("vcas", rs.getInt("vcas"));
                entry.put("pozde", rs.getInt("pozde"));
                data.add(entry);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return data;
    }
    public static List<Map<String, Object>> ziskajStatistikyPerTyzden(int idZakaznika) {
        String sql = "SELECT * FROM (" +
                "  SELECT strftime('%Y-W%W', datumDokonceni) AS tyzden," +
                "    SUM(CASE WHEN datumDokonceni <= deadline THEN 1 ELSE 0 END) AS vcas," +
                "    SUM(CASE WHEN datumDokonceni > deadline THEN 1 ELSE 0 END) AS pozde" +
                "  FROM UKOL" +
                "  WHERE id_zakaznik = ? AND stav = 'Dokoncena' AND datumDokonceni IS NOT NULL" +
                "  GROUP BY tyzden ORDER BY tyzden DESC LIMIT 8" +
                ") ORDER BY tyzden ASC";
        List<Map<String, Object>> data = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idZakaznika);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> entry = new java.util.HashMap<>();
                entry.put("tyzden", rs.getString("tyzden"));
                entry.put("vcas", rs.getInt("vcas"));
                entry.put("pozde", rs.getInt("pozde"));
                data.add(entry);
            }
        } catch (SQLException e) {
            System.out.println("Chyba pri nacitani statistik per tyzden: " + e.getMessage());
        }
        return data;
    }

    public static Map<String, Integer> ziskajRozlozenieObtiaznosti(int idZakaznika) {
        String sql = "SELECT obtiaznost, COUNT(*) AS pocet FROM UKOL" +
                " WHERE id_zakaznik = ? AND stav = 'Dokoncena' AND obtiaznost IS NOT NULL" +
                " GROUP BY obtiaznost";
        Map<String, Integer> result = new java.util.HashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idZakaznika);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                result.put(rs.getString("obtiaznost"), rs.getInt("pocet"));
            }
        } catch (SQLException e) {
            System.out.println("Chyba pri nacitani rozlozenia obtiaznosti: " + e.getMessage());
        }
        return result;
    }

    public static String zistiTopDen(int idZakaznika) {
        String sql = "SELECT strftime('%w', datumDokonceni) AS denVTydnu, COUNT(*) as pocet " +
                "FROM UKOL " +
                "WHERE id_zakaznik = ? AND stav = 'Dokoncena' AND datumDokonceni IS NOT NULL " +
                "GROUP BY denVTydnu " +
                "ORDER BY pocet DESC LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idZakaznika);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("denVTydnu"); // Vrátí textově "0" až "6"
            }
        } catch (SQLException e) {
            System.out.println("Chyba při zjišťování top dne: " + e.getMessage());
        }
        return null;
    }
}
