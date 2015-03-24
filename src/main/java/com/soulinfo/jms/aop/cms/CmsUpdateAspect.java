/**
 * Most of the code in the Qalingo project is copyrighted Hoteia and licensed
 * under the Apache License Version 2.0 (release version 0.8.0)
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *                   Copyright (c) Hoteia, 2012-2014
 * http://www.hoteia.com - http://twitter.com/hoteia - contact@hoteia.com
 *
 */
package com.soulinfo.jms.aop.cms;

import java.net.InetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.JoinPoint.StaticPart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.soulinfo.jms.email.producer.EmailNotificationMessageProducer;
import com.soulinfo.jms.email.producer.EmailNotificationMessageJms;

@Component(value = "cmsNotificationAspect")
public class CmsUpdateAspect {

    protected final Log logger = LogFactory.getLog(getClass());
    
    @Autowired
    protected EmailNotificationMessageProducer emailNotificationMessageProducer;
    
    
    public void before(final JoinPoint joinPoint) {
        if(logger.isDebugEnabled()){
            logger.debug("EmailNotificationAspect, before");
        }
    }

    public void afterReturning(final StaticPart staticPart, final Object result) {
        if(logger.isDebugEnabled()){
            logger.debug("EmailNotificationAspect, afterReturning");
        }
        try {
           // final Email email = (Email) result;
        	
            final EmailNotificationMessageJms emailnotificationMessageJms = new EmailNotificationMessageJms();
            emailnotificationMessageJms.setEnvironmentName("test");
            emailnotificationMessageJms.setEnvironmentId("test");
            emailnotificationMessageJms.setApplicationName("test");
            emailnotificationMessageJms.setServerName(InetAddress.getLocalHost().getHostName());
            emailnotificationMessageJms.setServerIp(InetAddress.getLocalHost().getHostAddress());
            
//            if(email != null){
//                emailnotificationMessageJms.setEmailType("emailType");
//                emailnotificationMessageJms.setEmailId((long) 123);
//            }
            
            // Generate and send the JMS message
            emailNotificationMessageProducer.generateAndSendMessages(emailnotificationMessageJms);
            
        } catch (Exception e) {
            logger.error("EmailNotificationAspect Target Object error: " + e);
        }
    }

}