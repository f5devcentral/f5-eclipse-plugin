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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.f5.rest.common.RestOperation.RestMethod;
import com.google.gson.JsonObject;

public class ModelParent extends ModelObject {

    private static Logger logger = Logger.getLogger(ModelParent.class);

    private ArrayList<ModelObject> children;

    public ModelParent(String name, BigIPConnection conn, String partition, Type type, IPath filePath) {
        super(name, conn, partition, type, filePath);
        children = new ArrayList<ModelObject>();
    }

    public void addChild(ModelObject child) {
        ModelObject existingChild = getChild(child.getName());
        if (existingChild != null){
            logger.warn(this + " already had child " + existingChild);
            removeChild(existingChild);
        }
        children.add(child);
        child.setParent(this);
        listenerUpdateChild(child);
    }

    public void removeChild(ModelObject child) {
        children.remove(child);
        child.setParent(null);
        listenerUpdateChild(child);
    }

    public ModelObject[] getChildren() {
        return (ModelObject[]) children.toArray(new ModelObject[children.size()]);
    }

    public boolean hasChildren() {
        return children.size() > 0;
    }

    /**
     * Recursively Clear the children of this model parent.<br>
     * In case a {@link ModelFilter} is provided, only clear the children that are accepted by the filter.
     */
    public List<ModelObject> clearChildren(ModelFilter filter) {
        List<ModelObject> removableList = new LinkedList<ModelObject>();
        for (ModelObject child : getChildren()) {
            if (filter != null && !filter.applyModel(child)) {
                continue;
            }
            if (child instanceof ModelParent) {
                ModelParent childParent = (ModelParent) child;
                childParent.clearChildren(filter);
            }
            removableList.add(child);
        }
        for (ModelObject removableModel : removableList) {
            children.remove(removableModel);
        }
        return removableList;
    }

    /**
     * Returns child object with specified name or null if not found
     */
    public ModelObject getChild(String name) {
        for (ModelObject o : children) {
            if (o.getName().equals(name)) {
                return o;
            }
        }
        return null;
    }
    
    public IFolder getFolder() {
        IProject project = getConnection().getProject();
        IPath filePath = getFilePath();
        IFolder folder = project.getFolder(filePath.toString());
        return folder;
    }

    /**
     * Iterate over this model descendants and find
     * the {@link ModelObject} with the given name or filepath location
     */
    public ModelObject getModel(String name, IPath location) {
        for (ModelObject o : children) {
            IPath filePath = o.getFilePath();
            if (location == null) {
                if (o.getName().equals(name)) {
                    return o;
                }                
            } else {
                if (filePath != null) {
                    if (filePath.equals(location)) {
                        return o;
                    }
                }
            }
            if (o instanceof ModelParent) {
                ModelParent child = (ModelParent) o;
                ModelObject childDescendant = child.getModel(name, location);
                if (childDescendant != null) {
                    return childDescendant;
                }
            }
        }
        return null;
    }

    @Override
    public IStatus iControlRestGet(RequestCompletion completion) {
        throw new UnsupportedOperationException("iControlRestGet not implemented for " + getClass());
    }

    @Override
    public IStatus iControlRestPatch(RequestCompletion completion) {
        throw new UnsupportedOperationException("iControlRestPatch(completion) not implemented for " + getClass());
    }

    @Override
    public String parseContent(JsonObject jsonBody) {
        throw new UnsupportedOperationException("getContent not implemented for " + getClass());
    }

    @Override
    public void setResponseContents(IFile file, String content) {
        throw new UnsupportedOperationException("setResponseContents not implemented for " + getClass());
    }

    /** 
     * Overrides the parent ModelObject.setResponseLocally(...) method.
     * Instead it creates the folder hierarchy of this model IFolder folder
     */
    @Override
    protected IResource setResponseLocally(JsonObject jsonBody) {
        IFolder folder = getFolder();
        ModelUtils.prepareFolder(folder, isContentFromResponse());
        return folder;
    }

    @Override
    public IStatus iControlRestDelete(RequestCompletion completion) {

        BigIPConnection conn = getConnection();
        String body, uri;
        ModelObject.Type type = getType();
        switch (type) {
        case CONNECTION:
            // Nothing to do on the bigip
            return Status.OK_STATUS;
        case EXTENSION:
            uri = RuleProvider.getIlxWorkspaceUri(conn, null, getWorkspaceName(), getName());
            body = RuleProvider.getIlxBody(getPartition(), null);
            break;
        case WORKSPACE:
            uri = RuleProvider.getIlxWorkspaceUri(conn, null, getName(), null);
            body = RuleProvider.getIlxBody(getPartition(), null);
            break;
        case IAPPLX_MODEL_DIR:
            uri = getIappUri(conn, RestFramework.IAPP_DIRECTORY_MANAGEMENT_RECURSIVE, getFilePath());
            body = null;
            break;
        default:
            logger.warn("Unexpected type " + type + " for model " + this);
            return Status.CANCEL_STATUS;
        }

        IStatus status = RestFramework.sendRequest(conn, RestMethod.DELETE, uri, null, body, completion);
        return status;
    }

    private static String getIappUri(BigIPConnection conn, String apiPath, IPath filePath) {

        IPath path = filePath.removeFirstSegments(1);
        String remotePath = path.toString();
        String endpoint = apiPath + "/" + remotePath;
        RestURI uri = conn.getURI(endpoint);
        return uri.toString();
    }

    @Override
    public IStatus iControlRestPost(RequestCompletion completion) {
        
        String uri;
        String body = null;
        BigIPConnection conn = getConnection();
        String partition = getPartition();
        ModelObject.Type type = getType();
        switch (type) {
        case CONNECTION:
        case NODE_MODULES_DIR:
            // Nothing to do on the bigip
            return Status.OK_STATUS;
        case EXTENSION:
            uri = RuleProvider.getIlxWorkspaceUri(conn, null, null, getName());
            ModelParent workspaceObj = findAncestorOfType(ModelObject.Type.WORKSPACE);
            body = RuleProvider.getIlxBody(partition, workspaceObj == null ? "" : workspaceObj.getName());
            break;
        case WORKSPACE:
            uri = RuleProvider.getIlxWorkspaceUri(conn, null, null, null);
            body = RuleProvider.getIlxBody(partition, getName());
            break;
        case IAPPLX_MODEL_DIR:
            uri = getIappUri(conn, RestFramework.IAPP_DIRECTORY_MANAGEMENT, getFilePath());
            body = "{}";
            break;
        default:
            logger.warn("Unexpected type " + type + " for model " + this);
            return Status.OK_STATUS;
        }

        return RestFramework.sendRequest(conn, RestMethod.POST, uri, null, body, completion);
    }
    
    private String getWorkspaceName() {
        ModelParent workspaceObj = findAncestorOfType(ModelObject.Type.WORKSPACE);
        if (workspaceObj != null) {
            String workspaceName = workspaceObj.getName();
            return workspaceName;
        }
        return null;
    }

    @Override
    protected boolean fileExists() {
        IFolder folder = getFolder();
        boolean exists = folder.exists();
        return exists;
    }
}
