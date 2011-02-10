/* $HeadURL$ */
package com.untangle.node.router;

import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.StatisticEvent;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.StatisticManager;
import com.untangle.uvm.node.IntfMatcher;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.Protocol;

class RouterStatisticManager extends StatisticManager
{
    /* Interface matcher to determine if the sessions is incoming or outgoing */
    final IntfMatcher matcherIncoming = IntfMatcher.getWanMatcher();

    private RouterStatisticEvent statisticEvent = new RouterStatisticEvent();

    RouterStatisticManager(NodeContext tctx)
    {
        super(EventLoggerFactory.factory().getEventLogger(tctx));
    }

    protected StatisticEvent getInitialStatisticEvent()
    {
        return this.statisticEvent;
    }

    protected StatisticEvent getNewStatisticEvent()
    {
        return (this.statisticEvent = new RouterStatisticEvent());
    }

    void incrRouterSessions()
    {
        this.statisticEvent.incrRouterSessions();
    }

    void incrRedirect(Protocol protocol, IPNewSessionRequest request)
    {
        LocalUvmContext uc = LocalUvmContextFactory.context();

        /**
         * FIXME this needs to be rewritten
         * The "direction" of the redirect is totally irrelevant
         * just use one counter for both "directions"
         */
        
        /* XXX Incoming/outgoing is all wrong */
        boolean isOutgoing = matcherIncoming.isMatch(request.clientIntf());

        if (protocol == Protocol.TCP) {
            if (isOutgoing) incrTcpOutgoingRedirects();
            else              incrTcpIncomingRedirects();
        } else {
            /* XXX ICMP Hack */
            if ((request.clientPort() == 0) && (request.serverPort() == 0)) {
                /* Ping Sessions */
                if (isOutgoing) incrIcmpOutgoingRedirects();
                else              incrIcmpIncomingRedirects();
            } else {
                /* UDP Sessions */
                if (isOutgoing) incrUdpOutgoingRedirects();
                else              incrUdpIncomingRedirects();
            }
        }
    }

    void incrTcpIncomingRedirects()
    {
        this.statisticEvent.incrTcpIncomingRedirects();
    }

    void incrTcpOutgoingRedirects()
    {
        this.statisticEvent.incrTcpOutgoingRedirects();
    }

    void incrUdpIncomingRedirects()
    {
        this.statisticEvent.incrUdpIncomingRedirects();
    }

    void incrUdpOutgoingRedirects()
    {
        this.statisticEvent.incrUdpOutgoingRedirects();
    }

    void incrIcmpIncomingRedirects()
    {
        this.statisticEvent.incrIcmpIncomingRedirects();
    }

    void incrIcmpOutgoingRedirects()
    {
        this.statisticEvent.incrIcmpOutgoingRedirects();
    }

    void incrDmzSessions()
    {
        this.statisticEvent.incrDmzSessions();
    }
}
