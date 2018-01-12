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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

import com.f5.irule.model.jobs.ConnectionJob;
import com.f5.irule.model.jobs.RestDeleteJob;
import com.f5.irule.model.jobs.RestGetJob;
import com.f5.irule.model.jobs.RestPatchJob;
import com.f5.irule.model.jobs.RestPostJob;
import com.f5.rest.common.RestOperation;
import com.google.gson.JsonObject;

public abstract class ModelObject implements IAdaptable {

    private static Logger logger = Logger.getLogger(ModelObject.class);

    private String name = "";
    private String partition = "";
    private ModelParent parent;
    private LinkedList<ModelListener> listeners = new LinkedList<ModelListener>();
    private Type type = Type.UNKNOWN;
    private IPath filePath;
    private BigIPConnection connection;
    private boolean readOnly = false;

    /**
     * A flag indicating if the content in this file model
     * was just received from a REST response.
     */
    private boolean contentFromResponse;

    /**
     * A flag indicating if the content was modified locally only,
     * i.e. not in sync with the Big-IP
     */
    private boolean locallyModified;

    /**
     * A flag indicating if the content was created locally and hasn't been sync'ed to the Big-IP
     */
    private boolean locallyAdded;

    public ModelObject(String name, BigIPConnection conn, String partition, Type type, IPath filePath) {
        this.name = name;
        this.connection = conn;
        this.type = type;
        this.partition = partition;
        this.filePath = filePath;
        setLocallyFlags();
    }

    /**
     * Set this model locallyModified and locallyAdded flags<br>
     * by comparing the model file resource time stamp<br>
     * with the resource response time stamp that is cached in the project persistent properties.
     */
    public void setLocallyFlags() {
        if (filePath != null && connection != null) {
            locallyModified = PersistentPropertiesUtil.isFileModified(connection, filePath);
            if (fileExists()) {
                locallyAdded = PersistentPropertiesUtil.isNewFile(connection, filePath);
            } else {
                locallyAdded = false;
            }
        }
    }

    public abstract IStatus iControlRestDelete(RequestCompletion completion);
    public abstract IStatus iControlRestPost(RequestCompletion completion);
    public abstract IStatus iControlRestGet(RequestCompletion completion);
    public abstract IStatus iControlRestPatch(RequestCompletion completion);

    /**
     * Parse the String content from the response
     */
    public abstract String parseContent(JsonObject jsonBody);
    
    /**
     * Set this model file content and any other related variable
     * according to the String content
     */
    public abstract void setResponseContents(IFile file, String content);

    public IPath getFilePath() {
        return filePath;
    }

    /**
     * Return true if the model is locally modified (not in sync with the Big-IP)
     */
    public boolean isLocallyModified() {
        return locallyModified;
    }

    public void setLocallyModified(boolean modified) {
        logger.debug("Set locallyModified of " + this + " to " + modified);
        this.locallyModified = modified;
    }

    public boolean isLocallyAdded() {
        boolean ans = locallyAdded;
        return ans;
    }

    public void setLocallyAdded(boolean locallyAdded) {
        logger.debug("Set setLocallyAdded of " + this + " to " + locallyAdded);
        this.locallyAdded = locallyAdded;
    }

    public boolean isLocallyEdited() {
        return locallyModified || locallyAdded;
    }

    /**
     * Return the {@link IFile} of this model {@link IPath}. 
     */
    public IFile getFile() {
        IProject project = connection.getProject();
        IFile file = project.getFile(filePath.toString());
        return file;
    }

    protected IPath getLocation() {
        IFile file = getFile();
        IPath location = file.getLocation();
        return location;
    }

    public BigIPConnection getConnection() {
        return connection;
    }

    public void setConnection(BigIPConnection conn) {
        this.connection = conn;
    }
    
    public String getPartition() {
        return partition;
    }

    public enum Type {
        CONNECTION,
        DIRECTORY,
        EXTENSION,
        EXTENSION_DIR,
        EXTENSION_FILE,
        GTM_RULE,
        GTM_DATA_GROUP,
        ILX_RULE,
        LTM_RULE,
        LTM_DATA_GROUP,
        IRULE_DIR_GTM,
        IRULE_DIR_ILX,
        IRULE_DIR_LTM,
        IAPPLX_DIR,
        IAPPLX_MODEL,
        IAPPLX_MODEL_PACKAGE,
        IAPPLX_MODEL_DIR,
        DATA_GROUP_MODEL,
        NODE_MODULES_DIR,
        ROOT,
        UNKNOWN,
        WORKSPACE,
        WORKSPACE_DIR
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void addListener(ModelListener listener) {
        listeners.add(listener);
    }

