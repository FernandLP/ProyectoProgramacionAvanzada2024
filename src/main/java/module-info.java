module com.fei.proyectoprogramacionavanzada2024 {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;

    opens com.fei.proyectoprogramacionavanzada2024 to javafx.fxml, com.google.gson;
    exports com.fei.proyectoprogramacionavanzada2024;
}
