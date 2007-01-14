package ejava.examples.jmsmechanics;

import java.util.Enumeration;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.naming.InitialContext;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This test case provides a demonstration of the JMS QueueBrowser 
 * functionality. This object can walk through a JMS queue and perform 
 * an inspection of pending messages.
 *
 * @author jcstaff
 */
public class QueueBrowserTest extends TestCase {
    static Log log = LogFactory.getLog(QueueBrowserTest.class);
    InitialContext jndi;
    String connFactoryJNDI = System.getProperty("jndi.name.connFactory");
    String destinationJNDI = System.getProperty("jndi.name.testQueue");
    String msgCountStr = System.getProperty("multi.message.count");
    
    protected ConnectionFactory connFactory;
    protected Destination destination;        
    protected MessageCatcher catcher1;
    MessageCatcher catcher2;
    int msgCount;
    
    public void setUp() throws Exception {
        log.debug("***** VAR=" + System.getProperty("testx"));
        log.debug("getting jndi initial context");
        jndi = new InitialContext();    
        log.debug("jndi=" + jndi.getEnvironment());
        
        assertNotNull("jndi.name.connFactory not supplied", connFactoryJNDI);
        log.debug("connection factory name:" + connFactoryJNDI);
        connFactory = (ConnectionFactory)jndi.lookup(connFactoryJNDI);
        
        assertNotNull("jndi.name.testQueue not supplied", destinationJNDI);
        log.debug("destination name:" + destinationJNDI);
        destination = (Queue) jndi.lookup(destinationJNDI);
        
        assertNotNull("multi.message.count not supplied", msgCountStr);
        msgCount = Integer.parseInt(msgCountStr);
        
        catcher1 = new MessageCatcher("receiver1");
        catcher1.setConnFactory(connFactory);
        catcher1.setDestination(destination);

        catcher2 = new MessageCatcher("receiver2");
        catcher2.setConnFactory(connFactory);
        catcher2.setDestination(destination);
    }
    
    protected void tearDown() throws Exception {
        while (catcher1.isStarted() != true) {
            log.debug("waiting for catcher1 to start");
            Thread.sleep(2000);
        }
        catcher1.stop();
        
        while (catcher2.isStarted() != true) {
            log.debug("waiting for catcher2 to start");
            Thread.sleep(2000);
        }
        catcher2.stop();

        while (catcher1.isStopped() != true) {
            log.debug("waiting for catcher1 to stop");
            Thread.sleep(2000);
        }
        while (catcher2.isStopped() != true) {
            log.debug("waiting for catcher2 to stop");
            Thread.sleep(2000);
        }
    }

    public void testQueueBrowser() throws Exception {
        log.info("*** testQueueBrowser ***");
        Connection connection = null;
        Session session = null;
        MessageProducer producer = null;
        try {
            connection = connFactory.createConnection();
            session = connection.createSession(
                    false, Session.AUTO_ACKNOWLEDGE);
            producer = session.createProducer(destination);
            Message message = session.createMessage();
            
            catcher1.clearMessages();
            for(int i=0; i<msgCount; i++) {
                producer.send(message);
                log.info("sent msgId=" + message.getJMSMessageID());
            }
            
            QueueBrowser qbrowser = session.createBrowser((Queue)destination);
            int msgs=0;
            for (Enumeration e = qbrowser.getEnumeration(); 
                 e.hasMoreElements(); ) {
                Message m = (Message) e.nextElement();
                msgs += 1;
                log.debug("browsing message (" + msgs + 
                        ")=" + m.getJMSMessageID());
            }
            assertEquals("unexpected number if queue browser messages", 
                    msgCount, msgs);
            
            //queues will hold messages waiting for delivery
            new Thread(catcher1).start();
            new Thread(catcher2).start();
            for(int i=0; i<10 && 
                (catcher1.getMessages().size() +
                 catcher2.getMessages().size()< msgCount); i++) {
                log.debug("waiting for messages...");
                Thread.sleep(1000);
            }
            assertEquals(msgCount, 
                    catcher1.getMessages().size() +
                    catcher2.getMessages().size());
        }
        finally {
            if (producer != null) { producer.close(); }
            if (session != null)  { session.close(); }
            if (connection != null) { connection.close(); }
        }
    }
}