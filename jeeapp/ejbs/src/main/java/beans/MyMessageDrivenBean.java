package beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import book.Invite;
import book.UserActor;
import jpaprimer.generated.Serie;

@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/queue/playQueue"),
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue") })
public class MyMessageDrivenBean implements MessageListener {

	@EJB
	private IManageSeries manage;

	@Inject
	private JMSContext context;

	@Resource(mappedName = "java:jboss/exported/jms/queue/playQueue")
	private Destination replyDestination;

	@Resource(mappedName = "java:jboss/exported/jms/topic/invitations")
	private Destination destinationTopic;

	Map<String, String> activeActors = new HashMap<>();

	@Override
	public void onMessage(Message message) {
		try {
			replyDestination = message.getJMSReplyTo();
			JMSProducer messageProducer = context.createProducer();
			if (message instanceof TextMessage) {
				// Get the Request
				String request = message.getBody(String.class);
				System.out.println("Request -> " + request);

				// Logic of the Request
				if (request.equals("GetDirectorsList")) {
					List<String> directors = manage.directorsList();
					replyObject(replyDestination, directors);
				} else if (request.equals("Logout")) {
					String tokenLogin = message.getStringProperty("token");
					for (Entry<String, String> t : activeActors.entrySet()) {
						if (t.getValue().equals(tokenLogin)) {
							activeActors.remove(t.getKey());
							System.out.println("Actor Removed From Active Users!");
							replyString(replyDestination, "Thanks for Using our App Message and Return Anytime!");
						}
					}
				} else if (request.equals("ListAllSeries")) {
					if (verifyLogin(message.getStringProperty("tokenLogin"))) {
						List<Serie> series = manage.getSeries();
						replyObject(replyDestination, series);
					}
					replyObject(replyDestination, null);
				} else if (request.equals("InvitationsList")) {
					List<Invite> invitations = manage.getInvitationsList();
					replyObject(replyDestination, invitations);

				} else if (request.equals("AddInvitation")) {
					Invite i = new Invite("Friends");
					boolean added = manage.addInvite(i);
					System.out.println(added + " Invitation " + i.getTitle());
					if (added) {
						TextMessage msg = context.createTextMessage(
								i.getDirector() + "invited you to fill a cast position for " + i.getTitle());
						messageProducer.send(destinationTopic, msg);
					}

				} else if (request.equals("AcceptInvitation")) {
					if (verifyLogin(message.getStringProperty("tokenLogin"))) {
						boolean accept = message.getBooleanProperty("acceptOrReject");
						String actor = message.getStringProperty("actor");
						String title = message.getStringProperty("title");
						if (accept) {
							manage.acceptInvitation(title, actor);
							replyString(replyDestination, "Thanks For Accepting The Invitation.");
						}
					} else {
						System.out.println("Not Loged In");
					}

				} else if (request.startsWith("ChooseActor")) {
					String title = request.split(",")[1];
					List<String> replys = manage.getInvitationReplys(title);
					System.out.println("[MessageDrivenBean] " + replys);
					replyObject(replyDestination, replys);
				} else if (request.startsWith("AddChooseActor")) {
					String title = request.split(",")[1];
					String choosenActor = request.split(",")[2];
					String confirmation = manage.addActorToSerie(title, choosenActor);
					replyString(replyDestination, confirmation);
				} else if (request.startsWith("DiretorSeries")) {
					String diretor = request.split(",")[1];
					List<Serie> series = manage.getDirectoSeries(diretor);
					List<String> titles = new ArrayList<>();
					for (Serie s : series) {
						System.out.println(s.getTitle());
						titles.add(s.getTitle());
					}
					System.out.println(titles);

					replyObject(replyDestination, titles);
				}
			}
			if (message instanceof ObjectMessage) {
				if (((ObjectMessage) message).getObject() instanceof Serie) {
					System.out.println("Add a serie to the DB....");
					Serie serie = (Serie) ((ObjectMessage) message).getObject();
					// Check if the Series already exists
					if (serie != null) {
						List<String> titles = manage.getSeriesTitles();
						if (titles.contains(serie.getTitle())) {
							System.out.println("Serie already exists!");
							replyString(replyDestination, "Serie already exists!");
						} else {
							manage.addSerie(serie);
							TextMessage msg = context.createTextMessage();
							String menssagem = "There is a new Serie in the System.\nThe name of the serie is "
									+ serie.getTitle();
							msg.setText(menssagem);
							addTopic(msg);
							replyString(replyDestination, "Serie Added to DB!");
						}
					}
				}

				else if (((ObjectMessage) message).getObject() instanceof UserActor) {
					UserActor user = (UserActor) ((ObjectMessage) message).getObject();

					// Registro
					if (message.getStringProperty("action").equals("register")) {
						System.out.println("inicio do Registo");
						boolean added = manage.addUserActor(user);
						if (added) {
							System.out.println("MDB - Added to DB.");
							replyString(replyDestination, "Register OK");
						} else {
							replyString(replyDestination, "Username Already in Use");
						}
					}
					// Login
					if (message.getStringProperty("action").equals("login")) {
						System.out.println("[MessageDrivenBean]Trying to Log in...");
						boolean loginDone = manage.login(user);
						if (loginDone) {
							System.out.println(loginDone + user.getUsername());
							TextMessage reply = context.createTextMessage();
							String loginToken = generateToken();
							reply.setText(loginToken);
							activeActors.put(user.getUsername(), loginToken);
							messageProducer.send(replyDestination, reply);
							System.out.println("Message Replied to Request");
						} else {
							TextMessage reply = context.createTextMessage("LoginFailed");
							messageProducer.send(replyDestination, reply);

						}
					}
				}

				if (((ObjectMessage) message).getObject() instanceof Invite) {
					Invite inv = (Invite) (((ObjectMessage) message).getObject());
					if (!manage.directorsList().contains(inv.getDirector())) {
						System.out.print("Director does not exist in the DB, please choose another.");
						replyString(replyDestination, "Director does not exist in the DB, please choose another.");
					} else {
						boolean added = manage.addInvite(inv);
						if (added) {
							System.out.println("Hello, Director " + inv.getDirector());
							TextMessage msg = context.createTextMessage(
									inv.getDirector() + " is inviting you to fill a position in " + inv.getTitle());
							addTopic(msg);
							System.out.print("Invite sent!");
							replyString(replyDestination, "Invite sent!");
						} else {
							replyString(replyDestination, "Erro no Invite");
						}
					}
				}
			}
		} catch (JMSException e) {
			System.out.println("Erro!!! Erro no Message Driven Bean");
			e.printStackTrace();
		}
	}

