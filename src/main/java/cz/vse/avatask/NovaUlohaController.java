package cz.vse.avatask;

import cz.vse.avatask.dao.DatabaseManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDate;

public class NovaUlohaController {

    @FXML
    private TextField nazevUlohy;

    @FXML
    private DatePicker deadlineUlohy;

    @FXML
    private TextField popisUlohy;

    @FXML
    private javafx.scene.control.ComboBox<String> obtiaznostCombo;

    @FXML
    private void initialize() {
        deadlineUlohy.setEditable(false);
        deadlineUlohy.setDayCellFactory(datePicker -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(empty || item.isBefore(LocalDate.now()));
            }
        });
        obtiaznostCombo.getItems().clear();
        obtiaznostCombo.getItems().addAll("Ľahká", "Stredná", "Ťažká");
        obtiaznostCombo.setValue("Ľahká");
    }

    @FXML
    private void zrusit(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML
    public void ulozUlohu(ActionEvent event) {
        String nazev = nazevUlohy.getText();
        String popis = popisUlohy.getText();

        if (nazev == null || nazev.trim().isEmpty() || popis == null || popis.trim().isEmpty()) {
            zobrazChybu("Nazov aj popis ulohy musia byt vyplnene.");
            return;
        }

        LocalDate deadline = deadlineUlohy.getValue();
        if (deadline == null) {
            zobrazChybu("Termín úlohy musí byť vyplnený. Vyberte dátum z kalendára.");
            return;
        }
        if (deadline.isBefore(LocalDate.now())) {
            zobrazChybu("Termín úlohy nemôže byť skôr ako dnešný dátum.");
            return;
        }

        String deadlineText = deadline.toString();

        String obtiaznost = obtiaznostCombo.getValue();
        if (obtiaznost == null) obtiaznost = "Ľahká";

        int zakladneXp = switch (obtiaznost) {
            case "Ľahká" -> 20;
            case "Stredná" -> 50;
            case "Ťažká" -> 75;
            default -> 20;
        };

        DatabaseManager.pridejUkol(nazev.trim(), deadlineText, popis.trim(), zakladneXp, obtiaznost);

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void zobrazChybu(String sprava) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Neplatne udaje");
        alert.setHeaderText("Ulohu sa nepodarilo ulozit");
        alert.setContentText(sprava);
        alert.showAndWait();
    }

}
