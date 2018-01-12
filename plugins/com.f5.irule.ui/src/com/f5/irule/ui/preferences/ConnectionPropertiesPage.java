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
package com.f5.irule.ui.preferences;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.PropertyPage;

import com.f5.irule.model.BigIPConnection;
import com.f5.irule.model.Credentials;
import com.f5.irule.model.Ids;
import com.f5.irule.model.ModelRoot;
import com.f5.irule.model.ProxyDetails;
import com.f5.irule.ui.views.ExplorerContentProvider;
import com.f5.irule.ui.views.Util;
import com.f5.irule.ui.wizards.NewConnectionWizard;

public class ConnectionPropertiesPage extends PropertyPage {

    private static Logger logger = Logger.getLogger(ConnectionPropertiesPage.class);

    private BigIPConnection conn = null;
    private IProject project = null;
    private String currentPartition = null;
    private final ConnectionSettingsBlock settingsBlock;
    private final ProxySettingsBlock proxyBlock;

    public ConnectionPropertiesPage() {
        this.settingsBlock = new ConnectionSettingsBlock();
        this.proxyBlock = new ProxySettingsBlock();
        this.noDefaultAndApplyButton();
    }

    @Override
    public void applyData(Object data) {
        // The data passed from ProjectPropertiesAction createPropertyDialogOn() is the current Connection
        if (data != null && data instanceof BigIPConnection) {
            conn = (BigIPConnection) data;
            project = conn.getProject();
            try {
                ArrayList<String> partitions = conn.getPartitions();
                if (partitions.isEmpty()) {
                    // must have had a connection issue trying to obtain partitions, add Common
                    partitions.add(Ids.PARTITION_COMMON);
                }
                String[] options = partitions.toArray(new String[0]);
                settingsBlock.setPartitionOptions(options);
                currentPartition = project.getPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.TMOS_PARTITION));
                if (currentPartition == null) {
                    currentPartition = Ids.PARTITION_COMMON;
                }
                int i;
                for (i = 0; i < options.length; i++) {
                    if (options[i].equals(currentPartition)) {
                        break;
                    }
                }
                settingsBlock.setSelectedPartition(i);
                settingsBlock.setUser(conn.getUser());
                settingsBlock.setPassword(conn.getPassword());
                settingsBlock.setSecureStore(conn.isSecureStore());
                
                ProxyDetails proxyDetails = conn.getProxyDetails();
                proxyBlock.setIp(proxyDetails.getIp());
                proxyBlock.setPort(proxyDetails.getPort());
                proxyBlock.setUser(proxyDetails.getUser());
                proxyBlock.setPassword(proxyDetails.getPassword());
                proxyBlock.setSecureStore(proxyDetails.isSecureStore());
                boolean useProxy = proxyDetails.isUseProxy();
                proxyBlock.setUseProxy(useProxy);
            } catch (CoreException ex) {
                logger.warn("Failed to apply data " + data, ex);
            }
        }
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        settingsBlock.createControl(composite);        
        proxyBlock.createControl(composite);
        return composite;
    }

    @Override
    public boolean performOk() {
        // First, a basic ip check
        if (!ConnectionSettingsBlock.checkConnection(this, conn.getAddress())) {
            return super.performOk();
        }
        if (project != null) {
            try {
                boolean reload = shouldReload(conn, project, settingsBlock, proxyBlock, currentPartition);
                if (reload) {
                    // If any of the settings have changed, reload the connection/project                    
                    doReload(settingsBlock, proxyBlock, project.getName());
                }
            } catch (Exception ex) {
                setMessage(ex.getMessage(), IMessageProvider.ERROR);
                return false;
            }
        }
        return super.performOk();
    }

    boolean shouldReload(BigIPConnection connection, IProject project, ConnectionSettingsBlock settingsBlock,
            ProxySettingsBlock proxyBlock, String currentPartition) throws CoreException {

        String newUser = settingsBlock.getUser();
        String newPassword = settingsBlock.getPassword();
        boolean newSecure = settingsBlock.isSecureStore();
        String newPartition = settingsBlock.getPartition();
        boolean reload = false;
        // If credentials changed, store them
        if ((!newUser.equals(connection.getUser()) || !newPassword.equals(connection.getPassword())) ||
                (newSecure != connection.isSecureStore())) {
            connection.updateCredentials(project, newUser, newPassword, newSecure);
            reload = true;
        }
        ProxyDetails proxyDetails = NewConnectionWizard.getProxyDetails(proxyBlock);
        if (!proxyDetails.equals(connection.getProxyDetails())) {
            connection.updateProxyDetails(project, proxyDetails);
            reload = true;   
        }                
        // If partition changed, store it
        if (currentPartition != null && !newPartition.equals(currentPartition)) {
            project.setPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.TMOS_PARTITION), newPartition);
            reload = true;
        }
        return reload;
    }

    /**
     * If the connection user or password have changed then load the connection again from the Big-IP.<br>
     * Otherwise just sync the UI table to reflect partition change
     */
    private static void doReload(ConnectionSettingsBlock settingsBlock, ProxySettingsBlock proxyBlock, String name) {

        ExplorerContentProvider cp = Util.getExplorerContentProvider();
        ModelRoot root = cp.getRoot();
        if (root == null) {
            logger.warn("No Root in provider " + cp);
            return;
        }
        BigIPConnection connection = (BigIPConnection) root.getChild(name);
        if (connection == null) {
            logger.warn("No connection " + name + " in root " + root);
            return;
        }

        String newUser = settingsBlock.getUser();
        String newPassword = settingsBlock.getPassword();
        String connectionUser = connection.getUser();
        String connectionPassword = connection.getPassword();
        if (connectionUser.equals(newUser) && connectionPassword.equals(newPassword)) {
            cp.syncWithUi();
        } else {
            // Reload the Connection
            root.removeChild(connection);
            Credentials credentials = new Credentials(settingsBlock.getUser(), settingsBlock.getPassword(), settingsBlock.isSecureStore());
            ProxyDetails proxyDetails = new ProxyDetails(proxyBlock.getIp(), proxyBlock.getPort(), proxyBlock.getUser(),
                proxyBlock.getPassword(), proxyBlock.isUseProxy(), proxyBlock.isSecureStore());
            cp.loadConnection(name, credentials, proxyDetails, null, null, null);
        }
    }

}
