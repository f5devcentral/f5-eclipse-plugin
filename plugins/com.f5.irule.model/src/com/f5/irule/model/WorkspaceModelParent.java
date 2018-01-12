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

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.f5.rest.common.RestOperation.RestMethod;
import com.google.gson.JsonObject;

public class WorkspaceModelParent extends ModelParent {

    private static Logger logger = Logger.getLogger(WorkspaceModelParent.class);
    
    List<Object[]> postWaitList = new LinkedList<Object[]>();
    WorkspaceCompletion workspaceCompletion;

    public WorkspaceModelParent(String name, BigIPConnection conn, String partition, IPath filePath) {
        super(name, conn, partition, Type.WORKSPACE, filePath);
        workspaceCompletion = new WorkspaceCompletion(this);
    }

    @Override
    public IStatus iControlRestPost(RequestCompletion completion) {

        BigIPConnection conn = getConnection();
        String uri = RuleProvider.getIlxWorkspaceUri(conn, null, null, null);
        String partition = getPartition();
        String body = RuleProvider.getIlxBody(partition, getName());
        return RestFramework.sendRequest(conn, RestMethod.POST, uri, null, body, completion);
    }

    /**
     * Post an ilx resource after making sure the workspace exists.<br>
     * Synchronize on the Post Wait List and do the following:<br>
     * <ul><li>If this workspace is already created on the Big-Ip then just send a POST request to create the ilx resource.</li>
     * <li>If this workspace locallyAdded flag is true then this workspace was created in Offline mode and doesn't exist on the Big-Ip.<br>
     * In that case add the model and its completion to the Post Wait List.</li></ul>
     * <ul><ul><li>If its the first item in the Wait List then send a Post request to create this workspace on the Big-Ip.</li>
     * <li>If the list already contains waiting models, then a Post request for creating the workspace was already sent.<br>
     * In that case the ilx model would be posted when the workspace creation operation completes.</li></ul></ul>
     */
    public IStatus postIlxResource(ILXModelFile ilxModelFile, RequestCompletion completion) {
        
        synchronized (postWaitList) {
            if (isLocallyAdded()) {
                logger.debug("Add " + ilxModelFile + " to post wait list");
                postWaitList.add(new Object[] { ilxModelFile, completion });
                if (postWaitList.size() == 1) {
                    logger.debug("Post Workspace " + this);
                    IStatus status = iControlRestPost(workspaceCompletion);
                    return status;
                }
            } else {
                ilxModelFile.postIlxResource(this, completion);
            }
        }
        return Status.OK_STATUS;
    }

    /**
     * Synchronously iterate over the ilx models in the post wait list<br>
     * and for each one schedule a Post Job to create the ilx resource.
     */
    private void postIlxModels() {
        synchronized (postWaitList) {
            for (Object[] pair : postWaitList) {
                ILXModelFile ilxModelFile = (ILXModelFile) pair[0];
                RequestCompletion completion = (RequestCompletion) pair[1];
                logger.debug("Schedule Post job for " + ilxModelFile);
                ilxModelFile.postIlxResourceJob(this, completion);
            }
            postWaitList.clear();
        }
    }

    private void failIlxModels(Exception ex, String method, String uri, String responseBody) {
        synchronized (postWaitList) {
            for (Object[] pair : postWaitList) {
                RequestCompletion completion = (RequestCompletion) pair[1];
                completion.failed(ex, method, uri, responseBody);
            }
            postWaitList.clear();
        }
    }
    
    /**
     * On Completed, set the workspace model locallyAdded flag to false
     */
    private static class WorkspaceCompletion extends RequestCompletion {

        private WorkspaceModelParent workspaceModelParent;

        private WorkspaceCompletion(WorkspaceModelParent workspaceModelParent) {
            this.workspaceModelParent = workspaceModelParent;
        }

        @Override
        public void completed(String method, String uri, JsonObject responseBody) {
            logger.debug("Completed " + method + " " + uri);
            IFile file = workspaceModelParent.getFile();
            PersistentPropertiesUtil.updateModificationStampMap(file, workspaceModelParent.getConnection());
            workspaceModelParent.setLocallyAdded(false);
            workspaceModelParent.postIlxModels();
        }

        @Override
        public void failed(Exception ex, String method, String uri, String responseBody) {
            logger.warn("Failed " + method + " " + uri, ex);
            workspaceModelParent.failIlxModels(ex, method, uri, responseBody);
        }
    }

}
