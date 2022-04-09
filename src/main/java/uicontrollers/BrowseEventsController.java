package uicontrollers;

import businessLogic.BlFacade;
import com.jfoenix.controls.JFXSlider;
import domain.Event;
import javafx.animation.RotateTransition;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.Cursor;
import javafx.scene.control.*;

import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.util.Callback;
import javafx.util.Duration;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ui.MainGUI;
import utils.Dates;
import utils.Formatter;
import utils.skin.MyDatePickerSkin;

import org.apache.poi.ss.usermodel.Row;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

import static utils.Dates.isValidDate;

public class BrowseEventsController implements Controller {
    private BlFacade businessLogic;
    private MainGUI mainGUI;

    private LocalDate lastValidDate;
    private List<LocalDate> holidays = new ArrayList<>();
    private ObservableList<Event> events;

    @FXML private AnchorPane main;
    @FXML private DatePicker eventDatePicker;
    @FXML private TextField dayField, monthField, yearField;
    @FXML private TableView<Event> eventTbl;
    @FXML private TableColumn<Event, Integer> idCol;
    @FXML private TableColumn<Event, String> descriptionCol;
    @FXML private TableColumn<Event, String> countryCol;

    // [*] ----- Earth and slider attributes ----- [*]
    private Sphere earth;
    private static final int EARTH_RADIUS = 175;
    private Group earthGroup;
    private Map<String, EarthPoint> earthPoints;

    @FXML JFXSlider earthRotationSlider;

    /**
     * Constructor. Initializes business logic.
     * @param bl business logic
     */
    public BrowseEventsController(BlFacade bl) {
        businessLogic = bl;
    }

    @FXML
    void initialize() {

        // Fetch all events from previous, current and next month
        setEventsPrePost(LocalDate.now().getYear(), LocalDate.now().getMonth().getValue());

        // Change DatePicker skin in order to remove text field
        eventDatePicker.setSkin(new MyDatePickerSkin(eventDatePicker));

        // Initialize the event date select with current day
        lastValidDate = LocalDate.now();
        setPreviousDate();

        addDateFormatters();
        initializeDatePicker();
        initializeEventTable();
        initialize3dScene();
        initializeSlider();
    }

