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

package org.eclipse.jetty.websocket.core.extensions.mux.add;

import static org.hamcrest.Matchers.*;

import org.eclipse.jetty.websocket.core.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.core.extensions.mux.MuxDecoder;
import org.eclipse.jetty.websocket.core.extensions.mux.MuxEncoder;
import org.eclipse.jetty.websocket.core.extensions.mux.MuxOp;
import org.eclipse.jetty.websocket.core.extensions.mux.Muxer;
import org.eclipse.jetty.websocket.core.extensions.mux.op.MuxAddChannelRequest;
import org.eclipse.jetty.websocket.core.extensions.mux.op.MuxAddChannelResponse;
import org.eclipse.jetty.websocket.core.io.LocalWebSocketConnection;
import org.eclipse.jetty.websocket.core.protocol.OpCode;
import org.eclipse.jetty.websocket.core.protocol.WebSocketFrame;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class MuxerAddServerTest
{
    @Rule
    public TestName testname = new TestName();

    @Test
    @Ignore("Interrim, not functional yet")
    public void testAddChannel_Server() throws Exception
    {
        // Server side physical connection
        LocalWebSocketConnection physical = new LocalWebSocketConnection(testname);
        physical.setPolicy(WebSocketPolicy.newServerPolicy());
        physical.onOpen();

        // Client reader
        MuxDecoder clientRead = new MuxDecoder();

        // Build up server side muxer.
        Muxer muxer = new Muxer(physical,clientRead);
        DummyMuxAddServer addServer = new DummyMuxAddServer();
        muxer.setAddServer(addServer);

        // Wire up physical connection to forward incoming frames to muxer
        physical.setIncoming(muxer);

        // Client simulator
        // Can inject mux encapsulated frames into physical connection as if from
        // physical connection.
        MuxEncoder clientWrite = MuxEncoder.toIncoming(physical);

        // Build AddChannelRequest handshake data
        StringBuilder request = new StringBuilder();
        request.append("GET /echo HTTP/1.1\r\n");
        request.append("Host: localhost\r\n");
        request.append("Upgrade: websocket\r\n");
        request.append("Connection: Upgrade\r\n");
        request.append("Sec-WebSocket-Key: ZDTIRU5vU9xOfkg8JAgN3A==\r\n");
        request.append("Sec-WebSocket-Version: 13\r\n");
        request.append("\r\n");

        // Build AddChannelRequest
        MuxAddChannelRequest req = new MuxAddChannelRequest();
        req.setChannelId(1);
        req.setEncoding((byte)0);
        req.setHandshake(request.toString());

        // Have client sent AddChannelRequest
        clientWrite.op(req);

        // Make sure client got AddChannelResponse
        clientRead.assertHasOp(MuxOp.ADD_CHANNEL_RESPONSE,1);
        MuxAddChannelResponse response = (MuxAddChannelResponse)clientRead.getOps().pop();
        Assert.assertThat("AddChannelResponse.channelId",response.getChannelId(),is(1L));
        Assert.assertThat("AddChannelResponse.failed",response.isFailed(),is(false));
        Assert.assertThat("AddChannelResponse.handshake",response.getHandshake(),notNullValue());
        Assert.assertThat("AddChannelResponse.handshakeSize",response.getHandshakeSize(),is(57L));

        clientRead.reset();

        // Send simple echo request
        clientWrite.frame(1,WebSocketFrame.text("Hello World"));

        // Test for echo response (is there a user echo websocket connected to the sub-channel?)
        clientRead.assertHasFrame(OpCode.TEXT,1L,1);
    }
}