    public List<ModelListener> getListeners() {
        return new ArrayList<ModelListener>(listeners);
    }

    public String getName() {
        return name;
    }

    public void setParent(ModelParent parent) {
        this.parent = parent;
    }

    public ModelParent getParent() {
        return parent;
    }
    
    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String toString() {
        return getName();
    }

    @SuppressWarnings("unchecked")
    public Object getAdapter(@SuppressWarnings("rawtypes") Class key) {
        return null;
    }

    /**
     * Find the ancestor of this object which has the specified type
     * Returns null if the type isn't found in the object's ancestry
     */
    public ModelParent findAncestorOfType(ModelObject.Type type) {
        return recurseAncestorOfType(this, type);
    }

    private ModelParent recurseAncestorOfType(ModelObject obj, ModelObject.Type type) {
        ModelParent parent = obj.getParent();
        if (parent == null) {
            return null;
        }
        if (parent.getType() == type) {
            return parent;
        }
        return recurseAncestorOfType(parent, type);
    }
    
    /**
     * Get the project based on the connection.  If no connection was set, return null.
     */
    public IProject getProject() {
        Connection conn = getConnection();
        if (conn != null) {
            String project = conn.getName();
            if (project != null) {
                IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(project);
                return proj;
            }
        }
        return null;        
    }

    /**
     * Find the resource associated with this ModelObject based on the filePath.
     * If no connection or filePath was specified,
     * or a resource just doesn't exist to be found, return null
     */
    public IResource findResource() {
        IResource resource = null;
        IPath path = getFilePath();
        IProject proj = getProject();
        if (path != null && proj != null) {
            resource = proj.findMember(path.makeAbsolute());    
        }
        return resource;
    }

