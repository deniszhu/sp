package com.soulinfo.jms.book.producer;

import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;

import com.soulinfo.jms.XmlMapper;

@Component(value = "bookMessageProducer")
public class BookMessageProducer {

    protected final Log logger = LogFactory.getLog(getClass());

    @Resource(name="bookJmsTemplate")
    protected JmsTemplate jmsTemplate;
    
    @Autowired
    protected XmlMapper xmlMapper;

    /**
     * Generates JMS messages
     * 
     */
    public void generateAndSendMessages(final BookUpdateMessageJms bookUpdateMessageJms) {
        try {
        	 final String valueJMSMessage = xmlMapper.getXmlMapper().writeValueAsString(bookUpdateMessageJms);
        	
            jmsTemplate.send( new MessageCreator() {
                public Message createMessage(Session session) throws JMSException {
                    TextMessage message = session.createTextMessage(valueJMSMessage);
                    if (logger.isDebugEnabled()) {
                        logger.info("Sending JMS message: " + valueJMSMessage);
                    }
                    return message;
                }
            });
        } catch (Exception e) {
            logger.error("Exception during create/send message process");
        }
    }

}