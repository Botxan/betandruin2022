package ui;

import businessLogic.BlFacade;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.bootstrapfx.BootstrapFX;
import uicontrollers.*;
import utils.History;
import utils.Window;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

public class MainGUI {

    private BorderPane mainWrapper;

    private Window navBar, loginLag, registerLag, mainLag, createQuestionLag, browseQuestionsLag;

    private BlFacade businessLogic;
    private Stage stage;
    private Scene scene;

    // Default scene resolution
    public static final int NAVBAR_HEIGHT = 64;
    public static final int SCENE_WIDTH = 1280;
    public static final int SCENE_HEIGHT = 720-NAVBAR_HEIGHT;

    // The history
    private History history;

    public BlFacade getBusinessLogic() {
        return businessLogic;
    }

    public void setBusinessLogic(BlFacade afi) {
        businessLogic = afi;
    }

    /**
     * Constructor for the main GUI class.
     * Sets the business logic, loads all the windows and
     * displays the main GUI in the scene.
     * @param bl the business logic.
     */
    public MainGUI(BlFacade bl) {
        Platform.startup(() -> {
            try {
                setBusinessLogic(bl);
                history = new History();
                init(new Stage());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Creates a new window and assigns its corresponding value
     * UI and Controller
     * @param fxmlfile the name of the fxml file
     * @return the new window
     * @throws IOException in case de load.loader() fails.
     */
    private Window load(String fxmlfile, String title, int width, int height) throws IOException {
        Window window = new Window(title, width, height);
        FXMLLoader loader = new FXMLLoader(MainGUI.class.getResource(fxmlfile), ResourceBundle.getBundle("Etiquetas", Locale.getDefault()));
        loader.setControllerFactory(controllerClass -> {
            if (controllerClass == NavBarController.class) {
                return new NavBarController(businessLogic);
            } else if (controllerClass == BrowseEventsController.class) {
                return new BrowseEventsController(businessLogic);
            } else if (controllerClass == CreateQuestionController.class) {
                return new CreateQuestionController(businessLogic);
            } else if (controllerClass == LoginController.class) {
                return new LoginController(businessLogic);
            } else if (controllerClass == RegisterController.class) {
                return new RegisterController(businessLogic);
            } else {
                // default behavior for controllerFactory:
                try {
                    return controllerClass.getDeclaredConstructor().newInstance();
                } catch (Exception exc) {
                    exc.printStackTrace();
                    throw new RuntimeException(exc); // fatal, just bail...
                }
            }
        });
        window.setUi(loader.load());
        ((Controller) loader.getController()).setMainApp(this);
        window.setController(loader.getController());
        return window;
    }

    /**
     * Initializes all the windows used in the app.
     * @param stage the stage of the project.
     * @throws IOException thrown if any fxml file is not found.
     */
    public void init(Stage stage) throws IOException {
        this.stage = stage;
        this.stage.initStyle(StageStyle.UNDECORATED);

        navBar = load("/NavBarGUI.fxml", "NavBar",  SCENE_WIDTH, SCENE_HEIGHT);
        loginLag = load("/LoginGUI.fxml", "Login", SCENE_WIDTH, SCENE_HEIGHT);
        registerLag = load("/RegisterGUI.fxml", "Register", SCENE_WIDTH, SCENE_HEIGHT);
        mainLag = load("/MainGUI.fxml", "MainTitle", SCENE_WIDTH, SCENE_HEIGHT);
        browseQuestionsLag = load("/BrowseEvents.fxml", "BrowseQuestions", SCENE_WIDTH, SCENE_HEIGHT);
        createQuestionLag = load("/CreateQuestion.fxml", "CreateQuestion", SCENE_WIDTH, SCENE_HEIGHT);

        setupScene();
        ResizeHelper.addResizeListener(this.stage);
        history.setCurrentWindow(mainLag);
        showScene(mainLag);

    }

    /**
     * Prepares the window with the navigation bar so that it is possible
     * to navigate between windows without removing the navigation bar.
     */
    private void setupScene() {
        // Initialize the wrapper for the navbar and the content
        mainWrapper = new BorderPane();

        // Add the navbar to the wrapper
        mainWrapper.setTop(navBar.getUi());

        // Initialize the scene
        scene = new Scene(mainWrapper, SCENE_WIDTH, SCENE_HEIGHT);
        scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());

        // Import Roboto fonts
        scene.getStylesheets().add("https://fonts.googleapis.com/css2?family=Roboto:ital,wght@0,100;0,300;0,400;0,500;0,700;0,900;1,100;1,300;1,400;1,500;1,700;1,900&display=swap");
        // Global font css
        scene.getStylesheets().add(getClass().getResource("/css/fonts.css").toExternalForm());

        // Add the wrapper of the navbar and the content to the scene
        scene.setRoot(mainWrapper);

        // Add the scene to the root
        stage.setScene(scene);
    }

    /**
     * Displays the given window in the scene.
     * @param window the window.
     */
    private void showScene(Window window) {
        stage.setTitle(ResourceBundle.getBundle("Etiquetas", Locale.getDefault()).getString(window.getTitle()));
        stage.setWidth(window.getWidth());
        stage.setHeight(window.getHeight());

        mainWrapper.setCenter(window.getUi());
        ((NavBarController)navBar.getController()).updateNav();
        stage.show();
    }

    /**
     * Changes the stage to the last visited window.
     */
    public void goBack() {
        Window previousWindow = history.moveToPrevious();
        ((NavBarController) navBar.getController()).enableHistoryBtns();
        if (previousWindow != null) showScene(previousWindow);
    }

    /**
     * Stores the current window in the history and displays
     * the next window
     */
    public void goForward() {
        Window nextWindow = history.moveToNext();
        ((NavBarController) navBar.getController()).enableHistoryBtns();
        if (nextWindow != null) showScene(nextWindow);
    }
    /**
     * Stores the current window in the history and displays
     * the window with the given title.
     * @param title the title of the window.
     */
    public void goForward(String title) {
        // Get the new window
        Window newWindow = getWindow(title);
        // Move to the requested window and store the old one
        history.moveToWindow(newWindow);

        ((NavBarController) navBar.getController()).enableHistoryBtns();
        showScene(newWindow);
    }

    /**
     * Returns the window with the given title
     * @param title the title of the window.
     */
    public Window getWindow(String title) {
        return switch(title) {
            case "Login":
                yield loginLag;
            case "Register":
                yield registerLag;
            case "BrowseQuestions":
                yield browseQuestionsLag;
            case "CreateQuestion":
                yield createQuestionLag;
            default: // get the initial window
                yield mainLag;
        };
    }

    /**
     * Returns the history.
     * @return the history.
     */
    public History getHistory() {
        return history;
    }
}
