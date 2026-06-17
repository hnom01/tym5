package cz.vse.avatask;

import cz.vse.avatask.dao.DatabaseManager;
import cz.vse.avatask.model.AvatarVyzor;
import cz.vse.avatask.model.Pouzivatel;
import cz.vse.avatask.model.SatnikItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Spolocne vykreslenie navrstveneho avatara (zakladna postava + vybavene oblecenie),
 * pouzivane v SatnikController aj UlohyController, aby boli obe obrazovky vzdy v zhode.
 */
public final class AvatarRenderer {

    private static final String AVATAR_MUZ = "/FXML_files/muz_basic.png";
    private static final String AVATAR_ZENA = "/FXML_files/zena_basic.png";
    private static final double AVATAR_FIT_WIDTH = 220.0;
    private static final double AVATAR_FIT_HEIGHT = 310.0;

    private AvatarRenderer() {
    }

    /**
     * Velkost a posun prekryvajuceho obrazka oblecenia voci stredu avatara.
     */
    private record AvatarUmiestnenie(double fitWidth, double fitHeight, double translateX, double translateY) {
        private static final AvatarUmiestnenie PREDVOLENE =
                new AvatarUmiestnenie(AVATAR_FIT_WIDTH, AVATAR_FIT_HEIGHT, 0, 0);
    }

    /**
     * Umiestnenie kazdeho typu oblecenia (castTela z OBLECENI) podla pohlavia avatara.
     * Pre novy typ oblecenia staci doplnit dalsi riadok s offsetmi pre MUZ/ZENA -
     * ak chyba zaznam alebo pohlavie, pouzije sa AvatarUmiestnenie.PREDVOLENE / MUZ offset.
     */
    private static final Map<String, Map<String, AvatarUmiestnenie>> UMIESTNENIA_OBLECENIA = Map.of(
            "HLAVA_KLOBUK", Map.of(
                    "MUZ", new AvatarUmiestnenie(55, 55, 0, -86),
                    "ZENA", new AvatarUmiestnenie(55, 55, 0, -86)
            ),
            "HLAVA_SILTOVKA", Map.of(
                    "MUZ", new AvatarUmiestnenie(40, 40, 0, -84),
                    "ZENA", new AvatarUmiestnenie(43, 43, 0, -85)
            ),
            "TVAR_OKULIARE", Map.of(
                    "MUZ", new AvatarUmiestnenie(30, 30, 0, -72),
                    "ZENA", new AvatarUmiestnenie(33, 33, 0, -70)
            ),
            "KRK_NAHRDELNIK", Map.of(
                    "MUZ", new AvatarUmiestnenie(29, 29, 0, -36),
                    "ZENA", new AvatarUmiestnenie(29, 29, 0, -36)
            )
    );

    /**
     * Vykresli zakladnu postavu a vsetky vybavene predmety pre dany ImageView-set.
     * Ktorykolvek z ImageView parametrov moze byt null, ak dana obrazovka danu vrstvu nema.
     */
    public static void vykresliAvatar(ImageView base, ImageView nahrdelnik, ImageView siltovka,
                                       ImageView klobuk, ImageView okuliare, Pouzivatel pouzivatel) {
        if (pouzivatel == null) {
            return;
        }

        String pohlavie = normalizujPohlavie(pouzivatel.getPohlavie());
        AvatarVyzor avatar = DatabaseManager.nacitajAvatar(pouzivatel.getId()).orElse(null);

        if (base != null) {
            base.setImage(nacitajObrazok(spravnyZaklad(pohlavie)));
        }
        nastavPrekrytie(nahrdelnik, avatar == null ? null : avatar.idBoty(), pohlavie);
        nastavPrekrytie(siltovka, avatar == null ? null : avatar.idNohy(), pohlavie);
        nastavPrekrytie(klobuk, avatar == null ? null : avatar.idHlava(), pohlavie);
        nastavPrekrytie(okuliare, avatar == null ? null : avatar.idTelo(), pohlavie);
    }

    private static void nastavPrekrytie(ImageView ciel, Integer idPredmetu, String pohlavie) {
        if (ciel == null) {
            return;
        }

        if (idPredmetu == null) {
            ciel.setImage(null);
            aplikujUmiestnenie(ciel, AvatarUmiestnenie.PREDVOLENE);
            return;
        }

        Optional<SatnikItem> predmet = DatabaseManager.nacitajPredmetPodlaId(idPredmetu);
        if (predmet.isEmpty()) {
            ciel.setImage(null);
            aplikujUmiestnenie(ciel, AvatarUmiestnenie.PREDVOLENE);
            return;
        }

        ciel.setImage(nacitajObrazok(predmet.get().obrazok()));
        aplikujUmiestnenie(ciel, najdiUmiestnenie(predmet.get().castTela(), pohlavie));
    }

    private static AvatarUmiestnenie najdiUmiestnenie(String castTela, String pohlavie) {
        Map<String, AvatarUmiestnenie> podlaPohlavia = UMIESTNENIA_OBLECENIA.get(castTela);
        if (podlaPohlavia == null) {
            return AvatarUmiestnenie.PREDVOLENE;
        }

        String kluc = pohlavie == null ? "MUZ" : pohlavie;
        return podlaPohlavia.getOrDefault(kluc, AvatarUmiestnenie.PREDVOLENE);
    }

    private static void aplikujUmiestnenie(ImageView ciel, AvatarUmiestnenie umiestnenie) {
        ciel.setFitWidth(umiestnenie.fitWidth());
        ciel.setFitHeight(umiestnenie.fitHeight());
        ciel.setTranslateX(umiestnenie.translateX());
        ciel.setTranslateY(umiestnenie.translateY());
    }

    /**
     * Nacita obrazok zo zadanej cesty v resources, alebo zakladnu postavu muza ako fallback.
     */
    public static Image nacitajObrazok(String resourcePath) {
        java.net.URL imageUrl = AvatarRenderer.class.getResource(resourcePath);

        if (imageUrl == null) {
            System.out.println("VAROVANIE: Nenašiel sa obrázok na ceste: " + resourcePath);
            return new Image(Objects.requireNonNull(AvatarRenderer.class.getResource(AVATAR_MUZ)).toExternalForm());
        }

        return new Image(imageUrl.toExternalForm());
    }

    private static String spravnyZaklad(String pohlavie) {
        return switch (pohlavie == null ? "NEURCENE" : pohlavie) {
            case "MUZ" -> AVATAR_MUZ;
            case "ZENA" -> AVATAR_ZENA;
            default -> AVATAR_MUZ;
        };
    }

    private static String normalizujPohlavie(String pohlavie) {
        if (pohlavie == null) {
            return null;
        }

        String normalized = pohlavie.trim().toUpperCase();
        if ("MUZ".equals(normalized) || "M".equals(normalized)) {
            return "MUZ";
        }
        if ("ZENA".equals(normalized) || "Z".equals(normalized)) {
            return "ZENA";
        }
        return null;
    }
}
