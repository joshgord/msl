/**
 * Copyright (c) 2013-2015 Netflix, Inc.  All rights reserved.
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
 * <p>The message capabilities identify the features supported by the message
 * sender.</p>
 * 
 * <p>The message capabilities are represented as
 * {@code
 * capabilities = {
 *   "compressionalgos" : [ enum(GZIP|LZW) ],
 *   "languages" : [ "string" ],
 *   "encoderformats" : [ "string" ],
 * }} where:
 * <ul>
 * <li>{@code compressionalgos} is the set of supported compression algorithms</li>
 * <li>{@code languages} is the preferred list of BCP-47 languages in descending order</li>
 * <li>{@code encoderformats} is the preferred list of MSL encoder formats in descending order</li>
 * </ul></p>
 *
 * @author Wesley Miaw <wmiaw@netflix.com>
 */
var MessageCapabilities;
var MessageCapabilities$parse;
var MessageCapabilities$intersection;

(function() {
    /**
     * Key compression algorithms.
     * @const
     * @type {string}
     */
    var KEY_COMPRESSION_ALGOS = "compressionalgos";
    /**
     * Key languages.
     * @const
     * @type {string}
     */
    var KEY_LANGUAGES = "languages";
    /**
     * Key encoder formats.
     * @const
     * @type {string}
     */
    var KEY_ENCODER_FORMATS = "encoderformats";
    
    /**
     * Computes and returns the intersection of two arrays.
     */
    function computeIntersection(a, b) {
        var hash = {},
            intersection = [],
            i;
        for (i = 0; i < a.length; ++i) {
            hash[a[i]] = true;
        }
        for (i = 0; i < b.length; ++i) {
            if (hash[b[i]])
                intersection.push(b[i]);
        }
        return intersection;
    }
    
    /**
     * Computes and returns the intersection of two message capabilities.
     * 
     * @param {?MessageCapabilities} mc1 first message capabilities. May be {@code null}.
     * @param {?MessageCapabilities} mc2 second message capabilities. May be {@code null}.
     * @return {?MessageCapabilities} the intersection of message capabilities or {@code null} if one
     *         of the message capabilities is {@code null}.
     */
    MessageCapabilities$intersection = function MessageCapabilities$intersection(mc1, mc2) {
        if (!mc1 || !mc2)
            return null;
        
        // Compute the intersection of compression algorithms.
        var compressionAlgos = computeIntersection(mc1.compressionAlgorithms, mc2.compressionAlgorithms);
        
        // Compute the intersection of languages. This may not respect order.
        var languages = computeIntersection(mc1.languages, mc2.languages);
        
        // Compute the intersection of encoder formats. This may not respect
        // order.
        var encoderFormats = computerIntersection(mc1.encoderFormats, mc2.encoderFormats);
        
        return new MessageCapabilities(compressionAlgos, languages, encoderFormats);
    };

    MessageCapabilities = MslEncodable.extend({
        /**
         * Create a new message capabilities object with the specified supported
         * features.
         *
         * @param {Array.<MslConstants$CompressionAlgorithm>} compressionAlgos supported payload compression algorithms.
         * @param {Array.<String>} languages preferred languages as BCP-47 codes in descending
         *        order. May be {@code null}.
         * @param {Array.<MslEncoderFormat>} encoderFormats supported encoder formats. May be {@code null}.
         */
        init: function init(compressionAlgos, languages, encoderFormats) {
            if (!compressionAlgos)
                compressionAlgos = [];
            if (!languages)
                languages = [];
            if (!encoderFormats)
                encoderFormats = [];
            compressionAlgos.sort();

            // The properties.
            var props = {
                /** @type {Array.<MslConstants$CompressionAlgorithm>} */
                compressionAlgorithms: { value: compressionAlgos, writable: false, enumerable: true, configurable: false },
                /** @type {Array.<String>} */
                languages: { value: languages, writable: false, enumerable: true, configurable: false },
                /** @type {Array.<MslEncoderFormat>} */
                encoderFormats: { value: encoderFormats, writable: false, enumerable: true, configurable: false },
            };
            Object.defineProperties(this, props);
        },

        /** @inheritDoc */
        toMslEncoding: function toMslEncoding(encoder, format, callback) {
            try {
                var mo = encoder.createObject();
                mo.put(KEY_COMPRESSION_ALGOS, encoder.createArray(this.compressionAlgorithms));
                mo.put(KEY_LANGUAGES, this.languages);
                mo.put(KEY_ENCODER_FORMATS, encoder.createArray(this.encoderFormats));
                return encoder.encodeObject(mo, format);
            } catch (e) {
                if (e instanceof MslEncoderFormat)
                    throw new MslInternalException("Error encoding MessageCapabilities.", e);
                throw e;
            }
        }

        /** @inheritDoc */
        equals: function equals(that) {
            if (this === that) return true;
            if (!(that instanceof MessageCapabilities)) return false;
            return Arrays$containEachOther(this.compressionAlgorithms, that.compressionAlgorithms) &&
                   Arrays$containEachOther(this.languages, that.languages) &&
                   Arrays$containEachOther(this.encoderFormats, that.encoderFormats);
        },

        /** @inheritDoc */
        uniqueKey: function uniqueKey() {
            return this.compressionAlgorithms.join(':') + '|' + this.languages.join(':') + '|' + this.encoderFormats.join(':');
        },
    });

    /**
     * Construct a new message capabilities object from the provided MSL
     * object.
     *
     * @param {Object} capabilitiesMo the MSL object.
     * @throws MslEncodingException if there is an error parsing the data.
     */
    MessageCapabilities$parse = function MessageCapabilities$parse(capabilitiesMo) {
        try {
            // Extract compression algorithms.
            var compressionAlgos = [];
            var algos = capabilitiesMo.optMslArray(KEY_COMPRESSION_ALGOS);
            for (var i = 0; algos && i < algos.length; ++i) {
                var algo = algos[i];
                // Ignore unsupported algorithms.
                if (MslConstants$CompressionAlgorithm[algo])
                    compressionAlgos.push(algo);
            }
            
            // Extract languages.
            var languages = [];
            var langs = capabilitiesMo.optMslArray(KEY_LANGUAGES);
            for (var i = 0; langs && i < langs.length; ++i)
                languages.push(langs.getString(i));
            
            // Extract encoder formats.
            var encoderFormats = [];
            var formats = capabilitiesMo.optMslArray(KEY_ENCODER_FORMATS);
            for (var i = 0; formats && i < formats.length; ++i) {
                var format = formats.getString(i);
                var encoderFormat = MslEncoderFormat.getFormat(format);
                // Ignore unsupported formats.
                if (encoderFormat)
                    encoderFormats.push(encoderFormat);
            }
            
            return new MessageCapabilities(compressionAlgos, languages, encoderFormats);
        } catch (e) {
            if (e instanceof MslEncoderException)
                throw new MslEncodingException(MslError.MSL_PARSE_ERROR, "capabilities " + capabilitiesMo, e);
            throw e;
        }
    };
})();
