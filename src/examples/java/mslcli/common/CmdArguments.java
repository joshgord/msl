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

package mslcli.common;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.netflix.msl.MslConstants;

/**
 * <p>
 * This class parses MSL CLI command-line arguments, validates them,
 * and provides access methods.
 * </p>
 *
 * @author Vadim Spector <vspector@netflix.com>
 */

public final class CmdArguments {

    // parameters
    /** interactive mode */
    public static final String P_INT  = "-int" ;
    /** configuration file */
    public static final String P_CFG  = "-cfg" ;
    /** remote url */
    public static final String P_URL  = "-url" ;
    /** entity id */
    public static final String P_EID  = "-eid" ;
    /** user id */
    public static final String P_UID  = "-uid" ;
    /** key exchange type */
    public static final String P_KX   = "-kx"  ;
    /** key exchange mechanism */
    public static final String P_KXM  = "-kxm" ;
    /** message encrypted */
    public static final String P_ENC  = "-enc" ;
    /** message integrity protected */
    public static final String P_SIG  = "-sig" ;
    /** message non-replayable */
    public static final String P_NREP = "-nrep";
    /** input message payload file */
    public static final String P_IF   = "-if"  ;
    /** output message payload file */
    public static final String P_OF   = "-of"  ;
    /** input message payload text */
    public static final String P_MSG  = "-msg" ;
    /** pre-shared key file path */
    public static final String P_PSK  = "-psk" ;
    /** MSL store file path */
    public static final String P_MST  = "-mst" ;
    /** entity authentication scheme */
    public static final String P_EAS  = "-eas" ;
    /** verbose */
    public static final String P_V    = "-v"   ;

