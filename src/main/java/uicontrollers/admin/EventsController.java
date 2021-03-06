package uicontrollers.admin;

import businessLogic.BlFacade;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import domain.Event;
import domain.Match;
import exceptions.EventAlreadyExistException;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.skin.DatePickerSkin;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import ui.MainGUI;
import uicontrollers.Controller;
import uicontrollers.NavBarController;
import utils.Dates;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * This class serves as a controller for the event section of administrator dashboard.css.
 * It is mainly used to control the operations carried out related to events (such as creation,
 * modification, publishing results...).
 * @author Josefinators
 * @version v1
 */
public class EventsController implements Controller {
    private BlFacade businessLogic;
    private MainGUI mainGUI;
    private List<LocalDate> holidays = new ArrayList<>();
    private ObservableList<Event> events;
    private StackPane createEventOverlay;
    private JFXDialog createEventDialog;
    private TableColumn<Event, Integer> questionsCol;
    private TableColumn<Event, Date> actionCol;

    @FXML private AnchorPane mainPane;
    @FXML private DatePicker datePicker;
    @FXML private DatePicker datePicker1;
    @FXML private JFXButton createEventBtn;
    @FXML private JFXButton addEventBtn;
    @FXML private JFXButton backBtn;
    @FXML private Label countryLbl;
    @FXML private Label eventDateLbl;
    @FXML private Label eventNameLbl;
    @FXML private Label resultLabel;
    @FXML private Pane createEventPane;
    @FXML private TableColumn<Event, String> countryCol;
    @FXML private TableColumn<Event, String> dateCol;
    @FXML private TableColumn<Event, String> descriptionCol;
    @FXML private TableColumn<Event, Integer> idCol;
    @FXML private TableView<Event> eventsTbl;
    @FXML private TextField countryField;
    @FXML private TextField eventNameField;
    @FXML private TextField searchField;

    /**
     * Constructor for the EventsController.
     * Initializes the business logic.
     * @param bl the busines logic.
     */
    public EventsController(BlFacade bl) {
        businessLogic = bl;
    }

    @FXML
    void initialize() {
        initEventsTable();
        initDatePicker();
        initCreateEventDialog();
    }

    private void initDatePicker() {
        holidays.clear();
        setEventsPrePost(LocalDate.now().getYear(), LocalDate.now().getMonth().getValue());

        // get a reference to datepicker inner content
        // attach a listener to the  << and >> buttons
        // mark events for the (prev, current, next) month and year shown
        datePicker1.setOnMouseClicked(e -> {
            DatePickerSkin skin = (DatePickerSkin) datePicker1.getSkin();
            skin.getPopupContent().lookupAll(".button").forEach(node -> {
                node.setOnMouseClicked(event -> {
                    List<Node> labels = skin.getPopupContent().lookupAll(".label").stream().toList();
                    String month = ((Label) (labels.get(0))).getText();
                    String year = ((Label) (labels.get(1))).getText();
                    YearMonth ym = Dates.getYearMonth(month + " " + year);
                    setEventsPrePost(ym.getYear(), ym.getMonthValue());
                });
            });
        });
        datePicker1.setDayCellFactory(new Callback<DatePicker, DateCell>() {
            @Override
            public DateCell call(DatePicker param) {
                return new DateCell() {
                    @Override
                    public void updateItem(LocalDate item, boolean empty) {
                        super.updateItem(item, empty);
                        if (!empty && item != null)
                            if (holidays.contains(item))
                                this.getStyleClass().add("holiday");
                            else
                                this.getStyleClass().remove("holiday");
                    }
                };
            }
        });

        datePicker1.valueProperty().addListener((obs, oldVal, newVal) ->
            events.setAll(businessLogic.getEvents(Dates.convertToDate(datePicker1.getValue()))));
    }

    /**
     * Marks the events for current, previous and next month.
     * @param year the year
     * @param month the month
     */
    private void setEventsPrePost(int year, int month) {
        LocalDate date = LocalDate.of(year, month, 1);
        setEvents(date.getYear(), date.getMonth().getValue());
        setEvents(date.plusMonths(1).getYear(), date.plusMonths(1).getMonth().getValue());
        setEvents(date.plusMonths(-1).getYear(), date.plusMonths(-1).getMonth().getValue());
    }

    /**
     * Fetches the days in which there are events.
     * @param year the year
     * @param month the month
     */
    private void setEvents(int year, int month) {
        Date date = Dates.toDate(year, month);

        for (Date day : businessLogic.getEventsMonth(date)) {
            holidays.add(Dates.convertToLocalDateViaInstant(day));
        }
    }

