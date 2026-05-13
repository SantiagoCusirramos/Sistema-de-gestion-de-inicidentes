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
