/**
 * Most of the code in the Qalingo project is copyrighted Hoteia and licensed
 * under the Apache License Version 2.0 (release version 0.8.0)
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *                   Copyright (c) Hoteia, 2012-2014
 * http://www.hoteia.com - http://twitter.com/hoteia - contact@hoteia.com
 *
 */
package com.soulinfo.jms.aop.book;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.JoinPoint.StaticPart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.soulinfo.jms.book.producer.BookMessageProducer;
import com.soulinfo.jms.book.producer.BookUpdateMessageJms;

@Component(value = "bookUpdateAspect")
public class BookUpdateAspect {

    protected final Log logger = LogFactory.getLog(getClass());
    
    @Autowired
    protected BookMessageProducer bookMessageProducer;
    
    public void before(final JoinPoint joinPoint) {
        if(logger.isDebugEnabled()){
            logger.debug("bookUpdateAspect, before");
        }
    }

    @SuppressWarnings("unchecked")
	public void afterReturning(final StaticPart staticPart, final Object result) {
        if(logger.isDebugEnabled()){
            logger.debug("bookUpdateAspect, afterReturning");
        }
        try {
          
			final HashMap<String, Object> returnMap = (HashMap<String, Object>) result;
        	
            final BookUpdateMessageJms bookUpdateMessageJms = new BookUpdateMessageJms();
            bookUpdateMessageJms.setEnvironmentName("test");
            bookUpdateMessageJms.setEnvironmentId("test");
            bookUpdateMessageJms.setApplicationName("test");
            bookUpdateMessageJms.setServerName(InetAddress.getLocalHost().getHostName());
            bookUpdateMessageJms.setServerIp(InetAddress.getLocalHost().getHostAddress());
            
            if(returnMap != null){
            	bookUpdateMessageJms.setBookId((String) returnMap.get("bookId"));
            	bookUpdateMessageJms.setType((String) returnMap.get("type"));
            	bookUpdateMessageJms.setEvent((String) returnMap.get("event"));
            	bookUpdateMessageJms.setPrintBookId((String) returnMap.get("printBookId"));
            	bookUpdateMessageJms.setPageId((String) returnMap.get("pageId"));
            	bookUpdateMessageJms.setSectionId((String) returnMap.get("sectionId"));
            	bookUpdateMessageJms.setPageList((List<String>) returnMap.get("pageList"));
            }
            
            // Generate and send the JMS message
            bookMessageProducer.generateAndSendMessages(bookUpdateMessageJms);
            
        } catch (Exception e) {
            logger.error("bookUpdateAspect Target Object error: " + e);
        }
    }

}