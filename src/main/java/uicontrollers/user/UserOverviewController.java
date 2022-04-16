package uicontrollers.user;

import businessLogic.BlFacade;
import ui.MainGUI;
import uicontrollers.Controller;

public class UserOverviewController implements Controller {
    private BlFacade businessLogic;
    private MainGUI mainGUI;

    public UserOverviewController(BlFacade bl) {
        businessLogic = bl;
    }

    @Override
    public void setMainApp(MainGUI mainGUI) {
        this.mainGUI = mainGUI;
    }

    @Override
    public void redraw() {

    }
}
