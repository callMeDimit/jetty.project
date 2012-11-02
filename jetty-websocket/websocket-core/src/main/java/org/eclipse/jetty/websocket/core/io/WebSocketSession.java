//
//  ========================================================================
//  Copyright (c) 1995-2012 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.websocket.core.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.core.api.LogicalConnection;
import org.eclipse.jetty.websocket.core.api.SuspendToken;
import org.eclipse.jetty.websocket.core.api.WebSocketConnection;
import org.eclipse.jetty.websocket.core.api.WebSocketException;
import org.eclipse.jetty.websocket.core.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.core.io.event.EventDriver;
import org.eclipse.jetty.websocket.core.protocol.CloseInfo;
import org.eclipse.jetty.websocket.core.protocol.ConnectionState;
import org.eclipse.jetty.websocket.core.protocol.OpCode;
import org.eclipse.jetty.websocket.core.protocol.WebSocketFrame;

public class WebSocketSession implements WebSocketConnection, IncomingFrames, OutgoingFrames
{
    private static final Logger LOG = Log.getLogger(WebSocketSession.class);

    /**
     * The reference to the logical connection.
     * <p>
     * This will be the {@link AbstractWebSocketConnection} on normal websocket use, and be a MuxChannel when MUX is in the picture.
     */
    private final LogicalConnection baseConnection;
    private final WebSocketPolicy policy;
    private final String subprotocol;
    private final EventDriver websocket;
    private OutgoingFrames outgoing;

    public WebSocketSession(EventDriver websocket, LogicalConnection connection, WebSocketPolicy policy, String subprotocol)
    {
        super();
        this.websocket = websocket;
        this.baseConnection = connection;
        this.policy = policy;
        this.subprotocol = subprotocol;
    }

    @Override
    public void close()
    {
        baseConnection.close();
    }

    @Override
    public void close(int statusCode, String reason)
    {
        baseConnection.close(statusCode,reason);
    }

    @Override
    public void disconnect()
    {
        baseConnection.disconnect();
    }

    public IncomingFrames getIncoming()
    {
        return websocket;
    }

    public OutgoingFrames getOutgoing()
    {
        return outgoing;
    }

    @Override
    public WebSocketPolicy getPolicy()
    {
        return policy;
    }

    @Override
    public InetSocketAddress getRemoteAddress()
    {
        return baseConnection.getRemoteAddress();
    }

    @Override
    public ConnectionState getState()
    {
        return baseConnection.getState();
    }

    @Override
    public String getSubProtocol()
    {
        return subprotocol;
    }

    @Override
    public void incoming(WebSocketException e)
    {
        if (baseConnection.isInputClosed())
        {
            return; // input is closed
        }
        // pass on incoming to websocket itself
        websocket.incoming(e);
    }

    @Override
    public void incoming(WebSocketFrame frame)
    {
        if (baseConnection.isInputClosed())
        {
            return; // input is closed
        }
        // pass on incoming to websocket itself
        websocket.incoming(frame);
    }

    @Override
    public boolean isInputClosed()
    {
        return baseConnection.isInputClosed();
    }

    @Override
    public boolean isOpen()
    {
        return baseConnection.isOpen();
    }

    @Override
    public boolean isOutputClosed()
    {
        return baseConnection.isOutputClosed();
    }

    @Override
    public boolean isReading()
    {
        return baseConnection.isReading();
    }

    @Override
    public void onCloseHandshake(boolean incoming, CloseInfo close)
    {
        baseConnection.onCloseHandshake(incoming,close);
    }

    public void onConnect()
    {
        LOG.debug("onConnect()");
        websocket.setSession(this);
        websocket.onConnect();
    }

    @Override
    public <C> void output(C context, Callback<C> callback, WebSocketFrame frame) throws IOException
    {
        if (LOG.isDebugEnabled())
        {
            LOG.debug("output({},{},{}) - {}",context,callback,frame,outgoing);
        }
        // forward on to chain
        outgoing.output(context,callback,frame);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> void ping(C context, Callback<C> callback, byte[] payload) throws IOException
    {
        // Delegate the application called ping to the OutgoingFrames interface to allow
        // extensions to process the frame appropriately.
        WebSocketFrame frame = new WebSocketFrame(OpCode.PING).setPayload(payload);
        frame.setFin(true);
        output(context,callback,frame);
    }

    public void setOutgoing(OutgoingFrames outgoing)
    {
        this.outgoing = outgoing;
    }

    @Override
    public SuspendToken suspend()
    {
        return baseConnection.suspend();
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("WebSocketSession[websocket=");
        builder.append(websocket);
        builder.append(",baseConnection=");
        builder.append(baseConnection);
        builder.append(",subprotocol=");
        builder.append(subprotocol);
        builder.append(",outgoing=");
        builder.append(outgoing);
        builder.append("]");
        return builder.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> void write(C context, Callback<C> callback, byte[] buf, int offset, int len) throws IOException
    {
        if (baseConnection.isOutputClosed())
        {
            return; // output is closed
        }
        if (LOG.isDebugEnabled())
        {
            LOG.debug("write(context,{},byte[],{},{})",callback,offset,len);
        }
        // Delegate the application called write to the OutgoingFrames interface to allow
        // extensions to process the frame appropriately.
        WebSocketFrame frame = WebSocketFrame.binary().setPayload(buf,offset,len);
        frame.setFin(true);
        output(context,callback,frame);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> void write(C context, Callback<C> callback, ByteBuffer buffer) throws IOException
    {
        if (baseConnection.isOutputClosed())
        {
            return; // output is closed
        }
        if (LOG.isDebugEnabled())
        {
            LOG.debug("write(context,{},ByteBuffer->{})",callback,BufferUtil.toDetailString(buffer));
        }
        // Delegate the application called write to the OutgoingFrames interface to allow
        // extensions to process the frame appropriately.
        WebSocketFrame frame = WebSocketFrame.binary().setPayload(buffer);
        frame.setFin(true);
        output(context,callback,frame);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> void write(C context, Callback<C> callback, String message) throws IOException
    {
        if (baseConnection.isOutputClosed())
        {
            return; // output is closed
        }
        if (LOG.isDebugEnabled())
        {
            LOG.debug("write(context,{},message.length:{})",callback,message.length());
        }
        // Delegate the application called ping to the OutgoingFrames interface to allow
        // extensions to process the frame appropriately.
        WebSocketFrame frame = WebSocketFrame.text(message);
        frame.setFin(true);
        output(context,callback,frame);
    }
}