    /** list of supported arguments */
    private static final List<String> supportedArguments =
        Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
            P_INT,
            P_CFG,
            P_URL,
            P_EID,
            P_UID,
            P_KX,
            P_KXM,
            P_ENC,
            P_SIG,
            P_NREP,
            P_IF,
            P_OF,
            P_MSG,
            P_PSK,
            P_MST,
            P_EAS,
            P_V
        )));

    /** underlying representation of arguments */
    private final Map<String,String> argMap;

    /**
     * Ctor.
     *
     * @param args array of arguments
     * @throws IllegalCmdArgumentException
     */
    public CmdArguments(final String[] args) throws IllegalCmdArgumentException {
        if (args == null) {
            throw new IllegalCmdArgumentException("NULL args");
        }
        this.argMap = new HashMap<String,String>();

        String param = null;
        String value = null;
        for (String s : args) {
            // one of the supported parameters
            if (supportedArguments.contains(s)) {
                // already occured - error
                if (argMap.containsKey(s)) {
                    throw new IllegalCmdArgumentException("Multiple Occurences of " + s);
                }
                // expected value, not parameter
                if (param != null) {
                    throw new IllegalCmdArgumentException("Missing Value for " + param);
                }
                // ok, new parameter; previous ones were successfully parsed
                param = s;
            // looks like partameter, but not one of the supported ones - error
            } else if (s.startsWith("-") && (s.length() > 1)) {
                throw new IllegalCmdArgumentException("Illegal Option " + s);
            // if not a parameter, then must be a value
            } else if (param != null) {
                value = s.equals("-") ? null : s; // special case "-" for deleting the value
                argMap.put(param, value);
                param = null;
                value = null;
            // looks like parameter value, but next parameter is expected
            } else {
                throw new IllegalCmdArgumentException("Unexpected Value \"" + s + "\"");
            }
        }
        if (param != null) {
            throw new IllegalCmdArgumentException("Missing Value for Option \"" + param + "\"");
        }
    }

    /**
     * @return all parameters as unmodifiable Map
     */
    public Map<String,String> getParameters() {
        return Collections.unmodifiableMap(argMap);
    }

    /**
     * merge parameters from another CmdArguments instance
     * @param other another CmdArguments instance to merge parameters from
     * @throws IllegalCmdArgumentException
     */
    public void merge(CmdArguments other) throws IllegalCmdArgumentException {
        if (other == null) {
            throw new IllegalArgumentException("NULL CmdArguments argument");
        }
        if (other.argMap.containsKey(P_CFG)) {
            throw new IllegalCmdArgumentException("Cannot reset Configuration File");
        }
        if (other.argMap.containsKey(P_INT)) {
            throw new IllegalCmdArgumentException("Cannot reset Interactive Mode");
        }
        if (other.argMap.containsKey(P_PSK)) {
            throw new IllegalCmdArgumentException("Cannot add PSK file interactively");
        }
        if (other.argMap.containsKey(P_MST) && argMap.containsKey(P_MST)) {
            throw new IllegalCmdArgumentException("Cannot reset MSL Store File");
        }
        for (Map.Entry<String,String> entry : other.argMap.entrySet()) {
            if (entry.getValue() != null) {
                argMap.put(entry.getKey(), entry.getValue());
            } else {
                argMap.remove(entry.getKey());
            }
        }
    }

    /**
     * @return interactive mode true/false
     */
    public boolean isInteractive() {
        return getBoolean(P_INT, false);
    }

    /**
     * @return remote URL - must exist
     * @throws IllegalCmdArgumentException
     */
    public URL getUrl() throws IllegalCmdArgumentException {
        final String url = getRequiredValue(P_URL);
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalCmdArgumentException("Invalid URL " + url, e);
        }
    }

    /**
     * @return configuration file path - must exist and be a regular file
     * @throws IllegalCmdArgumentException
     */
    public String getConfigFilePath() throws IllegalCmdArgumentException {
        final String file = getRequiredValue(P_CFG);
        final File f = new File(file);
        if (f.isFile()) {
            return file;
        } else {
            throw new IllegalCmdArgumentException("Not a File: " + file);
        }
    }

    /**
     * @return file path to read request payload from. Must exist and be a regular file.
     * @throws IllegalCmdArgumentException
     */
    public String getPayloadInputFile() throws IllegalCmdArgumentException {
        final String file = argMap.get(P_IF);
        if (file == null) {
            return null;
        }
        final File f = new File(file);
        if (f.isFile()) {
            return file;
        } else {
            throw new IllegalCmdArgumentException("Not a File: " + file);
        }
    }

    /**
     * @return file path to write response payload to
     * @throws IllegalCmdArgumentException
     */
    public String getPayloadOutputFile() throws IllegalCmdArgumentException {
        final String file = argMap.get(P_OF);
        if (file == null) {
            return null;
        }
        final File f = new File(file);
        if (!f.exists()) {
            return file;
        } else {
            throw new IllegalCmdArgumentException("Cannot Overwrite Existing File: " + file);
        }
    }

    /**
     * @return file path to PSK file. If defined, must be a file.
     * @throws IllegalCmdArgumentException
     */
    public String getPskFile() throws IllegalCmdArgumentException {
        final String file = argMap.get(P_PSK);
        if (file == null) {
            return null;
        }
        final File f = new File(file);
        if (f.isFile()) {
            return file;
        } else {
            throw new IllegalCmdArgumentException("Not a File: " + file);
        }
    }

    /**
     * @return file path to PSK file. If defined, must be a file.
     * @throws IllegalCmdArgumentException
     */
    public String getMslStorePath() throws IllegalCmdArgumentException {
        final String file = argMap.get(P_MST);
        if (file == null) {
            return null;
        }
        final File f = new File(file);
        if (f.isFile()) {
            return file;
        } else if (!f.exists() && (f.getParentFile() == null || f.getParentFile().isDirectory())) {
            return file;
        } else {
            throw new IllegalCmdArgumentException("Invalid File Path: " + file);
        }
    }

    /**
     * @return payload text message
     */
    public byte[] getPayloadMessage() {
        final String s = argMap.get(P_MSG);
        return (s != null) ? s.getBytes(MslConstants.DEFAULT_CHARSET) : null;
    }

    /**
     * @return entityId - must be initialized
     * @throws IllegalCmdArgumentException
     */
    public String getEntityId() throws IllegalCmdArgumentException {
        return getRequiredValue(P_EID);
    }

    /**
     * @return userId - can be uninitialized
     */
    public String getUserId() {
        return argMap.get(P_UID);
    }

    /**
     * @return whether message needs encryption
     */
    public boolean isEncrypted() {
        return getBoolean(P_ENC, true);
    }

    /**
     * @return whether message needs integrity protection
     */
    public boolean isIntegrityProtected() {
        return getBoolean(P_SIG, true);
    }

    /**
     * @return whether message needs to be non-replayable
     */
    public boolean isNonReplayable() {
        return getBoolean(P_NREP, false);
    }

    /**
     * @return get entity authentication scheme
     * @throws IllegalCmdArgumentException if property is not defined
     */
    public String getEntityAuthenticationScheme() throws IllegalCmdArgumentException {
        return getRequiredValue(P_EAS);
    }

    /**
     * @return key exchange scheme
     */
    public String getKeyExchangeScheme() {
        return argMap.get(P_KX);
    }

    /**
     * @return key exchange mechanism
     */
    public String getKeyExchangeMechanism() {
        return argMap.get(P_KXM);
    }

    /**
     * @return verbose mode y/n
     */
    public boolean isVerbose() {
        return getBoolean(P_V, false);
    }

    /**
     * @param name name of the boolean property
     * @param def default value of the boolean property
     * @return property parsed as boolean or default value if it does not exist
     */
    private boolean getBoolean(final String name, final boolean def) {
        final String s = argMap.get(name);
        return (s != null) ? Boolean.parseBoolean(s) : def;
    }

    /**
     * @param name name of the mandatory property
     * @return property value
     * @throws IllegalCmdArgumentException if property is not defined
     */
    private String getRequiredValue(final String name) throws IllegalCmdArgumentException {
        final String s = argMap.get(name);
        if (s != null) {
            return s;
        } else {
            throw new IllegalCmdArgumentException("Missing Required Argument " + name);
        }
    }
}
