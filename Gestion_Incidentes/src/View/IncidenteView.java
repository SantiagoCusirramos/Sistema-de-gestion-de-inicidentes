package View;

import Controller.IncidenteController;
import Exceptions.IncidenteException;
import Exceptions.PermisoDenegadoException;
import Model.Incidente;
import Model.Usuario;
import Service.UsuarioService;
import enums.EstadoIncidente;
import enums.Prioridad;
import enums.RolUsuario;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Correcciones aplicadas:
 *  [SEC-005] listarComentarios ahora puede lanzar excepciones checked; se manejan en la UI.
 *  [SEC-012] Los campos de texto tienen límites visuales y se valida longitud antes de enviar.
 */
public class IncidenteView {

    private IncidenteController incidenteController;
    private UsuarioService usuarioService;
    private Usuario usuarioActual;
    private TableView<Incidente> tablaIncidentes;
    private ObservableList<Incidente> datosIncidentes;

    // Límites de longitud visibles al usuario [SEC-012]
    private static final int MAX_TITULO      = 200;
    private static final int MAX_DESCRIPCION = 5000;
    private static final int MAX_COMENTARIO  = 2000;
    private static final int MAX_SOLUCION    = 3000;

    public IncidenteView(Usuario usuarioActual) {
        this.usuarioActual       = usuarioActual;
        this.incidenteController = new IncidenteController(usuarioActual);
        this.usuarioService      = new UsuarioService();
    }

