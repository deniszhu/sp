package com.soulinfo.jms.wechatImage.listener;
import java.beans.ExceptionListener;
import java.io.IOException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.soulinfo.jms.XmlMapper;
import com.soulinfo.jms.wechatImage.producer.WechatImageDownMessageJms;

@Component(value = "wechatImageQueueListener")
public class WechatImageQueueListener implements MessageListener, ExceptionListener {

	protected final Log logger = LogFactory.getLog(getClass());
	
	
	@Autowired
	protected XmlMapper xmlMapper;
	
	@Override
	public void exceptionThrown(Exception e) {
		 logger.debug("Exception on queue listener: " + e.getCause() + ":" + e.getLocalizedMessage());
		
	}

	@Override
	public void onMessage(Message message) {
		//1.下载微信图片
		//2.上传七牛
		//3.更新相册				
		try {
            if (message instanceof TextMessage) {
                TextMessage tm = (TextMessage) message;
                String valueJMSMessage = tm.getText();

                if (logger.isDebugEnabled()) {
                    logger.debug("Processed message, value: " + valueJMSMessage);
                }
                if(StringUtils.isNotEmpty(valueJMSMessage)){
                	final WechatImageDownMessageJms WechatImageMessageJms=xmlMapper.getXmlMapper().readValue(valueJMSMessage, WechatImageDownMessageJms.class);             
                    System.out.println(WechatImageMessageJms.getAccess_token()+"!!!!!!!!!!!!!!!!!!!!!");	
                    
//                    if ("PrintBook".equals(bookUpdateMessageJms.getType()))
//                    		bpbThreadImplThumbnailProcessor.doProcessor(bookUpdateMessageJms);
//                    else
//                    		wkThreadImplThumbnailProcessor.doProcessor(bookUpdateMessageJms);
                }
            }
		} catch (JMSException e) {
            logger.error(e.getMessage(), e);
        } catch (JsonParseException e) {
        	logger.error(e.getMessage(), e);
		} catch (JsonMappingException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} 
		
		
		
	}

}
