package cz.vse.avatask;

import cz.vse.avatask.dao.DatabaseManager;
import cz.vse.avatask.model.Pouzivatel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class ProfilController {
    private static final String LOGIN_FXML = "/FXML_files/login.fxml";

    @FXML
    private Label menoPouzivatelaLabel;

    @FXML
    private Label levelLabel;

    @FXML
    private Label xpLabel;

    @FXML
    private TextField emailField;

    @FXML
    private Label hesloMaskLabel;

    @FXML
    private TextField hesloVisibleField;

    @FXML
    private ToggleButton hesloToggleButton;

    @FXML
    private CheckBox emailNotifikacieCheckBox;

    @FXML
    private CheckBox statistickeUdajeCheckBox;

    @FXML
    private ToggleButton tmavyRezimToggle;

    @FXML
    private Label statusLabel;

    private int aktualnyPouzivatelId = -1;
    private String aktualneHeslo = "";
    private boolean hesloViditelne;

    @FXML
    private void initialize() {
        nacitajProfil();
    }

    @FXML
    private void prepniHesloZobrazenie() {
        hesloViditelne = hesloToggleButton.isSelected();
        aktualizujHesloZobrazenie();
        zobrazStatus(hesloViditelne ? "Heslo je zobrazené." : "Heslo je skryté.");
    }

    @FXML
    private void ulozEmail() {
        if (aktualnyPouzivatelId < 0) {
            return;
        }

        boolean uspesne = DatabaseManager.ulozEmail(aktualnyPouzivatelId, emailField.getText());
        if (uspesne) {
            obnovAktualnehoPouzivatela();
            zobrazStatus("Email uložený.");
            zobrazInfo("Email bol úspešne uložený.");
        } else {
            zobrazChybu("Email sa nepodarilo uložiť.");
        }
    }

    @FXML
    private void ulozNastavenia() {
        if (aktualnyPouzivatelId < 0) {
            return;
        }

        Pouzivatel aktualny = DatabaseManager.nacitajAktualnehoPouzivatela().orElse(null);
        boolean notifikaceMesic = aktualny != null && aktualny.isNotifikaceMesic();
        boolean notifikaceTyden = aktualny != null && aktualny.isNotifikaceTyden();

        boolean uspesne = DatabaseManager.ulozNastavenia(
                aktualnyPouzivatelId,
                emailNotifikacieCheckBox.isSelected(),
                statistickeUdajeCheckBox.isSelected(),
                notifikaceMesic,
                notifikaceTyden,
                tmavyRezimToggle.isSelected()
        );

        if (uspesne) {
            obnovAktualnehoPouzivatela();
            zobrazStatus("Nastavenia uložené.");
        } else {
            zobrazChybu("Nastavenia sa nepodarilo uložiť.");
        }
    }

    @FXML
    private void otvorDialogZmenyHesla() {
        if (aktualnyPouzivatelId < 0) {
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Zmena hesla");
        dialog.setHeaderText("Zadaj stare a nove heslo");

        ButtonType ulozitButtonType = new ButtonType("Ulozit", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ulozitButtonType, ButtonType.CANCEL);

        PasswordField stareHesloField = new PasswordField();
        PasswordField noveHesloField = new PasswordField();
        PasswordField potvrditHesloField = new PasswordField();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Stare heslo:"), 0, 0);
        grid.add(stareHesloField, 1, 0);
        grid.add(new Label("Nove heslo:"), 0, 1);
        grid.add(noveHesloField, 1, 1);
        grid.add(new Label("Potvrdit heslo:"), 0, 2);
        grid.add(potvrditHesloField, 1, 2);

        Region spacer = new Region();
        GridPane.setHgrow(spacer, Priority.ALWAYS);
        grid.add(spacer, 0, 3);
        GridPane.setColumnSpan(spacer, 2);
        GridPane.setHalignment(spacer, HPos.CENTER);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ulozitButtonType) {
            return;
        }

        String stareHeslo = stareHesloField.getText();
        String noveHeslo = noveHesloField.getText();
        String potvrdenie = potvrditHesloField.getText();

        if (noveHeslo == null || noveHeslo.isBlank()) {
            zobrazChybu("Nove heslo nesmie byt prazdne.");
            return;
        }

        if (!noveHeslo.equals(potvrdenie)) {
            zobrazChybu("Nove hesla sa nezhoduju.");
            return;
        }

        boolean uspesne = DatabaseManager.zmenHeslo(aktualnyPouzivatelId, stareHeslo, noveHeslo);
        if (!uspesne) {
            zobrazChybu("Stare heslo nie je spravne alebo sa zmenu nepodarilo ulozit.");
            return;
        }

        aktualneHeslo = noveHeslo;
        aktualizujHesloZobrazenie();
        obnovAktualnehoPouzivatela();
        zobrazStatus("Heslo bolo zmenené.");
        zobrazInfo("Heslo bolo úspešne zmenené.");
    }

    @FXML
    private void odhlasSa(ActionEvent event) throws IOException {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Potvrdenie odhlasenia");
        alert.setHeaderText("Naozaj sa chces odhlasit?");
        alert.setContentText("Tvoj aktualny rozpracovany stav sa neuklada mimo databazy.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            zobrazStatus("Odhlásenie zrušené.");
            return;
        }

        CurrentUserSession.clear();

        FXMLLoader loader = new FXMLLoader(getClass().getResource(LOGIN_FXML));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("Avatask - Prihlasenie");
        stage.setScene(new Scene(root));
        stage.show();
    }

    private void nacitajProfil() {
        Optional<Pouzivatel> pouzivatel = DatabaseManager.nacitajAktualnehoPouzivatela();
        if (pouzivatel.isEmpty()) {
            zobrazStatus("Nie si prihlásený.");
            return;
        }

        Pouzivatel aktualny = pouzivatel.get();
        aktualnyPouzivatelId = aktualny.getId();
        menoPouzivatelaLabel.setText(aktualny.getUzivatelskeMeno());
        levelLabel.setText("Aktualny Level: " + aktualny.getLevel());
        xpLabel.setText("Pocet XP: " + aktualny.getXp());
        emailField.setText(aktualny.getEmail() == null ? "" : aktualny.getEmail());
        emailNotifikacieCheckBox.setSelected(aktualny.isNotifikace());
        statistickeUdajeCheckBox.setSelected(aktualny.isNotifikaceDeadline());
        tmavyRezimToggle.setSelected(aktualny.isLightMode());
        tmavyRezimToggle.setText(aktualny.isLightMode() ? "Svetly rezim" : "Tmavy rezim");
        aktualneHeslo = aktualny.getHeslo();
        hesloViditelne = false;
        hesloToggleButton.setSelected(false);
        aktualizujHesloZobrazenie();
        zobrazStatus("Profil nacitany.");
    }

    private void aktualizujHesloZobrazenie() {
        String maska = vytvorMasku(aktualneHeslo);
        hesloMaskLabel.setText(maska);
        hesloVisibleField.setText(aktualneHeslo);

        hesloMaskLabel.setVisible(!hesloViditelne);
        hesloMaskLabel.setManaged(!hesloViditelne);
        hesloVisibleField.setVisible(hesloViditelne);
        hesloVisibleField.setManaged(hesloViditelne);
        hesloToggleButton.setText(hesloViditelne ? "Skryt heslo" : "Zobrazit heslo");
    }

    private String vytvorMasku(String heslo) {
        if (heslo == null || heslo.isBlank()) {
            return "********";
        }
        return "*".repeat(Math.max(8, heslo.length()));
    }

    private void obnovAktualnehoPouzivatela() {
        if (aktualnyPouzivatelId < 0) {
            return;
        }

        DatabaseManager.nacitajPouzivatelaPodlaId(aktualnyPouzivatelId).ifPresent(aktualny -> {
            CurrentUserSession.setCurrentUser(aktualny);
            aktualnyPouzivatelId = aktualny.getId();
        });
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
