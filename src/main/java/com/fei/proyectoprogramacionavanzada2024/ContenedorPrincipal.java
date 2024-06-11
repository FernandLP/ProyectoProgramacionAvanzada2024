package com.fei.proyectoprogramacionavanzada2024;

import com.google.gson.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.io.*;
import java.net.Socket;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ContenedorPrincipal extends BorderPane {

    private BorderPane menuLateral;
    private Label labelNombre;
    private Label labelCorreo;
    private ImageView fotoPerfil;
    private ArrayList<Tarea> listaTareas;
    private ObservableList<Tarea> items;
    private ListView<Tarea> listView;
    private Usuario usuario;

    private BorderPane vistaPrincipal;
    private TextField textFieldTitulo;
    private TextArea textFieldDescripcion;
    private Button buttonGuardar;
    private Button buttonNuevaTarea;
    private Button buttonGuardarEnServidor;
    private ComboBox<String> comboBoxPrioridad;
    private ColorPicker colorPicker;
    private String colorHex = "#FFFFFF";
    private DatePicker datePickerFecha;

    private Tarea tareaActual = null;
    private Socket socket;
    private final Gson gson;
    private BufferedReader entrada;
    private BufferedWriter salida;

    public ContenedorPrincipal(Socket socket) throws IOException {
        this.socket = socket;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .create();
        this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.salida = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        iniciarAplicacion();
    }

    private void iniciarAplicacion() throws IOException {
        iniciarMenuLateral();
        iniciarVistaPrincipal();
        cargarPerfilDesdeServidor();
        cargarTareasDesdeServidor();
    }

    private void cargarPerfilDesdeServidor() throws IOException {
        JsonObject loginJson = new JsonObject();
        loginJson.addProperty("type", "loadProfile");
        String jsonString = gson.toJson(loginJson);

        salida.write(jsonString + "\n");
        salida.flush();

        String respuesta = entrada.readLine();

        JsonObject respuestaJson = gson.fromJson(respuesta, JsonObject.class);
        usuario = new Usuario(
                respuestaJson.get("id").getAsInt(),
                respuestaJson.get("nombre").getAsString(),
                respuestaJson.get("email").getAsString(),
                respuestaJson.get("contrasena").getAsString()
        );

        labelNombre.setText(usuario.getNombre());
        labelCorreo.setText(usuario.getEmail());
    }

    private void cargarTareasDesdeServidor() throws IOException {
        JsonObject loginJson = new JsonObject();
        loginJson.addProperty("type", "allTask");
        String jsonString = gson.toJson(loginJson);

        salida.write(jsonString + "\n");
        salida.flush();

        String respuesta = entrada.readLine();

        JsonElement jsonElement = JsonParser.parseString(respuesta);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        if (jsonObject.has("type") && jsonObject.get("type").getAsString().equals("allTaskResponse")) {
            JsonArray tareasArray = jsonObject.getAsJsonArray("tareas");

            for (JsonElement tareaElement : tareasArray) {
                JsonObject tareaObject = tareaElement.getAsJsonObject();

                int id = tareaObject.get("id").getAsInt();
                String titulo = tareaObject.get("titulo").getAsString();
                String descripcion = tareaObject.get("descripcion").getAsString();
                LocalDate fechaLimite = LocalDate.parse(tareaObject.get("fechaLimite").getAsString(), DateTimeFormatter.ofPattern("MMM d, yyyy"));
                String prioridad = tareaObject.get("prioridad").getAsString();
                String color = tareaObject.get("color").getAsString();
                int usuarioId = tareaObject.get("usuarioId").getAsInt();

                Tarea tarea = new Tarea(id, titulo, descripcion, fechaLimite, prioridad, color, usuarioId, "sinCambios");
                listaTareas.add(tarea);
                items.add(tarea);
            }
        } else {
            System.out.println("Error: El servidor no devolvió una respuesta válida.");
        }
    }


    private void iniciarMenuLateral() {
        menuLateral = new BorderPane();

        labelNombre = new Label("Nombre");
        labelCorreo = new Label("Correo");
        Image image = new Image("user-profile-icon.jpg");
        fotoPerfil = new ImageView(image);
        fotoPerfil.setFitWidth(80);
        fotoPerfil.setPreserveRatio(true);
        HBox perfil = new HBox();
        VBox nombreCorreo = new VBox();

        nombreCorreo.getChildren().addAll(
                labelNombre,
                labelCorreo
        );
        perfil.getChildren().addAll(
                fotoPerfil,
                nombreCorreo
        );

        buttonNuevaTarea = new Button("Nueva Tarea");
        buttonNuevaTarea.setOnAction(evt -> {
            nuevaTarea();
        });

        buttonGuardarEnServidor = new Button("Sincronizar");
        buttonGuardarEnServidor.setOnAction(evt -> {
            try {
                guardarTareasEnServidor();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        listaTareas = new ArrayList<>();
        items = FXCollections.observableArrayList(listaTareas);
        listView = new ListView<>(items);

        // Personalizar la celda para mostrar el título y el color de fondo
        listView.setCellFactory(param -> new ListCell<Tarea>() {
            @Override
            protected void updateItem(Tarea tarea, boolean empty) {
                super.updateItem(tarea, empty);
                if (empty || tarea == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(null); // Limpiar el estilo si la celda está vacía
                } else {
                    setText(null); // Limpiar el texto para usar un contenedor personalizado
                    HBox hbox = new HBox();
                    hbox.setSpacing(10); // Espacio entre los elementos del HBox
                    Label labelTitulo = new Label(tarea.getTitulo());
                    Label labelPrioridad = new Label("Prioridad: " + tarea.getPrioridad());
                    Button buttonBorrar = new Button("-");
                    buttonBorrar.setOnAction(event -> {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "¿Está seguro de que desea eliminar esta tarea?", ButtonType.YES, ButtonType.NO);
                        alert.showAndWait().ifPresent(response -> {
                            if (response == ButtonType.YES) {
                                items.remove(tarea);
                                tarea.setEstado("borrado");
                            }
                        });
                    });
                    hbox.getChildren().addAll(
                            labelTitulo,
                            labelPrioridad,
                            buttonBorrar
                    );
                    hbox.setBackground(new Background(new BackgroundFill(Color.web(tarea.getColor()), CornerRadii.EMPTY, Insets.EMPTY)));
                    setGraphic(hbox);
                }
            }
        });

        // Evento al seleccionar una celda
        listView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (tareaActual == null){
                    //si la tarea actual no contiene nada, a;adir la tarea seleccionada
                    tareaActual = newValue;
                    cargarTareaActual();
                } else {
                    //si la tarea actual ya contiene algo, guardar la tarea actual antes de cambiar la vista
                    guardarActual();
                    tareaActual = newValue;
                    cargarTareaActual();
                }
            }
        });

        menuLateral.setTop(perfil);

        VBox botonYLista = new VBox();
        botonYLista.getChildren().addAll(
                buttonNuevaTarea,
                listView
        );
        menuLateral.setCenter(botonYLista);

        menuLateral.setBottom(buttonGuardarEnServidor);

        setLeft(menuLateral);
    }

    public void guardarTareasEnServidor() throws IOException {
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("type", "saveData");

        // Filtrar las tareas que necesitan ser enviadas
        List<Tarea> tareasParaEnviar = listaTareas.stream()
                .filter(tarea -> !tarea.getEstado().equals("sinCambios"))
                .collect(Collectors.toList());

        // Convertir la lista filtrada a JSON
        JsonElement tareasJson = gson.toJsonTree(tareasParaEnviar);
        requestJson.add("tareas", tareasJson);

        String jsonString = gson.toJson(requestJson);
//        System.out.println(jsonString);

        // Enviar el JSON al servidor
        salida.write(jsonString + "\n");
        salida.flush();

        // Leer la respuesta del servidor
        String respuesta = entrada.readLine();
        JsonObject respuestaJson = gson.fromJson(respuesta, JsonObject.class);

        // Manejar la respuesta del servidor
        if (respuestaJson.get("status").getAsString().equals("success")) {
            // Cambiar el estado de las tareas a "sinCambios"
            for (Tarea tarea : tareasParaEnviar) {
                tarea.setEstado("sinCambios");
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Datos guardados correctamente.");
            alert.showAndWait();
        } else {
            String errorMessage = respuestaJson.has("message") ? respuestaJson.get("message").getAsString() : "Error desconocido.";
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error al guardar datos: " + errorMessage);
            alert.showAndWait();
        }
    }

    private void guardarActual() {
        if (tareaActual != null) {
            for (Tarea tarea: listaTareas){
                if (tarea.getId() == tareaActual.getId()) {
                    tarea.setTitulo(textFieldTitulo.getText().toString());
                    tarea.setDescripcion(textFieldDescripcion.getText().toString());
                    tarea.setPrioridad(comboBoxPrioridad.getValue());
                    tarea.setColor(colorHex);
//                    System.out.println(colorHex);
                    tarea.setFechaLimite(datePickerFecha.getValue());
                    if (!tarea.getEstado().equals("nueva")) {
                        tarea.setEstado("editada");
                    }
                    listView.refresh();
                }
            }
        } else {
            int id = 0;
            if (listaTareas.isEmpty()) {
                id = 1;
            } else {
                id = listaTareas.getLast().getId() + 1;
            }
            Tarea nuevaTarea = new Tarea(
                    id,
                    textFieldTitulo.getText().toString(),
                    textFieldDescripcion.getText().toString(),
                    datePickerFecha.getValue(),
                    comboBoxPrioridad.getValue(),
                    colorHex,
                    1,
                    "nueva"
            );
            listaTareas.add(nuevaTarea);
            items.add(nuevaTarea);
            tareaActual = nuevaTarea;
        }
    }


    private void cargarTareaActual() {
        if (tareaActual != null) {
            textFieldTitulo.setText(tareaActual.getTitulo());
            textFieldDescripcion.setText(tareaActual.getDescripcion());
            comboBoxPrioridad.setValue(tareaActual.getPrioridad());
            colorPicker.setValue(Color.web(tareaActual.getColor()));
            Color color = colorPicker.getValue();
            colorHex = String.format("#%02X%02X%02X",
                    (int) (color.getRed() * 255),
                    (int) (color.getGreen() * 255),
                    (int) (color.getBlue() * 255));
//            System.out.println(colorHex);
            datePickerFecha.setValue(tareaActual.getFechaLimite());
            vistaPrincipal.setStyle("-fx-background-color: " + tareaActual.getColor());
            textFieldTitulo.setStyle("-fx-control-inner-background: " + tareaActual.getColor());
            textFieldDescripcion.setStyle("-fx-control-inner-background: " + tareaActual.getColor());
        } else {
            textFieldTitulo.setText("");
            textFieldDescripcion.setText("");
            colorPicker.setValue(Color.WHITE);
            colorHex="#FFFFFF";
            datePickerFecha.setValue(LocalDate.now());
            vistaPrincipal.setStyle("-fx-background-color: #FFFFFF");
            textFieldTitulo.setStyle("-fx-control-inner-background: #FFFFFF");
            textFieldDescripcion.setStyle("-fx-control-inner-background: #FFFFFF");
        }
    }

    private void nuevaTarea() {
        if (tareaActual != null) {
            guardarActual();
            tareaActual = null;
            cargarTareaActual();
        } else {
            tareaActual = null;
            cargarTareaActual();
        }
    }

    private void iniciarVistaPrincipal() {
        vistaPrincipal = new BorderPane();

        textFieldTitulo = new TextField();
        textFieldTitulo.setPromptText("Agregar titulo...");
        textFieldDescripcion = new TextArea();
        textFieldDescripcion.setPromptText("Agregar descripcion...");
        comboBoxPrioridad = new ComboBox<>();
        comboBoxPrioridad.setValue("Baja");
        comboBoxPrioridad.getItems().addAll("Alta", "Media", "Baja");
        colorPicker = new ColorPicker();
        Label labelPrioridad = new Label("Prioridad: ");
        Label labelColor = new Label("Color: ");
        Label labelFecha = new Label("Vence: ");

        // Inicializa el DatePicker
        datePickerFecha = new DatePicker();
        datePickerFecha.setValue(LocalDate.now()); // Establece la fecha actual como valor predeterminado

        VBox encabezado = new VBox();
        HBox hBox = new HBox();
        encabezado.setSpacing(10); // Espacio entre los elementos del HBox
        encabezado.setAlignment(Pos.CENTER_LEFT); // Alineación izquierda

        hBox.getChildren().addAll(
                labelPrioridad,
                comboBoxPrioridad,
                labelColor,
                colorPicker,
                labelFecha,
                datePickerFecha
        );

        encabezado.getChildren().addAll(
                textFieldTitulo,
                hBox
        );

        buttonGuardar = new Button("Guardar");
//        buttonGuardar.setDisable(true);
        HBox herramientas = new HBox();

        herramientas.getChildren().addAll(
                buttonGuardar
        );

        colorPicker.setOnAction(event -> {
            Color color = colorPicker.getValue();
            colorHex = String.format("#%02X%02X%02X",
                    (int) (color.getRed() * 255),
                    (int) (color.getGreen() * 255),
                    (int) (color.getBlue() * 255));
            vistaPrincipal.setStyle("-fx-background-color: " + colorHex);
            textFieldTitulo.setStyle("-fx-control-inner-background: " + colorHex);
            textFieldDescripcion.setStyle("-fx-control-inner-background: " + colorHex);
        });

        buttonGuardar.setOnAction(evt -> {
            guardarActual();
        });

        vistaPrincipal.setTop(encabezado);
        vistaPrincipal.setCenter(textFieldDescripcion);
        vistaPrincipal.setBottom(herramientas);

        setCenter(vistaPrincipal);
    }
}
