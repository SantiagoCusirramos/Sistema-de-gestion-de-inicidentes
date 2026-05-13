package View;

import Controller.AuthController;
import Exceptions.UsuarioException;
import Model.Usuario;
import Service.UsuarioService;
import Utils.Validador;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class LoginView {

    private AuthController authController;
    private Validador validador;

    public LoginView() {
        this.authController = new AuthController();
        this.validador = new Validador();
    }

    public void mostrar(Stage stage) {
        TabPane tabPane = new TabPane();

        Tab loginTab = new Tab("Iniciar Sesion");
        loginTab.setClosable(false);
        loginTab.setContent(crearLoginPanel(stage));

        Tab registerTab = new Tab("Registrarse");
        registerTab.setClosable(false);
        registerTab.setContent(crearRegisterPanel());

        tabPane.getTabs().addAll(loginTab, registerTab);

        VBox root = new VBox(10, crearEncabezado("Sistema de Gestion de Incidentes"), tabPane);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);

        Scene scene = new Scene(root, 480, 420);
        stage.setTitle("Sistema de Gestion de Incidentes");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private Label crearEncabezado(String texto) {
        Label label = new Label(texto);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        label.setStyle("-fx-text-fill: #1a237e;");
        return label;
    }

    private VBox crearLoginPanel(Stage stage) {
        Label lblEmail = new Label("Email:");
        TextField txtEmail = new TextField();
        txtEmail.setPromptText("correo@ejemplo.com");

        Label lblPassword = new Label("Contrasena:");
        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText("Ingrese su contrasena");

        Label lblMensaje = new Label();
        lblMensaje.setStyle("-fx-text-fill: red;");

        Button btnLogin = new Button("Iniciar Sesion");
        btnLogin.setDefaultButton(true);
        btnLogin.setStyle("-fx-background-color: #1a237e; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 30;");
        btnLogin.setOnAction(e -> {
            String email = txtEmail.getText().trim();
            String password = txtPassword.getText();

            if (!validador.validarEmail(email)) {
                lblMensaje.setText("Formato de email invalido");
                return;
            }
            if (!validador.validarPassword(password)) {
                lblMensaje.setText("La contrasena debe tener al menos 6 caracteres");
                return;
            }

            if (authController.login(email, password)) {
                Usuario usuario = authController.getUsuarioActual();
                DashboardView dashboard = new DashboardView(usuario);
                dashboard.mostrar(new Stage());
                stage.close();
            } else {
                lblMensaje.setText("Email o contrasena incorrectos");
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setAlignment(Pos.CENTER);
        grid.add(lblEmail, 0, 0);
        grid.add(txtEmail, 1, 0);
        grid.add(lblPassword, 0, 1);
        grid.add(txtPassword, 1, 1);
        grid.add(btnLogin, 1, 2);
        GridPane.setMargin(btnLogin, new Insets(10, 0, 0, 0));

        VBox panel = new VBox(15, grid, lblMensaje);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(40, 20, 20, 20));
        return panel;
    }

    private VBox crearRegisterPanel() {
        Label lblNombre = new Label("Nombre:");
        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Nombre completo");

        Label lblEmail = new Label("Email:");
        TextField txtEmail = new TextField();
        txtEmail.setPromptText("correo@ejemplo.com");

        Label lblPassword = new Label("Contrasena:");
        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText("Min. 6 caracteres");

        Label lblConfirmar = new Label("Confirmar:");
        PasswordField txtConfirmar = new PasswordField();
        txtConfirmar.setPromptText("Repita la contrasena");

        Label lblTelefono = new Label("Telefono:");
        TextField txtTelefono = new TextField();
        txtTelefono.setPromptText("Opcional");

        Label lblMensaje = new Label();
        lblMensaje.setStyle("-fx-text-fill: red;");

        Button btnRegister = new Button("Registrarse");
        btnRegister.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 30;");
        btnRegister.setOnAction(e -> {
            String nombre = txtNombre.getText().trim();
            String email = txtEmail.getText().trim();
            String password = txtPassword.getText();
            String confirmar = txtConfirmar.getText();
            String telefono = txtTelefono.getText().trim();

            if (!validador.validarNombre(nombre)) {
                lblMensaje.setText("Nombre invalido");
                return;
            }
            if (!validador.validarEmail(email)) {
                lblMensaje.setText("Formato de email invalido");
                return;
            }
            if (!validador.validarPassword(password)) {
                lblMensaje.setText("La contrasena debe tener al menos 6 caracteres");
                return;
            }
            if (!password.equals(confirmar)) {
                lblMensaje.setText("Las contrasenas no coinciden");
                return;
            }

            if (authController.registrar(nombre, email, password, "usuario")) {
                lblMensaje.setStyle("-fx-text-fill: green;");
                lblMensaje.setText("Registro exitoso. Ahora puede iniciar sesion.");
                txtNombre.clear();
                txtEmail.clear();
                txtPassword.clear();
                txtConfirmar.clear();
                txtTelefono.clear();
            } else {
                lblMensaje.setStyle("-fx-text-fill: red;");
                lblMensaje.setText("Error al registrarse. El email podria ya estar en uso.");
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);
        grid.add(lblNombre, 0, 0);
        grid.add(txtNombre, 1, 0);
        grid.add(lblEmail, 0, 1);
        grid.add(txtEmail, 1, 1);
        grid.add(lblPassword, 0, 2);
        grid.add(txtPassword, 1, 2);
        grid.add(lblConfirmar, 0, 3);
        grid.add(txtConfirmar, 1, 3);
        grid.add(lblTelefono, 0, 4);
        grid.add(txtTelefono, 1, 4);
        grid.add(btnRegister, 1, 5);
        GridPane.setMargin(btnRegister, new Insets(10, 0, 0, 0));

        VBox panel = new VBox(15, grid, lblMensaje);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(20));
        return panel;
    }
}
