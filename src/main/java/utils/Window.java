package utils;

import javafx.scene.Parent;
import ui.MainGUI;
import uicontrollers.Controller;

/**
 * This represents a Window inside the stage, and holds both
 * the user interface and the controller.
 */
public class Window {
    private int width;
    private int height;
    private Controller controller;
    private Parent ui;

    /**
     * Constructor that initializes an empty window.
     */
    public Window() {
        this(MainGUI.SCENE_WIDTH, MainGUI.SCENE_HEIGHT, null, null);
    }

    /**
     * Constructor that initializes a sized window without controller or UI.
     * @param width the width of the window.
     * @param height the height of the window.
     */
    public Window(int width, int height) {
        this(width, height, null, null);
    }

    /**
     * Constructor that initializes a sized window with controller and UI.
     * @param width the width of the window.
     * @param height the height of the window.
     * @param controller the controller of the window.
     * @param ui the UI of the window.
     */
    public Window(int width, int height, Controller controller, Parent ui) {
        this.width = width;
        this.height = height;
        this.controller = controller;
        this.ui = ui;
    }

    /**
     * Retrieves the width of the window.
     * @return the width of the window.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Sets the width of the window.
     * @param width the width of the window.
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Retrieves the height of the window.
     * @return the height of the window.
     */
    public int getHeight() {
        return height;
    }

    /**
     * Sets the height of the window.
     * @param height the height of the window.
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Retrieves the controller of the window.
     * @return the controller of the window.
     */
    public Controller getController() {
        return controller;
    }

    /**
     * Sets the controller of the window.
     * @param c the controller of the window.
     */
    public void setController(Controller c) {
        this.controller = c;
    }

    /**
     * Retrieves the UI of the window.
     * @return the UI of the window.
     */
    public Parent getUi() {
        return ui;
    }

    /**
     * Sets the UI of the window.
     * @param ui the UI of the window.
     */
    public void setUi(Parent ui) {
        this.ui = ui;
    }
}