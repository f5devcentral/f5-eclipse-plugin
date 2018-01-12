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
package com.f5.irule.ui.views;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.statushandlers.StatusManager;

import com.f5.irule.model.RequestCompletion;
import com.f5.irule.model.ModelObject;
import com.f5.irule.ui.Ids;
import com.google.gson.JsonObject;

public class ProcessResponseJobCompletion extends RequestCompletion {

    private static Logger logger = Logger.getLogger(ProcessResponseJobCompletion.class);

    private ModelObject model;
    private IWorkbenchPage page;
    private IFile file;
    private String failedMessage;

    public ProcessResponseJobCompletion(ModelObject obj, IWorkbenchPage page, IFile file, String failedMessage) {
        this.model = obj;
        this.page = page;
        this.file = file;
        this.failedMessage = failedMessage;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(getClass().getSimpleName()).append(" ");
        builder.append(file);
        builder.append("]");
        return builder.toString();
    }

    public void completed(String method, final String uri, JsonObject jsonBody) {
        logger.trace("Completed " + (method == null ? model : (method + " " + uri)));
        // Set a finish Runnable that updates the UI table and open the file editor for the model.
        // The finish runnable would run after the model processing finishes
        // and that is to make sure the model locallyModified and locallyAdded flags are set
        // prior to calling the IruleView.openIdeEditor(...) which depends on their value.
        model.processResponse(uri, jsonBody, new Runnable() {
            public void run() {
                Util.syncWithUi();
                if (file != null) {
                    IruleView.openIdeEditor(page, file, model);
                }
            }
        });
    }

    public void failed(Exception ex, String method, String uri, String responseBody) {
        logger.warn("Failed " + (method == null ? "getting content of " + model : method + " " + uri));
        // Update the locally modified flag to true and call for UI sync
        // so the UI would show that this model file is out of sync with the Big-IP
        // by applying a modified icon to this model UI tree item
        model.setLocallyFlags();
        Util.syncWithUi();
        IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN, failedMessage + ": " +
            Util.getMessageElementFromException(ex));
        StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.SHOW);
    }

    /* 
     * Delegate to wrapped ModelObject 
     */
    public boolean isJson() {
        return model.isJsonResponse();
    };
};