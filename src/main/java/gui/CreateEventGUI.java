package gui;

import businessLogic.BlFacade;
import businessLogic.DynamicJFrame;
import com.toedter.calendar.JCalendar;
import configuration.UtilDate;
import exceptions.EventAlreadyExistException;
import gui.components.MenuBar;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

/**
 * This class represents the GUI to create new events in the application
 * @author Josefinators team
 * @version first iteration
 */
public class CreateEventGUI extends JFrame implements DynamicJFrame {

	private static final long serialVersionUID = 1L;
	
	private BlFacade businessLogic;
	
	private JPanel contentPane;
	private JTextField eventDescriptionField;
	private JButton createEventBtn;
	private JLabel eventDescriptionLbl;
	private JLabel eventStatusLabel;
	private JCalendar calendar;
	private JMenuBar menuBar;
	
	/**
	 * Constructor that instantiates the CreateEventGUI class 
	 * @param bl an instance of the business logic layer 
	 */
	public CreateEventGUI(BlFacade bl) {
		businessLogic = bl;
		try {
			jbInit();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * It creates the main frame
	 */
	private void jbInit() {
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 459, 352);
		setIconImage(Toolkit.getDefaultToolkit().getImage("./resources/favicon.png"));
		setTitle(ResourceBundle.getBundle("Etiquetas").getString("CreateEvent"));

		initializeMainPane();
		
		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(
			gl_contentPane.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING)
						.addGroup(gl_contentPane.createSequentialGroup()
							.addContainerGap()
							.addComponent(createEventBtn))
						.addGroup(gl_contentPane.createSequentialGroup()
							.addGap(43)
							.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
								.addComponent(eventStatusLabel, GroupLayout.DEFAULT_SIZE, 345, Short.MAX_VALUE)
								.addComponent(calendar, GroupLayout.DEFAULT_SIZE, 345, Short.MAX_VALUE)
								.addGroup(gl_contentPane.createSequentialGroup()
									.addComponent(eventDescriptionLbl, GroupLayout.PREFERRED_SIZE, 75, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(eventDescriptionField, GroupLayout.DEFAULT_SIZE, 266, Short.MAX_VALUE)))))
					.addGap(45))
		);
		gl_contentPane.setVerticalGroup(
			gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
						.addComponent(eventDescriptionLbl, GroupLayout.DEFAULT_SIZE, 21, Short.MAX_VALUE)
						.addComponent(eventDescriptionField, GroupLayout.DEFAULT_SIZE, 28, Short.MAX_VALUE))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(calendar, GroupLayout.PREFERRED_SIZE, 157, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(createEventBtn)
					.addGap(16)
					.addComponent(eventStatusLabel, GroupLayout.PREFERRED_SIZE, 17, GroupLayout.PREFERRED_SIZE)
					.addGap(34))
		);
		contentPane.setLayout(gl_contentPane);
	}
	
	/**
	 * It initializes most of the components in the GUI
	 */
	private void initializeMainPane() {
		// Content Panel
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		
		menuBar = MenuBar.getMenuBar(this);	
	    setJMenuBar(menuBar);
		
		eventDescriptionLbl = new JLabel();
		eventDescriptionLbl.setText(ResourceBundle.getBundle("Etiquetas").getString("Description"));

		eventStatusLabel = new JLabel();
		eventStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
		
		// Text fields
		initializeEventDescriptionInput();
		
		// Calendar
		initializeCalendar();
		
		// Buttons
		initializeCreateEventBtn();
	}
	
	/**
	 *  It initializes the event description input
	 */
	private void initializeEventDescriptionInput() {
		eventDescriptionField = new JTextField();
		eventDescriptionField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				enableCreateEventBtn();
			}
		});
		eventDescriptionField.setColumns(10);
	}
	
	/**
	 * It initializes the calendar
	 */
	private void initializeCalendar() {
		calendar = new JCalendar();
		
		// Code for JCalendar
			calendar.addPropertyChangeListener(new PropertyChangeListener() {

				@Override
				public void propertyChange(PropertyChangeEvent propertyChangeEvent) {

					if (propertyChangeEvent.getPropertyName().equals("locale")) {
						calendar.setLocale((Locale) propertyChangeEvent.getNewValue());
					}
					else if (propertyChangeEvent.getPropertyName().equals("calendar")) {						
						Vector<Date> datesWithEventsInCurrentMonth = businessLogic.getEventsMonth(calendar.getDate());
						CreateQuestionGUI.paintDaysWithEvents(calendar,datesWithEventsInCurrentMonth);
					}
				}
			});
	}
	
	/**
	 * It initializes the create event button to store the new event in the database 
	 */
	private void initializeCreateEventBtn() {
		createEventBtn = new JButton();
		createEventBtn.setText(ResourceBundle.getBundle("Etiquetas").getString("CreateEvent"));
		createEventBtn.setEnabled(false);
		
		createEventBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Check if the date has not passed
				if (calendar.getDate().before(Calendar.getInstance().getTime())) {
					// Print error message
					eventStatusLabel.setForeground(new Color(220, 20, 60));
					eventStatusLabel.setText("<html><p style=\\\"width:200px\\\">"+ResourceBundle.getBundle("Etiquetas").getString("ErrorInvalidDate")+"</p></html>");
				} else {					
					// Save the event in the database

						// businessLogic.createEvent(eventDescriptionField.getText(), UtilDate.trim(calendar.getDate(), "countryShouldGoHere"));
						// Print success message
						eventStatusLabel.setForeground(new Color(46, 204, 113));
						eventStatusLabel.setText("<html><p style=\\\"width:200px\\\">" + ResourceBundle.getBundle("Etiquetas").getString("EventAddedSuccessfully") + "</p></html>");

						// Print error message
						eventStatusLabel.setForeground(new Color(220, 20, 60));
						eventStatusLabel.setText("<html><p style=\\\"width:200px\\\">"+ResourceBundle.getBundle("Etiquetas").getString("ErrorEventAlreadyExist")+"</p></html>");

				}
			}
		});
	}
	
	/**
	 * It enables the create event button when the event is valid
	 */
	private void enableCreateEventBtn() {
		if (eventDescriptionField.getText().isEmpty()) createEventBtn.setEnabled(false);
		else createEventBtn.setEnabled(true);
	}

	/**
	 * It updates issues related to several options of the GUI
	 */
	public void redraw() {
		eventDescriptionLbl.setText(ResourceBundle.getBundle("Etiquetas").getString("Description"));
		createEventBtn.setText(ResourceBundle.getBundle("Etiquetas").getString("CreateEvent"));
		calendar.setLocale(Locale.getDefault());
		setTitle(ResourceBundle.getBundle("Etiquetas").getString("CreateEvent"));
	}
}