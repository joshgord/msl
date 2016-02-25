/**
 * Copyright (c) 2015 Netflix, Inc.  All rights reserved.
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
 * <p>A {@code MslArray} that encodes its data as JSON.</p>
 * 
 * @author Wesley Miaw <wmiaw@netflix.com>
 */
var JsonMslArray;

(function() {
    "use strict";
    
    JsonMslArray = MslArray.extend({
        /**
         * <p>Create a new {@code MslArray}.</p>
         * 
         * <p>If a source {@code MslArray}, {@code Array}, or an encoded
         * representation is provided then the array will be populated
         * accordingly.</p>
         * 
         * @param {MslEncoderFactory} encoder the encoder factory.
         * @param {{MslArray|Array.<*>|Uint8Array}=} source optional MSL array,
         *        array of elements, or encoded data.
         * @throws MslEncoderException if the data is malformed or invalid.
         * @see #getEncoded()
         */
        init: function init(encoder, source) {
            init.base.call(this);
            
            // Identify the source data.
            var ja = [];
            if (source instanceof Array) {
                ja = source;
            } else if (source instanceof MslArray) {
                ja = source.ja;
            } else if (source instanceof Uint8Array) {
                try {
                    var json = textEncoding$getString(source, MslConstants$DEFAULT_CHARSET);
                    var decoded = JSON.parse(json);
                    if (!(decoded instanceof Array))
                        throw new MslEncoderException("Invalid JSON array encoding.");
                    ja = decoded;
                } catch (e) {
                    if (e instanceof SyntaxError)
                        throw new MslEncoderException("Invalid JSON array encoding.", e);
                    throw e;
                }
            }
            
            // Shallow copy the source data into this MSL array.
            try {
                for (var i = 0; i < ja.length; ++i)
                    this.put(-1, ja[i]);
            } catch (e) {
                if (e instanceof TypeError)
                    throw new MslEncoderException("Invalid MSL array encoding.", e);
                throw e;
            }
        },
        
        /** @inheritDoc */
        put: function put(index, value) {
            var o;
            try {
                // Convert JSONObject to MslObject.
                if (value instanceof Object)
                    o = new JsonMslObject(encoder, value);
                // Convert JSONarray to a MslArray.
                else if (value instanceof Array)
                    o = new JsonMslArray(encoder, value);
                // All other types are OK as-is.
                else
                    o = value;
            } catch (e) {
                if (e instanceof MslEncoderException)
                    throw new IllegalArgumentException("Unsupported JSON object or array representation.", e);
                throw e;
            }
            return put.base.call(this, index, o);
        },
        
        /** @inheritDoc */
        getBytes: function getBytes(index) {
            // When a JsonMslArray is decoded, there's no way for us to know if a
            // value is supposed to be a String to byte[]. Therefore interpret
            // Strings as Base64-encoded data consistent with the toJSONString()
            // and getEncoded().
            var value = this.get(index);
            if (value instanceof Uint8Array)
                return value;
            if (value instanceof String)
                return base64$decode(value.valueOf());
            if (typeof value === 'string')
                return base64$decode(value);
            throw new MslEncoderException("MslArray[" + index + "] is not binary data.");
        },
        
        /** @inheritDoc */
        toJSON: function toJSON() {
            return JSON.stringify(this.list);
        },
    });
})();