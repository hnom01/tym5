package cz.vse.avatask;

import cz.vse.avatask.dao.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    private static final String LOGIN_FXML = "/FXML_files/login.fxml";

    @Override
    public void start(Stage stage) throws Exception {
        DatabaseManager.inicializujDatabazi();

        FXMLLoader loader = new FXMLLoader(getClass().getResource(LOGIN_FXML));
        Parent root = loader.load();

        stage.setTitle("Avatask - Prihlasenie");
        stage.setScene(new Scene(root));
        stage.setMaximized(true);
        stage.show();
    }
}
