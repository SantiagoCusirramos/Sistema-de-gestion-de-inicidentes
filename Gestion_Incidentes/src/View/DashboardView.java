package View;

import Model.Usuario;
import enums.RolUsuario;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
        Label lblUsuario = new Label("Usuario: " + usuarioActual.getNombre());
        lblUsuario.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        Label lblRol = new Label("Rol: " + usuarioActual.getRol().name());
        lblRol.setStyle("-fx-text-fill: #1565c0; -fx-font-weight: bold;");

        Label lblDashboard = new Label("Panel de Control");
        lblDashboard.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        lblDashboard.setStyle("-fx-text-fill: #1a237e;");

        Button btnLogout = new Button("Cerrar Sesion");
        btnLogout.setStyle("-fx-background-color: #c62828; -fx-text-fill: white;");
        btnLogout.setOnAction(e -> {
            LoginView loginView = new LoginView();
            loginView.mostrar(new Stage());
            stage.close();
        });

        HBox left = new HBox(15, lblUsuario, lblRol);
        left.setAlignment(Pos.CENTER_LEFT);

        HBox right = new HBox(15, btnLogout);
        right.setAlignment(Pos.CENTER_RIGHT);

        BorderPane bar = new BorderPane();
        bar.setLeft(left);
        bar.setCenter(lblDashboard);
        bar.setRight(right);
        bar.setPadding(new Insets(15));
        bar.setStyle("-fx-background-color: #e8eaf6; -fx-border-color: #c5cae9; -fx-border-width: 0 0 2 0;");
        return new HBox(bar);
    }

    private TabPane crearTabs() {
        TabPane tabPane = new TabPane();
        tabPane.setTabMinWidth(150);

        Tab tabIncidentes = new Tab("Incidentes");
        tabIncidentes.setClosable(false);
        tabIncidentes.setContent(incidenteView.getContenido());

        Tab tabReportes = new Tab("Reportes");
        tabReportes.setClosable(false);
        tabReportes.setContent(reporteView.getContenido());

        tabPane.getTabs().addAll(tabIncidentes, tabReportes);

        if (usuarioActual.getRol() == RolUsuario.ADMIN) {
            Tab tabUsuarios = new Tab("Usuarios");
            tabUsuarios.setClosable(false);
            tabUsuarios.setContent(crearPanelUsuarios());
            tabPane.getTabs().add(tabUsuarios);
        }

        return tabPane;
    }

    private VBox crearPanelUsuarios() {
        Label lblTitle = new Label("Administracion de Usuarios");
        lblTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        Label lblInfo = new Label("Funcionalidad en desarrollo.\n\n"
                + "Proximamente: crear, editar y eliminar usuarios del sistema.");
        lblInfo.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        VBox panel = new VBox(20, lblTitle, lblInfo);
        panel.setPadding(new Insets(30));
        panel.setAlignment(Pos.TOP_CENTER);
        return panel;
    }
}
