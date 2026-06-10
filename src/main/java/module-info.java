module cz.vse.avatask {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens cz.vse.avatask to javafx.fxml;
    exports cz.vse.avatask;
}