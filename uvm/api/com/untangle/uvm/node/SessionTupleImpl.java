/*
 * $Id$
 */
package com.untangle.uvm.node;

import java.net.InetAddress;

/**
 * This is a generic 5-tuple that describes sessions
 * (Protocol, Client, Client Port, Server, Server Port)
 */
public class SessionTupleImpl implements SessionTuple
{
    public static final short PROTO_TCP = 6;
    public static final short PROTO_UDP = 17;

    private short protocol = 0;
    private InetAddress clientAddr;
    private int clientPort = 0;
    private InetAddress serverAddr;
    private int serverPort = 0;
    
    public SessionTupleImpl( short protocol,
                             InetAddress clientAddr, InetAddress serverAddr,
                             int clientPort, int serverPort )
    {
        this.protocol = protocol;
        this.clientAddr = clientAddr;
        this.clientPort = clientPort;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
    }

    public SessionTupleImpl( SessionTuple tuple )
    {
        this.protocol = tuple.getProtocol();
        this.clientAddr = tuple.getClientAddr();
        this.clientPort = tuple.getClientPort();
        this.serverAddr = tuple.getServerAddr();
        this.serverPort = tuple.getServerPort();
    }

    public short getProtocol() { return this.protocol; }
    public void setProtocol( short protocol ) { this.protocol = protocol; }

    public InetAddress getClientAddr() { return this.clientAddr; }
    public void setClientAddr( InetAddress clientAddr ) { this.clientAddr = clientAddr; }

    public int getClientPort() { return this.clientPort; }
    public void setClientPort( int clientPort ) { this.clientPort = clientPort; }

    public InetAddress getServerAddr() { return this.serverAddr; }
    public void setServerAddr( InetAddress serverAddr ) { this.serverAddr = serverAddr; }

    public int getServerPort() { return this.serverPort; }
    public void setServerPort( int serverPort ) { this.serverPort = serverPort; }

    @Override
    public int hashCode()
    {
        if ( clientAddr == null || serverAddr == null )
            return protocol + clientPort + serverPort;
        else
            return protocol + clientAddr.hashCode() + clientPort + serverAddr.hashCode() + serverPort;
    }

    @Override
    public boolean equals(Object o)
    {
        if ( ! (o instanceof SessionTupleImpl) )
            return false;
        SessionTupleImpl t = (SessionTupleImpl)o;
        if ( t.protocol != this.protocol ||
             t.clientPort != this.clientPort ||
             t.serverPort != this.serverPort) {
            return false;
        }
        if ( ! ( t.clientAddr == null ? this.clientAddr == null : t.clientAddr.equals(this.clientAddr) ) ) {
            return false;
        }
        if ( ! ( t.serverAddr == null ? this.serverAddr == null : t.serverAddr.equals(this.serverAddr) ) ) {
            return false;
        }
        return true;
    }


    @Override
    public String toString()
    {
        String str = "[Tuple ";
        switch ( protocol ) {
        case PROTO_UDP:
            str += "UDP ";
            break;
        case PROTO_TCP:
            str += "TCP ";
            break;
        default:
            str += "PROTO:" + protocol + " ";
            break;
        }
        str += (clientAddr == null ? "null" : clientAddr.getHostAddress()) + ":" + clientPort;
        str += " -> ";
        str += (serverAddr == null ? "null" : serverAddr.getHostAddress()) + ":" + serverPort;
        str += "]";
        
        return str;
    }
}
