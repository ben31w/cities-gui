import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Cities is a JFrame GUI that displays the name, state, and population of 
 * large US cities in a scroll-able text area. There are several options to 
 * customize the way the cities are displayed. Users can choose to display only 
 * cities from certain states using a combo box/drop down menu. Users can also
 * sort the cities by name or by population.
 *  
 * @author ben31w
 *
 */
public class Cities extends JFrame {
	private static final long serialVersionUID = 1L;
	private String[] states = {"All","AK","AL","AR","AZ","CA","CO","CT","DC",
			"DE","FL","GA","HI","IA","ID","IL","IN","KS","KY","LA","MA","MD",
			"ME","MI","MN","MO","MS","MT","NC","ND","NE","NH","NJ","NM","NV",
			"NY","OH","OK","OR","PA","RI","SC","SD","TN","TX","UT","VA","VT",
			"WA","WI","WV","WY"};
	
	// This ArrayList stores the cities that will be displayed on the text area.
	private ArrayList<City> citiesToDisplay;
	
	// Create an ArrayList containing all the cities from the csv file.
	private InputStream is = getClass().getResourceAsStream( "cities.csv" );
	private ArrayList<City> allCities = getCities(is);
	
	
	/**
	 * City is a helper class that stores the data (name, state, and 
	 * population) of a city.
	 * 
	 * @author ben31w
	 */
	class City implements Comparable<City>, Serializable {
		private static final long serialVersionUID = 1L;
		String name;
		String state;
		int population;
		
		public City(String name, String state, int population) {
			this.name = name;
			this.state = state;
			this.population = population;
		}
		
		public String toString() {
			return String.format("%s, %s, (%d)", name, state, population);
		}
		
