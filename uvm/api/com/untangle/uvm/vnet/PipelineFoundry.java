/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.net.InetSocketAddress;

import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.vnet.event.SessionEventListener;

/**
 * Compiles pipes based on subscriptions and interest sets.
 */
public interface PipelineFoundry
{
    ArgonConnector createArgonConnector( PipeSpec spec, SessionEventListener listener, Fitting inputFitting, Fitting outputFitting );

    void registerArgonConnector( ArgonConnector argonConnector );

    void deregisterArgonConnector( ArgonConnector argonConnector );

    void registerCasing( ArgonConnector insideArgonConnector, ArgonConnector outsideArgonConnector );

    void deregisterCasing( ArgonConnector insideArgonConnector);

    /* Remove all of the cached chains */
    void clearCache();

    void addConnectionFittingHint( InetSocketAddress socketAddress, Fitting fitting );

    Pipeline getPipeline( long sessionId );
}
