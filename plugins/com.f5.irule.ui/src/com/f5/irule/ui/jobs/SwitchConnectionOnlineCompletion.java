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
package com.f5.irule.ui.jobs;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

import com.f5.irule.model.BigIPConnection;
import com.f5.irule.model.RequestCompletion;
import com.f5.irule.model.ModelObject;
import com.f5.irule.ui.Ids;
import com.f5.irule.ui.Strings;
import com.f5.irule.ui.views.ExplorerContentProvider;
import com.f5.irule.ui.views.Util;
import com.f5.irule.ui.wizards.SyncModifiedResourcesWizard;
import com.google.gson.JsonObject;

/**
 * A {@link RequestCompletion} that on completion opens the {@link SyncModifiedResourcesWizard}<br>
 * in order to let the user synchronize any modified resources in the {@link BigIPConnection} to the Big-IP<br>
 * by calling the {@link ModelObject #iControlRestPatchJob} method of each of the resources.<br>
 * After all selected items are synched to the Big-IP it would reload the connection.
 */
public class SwitchConnectionOnlineCompletion extends RequestCompletion {

    private static Logger logger = Logger.getLogger(SwitchConnectionOnlineCompletion.class);

    private IWorkbenchWindow window;
    private BigIPConnection connection;

    public SwitchConnectionOnlineCompletion(BigIPConnection connection) {
        this.connection = connection;
        window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    }

    @Override
    public void completed(String method, String uri, JsonObject jsonBody) {
        logger.debug("Completed " + method + " " + uri);
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                if (connection.hasEditedChild()) {
                    SyncModifiedResourcesWizard.openWizard(connection, window, true);                    
                } else {
                    logger.debug("No edited children, reload connection " + connection);
                    ExplorerContentProvider.reloadConnection(connection);
                }
            }
        });
    }

    @Override
    public void failed(Exception ex, String method, String uri, String responseBody) {
        if (method == null) {
            logger.warn("No connection to " + connection, ex);
        } else {
            logger.warn("Failed " + method + " " + uri + ", No connection to " + connection, ex);            
        }
        IStatus newStatus = new Status(IStatus.OK, Ids.PLUGIN, Strings.ERROR_CONNECTION_UNREACHABLE_GOING_OFFLINE);
        StatusManager.getManager().handle(newStatus, StatusManager.LOG | StatusManager.SHOW);
        connection.setOnlineMode(false);
        ExplorerContentProvider.loadFromFiles(connection);
        Util.syncWithUi();
    }
}