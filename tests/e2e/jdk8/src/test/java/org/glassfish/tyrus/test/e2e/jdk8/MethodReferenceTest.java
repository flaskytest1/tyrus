/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.tyrus.test.e2e.jdk8;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class MethodReferenceTest extends TestContainer {

    private final CountDownLatch messageLatchWhole = new CountDownLatch(1);
    private final CountDownLatch onCloseLatchWhole = new CountDownLatch(1);
    private final CountDownLatch messageLatchPartial = new CountDownLatch(1);
    private final CountDownLatch onCloseLatchPartial = new CountDownLatch(1);

    @ServerEndpoint("/methodReferenceTestEchoWhole")
    public static class EchoWholeEndpoint {

        @OnMessage
        public void echo(Session session, String message) throws IOException {
            session.getBasicRemote().sendText(message + " (from your server)");
            session.close();
        }

        @OnError
        public void onError(Throwable t) {
            t.printStackTrace();

        }
    }

    @Test
    public void echoWhole() throws DeploymentException, InterruptedException, IOException {
        final Server server = startServer(EchoWholeEndpoint.class);

        try {
            final ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {

                    try {
                        session.addMessageHandler(String.class, MethodReferenceTest.this::onMessageWhole);
                        session.getBasicRemote().sendText("Do or do not, there is no try.");
                    } catch (IOException e) {
                        // do nothing
                    }
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    System.out.println("### Client session closed: " + closeReason);
                    onCloseLatchWhole.countDown();
                }

            }, ClientEndpointConfig.Builder.create().build(), getURI(EchoWholeEndpoint.class));

            assertTrue(messageLatchWhole.await(1, TimeUnit.SECONDS));
            assertTrue(onCloseLatchWhole.await(1, TimeUnit.SECONDS));

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    public void onMessageWhole(String message) {
        System.out.println("### Received: " + message);

        if (message.equals("Do or do not, there is no try. (from your server)")) {
            messageLatchWhole.countDown();
        }
    }

    @ServerEndpoint("/methodReferenceTestEchoPartial")
    public static class EchoPartialEndpoint {

        @OnMessage
        public void echo(Session session, String message) throws IOException {
            session.getBasicRemote().sendText(message + " (from your server)", false);
            session.getBasicRemote().sendText("", true);
            session.close();
        }

        @OnError
        public void onError(Throwable t) {
            t.printStackTrace();

        }
    }

    @Test
    public void echoPartial() throws DeploymentException, InterruptedException, IOException {
        final Server server = startServer(EchoPartialEndpoint.class);

        try {
            final ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {

                    try {
                        session.addMessageHandler(String.class, MethodReferenceTest.this::onMessagePartial);
                        session.getBasicRemote().sendText("Do or do not, there is no try.");
                    } catch (IOException e) {
                        // do nothing
                    }
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    System.out.println("### Client session closed: " + closeReason);
                    onCloseLatchPartial.countDown();
                }

            }, ClientEndpointConfig.Builder.create().build(), getURI(EchoPartialEndpoint.class));

            assertTrue(messageLatchPartial.await(1, TimeUnit.SECONDS));
            assertTrue(onCloseLatchPartial.await(1, TimeUnit.SECONDS));

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    public void onMessagePartial(String message, boolean last) {
        System.out.println("### Received: " + message + " " + last);

        if (message.equals("Do or do not, there is no try. (from your server)") && !last) {
            messageLatchPartial.countDown();
        }
    }
}