    public void initializeEventTable() {
        events = FXCollections.observableArrayList();

        // Bind columns
        idCol.setCellValueFactory(new PropertyValueFactory<>("eventID"));
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        countryCol.setCellValueFactory(new PropertyValueFactory<>("country"));

        // Get all events of the initial date
        Date today = (Dates.convertToDate(lastValidDate));
        events.addAll(businessLogic.getEvents(today));

        // Bind observable list to the table
        eventTbl.setItems(events);

        // Add event listener so earth is rotated when an event is selected
        eventTbl.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // Get the country
                String country = eventTbl.getSelectionModel().getSelectedItem().getCountry();

                // Rotate towards the country
                double currentRotation = earthGroup.getRotate();
                double rotation = earthPoints.get(country).rotation;

                RotateTransition rt = new RotateTransition(Duration.millis(1000), earthGroup);
                rt.setByAngle(rotation - currentRotation);
                rt.play();

                // Update the slider
                earthRotationSlider.setValue(rotation);
            }
        });
    }

    /**
     * Fetches all the events of the given month and adds them
     * to holidays list.
     * @param year year of the events
     * @param month month of the date
     */
    private void setEvents(int year, int month) {
        Date date = Dates.toDate(year,month);

        for (Date day : businessLogic.getEventsMonth(date)) {
            holidays.add(Dates.convertToLocalDateViaInstant(day));
        }
    }

    /**
     * Fetches events for current, previous and next month.
     * @param year year of the events
     * @param month month of the events
     */
    private void setEventsPrePost(int year, int month) {
        LocalDate date = LocalDate.of(year, month, 1);
        setEvents(date.getYear(), date.getMonth().getValue());
        setEvents(date.plusMonths(1).getYear(), date.plusMonths(1).getMonth().getValue());
        setEvents(date.plusMonths(-1).getYear(), date.plusMonths(-1).getMonth().getValue());
    }

    /**
     * Updates the values in the table with a given date.
     * @param date event date
     */
    public void updateEventTable(Date date) {
        // Empty the list and the table
        events.clear();
        eventTbl.getItems().removeAll();

        // Get all events of the initial date
        events.addAll(businessLogic.getEvents(date));
    }

    /**
     * Updates the values in the table with a given country.
     * @param country country where event take place
     */
    public void updateEventTable(String country) {
        // Empty the list and the table
        events.clear();
        eventTbl.getItems().removeAll();

        // Get all events of the given country
        events.addAll(businessLogic.getEventsCountry(country));
    }



    /* ---------------------------------- Date and DatePicker ----------------------------------*/


    /**
     * Adds the event listener to the event DatePicker
     */
    public void initializeDatePicker() {
        // When a date is selected
        eventDatePicker.valueProperty().addListener((ov, oldValue, newValue) -> {
            String[] date = newValue.toString().split("-");
            saveLastDate(date[0], date[1], date[2]);
            setPreviousDate();
            updateEventTable(Dates.convertToDate(newValue));
        });

        eventDatePicker.setDayCellFactory(new Callback<DatePicker, DateCell>() {
            @Override
            public DateCell call(DatePicker param) {
                return new DateCell() {
                    @Override
                    public void updateItem(LocalDate item, boolean empty) {
                        super.updateItem(item, empty);

                        if (!empty && item != null) {
                            if (holidays.contains(item)) {
                                this.setStyle("-fx-background-color: pink");
                            }
                        }
                    }
                };
            }
        });
    }

    /**
     * Adds fixed format to date text fields, and adds some observators to maintain a valid day.
     */
    public void addDateFormatters() {
        // Text formatter for day field
        dayField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (!newValue.matches("\\d*"))
                    dayField.setText(newValue.replaceAll("[^\\d]", ""));

                if (newValue.length() > 2) dayField.setText(oldValue);
            }
        });

        // When defocusing day field, check if introduced date is valid
        dayField.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (oldValue)
                    if (isValidDate(yearField.getText(), monthField.getText(), dayField.getText()))
                        saveLastDate(yearField.getText(), monthField.getText(), dayField.getText());
                setPreviousDate();
            }
        });

        // Text formatter for month field
        monthField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (!newValue.matches("\\d*"))
                    monthField.setText(newValue.replaceAll("[^\\d]", ""));

                if (newValue.length() > 2) monthField.setText(oldValue);
            }
        });

        // When defocusing month field, check if introduced date is valid
        monthField.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (oldValue)
                    if (isValidDate(yearField.getText(), monthField.getText(), dayField.getText()))
                        saveLastDate(yearField.getText(), monthField.getText(), dayField.getText());
                setPreviousDate();
            }
        });

        // Text formatter for year field
        yearField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (!newValue.matches("\\d*"))
                    yearField.setText(newValue.replaceAll("[^\\d]", ""));

                if (newValue.length() > 4) yearField.setText(oldValue);
            }
        });

        // When defocusing year field, check if introduced date is valid
        yearField.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (oldValue)
                    if (isValidDate(yearField.getText(), monthField.getText(), dayField.getText()))
                        saveLastDate(yearField.getText(), monthField.getText(), dayField.getText());
                setPreviousDate();
            }
        });
    }

    /**
     * Stores the last valid date introduced by the user.
     * @param year event day
     * @param month event month
     * @param day event year
     */
    public void saveLastDate(String year, String month, String day) {
        lastValidDate = LocalDate.parse(
                String.format("%4s", year).replace(' ', '0') + "-" +
                String.format("%2s", month).replace(' ', '0') + "-" +
                String.format("%2s", day).replace(' ', '0'));
        eventDatePicker.setValue(lastValidDate);
    }

    /**
     * Sets the event date displayed on the scene to the last valid date.
     */
    public void setPreviousDate() {
        dayField.setText(Formatter.padLeft(String.valueOf(lastValidDate.getDayOfMonth()), '0', 2));
        monthField.setText(Formatter.padLeft(String.valueOf(lastValidDate.getMonthValue()), '0', 2));
        yearField.setText(Formatter.padLeft(String.valueOf(lastValidDate.getYear()), '0', 4));
        eventDatePicker.setValue(lastValidDate);
    }

    @FXML
    void dateKeyPressed(KeyEvent key) {
       if (key.getCode().equals(KeyCode.ENTER)) {
           eventTbl.requestFocus();
       }
    }


    /* ---------------------------------- Earth and slider methods ----------------------------------*/


    public void initialize3dScene() {
        // [*] --- earthGroup and earth objects already created in scene builder --- [*]

        // 3d objects group
        earthGroup = new Group();

        // Setup rotation
        earthGroup.setRotationAxis(Rotate.Y_AXIS);

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setNearClip(0.01);
        camera.setFarClip(10000);
        camera.setTranslateZ(-700);

        // Create the earth
        earth = new Sphere(EARTH_RADIUS);
        earth.setRotationAxis(Rotate.Y_AXIS);
        earth.rotateProperty().set(180);

        // Add the earth texture to the earth
        PhongMaterial earthMaterial = new PhongMaterial();
        earthMaterial.setDiffuseMap(new Image(getClass().getResourceAsStream("/img/earth-d.jpeg")));
        earthMaterial.setBumpMap(new Image(getClass().getResourceAsStream("/img/earth-b.jpeg")));
        earthMaterial.setSpecularMap(new Image(getClass().getResourceAsStream("/img/earth-s.jpeg")));
        earth.setMaterial(earthMaterial);

        earthGroup.getChildren().add(earth);

        // [*] --- Get country points ---- [*]

        // Load countries and their coords and corresponding rotations
        importCountryCoords();

        // Add country markers to the map
        for (String country: earthPoints.keySet()) {
            Sphere s = new Sphere(3);
            s.setMaterial(new PhongMaterial(Color.web("#B3CF00")));
            s.setTranslateX(-earthPoints.get(country).p.getX());
            s.setTranslateY(earthPoints.get(country).p.getY());
            s.setTranslateZ(-earthPoints.get(country).p.getZ());
            earthGroup.getChildren().add(s);

            // Add on click listener to each point to get the country
            s.setOnMouseClicked(e -> {
                updateEventTable(country);
            });

            s.setOnMouseEntered(e -> {
                s.setCursor(Cursor.HAND);
            });
        }

        // Subscene where we can enable depth buffer
        SubScene scene3d = new SubScene(earthGroup, 500, 500, true, SceneAntialiasing.BALANCED);
        scene3d.setCamera (camera);
        scene3d.setWidth(EARTH_RADIUS * 2);
        scene3d.setHeight(EARTH_RADIUS * 2);
        scene3d.setTranslateX(160);
        scene3d.setTranslateY(50);

        main.getChildren().add(scene3d);
    }

    /**
     * Reads country names, coordinates and rotations from xlsx file
     * and adds them to the countryCoords map
     */
    private void importCountryCoords() {
        earthPoints = new HashMap<String, EarthPoint>();
        try {
            InputStream is = getClass().getResourceAsStream("/dataset/countryCoords.xlsx");
            XSSFWorkbook wb = new XSSFWorkbook(is);
            XSSFSheet sheet = wb.getSheetAt(0);
            Iterator<Row> it = sheet.iterator();

            while(it.hasNext()) {
                Row row = it.next();

                // Get country name
                String countryName = row.getCell(0).getStringCellValue();

                // Get coords and create 3d point
                double[] coords = Arrays.stream(row.getCell(1)
                                .getStringCellValue().split(","))
                        .mapToDouble(Double::parseDouble)
                        .toArray();
                Point3D p = new Point3D(coords[0], coords[1], coords[2]);

                // Get rotation
                double rotation = row.getCell(2).getNumericCellValue();

                // Create new 3d point
                earthPoints.put(countryName, new EarthPoint(p, rotation));
            }
        } catch(IOException e) {
            System.out.println("Cannot load country coords.");
            e.printStackTrace();
        }
    }

    /**
     * Binds the slider value to the rotation of the 3d group.
     */
    public void initializeSlider() {
        // Bind the slider with the rotation property
        earthRotationSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                // Set the angle for the rotation
                earthGroup.rotateProperty().set((double)newValue);
            }
        });

        earthRotationSlider.setValue(180);
    }


    /* ---------------------------------- Controller interface ----------------------------------*/


    @Override
    public void setMainApp(MainGUI mainGUI) {
        this.mainGUI = mainGUI;
    }

    @Override
    public void redraw() {}
}

/**
 * Class used to represents a point on the globe.
 */
class EarthPoint {
    protected Point3D p;
    protected double rotation;

    public EarthPoint(Point3D p, double rotation) {
        this.p = p;
        this.rotation = rotation;
    }

}