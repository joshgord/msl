/**
 * Copyright (c) 2014 Netflix, Inc.  All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mslcli.client;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.netflix.msl.MslException;
import com.netflix.msl.msg.ErrorHeader;
import com.netflix.msl.msg.MessageContext;
import com.netflix.msl.msg.MslControl;
import com.netflix.msl.msg.MslControl.MslChannel;
import com.netflix.msl.util.MslContext;
import com.netflix.msl.util.MslStore;

import mslcli.client.msg.ClientRequestMessageContext;
import mslcli.client.msg.MessageConfig;
import mslcli.client.util.KeyRequestDataHandle;
import mslcli.client.util.UserAuthenticationDataHandle;

import mslcli.client.util.ClientMslContext;

import mslcli.common.IllegalCmdArgumentException;
import mslcli.common.util.AppContext;
import mslcli.common.util.ConfigurationException;
import mslcli.common.util.SharedUtil;

/**
 * <p>
 * MSL client. Hides some complexities of MSL core APIs and send/receive error handling.
 * Instance of thsi class is bound to a given client entity identity via an instance of
 * ClientMslConfig, which, in turn, is bound to a given client entity identity.
 * </p>
 *
 * @author Vadim Spector <vspector@netflix.com>
 */

public final class Client {

    /** timeout in milliseconds for processing request and composing response */
    private static final int TIMEOUT_MS = 120 * 1000;

    /**
     * Data object encapsulating payload and/or error header from the server
     */
    public static final class Response {
        /**
         * @param payload message application payload of the response, if any
         * @param errHeader MSL error header in the response, if any
         */
        private Response(final byte[] payload, final ErrorHeader errHeader) {
            this.payload = payload;
            this.errHeader = errHeader;
        }
        /**
         * @return application payload in the response, if any
         */
        public byte[] getPayload() {
            return payload;
        }
        /**
         * @return MSL error header in the response, if any
         */
        public ErrorHeader getErrorHeader() {
            return errHeader;
        }
        /** application payload */
        private final byte[] payload;
        /** MSL error header */
        private final ErrorHeader errHeader;
    }
        
    /**
     * @param appCtx application context
     * @param userAuthenticationDataHandle callback for obtaining user authentication data
     * @param keyRequestDataHandle callback for obtaining key request data
     * @param mslCfg encapsulation of MSL configuration parameters
     * @throws ConfigurationException
     * @throws IllegalCmdArgumentException
     */
    public Client(final AppContext appCtx,
                  final UserAuthenticationDataHandle userAuthenticationDataHandle,
                  final KeyRequestDataHandle keyRequestDataHandle,
                  final ClientMslConfig mslCfg
                 ) throws ConfigurationException, IllegalCmdArgumentException
    {
        if (appCtx == null) {
            throw new IllegalArgumentException("NULL app context");
        }
        if (userAuthenticationDataHandle == null) {
            throw new IllegalArgumentException("NULL user authentication data handle");
        }
        if (keyRequestDataHandle == null) {
            throw new IllegalArgumentException("NULL key request data handle");
        }

        // Initialize app context.
        this.appCtx = appCtx;

        // Set user authentication data handle
        this.userAuthenticationDataHandle = userAuthenticationDataHandle;

        // Set key request data handle
        this.keyRequestDataHandle = keyRequestDataHandle;

        // set MSL configuration
        this.mslCfg = mslCfg;

        // Set up the MSL context
        this.mslCtx = new ClientMslContext(appCtx, mslCfg);

        // Set up the MSL Control
        this.mslCtrl = appCtx.getMslControl();
    }

    /**
     * Send single request.
     * @param request message payload to send
     * @param cfg message security policies
     * @param remoteUrl target URL for sending message
     * @return response encapsulating payload and/or error header
     * @throws ExecutionException
     * @throws IOException
     * @throws InterruptedException
     * @throws MslException
     */
    public Response sendRequest(final byte[] request, final MessageConfig cfg, final URL remoteUrl)
        throws ExecutionException, IOException, InterruptedException, MslException
    {
        if (userAuthenticationDataHandle == null) {
            throw new IllegalStateException("Uninitialized UserAuthenticationDataHandle");
        }

        final MessageContext msgCtx = new ClientRequestMessageContext(
            cfg,
            userAuthenticationDataHandle,
            keyRequestDataHandle,
            request
            );

        final Future<MslChannel> f = mslCtrl.request(mslCtx, msgCtx, remoteUrl, TIMEOUT_MS);
        final MslChannel ch;
        ch = f.get();
        if (ch == null)
            return null;

        final ErrorHeader errHeader = ch.input.getErrorHeader();
        if (errHeader == null) {
            return new Response(SharedUtil.readIntoArray(ch.input), null);
        } else {
            return new Response(null, errHeader);
        }
    }

    /**
     * @return MSL Store
     */
    public MslStore getMslStore() {
        return mslCfg.getMslStore();
    }

    /**
     * save MSL Store
     * @throws IOException if cannot save MSL store
     */
    public void saveMslStore() throws IOException {
        mslCfg.saveMslStore();
    }

    /** App context */
    private final AppContext appCtx;

    /** MSL context */
    private final MslContext mslCtx;

    /** MSL config */
    private final ClientMslConfig mslCfg;

    /** MSL control */
    private final MslControl mslCtrl;

    /** User Authentication Data Supplier */
    private final UserAuthenticationDataHandle userAuthenticationDataHandle;

    /** Key Request Data Supplier */
    private final KeyRequestDataHandle keyRequestDataHandle;
}
