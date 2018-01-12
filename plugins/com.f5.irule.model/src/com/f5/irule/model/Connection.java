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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

public abstract class Connection extends ModelParent {

    private static Logger logger = Logger.getLogger(Connection.class);

    private String address;
    private InetAddress inetAddress;
    private Credentials credentials;
        
    private ProxyDetails proxyDetails;
    private InetAddress proxyInetAddress;

    /* 
     * This returns the address entered by the user
     */
    public String getAddress() {
        return address;
    }

    private void setAddress(String address) {
        this.address = address;
        try {
            inetAddress = InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN, e.getMessage(), e);
            StatusManager.getManager().handle(status, StatusManager.LOG);
        }
    }

    private void setProxyAddress(String address) {
        
        StringTokenizer tokenizer = new StringTokenizer(address, ":");
        String proxyIp = tokenizer.nextToken();
        try {
            proxyInetAddress = InetAddress.getByName(proxyIp);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN, e.getMessage(), e);
            StatusManager.getManager().handle(status, StatusManager.LOG);
        }
    }
    
    /**
     * Get the host address of the Big-IP peer socket {@link InetAddress}
     * (proxy or the actual big-ip ip)
     */
    public String getIp() {
        String ip = proxyInetAddress == null ?
            inetAddress.getHostAddress() :
            proxyInetAddress.getHostAddress();
        return ip;
    }

    public String getUser() {
        return credentials.getUser();
    }

    public String getPassword() {
        return credentials.getPassword();
    }
    
    public boolean isSecureStore() {
        return credentials.isSecureStore();
    }

    public String getProxyIp() {
        return proxyDetails.getIp();
    }
    
    public int getProxyPort() {
        return proxyDetails.getPort();
    }

    public String getProxyUser() {
        return proxyDetails.getUser();
    }

    public String getProxyPassword() {
        return proxyDetails.getPassword();
    }

    public boolean isUseProxy() {
        return proxyDetails.isUseProxy();
    }

    public ProxyDetails getProxyDetails() {
        return proxyDetails;
    }

    public void setProxyDetails(ProxyDetails proxyDetails) {
        this.proxyDetails = proxyDetails;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }
    
    /*
     * Update the credentials in the credentials object and then store them on the filesystem.
     */
    public boolean updateCredentials(IProject project, String user, String password, boolean secureStore)
            throws CoreException {
        credentials.set(user, password, secureStore);
        return credentials.store(project);    
    }

    /*
     * Only store the currently set credentials.  This is useful for the New Connection case where
     * a Connection object has to be created, then connected to, before an IProject exists for
     * storing credentials.
     */    
    public boolean storeCredentials(IProject project) throws CoreException {
        return credentials.store(project);
    }
    
    /**
     * Update the proxy details of this connection (address, user, password)
     */
    public void updateProxyDetails(IProject project, ProxyDetails proxyDetails) throws CoreException {
        this.proxyDetails = proxyDetails;
        proxyDetails.store(project);
    }
    
    /**
     * Store proxy details on the project persistent properties.
     */
    public void storeProxyDetails(IProject project) throws CoreException {
        proxyDetails.store(project);
    }
   
    /*
     * In general, use of this method should be avoided.  It returns the partition set for the
     * project which may not match the partition for a given ModelObject.  When possible,
     * use ModelObject.getPartition().
     */
    public String getCurrentPartition() {
        IProject project = getProject();
        String partition = Ids.PARTITION_COMMON;
        try {
            partition = project.getPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.TMOS_PARTITION));
        } catch (CoreException ex) {
            logger.warn("Failed to get partition of " + project + ": " + ex.getMessage());
            ModelUtils.logError(Messages.FAILED_TO_GET_PARTITION + ": " + project, StatusManager.LOG, ex);
            return null;
        }
        if (partition == null || partition.isEmpty()) {
            partition = Ids.PARTITION_COMMON;
        }
        return partition;
    }

    public abstract RestURI getURI(String module, String component);

    public Connection(String name, Credentials credentials, ProxyDetails proxyDetails) {
        // The empty path assumes that the path for a connection is top level
        super(name, null, null, ModelObject.Type.CONNECTION, new Path(""));
        setType(ModelObject.Type.CONNECTION);
        setAddress(name);
        String proxyIp = proxyDetails.getIp();
        if (proxyDetails.isUseProxy() && proxyIp != null) {
            setProxyAddress(proxyIp);
        }
        this.credentials = credentials;
        this.proxyDetails = proxyDetails;
    }
}
