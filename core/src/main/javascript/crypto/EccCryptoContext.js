/**
 * Copyright (c) 2012-2015 Netflix, Inc.  All rights reserved.
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

/**
 * <p>An ECC crypto context supports sign.</p>
 *
 * @author Pablo Pissanetzky <ppissanetzky@netflix.com>
 * @implements {ICryptoContext}
 */
var EccCryptoContext;

(function() {
    "use strict";

    EccCryptoContext = ICryptoContext.extend({
        /**
         * <p>Create a new ECC crypto context for sign using the provided private key.
         *
         * @param {MslContext} ctx MSL context.
         * @param {PrivateKey} privateKey the private key.
         * @constructor
         */
        init: function init(ctx, privateKey) {
            init.base.call(this);

            // The properties.
            var props = {
                privateKey: { value: privateKey, writable: false, enumerable: false, configurable: false }
            };
            Object.defineProperties(this, props);
        },

        /** @inheritDoc */
        encrypt: function encrypt(data, encoder, format, callback) {
            AsyncExecutor(callback, function() {
                return data;
            }, this);
        },

        /** @inheritDoc */
        decrypt: function decrypt(data, encoder, callback) {
            AsyncExecutor(callback, function() {
                return data;
            }, this);
        },

        /** @inheritDoc */
        wrap: function wrap(key, encoder, format, callback) {
            AsyncExecutor(callback, function() {
                throw new MslCryptoException(MslError.WRAP_NOT_SUPPORTED, "ECC does not wrap");
            }, this);
        },

        /** @inheritDoc */
        unwrap: function unwrap(data, algo, usages, encoder, callback) {
            AsyncExecutor(callback, function() {
                throw new MslCryptoException(MslError.UNWRAP_NOT_SUPPORTED, "ECC does not unwrap");
            }, this);
        },

        /** @inheritDoc */
        sign: function sign(data, encoder, format, callback) {
            AsyncExecutor(callback, function() {
                var oncomplete = function(hash) {
                    // Return the signature envelope byte representation.
                    MslSignatureEnvelope$create(new Uint8Array(hash), {
                        result: function(envelope) {
                            envelope.getBytes(encoder, format, {
                                result: function(bytes) {
                                    callback.result(bytes);
                                },
                                error: callback.error,
                            });
                        },
                        error: callback.error
                    });
                };
                var onerror = function(e) {
                    callback.error(new MslCryptoException(MslError.SIGNATURE_ERROR));
                };
                mslCrypto['sign'](WebCryptoAlgorithm.ECDSA_SHA256, this.privateKey, data)
                    .then(oncomplete, onerror);
            }, this);
        },

        /** @inheritDoc */
        verify: function verify(data, signature, encoder, callback) {
            AsyncExecutor(callback, function() {
                throw new MslCryptoException(MslError.VERIFY_NOT_SUPPORTED, "ECC does not verify");
            }, this);
        }
    });
})();
