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
    private void initialize() {
        deadlineUlohy.setDayCellFactory(datePicker -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(empty || item.isBefore(LocalDate.now()));
            }
        });
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
        if (deadline != null && deadline.isBefore(LocalDate.now())) {
            zobrazChybu("Termin ulohy nemoze byt skor ako dnesny datum.");
            return;
        }

        String deadlineText = deadline == null ? "" : deadline.toString();

        int zakladneXp = 20;

        DatabaseManager.pridejUkol(nazev.trim(), deadlineText, popis.trim(), zakladneXp);

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

    private void zavriOkno() {
        Stage stage = (Stage) nazevUlohy.getScene().getWindow();
        stage.close();
    }
}
