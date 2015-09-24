/*
 * $Id: SslInspectorUnparserEventHandler.java 39802 2015-03-06 00:18:06Z mahotz $
 */

package com.untangle.node.ssl_inspector;

import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.vnet.ReleaseToken;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.AbstractEventHandler;
import org.apache.log4j.Logger;

/*
 * The unparser handles converting plaint-text SSL traffic back to SSL
 * traffic, and sending the encrypted data to the client or server as
 * required. During the SSL handshake, our casing needs to pass raw SSL
 * traffic back and forth with both the client and server, and the parser is
 * the best place to handle that which means less work to do here. For the
 * server side, we only need to do the initial wrap to start the handshake.
 * On the client side, we have to unwrap the original message, and then
 * handle enough wrap and task calls until we have something to return to
 * the client, after which the parser will finish the handshake.
 */

public class SslInspectorUnparserEventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(getClass());

    private final SslInspectorApp node;
    private final boolean clientSide;

    // the corresponding parser for this unparser
    private SslInspectorParserEventHandler parser;
    
    // ------------------------------------------------------------------------

    protected SslInspectorUnparserEventHandler( boolean clientSide, SslInspectorApp node )
    {
        this.clientSide = clientSide;
        this.node = node;
    }

    // ------------------------------------------------------------------------

    public void setParser( SslInspectorParserEventHandler parser )
    {
        this.parser = parser;
    }
    
    
    @Override
    public void handleTCPClientChunk( NodeTCPSession session, ByteBuffer data )
    {
        if (clientSide)  {
            logger.warn("Received unexpected event");
            throw new RuntimeException("Received unexpected event");
        } else {
            streamUnparse( session, data, false );
        }
    }

    @Override
    public void handleTCPServerChunk( NodeTCPSession session, ByteBuffer data )
    {
        if (clientSide) {
            streamUnparse( session, data, true );
        } else {
            logger.warn("Received unexpected event");
            throw new RuntimeException("Received unexpected event");
        }            
    }

    @Override
    public void handleTCPClientDataEnd( NodeTCPSession session, ByteBuffer data )
    {
        if (clientSide) {
            logger.warn("Received unexpected event");
            throw new RuntimeException("Received unexpected event");
        } else {
            streamUnparse( session, data, false );
        }
    }

    @Override
    public void handleTCPServerDataEnd( NodeTCPSession session, ByteBuffer data )
    {
        if (clientSide) {
            streamUnparse( session, data, true );
        } else {
            logger.warn("Received unexpected event");
            throw new RuntimeException("Received unexpected event");
        }
    }

    // private methods --------------------------------------------------------

    private void streamUnparse( NodeTCPSession session, ByteBuffer data, boolean s2c )
    {
        try {
            unparse( session, data );
        }
        catch (Exception exn) {
            logger.warn("Error during streamUnparse()", exn);
        }

        return;
    }

    public void unparse( NodeTCPSession session, ByteBuffer data )
    {
        SslInspectorManager manager = getManager( session );
        ByteBuffer buff = data;
        boolean success = false;

        logger.debug("---------- " + (manager.getClientSide() ? "CLIENT" : "SERVER") + " unparse() received " + buff.limit() + " bytes ----------");

        // empty buffer indicates the session is terminating
        if (buff.limit() == 0)
            return;

        try {
            // pass the data to the unparse worker function
            success = unparseWorker( session, buff );
        }

        catch (Exception exn) {
            logger.debug("Exception calling unparseWorker", exn);
        }

        // null result means something went haywire so we abandon 
        if ( ! success ) {

            String logDetail = (String) session.globalAttachment(NodeTCPSession.KEY_SSL_INSPECTOR_SNI_HOSTNAME);
            if (logDetail == null)
                logDetail = session.getServerAddr().getHostAddress();
            SslInspectorLogEvent logevt = new SslInspectorLogEvent(session.sessionEvent(), 0, SslInspectorApp.STAT_ABANDONED, logDetail);
            node.logEvent(logevt);
            node.incrementMetric(SslInspectorApp.STAT_ABANDONED);
            logger.warn("Session abandon on unparseWorker false return for " + logDetail);

            // first we have to pass a release message to the other side
            ByteBuffer message = ByteBuffer.allocate(256);
            message.put(SslInspectorManager.IPC_RELEASE_MESSAGE);
            message.flip();

            if (manager.getClientSide()) {
                SslInspectorManager server = (SslInspectorManager) session.globalAttachment(NodeTCPSession.KEY_SSL_INSPECTOR_SERVER_MANAGER);
                server.getSession().simulateClientData(message);
            } else {
                SslInspectorManager client = (SslInspectorManager) session.globalAttachment(NodeTCPSession.KEY_SSL_INSPECTOR_CLIENT_MANAGER);
                client.getSession().simulateServerData(message);
            }
            
            // return a session release result
            session.release();
        }

        return;
    }

    // ------------------------------------------------------------------------

    private boolean unparseWorker( NodeTCPSession session, ByteBuffer data ) throws Exception
    {
        SslInspectorManager manager = getManager( session );
        ByteBuffer target = ByteBuffer.allocate(32768);
        boolean done = false;
        HandshakeStatus status;

        // if not in dataMode yet look for our special message that
        // lets us know when it's time to initialize our SSLEngine
        if ((manager.getSSLEngine() == null) && (manager.getDataMode() == false)) {
            if (manager.checkIPCMessage(data.array(), SslInspectorManager.IPC_WAKEUP_MESSAGE) == true) {
                logger.debug("Received IPC_WAKEUP message");
                manager.initializeEngine();
            }
        }

        if (manager.checkIPCMessage(data.array(), SslInspectorManager.IPC_RELEASE_MESSAGE) == true) {
            logger.debug("Received IPC_RELEASE message");
            session.release();
            return true;
        }

        if (manager.checkIPCMessage(data.array(), SslInspectorManager.IPC_DESTROY_MESSAGE) == true) {
            logger.debug("Received IPC_DESTROY message");
            session.killSession();
            return true;
        }

        logger.debug("CASING_BUFFER = " + manager.getCasingBuffer().toString());
        logger.debug("PARAM_BUFFER = " + data.toString());
        logger.debug("DATA_MODE = " + manager.getDataMode());

        while ( ! done ) {
            status = manager.getSSLEngine().getHandshakeStatus();
            logger.debug("STATUS = " + status);

            if (manager.getSSLEngine().isInboundDone()) {
                logger.debug("Unexpected isInboundDone() == TRUE");
                return false;
            }
            if (manager.getSSLEngine().isOutboundDone()) {
                logger.debug("Unexpected IsOutboundDone() == TRUE");
                return false;
            }

            switch (status) {
            // should never happen since this will only be returned from
            // a call to wrap or unwrap but we include it to be complete
            case FINISHED:
                logger.error("Unexpected FINISHED in unparseWorker loop");
                return false;

                // handle outstanding tasks during handshake
            case NEED_TASK:
                done = doNeedTask( session, data );
                break;

            // the parser handles most handshake stuff so we can ignore
            case NEED_UNWRAP:
                logger.error("Unexpected NEED_UNWRAP in unparseWorker loop");
                return false;

                // handle wrap during handshake
            case NEED_WRAP:
                done = doNeedWrap( session, data, target );
                break;

            // handle data when no handshake is in progress
            case NOT_HANDSHAKING:
                done = doNotHandshaking( session, data, target );
                break;

            // should never happen but we handle just to be safe
            default:
                logger.error("Unknown SSLEngine status in unparseWorker loop");
                return false;
            }
        }

        return done;
    }

    // ------------------------------------------------------------------------

    private boolean doNeedTask( NodeTCPSession session, ByteBuffer data ) throws Exception
    {
        SslInspectorManager manager = getManager( session );
        Runnable runnable;

        // loop and run SSLEngine outstanding tasks
        while ((runnable = manager.getSSLEngine().getDelegatedTask()) != null) {
            logger.debug("EXEC_TASK " + runnable.toString());
            runnable.run();
        }
        return false;
    }

    // ------------------------------------------------------------------------

    private boolean doNeedWrap( NodeTCPSession session, ByteBuffer data, ByteBuffer target ) throws Exception
    {
        SslInspectorManager manager = getManager( session );
        SSLEngineResult result;
        ByteBuffer empty = ByteBuffer.allocate(32);

        // during handshake the SSL engine doesn't do anything with
        // data we pass, so we just wrap an empty buffer here.
        result = manager.getSSLEngine().wrap(empty, target);
        logger.debug("EXEC_WRAP " + result.toString());
        if (result.getStatus() != SSLEngineResult.Status.OK)
            throw new Exception("SSLEngine wrap fault");

        // during the handshake we only expect to need a single wrap call
        // so if the status did not transition we have a big problem
        if (result.getHandshakeStatus() != HandshakeStatus.NEED_UNWRAP)
            throw new Exception("SSLEngine logic fault");

        // if the wrap call didn't produce any data return false
        if (result.bytesProduced() == 0)
            return false;

        // send the initial handshake data and let the parser handle the rest
        target.flip();

        if (manager.getClientSide()) {
            session.sendDataToClient( target );
            session.setServerBuffer( null );
        } else {
            session.sendDataToServer( target );
            session.setClientBuffer( null );
        }
        return true;
    }

    // ------------------------------------------------------------------------

    private boolean doNotHandshaking( NodeTCPSession session, ByteBuffer data, ByteBuffer target ) throws Exception
    {
        SslInspectorManager manager = getManager( session );
        SSLEngineResult result;

        // if we're not in dataMode yet we need to work on the handshake
        if (manager.getDataMode() == false) {
            // server side should not get here unless dataMode is active
            if (manager.getClientSide() == false) {
                throw new Exception("SSLEngine datamode fault");
            }

            // we're on the client side and dataMode is not active yet so we
            // need to unwrap the initial client data saved by the parser
            manager.getCasingBuffer().flip();
            result = manager.getSSLEngine().unwrap(manager.getCasingBuffer(), target);
            logger.debug("UNWRAP_TRANSITION " + result.toString());
            manager.getCasingBuffer().clear();
            return false;
        }

        // the unparser will always call wrap to convert plain to SSL
        result = manager.getSSLEngine().wrap(data, target);
        logger.debug("EXEC_HANDSHAKING " + result.toString());

        // if the engine reports closed return an empty result
        if (result.getStatus() == SSLEngineResult.Status.CLOSED) {
            if (manager.getClientSide()) {
                session.setClientBuffer( null );
            } else {
                session.setServerBuffer( null );
            }
            return true;
        }

        // any other result is very bad news
        if (result.getStatus() != SSLEngineResult.Status.OK)
            throw new Exception("SSLEngine wrap fault");

        // if we have gone back into handshake mode we return null to abandon
        // the session since the SSLEngine doesn't support rehandshake
        if (result.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING) {
            manager.setDataMode(false);
            return false;
        }

        // if the wrap call didn't produce anything yet just return null
        // wo we can call wrap again on the next pass through the loop
        if (result.bytesProduced() == 0)
            return false;

        // got some data from the wrap call so well prepare to send it along
        target.flip();

        // here in the unparser we're converting plain-text back to SSL so we
        // send client data to the client and server data to the server
        if (manager.getClientSide()) {
            session.sendDataToClient( target );
            session.setServerBuffer( null );
        } else {
            session.sendDataToServer( target );
            session.setClientBuffer( null );
        }
        return true;
        // if (manager.getClientSide())
        //     return new TCPChunkResult(array, null, null);
        // return new TCPChunkResult(null, array, null);
    }

    private SslInspectorManager getManager( NodeTCPSession session )
    {
        if (clientSide)
            return (SslInspectorManager) session.globalAttachment(NodeTCPSession.KEY_SSL_INSPECTOR_CLIENT_MANAGER);
        else
            return (SslInspectorManager) session.globalAttachment(NodeTCPSession.KEY_SSL_INSPECTOR_SERVER_MANAGER);
    }
    
}
