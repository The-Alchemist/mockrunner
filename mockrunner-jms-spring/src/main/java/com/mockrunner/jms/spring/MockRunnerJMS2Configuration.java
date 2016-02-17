package com.mockrunner.jms.spring;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import com.melowe.jms2.compat.Jms2ConnectionFactory;
import com.mockrunner.jms.ConfigurationManager;
import com.mockrunner.jms.DestinationManager;
import com.mockrunner.mock.jms.MockQueueConnectionFactory;

/**
 * Support for JMS 2.x
 * 
 * @author the-alchemist
 */
public class MockRunnerJMS2Configuration {
    

    @Bean
    @Lazy
    Jms2ConnectionFactory mockJms2QueueConnectionFactory(DestinationManager dm, ConfigurationManager configurationManager) {
        return new Jms2ConnectionFactory(new MockQueueConnectionFactory(dm, configurationManager));
    }
    /**
     * MockRunner doesn't support JMS2 {@link JMSContext}'s :(
     *
     * So someone wrote a "JMS1" -> "JMS2" wrapper
     *
     *
     * @param connectionFactory
     * @return
     * @see https://github.com/melowe/jms2-compat/tree/master/src/main/java/com/melowe/jms2/compat
     */
    @Bean
    @Lazy
    @Scope(scopeName=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    JMSContext mockJmsContext(ConnectionFactory mockJms2QueueConnectionFactory) {
        JMSContext context = new Jms2ConnectionFactory(mockJms2QueueConnectionFactory).createContext();
        return context;
    }

}
