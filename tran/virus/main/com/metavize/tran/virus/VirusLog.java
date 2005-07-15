/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.virus;

import java.io.Serializable;
import java.util.Date;

import com.metavize.mvvm.tran.Direction;

public class VirusLog implements Comparable<VirusLog>, Serializable
{
    private static final long serialVersionUID = 5822180413983083372L;

    public enum Type { HTTP, FTP, MAIL };

    private final Date createDate;
    private final Type type;
    private final String location;
    private final boolean clean;
    private final String clientAddr;
    private final int clientPort;
    private final String serverAddr;
    private final int serverPort;
    private final Direction direction;

    public VirusLog(Date createDate, String type, String location,
                    boolean clean, String clientAddr,
                    int clientPort, String serverAddr, int serverPort,
                    Direction direction)
    {
        this.createDate = createDate;
        this.type = Type.valueOf(type);
        this.location = location;
        this.clean = clean;
        this.clientAddr = clientAddr;
        this.clientPort = clientPort;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        this.direction = direction;
    }

    // util -------------------------------------------------------------------
    public String getAction(){
	return "";
    }

    public String getReason(){
	return "";
    }

    public String getTraffic(){
	return type.toString() + "  " + location;
    }

    // accessors --------------------------------------------------------------

    public Date getCreateDate()
    {
        return createDate;
    }

    public Type getType()
    {
        return type;
    }

    public String getLocation()
    {
        return location;
    }

    public boolean isClean()
    {
        return clean;
    }

    public String getClientAddr()
    {
        return clientAddr;
    }

    public int getClientPort()
    {
        return clientPort;
    }

    public String getServerAddr()
    {
        return serverAddr;
    }

    public int getServerPort()
    {
        return serverPort;
    }

    public Direction getDirection()
    {
        return direction;
    }

    // Comparable<VirusLog> methods -------------------------------------------

    public int compareTo(VirusLog vl)
    {
        return createDate.compareTo(vl.createDate);
    }
}
