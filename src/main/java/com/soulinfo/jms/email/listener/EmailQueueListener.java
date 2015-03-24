package com.soulinfo.jms.email.listener;

import java.beans.ExceptionListener;
import java.io.IOException;
import java.util.Date;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.soulinfo.jms.email.producer.EmailNotificationMessageJms;
import com.soulinfo.jms.XmlMapper;

@Component(value = "emailQueueListener")
public class EmailQueueListener implements MessageListener, ExceptionListener {
	 
	protected final Log logger = LogFactory.getLog(getClass());
	
	@Autowired
	protected XmlMapper xmlMapper;
	
	@Override
	public void exceptionThrown(Exception e) {
		 logger.debug("Exception on queue listener: " + e.getCause() + ":" + e.getLocalizedMessage());
	}

	@Override
	public void onMessage(Message message) {
		try {
            if (message instanceof TextMessage) {
                TextMessage tm = (TextMessage) message;
                String valueJMSMessage = tm.getText();

                if (logger.isDebugEnabled()) {
                    logger.debug("Processed message, value: " + valueJMSMessage);
                }

                if(StringUtils.isNotEmpty(valueJMSMessage)){
                    final EmailNotificationMessageJms emailnotificationMessageJms = xmlMapper.getXmlMapper().readValue(valueJMSMessage, EmailNotificationMessageJms.class);
                    
                    // TRIGGER A BATCH TO PROCESS THE EMAIL
                    if (logger.isDebugEnabled()) {
                        logger.debug("Trigger a new job for a new email, type: " + emailnotificationMessageJms.getEmailType());
                    }
                }
            }
            
        } catch (JMSException e) {
            logger.error(e.getMessage(), e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } 
		
	}

}
