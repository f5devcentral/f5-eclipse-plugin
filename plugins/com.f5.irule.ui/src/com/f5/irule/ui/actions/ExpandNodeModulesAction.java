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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.statushandlers.StatusManager;

import com.f5.irule.model.BigIPConnection;
import com.f5.irule.model.ILXModelFile;
import com.f5.irule.model.ModelFile;
import com.f5.irule.model.ModelObject;
import com.f5.irule.model.ModelParent;
import com.f5.irule.model.ModelUtils;
import com.f5.irule.model.RequestCompletion;
import com.f5.irule.model.RestFramework;
import com.f5.irule.model.RuleProvider;
import com.f5.irule.ui.Ids;
import com.f5.irule.ui.Strings;
import com.f5.irule.ui.views.Util;
import com.f5.rest.common.RestOperation.RestMethod;
import com.google.gson.JsonObject;

public class ExpandNodeModulesAction extends Action {

    private static Logger logger = Logger.getLogger(ExpandNodeModulesAction.class);

    private ModelParent folder;

    public ExpandNodeModulesAction(ModelParent folder) {
        this.folder = folder;
    }

    @Override
    public void run() {
        ModelParent workspace = folder.findAncestorOfType(ModelObject.Type.WORKSPACE);
        ModelParent extension = folder.findAncestorOfType(ModelObject.Type.EXTENSION);
        if (folder.getType() != ModelObject.Type.NODE_MODULES_DIR || workspace == null || extension == null) {
            IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN, Strings.ERROR_LOADING_ILX_CONTENT_FAILED);
            StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.SHOW);
            return;
        }
        
        BigIPConnection connection = folder.getConnection();
        String currentPartition = connection.getCurrentPartition();
        expand(folder, connection, currentPartition, workspace.getName(), extension.getName() + "/" + folder.getName());
        // slash delimiter is per tmsh/icontrol syntax
    }

    private static void expand(ModelParent folder, final BigIPConnection conn, String partition,
            final String workspaceName, final String dirName) {
        logger.debug("Expand " + folder);
        RequestCompletion completion = new ExpandCompletion(conn, partition, folder, workspaceName, dirName);
        String uri = RuleProvider.getIlxWorkspaceUri(conn, conn.getCurrentPartition(), workspaceName, dirName);
        RestFramework.sendRequestJob(conn, RestMethod.GET, uri, null, null, completion, Util.getMutex());
    }
    
    private static class ExpandCompletion extends RequestCompletion {
        
        private BigIPConnection conn;
        private String partition;
        private ModelParent folder;
        private String workspaceName;
        private String dirName;

        ExpandCompletion(BigIPConnection conn, String partition, ModelParent folder, String workspaceName, String dirName) {
            this.conn = conn;
            this.partition = partition;
            this.folder = folder;
            this.workspaceName = workspaceName;
            this.dirName = dirName;
        }

        public void completed(String method, String uri, JsonObject jsonBody) {

            logger.debug("Completed " + method + " " + uri);
            if (RuleProvider.readILXExtension(conn, partition, folder, jsonBody)) {
                // Folder contains children so iterate those and recurse
                ModelObject children[] = folder.getChildren();
                for (int i = 0; i < children.length; i++) {
                    if (children[i] instanceof ModelParent) {
                        // Recurse!
                        ModelParent child = (ModelParent) children[i];
                        String childDir = dirName + "/" + children[i].getName();// delimiter is per tmsh/icontrol syntax
                        expand(child, conn, partition, workspaceName, childDir);
                    }
                }
            }
            else {
                // No children so this "folder" must actually be a file.
                // Remove original ModelParent and then replace with a ModelFile.
                // Ideally the type change would not be required
                // However, the iControlRest response doesn't differentiate between folders and files,
                // so we don't know which it is until we've tried to read its contents.
                String fileName = folder.getName();
                IPath filePath = folder.getFilePath();
                ModelParent parent = folder.getParent();
                parent.removeChild(folder);
                ModelFile file = new ILXModelFile(fileName, conn, partition, ModelObject.Type.EXTENSION_FILE, filePath);
                parent.addChild(file);
            }
            
            // Sync the explorer tree with the newly added objects
            Util.syncWithUi();
        }
        
        public void failed(Exception ex, String method, String uri, String responseBody) {
            ModelUtils.logError(Strings.ERROR_LOADING_ILX_CONTENT_FAILED, StatusManager.LOG, ex);
        }        
    }
}
