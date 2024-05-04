package book;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class Receiver {
	private ConnectionFactory connectionFactory;
	private Destination destination;

	public Receiver() throws NamingException {
		this.connectionFactory = InitialContext.doLookup("jms/RemoteConnectionFactory");
		this.destination = InitialContext.doLookup("jms/topic/invitations");
	}

//	private String receive() {
//		String msg = null;
//		try (JMSContext context = connectionFactory.createContext("pedrohferreira", "1998Candelaria!");) {
//			context.setClientID("receiver");
//			JMSConsumer mc = context.createDurableConsumer((Topic)destination,"mySubscription");
//			msg = mc.receiveBody(String.class);
//		} catch (JMSRuntimeException re) {
//			re.printStackTrace();
//		}
//		return msg;
//	}

	private void receive() throws JMSException {
//		String msg = null;
		try (JMSContext context = connectionFactory.createContext("pedroferreira", "coimbra2022!");) {
			context.setClientID("receiver01");
			JMSConsumer consumer = context.createDurableConsumer((Topic) destination, "Invites");
				TextMessage msg = (TextMessage) consumer.receive();
				System.out.println("Message Received -> " + msg.getText());
				//			JMSProducer producer = context.createProducer();
				//			TextMessage reply = context.createTextMessage();
				//			reply.setText("GoodBye!");
				//			producer.send(msg.getJMSReplyTo(), reply);
				//			System.out.println("Sent Reply to ... " + msg.getJMSReplyTo());
			
		} catch (JMSRuntimeException re) {
			re.printStackTrace();
		}
	}
	public static void main(String[] args) throws NamingException, JMSException {
		Receiver receiver = new Receiver();
		receiver.receive();
	}
}
