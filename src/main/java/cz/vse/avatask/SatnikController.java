package cz.vse.avatask;

import cz.vse.avatask.dao.DatabaseManager;
import cz.vse.avatask.model.AkciaVysledok;
import cz.vse.avatask.model.Pouzivatel;
import cz.vse.avatask.model.SatnikItem;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class SatnikController {
    private static final String ULOHY_FXML = "/FXML_files/ulohy.fxml";
    private static final String PROFIL_FXML = "/FXML_files/profil.fxml";

    @FXML
    private TilePane obchodTilePane;

    @FXML
    private TilePane satnikTilePane;

    @FXML
    private StackPane avatarStackPane;

    @FXML
    private ImageView avatarBaseImageView;

    @FXML
    private ImageView avatarKabatImageView;

    @FXML
    private ImageView avatarSiltovkaImageView;

    @FXML
    private ImageView avatarKlobukImageView;

    @FXML
    private ImageView avatarOkuliareImageView;

    @FXML
    private Label menoLabel;

    @FXML
    private Label xpLabel;

    @FXML
    private Label levelLabel;

    @FXML
    private Label menaLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private void initialize() {
        obnovVsetko();
    }

    @FXML
    private void obnovAvatar(ActionEvent event) {
        obnovAvatar();
    }

    @FXML
    private void otvorProfil(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(PROFIL_FXML));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("Avatask - Profil");
        stage.setScene(new Scene(root));
        stage.show();
    }

    @FXML
    private void otvorUlohy(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(ULOHY_FXML));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("Avatask - Ulohy");
        stage.setScene(new Scene(root));
        stage.show();
    }

    private void obnovVsetko() {
        Optional<Pouzivatel> pouzivatelOpt = DatabaseManager.nacitajAktualnehoPouzivatela();
        if (pouzivatelOpt.isEmpty()) {
            zobrazStatus("Najprv sa prihlas.");
            return;
        }

        Pouzivatel pouzivatel = pouzivatelOpt.get();
        if (menoLabel != null) {
            menoLabel.setText(pouzivatel.getUzivatelskeMeno());
        }
        if (xpLabel != null) {
            xpLabel.setText("XP: " + pouzivatel.getXp());
        }
        if (levelLabel != null) {
            levelLabel.setText("Level " + pouzivatel.getLevel());
        }
        if (menaLabel != null) {
            menaLabel.setText("Mena: " + pouzivatel.getHerniMena());
        }

        obnovAvatar();
        obnovObchod();
        obnovSatnik();
    }

    private void obnovObchod() {
        if (obchodTilePane == null) {
            return;
        }

        obchodTilePane.getChildren().clear();
        List<SatnikItem> predmety = DatabaseManager.nacitajPredmetyObchodu();
        for (SatnikItem predmet : predmety) {
            obchodTilePane.getChildren().add(vyrobKartuPredmetu(predmet, true));
        }
    }

    private void obnovSatnik() {
        if (satnikTilePane == null) {
            return;
        }

        satnikTilePane.getChildren().clear();
        Optional<Pouzivatel> pouzivatelOpt = DatabaseManager.nacitajAktualnehoPouzivatela();
        if (pouzivatelOpt.isEmpty()) {
            return;
        }

        List<SatnikItem> vlastnene = DatabaseManager.nacitajPredmetyPouzivatela(pouzivatelOpt.get().getId());
        for (SatnikItem predmet : vlastnene) {
            satnikTilePane.getChildren().add(vyrobKartuPredmetu(predmet, false));
        }
    }

    private VBox vyrobKartuPredmetu(SatnikItem predmet, boolean obchod) {
        ImageView imageView = new ImageView(AvatarRenderer.nacitajObrazok(predmet.obrazok()));
        imageView.setFitWidth(120);
        imageView.setFitHeight(120);
        imageView.setPreserveRatio(true);

        Label nazovLabel = new Label(predmet.nazov());
        nazovLabel.setWrapText(true);
        nazovLabel.setAlignment(Pos.CENTER);
        nazovLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #6A040F; -fx-font-size: 13px;");

        Label popisLabel = new Label(predmet.popis() == null || predmet.popis().isBlank()
                ? ""
                : predmet.popis().trim());
        popisLabel.setWrapText(true);
        popisLabel.setAlignment(Pos.CENTER);
        popisLabel.setStyle("-fx-text-fill: #7a5960; -fx-font-size: 11px;");

        Label cenaLabel = new Label("Cena: " + predmet.cena());
        cenaLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #6A040F;");

        Button akciaButton = new Button(obchod ? "Kupit" : "Obliect");
        akciaButton.setMaxWidth(Double.MAX_VALUE);
        akciaButton.setStyle("-fx-background-color: #6A040F; -fx-text-fill: white; -fx-font-weight: bold;");
        akciaButton.setOnAction(event -> {
            AkciaVysledok vysledok = obchod
                    ? DatabaseManager.nakupPredmet(aktualneIdPouzivatela(), predmet.id())
                    : DatabaseManager.vybavPredmet(aktualneIdPouzivatela(), predmet.id());

            if (vysledok.uspesne()) {
                zobrazInfo(vysledok.sprava());
                obnovVsetko();
            } else {
                zobrazChybu(vysledok.sprava());
            }
        });

        VBox karta = new VBox(8, imageView, nazovLabel, popisLabel, cenaLabel, akciaButton);
        karta.setAlignment(Pos.CENTER);
        karta.setPadding(new Insets(12));
        karta.setPrefWidth(180);
        karta.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #fff8f8, #ffffff);" +
                        "-fx-border-color: #d8a2ab;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 16;" +
                        "-fx-background-radius: 16;"
        );
        return karta;
    }

    private void obnovAvatar() {
        Optional<Pouzivatel> pouzivatelOpt = DatabaseManager.nacitajAktualnehoPouzivatela();
        if (pouzivatelOpt.isEmpty()) {
            return;
        }

        AvatarRenderer.vykresliAvatar(avatarBaseImageView, avatarKabatImageView, avatarSiltovkaImageView,
                avatarKlobukImageView, avatarOkuliareImageView, pouzivatelOpt.get());
    }

    private int aktualneIdPouzivatela() {
        Pouzivatel pouzivatel = CurrentUserSession.getCurrentUser();
        return pouzivatel == null ? 1 : pouzivatel.getId();
    }

    private void zobrazStatus(String sprava) {
        if (statusLabel != null) {
            statusLabel.setText(sprava);
        }
    }

    private void zobrazInfo(String sprava) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informacia");
        alert.setHeaderText(null);
        alert.setContentText(sprava);
        alert.showAndWait();
    }

    private void zobrazChybu(String sprava) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Chyba");
        alert.setHeaderText(null);
        alert.setContentText(sprava);
        alert.showAndWait();
    }
}
