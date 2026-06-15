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
import javafx.scene.image.ImageView;
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
    private static final String SATNIK_FXML = "/FXML_files/satnik.fxml";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @FXML
    private ListView<Uloha> ulohyList;

    @FXML
    private Label menoPouzivatelaLabel;

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
    private Label hornyLevelLabel;

    @FXML
    private Label hornyXpLabel;

    @FXML
    private Label hornaMenaLabel;

    @FXML
    private Label kartaXpLabel;

    @FXML
    private Label kartaLevelLabel;

    @FXML
    private javafx.scene.control.ProgressBar xpProgressBar;

    @FXML
    private Label xpZostatokLabel;

    @FXML
    private void initialize() {
        ulohyList.setCellFactory(listView -> new UlohaCell());
        obnovZoznamUloh();
        obnovPouzivatela();
        obnovAvatar();
        aktualizujStatistiky();
    }

    public void setPouzivatel(String menoPouzivatela) {
        if (menoPouzivatelaLabel != null) {
            menoPouzivatelaLabel.setText(menoPouzivatela);
        }
    }

    private void obnovPouzivatela() {
        Pouzivatel pouzivatel = CurrentUserSession.getCurrentUser();
        if (pouzivatel != null) {
            setPouzivatel(pouzivatel.getUzivatelskeMeno());
        }
    }

    private void obnovAvatar() {
        Pouzivatel pouzivatel = CurrentUserSession.getCurrentUser();
        if (pouzivatel == null) {
            return;
        }

        AvatarRenderer.vykresliAvatar(avatarBaseImageView, avatarKabatImageView, avatarSiltovkaImageView,
                avatarKlobukImageView, avatarOkuliareImageView, pouzivatel);
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
    private void otvorSatnik(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(SATNIK_FXML));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("Avatask - Obchod a satnik");
        stage.setScene(new Scene(root));
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

        obnovZoznamUloh();
    }

    private void obnovZoznamUloh() {
        if (ulohyList != null) {
            ulohyList.setItems(FXCollections.observableArrayList(DatabaseManager.nacitajUlohy()));
        }
    }

    private void aktualizujStatistiky() {
        Optional<cz.vse.avatask.model.Pouzivatel> aktualnyPouzivatel = DatabaseManager.nacitajAktualnehoPouzivatela();
        if (aktualnyPouzivatel.isPresent()) {
            cz.vse.avatask.model.Pouzivatel aktualny = aktualnyPouzivatel.get();

            if (hornyLevelLabel != null) hornyLevelLabel.setText("Level " + aktualny.getLevel());
            if (hornyXpLabel != null) hornyXpLabel.setText("XP: " + aktualny.getXp());
            if (hornaMenaLabel != null) hornaMenaLabel.setText("Mena: " + aktualny.getHerniMena());

            if (kartaLevelLabel != null) kartaLevelLabel.setText("Level " + aktualny.getLevel());
            if (kartaXpLabel != null) kartaXpLabel.setText("XP: " + aktualny.getXp());
            if (menoPouzivatelaLabel != null) menoPouzivatelaLabel.setText(aktualny.getUzivatelskeMeno());

            if (xpProgressBar != null) {
                double spodnahranica = (aktualny.getLevel() - 1) * 100.0;
                double vrchnaHranica = aktualny.getLevel() * 100.0;

                double xpVTomtoLeveli = aktualny.getXp() - spodnahranica;
                double rozdielMedziLevelmi = vrchnaHranica - spodnahranica;

                double progress = xpVTomtoLeveli / rozdielMedziLevelmi;
                xpProgressBar.setProgress(progress);

                if (xpZostatokLabel != null) {
                    int zostavaXp = (int) (vrchnaHranica - aktualny.getXp());
                    xpZostatokLabel.setText("Do levelu " + (aktualny.getLevel() + 1) + " chýba: " + zostavaXp + " XP");
                }
            }
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
            deadlineLabel.setStyle(
                    "-fx-font-size: 13px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-text-fill: " + (oznacenaUloha ? "#8B0000" : "#C1121F") + ";"
            );

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

            HBox metaRow = new HBox(8, deadlineLabel, stavLabel, detailButton);
            metaRow.setAlignment(Pos.CENTER_LEFT);

            VBox infoBox = new VBox(8, nazovLabel, metaRow, popisLabel);
            infoBox.setAlignment(Pos.CENTER_LEFT);

            Button dokoncitButton = new Button("Dokoncit");
            dokoncitButton.setDisable(uloha.jeDokoncena());
            dokoncitButton.setStyle("-fx-background-color: #6A040F; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 8 14 8 14;");
            dokoncitButton.setOnAction(event -> {
                boolean bolLevelUp = DatabaseManager.oznacAkoDokoncenu(uloha.getId());

                if (bolLevelUp) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Level up!");
                    alert.setHeaderText("Gratulujeme, dosiahli ste novy level!");
                    alert.setContentText("Skvelá práca! Ako odmenu získavaš hernú menu, ktorú môžeš použiť v obchode!.");
                    alert.showAndWait();
                }

                DatabaseManager.oznacAkoDokoncenu(uloha.getId());
                // Zavolá metodu pro změnu stavu v databázi na "Dokoncena"
                DatabaseManager.dokonciUlohu(uloha.getId());

                // Okamžitě aktualizuje seznam úkolů na obrazovce
                obnovZoznamUloh();
                aktualizujStatistiky();
            });
            Button predlzitButton = new Button("Predlzit +1 den");
            predlzitButton.setOnAction(ActionEvent_event -> {
                int cena = 50;
                int volneProdlouzeni = CurrentUserSession.getCurrentUser().getPocetVolnychProdlouzeni();

                if (volneProdlouzeni > 0) {
                    // MÁ NÁROK NA BEZPLATNÉ
                    DatabaseManager.predlzTermin(uloha.getId(), dalsiTermin(uloha.getDeadline()));

                    // Snížíme počet volných prodloužení o 1 (v DB i v aktuální Session)
                    DatabaseManager.snizPocetVolnychProdlouzeni(CurrentUserSession.getCurrentUser().getId());
                    CurrentUserSession.getCurrentUser().setPocetVolnychProdlouzeni(volneProdlouzeni - 1);

                    obnovZoznamUloh();

                } else {
                    // MUSÍ PLATIT (Už nemá žádné volné prodloužení)
                    int aktualniMena = CurrentUserSession.getCurrentUser().getHerniMena();

                    if (aktualniMena >= cena) {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("Placene predlzenie");
                        alert.setHeaderText("Vycerpali ste vsetky bezplatne predlzenia.");
                        alert.setContentText("Predlzenie o 1 den vas bude stat " + cena + " minci. Chcete pokracovat?");

                        Optional<ButtonType> result = alert.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            DatabaseManager.odectiMenu(CurrentUserSession.getCurrentUser().getId(), cena);
                            CurrentUserSession.getCurrentUser().setHerniMena(aktualniMena - cena);

                            DatabaseManager.predlzTermin(uloha.getId(), dalsiTermin(uloha.getDeadline()));
                            obnovZoznamUloh();
                        }
                    } else {
                        zobrazVarovanie("Nemate dostatok hernej meny na predlzenie!");
                    }
                }
            });
            predlzitButton.setDisable(uloha.jeDokoncena());
            predlzitButton.setStyle("-fx-background-color: white; -fx-text-fill: #6A040F; -fx-font-weight: bold; -fx-border-color: #6A040F; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 8 14 8 14;");

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
            wrapper.setStyle(
                    "-fx-background-color: " + (oznacenaUloha
                            ? "linear-gradient(from 0% 0% to 100% 100%, #ffe4e8, #ffd7de)"
                            : "linear-gradient(from 0% 0% to 100% 100%, #fff8f8, #fff0f2)") + ";" +
                            "-fx-background-radius: 16;" +
                            "-fx-border-color: " + (oznacenaUloha ? "#C1121F" : "#f0c9d0") + ";" +
                            "-fx-border-radius: 16;" +
                            "-fx-border-width: " + (oznacenaUloha ? "2" : "1") + ";"
            );
            setText(null);
            setGraphic(wrapper);
            setStyle("-fx-background-color: transparent; -fx-padding: 6 0 6 0;");
        }

        private String formatujDeadline(LocalDate deadline) {
            if (deadline == null) {
                return "Termin: bez terminu";
            }
            return "Termin: " + deadline.format(DATE_FORMAT);
        }

        private String formatujPopis(String popis) {
            if (popis == null || popis.isBlank()) {
                return "Bez detailneho popisu.";
            }
            return popis.trim();
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
            if (result.isEmpty()) {
                return;
            }

            try {
                int pocetDni = Integer.parseInt(result.get().trim());
                if (pocetDni <= 0) {
                    zobrazVarovanie("Zadaj kladne cele cislo vacsie ako 0.");
                    return;
                }

                int cenaZaDen = 50;
                int celkovaCena = cenaZaDen; // Nebo cenaZaDen * pocetDni
                int volneProdlouzeni = CurrentUserSession.getCurrentUser().getPocetVolnychProdlouzeni();

                LocalDate zaklad = dalsiTermin(uloha.getDeadline()).minusDays(1);
                LocalDate novyTermin = zaklad.plusDays(pocetDni);

                if (volneProdlouzeni > 0) {
                    // BEZPLATNÉ
                    DatabaseManager.predlzTermin(uloha.getId(), novyTermin);

                    DatabaseManager.snizPocetVolnychProdlouzeni(CurrentUserSession.getCurrentUser().getId());
                    CurrentUserSession.getCurrentUser().setPocetVolnychProdlouzeni(volneProdlouzeni - 1);

                    obnovZoznamUloh();
                } else {
                    // PLACENÉ
                    int aktualniMena = CurrentUserSession.getCurrentUser().getHerniMena();

                    if (aktualniMena >= celkovaCena) {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("Placene predlzenie");
                        alert.setHeaderText("Vycerpali ste vsetky bezplatne predlzenia.");
                        alert.setContentText("Predlzenie o " + pocetDni + " dni vas bude stat " + celkovaCena + " minci. Chcete pokracovat?");

                        Optional<ButtonType> potvrdenie = alert.showAndWait();
                        if (potvrdenie.isPresent() && potvrdenie.get() == ButtonType.OK) {
                            DatabaseManager.odectiMenu(CurrentUserSession.getCurrentUser().getId(), celkovaCena);
                            CurrentUserSession.getCurrentUser().setHerniMena(aktualniMena - celkovaCena);

                            DatabaseManager.predlzTermin(uloha.getId(), novyTermin);
                            obnovZoznamUloh();
                        }
                    } else {
                        zobrazVarovanie("Nemate dostatok hernej meny na predlzenie!");
                    }
                }
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
