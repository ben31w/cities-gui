import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.WindowEvent;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.truth.Truth;

import edu.cnu.cs.gooey.Gooey;
import edu.cnu.cs.gooey.GooeyFrame;

public class CitiesTest {
	private static final Class<?> CLAZZ = Cities.class;
	@BeforeAll
	private static void runMain() {
		Cities.main( new String[]{} );
	}

	@BeforeEach
	public void hasClass() {
		String clazz = CLAZZ.getSimpleName();
		try {
			Package pkg  = getClass().getPackage();
			String  path = (pkg == null || pkg.getName().isEmpty()) ? "" : pkg.getName()+".";
			Class.forName( path + clazz );
		} catch (ClassNotFoundException e) {
			fail( String.format( "Class '%s' not found", clazz ));
		}
	}
	@Nested
	class NonFunctionalTests {
		@Test
		void testHasPrivateFieldsOnly() {
			Arrays.stream( CLAZZ.getDeclaredFields() ).filter( f->!f.isSynthetic() ).forEach( f->{
				var mod  = f.getModifiers();
				var name = f.getName(); 
				Truth.assertWithMessage( String.format("field '%s' is not private", name )).that( Modifier.isPrivate( mod )).isTrue();
				if (Modifier.isStatic ( mod )) {
					Truth.assertWithMessage( String.format("field '%s' is static but not final", name )).that(Modifier.isFinal( mod )).isTrue();
				}
			});
		}
		@Test
		void testClassExtendsJFrame() {
			Class<?>    type = CLAZZ.getSuperclass();
			Truth.assertWithMessage( "Class should extend JFrame" ).that( type ).isSameInstanceAs( JFrame.class );
		}
		@Test
		void testMainRunsJFrame() throws NoSuchMethodException, SecurityException {
			Gooey.capture(new GooeyFrame() {
				@Override
				public void invoke() {
					runMain();
				}
				@Override
				public void test(JFrame f) {
					Class<?> expected = CLAZZ;
					Class<?> actual   = f.getClass();
					Truth.assertWithMessage( "Window should be of type " + CLAZZ ).that( actual ).isEqualTo( expected );
				}
			});
		}
		// startup
		@Test
		void testWindowHasTitle() {
			Gooey.capture(new GooeyFrame() {
				@Override
				public void invoke() {
					runMain();
				}
				@Override
				public void test(JFrame f) {
					String expected = "Cities";
					String actual   = f.getTitle();
					Truth.assertWithMessage( "Incorrect title" ).that( actual ).isEqualTo( expected );
				}
			});
		}
		@Test
		void testWindowIsPackedAndCenteredOnScreen() {
			Gooey.capture(new GooeyFrame() {
				@Override
				public void invoke() {
					runMain();
				}
				@Override
				public void test(JFrame f) {
					Rectangle actual   = f.getBounds();
					f.pack();
					f.setLocationRelativeTo( null );

					Rectangle expected = f.getBounds();
					Truth.assertWithMessage( "Window not packed" ).that( actual.width  ).isEqualTo( expected.width  );
					Truth.assertWithMessage( "Window not packed" ).that( actual.height ).isEqualTo( expected.height );
					
					Truth.assertWithMessage( "Window not centered on screen" ).that( actual.x ).isEqualTo( expected.x );
					Truth.assertWithMessage( "Window not centered on screen" ).that( actual.y ).isEqualTo( expected.y );
				}
			});
		}
		@Test
		void testWindowHasDefaultCloseOperation() {
			Gooey.capture(new GooeyFrame() {
				@Override
				public void invoke() {
					runMain();
				}
				@Override
				public void test(JFrame f) {
					int actual   = f.getDefaultCloseOperation();
					int expected = JFrame.DISPOSE_ON_CLOSE;
					Truth.assertWithMessage( "Window doesn't implement DISPOSE_ON_CLOSE" ).that( actual ).isEqualTo( expected );
				}
			});
		}
		@Test
		void testWindowHasLabelAndComboBox() {
			Gooey.capture(new GooeyFrame() {
				@Override
				public void invoke() {
					runMain();
				}
				@Override
				public void test(JFrame f) {
					// label
					Gooey.getLabel( f, "State" );
					// combo box
					JComboBox<?> combo = Gooey.getComponent( f, JComboBox.class );
					Object       now   = combo.getSelectedItem();
					Truth.assertWithMessage( "combo box selection should be 'All'" ).that( now ).isEqualTo( "All" );
					Truth.assertWithMessage( "number of combo box states" ).that( combo.getItemCount() ).isEqualTo( STATES.length );
					for (int i = 0; i < STATES.length; i++) {
						Truth.assertWithMessage( "combo box item at index "+i ).that( combo.getItemAt( i )).isEqualTo( STATES[i] );
					}
				}
			});
		}
		@Test
		void testWindowHasRadioButtons() {
			Gooey.capture(new GooeyFrame() {
				@Override
				public void invoke() {
					runMain();
				}
				@Override
				public void test(JFrame f) {
					List<JRadioButton> radios = Gooey.getComponents( f, JRadioButton.class );
					Truth.assertWithMessage( "number of radio buttons" ).that( radios.size() ).isEqualTo( 2 );

					JRadioButton by_city       = null;
					JRadioButton by_population = null;
					for (JRadioButton rb : radios) {
						String txt = rb.getText();
						switch (txt) {
						case RADIO_BY_CITY       : by_city       = rb; break; 
						case RADIO_BY_POPULATION : by_population = rb; break;
						default                  : fail( String.format( "Unexpected radio button '%s'", txt ));
						}
					}
					Truth.assertWithMessage( "radio button '"+RADIO_BY_CITY      +"' not found" ).that( by_city       ).isNotNull();
					Truth.assertWithMessage( "radio button '"+RADIO_BY_POPULATION+"' not found" ).that( by_population ).isNotNull();
					
					Truth.assertWithMessage( "'"+RADIO_BY_CITY      +"' should be selected"     ).that( by_city      .isSelected() ).isTrue();
					Truth.assertWithMessage( "'"+RADIO_BY_POPULATION+"' should not be selected" ).that( by_population.isSelected() ).isFalse();
				}
			});
		}
		@Test
		void testWindowHasScrollPane() {
			Gooey.capture(new GooeyFrame() {
				@Override
				public void invoke() {
					runMain();
				}
				@Override
				public void test(JFrame f) {
					JScrollPane scroll = Gooey .getComponent( f, JScrollPane.class );
					JTextArea   area   = Gooey .getComponent( f, JTextArea  .class );
					JViewport   view   = scroll.getViewport ();
					Component[] list   = view  .getComponents();
					Truth.assertWithMessage( "scroll pane should display one component only" ).that( list    ).hasLength( 1 );
					Truth.assertWithMessage( "scroll pane should display a text area"        ).that( list[0] ).isInstanceOf( JTextArea.class );
					Truth.assertWithMessage( "there is more than one text area in the frame" ).that( list[0] ).isSameInstanceAs( area );
				}
			});
		}
		@Test
		void testWindowClosesWhenClickingOnCloseIcon() {
			Gooey.capture(new GooeyFrame() {
				@Override
				public void invoke() {
					runMain();
				}
				@Override
				public void test(JFrame f) {
					Truth.assertWithMessage( "frame should be open"   ).that( f.isShowing() ).isTrue();
					f.dispatchEvent(new WindowEvent( f, WindowEvent.WINDOW_CLOSING ));
					Truth.assertWithMessage( "frame should be closed" ).that( f.isShowing() ).isFalse();
				}
			});
		}
	}
	@Nested
	class FunctionalTests {
		@Test
		void testTextArea_AllCitiesByNameAtStartup() {
			Gooey.capture(new GooeyFrame() {
				@Override
				public void invoke() {
					runMain();
				}
				@Override
				public void test(JFrame f) {
					JTextArea area = Gooey.getComponent( f, JTextArea.class );
					String    text = area.getText();
					Truth.assertThat( text ).isEqualTo( CITIES_BY_NAME );
				}
			});
		}
		private final Function<Container, Function<String,Optional<JRadioButton>>> getRadio = c -> s -> 
		   Gooey.getComponents( c, JRadioButton.class )
		        .stream()
		        .filter( r->s.equals( r.getText() ))
		        .findFirst();
		@Test
		void testTextArea_AllCitiesByPopulation() {
			Gooey.capture(new GooeyFrame() {
				@Override
				public void invoke() {
					runMain();
				}
				@Override
				public void test(JFrame f) {
					JComboBox<?>           combo = Gooey.getComponent( f, JComboBox.class );
					Optional<JRadioButton> radio = getRadio.apply( f ).apply( RADIO_BY_POPULATION );
					radio.ifPresentOrElse( r->r.doClick(), ()->fail("radio button '"+RADIO_BY_POPULATION+"' not found"));
					combo.setSelectedItem( "All" );
					JTextArea    area  = Gooey.getComponent( f, JTextArea.class );
					String       text  = area.getText();
					Truth.assertThat( text ).isEqualTo( CITIES_BY_POPULATION );
				}
			});
		}
		@Test
		void testTextArea_WA_ByPopulation() {
			Gooey.capture(new GooeyFrame() {
				@Override
				public void invoke() {
					runMain();
				}
				@Override
				public void test(JFrame f) {
					JComboBox<?>           combo = Gooey.getComponent( f, JComboBox.class );
					Optional<JRadioButton> radio = getRadio.apply( f ).apply( RADIO_BY_POPULATION );
					radio.ifPresentOrElse( r->r.doClick(), ()->fail( "radio button '"+RADIO_BY_POPULATION+"' not found" ));
					combo.setSelectedItem( "WA" );
					JTextArea              area  = Gooey.getComponent( f, JTextArea.class );
					String                 text  = area.getText();
					Truth.assertThat( text ).isEqualTo( WA_BY_POPULATION );
				}
			});
		}
		@Test
		void testTextArea_NY_ByPopulation() {
			Gooey.capture(new GooeyFrame() {
				@Override
				public void invoke() {
					runMain();
				}
				@Override
				public void test(JFrame f) {
					JComboBox<?>           combo = Gooey.getComponent( f, JComboBox.class );
					Optional<JRadioButton> radio = getRadio.apply( f ).apply( RADIO_BY_POPULATION );
					radio.ifPresentOrElse( r->r.doClick(), ()->fail( "radio button '"+RADIO_BY_POPULATION+"' not found" ));
					combo.setSelectedItem( "NY" );
					JTextArea              area  = Gooey.getComponent( f, JTextArea.class );
					String                 text  = area .getText();
					Truth.assertThat( text ).isEqualTo( NY_BY_POPULATION );
				}
			});
		}
		@Test
		void testTextArea_CA_ByCity() {
			Gooey.capture(new GooeyFrame() {
				@Override
				public void invoke() {
					runMain();
				}
				@Override
				public void test(JFrame f) {
					JComboBox<?>           combo = Gooey.getComponent( f, JComboBox.class );
					Optional<JRadioButton> radio = getRadio.apply( f ).apply( RADIO_BY_CITY );
					radio.ifPresentOrElse( r->r.doClick(), ()->fail( "radio button '"+RADIO_BY_CITY+"' not found" ));
					combo.setSelectedItem( "CA" );
					JTextArea              area  = Gooey.getComponent( f, JTextArea.class );
					String                 text  = area .getText();
					Truth.assertThat( text ).isEqualTo( CA_BY_NAME );
				}
			});
		}
		@Test
		void testTextArea_FL_ByCity() {
			Gooey.capture(new GooeyFrame() {
				@Override
				public void invoke() {
					runMain();
				}
				@Override
				public void test(JFrame f) {
					JComboBox<?>           combo = Gooey.getComponent( f, JComboBox.class );
					Optional<JRadioButton> radio = getRadio.apply( f ).apply( RADIO_BY_CITY );
					radio.ifPresentOrElse( r->r.doClick(), ()->fail( "radio button '"+RADIO_BY_CITY+"' not found" ));
					combo.setSelectedItem( "FL" );
					JTextArea              area  = Gooey.getComponent( f, JTextArea.class );
					String                 text  = area .getText();
					Truth.assertThat( text ).isEqualTo( FL_BY_NAME );
				}
			});
		}
		@Test
		void testTextArea_VT_ByCity() {
			Gooey.capture(new GooeyFrame() {
				@Override
				public void invoke() {
					runMain();
				}
				@Override
				public void test(JFrame f) {
					JComboBox<?>           combo = Gooey.getComponent( f, JComboBox.class );
					Optional<JRadioButton> radio = getRadio.apply( f ).apply( RADIO_BY_CITY );
					radio.ifPresentOrElse( r->r.doClick(), ()->fail( "radio button '"+RADIO_BY_CITY+"' not found" ));
					combo.setSelectedItem( "VT" );
					JTextArea              area  = Gooey.getComponent( f, JTextArea.class );
					String                 text  = area .getText();
					Truth.assertThat( text.isEmpty() ).isTrue();
				}
			});
		}
	}
	private static final String   RADIO_BY_CITY       = "by city";
	private static final String   RADIO_BY_POPULATION = "by population";
	private static final String[] STATES = { "All",
			"AK","AL","AR","AZ","CA","CO","CT","DC","DE","FL","GA","HI","IA","ID",
			"IL","IN","KS","KY","LA","MA","MD","ME","MI","MN","MO","MS","MT","NC",
			"ND","NE","NH","NJ","NM","NV","NY","OH","OK","OR","PA","RI","SC","SD",
			"TN","TX","UT","VA","VT","WA","WI","WV","WY" };

