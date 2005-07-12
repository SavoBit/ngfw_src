/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: SpamLogEvent.java 502 2005-04-28 03:31:42Z amread $
 */

package com.metavize.tran.spam;

import com.metavize.mvvm.logging.LogEvent;
import com.metavize.tran.mail.SMTPSpamMessageAction;
import com.metavize.tran.mail.SMTPSpamMessageActionUserType;
import com.metavize.tran.mail.MessageInfo;

/**
 * Log for SMTP Spam events.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_SPAM_EVT_SMTP"
 * mutable="false"
 */
public class SpamSmtpEvent extends LogEvent
{
    private MessageInfo messageInfo;
    private float score;
    private boolean isSpam;
    private SMTPSpamMessageAction action;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public SpamSmtpEvent() { }

    public SpamSmtpEvent(MessageInfo messageInfo, float score, boolean isSpam,
                         SMTPSpamMessageAction action)
    {
        this.messageInfo = messageInfo;
        this.score = score;
        this.isSpam = isSpam;
        this.action = action;
    }

    // accessors --------------------------------------------------------------

    /**
     * Associate e-mail message info with event.
     *
     * @return e-mail message info.
     * @hibernate.many-to-one
     * column="MSG_ID"
     */
    public MessageInfo getMessageInfo()
    {
        return messageInfo;
    }

    public void setMessageInfo(MessageInfo messageInfo)
    {
        this.messageInfo = messageInfo;
    }

    /**
     * Spam scan score.
     *
     * @return the spam score
     * @hibernate.property
     * column="SCORE"
     */
    public float getScore()
    {
        return score;
    }

    public void setScore(float score)
    {
        this.score = score;
    }

    /**
     * Was it declared spam?
     *
     * @return true if the message is declared to be Spam
     * @hibernate.property
     * column="IS_SPAM"
     */
    public boolean isSpam()
    {
        return isSpam;
    }

    public void setSpam(boolean isSpam)
    {
        this.isSpam = isSpam;
    }

    /**
     * The action taken
     *
     * @return action.
     * @hibernate.property
     * type="com.metavize.tran.mail.SMTPSpamMessageActionUserType"
     * column="ACTION"
     */
    public SMTPSpamMessageAction getAction()
    {
        return action;
    }

    public void setAction(SMTPSpamMessageAction action)
    {
        this.action = action;
    }

}
