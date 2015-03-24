package com.soulinfo.jms.email.producer;

import com.soulinfo.jms.AbstractMessageJms;

public class EmailNotificationMessageJms extends AbstractMessageJms {

    private String emailType;
    private Long emailId;
    
    public String getEmailType() {
        return emailType;
    }
    
    public void setEmailType(String emailType) {
        this.emailType = emailType;
    }
    
    public Long getEmailId() {
        return emailId;
    }
    
    public void setEmailId(Long emailId) {
        this.emailId = emailId;
    }
    
}
