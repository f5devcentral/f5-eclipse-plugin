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
package com.f5.irule.ui.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.f5.irule.model.BigIPConnection;
import com.f5.irule.model.Credentials;
import com.f5.irule.model.ModelRoot;
import com.f5.irule.model.ProxyDetails;
import com.f5.irule.model.RequestCompletion;
import com.f5.irule.ui.Strings;
import com.f5.irule.ui.preferences.ConnectionSettingsBlock;
import com.f5.irule.ui.preferences.ProxySettingsBlock;
import com.f5.irule.ui.views.ExplorerContentProvider;
import com.f5.irule.ui.views.Util;
import com.google.gson.JsonObject;

public class NewConnectionWizard extends Wizard implements INewWizard {

    private NewConnectionPage newConnectionPage = new NewConnectionPage();

    public void addPages() {
        addPage(newConnectionPage);
    }

    public void init(IWorkbench workbench, IStructuredSelection selection) {
    }

    @Override
    public boolean performFinish() {
        final String ip = newConnectionPage.getIP();
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProject project = workspace.getRoot().getProject(ip);
        if (project.exists()) {
            newConnectionPage.setMessage(Strings.MESSAGE_PROJECT_EXISTS, IMessageProvider.ERROR);
            return false;            
        }
        try {
            if (ConnectionSettingsBlock.checkConnection(newConnectionPage, ip)) {
            	// Now try connection with auth, load contents if successful
				openConnection(project, ip);
				// Return false so the dialog wouldn't close.
				// It would only be closed when the connection initialization completes successfully.
				return false;   	
            }
			else {
                return false;
            }
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            newConnectionPage.setMessage(errorMessage, IMessageProvider.ERROR);
            return false;
        }
    }

    /**
     * Open a connection into the project.<br>
     * Update the connection credentials and proxy details from the page inputs and call for<br>
     * {@link ExplorerContentProvider #loadConnection(String, Credentials, ProxyDetails, IProject, Shell, RequestCompletion)}
     */
    private void openConnection(IProject project, String name) {
        
        ExplorerContentProvider cp = Util.getExplorerContentProvider();
        ModelRoot root = cp.getRoot();        
        BigIPConnection connection = (BigIPConnection) root.getChild(name);
        ConnectionSettingsBlock settingsBlock = newConnectionPage.getSettingsBlock();
        Credentials credentials = getCredentials(settingsBlock);
        if (connection != null) {
            connection.setCredentials(credentials);
        }
        ProxySettingsBlock proxyBlock = newConnectionPage.getProxyBlock();
        ProxyDetails proxyDetails = getProxyDetails(proxyBlock);
        if (connection != null) {
            connection.setProxyDetails(proxyDetails);
        }
        
        WizardDialog wizardDialog = (WizardDialog) getContainer();
        PerformFinishCompletion performFinishCompletion = new PerformFinishCompletion(wizardDialog);
        Shell shell = wizardDialog.getShell();
        cp.loadConnection(name, credentials, proxyDetails, project, shell, performFinishCompletion);
    }

    public static ProxyDetails getProxyDetails(ProxySettingsBlock proxyBlock) {
        String proxyIp = proxyBlock.getIp();
        int proxyPort = proxyBlock.getPort();
        String proxyUser = proxyBlock.getUser();
        String proxyPassword = proxyBlock.getPassword();
        boolean useProxy = proxyBlock.isUseProxy();
        boolean secureStore = proxyBlock.isSecureStore();
        ProxyDetails proxyDetails = new ProxyDetails(proxyIp, proxyPort, proxyUser, proxyPassword, useProxy, secureStore);
        return proxyDetails;
    }

    private static Credentials getCredentials(ConnectionSettingsBlock settingsBlock) {
        String user = settingsBlock.getUser();
        String password = settingsBlock.getPassword();
        boolean secureStore = settingsBlock.isSecureStore();
        Credentials credentials = new Credentials(user, password, secureStore);
        return credentials;
    }

	private class PerformFinishCompletion extends RequestCompletion {

		private WizardDialog wizardDialog;

		private PerformFinishCompletion(WizardDialog wizardDialog) {
			this.wizardDialog = wizardDialog;
		}

		@Override
		public void completed(String method, String uri, JsonObject jsonBody) {
			// asynchronously close the wizard dialog (done by main thread)
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					wizardDialog.close();
				}
			});
		}

		@Override
		public void failed(Exception ex, String method, String uri, String responseBody) {
		    if (ex != null) {
		        ex.printStackTrace();
		    }
		}
    	
    }
}
