/*
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Copyright 1999,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package org.apache.tomcat.util.net.jsse;

import java.io.*;
import java.net.*;
import java.util.Vector;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.KeyManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.tomcat.util.res.StringManager;

/*
  1. Make the JSSE's jars available, either as an installed
     extension (copy them into jre/lib/ext) or by adding
     them to the Tomcat classpath.
  2. keytool -genkey -alias tomcat -keyalg RSA
     Use "changeit" as password ( this is the default we use )
 */

/**
 * SSL server socket factory. It _requires_ a valid RSA key and
 * JSSE. 
 *
 * @author Harish Prabandham
 * @author Costin Manolache
 * @author Stefan Freyr Stefansson
 * @author EKR -- renamed to JSSESocketFactory
 * @author Jan Luehe
 */
public class JSSE14SocketFactory  extends JSSESocketFactory {

    private static StringManager sm =
        StringManager.getManager("org.apache.tomcat.util.net.jsse.res");

    public JSSE14SocketFactory () {
        super();
    }

    /**
     * Reads the keystore and initializes the SSL socket factory.
     */
    void init() throws IOException {
        try {

            String clientAuthStr = (String) attributes.get("clientauth");
            if (clientAuthStr != null){
                clientAuth = Boolean.valueOf(clientAuthStr).booleanValue();
            }

            // SSL protocol variant (e.g., TLS, SSL v3, etc.)
            String protocol = (String) attributes.get("protocol");
            if (protocol == null) {
                protocol = defaultProtocol;
            }

            // Certificate encoding algorithm (e.g., SunX509)
            String algorithm = (String) attributes.get("algorithm");
            if (algorithm == null) {
                algorithm = defaultAlgorithm;
            }

            String keystoreType = (String) attributes.get("keystoreType");
            if (keystoreType == null) {
                keystoreType = defaultKeystoreType;
            }

            // Create and init SSLContext
            SSLContext context = SSLContext.getInstance(protocol);
 
            // Configure SSL session timeout and cache size
            configureSSLSessionContext(context.getServerSessionContext());
                
            context.init(getKeyManagers(keystoreType, algorithm,
                                        (String) attributes.get("keyAlias")),
                         getTrustManagers(keystoreType),
                         new SecureRandom());

            // create proxy
            sslProxy = context.getServerSocketFactory();

            // Determine which cipher suites to enable
            String requestedCiphers = (String)attributes.get("ciphers");
            if (requestedCiphers != null) {
                enabledCiphers = getEnabledCiphers(requestedCiphers,
                                                   sslProxy.getSupportedCipherSuites());
            }

        } catch(Exception e) {
            if( e instanceof IOException )
                throw (IOException)e;
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Gets the initialized key managers.
     */
    protected KeyManager[] getKeyManagers(String keystoreType,
                                          String algorithm,
                                          String keyAlias)
                throws Exception {

        KeyManager[] kms = null;

        String keystorePass = getKeystorePassword();

        KeyStore ks = getKeystore(keystoreType, keystorePass);
        if (keyAlias != null && !ks.isKeyEntry(keyAlias)) {
            throw new IOException(sm.getString("jsse.alias_no_key_entry", keyAlias));
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
        kmf.init(ks, keystorePass.toCharArray());

        kms = kmf.getKeyManagers();
        if (keyAlias != null) {
            if (JSSESocketFactory.defaultKeystoreType.equals(keystoreType)) {
                keyAlias = keyAlias.toLowerCase();
            }
            for(int i=0; i<kms.length; i++) {
                kms[i] = new JSSEKeyManager((X509KeyManager)kms[i], keyAlias);
            }
        }

        return kms;
    }

    /**
     * Gets the intialized trust managers.
     */
    protected TrustManager[] getTrustManagers(String keystoreType)
                throws Exception {

        TrustManager[] tms = null;

        KeyStore trustStore = getTrustStore(keystoreType);
        if (trustStore != null) {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(trustStore);
            tms = tmf.getTrustManagers();
        }

        return tms;
    }
    protected void setEnabledProtocols(SSLServerSocket socket, String []protocols){
        if (protocols != null) {
            socket.setEnabledProtocols(protocols);
        }
    }

    protected String[] getEnabledProtocols(SSLServerSocket socket,
                                           String requestedProtocols){
        String[] supportedProtocols = socket.getSupportedProtocols();

        String[] enabledProtocols = null;

        if (requestedProtocols != null) {
            Vector vec = null;
            String protocol = requestedProtocols;
            int index = requestedProtocols.indexOf(',');
            if (index != -1) {
                int fromIndex = 0;
                while (index != -1) {
                    protocol = requestedProtocols.substring(fromIndex, index).trim();
                    if (protocol.length() > 0) {
                        /*
                         * Check to see if the requested protocol is among the
                         * supported protocols, i.e., may be enabled
                         */
                        for (int i=0; supportedProtocols != null
                                     && i<supportedProtocols.length; i++) {
                            if (supportedProtocols[i].equals(protocol)) {
                                if (vec == null) {
                                    vec = new Vector();
                                }
                                vec.addElement(protocol);
                                break;
                            }
                        }
                    }
                    fromIndex = index+1;
                    index = requestedProtocols.indexOf(',', fromIndex);
                } // while
                protocol = requestedProtocols.substring(fromIndex);
            }

            if (protocol != null) {
                protocol = protocol.trim();
                if (protocol.length() > 0) {
                    /*
                     * Check to see if the requested protocol is among the
                     * supported protocols, i.e., may be enabled
                     */
                    for (int i=0; supportedProtocols != null
                                 && i<supportedProtocols.length; i++) {
                        if (supportedProtocols[i].equals(protocol)) {
                            if (vec == null) {
                                vec = new Vector();
                            }
                            vec.addElement(protocol);
                            break;
                        }
                    }
                }
            }           

            if (vec != null) {
                enabledProtocols = new String[vec.size()];
                vec.copyInto(enabledProtocols);
            }
        }

        return enabledProtocols;
    }


    /*
     * Configures the given SSLSessionContext.
     *
     * @param sslSessionCtxt The SSLSessionContext to configure
     */
    private void configureSSLSessionContext(SSLSessionContext sslSessionCtxt) {

        String attrValue = (String) attributes.get("sslSessionTimeout");
        if (attrValue != null) {
            sslSessionCtxt.setSessionTimeout(
                Integer.valueOf(attrValue).intValue());
        }

        attrValue = (String) attributes.get("ssl3SessionTimeout");
        if (attrValue != null) {
            sslSessionCtxt.setSessionTimeout(
                Integer.valueOf(attrValue).intValue());
        }
     
        attrValue = (String) attributes.get("sslSessionCacheSize");
        if (attrValue != null) {
            sslSessionCtxt.setSessionCacheSize(
                Integer.valueOf(attrValue).intValue());
        }
    }

}