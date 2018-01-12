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
package com.f5.irule.ui.actions;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.f5.irule.model.BigIPConnection;
import com.f5.irule.model.RequestCompletion;
import com.f5.irule.model.RestFramework;
import com.f5.irule.model.RestURI;
import com.f5.irule.ui.Strings;
import com.f5.irule.ui.jobs.SwitchConnectionOfflineCompletion;
import com.f5.irule.ui.jobs.SwitchConnectionOnlineCompletion;
import com.f5.irule.ui.views.ExplorerContentProvider;
import com.f5.irule.ui.views.Util;
import com.f5.rest.common.RestOperation.RestMethod;

/**
 * This {@link Action} toggles the Offline/Online mode of the {@link BigIPConnection}<br><br>
 * - When Switching to Offline Mode:<br>
 * If Big-IP is connected then get this connection models contents
 * from the Big-IP and set them in the local files.<br>
 * If not connected then load the connection models from the project local files.<br><br>
 * 
 * - When Switching to Online Mode:<br>
 * For each changed resources, Update the BigIP about the model changed content.
 */
public class SwitchModeAction extends Action {

    private static Logger logger = Logger.getLogger(SwitchModeAction.class);

    private BigIPConnection connection;

    public SwitchModeAction(BigIPConnection connection) {
        this.connection = connection;
        boolean onlineMode = connection.isOnlineMode();
        this.setText(onlineMode ? Strings.LABEL_OFFLINE : Strings.LABEL_ONLINE);
        String imageName = onlineMode ? ISharedImages.IMG_TOOL_UNDO : ISharedImages.IMG_TOOL_REDO;
        Image deleteImage = PlatformUI.getWorkbench().getSharedImages().getImage(imageName);
        this.setImageDescriptor(ImageDescriptor.createFromImage(deleteImage));
        logger.debug("Initiated " + this);
    }

    public void run() {
        boolean newMode = !connection.isOnlineMode();
        RestURI uri = connection.getURI(ExplorerContentProvider.SYS, ExplorerContentProvider.VERSION);
        ISchedulingRule mutex = Util.getMutex();
        if (newMode) {
            logger.debug("Switch " + connection + " to Online Mode");
            // When switching to Online Mode the online flag changes only after
            // the user press complete on the SyncModifiedResourcesWizard wizard
            RequestCompletion completion = new SwitchConnectionOnlineCompletion(connection);
            RestFramework.sendRequestJob(connection, RestMethod.GET, uri.toString(),
                null, null, completion, mutex);
        }
        else{
            connection.setOnlineMode(false);
            logger.debug("Switch " + connection + " to Offline Mode");
            RequestCompletion completion = new SwitchConnectionOfflineCompletion(connection);
            RestFramework.sendRequestJob(connection, RestMethod.GET, uri.toString(),
                null, null, completion, mutex);
        }
        //Util.syncWithUi();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(getClass().getSimpleName()).append(" ");
        builder.append(connection);
        builder.append("]");
        return builder.toString();
    }

}
