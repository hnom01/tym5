package cz.vse.avatask;

import cz.vse.avatask.dao.DatabaseManager;
import cz.vse.avatask.model.AkciaVysledok;
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
import java.util.List;

public class UlohyController {
    private static final String NOVA_ULOHA_FXML = "/FXML_files/nova_uloha.fxml";
    private static final String PROFIL_FXML = "/FXML_files/profil.fxml";
    private static final String SATNIK_FXML = "/FXML_files/satnik.fxml";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @FXML
    private ListView<Uloha> aktivneUlohyList;

    @FXML
    private ListView<Uloha> dokonceneUlohyList;

    @FXML
    private Label menoPouzivatelaLabel;

    @FXML
    private ImageView avatarBaseImageView;

    @FXML
    private ImageView avatarNahrdelnikImageView;

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
    private Label volnePredlzeniaLabel;

    @FXML
    private javafx.scene.control.ProgressBar xpProgressBar;

    @FXML
    private Label xpZostatokLabel;

    @FXML
    private void initialize() {
        if (aktivneUlohyList != null) aktivneUlohyList.setCellFactory(listView -> new UlohaCell());
        if (dokonceneUlohyList != null) dokonceneUlohyList.setCellFactory(listView -> new UlohaCell());
        obnovZoznamUloh();
        obnovPouzivatela();
        obnovAvatar();
        aktualizujStatistiky();

        javafx.application.Platform.runLater(() -> {
            if (aktivneUlohyList != null && aktivneUlohyList.getScene() != null) {
                ThemeManager.aplikujTemu(aktivneUlohyList.getScene());
            }
        });
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

        AvatarRenderer.vykresliAvatar(avatarBaseImageView, avatarNahrdelnikImageView, avatarSiltovkaImageView,
                avatarKlobukImageView, avatarOkuliareImageView, pouzivatel);
    }

