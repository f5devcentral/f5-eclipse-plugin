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

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.statushandlers.StatusManager;

import com.f5.irule.model.ModelObject;
import com.f5.irule.model.ModelParent;
import com.f5.irule.ui.Ids;
import com.f5.irule.ui.Strings;
import com.f5.irule.ui.views.Util;

public class DeleteJob extends Job {

    private static Logger logger = Logger.getLogger(DeleteJob.class);

    private IResource resource;
    private int counter;

    private ModelObject obj;
    private Runnable completeRunnable;

    public DeleteJob(ModelObject obj, IResource resource, Runnable completeRunnable) {
        super(Strings.JOB_DELETING_RESOURCE);
        this.resource = resource;
        this.obj = obj;
        this.completeRunnable = completeRunnable;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(getClass().getSimpleName());
        builder.append(" ").append(resource);
        builder.append("]");
        return builder.toString();
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        logger.debug("Delete " + resource);
        boolean ok = true;
        if (resource != null) {
            logger.debug("Delete resource " + resource);
            try {
                resource.delete(true, null); // probably don't need a monitor for (fairly quick) resource deletes
            } catch (CoreException e) {
                e.printStackTrace();
                /* For some reason creating a Data-Group and setting its content by calling IFile.setContents(...)
                 * and right after trying to delete the file, sometimes results in a ResourceException.
                 * A workaround fix is to keep scheduling this DeleteJob
                 * until a deletion success or passing the time limit (failure) */
                ok = false;
                if (counter < 12) {
                    logger.debug("Set another Delete Job for " + resource);
                    counter++;
                    this.schedule(5000);                    
                } else {
                    IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN, Strings.ERROR_FAILED_TO_DELETE_RESOURCE, e);
                    StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.SHOW);
                }
            }
        }
        // If not ok then deletion failed.
        // In that case don't proceed with deletion logic
        if (ok) {
            removeObjectFromParent(obj);
            if (completeRunnable != null) {
                completeRunnable.run();
            }
            Util.syncWithUi();
        }
        return Status.OK_STATUS;
    }

    private static void removeObjectFromParent(ModelObject obj) {
        // Remove this object from it's parent
        ModelParent parent = obj.getParent();
        parent.removeChild(obj);

        // Log it
        IStatus status = new Status(IStatus.INFO, Ids.PLUGIN, Strings.INFO_RESOURCE_DELETED + obj);
        StatusManager.getManager().handle(status, StatusManager.LOG);
        
    }

}
