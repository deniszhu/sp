package com.soulinfo.jms.cms.listener;

import java.beans.ExceptionListener;

import javax.jms.Message;
import javax.jms.MessageListener;

import org.springframework.stereotype.Component;

@Component(value = "cmsQueueListener")
public class CmsQueueListener implements MessageListener, ExceptionListener {

	@Override
	public void exceptionThrown(Exception e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessage(Message message) {
		// TODO Auto-generated method stub
		
	}

}
