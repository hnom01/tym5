package cz.vse.avatask.dao;

import cz.vse.avatask.CurrentUserSession;
import cz.vse.avatask.model.Pouzivatel;
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
import java.util.Optional;

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
                    "pocetVolnychProdlouzeni INTEGER DEFAULT 3" +
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

            stmt.executeUpdate("UPDATE UKOL SET deadline = NULL WHERE deadline = '' OR TRIM(deadline) = ''");
        } catch (SQLException e) {
            System.out.println("Chyba pri praci s databazou: " + e.getMessage());
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
                "z.pocetVolnychProdlouzeni, COALESCE(n.notifikace, 1), COALESCE(n.notifikaceDeadline, 1), " +
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
                "z.pocetVolnychProdlouzeni, COALESCE(n.notifikace, 1), COALESCE(n.notifikaceDeadline, 1), " +
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

    public static void pridejUkol(String nazev, String deadline, String popis) {
        String sql = "INSERT INTO UKOL(nazev, deadline, datumVytvoreni, popis, id_zakaznik) VALUES(?,?,date('now'),?,?)";

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
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Chyba pri ukladani: " + e.getMessage());
        }
    }

    public static List<Uloha> nacitajUlohy() {
        List<Uloha> ulohy = new ArrayList<>();
        String sql = "SELECT id_ukol, nazev, deadline, stav, popis FROM UKOL WHERE id_zakaznik = ? ORDER BY " +
                "CASE WHEN stav = 'Dokoncena' THEN 1 ELSE 0 END, " +
                "CASE WHEN deadline IS NULL THEN 1 ELSE 0 END, deadline, id_ukol DESC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, aktualneIdZakaznika());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    LocalDate deadline = parseDeadline(rs.getString("deadline"));
                    ulohy.add(new Uloha(
                            rs.getInt("id_ukol"),
                            rs.getString("nazev"),
                            deadline,
                            rs.getString("stav"),
                            rs.getString("popis")
                    ));
                }
            }
        } catch (SQLException e) {
            System.out.println("Chyba pri nacitani uloh: " + e.getMessage());
        }

        return ulohy;
    }

    public static void oznacAkoDokoncenu(int idUlohy) {
        String sql = "UPDATE UKOL SET stav = 'Dokoncena' WHERE id_ukol = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUlohy);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Chyba pri dokonceni ulohy: " + e.getMessage());
        }
    }

    public static void predlzTermin(int idUlohy, LocalDate novyTermin) {
        String sql = "UPDATE UKOL SET deadline = ?, stav = CASE WHEN stav = 'Dokoncena' THEN stav ELSE 'Aktivna' END WHERE id_ukol = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, novyTermin.toString());
            pstmt.setInt(2, idUlohy);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Chyba pri predlzeni terminu: " + e.getMessage());
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
                rs.getInt(9) != 0,
                rs.getInt(10) != 0,
                rs.getInt(11) != 0,
                rs.getInt(12) != 0,
                rs.getInt(13) != 0
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
}