		/**
		 * Compare this city to another city for sorting purposes. The 
		 * comparison is done using the cities' names, so the city that comes 
		 * first alphabetically is considered "less than" the other city.
		 * When comparing cities that have the same name, the cities' state 
		 * abbreviations are compared alphabetically.
		 * 
		 * @return a negative integer if this city comes before the other city alphabetically, or
		 * 		a positive integer if this city comes after the other city
		 */
		@Override
		public int compareTo(Cities.City otherCity) {
			if ( this.name.equals( otherCity.name ) ) {
				return this.state.compareTo( otherCity.state );
			}
			return this.name.compareTo( otherCity.name );
		}
	}
	
	
	/**
	 * Create a new Cities frame that displays info (name, state, population) 
	 * about large US cities, and enables users to limit the list to a specific 
	 * state and sort by city name or population.
	 */
	public Cities() {		
		setTitle("Cities");
		setLayout( new BorderLayout() );
		
		// Top panel has a combo box with every state to choose from, and radio 
		// buttons that enable the user to sort by city name or by population.
		JPanel top = new JPanel( new FlowLayout() );
		add( top, BorderLayout.NORTH );
		
		JLabel stateLabel = new JLabel("State");
		top.add(stateLabel);
		
		JComboBox<String> combobox = new JComboBox<>(states);
		combobox.setSelectedItem("All");
		top.add(combobox);
		
		JPanel grid = new JPanel( new GridLayout(2, 1) );
		top.add(grid);		
		JRadioButton byCity = new JRadioButton("by city");
		grid.add(byCity);
		byCity.setSelected(true);
		JRadioButton byPopulation = new JRadioButton("by population");
		grid.add(byPopulation);
		
		ButtonGroup rGroup = new ButtonGroup();
		rGroup.add(byCity);
		rGroup.add(byPopulation);
		
		// Bottom panel displays the city names in a text are wrapped by a 
		// scroll pane.
		JPanel bottom = new JPanel( new FlowLayout() );
		add( bottom, BorderLayout.CENTER );
		
		JTextArea area = new JTextArea();
		JScrollPane scroll = new JScrollPane(area);
		area.setFont( new Font("Courier", Font.PLAIN, 14) );
		scroll.setPreferredSize( new Dimension(400, 400) );
		add( scroll, BorderLayout.CENTER );	
				
		// Add an action listener to the combo box.
		combobox.addActionListener( e -> {
			String state = (String) combobox.getSelectedItem();
			
			// Clear the list of cities to display to the text area.
			citiesToDisplay.clear();
			
			// Go through the list containing all cities, and for each city 
			// that has the same state as the one currently selected, add that 
			// city to the list that will get displayed
			for (City c: allCities) {
				if (state.equals( c.state) || state.equals("All") ) {
					citiesToDisplay.add(c);
				}
			}
			
			// Check which radio button is selected and sort the cities accordingly.
			if ( byCity.isSelected() ) {
				Collections.sort(citiesToDisplay);
			}
			else if ( byPopulation.isSelected() ) {
				sortByPopulation(citiesToDisplay);
			}
			
			// Update the text area.
			setTextArea( area, citiesToDisplay );
		});
		
		// Add an action listener to the "by city" button.
		byCity.addActionListener( e -> {
			Collections.sort(citiesToDisplay);
			setTextArea( area, citiesToDisplay );
		});
		// Add an action listener to the "by population" button.
		byPopulation.addActionListener( e -> {
			sortByPopulation(citiesToDisplay);
			setTextArea( area, citiesToDisplay );
		});
		
		// Set the text area to display all cities in alphabetical order when 
		// the frame is initially loaded.
		citiesToDisplay = new ArrayList<>(allCities);	
		Collections.sort(citiesToDisplay);
		setTextArea( area, citiesToDisplay );
		
		// Set frame dimensions.
		pack();
		setLocationRelativeTo(null);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}
	
	
	/**
	 * Return an ArrayList of all the cities stored in an input stream.
	 * 
	 * @param input
	 * 			the input stream/file to be read from
	 * @return 
	 * 			an array list storing all the cities in the file
	 */
	private ArrayList<City> getCities(InputStream input) {
		ArrayList<City> result = new ArrayList<>();
		Scanner fin = new Scanner(input);
		
		// Search the file for cities to add to the list until there are no 
		// more lines in the file.
		while ( fin.hasNext() ) {
			Scanner scanner = new Scanner( fin.nextLine() );
			scanner.useDelimiter(", ");
			
			// Add the city to the list.
			String name = scanner.next();
			String state = scanner.next();
			int population = scanner.nextInt();
			
			result.add(new City(name, state, population) );
			
			scanner.close();
		}
		fin.close();
		
		return result;
	}
	
	
	/**
	 * Sort all the cities in an array list by ascending population. This 
	 * method uses insertion sort.
	 * @param cities
	 * `			the array list to be sorted
	 */
	private void sortByPopulation(ArrayList<City> cities) {
		int pos;
		City temp;
		for (int i=0; i<cities.size(); i++) {
			pos = i;
			for (int j=i+1; j<cities.size(); j++) {
				// Find the index of the city with the lowest population
				if (cities.get(j).population < cities.get(pos).population) {
					pos = j;
				}
			}
			// Swap the city at i with the lowest population city
			temp = cities.get(pos);
			cities.set(pos, cities.get(i));
			cities.set(i, temp);
		}
	}
	
	
	/**
	 * Update a given JTextArea to display the given ArrayList of cities.
	 * 
	 * @param area
	 * 				the JTextArea that will get updated
	 * @param cities
	 * 				the cities to put on the JTextArea
	 */
	private void setTextArea(JTextArea area, ArrayList<City> cities) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<cities.size(); i++) {
			City c = cities.get(i);
			sb.append( String.format("%-28s | %s | %,10d", c.name, c.state, c.population) );
			
			if (i != cities.size() - 1) {
				sb.append("\n");
			}
		}
		area.setText(sb.toString());
	}
	
	
	public static void main(String[] args) {
		Cities f = new Cities();
		f.setVisible(true);
	}
	
}
