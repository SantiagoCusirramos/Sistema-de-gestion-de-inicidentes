import Utils.ConexionBD;
import View.LoginView;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        ConexionBD.inicializarBaseDatos();
        LoginView loginView = new LoginView();
        loginView.mostrar(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}