    public Node getContenido() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        Label lblTitulo = new Label("Gestion de Incidentes");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        root.getChildren().addAll(lblTitulo, crearBarraHerramientas(), crearTabla(), crearBotonera());
        return root;
    }

    private HBox crearBarraHerramientas() {
        HBox barra = new HBox(8);
        barra.setPadding(new Insets(5, 0, 5, 0));

        Button btnActualizar = new Button("Actualizar");
        btnActualizar.setStyle("-fx-background-color: #1565c0; -fx-text-fill: white;");
        btnActualizar.setOnAction(e -> cargarIncidentes());

        Button btnCrear = new Button("Nuevo Incidente");
        btnCrear.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white;");
        btnCrear.setOnAction(e -> mostrarDialogoCrear());

        Button btnVerDetalle = new Button("Ver Detalle");
        btnVerDetalle.setOnAction(e -> mostrarDetalle());

        barra.getChildren().addAll(btnActualizar, btnCrear, btnVerDetalle);

        if (usuarioActual.getRol() == RolUsuario.ADMIN) {
            Button btnAsignar = new Button("Asignar Tecnico");
            btnAsignar.setStyle("-fx-background-color: #e65100; -fx-text-fill: white;");
            btnAsignar.setOnAction(e -> mostrarDialogoAsignar());
            barra.getChildren().add(btnAsignar);
        }

        if (usuarioActual.getRol() == RolUsuario.TECNICO ||
            usuarioActual.getRol() == RolUsuario.ADMIN) {

            Button btnResolver = new Button("Resolver");
            btnResolver.setStyle("-fx-background-color: #00695c; -fx-text-fill: white;");
            btnResolver.setOnAction(e -> mostrarDialogoResolver());
            barra.getChildren().add(btnResolver);

            Button btnCambiarEstado = new Button("Cambiar Estado");
            btnCambiarEstado.setOnAction(e -> mostrarDialogoCambiarEstado());
            barra.getChildren().add(btnCambiarEstado);

            Button btnCerrar = new Button("Cerrar");
            btnCerrar.setStyle("-fx-background-color: #6a1b9a; -fx-text-fill: white;");
            btnCerrar.setOnAction(e -> mostrarDialogoCerrar());
            barra.getChildren().add(btnCerrar);
        }

        Button btnComentar = new Button("Agregar Comentario");
        btnComentar.setOnAction(e -> mostrarDialogoComentar());
        barra.getChildren().add(btnComentar);

        return barra;
    }

    @SuppressWarnings("unchecked")
    private TableView<Incidente> crearTabla() {
        tablaIncidentes = new TableView<>();
        tablaIncidentes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaIncidentes.setPlaceholder(new Label("No hay incidentes para mostrar"));

        TableColumn<Incidente, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(50);

        TableColumn<Incidente, String> colTitulo = new TableColumn<>("Titulo");
        colTitulo.setCellValueFactory(new PropertyValueFactory<>("titulo"));

        TableColumn<Incidente, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getEstado().name()));

        TableColumn<Incidente, String> colPrioridad = new TableColumn<>("Prioridad");
        colPrioridad.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getPrioridad().name()));

        TableColumn<Incidente, LocalDate> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(cd ->
            new SimpleObjectProperty<>(cd.getValue().getFechaCreacion().toLocalDate()));

        TableColumn<Incidente, String> colTecnico = new TableColumn<>("Tecnico");
        colTecnico.setCellValueFactory(cd -> {
            Integer tId = cd.getValue().getTecnicoId();
            return new SimpleStringProperty(tId != null ? String.valueOf(tId) : "Sin asignar");
        });

        tablaIncidentes.getColumns().addAll(colId, colTitulo, colEstado, colPrioridad, colFecha, colTecnico);

        datosIncidentes = FXCollections.observableArrayList();
        tablaIncidentes.setItems(datosIncidentes);

        cargarIncidentes();
        return tablaIncidentes;
    }

    private HBox crearBotonera() {
        HBox barra = new HBox(10);
        barra.setPadding(new Insets(10, 0, 0, 0));
        barra.setAlignment(Pos.CENTER_RIGHT);

        Button btnTodos = new Button("Ver Todos");
        btnTodos.setOnAction(e -> cargarIncidentes());
        barra.getChildren().add(btnTodos);
        return barra;
    }

    private void cargarIncidentes() {
        List<Incidente> incidentes = incidenteController.listarIncidentes();
        datosIncidentes.setAll(incidentes);
    }

    private Incidente getIncidenteSeleccionado() {
        Incidente inc = tablaIncidentes.getSelectionModel().getSelectedItem();
        if (inc == null) mostrarAlerta("Seleccion", "Seleccione un incidente de la tabla.");
        return inc;
    }

    private void mostrarDialogoCrear() {
        Dialog<Incidente> dialogo = new Dialog<>();
        dialogo.setTitle("Nuevo Incidente");
        dialogo.setHeaderText("Crear un nuevo incidente");

        ButtonType btnGuardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField txtTitulo = new TextField();
        txtTitulo.setPromptText("Max. " + MAX_TITULO + " caracteres");

        TextArea txtDescripcion = new TextArea();
        txtDescripcion.setPromptText("Max. " + MAX_DESCRIPCION + " caracteres");
        txtDescripcion.setPrefRowCount(4);

        ComboBox<Prioridad> cmbPrioridad = new ComboBox<>(
            FXCollections.observableArrayList(Prioridad.values()));
        cmbPrioridad.setValue(Prioridad.MEDIA);

        // [SEC-012] Etiquetas de contador de caracteres
        Label lblContadorTitulo = new Label("0/" + MAX_TITULO);
        txtTitulo.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = newVal.length();
            lblContadorTitulo.setText(len + "/" + MAX_TITULO);
            if (len > MAX_TITULO) {
                txtTitulo.setText(oldVal);
                lblContadorTitulo.setText(oldVal.length() + "/" + MAX_TITULO);
            }
        });

        grid.add(new Label("Titulo:"), 0, 0);
        grid.add(txtTitulo, 1, 0);
        grid.add(lblContadorTitulo, 2, 0);
        grid.add(new Label("Descripcion:"), 0, 1);
        grid.add(txtDescripcion, 1, 1);
        grid.add(new Label("Prioridad:"), 0, 2);
        grid.add(cmbPrioridad, 1, 2);

        dialogo.getDialogPane().setContent(grid);

        dialogo.setResultConverter(dialogButton -> {
            if (dialogButton == btnGuardar) {
                Incidente inc = new Incidente();
                inc.setTitulo(txtTitulo.getText().trim());
                inc.setDescripcion(txtDescripcion.getText().trim());
                inc.setPrioridad(cmbPrioridad.getValue());
                return inc;
            }
            return null;
        });

        Optional<Incidente> resultado = dialogo.showAndWait();
        resultado.ifPresent(inc -> {
            try {
                incidenteController.crearIncidente(
                    inc.getTitulo(), inc.getDescripcion(), inc.getPrioridad());
                cargarIncidentes();
                mostrarInfo("Incidente creado exitosamente.");
            } catch (IncidenteException | PermisoDenegadoException ex) {
                mostrarError("Error al crear incidente", ex.getMessage());
            }
        });
    }

    private void mostrarDetalle() {
        Incidente inc = getIncidenteSeleccionado();
        if (inc == null) return;

        Dialog<Void> dialogo = new Dialog<>();
        dialogo.setTitle("Detalle del Incidente #" + inc.getId());
        dialogo.setHeaderText(inc.getTitulo());
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox contenido = new VBox(8);
        contenido.setPadding(new Insets(20));

        contenido.getChildren().addAll(
            new Label("ID: " + inc.getId()),
            new Label("Descripcion: " + inc.getDescripcion()),
            new Label("Estado: " + inc.getEstado().name()),
            new Label("Prioridad: " + inc.getPrioridad().name() + " - " + inc.getPrioridad().getDescripcion()),
            new Label("Fecha creacion: " + inc.getFechaCreacion()),
            new Label("Usuario ID: " + inc.getUsuarioId()),
            new Label("Tecnico ID: " + (inc.getTecnicoId() != null ? inc.getTecnicoId() : "Sin asignar"))
        );

        if (inc.getFechaActualizacion() != null) {
            contenido.getChildren().add(
                new Label("Ultima actualizacion: " + inc.getFechaActualizacion()));
        }

        Label lblComentarios = new Label("\nComentarios:");
        lblComentarios.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        contenido.getChildren().add(lblComentarios);

        // [SEC-005] listarComentarios ahora puede lanzar excepciones: se manejan aquí.
        try {
            List<String> comentarios = incidenteController.listarComentarios(inc.getId());
            if (comentarios.isEmpty()) {
                contenido.getChildren().add(new Label("  (Sin comentarios)"));
            } else {
                for (String c : comentarios) {
                    Label lblC = new Label("  " + c);
                    lblC.setWrapText(true);
                    contenido.getChildren().add(lblC);
                }
            }
        } catch (IncidenteException | PermisoDenegadoException ex) {
            contenido.getChildren().add(new Label("  No tiene permiso para ver los comentarios."));
        }

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(400);
        dialogo.getDialogPane().setContent(scroll);
        dialogo.showAndWait();
    }

    private void mostrarDialogoComentar() {
        Incidente inc = getIncidenteSeleccionado();
        if (inc == null) return;

        TextInputDialog dialogo = new TextInputDialog();
        dialogo.setTitle("Agregar Comentario");
        dialogo.setHeaderText("Comentario para Incidente #" + inc.getId() +
                              " (max. " + MAX_COMENTARIO + " caracteres)");
        dialogo.setContentText("Mensaje:");

        Optional<String> resultado = dialogo.showAndWait();
        resultado.ifPresent(mensaje -> {
            // [SEC-012] Validación de longitud en la UI antes de enviar
            if (mensaje.trim().length() > MAX_COMENTARIO) {
                mostrarError("Error", "El comentario no puede superar " + MAX_COMENTARIO + " caracteres.");
                return;
            }
            try {
                incidenteController.agregarComentario(inc.getId(), mensaje);
                mostrarInfo("Comentario agregado.");
            } catch (IncidenteException | PermisoDenegadoException ex) {
                mostrarError("Error", ex.getMessage());
            }
        });
    }

    private void mostrarDialogoAsignar() {
        Incidente inc = getIncidenteSeleccionado();
        if (inc == null) return;

        List<Usuario> tecnicos = usuarioService.listarTecnicos();
        if (tecnicos.isEmpty()) {
            mostrarAlerta("Sin tecnicos", "No hay tecnicos disponibles para asignar.");
            return;
        }

        ChoiceDialog<Usuario> dialogo = new ChoiceDialog<>(tecnicos.get(0), tecnicos);
        dialogo.setTitle("Asignar Tecnico");
        dialogo.setHeaderText("Asignar tecnico al Incidente #" + inc.getId());
        dialogo.setContentText("Seleccione tecnico:");

        Optional<Usuario> resultado = dialogo.showAndWait();
        resultado.ifPresent(tecnico -> {
            try {
                incidenteController.asignarTecnico(inc.getId(), tecnico.getId());
                cargarIncidentes();
                mostrarInfo("Tecnico " + tecnico.getNombre() + " asignado.");
            } catch (IncidenteException | PermisoDenegadoException ex) {
                mostrarError("Error", ex.getMessage());
            }
        });
    }

    private void mostrarDialogoResolver() {
        Incidente inc = getIncidenteSeleccionado();
        if (inc == null) return;

        TextInputDialog dialogo = new TextInputDialog();
        dialogo.setTitle("Resolver Incidente");
        dialogo.setHeaderText("Resolver Incidente #" + inc.getId() +
                              " (max. " + MAX_SOLUCION + " caracteres)");
        dialogo.setContentText("Describa la solucion:");

        Optional<String> resultado = dialogo.showAndWait();
        resultado.ifPresent(solucion -> {
            if (solucion.trim().length() > MAX_SOLUCION) {
                mostrarError("Error", "La solución no puede superar " + MAX_SOLUCION + " caracteres.");
                return;
            }
            try {
                incidenteController.resolverIncidente(inc.getId(), solucion);
                cargarIncidentes();
                mostrarInfo("Incidente resuelto.");
            } catch (IncidenteException | PermisoDenegadoException ex) {
                mostrarError("Error", ex.getMessage());
            }
        });
    }

    private void mostrarDialogoCerrar() {
        Incidente inc = getIncidenteSeleccionado();
        if (inc == null) return;

        Alert dialogo = new Alert(Alert.AlertType.CONFIRMATION);
        dialogo.setTitle("Cerrar Incidente");
        dialogo.setHeaderText("Confirmar cierre del Incidente #" + inc.getId());
        dialogo.setContentText("Esta seguro de que desea cerrar este incidente?");

        Optional<ButtonType> resultado = dialogo.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            try {
                incidenteController.cerrarIncidente(inc.getId());
                cargarIncidentes();
                mostrarInfo("Incidente cerrado.");
            } catch (IncidenteException | PermisoDenegadoException ex) {
                mostrarError("Error", ex.getMessage());
            }
        }
    }

    private void mostrarDialogoCambiarEstado() {
        Incidente inc = getIncidenteSeleccionado();
        if (inc == null) return;

        ChoiceDialog<EstadoIncidente> dialogo = new ChoiceDialog<>(
            inc.getEstado(),
            FXCollections.observableArrayList(EstadoIncidente.values()));
        dialogo.setTitle("Cambiar Estado");
        dialogo.setHeaderText("Cambiar estado del Incidente #" + inc.getId());
        dialogo.setContentText("Nuevo estado:");

        Optional<EstadoIncidente> resultado = dialogo.showAndWait();
        resultado.ifPresent(estado -> {
            try {
                incidenteController.cambiarEstado(inc.getId(), estado);
                cargarIncidentes();
                mostrarInfo("Estado cambiado a: " + estado.name());
            } catch (IncidenteException | PermisoDenegadoException ex) {
                mostrarError("Error", ex.getMessage());
            }
        });
    }

    // -------------------------------------------------------------------------
    // Helpers de alertas
    // -------------------------------------------------------------------------

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarInfo(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informacion");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
