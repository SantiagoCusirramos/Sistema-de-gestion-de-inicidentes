import View.LoginView;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        LoginView loginView = new LoginView();
        loginView.mostrar(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
