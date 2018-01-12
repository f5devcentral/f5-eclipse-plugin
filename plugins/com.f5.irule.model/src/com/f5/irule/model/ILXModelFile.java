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

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.ui.statushandlers.StatusManager;

import com.f5.irule.model.BigIPConnection.Module;
import com.f5.irule.model.jobs.ConnectionJob;
import com.f5.rest.common.RestOperation.RestMethod;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * {@link ModelFile} for non-tcl ILX resource file.<br>
 * Its {@link ModelObject #iControlRestPatch} method synchronously uploads the ILX file to the Big-IP.<br>
 * Its {@link ModelObject #parseContent(JsonObject)} method gets the body of the files element.
 */
public class ILXModelFile extends ModelFile {

    private static Logger logger = Logger.getLogger(ILXModelFile.class);

    private static final String FILE = "file";

    public ILXModelFile(String name, BigIPConnection conn, String partition, Type type, IPath filePath) {
        super(name, conn, partition, type, filePath);
    }

    @Override
    public IStatus iControlRestGet(RequestCompletion completion) {
        BigIPConnection conn = getConnection();
        IPath path = getFilePath();
        ModelObject workspace = findAncestorOfType(ModelObject.Type.WORKSPACE);
        RestURI uri = conn.getURI(BigIPConnection.Module.ilx.name(), RuleProvider.WORKSPACE);
        uri.appendPartitionedOID(getPartition(), workspace.getName());
        IPath filePath = path.removeFirstSegments(3);
        uri.addOption("file", filePath.toString());
        IStatus status = RestFramework.sendRequest(conn, RestMethod.GET, uri.toString(), null, null, completion);
        return status;        
    }
    
    /** 
     * If the workspace of this ilx model was created locally (i.e. does not exist on the Big-IP)
     * then first create the workspace and only after its created post this ilx resource.
     */
    @Override
    public IStatus iControlRestPost(RequestCompletion completion) {

        WorkspaceModelParent workspaceModel = (WorkspaceModelParent) findAncestorOfType(ModelObject.Type.WORKSPACE);
        if (workspaceModel.isLocallyAdded()) {
            logger.debug("Workspace " + workspaceModel +
                " is locally added. Post the workspace first !");
            return workspaceModel.postIlxResource(this, (RequestCompletion) completion);
        } else {
            return postIlxResource(workspaceModel, completion);
        }
    }

    /**
     * Send a POST request for creating an ilx resource,<br>
     * assuming the ilx workspace already exists in the big-ip.
     */
    IStatus postIlxResource(ModelParent workspaceModel, RequestCompletion completion) {
        BigIPConnection conn = getConnection();
        String uri = getPostUri(conn);
        String body = getPostBody(workspaceModel);
        WriteIlxModelCompletion ilxModelCompletion = new WriteIlxModelCompletion(this, (RequestCompletion) completion);
        return RestFramework.sendRequest(conn, RestMethod.POST, uri, null, body, ilxModelCompletion);
    }

    /**
     * Schedule a Job to send a POST request for creating an ilx resource,<br>
     * assuming the ilx workspace already exists in the big-ip.
     */
    void postIlxResourceJob(ModelParent workspaceModel, RequestCompletion completion) {
        BigIPConnection conn = getConnection();
        String uri = getPostUri(conn);
        String body = getPostBody(workspaceModel);
        WriteIlxModelCompletion ilxModelCompletion = new WriteIlxModelCompletion(this, completion);
        RestFramework.sendRequestJob(conn, RestMethod.POST, uri, null, body, ilxModelCompletion, workspaceModel.getFile());
    }

    private String getPostBody(ModelParent workspaceModel) {
        JsonObject json = new JsonObject();
        json.addProperty("name", workspaceModel.getName());
        json.addProperty("partition", getPartition());
        String body = json.toString();
        return body;
    }

    private String getPostUri(BigIPConnection conn) {
        RestURI uri = conn.getURI(Module.ilx.name(), RuleProvider.WORKSPACE);
        IPath filePath = getFilePath();
        IPath fileOption = filePath.removeFirstSegments(3);
        uri.addOption(FILE, fileOption.toString());
        String uriValue = uri.toString();
        return uriValue;
    }
    
    public IStatus iControlRestPatch(RequestCompletion completion) {
        logger.debug("Write ILX " + this);
        IFile file = getFile();
        BigIPConnection connection = getConnection();
        IPath fullPath = file.getFullPath();
        String partition = fullPath.segment(1);
        String workspace = fullPath.segments()[3];  // TODO LAME
        IPath uploadFileCopyPath = fullPath.removeFirstSegments(4); // TODO LAME
        IPath location = file.getLocation();
        String localFilePath = location.toString();
        RestFramework.getInstance().writeILXResource(connection, localFilePath,
            workspace, partition, uploadFileCopyPath.toString(), completion);
        return Status.OK_STATUS;
    }

    @Override
    public IStatus iControlRestDelete(RequestCompletion completion) {
        ModelParent workspaceObj = findAncestorOfType(ModelObject.Type.WORKSPACE);
        ModelObject.Type type = getType();
        if (workspaceObj == null || (type != ModelObject.Type.EXTENSION_FILE && type != ModelObject.Type.ILX_RULE)) {
            IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN, Messages.CANNOT_PERFORM_OPERATION_SELECTED);
            StatusManager.getManager().handle(status, StatusManager.LOG);
            return status;
        }
        BigIPConnection conn = getConnection();
        RestURI uri = conn.getURI(Module.ilx.name(), RuleProvider.WORKSPACE);
        uri.append(workspaceObj.getName());
        uri.addOption(FILE,getFilePath().removeFirstSegments(3).toString());
        
        JsonObject json = new JsonObject();
        json.addProperty("partition", getPartition());
        String body = json.toString();
        return RestFramework.sendRequest(conn, RestMethod.DELETE, uri.toString(), null, body, completion);
    }

    protected IPath getFullPath() {
        Connection connection = getConnection();
        IPath connectionFilePath = connection.getFilePath();
        IPath filePath = getFilePath();
        IPath path = connectionFilePath.append(filePath);
        return path;
    }

    /** 
     * Parse the the files element from the response body.
     * Return the body data of the first non-empty file item
     */
    public String parseContent(JsonObject jsonBody) {
        JsonElement files = RuleProvider.parseElement(jsonBody, "files");
        if (files == null) {
            return super.parseContent(jsonBody);
        }
        if (files.isJsonArray()) {
            for (JsonElement je : files.getAsJsonArray()) {
                ItemData itemData = ItemData.getData(je);
                if (itemData != null && itemData.name != null) {
                    // Create or Update file contents
                    return itemData.body;
                }
            }
        }
        return null;
    }

    public static void parseIlxItem(BigIPConnection conn, String partition, ModelParent parent, IPath path, String name) {
        logger.debug("Parse Ilx Item " + name);
        if (name != null) {
            IPath filePath = path.append(name);
            if (isNodeModulesDir(name, parent.getType())) {
                RuleProvider.getModelParent(name, conn, partition, ModelObject.Type.NODE_MODULES_DIR, filePath, parent);
            }
            else {
                ModelFile file = (ModelFile) parent.getChild(name);
                if (file == null) {
                    file = new ILXModelFile(name, conn, partition, ModelObject.Type.EXTENSION_FILE, filePath);
                    parent.addChild(file);                    
                }
            }
        }
    }

    /**
     * This should test true for any resources found in an ILX extension node_modules directory and below.
     * Once the node_modules folder is expanded via ( ExpandNodeModulesAction )
     * file resources (those without children) will become MODEL_FILE type.
     * Ideally the type change would not be required
     * however, the iControlRest response doesn't differentiate between folders and files,
     * so we don't know which it is until we've tried to read its contents.
     */
    private static boolean isNodeModulesDir(String name, Type type) {
        if ((name.equals("node_modules") && type == ModelObject.Type.EXTENSION)) {
            return true;
        }
        return type == ModelObject.Type.NODE_MODULES_DIR;
    }

    private static class WriteIlxModelCompletion extends RequestCompletion {

        private RequestCompletion externalCompletion;
        private ModelFile model;

        public WriteIlxModelCompletion(ModelFile modelFile, RequestCompletion externalCompletion) {
            this.model = modelFile;
            this.externalCompletion = externalCompletion;
        }

        @Override
        public void completed(String method, String uri, JsonObject jsonBody) {
            logger.debug("Completed " + method + " " + uri);
            IFile file = model.getFile();
            if (file == null) {
                logger.warn("No file for " + model);
            } else {
                if (!file.exists()) {
                    model.processResponse(null, jsonBody, null); // Creates File
                    //model.createFileDontPost(file);
                }
                // Extra write to assure non-zero file length which the rest Framework doesn't like
                // zero length is Ok for LTM/GTM iRules so only do this write for type ModelFile
                IPath fullPath = file.getFullPath();
                IPath location = file.getLocation();
                ModelParent workspaceModel = model.findAncestorOfType(ModelObject.Type.WORKSPACE);
                BigIPConnection conn = (BigIPConnection) ModelRoot.getInstance().findConnection(fullPath);
                ConnectionJob job = new WriteILXResourceJob((BigIPConnection) conn, fullPath, location, externalCompletion, workspaceModel.getFile());
                job.schedule(); // Uploads File                
            }
        }

        @Override
        public void failed(Exception ex, String method, String uri, String body) {
            logger.warn("Failed " + method + " " + uri, ex);
            externalCompletion.failed(ex, method, uri, body);
        }
    }

    private static class WriteILXResourceJob extends ConnectionJob {

        private IPath fullPath;
        private IPath location;

        public WriteILXResourceJob(BigIPConnection connection, IPath fullPath, IPath location,
            RequestCompletion completion, ISchedulingRule mutex) {
            super("WriteILXResource", connection, completion, mutex);
            this.fullPath = fullPath;
            this.location = location;
        }

        @Override
        protected IStatus doRestOperation(RequestCompletion jobCompletion) {
            RestFramework.getInstance().writeILXResource(getConnection(), fullPath, location, jobCompletion);
            return Status.OK_STATUS;
        }
    }

}
