package com.fei.proyectoprogramacionavanzada2024;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;

public class TareasApplication extends Application implements LoginCallback {
    private Stage stage;

    @Override
    public void start(Stage stage) throws IOException {
        this.stage = stage;
        LoginSigin root = new LoginSigin(this);
        Scene scene = new Scene(root, 400, 600);

        // Cargar el archivo CSS
        String css = getClass().getResource("/estilos.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("Tareas");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void onLoginSuccess(Socket socket) throws IOException {
        ContenedorPrincipal contenedorPrincipal = new ContenedorPrincipal(socket); // Pasar el socket al constructor
        Scene scene = new Scene(contenedorPrincipal, 800, 600);

        // Cargar el archivo CSS
        String css = getClass().getResource("/estilos.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setOnCloseRequest(evt -> {
            // Mostrar una alerta contextual para guardar las tareas en el servidor
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Guardar tareas");
            alert.setHeaderText("¿Desea guardar las tareas antes de salir?");
            alert.setContentText("Si elige \"Sí\", se guardarán las tareas en el servidor.");

            // Agregar botones personalizados
            ButtonType yesButton = new ButtonType("Sí");
            ButtonType noButton = new ButtonType("No");

            alert.getButtonTypes().setAll(yesButton, noButton);

            // Mostrar la alerta y esperar la respuesta del usuario
            alert.showAndWait().ifPresent(response -> {
                if (response == yesButton) {
                    // Si el usuario elige "Sí", llamar al método para guardar las tareas en el servidor
                    try {
                        contenedorPrincipal.guardarTareasEnServidor();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else if (response == noButton) {

                }
            });
        });

        stage.setScene(scene);
    }

    public static void main(String[] args) {
        launch();
    }
}
