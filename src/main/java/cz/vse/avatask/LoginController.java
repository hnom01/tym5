package cz.vse.avatask;

import cz.vse.avatask.dao.DatabaseManager;
import cz.vse.avatask.model.Pouzivatel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class LoginController {
    private static final String ULOHY_FXML = "/FXML_files/ulohy.fxml";
    private static final String REGISTRACIA_FXML = "/FXML_files/registracia.fxml";

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private void otvorRegistraciu(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(REGISTRACIA_FXML));
        Parent root = loader.load();

        Stage stage = new Stage();
        stage.setTitle("Avatask - Registracia");
        stage.initOwner(((Node) event.getSource()).getScene().getWindow());
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.setScene(new Scene(root));
        stage.showAndWait();
    }

    @FXML
    private void prihlasSa(ActionEvent event) throws IOException {
        Optional<Pouzivatel> prihlasenyPouzivatel = DatabaseManager.prihlasPouzivatela(
                usernameField.getText(),
                passwordField.getText()
        );

        if (prihlasenyPouzivatel.isEmpty()) {
            zobrazChybu("Nespravne prihlasovacie udaje.", "Skontroluj meno a heslo.");
            return;
        }

        CurrentUserSession.setCurrentUser(prihlasenyPouzivatel.get());

        FXMLLoader loader = new FXMLLoader(getClass().getResource(ULOHY_FXML));
        Parent root = loader.load();

        UlohyController controller = loader.getController();
        controller.setPouzivatel(prihlasenyPouzivatel.get().getUzivatelskeMeno());

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("Avatask - Ulohy");
        stage.getScene().setRoot(root);
    }

    private void zobrazChybu(String hlavicka, String text) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Prihlasenie zlyhalo");
        alert.setHeaderText(hlavicka);
        alert.setContentText(text);
        alert.showAndWait();
    }
}
