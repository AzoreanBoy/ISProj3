package book;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import jpaprimer.generated.Serie;

public class Director {
	private ConnectionFactory connectionFactory;
	private Destination destination;

	private Scanner sc = new Scanner(System.in);

	public Director() throws NamingException {
		this.connectionFactory = InitialContext.doLookup("jms/RemoteConnectionFactory");
		this.destination = InitialContext.doLookup("jms/queue/playQueue");
	}

	private List<String> sendStringReceiveList(String text) {
		List<String> lista = new ArrayList<>();
		try (JMSContext context = connectionFactory.createContext("pedroferreira", "coimbra2022!");) {
			JMSProducer messageProducer = context.createProducer();
			TextMessage msg = context.createTextMessage();
			msg.setStringProperty("actor", "add");
			Destination tmp = context.createTemporaryQueue();
			msg.setJMSReplyTo(tmp);
			msg.setText(text);
			messageProducer.send(destination, msg);
			JMSConsumer cons = context.createConsumer(tmp);
			lista = cons.receiveBody(List.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lista;
	}

	private List<?> sendStringReceiveSeries(String text) {
		List<?> reply = new ArrayList<>();
		try (JMSContext context = connectionFactory.createContext("pedroferreira", "coimbra2022!");) {
			JMSProducer messageProducer = context.createProducer();
			TextMessage msg = context.createTextMessage();
			msg.setText(text);
			Destination tmp = context.createTemporaryQueue();
			msg.setJMSReplyTo(tmp);
			messageProducer.send(destination, msg);
			JMSConsumer cons = context.createConsumer(tmp);
			reply = cons.receiveBody(List.class);
		} catch (JMSException e) {
			e.printStackTrace();
		}
		return reply;
	}

	private List<String> sendStringReceiveDirectors(String text) {
		List<String> lista = new ArrayList<>();
		try (JMSContext context = connectionFactory.createContext("pedroferreira", "coimbra2022!");) {
			JMSProducer messageProducer = context.createProducer();
			TextMessage msg = context.createTextMessage();
			Destination tmp = context.createTemporaryQueue();
			msg.setJMSReplyTo(tmp);
			msg.setText(text);
			messageProducer.send(destination, msg);
			JMSConsumer cons = context.createConsumer(tmp);
			lista = cons.receiveBody(List.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lista;
	}

//	private List<String> sendNewActorReceiveList(String actor, String title) {
//		List<String> lista = new ArrayList<>();
//		try (JMSContext context = connectionFactory.createContext("pedroferreira", "coimbra2022!");) {
//			JMSProducer messageProducer = context.createProducer();
//			TextMessage msg = context.createTextMessage();
//			Destination tmp = context.createTemporaryQueue();
//			msg.setJMSReplyTo(tmp);
//			msg.setText(actor + "," + title);
//			messageProducer.send(destination, msg);
//			JMSConsumer cons = context.createConsumer(tmp);
//			lista = cons.receiveBody(List.class);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return lista;
//	}

	private Serie sendStringReceiveSerie(String text) {
		Serie lista = new Serie();
		Serie s = new Serie();
		try (JMSContext context = connectionFactory.createContext("pedroferreira", "coimbra2022!");) {
			JMSProducer messageProducer = context.createProducer();
			TextMessage msg = context.createTextMessage();
			Destination tmp = context.createTemporaryQueue();
			msg.setJMSReplyTo(tmp);
			msg.setText(text);
			messageProducer.send(destination, msg);
			JMSConsumer cons = context.createConsumer(tmp);
			lista = cons.receiveBody(Serie.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lista;
	}

	private void send(Serie serie) {
		try (JMSContext context = connectionFactory.createContext("pedroferreira", "coimbra2022!");) {
			JMSProducer messageProducer = context.createProducer();
			ObjectMessage msg = context.createObjectMessage();
			msg.setObject(serie);
			Destination tmp = context.createTemporaryQueue();
			msg.setJMSReplyTo(tmp);
			messageProducer.send(destination, msg);
			JMSConsumer cons = context.createConsumer(tmp);
			String reply = cons.receiveBody(String.class);
			System.out.println(reply);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String sendInvitation(Invite i) {
		String reply = null;
		try (JMSContext context = connectionFactory.createContext("pedroferreira", "coimbra2022!")) {
			JMSProducer messageProducer = context.createProducer();
			ObjectMessage msg = context.createObjectMessage(i);
			Destination tmp = context.createTemporaryQueue();
			msg.setJMSReplyTo(tmp);
			messageProducer.send(destination, msg);
			JMSConsumer cons = context.createConsumer(tmp);
			reply = cons.receiveBody(String.class);
			System.out.println(reply);
		} catch (Exception e) {
			// TODO: handle exception
		}
		return reply;
	}

	private void chooseActor() {
		try (JMSContext context = connectionFactory.createContext("pedroferreira", "coimbra2022!");) {
			JMSProducer producer = context.createProducer();
			System.out.println("To Which Serie would you like to choose an actor from? ");
			String title = sc.nextLine();
//			choose.setText("ChooseActor,"+title);
			List<String> actoresReply = (List<String>) sendStringReceiveList("ChooseActor," + title);
			System.out.println("The Actors that accepted your invitation are -> ");
			for (String actor : actoresReply) {
				System.out.println(" -> " + actor);
			}
			System.out.print("Which actor would you like to add to the Serie? ");
			String actor = sc.nextLine();
			System.out.println(actor);
			Destination tmp = context.createTemporaryQueue();
			TextMessage choose = context.createTextMessage();
			choose.setText("AddChooseActor," + title + "," + actor);
			choose.setJMSReplyTo(tmp);
			producer.send(destination, choose);
			JMSConsumer consumer = context.createConsumer(tmp);
			String confirmation = consumer.receiveBody(String.class);
			System.out.println(confirmation);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws NamingException {
		Scanner scan = new Scanner(System.in);
		Director dir = new Director();
		System.out.println("Hello Director, Whats your name? ");
		String director = scan.nextLine();
		System.out.println("Thanks for using our App " + director);
		int choice = 0;
		String menu = "Menu.......\n" + " 1. Add a New Serie to DB\n" + " 2. List the Director Series\n"
				+ " 3. Invite all Actors to fill a cast Position\n" + " 4. Choose an actor to add to the Serie\n"
				+ " 5. Exit the App";
		while (choice != 5) {
			System.out.println(menu);
			choice = scan.nextInt();
			scan.nextLine();

			if (choice == 1) {
				Serie s = new Serie();
				Set<String> hash = new HashSet<>();
				hash.add(director);
				s.setDirector(hash);

				System.out.print("Title: ");
				String sc = scan.nextLine();
				s.setTitle(sc);
				System.out.print("Introduce a short Description of the Serie: ");
				s.setSummaryOfSerie(scan.nextLine());
				int year = 0;
				System.out.print("Year: ");
				do {
					while (!scan.hasNextInt()) {
						System.out.print("Invalid Year, please enter it again: ");
						scan.nextLine();
					}
					year = scan.nextInt();
					if (year < 1900 || year >= 2023) {
						System.out.print("Invalid Year, please enter it again: ");
					}
				} while (year < 1900 || year >= 2023);
				s.setYear(year);
				System.out.print("Number of Seasons: ");
				int number = 0;
				do {
					while (!scan.hasNextInt()) {
						System.out.print("Number Invalid, please enter a valid number: ");
						scan.nextLine();
					}
					number = scan.nextInt();
					if (number <= 0) {
						System.out.print("Number Invalid, please enter a valid number: ");
					}
				} while (number <= 0);
				scan.nextLine();
				s.setNumberOfSeasons(new BigInteger(Integer.toString(number)));
//				System.out.println(s.getNumberOfSeasons());
				System.out.print("Genres: ");
				Set<String> genres = new HashSet<String>(Arrays.asList(scan.nextLine().split(",")));
				Set<String> genresCap = new HashSet<>();
				for (String i : genres) {
					genresCap.add(capitalise(i));
				}
				s.setGenre(genresCap);
//				System.out.println(s.getGenre());
				dir.send(s);

			}
			if (choice == 2) {
//				List<String> lista = dir.sendStringReceiveDirectors("GetDirectorsList");
//				System.out.println("Diretors List: ");
//				for (String l : lista) {
//					System.out.println("- " + l);
//				}
//				System.out.println("Wich Director do you pretend to log in with?");
//				String director = scan.nextLine();
				System.out.println("Series directed by " + director);
				List<String> series = (List<String>) dir.sendStringReceiveSeries("DiretorSeries," + director);

				for (String s : series) {
					System.out.println(" -> " + s);
				}
			}
			if (choice == 3) {
				System.out.println("Write the Serie title's you are inviting for cast: ");
				String inviteTitle;
				inviteTitle = scan.nextLine();
//				System.out.println("Wich Director do you pretend to log in with?");
//				String director = scan.nextLine();
				Invite i = new Invite(inviteTitle);
				i.setDirector(director);
				dir.sendInvitation(i);
			}

			if (choice == 4) {
				dir.chooseActor();

			}
		}
		scan.close();
		System.out.println("Thanks for using the App and Return Anytime!");
	}

	// -------------auxiliares---------------
	private static String capitalise(String str) {
		return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
	}
}
