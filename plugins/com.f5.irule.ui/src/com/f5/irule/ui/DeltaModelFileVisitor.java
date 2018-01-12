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
package com.f5.irule.ui;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.ui.IWorkbenchPage;

import com.f5.irule.model.BigIPConnection;
import com.f5.irule.model.DataGroup;
import com.f5.irule.model.IAppsLxModelFile;
import com.f5.irule.model.ILXModelFile;
import com.f5.irule.model.ILXRuleModelFile;
import com.f5.irule.model.ModelObject;
import com.f5.irule.model.ModelObject.Type;
import com.f5.irule.model.ModelParent;
import com.f5.irule.model.ModelRoot;
import com.f5.irule.model.ModelUtils;
import com.f5.irule.model.PersistentPropertiesUtil;
import com.f5.irule.model.RequestCompletion;
import com.f5.irule.model.RestRule;
import com.f5.irule.ui.views.ProcessResponseJobCompletion;
import com.f5.irule.ui.views.Util;
import com.f5.irule.ui.wizards.PatchCompletion;
import com.f5.rest.common.RestRequestCompletion;

/**
 * {@link IResourceDeltaVisitor} that each time a resource is changed, it checks the conditions for the change operation<br>
 * and if Big-IP should be updated, then it calls the {@link ModelObject #iControlRestPatch(RestRequestCompletion)} method<br>
 * with the {@link PatchCompletion} that on completed,<br>
 * sets the response content on the model local file and sync the UI view.
 */
public class DeltaModelFileVisitor implements IResourceDeltaVisitor {

    private IWorkbenchPage page;
    private IResourceDelta lastChange;

    DeltaModelFileVisitor(IWorkbenchPage page) {
        this.page = page;
    }

    private static Logger logger = Logger.getLogger(DeltaModelFileVisitor.class);

    @Override
    public boolean visit(IResourceDelta change) throws CoreException {
        lastChange = change;
        return true;
    }

    /**
     * Get the {@link ModelObject} of the changed {@link IResourceDelta}<br>
     * If the model has to be updated on the Big-ip call for the relevant update method.
     */
    boolean processLastChange() {
        ModelObject model = getModel(lastChange);
        if (model == null) {
            return true;
        }

        IResource resource = lastChange.getResource();
        String kindString = getKindString(lastChange);
        if (!resource.exists()) {
            logger.debug("Resource Deleted " + lastChange + " kind: " + kindString);
            return true;
        }
        
        if (ModelUtils.contentFromResponse(resource)) {
            logger.trace("Content from Response " + resource);
            return true;
        }

        Type type = model.getType();
        long modificationStamp = resource.getModificationStamp();
        logger.debug("Resource " + kindString + " " + type + " " + lastChange + " ModificationStamp=" + modificationStamp);            

        if (ignoreModel(model)) {
            Util.syncWithUi();
            return true;
        }

        if (model instanceof ILXModelFile ||
                model instanceof RestRule ||
                model instanceof DataGroup ||
                model instanceof ILXRuleModelFile ||
                model instanceof IAppsLxModelFile) {
            modelUpdate(model, page);
            return true;
        }

        switch (type) {
        case IAPPLX_MODEL_DIR:
            ModelParent modelParent = (ModelParent) model;
            ModelObject[] children = modelParent.getChildren();
            if (children.length == 0) {
                modelUpdate(model, page);
            } else {
                // Ignore
            }
            break;

        default:
            break;
        }
        logger.trace("Ignore " + type + " " + model.getClass().getSimpleName() + " " + model);
        return true;
    }

    private static String getKindString(IResourceDelta change) {
        int kind = change.getKind();
        switch (kind) {
        case IResourceDelta.ADDED:
            return "ADDED";
        case IResourceDelta.REMOVED:
            return "REMOVED";
        case IResourceDelta.CHANGED:
            return "CHANGED";
        default:
            return null;
        }
    }

    /**
     * Update the model on the Big-IP.<br>
     * If the model is locally added then call its {@link ModelObject #iControlRestPostJob} method.<br>
     * Otherwise call its {@link ModelObject #iControlRestPatchJob} method.<br>
     */
    private static void modelUpdate(ModelObject model, IWorkbenchPage page) {
        boolean locallyAdded = model.isLocallyAdded();
        String failedMessage = (locallyAdded ? Strings.LABEL_FAILED_POSTING : Strings.LABEL_FAILED_PATCHING) + model;
        RequestCompletion completion = new ProcessResponseJobCompletion(model, page, model.getFile(), failedMessage);
        ISchedulingRule mutex = Util.getMutex();
        if (locallyAdded) {
            model.iControlRestPostJob(completion, mutex);                
        } else {
            model.iControlRestPatchJob(completion, mutex);
        }
    }

    private static ModelObject getModel(IResourceDelta change) {
        IPath location = change.getFullPath();
        if (location == null) {
            return null;
        }
        String modelName = getModelName(location);
        if (modelName == null) {
            return null;
        }
        BigIPConnection connection = (BigIPConnection) ModelRoot.getInstance().findConnection(location);
        if (connection == null) {
            return null;
        }
        location = location.removeFirstSegments(1);
        ModelObject model = connection.getModel(null, location);
        return model;
    }

    /**
     * Return true in one of these cases:<br>
     * 1. The running thread is not allowed to do model processing.<br>
     * 2. The model content was received in Response from the Big-IP.<br>
     * 3. The model is locally modified.<br>
     * 4. The connection is set to Offline Mode.
     */
    private boolean ignoreModel(ModelObject model) {
        
        Thread currentThread = Thread.currentThread();
        if (ignoreThread(currentThread)) {
            logger.trace("Ignore change in " + model + " identified by " + currentThread);
            return true;
        }
        /* Check if the Content change in the resource was triggered by REST response.
        If so, Don't try to upload the file */
        if (model.isContentFromResponse()) {
            logger.trace(model + " ContentFromResponse is True");
            return true;
        }
        // Check if the model is set to be modified locally.
        // In that case, don't write to Big-IP automatically 
        if (model.isLocallyModified()) {
            logger.debug(model + " is Edited. Don't Patch");
            return true;
        }
        BigIPConnection connection = model.getConnection();
        if (!connection.isOnlineMode()) {
            logger.trace(connection + " is in Offline Mode");
            IPath filePath = model.getFilePath();
            if (!model.isLocallyAdded()) {
                if (PersistentPropertiesUtil.isNewFile(connection, filePath)) {
                    model.setLocallyAdded(true);
                } else {
                    model.setLocallyModified(true);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Check if the current {@link Thread} is allowed to do model processing.<br>
     * return true if thread should be ignored (not allowed)<br>
     * The allowed threads are: 'main' and 'Worker-*'
     */
    private static boolean ignoreThread(Thread currentThread) {
        String threadName = currentThread.getName();
        return !threadName.equals("main") && !threadName.startsWith("Worker-");
    }

    /*
     * If file extension belongs to {@link DataGroup} model
     * then return the file name without the suffix.
     * Otherwise return the file simple name (xxxxx.yyy)
     */
    private static String getModelName(IPath location) {

        String modelName = null;
        String lastSegment = location.lastSegment();
        String fileExtension = location.getFileExtension();
        if (fileExtension == null) {
            return lastSegment;
        }
        if (fileExtension.equals(DataGroup.FILE_SUFFIX)) {
            if (lastSegment != null) {
                int index = lastSegment.indexOf(".");
                modelName = lastSegment.substring(0, index);
            }            
        }
        else{
            modelName = lastSegment;
        }
        return modelName;
    }


}