    /*
     * Call after the child has been added to the model
     */
    protected void listenerUpdateChild(ModelObject child) {
        for (ModelListener listener : getListeners()) {
            try {
                listener.childAdded(this, child);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * Schedule a job that executes the {@link ModelObject #iControlRestGet(RequestCompletion)} method
     */
    public void iControlRestGetJob(RequestCompletion completion, ISchedulingRule mutex) {
        BigIPConnection conn = (BigIPConnection) getConnection();
        ConnectionJob job = new RestGetJob(this, conn, completion, mutex);
        job.schedule();
    }

    /*
     * Schedule a job that executes the {@link ModelObject #iControlRestPost(RequestCompletion)} method
     */
    public void iControlRestPostJob(RequestCompletion completion, ISchedulingRule mutex) {
        BigIPConnection conn = (BigIPConnection) getConnection();
        ConnectionJob job = new RestPostJob(this, conn, completion, mutex);
        job.schedule();
    }

    /*
     * Schedule a job that executes the {@link ModelObject #iControlRestPatch(RequestCompletion)} method
     */
    public void iControlRestPatchJob(RequestCompletion completion, ISchedulingRule mutex) {

        IFile file = getFile();
        long modificationStamp = file.getModificationStamp();
        long contentModificationStamp = PersistentPropertiesUtil.getResponseTimeStampLong(connection, filePath);
        // Safety check - Validate that the model file content does not have the time stamp of the last response from the Big-IP.
        // If it did, it signifies some kind of a loop: a response received from the Big-IP is patched back to the Big-IP...
        if (contentModificationStamp == modificationStamp) {
            logger.warn("Big-IP already have content of " + this);
        } else {
            BigIPConnection conn = (BigIPConnection) getConnection();
            ConnectionJob job = new RestPatchJob(this, conn, completion, mutex);
            job.schedule();
        }
        
    }
    /**
     * Schedule a job that executes the {@link ModelObject #iControlRestDelete(RequestCompletion)} method
     */
    public void iControlRestDeleteJob(RequestCompletion completion, ISchedulingRule mutex) {
        BigIPConnection conn = (BigIPConnection) getConnection();
        ConnectionJob job = new RestDeleteJob(this, conn, completion, mutex);
        job.schedule();
    }

    public boolean isContentFromResponse() {
        return contentFromResponse;
    }
 
    /**
     * Set a Job to process the response body<br>
     * Write it to the model local file, update its persistent modification stamp and<br>
     * set the model locallyModified and the locallyAdded flags to false.<br>
     * In case the external Runnable finishRunnable exists<br>
     * it will execute after finishing processing the model.
     */
    public final void processResponse(String uri, JsonObject jsonBody, Runnable finishRunnable) {
        logger.trace("Schecule Processing " + (uri == null ? this : "Response " + uri));
        Job job = new ProcessResponseJob(this, uri, jsonBody, finishRunnable);
        // Processing the response involves writing to a local file
        // The Job ISchedulingRule is set to the model project
        // so two models in the same project would sync on file system modifications.
        IProject project = getProject();
        logger.trace("Set Rule " + project + " to " + job);
        job.setRule(project);
        job.schedule();
    }
    
    /**
     * A {@link Job} that set the content from the response on the model file<br>
     * Updates its persistent modification stamp and<br>
     * set the model locallyModified and the locallyAdded flags to false.<br>
     * In case the external Runnable finishRunnable exists<br>
     * it executes it after finishing processing the model.
     */
    private static class ProcessResponseJob extends Job {
        private ModelObject model;
        private String uri;
        private JsonObject jsonBody;
        private Runnable finishRunnable;
        public ProcessResponseJob(ModelObject model, String uri, JsonObject jsonBody, Runnable finishRunnable) {
            super(Ids.PROCESS_RESPONSE);
            this.model = model;
            this.uri = uri;
            this.jsonBody = jsonBody;
            this.finishRunnable = finishRunnable;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            doProcessResponse(uri, jsonBody);
            if (finishRunnable != null) {
                finishRunnable.run();
            }
            return Status.OK_STATUS;
        }

        /**
         * Parse the String content from the {@link RestOperation} response and set this model file.<br>
         * In case this model is a folder and not a file, create the folder hierarchy of the folder.<br>
         * Update this model resource modification stamp in the project persistent properties.<br>
         * Set the {@link #locallyModified} and the {@link #locallyAdded} flags to false. 
         */
        private void doProcessResponse(String uri, JsonObject jsonBody) {
            logger.debug("Process " + (uri == null ? this : "Response " + uri));
            // Set the contentFromResponse flag to true before calling to the setResponseLocally()
            // so the DeltaModelFileVisitor.ignoreModel() will return true on this model
            // and won't trigger an update to the Big-IP
            model.contentFromResponse = true;
            IResource resource = model.setResponseLocally(jsonBody);
            PersistentPropertiesUtil.updateModificationStampMap(resource, model.connection);
            model.contentFromResponse = false;
            model.locallyModified = false;
            model.locallyAdded = false;
        }
        
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("[").append(getClass().getSimpleName());
            builder.append(" ").append(model);
            builder.append(" ").append(uri);
            builder.append("]");
            return builder.toString();
        }
    }

    /**
     * Parse the String content from the response<br>
     * and set this model file content and any other related variable with the response content
     * @return this model {@link IFile} path
     */
    protected IResource setResponseLocally(JsonObject jsonBody) {
        final IFile file = getFile();
        if (jsonBody == null) {
            return file ;
        }

        final String content = parseContent(jsonBody);
        if (content != null) {
            setResponseContents(file, content);
        }
        return file;
    }

    public static InputStream getFileContents(ModelObject modelObject) throws CoreException {
        IProject proj = modelObject.getProject();
        IPath filePath = modelObject.getFilePath();
        return getFileContents(proj, filePath);
    }
    
    public static InputStream getFileContents(IProject proj, IPath filePath) throws CoreException {
        IFile file = proj.getFile(filePath.toString());
        InputStream fileContents = file.getContents();
        return fileContents;
    }

    /**
     * Return true if the {@link IFile} corresponding to this model file Path exists. 
     */
    protected boolean fileExists() {
        IFile file = getFile();
        boolean exists = file.exists();
        return exists;
    }

    /**
     * @return true if this model is expecting a JSON response for the REST operations.
     */
    public boolean isJsonResponse() {
        return true;
    }
}
