package cz.vse.avatask;

import cz.vse.avatask.dao.DatabaseManager;
import cz.vse.avatask.model.Pouzivatel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Optional;

public class RegistraciaController {
    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private void registrujSa(ActionEvent event) {
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            zobrazChybu("Vypln meno a heslo.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            zobrazChybu("Hesla sa nezhoduju.");
            return;
        }

        Optional<Pouzivatel> registracia = DatabaseManager.registrujPouzivatela(username, password, email);
        if (registracia.isEmpty()) {
            zobrazChybu("Registracia zlyhala. Meno uz existuje alebo udaje nie su platne.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Registracia uspesna");
        alert.setHeaderText("Novy ucet bol vytvoreny.");
        alert.setContentText("Teraz sa mozes prihlasit cez povodne prihlasovacie meno a heslo.");
        alert.showAndWait();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void zobrazChybu(String sprava) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Registracia zlyhala");
        alert.setHeaderText("Nepodarilo sa vytvorit ucet");
        alert.setContentText(sprava);
        alert.showAndWait();
    }
}
