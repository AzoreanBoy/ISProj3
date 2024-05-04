package book;

import java.util.Scanner;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class Sender {
	private ConnectionFactory connectionFactory;
	private Destination destination;

	public Sender() throws NamingException {
		this.connectionFactory = InitialContext.doLookup("jms/RemoteConnectionFactory");
		this.destination = InitialContext.doLookup("jms/topic/invitations");
	}

	private void send(String text) {
		try (JMSContext context = connectionFactory.createContext("pedroferreira", "coimbra2022!");) {
			JMSProducer messageProducer = context.createProducer();
			TextMessage msg = context.createTextMessage();
			Destination tmp = context.createTemporaryQueue();
			msg.setJMSReplyTo(tmp);
//			msg.setText("Hello My Friend...");
			msg.setText(text);
			messageProducer.send(destination, msg);
//			JMSConsumer cons = context.createConsumer(tmp);
//			String str = cons.receiveBody(String.class);
//			System.out.println("I received the reply sent to the temporary queue: " + str);
		} catch (Exception re) {
			re.printStackTrace();
		}
	}

	public static void main(String[] args) throws NamingException {
		Sender sender = new Sender();
		Scanner scan = new Scanner(System.in);
		String msg = scan.nextLine();
		scan.close();
		sender.send(msg);
		System.out.println("Finished sender...");
	}
}
