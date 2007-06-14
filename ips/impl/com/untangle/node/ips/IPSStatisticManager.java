/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.untangle.node.ips;

import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.StatisticEvent;
import com.untangle.uvm.node.StatisticManager;
import com.untangle.uvm.node.NodeContext;

class IPSStatisticManager extends StatisticManager {

    /* Interface matcher to determine if the sessions is incoming or outgoing */
    //final IntfMatcher matcherIncoming = IntfMatcher.MATCHER_IN;
    //final IntfMatcher matcherOutgoing = IntfMatcher.MATCHER_OUT;

    private IPSStatisticEvent statisticEvent = new IPSStatisticEvent();

    public IPSStatisticManager(NodeContext tctx) {
        super(EventLoggerFactory.factory().getEventLogger(tctx));
    }

    protected StatisticEvent getInitialStatisticEvent() {
        return this.statisticEvent;
    }

    protected StatisticEvent getNewStatisticEvent() {
        return ( this.statisticEvent = new IPSStatisticEvent());
    }

    void incrDNC() {
        this.statisticEvent.incrDNC();
    }

    void incrLogged() {
        this.statisticEvent.incrLogged();
    }

    void incrBlocked() {
        this.statisticEvent.incrBlocked();
    }
}