    private void initEventsTable() {
        events = FXCollections.observableArrayList();
        FilteredList<Event> filteredEvents = new FilteredList<>(events, e -> true);

        // Bind columns
        idCol.setCellValueFactory(new PropertyValueFactory<>("eventID"));
        idCol.setReorderable(false);
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionCol.setReorderable(false);
        dateCol.setCellValueFactory(cellData -> {
            // Date formatter for event date
            SimpleDateFormat sdf =  new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);
            try {
                Date d = sdf.parse(cellData.getValue().getEventDate().toString());
                sdf.applyPattern("yyyy-MM-dd HH:mm:ss");
                return new SimpleStringProperty(sdf.format(d));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        });
        dateCol.setReorderable(false);
        countryCol.setCellValueFactory(new PropertyValueFactory<>("country"));
        countryCol.setReorderable(false);

        // Text field to search and filter
        searchField.textProperty().addListener(obs -> {
            String filter = searchField.getText().toLowerCase().trim();
            if (filter == null || filter.length() == 0) {
                filteredEvents.setPredicate(e -> true);
            } else {
                filteredEvents.setPredicate(e -> {
                    SimpleDateFormat sdf =  new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);
                    try {
                        // Convert date before filtering, otherwise filtering is with the original format, which has other characters
                        Date date = sdf.parse(e.getEventDate().toString());
                        sdf.applyPattern("yyyy-MM-dd HH:mm:ss");
                        return String.valueOf(e.getEventID()).contains(filter) ||
                                e.getDescription().toLowerCase().contains(filter) ||
                                e.getCountry().toLowerCase().contains(filter) ||
                                sdf.format(date).toLowerCase().contains(filter);
                    } catch (ParseException ex) {
                        throw new RuntimeException(ex);
                    }
                });
            }
        });

        addQuestionsColumn();
        addActionColumn();