    @FXML
    private void otvorProfil(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(PROFIL_FXML));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("Avatask - Profil");
        stage.getScene().setRoot(root);
    }

    @FXML
    private void otvorSatnik(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(SATNIK_FXML));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("Avatask - Obchod a satnik");
        stage.getScene().setRoot(root);
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
        List<Uloha> vsetkyUlohy = DatabaseManager.nacitajUlohy();

        List<Uloha> aktivne = vsetkyUlohy.stream().filter(u -> !u.jeDokoncena()).toList();
        List<Uloha> dokoncene = vsetkyUlohy.stream().filter(Uloha::jeDokoncena).toList();

        if (aktivneUlohyList != null) aktivneUlohyList.setItems(FXCollections.observableArrayList(aktivne));
        if (dokonceneUlohyList != null) dokonceneUlohyList.setItems(FXCollections.observableArrayList(dokoncene));
    }

    private void aktualizujStatistiky() {
        Optional<cz.vse.avatask.model.Pouzivatel> aktualnyPouzivatel = DatabaseManager.nacitajAktualnehoPouzivatela();
        if (aktualnyPouzivatel.isPresent()) {
            cz.vse.avatask.model.Pouzivatel aktualny = aktualnyPouzivatel.get();

            if (hornyLevelLabel != null) hornyLevelLabel.setText("Level " + aktualny.getLevel());
            if (hornyXpLabel != null) hornyXpLabel.setText("XP: " + aktualny.getXp());
            if (hornaMenaLabel != null) hornaMenaLabel.setText("Mena: " + aktualny.getHerniMena());
            if (menoPouzivatelaLabel != null) menoPouzivatelaLabel.setText(aktualny.getUzivatelskeMeno());
            if (volnePredlzeniaLabel != null) volnePredlzeniaLabel.setText("Voľné predĺženia: " + aktualny.getPocetVolnychProdlouzeni());

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
            nazovLabel.getStyleClass().add("nadpis-text");
            nazovLabel.setStyle("-fx-font-size: 15px;");

            Label upozorneniLabel = new Label();
            if (!uloha.jeDokoncena() && uloha.getDeadline() != null && uloha.getDeadline().isBefore(LocalDate.now())) {
                upozorneniLabel.setText("PO TERMÍNE!");
                upozorneniLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 11px;");
            }
            String textObtiaznosti = uloha.getObtiaznost() != null ? uloha.getObtiaznost() : "Ľahká";
            Label obtiaznostLabel = new Label(textObtiaznosti);

            String farbaPozadia = switch (textObtiaznosti) {
                case "Stredná" -> "#ffeeba";
                case "Ťažká" -> "#f5c6cb";
                default -> "#d4edda";
            };
            obtiaznostLabel.setStyle("-fx-font-size: 11px; -fx-background-color: " + farbaPozadia + "; -fx-text-fill: black; -fx-padding: 2 6 2 6; -fx-background-radius: 6; -fx-font-weight: bold;");

            HBox nazovBox = new HBox(10, nazovLabel, obtiaznostLabel);
            nazovBox.setAlignment(Pos.CENTER_LEFT);

            Label deadlineLabel = new Label(formatujDeadline(uloha.getDeadline()));
            deadlineLabel.getStyleClass().add(oznacenaUloha ? "nadpis-text" : "hlavny-text");
            deadlineLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

            Label stavLabel = new Label(uloha.jeDokoncena() ? "Dokončená" : "Aktívna");
            stavLabel.setStyle(uloha.jeDokoncena()
                    ? "-fx-background-color: #d9f5e5; -fx-text-fill: #236b43; -fx-background-radius: 999; -fx-padding: 4 10 4 10; -fx-font-weight: bold;"
                    : "-fx-background-color: #fce7ea; -fx-text-fill: -fx-primary; -fx-background-radius: 999; -fx-padding: 4 10 4 10; -fx-font-weight: bold;");

            Button detailButton = new Button("Detail");
            detailButton.getStyleClass().add("nadpis-text");
            detailButton.setStyle("-fx-background-color: transparent; -fx-underline: true;");

            Label popisLabel = new Label(formatujPopis(uloha.getPopis()));
            popisLabel.setWrapText(true);
            popisLabel.setVisible(false);
            popisLabel.setManaged(false);
            popisLabel.getStyleClass().add("hlavny-text");
            popisLabel.setStyle("-fx-font-size: 12px;");

            detailButton.setOnAction(event -> {
                boolean zobrazene = !popisLabel.isVisible();
                popisLabel.setVisible(zobrazene);
                popisLabel.setManaged(zobrazene);
                detailButton.setText(zobrazene ? "Skryť detail" : "Detail");
            });

            HBox metaRow = new HBox(8, deadlineLabel, stavLabel, detailButton);
            metaRow.setAlignment(Pos.CENTER_LEFT);


            Label stavOpoždeníLabel = new Label();
            if (uloha.jeDokoncena() && uloha.getDeadline() != null) {

                if (uloha.getDatumDokonceni() != null && uloha.getDatumDokonceni().isAfter(uloha.getDeadline())) {
                    stavOpoždeníLabel.setText("Odovzdané po deadline!");
                    stavOpoždeníLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 11px;");
                }
            }

            VBox infoBox = new VBox(8, nazovBox, metaRow, popisLabel, stavOpoždeníLabel, upozorneniLabel);
            infoBox.setAlignment(Pos.CENTER_LEFT);

            Button dokoncitButton = new Button("Dokoncit");
            dokoncitButton.setDisable(uloha.jeDokoncena());
            dokoncitButton.setStyle("-fx-background-color: -fx-primary; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 8 14 8 14;");
            dokoncitButton.setOnAction(event -> {
                boolean bolLevelUp = DatabaseManager.oznacAkoDokoncenu(uloha.getId());

                if (bolLevelUp) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Level up!");
                    alert.setHeaderText("Gratulujeme, dosiahli ste novy level!");
                    alert.setContentText("Skvelá práca! Ako odmenu získavaš hernú menu, ktorú môžeš použiť v obchode!.");
                    alert.showAndWait();
                }

                obnovZoznamUloh();
                aktualizujStatistiky();
            });
            Button predlzitButton = new Button("Predĺžiť +1 den");
            predlzitButton.setOnAction(ActionEvent_event -> {
                int cena = 50;
                int volneProdlouzeni = CurrentUserSession.getCurrentUser().getPocetVolnychProdlouzeni();

                if (volneProdlouzeni > 0) {

                    DatabaseManager.predlzTermin(uloha.getId(), dalsiTermin(uloha.getDeadline()));


                    DatabaseManager.snizPocetVolnychProdlouzeni(CurrentUserSession.getCurrentUser().getId());
                    CurrentUserSession.getCurrentUser().setPocetVolnychProdlouzeni(volneProdlouzeni - 1);

                    obnovZoznamUloh();
                    aktualizujStatistiky();

                } else {
                    int aktualniMena = CurrentUserSession.getCurrentUser().getHerniMena();

                    if (aktualniMena >= cena) {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("Platené predĺženie");
                        alert.setHeaderText("Vyčerpali ste všetky bezplatné predĺženia.");
                        alert.setContentText("Predĺženie o 1 deň vás bude stáť " + cena + " mincí. Chcete pokračovať?");

                        Optional<ButtonType> result = alert.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            AkciaVysledok vysledok = DatabaseManager.plateneVykonajPredlzenie(
                                    CurrentUserSession.getCurrentUser().getId(),
                                    uloha.getId(),
                                    dalsiTermin(uloha.getDeadline()),
                                    cena
                            );
                            if (vysledok.uspesne()) {
                                obnovZoznamUloh();
                                aktualizujStatistiky();
                            } else {
                                zobrazVarovanie(vysledok.sprava());
                            }
                        }
                    } else {
                        zobrazVarovanie("Nemáte dostatok hernej meny na predĺženie!");
                    }
                }
            });
            predlzitButton.setDisable(uloha.jeDokoncena());
            predlzitButton.setStyle("-fx-background-color: transparent; -fx-text-fill: -fx-primary; -fx-font-weight: bold; -fx-border-color: -fx-primary; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 8 14 8 14;");

            Button vlastnePredlzenieButton = new Button("Vlastný termín");
            vlastnePredlzenieButton.setDisable(uloha.jeDokoncena());
            vlastnePredlzenieButton.setStyle("-fx-background-color: white; -fx-text-fill: #6A040F; -fx-font-weight: bold; -fx-border-color: -fx-primary; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 8 14 8 14;");
            vlastnePredlzenieButton.setOnAction(event -> otvorDialogPredlzenia(uloha));

            VBox actionsBox = new VBox(8, dokoncitButton, predlzitButton, vlastnePredlzenieButton);
            actionsBox.setAlignment(Pos.CENTER_RIGHT);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox wrapper = new HBox(16, infoBox, spacer, actionsBox);
            wrapper.setAlignment(Pos.CENTER_LEFT);
            wrapper.setPadding(new Insets(14));
            wrapper.getStyleClass().add("karta-pozadie");
            wrapper.setStyle("-fx-background-radius: 16; -fx-border-radius: 16; -fx-border-width: " + (oznacenaUloha ? "2" : "1") + ";");
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
            dialog.setTitle("Predĺženie termínu");
            dialog.setHeaderText("O koľko dní chceš predĺžiť termín?");
            dialog.setContentText("Počet dní:");

            Optional<String> result = dialog.showAndWait();
            if (result.isEmpty()) {
                return;
            }

            try {
                int pocetDni = Integer.parseInt(result.get().trim());
                if (pocetDni <= 0) {
                    zobrazVarovanie("Zadaj kladné celé číslo väčšie ako 0.");
                    return;
                }

                int cenaZaDen = 50;
                int celkovaCena = cenaZaDen;
                int volneProdlouzeni = CurrentUserSession.getCurrentUser().getPocetVolnychProdlouzeni();

                LocalDate zaklad = dalsiTermin(uloha.getDeadline()).minusDays(1);
                LocalDate novyTermin = zaklad.plusDays(pocetDni);

                if (volneProdlouzeni > 0) {

                    DatabaseManager.predlzTermin(uloha.getId(), novyTermin);

                    DatabaseManager.snizPocetVolnychProdlouzeni(CurrentUserSession.getCurrentUser().getId());
                    CurrentUserSession.getCurrentUser().setPocetVolnychProdlouzeni(volneProdlouzeni - 1);

                    obnovZoznamUloh();
                    aktualizujStatistiky();

                } else {

                    int aktualniMena = CurrentUserSession.getCurrentUser().getHerniMena();

                    if (aktualniMena >= celkovaCena) {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("Platené predĺženie");
                        alert.setHeaderText("Vyčerpali ste všetky bezplatné predĺženia.");
                        alert.setContentText("Predĺženie o " + pocetDni + " dní vás bude stáť " + celkovaCena + " mincí. Chcete pokracovat?");

                        Optional<ButtonType> potvrdenie = alert.showAndWait();
                        if (potvrdenie.isPresent() && potvrdenie.get() == ButtonType.OK) {
                            AkciaVysledok vysledok = DatabaseManager.plateneVykonajPredlzenie(
                                    CurrentUserSession.getCurrentUser().getId(),
                                    uloha.getId(),
                                    novyTermin,
                                    celkovaCena
                            );
                            if (vysledok.uspesne()) {
                                obnovZoznamUloh();
                                aktualizujStatistiky();
                            } else {
                                zobrazVarovanie(vysledok.sprava());
                            }
                        }
                    } else {
                        zobrazVarovanie("Nemáte dostatok hernej meny na predĺženie!");
                    }
                }
            } catch (NumberFormatException e) {
                zobrazVarovanie("Počet dní musí byť celé číslo.");
            }
        }

        private void zobrazVarovanie(String sprava) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Neplatná hodnota");
            alert.setHeaderText("Termín sa nepodarilo predĺžiť");
            alert.setContentText(sprava);
            alert.showAndWait();
        }
    }

    @FXML
    private void otvorUlohy(ActionEvent event) {
        obnovZoznamUloh();
    }
    @FXML
    private void otvorStatistiky(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML_files/stats.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("Avatask - Štatistiky");
        stage.getScene().setRoot(root);
        obnovZoznamUloh();
    }
}