	// ---------Auxiliary Methods----------

	// Enviar um Objecto como Resposta
	private void replyObject(Destination replyDestination, Object replyObject) {
		try {
			ObjectMessage reply = context.createObjectMessage();
			reply.setObject((Serializable) replyObject);
			JMSProducer messageProducer = context.createProducer();
			messageProducer.send(replyDestination, reply);
			System.out.println("Message Replied to Request");
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	// Enviar uma String como Resposta
	private void replyString(Destination replyDestination, String replyString) {
		try {
			TextMessage reply = context.createTextMessage(replyString);
			JMSProducer messageProducer = context.createProducer();
			messageProducer.send(replyDestination, reply);
			System.out.println("Message Replied to Request");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean verifyLogin(String token) {
		System.out.println("Verifying Login");
		boolean verified = false;
		if (activeActors.containsValue(token)) {
			verified = true;
			System.out.println("Login Verified = true ");
		}
		return verified;
	}

	public String generateToken() {
		int leftLimit = 48; // numeral '0'
		int rightLimit = 122; // letter 'z'
		int targetStringLength = 10;
		Random random = new Random();

		String generatedString = random.ints(leftLimit, rightLimit + 1)
				.filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97)).limit(targetStringLength)
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
		return generatedString;
	}

	public void addTopic(TextMessage msg) {
		System.out.println("Adding the Invitation to Topic...");
		System.out.println("Sendind Invite to the Topic...");
		JMSProducer messageProducer = context.createProducer();
		messageProducer.send(destinationTopic, msg);
	}

}