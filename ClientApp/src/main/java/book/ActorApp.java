package book;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import jpaprimer.generated.Serie;

@SuppressWarnings("unused")
public class ActorApp implements MessageListener {
	private ConnectionFactory connectionFactory;
	private Destination destination;
	private Destination destinationTopic;
	private JMSContext context;
	private JMSProducer messageProducer;
	private static Scanner scan = new Scanner(System.in);
	private String tokenLogin = null;
	private String name;

	public ActorApp() throws NamingException {
		this.connectionFactory = InitialContext.doLookup("jms/RemoteConnectionFactory");
		this.destination = InitialContext.doLookup("jms/queue/playQueue");
		this.destinationTopic = InitialContext.doLookup("jms/topic/invitations");
		this.context = connectionFactory.createContext("pedroferreira", "coimbra2022!");
		this.messageProducer = context.createProducer();
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws NamingException, InterruptedException {
		ActorApp actor = new ActorApp();
		int loginOption = 0;
		while (loginOption != 3) {
			String login = "Menu -> \n 1. Register\n 2. Login\n 3. Exit";
			System.out.println(login);
			loginOption = scan.nextInt();
			scan.nextLine();

			if (loginOption == 3) {
				scan.close();
				System.out.println("Thanks for Using the App. Be happy to return!");
			}
			// Registo
			if (loginOption == 1) {
				String reply = actor.resgister();
				System.out.println(reply);
			}
			// Login
			if (loginOption == 2) {
				String t = actor.login();
				if (!t.equals("LoginFailed")) {
					actor.tokenLogin = t;
					int choose = 0;
					actor.receiveFromTopic();
					do {
						String menuLogin = "1. List All Series in DB\n" + "2. Show Pending Invitations\n"
								+ "3. Accept or Reject Invitations\n" + "4. Logout\n" + "5. Logout and Exit the App";
						System.out.println("=*".repeat(30));
						System.out.println(menuLogin);
						choose = scan.nextInt();
						scan.nextLine();
						if (choose == 1) {
							List<Serie> series = actor.getSeries();
							for (Serie serie : series) {
								// System.out.println("*********************");
								System.out.println("------- " + serie.getTitle() + " -------");
								System.out.println("-> " + serie.getSummaryOfSerie());
								System.out.println("   Year -> " + serie.getYear());
								System.out.println("   Rating");
								System.out.println("      Score - > " + serie.getScore());
								System.out.println("      Voters - > " + serie.getNumberOfVotes());
								System.out.println("      PG-Rating -> " + serie.getPgRating());
								if (serie.getDirector().size() > 0) {
									System.out.println("   Directors");
									for (String dir : serie.getDirector()) {
										System.out.println("      -> " + dir);
									}
								}
								if (serie.getActor().size() > 0) {
									System.out.println("   Actors");
									for (String dir : serie.getActor()) {
										System.out.println("      -> " + dir);
									}
								}
								if (serie.getWriter().size() > 0) {
									System.out.println("   Writers");
									for (String dir : serie.getWriter()) {
										System.out.println("      -> " + dir);
									}
								}
								System.out.println("\n");
							}
						}
						if (choose == 2) {
							List<Invite> invites = (List<Invite>) actor.sendStringReceiveList("InvitationsList");
							System.out.println("Pending Invitations ....");
							for (Invite i : invites) {
								System.out.println(" -> " + i.getTitle());
							}
						}

						if (choose == 3) {
							System.out.println("Which Invitation would you like to accept/reject? ");
							String title = scan.nextLine();
							System.out.println("Do you want to accept[1] or reject[0]? ");
							int yesno = scan.nextInt();
							boolean accept;
							if (yesno == 1) {
								accept = true;
							} else {
								accept = false;
							}
							String reply = null;
							if (accept) {
								try {
									TextMessage msg = actor.context.createTextMessage("AcceptInvitation");
									msg.setBooleanProperty("acceptOrReject", accept);
									msg.setStringProperty("actor", actor.getName());
									msg.setStringProperty("tokenLogin", actor.tokenLogin);
									msg.setStringProperty("title", title);
									Destination tmp = actor.context.createTemporaryQueue();
									msg.setJMSReplyTo(tmp);
									actor.messageProducer.send(actor.destination, msg);
									JMSConsumer consumer = actor.context.createConsumer(tmp);
									reply = consumer.receiveBody(String.class);
								} catch (JMSException e) {
									System.out.println("[Error] Accept/Reject Invitation ClientApp");
									e.printStackTrace();
								}
								System.out.println(reply);
							} else {
								System.out.println("Your Invitation was Declined!");
							}

						}

						if (choose == 4) {
							actor.logout();
						}
						if (choose == 5) {
							actor.logout();
							scan.close();
							loginOption = 3; // Exits the App
						}
					} while (!(choose == 4 || choose == 5));
				} else {
					System.out.println("Something was Wrong with Username or Password! Try to Log in again!");
				}
			}
		}

	}

	private String send(String text) {
		String reply = "";
		try {
			TextMessage msg = context.createTextMessage(text);
			Destination tmp = context.createTemporaryQueue();
			msg.setJMSReplyTo(tmp);
			messageProducer.send(destination, msg);
			JMSConsumer cons = context.createConsumer(tmp);
			reply = cons.receiveBody(String.class);
		} catch (JMSException e) {
			e.printStackTrace();
		}
		return reply;
	}

	private List<?> sendStringReceiveList(String text) {
		List<?> reply = new ArrayList<>();
		try {
			TextMessage msg = context.createTextMessage(text);
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

	@SuppressWarnings("unchecked")
	private List<Serie> getSeries() {
		List<Serie> reply = new ArrayList<Serie>();
		try {
			TextMessage msg = context.createTextMessage();
			Destination tmp = context.createTemporaryQueue();
			msg.setJMSReplyTo(tmp);
			msg.setText("ListAllSeries");
			msg.setStringProperty("tokenLogin", tokenLogin);
			messageProducer.send(destination, msg);
			JMSConsumer cons = context.createConsumer(tmp);
			reply = cons.receiveBody(List.class);
		} catch (JMSException e) {
			e.printStackTrace();
		}
		return reply;
	}

	private String resgister() {
		String reply = "";
		try {
			// Getting the Data for Registering
			System.out.println("Lets begin the registration...");
			UserActor user = new UserActor();
			System.out.print("Username -> ");
			String username = scan.nextLine();
			user.setUsername(username);
			System.out.print("Password -> ");
			String password = scan.nextLine();
			user.setPassword(password);

			// Messaging
			ObjectMessage msg = context.createObjectMessage();
			msg.setObject(user);
			msg.setStringProperty("action", "register");
			Destination tmp = context.createTemporaryQueue();
			msg.setJMSReplyTo(tmp);
			messageProducer.send(destination, msg);
			JMSConsumer cons = context.createConsumer(tmp);
			reply = cons.receiveBody(String.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return reply;
	}

	private String login() {
		String reply = null;
		try {

			// Getting the Data for Registering
			System.out.println("Lets Try to Login into System...");
			UserActor user = new UserActor();
			System.out.print("Username -> ");
			String username = scan.nextLine();
			user.setUsername(username);
			System.out.print("Password -> ");
			String password = scan.nextLine();
			user.setPassword(password);

			// Messaging
			ObjectMessage msg = context.createObjectMessage();
			msg.setObject(user);
			msg.setStringProperty("action", "login");
			Destination tmp = context.createTemporaryQueue();
			msg.setJMSReplyTo(tmp);
			messageProducer.send(destination, msg);
			JMSConsumer cons = context.createConsumer(tmp);
			reply = cons.receiveBody(String.class);
			if (!reply.equals("LoginFailed")) {
				this.name = username;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return reply;
	}

	private void logout() {
		try {
			Destination tmp = context.createTemporaryQueue();
			TextMessage logoutMessage = context.createTextMessage("Logout");
			logoutMessage.setStringProperty("token", tokenLogin);
			logoutMessage.setJMSReplyTo(tmp);
			messageProducer.send(destination, logoutMessage);
			JMSConsumer consumer = context.createConsumer(tmp);
			String reply = consumer.receiveBody(String.class);
			System.out.println(reply);
		} catch (JMSException e) {
			System.out.println("[Logout Error]");
			e.printStackTrace();
		}
	}

	public void receiveFromTopic() {
		try {
			JMSConsumer consumer = context.createConsumer(destinationTopic);
			consumer.setMessageListener(this);

		} catch (JMSRuntimeException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onMessage(Message msg) {
		TextMessage textMsg = (TextMessage) msg;
		try {
			System.out.println(" ->>> Got Message: " + textMsg.getText());
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

}