/*******************************************************************************
 * Copyright 2015-2017 F5 Networks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.f5.irule.model;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;

public class ProxyDetails {

    private static Logger logger = Logger.getLogger(ProxyDetails.class);

    private String user;
    private String password;
    private String ip;
    private int port = -1;
    private boolean useProxy;
    private boolean secureStore;

    public ProxyDetails (String ip, int port, String user, String password, boolean useProxy, boolean secureStore) {
        this.user = user;
        this.password = password;
        this.useProxy = useProxy;
        this.ip = ip;
        this.port = port;
        this.secureStore = secureStore;
    }
    
    public ProxyDetails (IProject project) {
        load(project);
    }
    
    public String getIp() {
        return ip;
    }
    
    public int getPort() {
        return port;
    }
    
    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }
    
    public boolean isUseProxy() {
        return useProxy;
    }

    public boolean isSecureStore() {
        return secureStore;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(getClass().getSimpleName());
        builder.append(" ").append(useProxy);
        builder.append(" ").append(ip);
        builder.append(" ").append(port);
        builder.append(" ").append(user);
        builder.append(" ").append(password);
        builder.append("]");
        return builder.toString();
    }
    
    /*
     * Load persisted creds from filesystem
     */
    public ProxyDetails load(IProject project) {
        try {
            ip = project.getPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.PROXY_IP));
            if (ip == null) {
                useProxy = false;
            } else {
                String portValue = project.getPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.PROXY_PORT));
                if (portValue != null) {
                    port = Integer.parseInt(portValue);
                }

                String secure = project.getPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.PROXY_SECURE_CREDENTIALS));
                if (secure != null && secure.equals(Ids.TRUE)) {
                    ISecurePreferences root = SecurePreferencesFactory.getDefault();
                    ISecurePreferences node = root.node(Ids.PLUGIN + "/" + project.getName());
                    user = node.get(Ids.PROXY_USER, null);
                    password = node.get(Ids.PROXY_PASSWORD, null);
                    secureStore = true;
                } else {
                    user = project.getPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.PROXY_USER));
                    password = project.getPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.PROXY_PASSWORD));
                    secureStore = false;
                }
                useProxy = Boolean.parseBoolean(project.getPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.USE_PROXY)));
            }
        } catch (StorageException ex) {
            logger.warn("Failed to load project " + project, ex);
        } catch (CoreException ex) {
            logger.warn("Failed to load project " + project, ex);
        }
        return this;
    }
    
    /*
     * Store proxy details as project persistent properties.
     */
    public void store(IProject project) throws CoreException {
        project.setPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.PROXY_IP), ip);
        project.setPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.PROXY_PORT), String.valueOf(port));
        project.setPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.USE_PROXY), String.valueOf(useProxy));
        if (secureStore) {
            ISecurePreferences root = SecurePreferencesFactory.getDefault();
            ISecurePreferences node = root.node(Ids.PLUGIN + "/" + project.getName());
            try {
                node.put(Ids.PROXY_USER, user, false);
                node.put(Ids.PROXY_PASSWORD, password, true);
                // clear user/password from standard project props
                project.setPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.PROXY_USER), null);
                project.setPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.PROXY_PASSWORD), null);
                project.setPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.PROXY_SECURE_CREDENTIALS), Ids.TRUE);
            } catch (StorageException ex) {
                logger.warn("Failed to store proxy credentials", ex);
            }
        } else {
            project.setPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.PROXY_USER), user);
            project.setPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.PROXY_PASSWORD), password);
            project.setPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.PROXY_SECURE_CREDENTIALS), Ids.FALSE);
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ProxyDetails)) {
            return false;
        }
        ProxyDetails other = (ProxyDetails) obj;
        boolean ans = equalStrings(user, other.user) &&
            equalStrings(password, other.password) &&
            equalStrings(ip, other.ip) &&
            (port == other.port) &&
            (useProxy == other.useProxy) &&
            (secureStore == other.secureStore);
        return ans;
    }

    @Override
    public int hashCode() {
        return ip.hashCode() * port;
    }

    private boolean equalStrings(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }
        return s1.equals(s2);
    }
}