	private static final String CA_BY_NAME = String.format(
			"Alameda                      | CA |     75,641%n"+
			"Alhambra                     | CA |     84,322%n"+
			"Anaheim                      | CA |    343,248%n"+
			"Antioch                      | CA |    105,508%n"+
			"Apple Valley                 | CA |     70,700%n"+
			"Arcadia                      | CA |     57,497%n"+
			"Bakersfield                  | CA |    358,597%n"+
			"Baldwin Park                 | CA |     76,419%n"+
			"Bellflower                   | CA |     77,356%n"+
			"Berkeley                     | CA |    115,403%n"+
			"Brentwood                    | CA |     53,673%n"+
			"Buena Park                   | CA |     82,155%n"+
			"Burbank                      | CA |    104,391%n"+
			"Camarillo                    | CA |     65,968%n"+
			"Carlsbad                     | CA |    109,318%n"+
			"Carson                       | CA |     93,002%n"+
			"Cathedral                    | CA |     52,655%n"+
			"Chico                        | CA |     87,714%n"+
			"Chino                        | CA |     80,164%n"+
			"Chino Hills                  | CA |     76,457%n"+
			"Chula Vista                  | CA |    252,422%n"+
			"Citrus Heights               | CA |     84,870%n"+
			"Clovis                       | CA |     98,632%n"+
			"Colton                       | CA |     53,123%n"+
			"Compton                      | CA |     97,559%n"+
			"Concord                      | CA |    124,711%n"+
			"Corona                       | CA |    158,391%n"+
			"Costa Mesa                   | CA |    111,918%n"+
			"Cupertino                    | CA |     60,009%n"+
			"Daly                         | CA |    103,690%n"+
			"Davis                        | CA |     65,993%n"+
			"Delano                       | CA |     52,426%n"+
			"Diamond Bar                  | CA |     56,363%n"+
			"Downey                       | CA |    112,873%n"+
			"Eastvale                     | CA |     54,635%n"+
			"El Cajon                     | CA |    101,435%n"+
			"El Monte                     | CA |    115,111%n"+
			"Elk Grove                    | CA |    159,038%n"+
			"Encinitas                    | CA |     60,994%n"+
			"Escondido                    | CA |    147,575%n"+
			"Fairfield                    | CA |    107,684%n"+
			"Folsom                       | CA |     73,384%n"+
			"Fontana                      | CA |    201,812%n"+
			"Fountain Valley              | CA |     56,464%n"+
			"Fremont                      | CA |    221,986%n"+
			"Fresno                       | CA |    505,882%n"+
			"Fullerton                    | CA |    138,574%n"+
			"Garden Grove                 | CA |    174,389%n"+
			"Gardena                      | CA |     59,490%n"+
			"Gilroy                       | CA |     50,660%n"+
			"Glendale                     | CA |    194,478%n"+
			"Glendora                     | CA |     50,719%n"+
			"Hanford                      | CA |     54,324%n"+
			"Hawthorne                    | CA |     85,681%n"+
			"Hayward                      | CA |    149,392%n"+
			"Hemet                        | CA |     81,046%n"+
			"Hesperia                     | CA |     92,062%n"+
			"Highland                     | CA |     54,154%n"+
			"Huntington Beach             | CA |    194,708%n"+
			"Huntington Park              | CA |     58,673%n"+
			"Indio                        | CA |     79,302%n"+
			"Inglewood                    | CA |    111,182%n"+
			"Irvine                       | CA |    229,985%n"+
			"Jurupa Valley                | CA |     97,426%n"+
			"La Habra                     | CA |     61,392%n"+
			"La Mesa                      | CA |     58,160%n"+
			"Laguna Niguel                | CA |     64,452%n"+
			"Lake Elsinore                | CA |     55,288%n"+
			"Lake Forest                  | CA |     78,853%n"+
			"Lakewood                     | CA |     80,833%n"+
			"Lancaster                    | CA |    159,055%n"+
			"Livermore                    | CA |     83,547%n"+
			"Lodi                         | CA |     63,301%n"+
			"Long Beach                   | CA |    467,892%n"+
			"Los Angeles                  | CA |  3,857,799%n"+
			"Lynwood                      | CA |     70,709%n"+
			"Madera                       | CA |     62,624%n"+
			"Manteca                      | CA |     71,067%n"+
			"Menifee                      | CA |     81,474%n"+
			"Merced                       | CA |     80,793%n"+
			"Milpitas                     | CA |     68,800%n"+
			"Mission Viejo                | CA |     95,290%n"+
			"Modesto                      | CA |    203,547%n"+
			"Montebello                   | CA |     63,305%n"+
			"Monterey Park                | CA |     60,937%n"+
			"Moreno Valley                | CA |    199,552%n"+
			"Mountain View                | CA |     76,621%n"+
			"Murrieta                     | CA |    106,810%n"+
			"Napa                         | CA |     78,340%n"+
			"National                     | CA |     59,387%n"+
			"Newport Beach                | CA |     87,068%n"+
			"Norwalk                      | CA |    106,278%n"+
			"Novato                       | CA |     53,301%n"+
			"Oakland                      | CA |    400,740%n"+
			"Oceanside                    | CA |    171,293%n"+
			"Ontario                      | CA |    167,211%n"+
			"Orange                       | CA |    139,419%n"+
			"Oxnard                       | CA |    201,555%n"+
			"Palm Desert                  | CA |     50,013%n"+
			"Palmdale                     | CA |    155,650%n"+
			"Palo Alto                    | CA |     66,363%n"+
			"Paramount                    | CA |     54,680%n"+
			"Pasadena                     | CA |    138,547%n"+
			"Perris                       | CA |     71,326%n"+
			"Petaluma                     | CA |     58,921%n"+
			"Pico Rivera                  | CA |     63,522%n"+
			"Pittsburg                    | CA |     65,664%n"+
			"Placentia                    | CA |     51,673%n"+
			"Pleasanton                   | CA |     72,338%n"+
			"Pomona                       | CA |    150,812%n"+
			"Porterville                  | CA |     55,023%n"+
			"Rancho Cordova               | CA |     66,997%n"+
			"Rancho Cucamonga             | CA |    170,746%n"+
			"Redding                      | CA |     90,755%n"+
			"Redlands                     | CA |     69,916%n"+
			"Redondo Beach                | CA |     67,693%n"+
			"Redwood                      | CA |     79,009%n"+
			"Rialto                       | CA |    101,740%n"+
			"Richmond                     | CA |    106,516%n"+
			"Riverside                    | CA |    313,673%n"+
			"Rocklin                      | CA |     59,030%n"+
			"Rosemead                     | CA |     54,393%n"+
			"Roseville                    | CA |    124,519%n"+
			"Sacramento                   | CA |    475,516%n"+
			"Salinas                      | CA |    154,484%n"+
			"San Bernardino               | CA |    213,295%n"+
			"San Buenaventura (Ventura)   | CA |    107,734%n"+
			"San Clemente                 | CA |     64,882%n"+
			"San Diego                    | CA |  1,338,348%n"+
			"San Francisco                | CA |    825,863%n"+
			"San Jose                     | CA |    982,765%n"+
			"San Leandro                  | CA |     86,890%n"+
			"San Marcos                   | CA |     86,752%n"+
			"San Mateo                    | CA |     99,670%n"+
			"San Rafael                   | CA |     58,502%n"+
			"San Ramon                    | CA |     73,927%n"+
			"Santa Ana                    | CA |    330,920%n"+
			"Santa Barbara                | CA |     89,639%n"+
			"Santa Clara                  | CA |    119,311%n"+
			"Santa Clarita                | CA |    179,013%n"+
			"Santa Cruz                   | CA |     62,041%n"+
			"Santa Maria                  | CA |    101,459%n"+
			"Santa Monica                 | CA |     91,812%n"+
			"Santa Rosa                   | CA |    170,685%n"+
			"Santee                       | CA |     55,343%n"+
			"Simi Valley                  | CA |    125,793%n"+
			"South Gate                   | CA |     95,304%n"+
			"South San Francisco          | CA |     65,547%n"+
			"Stockton                     | CA |    297,984%n"+
			"Sunnyvale                    | CA |    146,197%n"+
			"Temecula                     | CA |    105,208%n"+
			"Thousand Oaks                | CA |    128,412%n"+
			"Torrance                     | CA |    147,027%n"+
			"Tracy                        | CA |     84,669%n"+
			"Tulare                       | CA |     60,933%n"+
			"Turlock                      | CA |     69,733%n"+
			"Tustin                       | CA |     78,049%n"+
			"Union                        | CA |     71,763%n"+
			"Upland                       | CA |     75,209%n"+
			"Vacaville                    | CA |     93,899%n"+
			"Vallejo                      | CA |    117,796%n"+
			"Victorville                  | CA |    120,336%n"+
			"Visalia                      | CA |    127,081%n"+
			"Vista                        | CA |     96,047%n"+
			"Walnut Creek                 | CA |     65,695%n"+
			"Watsonville                  | CA |     51,881%n"+
			"West Covina                  | CA |    107,440%n"+
			"Westminster                  | CA |     91,377%n"+
			"Whittier                     | CA |     86,177%n"+
			"Woodland                     | CA |     56,271%n"+
			"Yorba Linda                  | CA |     66,735%n"+
			"Yuba                         | CA |     65,105%n"+
			"Yucaipa                      | CA |     52,265" );
	private static final String FL_BY_NAME = String.format(
			"Boca Raton                   | FL |     87,836%n"+
			"Boynton Beach                | FL |     70,101%n"+
			"Bradenton                    | FL |     50,672%n"+
			"Cape Coral                   | FL |    161,248%n"+
			"Clearwater                   | FL |    108,732%n"+
			"Coconut Creek                | FL |     55,001%n"+
			"Coral Springs                | FL |    125,287%n"+
			"Davie                        | FL |     95,489%n"+
			"Daytona Beach                | FL |     62,035%n"+
			"Deerfield Beach              | FL |     77,439%n"+
			"Delray Beach                 | FL |     62,357%n"+
			"Deltona                      | FL |     85,442%n"+
			"Fort Lauderdale              | FL |    170,747%n"+
			"Fort Myers                   | FL |     65,725%n"+
			"Gainesville                  | FL |    126,047%n"+
			"Hialeah                      | FL |    231,941%n"+
			"Hollywood                    | FL |    145,236%n"+
			"Homestead                    | FL |     63,190%n"+
			"Jacksonville                 | FL |    836,507%n"+
			"Jupiter                      | FL |     57,221%n"+
			"Kissimmee                    | FL |     63,369%n"+
			"Lakeland                     | FL |     99,999%n"+
			"Largo                        | FL |     77,878%n"+
			"Lauderhill                   | FL |     69,100%n"+
			"Margate                      | FL |     55,026%n"+
			"Melbourne                    | FL |     77,048%n"+
			"Miami                        | FL |    413,892%n"+
			"Miami Beach                  | FL |     90,588%n"+
			"Miami Gardens                | FL |    110,754%n"+
			"Miramar                      | FL |    128,729%n"+
			"North Miami                  | FL |     60,565%n"+
			"North Port                   | FL |     58,378%n"+
			"Ocala                        | FL |     56,945%n"+
			"Orlando                      | FL |    249,562%n"+
			"Palm Bay                     | FL |    104,124%n"+
			"Palm Coast                   | FL |     77,374%n"+
			"Pembroke Pines               | FL |    160,306%n"+
			"Pensacola                    | FL |     52,340%n"+
			"Plantation                   | FL |     88,016%n"+
			"Pompano Beach                | FL |    102,984%n"+
			"Port Orange                  | FL |     56,766%n"+
			"Port St. Lucie               | FL |    168,716%n"+
			"Sanford                      | FL |     54,651%n"+
			"Sarasota                     | FL |     52,811%n"+
			"St. Petersburg               | FL |    246,541%n"+
			"Sunrise                      | FL |     88,843%n"+
			"Tallahassee                  | FL |    186,971%n"+
			"Tamarac                      | FL |     62,557%n"+
			"Tampa                        | FL |    347,645%n"+
			"Wellington                   | FL |     58,679%n"+
			"West Palm Beach              | FL |    101,903%n"+
			"Weston                       | FL |     67,641" );
	private static final String WA_BY_POPULATION = String.format(
			"Kirkland                     | WA |     50,697%n"+
			"Richland                     | WA |     51,440%n"+
			"Shoreline                    | WA |     54,352%n"+
			"Redmond                      | WA |     56,561%n"+
			"Lakewood                     | WA |     58,852%n"+
			"Marysville                   | WA |     62,402%n"+
			"Pasco                        | WA |     65,398%n"+
			"Auburn                       | WA |     73,505%n"+
			"Kennewick                    | WA |     75,971%n"+
			"Bellingham                   | WA |     82,234%n"+
			"Spokane Valley               | WA |     90,641%n"+
			"Federal Way                  | WA |     91,933%n"+
			"Yakima                       | WA |     93,101%n"+
			"Renton                       | WA |     95,448%n"+
			"Everett                      | WA |    104,655%n"+
			"Kent                         | WA |    122,999%n"+
			"Bellevue                     | WA |    126,439%n"+
			"Vancouver                    | WA |    165,489%n"+
			"Tacoma                       | WA |    202,010%n"+
			"Spokane                      | WA |    209,525%n"+
			"Seattle                      | WA |    634,535"	);
	private static final String NY_BY_POPULATION = String.format(
			"Niagara Falls                | NY |     49,722%n"+
			"Troy                         | NY |     49,946%n"+
			"Hempstead                    | NY |     54,883%n"+
			"White Plains                 | NY |     57,403%n"+
			"Utica                        | NY |     61,822%n"+
			"Schenectady                  | NY |     66,078%n"+
			"Mount Vernon                 | NY |     67,896%n"+
			"New Rochelle                 | NY |     78,388%n"+
			"Albany                       | NY |     97,904%n"+
			"Syracuse                     | NY |    144,170%n"+
			"Yonkers                      | NY |    198,449%n"+
			"Rochester                    | NY |    210,532%n"+
			"Buffalo                      | NY |    259,384%n"+
			"New York                     | NY |  8,336,697" );
	private static final String CITIES_BY_NAME = String.format(
			"Abilene                      | TX |    118,887%n"+
			"Akron                        | OH |    198,549%n"+
			"Alameda                      | CA |     75,641%n"+
			"Albany                       | GA |     77,431%n"+
			"Albany                       | NY |     97,904%n"+
			"Albany                       | OR |     51,322%n"+
			"Albuquerque                  | NM |    555,417%n"+
			"Alexandria                   | VA |    146,294%n"+
			"Alhambra                     | CA |     84,322%n"+
			"Allen                        | TX |     89,640%n"+
			"Allentown                    | PA |    118,974%n"+
			"Alpharetta                   | GA |     61,981%n"+
			"Amarillo                     | TX |    195,250%n"+
			"Ames                         | IA |     60,634%n"+
			"Anaheim                      | CA |    343,248%n"+
			"Anchorage                    | AK |    298,610%n"+
			"Anderson                     | IN |     55,554%n"+
			"Ann Arbor                    | MI |    116,121%n"+
			"Antioch                      | CA |    105,508%n"+
			"Apple Valley                 | CA |     70,700%n"+
			"Appleton                     | WI |     73,016%n"+
			"Arcadia                      | CA |     57,497%n"+
			"Arlington                    | TX |    375,600%n"+
			"Arlington Heights            | IL |     75,777%n"+
			"Arvada                       | CO |    109,745%n"+
			"Asheville                    | NC |     85,712%n"+
			"Athens-Clarke County         | GA |    118,999%n"+
			"Atlanta                      | GA |    443,775%n"+
			"Auburn                       | AL |     56,908%n"+
			"Auburn                       | WA |     73,505%n"+
			"Augusta-Richmond County      | GA |    197,872%n"+
			"Aurora                       | CO |    339,030%n"+
			"Aurora                       | IL |    199,932%n"+
			"Austin                       | TX |    842,592%n"+
			"Avondale                     | AZ |     78,256%n"+
			"Bakersfield                  | CA |    358,597%n"+
			"Baldwin Park                 | CA |     76,419%n"+
			"Baltimore                    | MD |    621,342%n"+
			"Bartlett                     | TN |     55,945%n"+
			"Baton Rouge                  | LA |    230,058%n"+
			"Battle Creek                 | MI |     51,911%n"+
			"Bayonne                      | NJ |     64,416%n"+
			"Baytown                      | TX |     73,238%n"+
			"Beaumont                     | TX |    118,228%n"+
			"Beaverton                    | OR |     92,680%n"+
			"Bellevue                     | NE |     52,604%n"+
			"Bellevue                     | WA |    126,439%n"+
			"Bellflower                   | CA |     77,356%n"+
			"Bellingham                   | WA |     82,234%n"+
			"Bend                         | OR |     79,109%n"+
			"Berkeley                     | CA |    115,403%n"+
			"Berwyn                       | IL |     56,800%n"+
			"Bethlehem                    | PA |     75,103%n"+
			"Billings                     | MO |    106,954%n"+
			"Birmingham                   | AL |    212,038%n"+
			"Bismarck                     | ND |     64,751%n"+
			"Blaine                       | MN |     59,412%n"+
			"Bloomington                  | IL |     77,733%n"+
			"Bloomington                  | IN |     81,963%n"+
			"Bloomington                  | MN |     86,033%n"+
			"Blue Springs                 | MO |     53,014%n"+
			"Boca Raton                   | FL |     87,836%n"+
			"Boise                        | ID |    212,303%n"+
			"Bolingbrook                  | IL |     74,039%n"+
			"Bossier                      | LA |     64,655%n"+
			"Boston                       | MA |    636,479%n"+
			"Boulder                      | CO |    101,808%n"+
			"Bowie                        | MD |     56,129%n"+
			"Bowling Green                | KY |     60,600%n"+
			"Boynton Beach                | FL |     70,101%n"+
			"Bradenton                    | FL |     50,672%n"+
			"Brentwood                    | CA |     53,673%n"+
			"Bridgeport                   | CT |    146,425%n"+
			"Bristol                      | CT |     60,603%n"+
			"Brockton                     | MA |     94,094%n"+
			"Broken Arrow                 | OK |    102,019%n"+
			"Brooklyn Park                | MN |     77,752%n"+
			"Broomfield                   | CO |     58,298%n"+
			"Brownsville                  | TX |    180,097%n"+
			"Bryan                        | TX |     78,061%n"+
			"Buckeye                      | AZ |     54,542%n"+
			"Buena Park                   | CA |     82,155%n"+
			"Buffalo                      | NY |    259,384%n"+
			"Burbank                      | CA |    104,391%n"+
			"Burlington                   | NC |     51,306%n"+
			"Burnsville                   | MN |     61,130%n"+
			"Camarillo                    | CA |     65,968%n"+
			"Cambridge                    | MA |    106,471%n"+
			"Camden                       | NJ |     77,250%n"+
			"Canton                       | OH |     72,683%n"+
			"Cape Coral                   | FL |    161,248%n"+
			"Carlsbad                     | CA |    109,318%n"+
			"Carmel                       | IN |     83,565%n"+
			"Carrollton                   | TX |    125,409%n"+
			"Carson                       | CA |     93,002%n"+
			"Carson                       | NV |     54,838%n"+
			"Cary                         | NC |    145,693%n"+
			"Casper                       | WY |     57,813%n"+
			"Castle Rock                  | CO |     51,348%n"+
			"Cathedral                    | CA |     52,655%n"+
			"Cedar Park                   | TX |     57,957%n"+
			"Cedar Rapids                 | IA |    128,119%n"+
			"Centennial                   | CO |    103,743%n"+
			"Champaign                    | IL |     82,517%n"+
			"Chandler                     | AZ |    245,628%n"+
			"Chapel Hill                  | NC |     58,424%n"+
			"Charleston                   | SC |    125,583%n"+
			"Charleston                   | WV |     51,018%n"+
			"Charlotte                    | NC |    775,202%n"+
			"Chattanooga                  | TN |    171,279%n"+
			"Chesapeake                   | VA |    228,417%n"+
			"Cheyenne                     | WY |     61,537%n"+
			"Chicago                      | IL |  2,714,856%n"+
			"Chico                        | CA |     87,714%n"+
			"Chicopee                     | MA |     55,490%n"+
			"Chino                        | CA |     80,164%n"+
			"Chino Hills                  | CA |     76,457%n"+
			"Chula Vista                  | CA |    252,422%n"+
			"Cicero                       | IL |     84,137%n"+
			"Cincinnati                   | OH |    296,550%n"+
			"Citrus Heights               | CA |     84,870%n"+
			"Clarksville                  | TN |    142,519%n"+
			"Clearwater                   | FL |    108,732%n"+
			"Cleveland                    | OH |    390,928%n"+
			"Clifton                      | NJ |     84,722%n"+
			"Clovis                       | CA |     98,632%n"+
			"Coconut Creek                | FL |     55,001%n"+
			"College Station              | TX |     97,801%n"+
			"Colorado Springs             | CO |    431,834%n"+
			"Colton                       | CA |     53,123%n"+
			"Columbia                     | MO |    113,225%n"+
			"Columbia                     | SC |    131,686%n"+
			"Columbus                     | GA |    198,413%n"+
			"Columbus                     | OH |    809,798%n"+
			"Compton                      | CA |     97,559%n"+
			"Concord                      | CA |    124,711%n"+
			"Concord                      | NC |     81,981%n"+
			"Conroe                       | TX |     61,533%n"+
			"Conway                       | AR |     62,939%n"+
			"Coon Rapids                  | MN |     61,931%n"+
			"Coral Springs                | FL |    125,287%n"+
			"Corona                       | CA |    158,391%n"+
			"Corpus Christi               | TX |    312,195%n"+
			"Corvallis                    | OR |     54,998%n"+
			"Costa Mesa                   | CA |    111,918%n"+
			"Council Bluffs               | IA |     62,115%n"+
			"Cranston                     | RI |     80,529%n"+
			"Cupertino                    | CA |     60,009%n"+
			"Dallas                       | TX |  1,241,162%n"+
			"Daly                         | CA |    103,690%n"+
			"Danbury                      | CT |     82,807%n"+
			"Davenport                    | IA |    101,363%n"+
			"Davie                        | FL |     95,489%n"+
			"Davis                        | CA |     65,993%n"+
			"Dayton                       | OH |    141,359%n"+
			"Daytona Beach                | FL |     62,035%n"+
			"DeSoto                       | TX |     51,102%n"+
			"Dearborn                     | MI |     96,474%n"+
			"Dearborn Heights             | MI |     56,838%n"+
			"Decatur                      | AL |     55,996%n"+
			"Decatur                      | IL |     75,407%n"+
			"Deerfield Beach              | FL |     77,439%n"+
			"Delano                       | CA |     52,426%n"+
			"Delray Beach                 | FL |     62,357%n"+
			"Deltona                      | FL |     85,442%n"+
			"Denton                       | TX |    121,123%n"+
			"Denver                       | CO |    634,265%n"+
			"Des Moines                   | IA |    206,688%n"+
			"Des Plaines                  | IL |     58,840%n"+
			"Detroit                      | MI |    701,475%n"+
			"Diamond Bar                  | CA |     56,363%n"+
			"Dothan                       | AL |     67,382%n"+
			"Downey                       | CA |    112,873%n"+
			"Dubuque                      | IA |     58,155%n"+
			"Duluth                       | MN |     86,211%n"+
			"Durham                       | NC |    239,358%n"+
			"Eagan                        | MN |     64,854%n"+
			"East Orange                  | NJ |     64,268%n"+
			"Eastvale                     | CA |     54,635%n"+
			"Eau Claire                   | WI |     66,966%n"+
			"Eden Prairie                 | MN |     62,258%n"+
			"Edinburg                     | TX |     81,029%n"+
			"Edmond                       | OK |     84,885%n"+
			"El Cajon                     | CA |    101,435%n"+
			"El Monte                     | CA |    115,111%n"+
			"El Paso                      | TX |    672,538%n"+
			"Elgin                        | IL |    109,927%n"+
			"Elizabeth                    | NJ |    126,458%n"+
			"Elk Grove                    | CA |    159,038%n"+
			"Elkhart                      | IN |     51,152%n"+
			"Elyria                       | OH |     54,086%n"+
			"Encinitas                    | CA |     60,994%n"+
			"Erie                         | PA |    101,047%n"+
			"Escondido                    | CA |    147,575%n"+
			"Eugene                       | OR |    157,986%n"+
			"Euless                       | TX |     52,780%n"+
			"Evanston                     | IL |     75,430%n"+
			"Evansville                   | IN |    120,235%n"+
			"Everett                      | WA |    104,655%n"+
			"Fairfield                    | CA |    107,684%n"+
			"Fall River                   | MA |     88,945%n"+
			"Fargo                        | ND |    109,779%n"+
			"Farmington Hills             | MI |     80,756%n"+
			"Fayetteville                 | AR |     76,899%n"+
			"Fayetteville                 | NC |    202,103%n"+
			"Federal Way                  | WA |     91,933%n"+
			"Fishers                      | IN |     81,833%n"+
			"Flagstaff                    | AZ |     67,468%n"+
			"Flint                        | MI |    100,515%n"+
			"Florissant                   | MO |     52,252%n"+
			"Flower Mound                 | TX |     67,825%n"+
			"Folsom                       | CA |     73,384%n"+
			"Fontana                      | CA |    201,812%n"+
			"Fort Collins                 | CO |    148,612%n"+
			"Fort Lauderdale              | FL |    170,747%n"+
			"Fort Myers                   | FL |     65,725%n"+
			"Fort Smith                   | AR |     87,443%n"+
			"Fort Wayne                   | IN |    254,555%n"+
			"Fort Worth                   | TX |    777,992%n"+
			"Fountain Valley              | CA |     56,464%n"+
			"Franklin                     | TN |     66,280%n"+
			"Frederick                    | MD |     66,382%n"+
			"Fremont                      | CA |    221,986%n"+
			"Fresno                       | CA |    505,882%n"+
			"Frisco                       | TX |    128,176%n"+
			"Fullerton                    | CA |    138,574%n"+
			"Gainesville                  | FL |    126,047%n"+
			"Gaithersburg                 | MD |     62,794%n"+
			"Garden Grove                 | CA |    174,389%n"+
			"Gardena                      | CA |     59,490%n"+
			"Garland                      | TX |    233,564%n"+
			"Gary                         | IN |     79,170%n"+
			"Gastonia                     | NC |     72,723%n"+
			"Georgetown                   | TX |     52,303%n"+
			"Gilbert                      | AZ |    221,140%n"+
			"Gilroy                       | CA |     50,660%n"+
			"Glendale                     | AZ |    232,143%n"+
			"Glendale                     | CA |    194,478%n"+
			"Glendora                     | CA |     50,719%n"+
			"Goodyear                     | AZ |     69,648%n"+
			"Grand Forks                  | ND |     53,456%n"+
			"Grand Junction               | CO |     59,899%n"+
			"Grand Prairie                | TX |    181,824%n"+
			"Grand Rapids                 | MI |    190,411%n"+
			"Great Falls                  | MO |     58,893%n"+
			"Greeley                      | CO |     95,357%n"+
			"Green Bay                    | WI |    104,868%n"+
			"Greensboro                   | NC |    277,080%n"+
			"Greenville                   | NC |     87,242%n"+
			"Greenville                   | SC |     60,709%n"+
			"Greenwood                    | IN |     52,652%n"+
			"Gresham                      | OR |    108,956%n"+
			"Gulfport                     | MS |     70,113%n"+
			"Hamilton                     | OH |     62,295%n"+
			"Hammond                      | IN |     79,686%n"+
			"Hampton                      | VA |    136,836%n"+
			"Hanford                      | CA |     54,324%n"+
			"Harlingen                    | TX |     65,679%n"+
			"Harrisonburg                 | VA |     50,981%n"+
			"Hartford                     | CT |    124,893%n"+
			"Haverhill                    | MA |     61,797%n"+
			"Hawthorne                    | CA |     85,681%n"+
			"Hayward                      | CA |    149,392%n"+
			"Hemet                        | CA |     81,046%n"+
			"Hempstead                    | NY |     54,883%n"+
			"Henderson                    | NV |    265,679%n"+
			"Hendersonville               | TN |     53,080%n"+
			"Hesperia                     | CA |     92,062%n"+
			"Hialeah                      | FL |    231,941%n"+
			"High Point                   | NC |    106,586%n"+
			"Highland                     | CA |     54,154%n"+
			"Hillsboro                    | OR |     95,327%n"+
			"Hoboken                      | NJ |     52,034%n"+
			"Hoffman Estates              | IL |     52,305%n"+
			"Hollywood                    | FL |    145,236%n"+
			"Homestead                    | FL |     63,190%n"+
			"Honolulu                     | HI |    345,610%n"+
			"Hoover                       | AL |     83,412%n"+
			"Houston                      | TX |  2,160,821%n"+
			"Huntington Beach             | CA |    194,708%n"+
			"Huntington Park              | CA |     58,673%n"+
			"Huntsville                   | AL |    183,739%n"+
			"Idaho Falls                  | ID |     57,899%n"+
			"Independence                 | MO |    117,270%n"+
			"Indianapolis                 | IN |    834,852%n"+
			"Indio                        | CA |     79,302%n"+
			"Inglewood                    | CA |    111,182%n"+
			"Iowa                         | IA |     70,133%n"+
			"Irvine                       | CA |    229,985%n"+
			"Irving                       | TX |    225,427%n"+
			"Jackson                      | MS |    175,437%n"+
			"Jackson                      | TN |     67,265%n"+
			"Jacksonville                 | FL |    836,507%n"+
			"Jacksonville                 | NC |     69,220%n"+
			"Janesville                   | WI |     63,588%n"+
			"Jersey                       | NJ |    254,441%n"+
			"Johns Creek                  | GA |     82,306%n"+
			"Johnson                      | TN |     64,528%n"+
			"Joliet                       | IL |    148,268%n"+
			"Jonesboro                    | AR |     70,187%n"+
			"Joplin                       | MO |     49,526%n"+
			"Jupiter                      | FL |     57,221%n"+
			"Jurupa Valley                | CA |     97,426%n"+
			"Kalamazoo                    | MI |     75,092%n"+
			"Kansas                       | KS |    147,268%n"+
			"Kansas                       | MO |    464,310%n"+
			"Kenner                       | LA |     66,820%n"+
			"Kennewick                    | WA |     75,971%n"+
			"Kenosha                      | WI |    100,150%n"+
			"Kent                         | WA |    122,999%n"+
			"Kettering                    | OH |     55,990%n"+
			"Killeen                      | TX |    134,654%n"+
			"Kingsport                    | TN |     51,501%n"+
			"Kirkland                     | WA |     50,697%n"+
			"Kissimmee                    | FL |     63,369%n"+
			"Knoxville                    | TN |    182,200%n"+
			"Kokomo                       | IN |     56,866%n"+
			"La Crosse                    | WI |     51,647%n"+
			"La Habra                     | CA |     61,392%n"+
			"La Mesa                      | CA |     58,160%n"+
			"Lafayette                    | IN |     67,925%n"+
			"Lafayette                    | LA |    122,761%n"+
			"Laguna Niguel                | CA |     64,452%n"+
			"Lake Charles                 | LA |     73,474%n"+
			"Lake Elsinore                | CA |     55,288%n"+
			"Lake Forest                  | CA |     78,853%n"+
			"Lake Havasu                  | AZ |     52,819%n"+
			"Lakeland                     | FL |     99,999%n"+
			"Lakeville                    | MN |     57,342%n"+
			"Lakewood                     | CA |     80,833%n"+
			"Lakewood                     | CO |    145,516%n"+
			"Lakewood                     | OH |     51,385%n"+
			"Lakewood                     | WA |     58,852%n"+
			"Lancaster                    | CA |    159,055%n"+
			"Lancaster                    | PA |     59,360%n"+
			"Lansing                      | MI |    113,996%n"+
			"Laredo                       | TX |    244,731%n"+
			"Largo                        | FL |     77,878%n"+
			"Las Cruces                   | NM |    101,047%n"+
			"Las Vegas                    | NV |    596,424%n"+
			"Lauderhill                   | FL |     69,100%n"+
			"Lawrence                     | KS |     89,512%n"+
			"Lawrence                     | MA |     77,326%n"+
			"Lawton                       | OK |     98,376%n"+
			"Layton                       | UT |     68,677%n"+
			"League                       | TX |     88,188%n"+
			"Lee's Summit                 | MO |     92,468%n"+
			"Lehi                         | UT |     51,173%n"+
			"Lewisville                   | TX |     99,453%n"+
			"Lexington-Fayette            | KY |    305,489%n"+
			"Lincoln                      | NE |    265,404%n"+
			"Little Rock                  | AR |    196,537%n"+
			"Livermore                    | CA |     83,547%n"+
			"Livonia                      | MI |     95,586%n"+
			"Lodi                         | CA |     63,301%n"+
			"Long Beach                   | CA |    467,892%n"+
			"Longmont                     | CO |     88,669%n"+
			"Longview                     | TX |     81,092%n"+
			"Lorain                       | OH |     63,707%n"+
			"Los Angeles                  | CA |  3,857,799%n"+
			"Louisville/Jefferson County  | KY |    605,110%n"+
			"Loveland                     | CO |     70,223%n"+
			"Lowell                       | MA |    108,522%n"+
			"Lubbock                      | TX |    236,065%n"+
			"Lynchburg                    | VA |     77,113%n"+
			"Lynn                         | MA |     91,253%n"+
			"Lynwood                      | CA |     70,709%n"+
			"Macon                        | GA |     91,234%n"+
			"Madera                       | CA |     62,624%n"+
			"Madison                      | WI |    240,323%n"+
			"Malden                       | MA |     60,374%n"+
			"Manchester                   | NH |    110,209%n"+
			"Manhattan                    | KS |     56,069%n"+
			"Mansfield                    | TX |     59,317%n"+
			"Manteca                      | CA |     71,067%n"+
			"Maple Grove                  | MN |     64,420%n"+
			"Margate                      | FL |     55,026%n"+
			"Marietta                     | GA |     58,359%n"+
			"Marysville                   | WA |     62,402%n"+
			"McAllen                      | TX |    134,719%n"+
			"McKinney                     | TX |    143,223%n"+
			"Medford                      | MA |     57,033%n"+
			"Medford                      | OR |     76,462%n"+
			"Melbourne                    | FL |     77,048%n"+
			"Memphis                      | TN |    655,155%n"+
			"Menifee                      | CA |     81,474%n"+
			"Merced                       | CA |     80,793%n"+
			"Meriden                      | CT |     60,638%n"+
			"Meridian                     | ID |     80,386%n"+
			"Mesa                         | AZ |    452,084%n"+
			"Mesquite                     | TX |    143,195%n"+
			"Miami                        | FL |    413,892%n"+
			"Miami Beach                  | FL |     90,588%n"+
			"Miami Gardens                | FL |    110,754%n"+
			"Midland                      | TX |    119,385%n"+
			"Midwest                      | OK |     56,080%n"+
			"Milford                      | CT |     51,488%n"+
			"Milpitas                     | CA |     68,800%n"+
			"Milwaukee                    | WI |    598,916%n"+
			"Minneapolis                  | MN |    392,880%n"+
			"Minnetonka                   | MN |     51,123%n"+
			"Miramar                      | FL |    128,729%n"+
			"Mission                      | TX |     80,452%n"+
			"Mission Viejo                | CA |     95,290%n"+
			"Missoula                     | MO |     68,394%n"+
			"Missouri                     | TX |     69,020%n"+
			"Mobile                       | AL |    194,822%n"+
			"Modesto                      | CA |    203,547%n"+
			"Montebello                   | CA |     63,305%n"+
			"Monterey Park                | CA |     60,937%n"+
			"Montgomery                   | AL |    205,293%n"+
			"Moore                        | OK |     57,810%n"+
			"Moreno Valley                | CA |    199,552%n"+
			"Mount Pleasant               | SC |     71,875%n"+
			"Mount Prospect               | IL |     54,505%n"+
			"Mount Vernon                 | NY |     67,896%n"+
			"Mountain View                | CA |     76,621%n"+
			"Muncie                       | IN |     70,087%n"+
			"Murfreesboro                 | TN |    114,038%n"+
			"Murrieta                     | CA |    106,810%n"+
			"Nampa                        | ID |     83,930%n"+
			"Napa                         | CA |     78,340%n"+
			"Naperville                   | IL |    143,684%n"+
			"Nashua                       | NH |     86,933%n"+
			"Nashville-Davidson           | TN |    624,496%n"+
			"National                     | CA |     59,387%n"+
			"New Bedford                  | MA |     94,929%n"+
			"New Braunfels                | TX |     60,761%n"+
			"New Britain                  | CT |     73,153%n"+
			"New Brunswick                | NJ |     56,160%n"+
			"New Haven                    | CT |    130,741%n"+
			"New Orleans                  | LA |    369,250%n"+
			"New Rochelle                 | NY |     78,388%n"+
			"New York                     | NY |  8,336,697%n"+
			"Newark                       | NJ |    277,727%n"+
			"Newport Beach                | CA |     87,068%n"+
			"Newport News                 | VA |    180,726%n"+
			"Newton                       | MA |     86,307%n"+
			"Niagara Falls                | NY |     49,722%n"+
			"Noblesville                  | IN |     55,075%n"+
			"Norfolk                      | VA |    245,782%n"+
			"Normal                       | IL |     53,837%n"+
			"Norman                       | OK |    115,562%n"+
			"North Charleston             | SC |    101,989%n"+
			"North Las Vegas              | NV |    223,491%n"+
			"North Little Rock            | AR |     64,633%n"+
			"North Miami                  | FL |     60,565%n"+
			"North Port                   | FL |     58,378%n"+
			"North Richland Hills         | TX |     65,290%n"+
			"Norwalk                      | CA |    106,278%n"+
			"Norwalk                      | CT |     87,190%n"+
			"Novato                       | CA |     53,301%n"+
			"Novi                         | MI |     56,912%n"+
			"O'Fallon                     | MO |     81,979%n"+
			"Oak Lawn                     | IL |     56,995%n"+
			"Oak Park                     | IL |     52,015%n"+
			"Oakland                      | CA |    400,740%n"+
			"Ocala                        | FL |     56,945%n"+
			"Oceanside                    | CA |    171,293%n"+
			"Odessa                       | TX |    106,102%n"+
			"Ogden                        | UT |     83,793%n"+
			"Oklahoma                     | OK |    599,199%n"+
			"Olathe                       | KS |    130,045%n"+
			"Omaha                        | NE |    421,570%n"+
			"Ontario                      | CA |    167,211%n"+
			"Orange                       | CA |    139,419%n"+
			"Orem                         | UT |     90,749%n"+
			"Orland Park                  | IL |     57,392%n"+
			"Orlando                      | FL |    249,562%n"+
			"Oshkosh                      | WI |     66,653%n"+
			"Overland Park                | KS |    178,919%n"+
			"Owensboro                    | KY |     58,083%n"+
			"Oxnard                       | CA |    201,555%n"+
			"Palatine                     | IL |     69,144%n"+
			"Palm Bay                     | FL |    104,124%n"+
			"Palm Coast                   | FL |     77,374%n"+
			"Palm Desert                  | CA |     50,013%n"+
			"Palmdale                     | CA |    155,650%n"+
			"Palo Alto                    | CA |     66,363%n"+
			"Paramount                    | CA |     54,680%n"+
			"Parma                        | OH |     80,597%n"+
			"Pasadena                     | CA |    138,547%n"+
			"Pasadena                     | TX |    152,272%n"+
			"Pasco                        | WA |     65,398%n"+
			"Passaic                      | NJ |     70,218%n"+
			"Paterson                     | NJ |    145,219%n"+
			"Pawtucket                    | RI |     71,170%n"+
			"Peabody                      | MA |     51,867%n"+
			"Pearland                     | TX |     96,294%n"+
			"Pembroke Pines               | FL |    160,306%n"+
			"Pensacola                    | FL |     52,340%n"+
			"Peoria                       | AZ |    159,789%n"+
			"Peoria                       | IL |    115,687%n"+
			"Perris                       | CA |     71,326%n"+
			"Perth Amboy                  | NJ |     51,744%n"+
			"Petaluma                     | CA |     58,921%n"+
			"Pflugerville                 | TX |     51,894%n"+
			"Pharr                        | TX |     73,138%n"+
			"Philadelphia                 | PA |  1,547,607%n"+
			"Phoenix                      | AZ |  1,488,750%n"+
			"Pico Rivera                  | CA |     63,522%n"+
			"Pittsburg                    | CA |     65,664%n"+
			"Pittsburgh                   | PA |    306,211%n"+
			"Placentia                    | CA |     51,673%n"+
			"Plainfield                   | NJ |     50,244%n"+
			"Plano                        | TX |    272,068%n"+
			"Plantation                   | FL |     88,016%n"+
			"Pleasanton                   | CA |     72,338%n"+
			"Plymouth                     | MN |     72,928%n"+
			"Pocatello                    | ID |     54,777%n"+
			"Pomona                       | CA |    150,812%n"+
			"Pompano Beach                | FL |    102,984%n"+
			"Pontiac                      | MI |     60,175%n"+
			"Port Arthur                  | TX |     54,010%n"+
			"Port Orange                  | FL |     56,766%n"+
			"Port St. Lucie               | FL |    168,716%n"+
			"Porterville                  | CA |     55,023%n"+
			"Portland                     | ME |     66,214%n"+
			"Portland                     | OR |    603,106%n"+
			"Portsmouth                   | VA |     96,470%n"+
			"Providence                   | RI |    178,432%n"+
			"Provo                        | UT |    115,919%n"+
			"Pueblo                       | CO |    107,772%n"+
			"Quincy                       | MA |     93,027%n"+
			"Racine                       | WI |     78,303%n"+
			"Raleigh                      | NC |    423,179%n"+
			"Rancho Cordova               | CA |     66,997%n"+
			"Rancho Cucamonga             | CA |    170,746%n"+
			"Rapid                        | SD |     69,854%n"+
			"Reading                      | PA |     88,102%n"+
			"Redding                      | CA |     90,755%n"+
			"Redlands                     | CA |     69,916%n"+
			"Redmond                      | WA |     56,561%n"+
			"Redondo Beach                | CA |     67,693%n"+
			"Redwood                      | CA |     79,009%n"+
			"Reno                         | NV |    231,027%n"+
			"Renton                       | WA |     95,448%n"+
			"Revere                       | MA |     53,179%n"+
			"Rialto                       | CA |    101,740%n"+
			"Richardson                   | TX |    103,297%n"+
			"Richland                     | WA |     51,440%n"+
			"Richmond                     | CA |    106,516%n"+
			"Richmond                     | VA |    210,309%n"+
			"Rio Rancho                   | NM |     90,818%n"+
			"Riverside                    | CA |    313,673%n"+
			"Roanoke                      | VA |     97,469%n"+
			"Rochester                    | MN |    108,992%n"+
			"Rochester                    | NY |    210,532%n"+
			"Rochester Hills              | MI |     72,283%n"+
			"Rock Hill                    | SC |     68,094%n"+
			"Rockford                     | IL |    150,843%n"+
			"Rocklin                      | CA |     59,030%n"+
			"Rockville                    | MD |     63,244%n"+
			"Rocky Mount                  | NC |     57,136%n"+
			"Rogers                       | AR |     58,895%n"+
			"Rosemead                     | CA |     54,393%n"+
			"Roseville                    | CA |    124,519%n"+
			"Roswell                      | GA |     93,692%n"+
			"Round Rock                   | TX |    106,573%n"+
			"Rowlett                      | TX |     57,703%n"+
			"Royal Oak                    | MI |     58,410%n"+
			"Sacramento                   | CA |    475,516%n"+
			"Saginaw                      | MI |     50,790%n"+
			"Salem                        | OR |    157,429%n"+
			"Salinas                      | CA |    154,484%n"+
			"Salt Lake                    | UT |    189,314%n"+
			"San Angelo                   | TX |     95,887%n"+
			"San Antonio                  | TX |  1,382,951%n"+
			"San Bernardino               | CA |    213,295%n"+
			"San Buenaventura (Ventura)   | CA |    107,734%n"+
			"San Clemente                 | CA |     64,882%n"+
			"San Diego                    | CA |  1,338,348%n"+
			"San Francisco                | CA |    825,863%n"+
			"San Jose                     | CA |    982,765%n"+
			"San Leandro                  | CA |     86,890%n"+
			"San Marcos                   | CA |     86,752%n"+
			"San Marcos                   | TX |     50,001%n"+
			"San Mateo                    | CA |     99,670%n"+
			"San Rafael                   | CA |     58,502%n"+
			"San Ramon                    | CA |     73,927%n"+
			"Sandy                        | UT |     89,344%n"+
			"Sandy Springs                | GA |     99,419%n"+
			"Sanford                      | FL |     54,651%n"+
			"Santa Ana                    | CA |    330,920%n"+
			"Santa Barbara                | CA |     89,639%n"+
			"Santa Clara                  | CA |    119,311%n"+
			"Santa Clarita                | CA |    179,013%n"+
			"Santa Cruz                   | CA |     62,041%n"+
			"Santa Fe                     | NM |     69,204%n"+
			"Santa Maria                  | CA |    101,459%n"+
			"Santa Monica                 | CA |     91,812%n"+
			"Santa Rosa                   | CA |    170,685%n"+
			"Santee                       | CA |     55,343%n"+
			"Sarasota                     | FL |     52,811%n"+
			"Savannah                     | GA |    142,022%n"+
			"Schaumburg                   | IL |     74,781%n"+
			"Schenectady                  | NY |     66,078%n"+
			"Scottsdale                   | AZ |    223,514%n"+
			"Scranton                     | PA |     75,809%n"+
			"Seattle                      | WA |    634,535%n"+
			"Shawnee                      | KS |     63,622%n"+
			"Shoreline                    | WA |     54,352%n"+
			"Shreveport                   | LA |    201,867%n"+
			"Simi Valley                  | CA |    125,793%n"+
			"Sioux                        | IA |     82,719%n"+
			"Sioux Falls                  | SD |    159,908%n"+
			"Skokie                       | IL |     65,074%n"+
			"Smyrna                       | GA |     52,650%n"+
			"Somerville                   | MA |     77,104%n"+
			"South Bend                   | IN |    100,800%n"+
			"South Gate                   | CA |     95,304%n"+
			"South Jordan                 | UT |     55,934%n"+
			"South San Francisco          | CA |     65,547%n"+
			"Southaven                    | MS |     50,374%n"+
			"Southfield                   | MI |     72,507%n"+
			"Sparks                       | NV |     92,183%n"+
			"Spokane                      | WA |    209,525%n"+
			"Spokane Valley               | WA |     90,641%n"+
			"Springdale                   | AR |     73,123%n"+
			"Springfield                  | IL |    117,126%n"+
			"Springfield                  | MA |    153,552%n"+
			"Springfield                  | MO |    162,191%n"+
			"Springfield                  | OH |     60,147%n"+
			"Springfield                  | OR |     59,869%n"+
			"St. Charles                  | MO |     66,463%n"+
			"St. Clair Shores             | MI |     59,749%n"+
			"St. Cloud                    | MN |     65,986%n"+
			"St. George                   | UT |     75,561%n"+
			"St. Joseph                   | MO |     77,176%n"+
			"St. Louis                    | MO |    318,172%n"+
			"St. Paul                     | MN |    290,770%n"+
			"St. Peters                   | MO |     54,078%n"+
			"St. Petersburg               | FL |    246,541%n"+
			"Stamford                     | CT |    125,109%n"+
			"Sterling Heights             | MI |    130,410%n"+
			"Stockton                     | CA |    297,984%n"+
			"Suffolk                      | VA |     85,181%n"+
			"Sugar Land                   | TX |     82,480%n"+
			"Sunnyvale                    | CA |    146,197%n"+
			"Sunrise                      | FL |     88,843%n"+
			"Surprise                     | AZ |    121,287%n"+
			"Syracuse                     | NY |    144,170%n"+
			"Tacoma                       | WA |    202,010%n"+
			"Tallahassee                  | FL |    186,971%n"+
			"Tamarac                      | FL |     62,557%n"+
			"Tampa                        | FL |    347,645%n"+
			"Taunton                      | MA |     56,055%n"+
			"Taylor                       | MI |     62,114%n"+
			"Taylorsville                 | UT |     60,227%n"+
			"Temecula                     | CA |    105,208%n"+
			"Tempe                        | AZ |    166,842%n"+
			"Temple                       | TX |     69,148%n"+
			"Terre Haute                  | IN |     61,112%n"+
			"Thornton                     | CO |    124,140%n"+
			"Thousand Oaks                | CA |    128,412%n"+
			"Tinley Park                  | IL |     57,144%n"+
			"Toledo                       | OH |    284,012%n"+
			"Topeka                       | KS |    127,939%n"+
			"Torrance                     | CA |    147,027%n"+
			"Tracy                        | CA |     84,669%n"+
			"Trenton                      | NJ |     84,477%n"+
			"Troy                         | MI |     82,212%n"+
			"Troy                         | NY |     49,946%n"+
			"Tucson                       | AZ |    524,295%n"+
			"Tulare                       | CA |     60,933%n"+
			"Tulsa                        | OK |    393,987%n"+
			"Turlock                      | CA |     69,733%n"+
			"Tuscaloosa                   | AL |     93,357%n"+
			"Tustin                       | CA |     78,049%n"+
			"Tyler                        | TX |     99,323%n"+
			"Union                        | CA |     71,763%n"+
			"Union                        | NJ |     67,744%n"+
			"Upland                       | CA |     75,209%n"+
			"Utica                        | NY |     61,822%n"+
			"Vacaville                    | CA |     93,899%n"+
			"Valdosta                     | GA |     57,597%n"+
			"Vallejo                      | CA |    117,796%n"+
			"Vancouver                    | WA |    165,489%n"+
			"Victoria                     | TX |     64,376%n"+
			"Victorville                  | CA |    120,336%n"+
			"Vineland                     | NJ |     60,854%n"+
			"Virginia Beach               | VA |    447,021%n"+
			"Visalia                      | CA |    127,081%n"+
			"Vista                        | CA |     96,047%n"+
			"Waco                         | TX |    127,018%n"+
			"Walnut Creek                 | CA |     65,695%n"+
			"Waltham                      | MA |     61,918%n"+
			"Warner Robins                | GA |     70,712%n"+
			"Warren                       | MI |    134,141%n"+
			"Warwick                      | RI |     81,873%n"+
			"Washington                   | DC |    632,323%n"+
			"Waterbury                    | CT |    109,915%n"+
			"Waterloo                     | IA |     68,297%n"+
			"Watsonville                  | CA |     51,881%n"+
			"Waukegan                     | IL |     88,862%n"+
			"Waukesha                     | WI |     70,920%n"+
			"Wellington                   | FL |     58,679%n"+
			"West Allis                   | WI |     60,732%n"+
			"West Covina                  | CA |    107,440%n"+
			"West Des Moines              | IA |     59,296%n"+
			"West Haven                   | CT |     55,404%n"+
			"West Jordan                  | UT |    108,383%n"+
			"West NY                      | NJ |     51,464%n"+
			"West Palm Beach              | FL |    101,903%n"+
			"West Valley                  | UT |    132,434%n"+
			"Westland                     | MI |     82,883%n"+
			"Westminster                  | CA |     91,377%n"+
			"Westminster                  | CO |    109,169%n"+
			"Weston                       | FL |     67,641%n"+
			"Weymouth                     | MA |     54,906%n"+
			"Wheaton                      | IL |     53,469%n"+
			"White Plains                 | NY |     57,403%n"+
			"Whittier                     | CA |     86,177%n"+
			"Wichita                      | KS |    385,577%n"+
			"Wichita Falls                | TX |    104,552%n"+
			"Wilmington                   | DE |     71,292%n"+
			"Wilmington                   | NC |    109,922%n"+
			"Winston-Salem                | NC |    234,349%n"+
			"Woodbury                     | MN |     64,496%n"+
			"Woodland                     | CA |     56,271%n"+
			"Worcester                    | MA |    182,669%n"+
			"Wyoming                      | MI |     73,371%n"+
			"Yakima                       | WA |     93,101%n"+
			"Yonkers                      | NY |    198,449%n"+
			"Yorba Linda                  | CA |     66,735%n"+
			"Youngstown                   | OH |     65,405%n"+
			"Yuba                         | CA |     65,105%n"+
			"Yucaipa                      | CA |     52,265%n"+
			"Yuma                         | AZ |     95,429" );
	private static final String CITIES_BY_POPULATION = String.format(
			"Joplin                       | MO |     49,526%n"+
					"Niagara Falls                | NY |     49,722%n"+
					"Troy                         | NY |     49,946%n"+
					"San Marcos                   | TX |     50,001%n"+
					"Palm Desert                  | CA |     50,013%n"+
					"Plainfield                   | NJ |     50,244%n"+
					"Southaven                    | MS |     50,374%n"+
					"Gilroy                       | CA |     50,660%n"+
					"Bradenton                    | FL |     50,672%n"+
					"Kirkland                     | WA |     50,697%n"+
					"Glendora                     | CA |     50,719%n"+
					"Saginaw                      | MI |     50,790%n"+
					"Harrisonburg                 | VA |     50,981%n"+
					"Charleston                   | WV |     51,018%n"+
					"DeSoto                       | TX |     51,102%n"+
					"Minnetonka                   | MN |     51,123%n"+
					"Elkhart                      | IN |     51,152%n"+
					"Lehi                         | UT |     51,173%n"+
					"Burlington                   | NC |     51,306%n"+
					"Albany                       | OR |     51,322%n"+
					"Castle Rock                  | CO |     51,348%n"+
					"Lakewood                     | OH |     51,385%n"+
					"Richland                     | WA |     51,440%n"+
					"West NY                      | NJ |     51,464%n"+
					"Milford                      | CT |     51,488%n"+
					"Kingsport                    | TN |     51,501%n"+
					"La Crosse                    | WI |     51,647%n"+
					"Placentia                    | CA |     51,673%n"+
					"Perth Amboy                  | NJ |     51,744%n"+
					"Peabody                      | MA |     51,867%n"+
					"Watsonville                  | CA |     51,881%n"+
					"Pflugerville                 | TX |     51,894%n"+
					"Battle Creek                 | MI |     51,911%n"+
					"Oak Park                     | IL |     52,015%n"+
					"Hoboken                      | NJ |     52,034%n"+
					"Florissant                   | MO |     52,252%n"+
					"Yucaipa                      | CA |     52,265%n"+
					"Georgetown                   | TX |     52,303%n"+
					"Hoffman Estates              | IL |     52,305%n"+
					"Pensacola                    | FL |     52,340%n"+
					"Delano                       | CA |     52,426%n"+
					"Bellevue                     | NE |     52,604%n"+
					"Smyrna                       | GA |     52,650%n"+
					"Greenwood                    | IN |     52,652%n"+
					"Cathedral                    | CA |     52,655%n"+
					"Euless                       | TX |     52,780%n"+
					"Sarasota                     | FL |     52,811%n"+
					"Lake Havasu                  | AZ |     52,819%n"+
					"Blue Springs                 | MO |     53,014%n"+
					"Hendersonville               | TN |     53,080%n"+
					"Colton                       | CA |     53,123%n"+
					"Revere                       | MA |     53,179%n"+
					"Novato                       | CA |     53,301%n"+
					"Grand Forks                  | ND |     53,456%n"+
					"Wheaton                      | IL |     53,469%n"+
					"Brentwood                    | CA |     53,673%n"+
					"Normal                       | IL |     53,837%n"+
					"Port Arthur                  | TX |     54,010%n"+
					"St. Peters                   | MO |     54,078%n"+
					"Elyria                       | OH |     54,086%n"+
					"Highland                     | CA |     54,154%n"+
					"Hanford                      | CA |     54,324%n"+
					"Shoreline                    | WA |     54,352%n"+
					"Rosemead                     | CA |     54,393%n"+
					"Mount Prospect               | IL |     54,505%n"+
					"Buckeye                      | AZ |     54,542%n"+
					"Eastvale                     | CA |     54,635%n"+
					"Sanford                      | FL |     54,651%n"+
					"Paramount                    | CA |     54,680%n"+
					"Pocatello                    | ID |     54,777%n"+
					"Carson                       | NV |     54,838%n"+
					"Hempstead                    | NY |     54,883%n"+
					"Weymouth                     | MA |     54,906%n"+
					"Corvallis                    | OR |     54,998%n"+
					"Coconut Creek                | FL |     55,001%n"+
					"Porterville                  | CA |     55,023%n"+
					"Margate                      | FL |     55,026%n"+
					"Noblesville                  | IN |     55,075%n"+
					"Lake Elsinore                | CA |     55,288%n"+
					"Santee                       | CA |     55,343%n"+
					"West Haven                   | CT |     55,404%n"+
					"Chicopee                     | MA |     55,490%n"+
					"Anderson                     | IN |     55,554%n"+
					"South Jordan                 | UT |     55,934%n"+
					"Bartlett                     | TN |     55,945%n"+
					"Kettering                    | OH |     55,990%n"+
					"Decatur                      | AL |     55,996%n"+
					"Taunton                      | MA |     56,055%n"+
					"Manhattan                    | KS |     56,069%n"+
					"Midwest                      | OK |     56,080%n"+
					"Bowie                        | MD |     56,129%n"+
					"New Brunswick                | NJ |     56,160%n"+
					"Woodland                     | CA |     56,271%n"+
					"Diamond Bar                  | CA |     56,363%n"+
					"Fountain Valley              | CA |     56,464%n"+
					"Redmond                      | WA |     56,561%n"+
					"Port Orange                  | FL |     56,766%n"+
					"Berwyn                       | IL |     56,800%n"+
					"Dearborn Heights             | MI |     56,838%n"+
					"Kokomo                       | IN |     56,866%n"+
					"Auburn                       | AL |     56,908%n"+
					"Novi                         | MI |     56,912%n"+
					"Ocala                        | FL |     56,945%n"+
					"Oak Lawn                     | IL |     56,995%n"+
					"Medford                      | MA |     57,033%n"+
					"Rocky Mount                  | NC |     57,136%n"+
					"Tinley Park                  | IL |     57,144%n"+
					"Jupiter                      | FL |     57,221%n"+
					"Lakeville                    | MN |     57,342%n"+
					"Orland Park                  | IL |     57,392%n"+
					"White Plains                 | NY |     57,403%n"+
					"Arcadia                      | CA |     57,497%n"+
					"Valdosta                     | GA |     57,597%n"+
					"Rowlett                      | TX |     57,703%n"+
					"Moore                        | OK |     57,810%n"+
					"Casper                       | WY |     57,813%n"+
					"Idaho Falls                  | ID |     57,899%n"+
					"Cedar Park                   | TX |     57,957%n"+
					"Owensboro                    | KY |     58,083%n"+
					"Dubuque                      | IA |     58,155%n"+
					"La Mesa                      | CA |     58,160%n"+
					"Broomfield                   | CO |     58,298%n"+
					"Marietta                     | GA |     58,359%n"+
					"North Port                   | FL |     58,378%n"+
					"Royal Oak                    | MI |     58,410%n"+
					"Chapel Hill                  | NC |     58,424%n"+
					"San Rafael                   | CA |     58,502%n"+
					"Huntington Park              | CA |     58,673%n"+
					"Wellington                   | FL |     58,679%n"+
					"Des Plaines                  | IL |     58,840%n"+
					"Lakewood                     | WA |     58,852%n"+
					"Great Falls                  | MO |     58,893%n"+
					"Rogers                       | AR |     58,895%n"+
					"Petaluma                     | CA |     58,921%n"+
					"Rocklin                      | CA |     59,030%n"+
					"West Des Moines              | IA |     59,296%n"+
					"Mansfield                    | TX |     59,317%n"+
					"Lancaster                    | PA |     59,360%n"+
					"National                     | CA |     59,387%n"+
					"Blaine                       | MN |     59,412%n"+
					"Gardena                      | CA |     59,490%n"+
					"St. Clair Shores             | MI |     59,749%n"+
					"Springfield                  | OR |     59,869%n"+
					"Grand Junction               | CO |     59,899%n"+
					"Cupertino                    | CA |     60,009%n"+
					"Springfield                  | OH |     60,147%n"+
					"Pontiac                      | MI |     60,175%n"+
					"Taylorsville                 | UT |     60,227%n"+
					"Malden                       | MA |     60,374%n"+
					"North Miami                  | FL |     60,565%n"+
					"Bowling Green                | KY |     60,600%n"+
					"Bristol                      | CT |     60,603%n"+
					"Ames                         | IA |     60,634%n"+
					"Meriden                      | CT |     60,638%n"+
					"Greenville                   | SC |     60,709%n"+
					"West Allis                   | WI |     60,732%n"+
					"New Braunfels                | TX |     60,761%n"+
					"Vineland                     | NJ |     60,854%n"+
					"Tulare                       | CA |     60,933%n"+
					"Monterey Park                | CA |     60,937%n"+
					"Encinitas                    | CA |     60,994%n"+
					"Terre Haute                  | IN |     61,112%n"+
					"Burnsville                   | MN |     61,130%n"+
					"La Habra                     | CA |     61,392%n"+
					"Conroe                       | TX |     61,533%n"+
					"Cheyenne                     | WY |     61,537%n"+
					"Haverhill                    | MA |     61,797%n"+
					"Utica                        | NY |     61,822%n"+
					"Waltham                      | MA |     61,918%n"+
					"Coon Rapids                  | MN |     61,931%n"+
					"Alpharetta                   | GA |     61,981%n"+
					"Daytona Beach                | FL |     62,035%n"+
					"Santa Cruz                   | CA |     62,041%n"+
					"Taylor                       | MI |     62,114%n"+
					"Council Bluffs               | IA |     62,115%n"+
					"Eden Prairie                 | MN |     62,258%n"+
					"Hamilton                     | OH |     62,295%n"+
					"Delray Beach                 | FL |     62,357%n"+
					"Marysville                   | WA |     62,402%n"+
					"Tamarac                      | FL |     62,557%n"+
					"Madera                       | CA |     62,624%n"+
					"Gaithersburg                 | MD |     62,794%n"+
					"Conway                       | AR |     62,939%n"+
					"Homestead                    | FL |     63,190%n"+
					"Rockville                    | MD |     63,244%n"+
					"Lodi                         | CA |     63,301%n"+
					"Montebello                   | CA |     63,305%n"+
					"Kissimmee                    | FL |     63,369%n"+
					"Pico Rivera                  | CA |     63,522%n"+
					"Janesville                   | WI |     63,588%n"+
					"Shawnee                      | KS |     63,622%n"+
					"Lorain                       | OH |     63,707%n"+
					"East Orange                  | NJ |     64,268%n"+
					"Victoria                     | TX |     64,376%n"+
					"Bayonne                      | NJ |     64,416%n"+
					"Maple Grove                  | MN |     64,420%n"+
					"Laguna Niguel                | CA |     64,452%n"+
					"Woodbury                     | MN |     64,496%n"+
					"Johnson                      | TN |     64,528%n"+
					"North Little Rock            | AR |     64,633%n"+
					"Bossier                      | LA |     64,655%n"+
					"Bismarck                     | ND |     64,751%n"+
					"Eagan                        | MN |     64,854%n"+
					"San Clemente                 | CA |     64,882%n"+
					"Skokie                       | IL |     65,074%n"+
					"Yuba                         | CA |     65,105%n"+
					"North Richland Hills         | TX |     65,290%n"+
					"Pasco                        | WA |     65,398%n"+
					"Youngstown                   | OH |     65,405%n"+
					"South San Francisco          | CA |     65,547%n"+
					"Pittsburg                    | CA |     65,664%n"+
					"Harlingen                    | TX |     65,679%n"+
					"Walnut Creek                 | CA |     65,695%n"+
					"Fort Myers                   | FL |     65,725%n"+
					"Camarillo                    | CA |     65,968%n"+
					"St. Cloud                    | MN |     65,986%n"+
					"Davis                        | CA |     65,993%n"+
					"Schenectady                  | NY |     66,078%n"+
					"Portland                     | ME |     66,214%n"+
					"Franklin                     | TN |     66,280%n"+
					"Palo Alto                    | CA |     66,363%n"+
					"Frederick                    | MD |     66,382%n"+
					"St. Charles                  | MO |     66,463%n"+
					"Oshkosh                      | WI |     66,653%n"+
					"Yorba Linda                  | CA |     66,735%n"+
					"Kenner                       | LA |     66,820%n"+
					"Eau Claire                   | WI |     66,966%n"+
					"Rancho Cordova               | CA |     66,997%n"+
					"Jackson                      | TN |     67,265%n"+
					"Dothan                       | AL |     67,382%n"+
					"Flagstaff                    | AZ |     67,468%n"+
					"Weston                       | FL |     67,641%n"+
					"Redondo Beach                | CA |     67,693%n"+
					"Union                        | NJ |     67,744%n"+
					"Flower Mound                 | TX |     67,825%n"+
					"Mount Vernon                 | NY |     67,896%n"+
					"Lafayette                    | IN |     67,925%n"+
					"Rock Hill                    | SC |     68,094%n"+
					"Waterloo                     | IA |     68,297%n"+
					"Missoula                     | MO |     68,394%n"+
					"Layton                       | UT |     68,677%n"+
					"Milpitas                     | CA |     68,800%n"+
					"Missouri                     | TX |     69,020%n"+
					"Lauderhill                   | FL |     69,100%n"+
					"Palatine                     | IL |     69,144%n"+
					"Temple                       | TX |     69,148%n"+
					"Santa Fe                     | NM |     69,204%n"+
					"Jacksonville                 | NC |     69,220%n"+
					"Goodyear                     | AZ |     69,648%n"+
					"Turlock                      | CA |     69,733%n"+
					"Rapid                        | SD |     69,854%n"+
					"Redlands                     | CA |     69,916%n"+
					"Muncie                       | IN |     70,087%n"+
					"Boynton Beach                | FL |     70,101%n"+
					"Gulfport                     | MS |     70,113%n"+
					"Iowa                         | IA |     70,133%n"+
					"Jonesboro                    | AR |     70,187%n"+
					"Passaic                      | NJ |     70,218%n"+
					"Loveland                     | CO |     70,223%n"+
					"Apple Valley                 | CA |     70,700%n"+
					"Lynwood                      | CA |     70,709%n"+
					"Warner Robins                | GA |     70,712%n"+
					"Waukesha                     | WI |     70,920%n"+
					"Manteca                      | CA |     71,067%n"+
					"Pawtucket                    | RI |     71,170%n"+
					"Wilmington                   | DE |     71,292%n"+
					"Perris                       | CA |     71,326%n"+
					"Union                        | CA |     71,763%n"+
					"Mount Pleasant               | SC |     71,875%n"+
					"Rochester Hills              | MI |     72,283%n"+
					"Pleasanton                   | CA |     72,338%n"+
					"Southfield                   | MI |     72,507%n"+
					"Canton                       | OH |     72,683%n"+
					"Gastonia                     | NC |     72,723%n"+
					"Plymouth                     | MN |     72,928%n"+
					"Appleton                     | WI |     73,016%n"+
					"Springdale                   | AR |     73,123%n"+
					"Pharr                        | TX |     73,138%n"+
					"New Britain                  | CT |     73,153%n"+
					"Baytown                      | TX |     73,238%n"+
					"Wyoming                      | MI |     73,371%n"+
					"Folsom                       | CA |     73,384%n"+
					"Lake Charles                 | LA |     73,474%n"+
					"Auburn                       | WA |     73,505%n"+
					"San Ramon                    | CA |     73,927%n"+
					"Bolingbrook                  | IL |     74,039%n"+
					"Schaumburg                   | IL |     74,781%n"+
					"Kalamazoo                    | MI |     75,092%n"+
					"Bethlehem                    | PA |     75,103%n"+
					"Upland                       | CA |     75,209%n"+
					"Decatur                      | IL |     75,407%n"+
					"Evanston                     | IL |     75,430%n"+
					"St. George                   | UT |     75,561%n"+
					"Alameda                      | CA |     75,641%n"+
					"Arlington Heights            | IL |     75,777%n"+
					"Scranton                     | PA |     75,809%n"+
					"Kennewick                    | WA |     75,971%n"+
					"Baldwin Park                 | CA |     76,419%n"+
					"Chino Hills                  | CA |     76,457%n"+
					"Medford                      | OR |     76,462%n"+
					"Mountain View                | CA |     76,621%n"+
					"Fayetteville                 | AR |     76,899%n"+
					"Melbourne                    | FL |     77,048%n"+
					"Somerville                   | MA |     77,104%n"+
					"Lynchburg                    | VA |     77,113%n"+
					"St. Joseph                   | MO |     77,176%n"+
					"Camden                       | NJ |     77,250%n"+
					"Lawrence                     | MA |     77,326%n"+
					"Bellflower                   | CA |     77,356%n"+
					"Palm Coast                   | FL |     77,374%n"+
					"Albany                       | GA |     77,431%n"+
					"Deerfield Beach              | FL |     77,439%n"+
					"Bloomington                  | IL |     77,733%n"+
					"Brooklyn Park                | MN |     77,752%n"+
					"Largo                        | FL |     77,878%n"+
					"Tustin                       | CA |     78,049%n"+
					"Bryan                        | TX |     78,061%n"+
					"Avondale                     | AZ |     78,256%n"+
					"Racine                       | WI |     78,303%n"+
					"Napa                         | CA |     78,340%n"+
					"New Rochelle                 | NY |     78,388%n"+
					"Lake Forest                  | CA |     78,853%n"+
					"Redwood                      | CA |     79,009%n"+
					"Bend                         | OR |     79,109%n"+
					"Gary                         | IN |     79,170%n"+
					"Indio                        | CA |     79,302%n"+
					"Hammond                      | IN |     79,686%n"+
					"Chino                        | CA |     80,164%n"+
					"Meridian                     | ID |     80,386%n"+
					"Mission                      | TX |     80,452%n"+
					"Cranston                     | RI |     80,529%n"+
					"Parma                        | OH |     80,597%n"+
					"Farmington Hills             | MI |     80,756%n"+
					"Merced                       | CA |     80,793%n"+
					"Lakewood                     | CA |     80,833%n"+
					"Edinburg                     | TX |     81,029%n"+
					"Hemet                        | CA |     81,046%n"+
					"Longview                     | TX |     81,092%n"+
					"Menifee                      | CA |     81,474%n"+
					"Fishers                      | IN |     81,833%n"+
					"Warwick                      | RI |     81,873%n"+
					"Bloomington                  | IN |     81,963%n"+
					"O'Fallon                     | MO |     81,979%n"+
					"Concord                      | NC |     81,981%n"+
					"Buena Park                   | CA |     82,155%n"+
					"Troy                         | MI |     82,212%n"+
					"Bellingham                   | WA |     82,234%n"+
					"Johns Creek                  | GA |     82,306%n"+
					"Sugar Land                   | TX |     82,480%n"+
					"Champaign                    | IL |     82,517%n"+
					"Sioux                        | IA |     82,719%n"+
					"Danbury                      | CT |     82,807%n"+
					"Westland                     | MI |     82,883%n"+
					"Hoover                       | AL |     83,412%n"+
					"Livermore                    | CA |     83,547%n"+
					"Carmel                       | IN |     83,565%n"+
					"Ogden                        | UT |     83,793%n"+
					"Nampa                        | ID |     83,930%n"+
					"Cicero                       | IL |     84,137%n"+
					"Alhambra                     | CA |     84,322%n"+
					"Trenton                      | NJ |     84,477%n"+
					"Tracy                        | CA |     84,669%n"+
					"Clifton                      | NJ |     84,722%n"+
					"Citrus Heights               | CA |     84,870%n"+
					"Edmond                       | OK |     84,885%n"+
					"Suffolk                      | VA |     85,181%n"+
					"Deltona                      | FL |     85,442%n"+
					"Hawthorne                    | CA |     85,681%n"+
					"Asheville                    | NC |     85,712%n"+
					"Bloomington                  | MN |     86,033%n"+
					"Whittier                     | CA |     86,177%n"+
					"Duluth                       | MN |     86,211%n"+
					"Newton                       | MA |     86,307%n"+
					"San Marcos                   | CA |     86,752%n"+
					"San Leandro                  | CA |     86,890%n"+
					"Nashua                       | NH |     86,933%n"+
					"Newport Beach                | CA |     87,068%n"+
					"Norwalk                      | CT |     87,190%n"+
					"Greenville                   | NC |     87,242%n"+
					"Fort Smith                   | AR |     87,443%n"+
					"Chico                        | CA |     87,714%n"+
					"Boca Raton                   | FL |     87,836%n"+
					"Plantation                   | FL |     88,016%n"+
					"Reading                      | PA |     88,102%n"+
					"League                       | TX |     88,188%n"+
					"Longmont                     | CO |     88,669%n"+
					"Sunrise                      | FL |     88,843%n"+
					"Waukegan                     | IL |     88,862%n"+
					"Fall River                   | MA |     88,945%n"+
					"Sandy                        | UT |     89,344%n"+
					"Lawrence                     | KS |     89,512%n"+
					"Santa Barbara                | CA |     89,639%n"+
					"Allen                        | TX |     89,640%n"+
					"Miami Beach                  | FL |     90,588%n"+
					"Spokane Valley               | WA |     90,641%n"+
					"Orem                         | UT |     90,749%n"+
					"Redding                      | CA |     90,755%n"+
					"Rio Rancho                   | NM |     90,818%n"+
					"Macon                        | GA |     91,234%n"+
					"Lynn                         | MA |     91,253%n"+
					"Westminster                  | CA |     91,377%n"+
					"Santa Monica                 | CA |     91,812%n"+
					"Federal Way                  | WA |     91,933%n"+
					"Hesperia                     | CA |     92,062%n"+
					"Sparks                       | NV |     92,183%n"+
					"Lee's Summit                 | MO |     92,468%n"+
					"Beaverton                    | OR |     92,680%n"+
					"Carson                       | CA |     93,002%n"+
					"Quincy                       | MA |     93,027%n"+
					"Yakima                       | WA |     93,101%n"+
					"Tuscaloosa                   | AL |     93,357%n"+
					"Roswell                      | GA |     93,692%n"+
					"Vacaville                    | CA |     93,899%n"+
					"Brockton                     | MA |     94,094%n"+
					"New Bedford                  | MA |     94,929%n"+
					"Mission Viejo                | CA |     95,290%n"+
					"South Gate                   | CA |     95,304%n"+
					"Hillsboro                    | OR |     95,327%n"+
					"Greeley                      | CO |     95,357%n"+
					"Yuma                         | AZ |     95,429%n"+
					"Renton                       | WA |     95,448%n"+
					"Davie                        | FL |     95,489%n"+
					"Livonia                      | MI |     95,586%n"+
					"San Angelo                   | TX |     95,887%n"+
					"Vista                        | CA |     96,047%n"+
					"Pearland                     | TX |     96,294%n"+
					"Portsmouth                   | VA |     96,470%n"+
					"Dearborn                     | MI |     96,474%n"+
					"Jurupa Valley                | CA |     97,426%n"+
					"Roanoke                      | VA |     97,469%n"+
					"Compton                      | CA |     97,559%n"+
					"College Station              | TX |     97,801%n"+
					"Albany                       | NY |     97,904%n"+
					"Lawton                       | OK |     98,376%n"+
					"Clovis                       | CA |     98,632%n"+
					"Tyler                        | TX |     99,323%n"+
					"Sandy Springs                | GA |     99,419%n"+
					"Lewisville                   | TX |     99,453%n"+
					"San Mateo                    | CA |     99,670%n"+
					"Lakeland                     | FL |     99,999%n"+
					"Kenosha                      | WI |    100,150%n"+
					"Flint                        | MI |    100,515%n"+
					"South Bend                   | IN |    100,800%n"+
					"Erie                         | PA |    101,047%n"+
					"Las Cruces                   | NM |    101,047%n"+
					"Davenport                    | IA |    101,363%n"+
					"El Cajon                     | CA |    101,435%n"+
					"Santa Maria                  | CA |    101,459%n"+
					"Rialto                       | CA |    101,740%n"+
					"Boulder                      | CO |    101,808%n"+
					"West Palm Beach              | FL |    101,903%n"+
					"North Charleston             | SC |    101,989%n"+
					"Broken Arrow                 | OK |    102,019%n"+
					"Pompano Beach                | FL |    102,984%n"+
					"Richardson                   | TX |    103,297%n"+
					"Daly                         | CA |    103,690%n"+
					"Centennial                   | CO |    103,743%n"+
					"Palm Bay                     | FL |    104,124%n"+
					"Burbank                      | CA |    104,391%n"+
					"Wichita Falls                | TX |    104,552%n"+
					"Everett                      | WA |    104,655%n"+
					"Green Bay                    | WI |    104,868%n"+
					"Temecula                     | CA |    105,208%n"+
					"Antioch                      | CA |    105,508%n"+
					"Odessa                       | TX |    106,102%n"+
					"Norwalk                      | CA |    106,278%n"+
					"Cambridge                    | MA |    106,471%n"+
					"Richmond                     | CA |    106,516%n"+
					"Round Rock                   | TX |    106,573%n"+
					"High Point                   | NC |    106,586%n"+
					"Murrieta                     | CA |    106,810%n"+
					"Billings                     | MO |    106,954%n"+
					"West Covina                  | CA |    107,440%n"+
					"Fairfield                    | CA |    107,684%n"+
					"San Buenaventura (Ventura)   | CA |    107,734%n"+
					"Pueblo                       | CO |    107,772%n"+
					"West Jordan                  | UT |    108,383%n"+
					"Lowell                       | MA |    108,522%n"+
					"Clearwater                   | FL |    108,732%n"+
					"Gresham                      | OR |    108,956%n"+
					"Rochester                    | MN |    108,992%n"+
					"Westminster                  | CO |    109,169%n"+
					"Carlsbad                     | CA |    109,318%n"+
					"Arvada                       | CO |    109,745%n"+
					"Fargo                        | ND |    109,779%n"+
					"Waterbury                    | CT |    109,915%n"+
					"Wilmington                   | NC |    109,922%n"+
					"Elgin                        | IL |    109,927%n"+
					"Manchester                   | NH |    110,209%n"+
					"Miami Gardens                | FL |    110,754%n"+
					"Inglewood                    | CA |    111,182%n"+
					"Costa Mesa                   | CA |    111,918%n"+
					"Downey                       | CA |    112,873%n"+
					"Columbia                     | MO |    113,225%n"+
					"Lansing                      | MI |    113,996%n"+
					"Murfreesboro                 | TN |    114,038%n"+
					"El Monte                     | CA |    115,111%n"+
					"Berkeley                     | CA |    115,403%n"+
					"Norman                       | OK |    115,562%n"+
					"Peoria                       | IL |    115,687%n"+
					"Provo                        | UT |    115,919%n"+
					"Ann Arbor                    | MI |    116,121%n"+
					"Springfield                  | IL |    117,126%n"+
					"Independence                 | MO |    117,270%n"+
					"Vallejo                      | CA |    117,796%n"+
					"Beaumont                     | TX |    118,228%n"+
					"Abilene                      | TX |    118,887%n"+
					"Allentown                    | PA |    118,974%n"+
					"Athens-Clarke County         | GA |    118,999%n"+
					"Santa Clara                  | CA |    119,311%n"+
					"Midland                      | TX |    119,385%n"+
					"Evansville                   | IN |    120,235%n"+
					"Victorville                  | CA |    120,336%n"+
					"Denton                       | TX |    121,123%n"+
					"Surprise                     | AZ |    121,287%n"+
					"Lafayette                    | LA |    122,761%n"+
					"Kent                         | WA |    122,999%n"+
					"Thornton                     | CO |    124,140%n"+
					"Roseville                    | CA |    124,519%n"+
					"Concord                      | CA |    124,711%n"+
					"Hartford                     | CT |    124,893%n"+
					"Stamford                     | CT |    125,109%n"+
					"Coral Springs                | FL |    125,287%n"+
					"Carrollton                   | TX |    125,409%n"+
					"Charleston                   | SC |    125,583%n"+
					"Simi Valley                  | CA |    125,793%n"+
					"Gainesville                  | FL |    126,047%n"+
					"Bellevue                     | WA |    126,439%n"+
					"Elizabeth                    | NJ |    126,458%n"+
					"Waco                         | TX |    127,018%n"+
					"Visalia                      | CA |    127,081%n"+
					"Topeka                       | KS |    127,939%n"+
					"Cedar Rapids                 | IA |    128,119%n"+
					"Frisco                       | TX |    128,176%n"+
					"Thousand Oaks                | CA |    128,412%n"+
					"Miramar                      | FL |    128,729%n"+
					"Olathe                       | KS |    130,045%n"+
					"Sterling Heights             | MI |    130,410%n"+
					"New Haven                    | CT |    130,741%n"+
					"Columbia                     | SC |    131,686%n"+
					"West Valley                  | UT |    132,434%n"+
					"Warren                       | MI |    134,141%n"+
					"Killeen                      | TX |    134,654%n"+
					"McAllen                      | TX |    134,719%n"+
					"Hampton                      | VA |    136,836%n"+
					"Pasadena                     | CA |    138,547%n"+
					"Fullerton                    | CA |    138,574%n"+
					"Orange                       | CA |    139,419%n"+
					"Dayton                       | OH |    141,359%n"+
					"Savannah                     | GA |    142,022%n"+
					"Clarksville                  | TN |    142,519%n"+
					"Mesquite                     | TX |    143,195%n"+
					"McKinney                     | TX |    143,223%n"+
					"Naperville                   | IL |    143,684%n"+
					"Syracuse                     | NY |    144,170%n"+
					"Paterson                     | NJ |    145,219%n"+
					"Hollywood                    | FL |    145,236%n"+
					"Lakewood                     | CO |    145,516%n"+
					"Cary                         | NC |    145,693%n"+
					"Sunnyvale                    | CA |    146,197%n"+
					"Alexandria                   | VA |    146,294%n"+
					"Bridgeport                   | CT |    146,425%n"+
					"Torrance                     | CA |    147,027%n"+
					"Kansas                       | KS |    147,268%n"+
					"Escondido                    | CA |    147,575%n"+
					"Joliet                       | IL |    148,268%n"+
					"Fort Collins                 | CO |    148,612%n"+
					"Hayward                      | CA |    149,392%n"+
					"Pomona                       | CA |    150,812%n"+
					"Rockford                     | IL |    150,843%n"+
					"Pasadena                     | TX |    152,272%n"+
					"Springfield                  | MA |    153,552%n"+
					"Salinas                      | CA |    154,484%n"+
					"Palmdale                     | CA |    155,650%n"+
					"Salem                        | OR |    157,429%n"+
					"Eugene                       | OR |    157,986%n"+
					"Corona                       | CA |    158,391%n"+
					"Elk Grove                    | CA |    159,038%n"+
					"Lancaster                    | CA |    159,055%n"+
					"Peoria                       | AZ |    159,789%n"+
					"Sioux Falls                  | SD |    159,908%n"+
					"Pembroke Pines               | FL |    160,306%n"+
					"Cape Coral                   | FL |    161,248%n"+
					"Springfield                  | MO |    162,191%n"+
					"Vancouver                    | WA |    165,489%n"+
					"Tempe                        | AZ |    166,842%n"+
					"Ontario                      | CA |    167,211%n"+
					"Port St. Lucie               | FL |    168,716%n"+
					"Santa Rosa                   | CA |    170,685%n"+
					"Rancho Cucamonga             | CA |    170,746%n"+
					"Fort Lauderdale              | FL |    170,747%n"+
					"Chattanooga                  | TN |    171,279%n"+
					"Oceanside                    | CA |    171,293%n"+
					"Garden Grove                 | CA |    174,389%n"+
					"Jackson                      | MS |    175,437%n"+
					"Providence                   | RI |    178,432%n"+
					"Overland Park                | KS |    178,919%n"+
					"Santa Clarita                | CA |    179,013%n"+
					"Brownsville                  | TX |    180,097%n"+
					"Newport News                 | VA |    180,726%n"+
					"Grand Prairie                | TX |    181,824%n"+
					"Knoxville                    | TN |    182,200%n"+
					"Worcester                    | MA |    182,669%n"+
					"Huntsville                   | AL |    183,739%n"+
					"Tallahassee                  | FL |    186,971%n"+
					"Salt Lake                    | UT |    189,314%n"+
					"Grand Rapids                 | MI |    190,411%n"+
					"Glendale                     | CA |    194,478%n"+
					"Huntington Beach             | CA |    194,708%n"+
					"Mobile                       | AL |    194,822%n"+
					"Amarillo                     | TX |    195,250%n"+
					"Little Rock                  | AR |    196,537%n"+
					"Augusta-Richmond County      | GA |    197,872%n"+
					"Columbus                     | GA |    198,413%n"+
					"Yonkers                      | NY |    198,449%n"+
					"Akron                        | OH |    198,549%n"+
					"Moreno Valley                | CA |    199,552%n"+
					"Aurora                       | IL |    199,932%n"+
					"Oxnard                       | CA |    201,555%n"+
					"Fontana                      | CA |    201,812%n"+
					"Shreveport                   | LA |    201,867%n"+
					"Tacoma                       | WA |    202,010%n"+
					"Fayetteville                 | NC |    202,103%n"+
					"Modesto                      | CA |    203,547%n"+
					"Montgomery                   | AL |    205,293%n"+
					"Des Moines                   | IA |    206,688%n"+
					"Spokane                      | WA |    209,525%n"+
					"Richmond                     | VA |    210,309%n"+
					"Rochester                    | NY |    210,532%n"+
					"Birmingham                   | AL |    212,038%n"+
					"Boise                        | ID |    212,303%n"+
					"San Bernardino               | CA |    213,295%n"+
					"Gilbert                      | AZ |    221,140%n"+
					"Fremont                      | CA |    221,986%n"+
					"North Las Vegas              | NV |    223,491%n"+
					"Scottsdale                   | AZ |    223,514%n"+
					"Irving                       | TX |    225,427%n"+
					"Chesapeake                   | VA |    228,417%n"+
					"Irvine                       | CA |    229,985%n"+
					"Baton Rouge                  | LA |    230,058%n"+
					"Reno                         | NV |    231,027%n"+
					"Hialeah                      | FL |    231,941%n"+
					"Glendale                     | AZ |    232,143%n"+
					"Garland                      | TX |    233,564%n"+
					"Winston-Salem                | NC |    234,349%n"+
					"Lubbock                      | TX |    236,065%n"+
					"Durham                       | NC |    239,358%n"+
					"Madison                      | WI |    240,323%n"+
					"Laredo                       | TX |    244,731%n"+
					"Chandler                     | AZ |    245,628%n"+
					"Norfolk                      | VA |    245,782%n"+
					"St. Petersburg               | FL |    246,541%n"+
					"Orlando                      | FL |    249,562%n"+
					"Chula Vista                  | CA |    252,422%n"+
					"Jersey                       | NJ |    254,441%n"+
					"Fort Wayne                   | IN |    254,555%n"+
					"Buffalo                      | NY |    259,384%n"+
					"Lincoln                      | NE |    265,404%n"+
					"Henderson                    | NV |    265,679%n"+
					"Plano                        | TX |    272,068%n"+
					"Greensboro                   | NC |    277,080%n"+
					"Newark                       | NJ |    277,727%n"+
					"Toledo                       | OH |    284,012%n"+
					"St. Paul                     | MN |    290,770%n"+
					"Cincinnati                   | OH |    296,550%n"+
					"Stockton                     | CA |    297,984%n"+
					"Anchorage                    | AK |    298,610%n"+
					"Lexington-Fayette            | KY |    305,489%n"+
					"Pittsburgh                   | PA |    306,211%n"+
					"Corpus Christi               | TX |    312,195%n"+
					"Riverside                    | CA |    313,673%n"+
					"St. Louis                    | MO |    318,172%n"+
					"Santa Ana                    | CA |    330,920%n"+
					"Aurora                       | CO |    339,030%n"+
					"Anaheim                      | CA |    343,248%n"+
					"Honolulu                     | HI |    345,610%n"+
					"Tampa                        | FL |    347,645%n"+
					"Bakersfield                  | CA |    358,597%n"+
					"New Orleans                  | LA |    369,250%n"+
					"Arlington                    | TX |    375,600%n"+
					"Wichita                      | KS |    385,577%n"+
					"Cleveland                    | OH |    390,928%n"+
					"Minneapolis                  | MN |    392,880%n"+
					"Tulsa                        | OK |    393,987%n"+
					"Oakland                      | CA |    400,740%n"+
					"Miami                        | FL |    413,892%n"+
					"Omaha                        | NE |    421,570%n"+
					"Raleigh                      | NC |    423,179%n"+
					"Colorado Springs             | CO |    431,834%n"+
					"Atlanta                      | GA |    443,775%n"+
					"Virginia Beach               | VA |    447,021%n"+
					"Mesa                         | AZ |    452,084%n"+
					"Kansas                       | MO |    464,310%n"+
					"Long Beach                   | CA |    467,892%n"+
					"Sacramento                   | CA |    475,516%n"+
					"Fresno                       | CA |    505,882%n"+
					"Tucson                       | AZ |    524,295%n"+
					"Albuquerque                  | NM |    555,417%n"+
					"Las Vegas                    | NV |    596,424%n"+
					"Milwaukee                    | WI |    598,916%n"+
					"Oklahoma                     | OK |    599,199%n"+
					"Portland                     | OR |    603,106%n"+
					"Louisville/Jefferson County  | KY |    605,110%n"+
					"Baltimore                    | MD |    621,342%n"+
					"Nashville-Davidson           | TN |    624,496%n"+
					"Washington                   | DC |    632,323%n"+
					"Denver                       | CO |    634,265%n"+
					"Seattle                      | WA |    634,535%n"+
					"Boston                       | MA |    636,479%n"+
					"Memphis                      | TN |    655,155%n"+
					"El Paso                      | TX |    672,538%n"+
					"Detroit                      | MI |    701,475%n"+
					"Charlotte                    | NC |    775,202%n"+
					"Fort Worth                   | TX |    777,992%n"+
					"Columbus                     | OH |    809,798%n"+
					"San Francisco                | CA |    825,863%n"+
					"Indianapolis                 | IN |    834,852%n"+
					"Jacksonville                 | FL |    836,507%n"+
					"Austin                       | TX |    842,592%n"+
					"San Jose                     | CA |    982,765%n"+
					"Dallas                       | TX |  1,241,162%n"+
					"San Diego                    | CA |  1,338,348%n"+
					"San Antonio                  | TX |  1,382,951%n"+
					"Phoenix                      | AZ |  1,488,750%n"+
					"Philadelphia                 | PA |  1,547,607%n"+
					"Houston                      | TX |  2,160,821%n"+
					"Chicago                      | IL |  2,714,856%n"+
					"Los Angeles                  | CA |  3,857,799%n"+
					"New York                     | NY |  8,336,697" );
}