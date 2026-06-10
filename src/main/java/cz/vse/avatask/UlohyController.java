package cz.vse.avatask;

import cz.vse.avatask.dao.DatabaseManager;
import cz.vse.avatask.model.Pouzivatel;
import cz.vse.avatask.model.Uloha;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class UlohyController {
    private static final String NOVA_ULOHA_FXML = "/FXML_files/nova_uloha.fxml";
    private static final String PROFIL_FXML = "/FXML_files/profil.fxml";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @FXML private ListView<Uloha> ulohyList;
    @FXML private Label menoPouzivatelaLabel;
    @FXML private Label topLevelLabel;
    @FXML private Label topXpLabel;
    @FXML private Label leftLevelLabel;
    @FXML private Label leftXpLabel;
    @FXML private ProgressBar xpProgressBar;

    @FXML
    private void initialize() {
        ulohyList.setCellFactory(listView -> new UlohaCell());
        aktualizujRozhranie();
    }

    public void setPouzivatel(String menoPouzivatela) {
        if (menoPouzivatelaLabel != null) {
            menoPouzivatelaLabel.setText(menoPouzivatela);
        }
    }

    private void aktualizujRozhranie() {
        obnovZoznamUloh();

        Pouzivatel user = CurrentUserSession.getCurrentUser();
        if (user != null) {
            String levelText = "Level " + user.getLevel();
            String xpText = "XP: " + user.getXp();

            if (topLevelLabel != null) topLevelLabel.setText(levelText);
            if (topXpLabel != null) topXpLabel.setText(xpText);
            if (leftLevelLabel != null) leftLevelLabel.setText(levelText);
            if (leftXpLabel != null) leftXpLabel.setText(xpText);
            if (menoPouzivatelaLabel != null) menoPouzivatelaLabel.setText(user.getUzivatelskeMeno());

            if (xpProgressBar != null) {
                // Postup do dalsieho levelu: vzdy zvysok po deleni 100
                double progress = (user.getXp() % 100) / 100.0;
                xpProgressBar.setProgress(progress);
            }
        }
    }

    @FXML
    private void otvorProfil(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(PROFIL_FXML));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("Avatask - Profil");
        stage.getScene().setRoot(root);
        stage.show();
    }

    @FXML
    private void otvorNovuUlohu() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(NOVA_ULOHA_FXML));
        Parent root = loader.load();
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Avatask - Nova uloha");
        dialogStage.setScene(new Scene(root));
        dialogStage.showAndWait();
        aktualizujRozhranie();
    }

    private void obnovZoznamUloh() {
        if (ulohyList != null) {
            ulohyList.setItems(FXCollections.observableArrayList(DatabaseManager.nacitajUlohy()));
        }
    }

    private final class UlohaCell extends ListCell<Uloha> {
        @Override
        protected void updateItem(Uloha uloha, boolean empty) {
            super.updateItem(uloha, empty);

            if (empty || uloha == null) {
                setText(null);
                setGraphic(null);
                setStyle("-fx-background-color: transparent;");
                return;
            }

            boolean oznacenaUloha = isSelected();
            Label nazovLabel = new Label(uloha.getNazov());
            nazovLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #6A040F;");

            Label deadlineLabel = new Label(formatujDeadline(uloha.getDeadline()));
            deadlineLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + (oznacenaUloha ? "#8B0000" : "#C1121F") + ";");

            Label xpLabel = new Label("+" + uloha.getXp() + " XP");
            xpLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #b08d00;");

            Label stavLabel = new Label(uloha.jeDokoncena() ? "Dokoncena" : "Aktivna");
            stavLabel.setStyle(uloha.jeDokoncena()
                    ? "-fx-background-color: #d9f5e5; -fx-text-fill: #236b43; -fx-background-radius: 999; -fx-padding: 4 10 4 10; -fx-font-weight: bold;"
                    : "-fx-background-color: #fce7ea; -fx-text-fill: #6A040F; -fx-background-radius: 999; -fx-padding: 4 10 4 10; -fx-font-weight: bold;");

            Button detailButton = new Button("Detail");
            detailButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #6A040F; -fx-font-weight: bold; -fx-underline: true;");

            Label popisLabel = new Label(formatujPopis(uloha.getPopis()));
            popisLabel.setWrapText(true);
            popisLabel.setVisible(false);
            popisLabel.setManaged(false);
            popisLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7a5960;");

            detailButton.setOnAction(event -> {
                boolean zobrazene = !popisLabel.isVisible();
                popisLabel.setVisible(zobrazene);
                popisLabel.setManaged(zobrazene);
                detailButton.setText(zobrazene ? "Skryt detail" : "Detail");
            });

            HBox metaRow = new HBox(8, deadlineLabel, xpLabel, stavLabel, detailButton);
            metaRow.setAlignment(Pos.CENTER_LEFT);
            VBox infoBox = new VBox(8, nazovLabel, metaRow, popisLabel);
            infoBox.setAlignment(Pos.CENTER_LEFT);

            Button dokoncitButton = new Button("Dokoncit");
            dokoncitButton.setDisable(uloha.jeDokoncena());
            dokoncitButton.setStyle("-fx-background-color: #6A040F; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 8 14 8 14;");
            dokoncitButton.setOnAction(event -> {
                DatabaseManager.oznacAkoDokoncenu(uloha.getId());
                aktualizujRozhranie();
            });

            Button predlzitButton = new Button("Predlzit +1 den");
            predlzitButton.setDisable(uloha.jeDokoncena());
            predlzitButton.setStyle("-fx-background-color: white; -fx-text-fill: #6A040F; -fx-font-weight: bold; -fx-border-color: #6A040F; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 8 14 8 14;");
            predlzitButton.setOnAction(event -> {
                DatabaseManager.predlzTermin(uloha.getId(), dalsiTermin(uloha.getDeadline()));
                aktualizujRozhranie();
            });

            Button vlastnePredlzenieButton = new Button("Vlastny termin");
            vlastnePredlzenieButton.setDisable(uloha.jeDokoncena());
            vlastnePredlzenieButton.setStyle("-fx-background-color: white; -fx-text-fill: #6A040F; -fx-font-weight: bold; -fx-border-color: #d8a2ab; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 8 14 8 14;");
            vlastnePredlzenieButton.setOnAction(event -> otvorDialogPredlzenia(uloha));

            VBox actionsBox = new VBox(8, dokoncitButton, predlzitButton, vlastnePredlzenieButton);
            actionsBox.setAlignment(Pos.CENTER_RIGHT);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox wrapper = new HBox(16, infoBox, spacer, actionsBox);
            wrapper.setAlignment(Pos.CENTER_LEFT);
            wrapper.setPadding(new Insets(14));
            wrapper.setStyle("-fx-background-color: " + (oznacenaUloha ? "linear-gradient(from 0% 0% to 100% 100%, #ffe4e8, #ffd7de)" : "linear-gradient(from 0% 0% to 100% 100%, #fff8f8, #fff0f2)") + "; -fx-background-radius: 16; -fx-border-color: " + (oznacenaUloha ? "#C1121F" : "#f0c9d0") + "; -fx-border-radius: 16; -fx-border-width: " + (oznacenaUloha ? "2" : "1") + ";");

            setText(null);
            setGraphic(wrapper);
            setStyle("-fx-background-color: transparent; -fx-padding: 6 0 6 0;");
        }

        private String formatujDeadline(LocalDate deadline) {
            return deadline == null ? "Termin: bez terminu" : "Termin: " + deadline.format(DATE_FORMAT);
        }

        private String formatujPopis(String popis) {
            return (popis == null || popis.isBlank()) ? "Bez detailneho popisu." : popis.trim();
        }

        private LocalDate dalsiTermin(LocalDate deadline) {
            LocalDate zaklad = deadline == null || deadline.isBefore(LocalDate.now()) ? LocalDate.now() : deadline;
            return zaklad.plusDays(1);
        }

        private void otvorDialogPredlzenia(Uloha uloha) {
            TextInputDialog dialog = new TextInputDialog("3");
            dialog.setTitle("Predlzenie terminu");
            dialog.setHeaderText("O kolko dni chces predlzit termin?");
            dialog.setContentText("Pocet dni:");

            Optional<String> result = dialog.showAndWait();
            if (result.isEmpty()) return;

            try {
                int pocetDni = Integer.parseInt(result.get().trim());
                if (pocetDni <= 0) {
                    zobrazVarovanie("Zadaj kladne cele cislo vacsie ako 0.");
                    return;
                }
                LocalDate zaklad = dalsiTermin(uloha.getDeadline()).minusDays(1);
                DatabaseManager.predlzTermin(uloha.getId(), zaklad.plusDays(pocetDni));
                aktualizujRozhranie();
            } catch (NumberFormatException e) {
                zobrazVarovanie("Pocet dni musi byt cele cislo.");
            }
        }

        private void zobrazVarovanie(String sprava) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Neplatna hodnota");
            alert.setHeaderText("Termin sa nepodarilo predlzit");
            alert.setContentText(sprava);
            alert.showAndWait();
        }
    }
}