package com.mockrunner.example.jms;

import javax.jms.JMSException;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;

import com.mockrunner.ejb.EJBTestModule;
import com.mockrunner.jms.JMSTestCaseAdapter;
import com.mockrunner.mock.jms.MockObjectMessage;
import com.mockrunner.mock.jms.MockQueue;
import com.mockrunner.mock.jms.MockTextMessage;
import com.mockrunner.servlet.ServletTestModule;

/**
 * Example test for {@link PrintMessageServlet}. Demonstrates
 * the usage of {@link com.mockrunner.jms.JMSTestModule}.
 * Even though this example does not use EJB, we use
 * {@link com.mockrunner.ejb.EJBTestModule} here, because we
 * need the JNDI implementation of MockEJB.
 * The JMS test framework can work with more than one connection.
 * Per default, the latest created connection is used, i.e.
 * unless the servlet <code>doGet</code> resp. <code>doPost</code>
 * is called, the framework works with the receiver connection
 * created in the <code>init</code> method.
 * Note that you can override the default behavior by calling
 * {@link com.mockrunner.jms.JMSTestModule#setCurrentQueueConnectionIndex}
 * resp.
 * {@link com.mockrunner.jms.JMSTestModule#setCurrentTopicConnectionIndex}.
 */
public class PrintMessageServletTest extends JMSTestCaseAdapter
{
    private EJBTestModule ejbModule;
    private ServletTestModule servletModule;
    private MockQueue queue;
    
    protected void setUp() throws Exception
    {
        super.setUp();
        ejbModule = new EJBTestModule(createEJBMockObjectFactory());
        ejbModule.bindToContext("java:/ConnectionFactory", getJMSMockObjectFactory().getMockQueueConnectionFactory());
        queue = getDestinationManager().createQueue("testQueue");
        ejbModule.bindToContext("queue/testQueue", queue);
        servletModule = new ServletTestModule(createWebMockObjectFactory());
        servletModule.createServlet(PrintMessageServlet.class);
    }

    public void testInitPrintMessageReceiver() throws Exception
    {
        verifyQueueConnectionStarted();
        verifyNumberQueueSessions(1);
        verifyNumberQueueReceivers(0, "testQueue", 1);
        QueueReceiver receiver = getQueueTransmissionManager(0).getQueueReceiver("testQueue");
        assertTrue(receiver.getMessageListener() instanceof PrintMessageListener);
    }
    
    public void testSuccessfulDelivery() throws Exception
    {
        servletModule.addRequestParameter("customerId", "1");
        servletModule.doGet();
        verifyNumberQueueSessions(1);
        verifyNumberQueueSenders(0, "testQueue", 1);
        verifyAllQueueSessionsClosed();
        verifyAllQueueSendersClosed(0);
        verifyQueueConnectionClosed();
        verifyNumberOfCreatedQueueTextMessages(0, 1);
        verifyNumberOfReceivedQueueMessages("testQueue", 1);
        verifyReceivedQueueMessageEquals("testQueue", 0, new MockTextMessage("1"));
        verifyReceivedQueueMessageAcknowledged("testQueue", 0);
    }
    
    public void testDeliveryMoreMessages() throws Exception
    {
        servletModule.addRequestParameter("customerId", "1");
        servletModule.doGet();
        servletModule.addRequestParameter("customerId", "2");
        servletModule.doGet();
        servletModule.addRequestParameter("customerId", "3");
        servletModule.doGet();
        verifyNumberOfReceivedQueueMessages("testQueue", 3);
        verifyAllReceivedQueueMessagesAcknowledged("testQueue");
        QueueSender sender = getQueueTransmissionManager(0).createQueueSender(queue);
        sender.send(new MockObjectMessage(new Integer(3)));
        verifyNumberOfReceivedQueueMessages("testQueue", 4);
        verifyReceivedQueueMessageNotAcknowledged("testQueue", 3);
    }
    
    public void testServletResponse() throws Exception
    {
        servletModule.setCaseSensitive(false);
        servletModule.addRequestParameter("customerId", "1");
        servletModule.doGet();
        servletModule.verifyOutputContains("successfully");
        servletModule.clearOutput();
        getJMSMockObjectFactory().getMockQueueConnectionFactory().setJMSException(new JMSException("TestException"));
        servletModule.doGet();
        servletModule.verifyOutputContains("error");
    }
}