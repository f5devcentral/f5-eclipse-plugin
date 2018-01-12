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
package com.f5.irule.ui.wizards;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.statushandlers.StatusManager;

import com.f5.irule.model.Connection;
import com.f5.irule.model.ModelObject;
import com.f5.irule.model.ModelParent;
import com.f5.irule.model.RequestCompletion;
import com.f5.irule.ui.Ids;
import com.f5.irule.ui.Strings;
import com.f5.irule.ui.jobs.DeleteJob;
import com.f5.irule.ui.views.Util;
import com.google.gson.JsonObject;

public class DeleteWizard extends Wizard implements IWizard {

    private static Logger logger = Logger.getLogger(DeleteWizard.class);
    
    private ModelObject obj = null;
    private IResource resource = null;
    private DeletePage deletePage = null;

    private Runnable completeRunnable;

    public DeleteWizard(Runnable completeRunnable) {
        this.completeRunnable = completeRunnable;
    }
    
    @Override
    public void addPages() {
        
        if (obj == null) {
            logger.warn("Missing Object. Cannot delete");
            return;
        }
        StringBuilder deleteMessage = new StringBuilder(Strings.MESSAGE_DELETE_CONFIRM_PREAMBLE);
        deleteMessage.append(" '").append(obj.getName()).append("' ");
        if (obj.getType() == ModelObject.Type.CONNECTION) {
            deleteMessage.append(Strings.MESSAGE_DELETE_CONNECTION);
        } else {
            deleteMessage.append(Strings.FROM_THE_CONTAINING_PROJECT);
            if (obj.getConnection().isOnlineMode()) {
                deleteMessage.append(Strings.AND_THE_CONNECTED_BIG_IP);
            } else {
                deleteMessage.append(Strings.OFFLINE_MODE_ONLY_LOCAL_RESOURCE_WOULD_BE_DELETED);
            }
        }        
        deletePage = new DeletePage(Strings.LABEL_DELETE_CONFIRMATION, deleteMessage.toString());
        addPage(deletePage);
    }

    public void init(IStructuredSelection selection) {
        Object ele = selection.getFirstElement();
        if (ele instanceof ModelObject) {
            obj = (ModelObject) ele;
            resource = obj.findResource();
        }
    }
    
    /**
     * Recursively unset the readOnly attr on children of parent
     * 
     * @param parent
     *            The root model to traverse
     */
    private void unsetReadOnlyOnChildren(ModelParent parent) {
        for (ModelObject child : parent.getChildren()) {
            if (child instanceof ModelParent) {
                ModelParent childParent = (ModelParent) child;
                unsetReadOnlyOnChildren(childParent);
            } else {
                IFile file = child.getFile();
                if (file != null) {
                    ResourceAttributes attr = file.getResourceAttributes();
                    if (attr != null) {
                        attr.setReadOnly(false);
                        try {
                            file.setResourceAttributes(attr);
                        } catch (CoreException e) {
                            // Attempt to unset is the best we can do
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean performFinish() {
        if (obj == null) {
            deletePage.setErrorMessage(Strings.ERROR_CANNOT_DELETE_RESOURCE);
            return false;
        }

        IResource proj = obj.getProject();
        DeleteJob job = null;
        if (obj.getType() == ModelObject.Type.CONNECTION) {
            // Delete on some platforms (MacOS, Windows, etc.) will fail on read-only files
            unsetReadOnlyOnChildren((Connection) obj);
            job = new DeleteJob(obj, proj, completeRunnable);
        }
        else {
            if (obj.isLocallyAdded() || !obj.getConnection().isOnlineMode()) {
                logger.debug("Delete locally " + obj);
                job = new DeleteJob(obj, resource, completeRunnable);
            } else {
                logger.debug("Delete " + obj);
                // Other types require removal from the bigip
                RequestCompletion completion = new DeleteResourceCompletion(obj, completeRunnable, resource);
                obj.iControlRestDeleteJob(completion, Util.getMutex());
            }
        }
        
        if (job != null) {
            logger.trace("Set Rule " + proj + " to " + job);
            job.setRule(proj);
            job.schedule();            
        }
        
        return true;
    }

    private static class DeleteResourceCompletion extends RequestCompletion {
        private ModelObject obj;
        private Runnable completeRunnable;
        private IResource resource;

        public DeleteResourceCompletion(ModelObject obj, Runnable completeRunnable, IResource resource) {
            this.obj = obj;
            this.resource = resource;
            this.completeRunnable = completeRunnable;
        }

        public void completed(String method, String uri, JsonObject jsonBody) {
            logger.debug("Completed deleting " + obj + " on Big-IP " + uri);
            Job job = new DeleteJob(obj, resource, completeRunnable);
            IProject project = obj.getProject();
            logger.trace("Set Rule " + project + " to " + job);
            job.setRule(project);
            job.schedule();
        }

        public void failed(Exception ex, String method, String uri, String responseBody) {
            String errorMessage = Strings.ERROR_FAILED_TO_DELETE_RESOURCE + " " + obj;
            logger.warn(errorMessage, ex);
            IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN, errorMessage, ex);
            StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.SHOW);
            //wizard.runDeleteJob();
        }

        public boolean isJson() {
            return false;
        };
    }
}
