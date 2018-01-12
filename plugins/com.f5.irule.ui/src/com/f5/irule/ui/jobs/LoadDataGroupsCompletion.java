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

import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import com.f5.irule.model.BigIPConnection;
import com.f5.irule.model.BigIPConnection.Module;
import com.f5.irule.model.DataGroup;
import com.f5.irule.model.ItemData;
import com.f5.irule.model.RequestCompletion;
import com.f5.irule.model.ModelObject;
import com.f5.irule.model.RuleProvider;
import com.f5.irule.ui.Ids;
import com.f5.irule.ui.Strings;
import com.f5.irule.ui.views.ExplorerContentProvider;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class LoadDataGroupsCompletion extends RequestCompletion {

    private static Logger logger = Logger.getLogger(LoadDataGroupsCompletion.class);

    private BigIPConnection conn;
    private Module module;
    private ExplorerContentProvider provider;

    public LoadDataGroupsCompletion(BigIPConnection conn, Module module, ExplorerContentProvider provider) {
        this.conn = conn;
        this.module = module;
        this.provider = provider;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(getClass().getSimpleName());
        builder.append(" ").append(conn);
        builder.append(" ").append(module);
        builder.append("]");
        return builder.toString();
    }

    public void completed(String method, String uri, JsonObject jsonBody) {
        logger.debug("Completed " + method + " " + uri);
        HashMap<IPath, DataGroup> dataGroups = parseDataGroups(conn, module, jsonBody);
        String logMessage = getLogMessage(dataGroups, method, uri);
        logger.debug(logMessage);
        provider.fillExplorerDataGroups(conn, module, dataGroups);
    }

    private static String getLogMessage(HashMap<IPath, DataGroup> dataGroups, String method, String uri) {
        StringBuilder logMessage = new StringBuilder("Completed ");
        logMessage.append(method).append(" ").append(uri).append(", Load Data-Groups:");
        for (IPath path : dataGroups.keySet()) {
            logMessage.append("\n\t").append(path);
        }
        return logMessage.toString();
    }

    public void failed(Exception ex, String method, String uri, String responseBody) {
        IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN, Strings.ERROR_LOADING_DATA_GROUPS_FAILED, ex);
        StatusManager.getManager().handle(status, StatusManager.LOG);
    }

    /** Parse the response for the Get data-groups request.
     * Return a map of {@link DataGroup} objects, each correspond to an item in the json response. 
     **/
    private HashMap<IPath, DataGroup> parseDataGroups(BigIPConnection conn, Module module, JsonElement jsonBody) {

        ModelObject.Type type = RuleProvider.getDataGroupType(module);
        HashMap<IPath, DataGroup> dataGroups = new HashMap<IPath, DataGroup>();
        List<JsonObject> jsonItemsList = RuleProvider.getJsonItemsList(jsonBody);
        for (JsonObject jsonObject : jsonItemsList) {
            ItemData itemData = ItemData.getData(jsonObject);
            IPath path = ExplorerContentProvider.getModuleFolderPath(itemData.partition, module);
            IPath folder = path.append(DataGroup.FOLDER_NAME);
            DataGroup dataGroup = new DataGroup(itemData.name, itemData.type, conn, itemData.partition, type, module, folder);
            dataGroup.update(jsonObject);
            IPath filePath = dataGroup.getFilePath();
            dataGroups.put(filePath, dataGroup);
        }
        return dataGroups;
    }
}
