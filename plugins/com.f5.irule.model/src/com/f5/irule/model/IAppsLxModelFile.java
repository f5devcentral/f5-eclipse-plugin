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

import java.net.URISyntaxException;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import com.f5.rest.common.RestOperation;
import com.f5.rest.common.RestOperation.RestMethod;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * {@link ModelFile} for IappsLx resource file.<br>
 * 
 * Its {@link ModelObject #iControlRestGet(RequestCompletion)}<br>
 * and {@link ModelObject #iControlRestDelete(RequestCompletion)} methods<br>
 * use the <b>/mgmt/shared/iapp/file-management</b> for the get/delete operations.<br>
 * Its {@link ModelObject #iControlRestPatch} method uses<br>
 * the <b>/mgmt/shared/iapp/file-management</b> REST api to upload the file resource to the Big-Ip.<br>
 * In case the file parent directory was created locally by the user
 * then first it creates the directory on the Big-Ip<br>
 * by using the <b>mgmt/shared/iapp/directory-management-recursive</b> REST api
 */
public class IAppsLxModelFile extends ModelFile {

    private static Logger logger = Logger.getLogger(IAppsLxModelFile.class);
    private final static String IAPP_PRESENTATION_FOLDER = "presentation";

    private IPath remotePath;

    public IAppsLxModelFile(String name, BigIPConnection conn, Type type, IPath remotePath, IPath filePath) {
        super(name, conn, null, type, filePath);
        this.remotePath = remotePath;
    }

    /**
     * Override super method.
     * return false since the response is plain file text
     */
    public boolean isJsonResponse() {
        return false;
    }

    /** 
     * The response for the IappLx REST file-download request
     * is not a json object, but the actual requested file.
     * No need to retrieve the body from a json element but just return the response body.
     */
    public String parseContent(RestOperation response, String body) {
        if (body == null) {
            byte[] binaryBody = response.getBinaryBody();
            body = new String(binaryBody);
        }
        return body;
    }

    /** 
     * Unwrap the text response body from the json object
     * that was created in the RestRequestCompletionBridge.doCompleted(...) method
     */
    @Override
    public String parseContent(JsonObject jsonBody) {
        JsonElement textElement = jsonBody.get(RestRequestCompletionBridge.TEXT);
        String content = textElement == null ? null : textElement.getAsString();
        return content;
    }

    @Override
	public IStatus iControlRestGet(RequestCompletion completion) {
        BigIPConnection conn = getConnection();
        RestURI uri = RestUri(conn, RestFramework.IAPP_FILE_MANAGEMENT, remotePath);
        return RestFramework.sendRequest(conn, RestMethod.GET, uri.toString(), null, null, completion);
	}

    @Override
    public IStatus iControlRestDelete(RequestCompletion completion) {
        BigIPConnection conn = getConnection();
        RestURI uri = RestUri(conn, RestFramework.IAPP_FILE_MANAGEMENT, remotePath);
        return RestFramework.sendRequest(conn, RestMethod.DELETE, uri.toString(), null, null, completion);
    }

    private static RestURI RestUri(BigIPConnection conn, String apiPath, IPath remotePath) {
        String endpoint = apiPath + "/" + remotePath;
        RestURI uri = conn.getURI(endpoint);
        return uri;
    }

    @Override
    public IStatus iControlRestPost(RequestCompletion completion) {
        return iControlRestPatch(completion);
    }

    public IStatus iControlRestPatch(RequestCompletion finalCompletion) {
        // Wrap the external completion with a RestartRestnodedCompletion
        // so if the resource patching completes successfully
        // it would send a REST request to restart the restnoded service.
        RequestCompletion restartCompletion = new RestartRestnodedCompletion(this, finalCompletion);
        BigIPConnection conn = getConnection();
        String localFilePath = getFile().getLocation().toString();
        ModelParent parent = getParent();
        boolean parentLocallyAdded = parent.isLocallyAdded();
        if (parentLocallyAdded) {
            // If the parent LocallyAdded flag is true
            // then the file parent directory was also created locally by the user.
            // In that case first, use the mgmt/shared/iapp/directory-management-recursive REST api
            // to create the resource parent directory on the Big-Ip.
            // When completed, upload the iAppsLx file resource to the Big-IP
            // using the /mgmt/shared/iapp/file-management api.
            IPath parentRemotePath = remotePath.removeLastSegments(1);
            RestURI uri = RestUri(conn, RestFramework.IAPP_DIRECTORY_MANAGEMENT_RECURSIVE, parentRemotePath);
            RequestCompletion completion = new MkdirCompletion(conn, localFilePath, remotePath, restartCompletion);
            return RestFramework.sendRequest(conn, RestMethod.POST, uri.toString(), null, "{}", completion);
        } else {
            return writeIappLXResource(conn, localFilePath, remotePath, restartCompletion);
        }
    }
    
    /**
     * On completed() schedule restart operation on the Big-Ip<br>
     * and then delegate response processing to the external final completion 
     */
    private static class RestartRestnodedCompletion extends RequestCompletion {
        private IAppsLxModelFile model;
        private RequestCompletion finalCompletion;
        private RestartRestnodedCompletion(IAppsLxModelFile model, RequestCompletion finalCompletion) {
            this.model = model;
            this.finalCompletion = finalCompletion;
        }
        public void completed(String method, String uri, JsonObject responseBody) {
            IPath path = model.getFilePath();
            if (!(path.segmentCount() > 2 && path.segment(2).equals(IAPP_PRESENTATION_FOLDER))) {
                // If the top level iApp directory is NOT the presentation dir, restart the service
                model.restartRestnoded();                
            }
            finalCompletion.completed(method, uri, responseBody);
        }
        public void failed(Exception ex, String method, String uri, String responseBody) {
            finalCompletion.failed(ex, method, uri, responseBody);
        }
    }

    /**
     * Send a REST request to the Big-ip to restart the restnoded service.
     */
    private void restartRestnoded() {
        // Finished updating Big-Ip with some file. Need to restart restnoded.
        BigIPConnection conn = getConnection();
        logger.debug("Restart restnoded on " + conn);
        IStatus status = new Status(IStatus.INFO, Ids.PLUGIN, Messages.REQUESTED_RESTART_OF_RESTNODED_SERVICE);
        StatusManager.getManager().handle(status, StatusManager.LOG);
        
        String uri = conn.getURI(RestFramework.MGMT_TM_SYS_SERVICE).toString();        
        JsonObject restartJson = new JsonObject();
        restartJson.addProperty("command", "restart");
        restartJson.addProperty("name", "restnoded");
        String body = restartJson.toString();
        
        // Send the restart request on the same thread.
        // Since this is a side task, the plugin updates the UI tree prior to receiving the response for this request.
        // If the RestFramework.sendRequestJob would be used then it would use another SendRequestJob job to send the request
        // and the UI connection label would remain with the '(Loading)' suffix.
        RestFramework.sendRequest(conn, RestMethod.POST, uri, "application/json", body, new LogMessageCompletion());
    }
    
    /**
     * {@link RequestCompletion} that logs a restnoded successful/failed message to the plug-in UI<br>
     * when the restart restnoded request execution completes
     */
    private static class LogMessageCompletion extends RequestCompletion {
        @Override
        public void completed(String method, String uri, JsonObject responseBody) {
            logger.debug("Completed " + method + " " + uri + " response:\n" + responseBody);
            /* String commandResult = responseBody.get("commandResult").getAsString();
            IStatus status = new Status(IStatus.INFO, Ids.PLUGIN, Messages.SUCCESSFUL_RESTNODED_SERVICE_RESULT + "\n" + commandResult);
            StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);*/
        }
        @Override
        public void failed(Exception ex, String method, String uri, String responseBody) {
            logger.warn("Failed " + method + " " + uri + " response:\n" + responseBody, ex);
            IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN, Messages.FAILED_RESTNODED_SERVICE, ex);
            StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
        }        
    }

    /**
     * Synchronously upload the iAppsLx resource to the Big-IP.<br>
     * Use the /mgmt/shared/iapp/file-management upload REST api to upload the file.
     */
    private static IStatus writeIappLXResource(BigIPConnection conn, String localFilePath,
            IPath remotePath, RequestCompletion finalCompletion) {
        try {
            RestFramework.getInstance().writeIappLXResource(conn, localFilePath, remotePath, finalCompletion);
            return Status.OK_STATUS;
        } catch (URISyntaxException ex) {
            logger.warn("Failed to write contents of " + localFilePath, ex);
            String message = Messages.FAILED_TO_RETRIEVE_FILE;
            IStatus status = RestRule.handleError(message, ex);
            return status;
        }
    }
    
    private static class MkdirCompletion extends RequestCompletion {

        private BigIPConnection conn;
        private String localFilePath;
        private IPath remotePath;
        private RequestCompletion finalCompletion;

        private MkdirCompletion(BigIPConnection conn, String localFilePath, IPath remotePath, RequestCompletion finalCompletion) {
            this.conn = conn;
            this.localFilePath = localFilePath;
            this.remotePath = remotePath;
            this.finalCompletion = finalCompletion;
        }

        @Override
        public void completed(String method, String uri, JsonObject jsonBody) {
            logger.debug("Completed " + method + " " + uri);
            writeIappLXResource(conn, localFilePath, remotePath, finalCompletion);
        }

        @Override
        public void failed(Exception ex, String method, String uri, String responseBody) {
            logger.warn("Failed " + method + " " + uri, ex);
        }
    }
}
