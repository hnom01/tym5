package cz.vse.avatask;

import cz.vse.avatask.dao.DatabaseManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
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
    private ComboBox<String> obtiznostUlohy;

    @FXML
    private void initialize() {
        deadlineUlohy.setDayCellFactory(datePicker -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(empty || item.isBefore(LocalDate.now()));
            }
        });

        if (obtiznostUlohy != null) {
            obtiznostUlohy.getItems().addAll(
                    "Veľmi ľahká (5 XP)",
                    "Ľahká (10 XP)",
                    "Stredná (15 XP)",
                    "Ťažká (20 XP)",
                    "Veľmi ťažká (25 XP)"
            );
            obtiznostUlohy.setValue("Stredná (15 XP)");
        }
    }

    @FXML
    private void zrusit(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML
    public void ulozUlohu() {
        String nazev = nazevUlohy.getText();
        String popis = popisUlohy.getText();

        if (nazev == null || nazev.trim().isEmpty() || popis == null || popis.trim().isEmpty()) {
            zobrazChybu("Nazov aj popis ulohy musia byt vyplnene.");
            return;
        }

        LocalDate deadline = deadlineUlohy.getValue();
        if (deadline != null && deadline.isBefore(LocalDate.now())) {
            zobrazChybu("Termin ulohy nemoze byt skor ako dnesny datum.");
            return;
        }

        int xpUlohy = 15;
        if (obtiznostUlohy != null && obtiznostUlohy.getValue() != null) {
            switch (obtiznostUlohy.getValue()) {
                case "Veľmi ľahká (5 XP)": xpUlohy = 5; break;
                case "Ľahká (10 XP)": xpUlohy = 10; break;
                case "Stredná (15 XP)": xpUlohy = 15; break;
                case "Ťažká (20 XP)": xpUlohy = 20; break;
                case "Veľmi ťažká (25 XP)": xpUlohy = 25; break;
            }
        }

        String deadlineText = deadline == null ? "" : deadline.toString();
        DatabaseManager.pridejUkol(nazev.trim(), deadlineText, popis.trim(), xpUlohy);
        zavriOkno();
    }

    private void zobrazChybu(String sprava) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Neplatne udaje");
        alert.setHeaderText("Ulohu sa nepodarilo ulozit");
        alert.setContentText(sprava);
        alert.showAndWait();
    }

    private void zavriOkno() {
        Stage stage = (Stage) nazevUlohy.getScene().getWindow();
        stage.close();
    }
}