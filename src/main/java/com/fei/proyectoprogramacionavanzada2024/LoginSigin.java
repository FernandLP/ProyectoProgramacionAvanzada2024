package com.fei.proyectoprogramacionavanzada2024;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javafx.geometry.Insets;

import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginSigin extends BorderPane {

    private LoginCallback loginCallback;
    private Socket socket;
    private PrintWriter out;
    private final Gson gson;
    private Button login;
    private Button sigin;
    private String user;

    public LoginSigin(LoginCallback loginCallback) {
        this.loginCallback = loginCallback;
        this.gson = new Gson();
        iniciarComponentes();
    }

    private void iniciarComponentes() {
        HBox botones = new HBox();
        login = new Button("Iniciar sesión");
        sigin = new Button("Crear cuenta");
        login.setDisable(true);
        pantallaIniciarSesion();

        login.setOnAction(evt -> {
            login.setDisable(true);
            sigin.setDisable(false);
            pantallaIniciarSesion();
        });

        sigin.setOnAction(evt -> {
            login.setDisable(false);
            sigin.setDisable(true);
            pantallaNuevoUsuario();
        });

        botones.getChildren().addAll(login, sigin);
        setTop(botones);
        setPadding(new Insets(10));
    }

    private void pantallaNuevoUsuario() {
        VBox vBox = new VBox(10);
        Label labelNombre = new Label("Nombre");
        TextField tfNombre = new TextField();
        Label labelCorreo = new Label("Correo");
        TextField tfCorreo = new TextField();
        Label labelContrasenia = new Label("Contraseña");
        PasswordField tfContrasenia = new PasswordField();
        Button buttonNuevoUsuario = new Button("Registrarse");

        buttonNuevoUsuario.setOnAction(evt -> {
            // Verificar si todos los campos están completos
            if (tfNombre.getText().isEmpty() || tfCorreo.getText().isEmpty() || tfContrasenia.getText().isEmpty()) {
                // Mostrar una alerta si algún campo está vacío
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Por favor, complete todos los campos.");
                alert.showAndWait();
            } else {
                // Todos los campos están completos, proceder con el registro del usuario
                registrarNuevoUsuario(tfNombre.getText(), tfCorreo.getText(), tfContrasenia.getText());
            }
        });

        vBox.getChildren().addAll(labelNombre, tfNombre, labelCorreo, tfCorreo, labelContrasenia, tfContrasenia, buttonNuevoUsuario);
        setCenter(vBox);
    }

    private void registrarNuevoUsuario(String nombre, String correo, String contrasenia) {
        // Cifrar la contraseña
        String contraseniaCifrada = cifrarContrasenia(contrasenia);

        // Verificar si la contraseña se pudo cifrar correctamente
        if (contraseniaCifrada != null) {
            // Crear el objeto JSON para enviar al servidor
            JsonObject signupJson = new JsonObject();
            signupJson.addProperty("type", "signup");
            signupJson.addProperty("nombre", nombre);
            signupJson.addProperty("correo", correo);
            signupJson.addProperty("contrasenia", contraseniaCifrada); // Enviar la contraseña cifrada

            // Convertir el objeto JSON a una cadena
            String jsonString = gson.toJson(signupJson);

            try {
                // Establecer conexión con el servidor
                socket = new Socket("127.0.0.1", 12345);
                BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter salida = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                // Enviar el mensaje JSON al servidor
                salida.write(jsonString + "\n");
                salida.flush();

                // Leer respuesta del servidor
                String respuesta = entrada.readLine();

                // Parsear la respuesta JSON
                JsonObject respuestaJson = gson.fromJson(respuesta, JsonObject.class);
                String registroStatus = respuestaJson.get("status").getAsString();

                // Mostrar mensaje adecuado según el resultado del registro
                if (registroStatus.equals("success")) {
                    // Registro exitoso, mostrar un mensaje de éxito
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Registro exitoso");
                    alert.setHeaderText(null);
                    alert.setContentText("El usuario se ha registrado correctamente.\n Puedes ingresar tu contraseña de nuevo para continuar.");
                    login.setDisable(true);
                    sigin.setDisable(false);
                    user = correo;
                    pantallaIniciarSesion();
                    alert.showAndWait();
                } else if (registroStatus.equals("failure")) {
                    // Error en el registro, mostrar un mensaje de error
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText(null);
                    alert.setContentText("Error al registrar el usuario. Inténtelo de nuevo más tarde.");
                    alert.showAndWait();
                }

            } catch (IOException e) {
                System.err.println("Error en la comunicación con el servidor: " + e.getMessage());
            }
        } else {
            // Mostrar un mensaje de error si no se pudo cifrar la contraseña
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Error al cifrar la contraseña.");
            alert.showAndWait();
        }


    }

    public void pantallaIniciarSesion() {
        VBox vBox = new VBox(10);
        Label labelCorreo = new Label("Correo");
        TextField tfCorreo = new TextField();
        if (user != "") {
            tfCorreo.setText(user);
        } else {
            tfCorreo.setText("");
        }
        Label labelContrasenia = new Label("Contraseña");
        PasswordField tfContrasenia = new PasswordField();
        Button buttonIniciarSesion = new Button("Ingresar");

        buttonIniciarSesion.setOnAction(evt -> {
            // Captura los valores de los campos de texto
            String correo = tfCorreo.getText();
            String contrasenia = tfContrasenia.getText();
            String contraseniaCifrada = cifrarContrasenia(contrasenia);

            if (contraseniaCifrada != null) {
                // Crear el objeto JSON para enviar al servidor
                JsonObject loginJson = new JsonObject();
                loginJson.addProperty("type", "login");
                loginJson.addProperty("usuario", correo);
                loginJson.addProperty("contraseña", contraseniaCifrada); // Enviar la contraseña cifrada

                // Convierte el objeto JSON a una cadena
                String jsonString = gson.toJson(loginJson);

                try {
                    socket = new Socket("127.0.0.1", 12345);
                    BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    BufferedWriter salida = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                    salida.write(jsonString + "\n");
                    salida.flush();

                    // Leer respuesta del servidor
                    String respuesta = entrada.readLine();

                    // Parsear la respuesta JSON
                    JsonObject respuestaJson = gson.fromJson(respuesta, JsonObject.class);
                    String validity = respuestaJson.get("validity").getAsString();

                    // Mostrar mensaje adecuado según la validez de la respuesta
                    if (validity.equals("VALIDO")) {
                        loginCallback.onLoginSuccess(socket);
                    } else if (validity.equals("INVALIDO")) {
                        // Mostrar una alerta de que la contraseña o el usuario son incorrectos
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error de inicio de sesión");
                        alert.setHeaderText(null);
                        alert.setContentText("El correo electrónico o la contraseña son incorrectos.");
                        alert.showAndWait();
                    }

                } catch (IOException e) {
                    System.err.println("Error en la comunicación con el servidor: " + e.getMessage());
                }
            } else {
                // Mostrar un mensaje de error si no se pudo cifrar la contraseña
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Error al cifrar la contraseña.");
                alert.showAndWait();
            }
        });

        vBox.getChildren().addAll(labelCorreo, tfCorreo, labelContrasenia, tfContrasenia, buttonIniciarSesion);
        setCenter(vBox);
    }

    private String cifrarContrasenia(String contrasenia) {
        try {
            // Crear una instancia de MessageDigest con el algoritmo MD5
            MessageDigest md = MessageDigest.getInstance("MD5");

            // Aplicar el cifrado a la contraseña
            byte[] bytes = md.digest(contrasenia.getBytes());

            // Convertir los bytes cifrados a una representación hexadecimal
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }

            // Retornar la contraseña cifrada
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // Manejar una excepción si el algoritmo MD5 no está disponible
            e.printStackTrace();
            return null;
        }
    }

    public Socket getSocket() {
        return socket;
    }
}