        events.addAll(businessLogic.getEvents());
        eventsTbl.setItems(filteredEvents);
    }

    private void addQuestionsColumn() {
        questionsCol = new TableColumn(ResourceBundle.getBundle("Etiquetas").getString("Questions"));
        questionsCol.setReorderable(false);
        questionsCol.setMinWidth(100);
        questionsCol.setMaxWidth(100);
        questionsCol.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<Integer>(cellData.getValue().getQuestions().size()));

        Callback<TableColumn<Event, Integer>, TableCell<Event, Integer>> cellFactory = new Callback<TableColumn<Event, Integer>, TableCell<Event, Integer>>() {
            @Override
            public TableCell<Event, Integer> call(final TableColumn<Event, Integer> param) {
                JFXButton btn = new JFXButton();
                FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.TABLE);
                icon.setSize("24px");
                icon.setFill(Color.web("#B3CF00"));
                btn.setCursor(Cursor.HAND);
                btn.setGraphic(icon);

                final TableCell<Event, Integer> cell = new TableCell<Event, Integer>() {
                    {
                        btn.setOnAction((ActionEvent event) -> {
                            ((QuestionsController) mainGUI.questionsLag.getController()).selectEvent(getTableView().getItems().get(getIndex()));
                            ((AdminMenuController) mainGUI.adminMenuLag.getController()).displayQuestions();
                        });
                    }

                    @Override
                    public void updateItem(Integer item, boolean empty) {
                        super.updateItem(item, empty);
                        setText("");
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setText(getTableView().getItems().get(getIndex()).getQuestions().size() + " ");
                            setGraphic(btn);
                        }
                    }
                };
                return cell;
            }
        };

        questionsCol.setCellFactory(cellFactory);

        eventsTbl.getColumns().add(questionsCol);
    }

    /**
     * Adds the cell and button to delete event to each row
     */
    private void addActionColumn() {
        actionCol = new TableColumn("");
        actionCol.setReorderable(false);
        actionCol.setMinWidth(80);
        actionCol.setMaxWidth(80);

        Date today = Calendar.getInstance().getTime();

        Callback<TableColumn<Event, Date>, TableCell<Event, Date>> cellFactory = new Callback<TableColumn<Event, Date>, TableCell<Event, Date>>() {
            @Override
            public TableCell<Event, Date> call(final TableColumn<Event, Date> param) {
                JFXButton btn = new JFXButton();
                FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.TRASH_ALT);
                icon.setSize("24px");
                icon.setFill(Color.web("#dc3545"));
                btn.setCursor(Cursor.HAND);

                final TableCell<Event, Date> cell = new TableCell<Event, Date>() {
                    {
                        btn.setOnAction((ActionEvent event) -> {
                            removeEvent(getTableView().getItems().get(getIndex()));
                        });
                    }

                    @Override
                    public void updateItem(Date item, boolean empty) {
                        super.updateItem(item, empty);
                        setText("");
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setText("");
                            btn.setDisable(today.compareTo(item) > 0);
                            setGraphic(btn);
                        }
                        btn.setGraphic(icon);
                    }
                };
                return cell;
            }
        };

        actionCol.setCellValueFactory(new PropertyValueFactory<>("eventDate"));
        actionCol.setCellFactory(cellFactory);

        eventsTbl.getColumns().add(actionCol);
    }

    public void removeEvent(Event e) {
        businessLogic.removeEvent(e.getEventID());

        // Remove the deleted event from the table
        events.remove(e);

        notifyChanges();
    }

    /**
     * Initializes the dialog for creating an event.
     */
    void initCreateEventDialog() {
        // Overlay to place the modal
        createEventOverlay = new StackPane();
        createEventOverlay.setPrefWidth(mainPane.getPrefWidth());
        createEventOverlay.setPrefHeight(mainPane.getPrefHeight());
        mainPane.getChildren().add(createEventOverlay);

        createEventDialog = new JFXDialog(createEventOverlay, createEventPane, JFXDialog.DialogTransition.CENTER);
        createEventDialog.setOnDialogClosed((e) -> {
            resetCreateEventDialog();
        });

        resetCreateEventDialog();
    }

    /**
     * Displays the dialog for creating an event.
     */
    @FXML
    void showCreateEventDialog() {
        createEventOverlay.setVisible(true);
        createEventPane.setVisible(true);
        createEventDialog.show();
    }

    /**
     * Closes the dialog for creating an event.
     */
    @FXML
    void closeCreateEventDialog() {
        createEventDialog.close();
    }

    /**
     * Returns the dialog to its initial state.
     */
    private void resetCreateEventDialog() {
        createEventOverlay.setVisible(false);
        createEventPane.setVisible(false);

        eventNameField.setText("");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        datePicker.setValue(Dates.convertToLocalDateViaInstant(cal.getTime()));
        eventNameField.setText("");
        resultLabel.setText("");
        resultLabel.getStyleClass().clear();
    }

    @FXML
    void createEvent() {
        // Remove previous styles
        resultLabel.setText("");
        resultLabel.getStyleClass().clear();

        String eventName = eventNameField.getText();
        LocalDate eventLocalDate = datePicker.getValue();
        String country = countryField.getText();

        // Check non-null fields
        if (eventName.isEmpty() || eventLocalDate == null || country.isEmpty()) {
            resultLabel.setText(ResourceBundle.getBundle("Etiquetas").getString("FieldsCompulsory"));
            resultLabel.getStyleClass().addAll("lbl", "lbl-danger");
        } else {
            Date eventDate = Dates.convertToDate(eventLocalDate);
            Date today = Calendar.getInstance().getTime();

            if (today.compareTo(eventDate) > 0) {
                resultLabel.setText("Date incorrect");
                resultLabel.getStyleClass().addAll("lbl", "lbl-danger");
            }
            else {
                try {
                    Match dummyMatch = new Match();
                    dummyMatch.setId(-1);
                    Event e = businessLogic.createEvent(eventName, eventDate, country,  dummyMatch);
                    resultLabel.setText(ResourceBundle.getBundle("Etiquetas").getString("EventAddedSuccessfully"));
                    resultLabel.getStyleClass().addAll("lbl", "lbl-success");
                    events.add(0, e);
                    notifyChanges();
                    closeCreateEventDialog();
                } catch (EventAlreadyExistException e) {
                    resultLabel.setText(ResourceBundle.getBundle("Etiquetas").getString("ErrorEventAlreadyExist"));
                    resultLabel.getStyleClass().addAll("lbl", "lbl-danger");
                }
            }
        }
    }

    /**
     * Requests data reload to affected windows
     */
    public void notifyChanges() {
        // Refresh data in other windows
        ((QuestionsController)mainGUI.questionsLag.getController()).reloadData();
        ((ForecastsController)mainGUI.forecastsLag.getController()).reloadData();

        // Update also the money displayed in the navigation bar
        ((NavBarController)mainGUI.navBarLag.getController()).updateWalletLabel();
    }

    /**
     * Updates the changes made in events in other windows.
     */
    public void reloadData() {
        events.clear();
        events.addAll(businessLogic.getEvents());
    }

    @Override
    public void setMainApp(MainGUI mainGUI) {
        this.mainGUI = mainGUI;
    }

    @Override
    public void redraw() {
        // Text fields
        searchField.setPromptText(ResourceBundle.getBundle("Etiquetas").getString("Search") + "...");

        // Table colums
        descriptionCol.setText(ResourceBundle.getBundle("Etiquetas").getString("Description"));
        dateCol.setText(ResourceBundle.getBundle("Etiquetas").getString("Date"));
        countryCol.setText(ResourceBundle.getBundle("Etiquetas").getString("Country"));
        questionsCol.setText(ResourceBundle.getBundle("Etiquetas").getString("Questions"));

        // Buttons
        addEventBtn.setText(ResourceBundle.getBundle("Etiquetas").getString("CreateEvent"));
        backBtn.setText(ResourceBundle.getBundle("Etiquetas").getString("Back"));
        createEventBtn.setText(ResourceBundle.getBundle("Etiquetas").getString("CreateEvent"));

        // Labels
        eventNameLbl.setText(ResourceBundle.getBundle("Etiquetas").getString("EventName"));
        eventDateLbl.setText(ResourceBundle.getBundle("Etiquetas").getString("Date"));
        countryLbl.setText(ResourceBundle.getBundle("Etiquetas").getString("Country"));

        // Table
        eventsTbl.setPlaceholder(new Label(ResourceBundle.getBundle("Etiquetas").getString("NoContentInTable")));
    }
}