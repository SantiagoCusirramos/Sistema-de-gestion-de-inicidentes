package View;

import Exceptions.UsuarioException;
import Model.Usuario;
import Service.UsuarioService;
import enums.RolUsuario;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class DashboardView {

    private Usuario usuarioActual;
    private IncidenteView incidenteView;
    private ReporteView reporteView;

    public DashboardView(Usuario usuarioActual) {
        this.usuarioActual = usuarioActual;
        this.incidenteView = new IncidenteView(usuarioActual);
        this.reporteView = new ReporteView(usuarioActual);
    }

    public void mostrar(Stage stage) {
        BorderPane root = new BorderPane();
        root.setTop(crearBarraSuperior(stage));
        root.setCenter(crearTabs());

        Scene scene = new Scene(root, 1000, 700);
        stage.setTitle("Dashboard - " + usuarioActual.getNombre());
        stage.setScene(scene);
        stage.show();
    }

    private HBox crearBarraSuperior(Stage stage) {
        VBox infoUsuario = new VBox(2);
        infoUsuario.setPadding(new Insets(8, 15, 8, 15));
        infoUsuario.setStyle("-fx-background-color: #1a237e; -fx-background-radius: 6;");

        Label lblNombre = new Label(usuarioActual.getNombre());
        lblNombre.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        lblNombre.setStyle("-fx-text-fill: white;");

        Label lblEmail = new Label(usuarioActual.getEmail());
        lblEmail.setFont(Font.font("Arial", 11));
        lblEmail.setStyle("-fx-text-fill: #bbdefb;");

        Label lblRol = new Label("Rol: " + usuarioActual.getRol().name());
        lblRol.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        lblRol.setStyle("-fx-text-fill: #ffd54f;");

        infoUsuario.getChildren().addAll(lblNombre, lblEmail, lblRol);

        Label lblDashboard = new Label("Panel de Control");
        lblDashboard.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        lblDashboard.setStyle("-fx-text-fill: #1a237e;");

        Button btnLogout = new Button("Cerrar Sesion");
        btnLogout.setStyle("-fx-background-color: #c62828; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 4;");
        btnLogout.setOnAction(e -> {
            LoginView loginView = new LoginView();
            loginView.mostrar(new Stage());
            stage.close();
        });

        HBox barra = new HBox(20);
        barra.setPadding(new Insets(10, 20, 10, 20));
        barra.setStyle("-fx-background-color: #e8eaf6; -fx-border-color: #c5cae9; -fx-border-width: 0 0 2 0;");
        barra.setAlignment(Pos.CENTER);
        barra.getChildren().addAll(infoUsuario, lblDashboard, btnLogout);
        HBox.setHgrow(lblDashboard, javafx.scene.layout.Priority.ALWAYS);
        lblDashboard.setAlignment(Pos.CENTER);
        return barra;
    }

    private TabPane crearTabs() {
        TabPane tabPane = new TabPane();
        tabPane.setTabMinWidth(150);

        Tab tabIncidentes = new Tab("Incidentes");
        tabIncidentes.setClosable(false);
        tabIncidentes.setContent(incidenteView.getContenido());

        tabPane.getTabs().add(tabIncidentes);

        if (usuarioActual.getRol() != RolUsuario.USUARIO) {
            Tab tabReportes = new Tab("Reportes");
            tabReportes.setClosable(false);
            tabReportes.setContent(reporteView.getContenido());
            tabPane.getTabs().add(tabReportes);
        }

        if (usuarioActual.getRol() == RolUsuario.ADMIN) {
            Tab tabUsuarios = new Tab("Usuarios");
            tabUsuarios.setClosable(false);
            tabUsuarios.setContent(crearPanelUsuarios());
            tabPane.getTabs().add(tabUsuarios);
        }

        return tabPane;
    }

    private VBox crearPanelUsuarios() {
        UsuarioService usuarioService = new UsuarioService();

        Label lblTitle = new Label("Administracion de Usuarios");
        lblTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        TableView<Usuario> tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("No hay usuarios registrados"));

        TableColumn<Usuario, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(50);

        TableColumn<Usuario, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));

        TableColumn<Usuario, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<Usuario, String> colRol = new TableColumn<>("Rol");
        colRol.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getRol().name()));

        tabla.getColumns().addAll(colId, colNombre, colEmail, colRol);

        ObservableList<Usuario> datos = FXCollections.observableArrayList();
        tabla.setItems(datos);

        HBox barra = new HBox(8);
        barra.setPadding(new Insets(5, 0, 10, 0));

        Button btnActualizar = new Button("Actualizar");
        btnActualizar.setStyle("-fx-background-color: #1565c0; -fx-text-fill: white;");
        btnActualizar.setOnAction(e -> cargarUsuarios(tabla, datos, usuarioService));

        Button btnTecnico = new Button("Asignar Tecnico");
        btnTecnico.setStyle("-fx-background-color: #e65100; -fx-text-fill: white;");
        btnTecnico.setOnAction(e -> cambiarRol(tabla, usuarioService, RolUsuario.TECNICO));

        Button btnRemover = new Button("Remover Tecnico");
        btnRemover.setStyle("-fx-background-color: #6a1b9a; -fx-text-fill: white;");
        btnRemover.setOnAction(e -> cambiarRol(tabla, usuarioService, RolUsuario.USUARIO));

        barra.getChildren().addAll(btnActualizar, btnTecnico, btnRemover);
        cargarUsuarios(tabla, datos, usuarioService);

        VBox panel = new VBox(10, lblTitle, barra, tabla);
        panel.setPadding(new Insets(20));
        return panel;
    }

    private void cargarUsuarios(TableView<Usuario> tabla, ObservableList<Usuario> datos,
                                 UsuarioService usuarioService) {
        datos.setAll(usuarioService.listarTodosUsuarios());
    }

    private void cambiarRol(TableView<Usuario> tabla, UsuarioService usuarioService,
                             RolUsuario nuevoRol) {
        Usuario seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Seleccion");
            alert.setHeaderText(null);
            alert.setContentText("Seleccione un usuario de la tabla.");
            alert.showAndWait();
            return;
        }

        if (seleccionado.getId() == usuarioActual.getId()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Accion no permitida");
            alert.setHeaderText(null);
            alert.setContentText("No puede cambiar su propio rol.");
            alert.showAndWait();
            return;
        }

        String accion = (nuevoRol == RolUsuario.TECNICO) ? "asignar como tecnico" : "remover rol tecnico";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar cambio de rol");
        confirm.setHeaderText("Usuario: " + seleccionado.getNombre() + " (" + seleccionado.getEmail() + ")");
        confirm.setContentText("Esta seguro de que desea " + accion + "?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            usuarioService.actualizarUsuario(
                    seleccionado.getId(),
                    seleccionado.getNombre(),
                    seleccionado.getEmail(),
                    nuevoRol);

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Rol actualizado");
            info.setHeaderText(null);
            info.setContentText("El rol de " + seleccionado.getNombre() + " ahora es " + nuevoRol.name() + ".");
            info.showAndWait();

            cargarUsuarios(tabla, tabla.getItems(), usuarioService);
        } catch (UsuarioException e) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Error");
            error.setHeaderText(null);
            error.setContentText(e.getMessage());
            error.showAndWait();
        }
    }
}
