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

import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import com.f5.irule.model.BigIPConnection;
import com.f5.irule.model.RequestCompletion;
import com.f5.irule.model.ModelObject;
import com.f5.irule.ui.Ids;
import com.f5.irule.ui.Strings;
import com.f5.irule.ui.views.ExplorerContentProvider;
import com.f5.irule.ui.views.Util;
import com.google.gson.JsonObject;

/**
 * A {@link RequestCompletion} that on completion<br>
 * get the {@link BigIPConnection} models contents from the Big-IP by calling<br>
 * the {@link ModelObject #iControlRestGet} method of each model<br>
 * and set them in the local files.
 */
public class SwitchConnectionOfflineCompletion extends RequestCompletion {

    private static Logger logger = Logger.getLogger(SwitchConnectionOfflineCompletion.class);
    
    private BigIPConnection connection;
    
    public SwitchConnectionOfflineCompletion(BigIPConnection connection) {
        this.connection = connection;
    }
    
    @Override
    public void completed(String method, String uri, JsonObject jsonBody) {
        // Iterate over all of the connection writable models (not locally edited, not ReadOnly)
        // and for each one get its content from the Big-IP and set its file with the received content.
        List<ModelObject> writeableModels = connection.getWriteableModels();
        for (ModelObject model : writeableModels) {
            RequestCompletion completion = new SetFileCompletion(model);
            model.iControlRestGetJob(completion, Util.getMutex());
        }
    }
    
    @Override
    public void failed(Exception ex, String method, String uri, String responseBody) {
        // Load the connection models from the project local files
        logger.warn("No connection to " + connection);
        IStatus newStatus = new Status(IStatus.OK, Ids.PLUGIN, Strings.ERROR_CONNECTION_UNREACHABLE_GOING_OFFLINE);
        StatusManager.getManager().handle(newStatus, StatusManager.LOG | StatusManager.SHOW);
        ExplorerContentProvider.loadFromFiles(connection);
        //Util.syncWithUi();
    }
}