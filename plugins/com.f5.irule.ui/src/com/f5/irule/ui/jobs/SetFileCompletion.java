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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import com.f5.irule.model.RequestCompletion;
import com.f5.irule.model.ModelObject;
import com.f5.irule.ui.Ids;
import com.f5.irule.ui.Strings;
import com.google.gson.JsonObject;

public class SetFileCompletion extends RequestCompletion {

    private static Logger logger = Logger.getLogger(SetFileCompletion.class);
    
    private ModelObject model;
    
    public SetFileCompletion(ModelObject model) {
        this.model = model;
    }
    
    @Override
    public void completed(String method, String uri, JsonObject jsonBody) {
        logger.debug("Completed " + method + " " + uri);
        IFile file = model.getFile();
        model.processResponse(uri, jsonBody, null);
        logger.trace("Finished setting content of " + model + " to " + file);
    }
    
    @Override
    public void failed(Exception ex, String method, String uri, String responseBody) {
        if (method == null) {
            logger.warn("Failed setting content of " + model, ex);
        } else {
            logger.warn("Failed " + method + " " + uri, ex);
        }
        IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN, Strings.ERROR_FAILED_SETTING_CONTENT + model, ex);
        StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.SHOW);
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(getClass().getSimpleName()).append(" ");
        builder.append(model);
        builder.append("]");
        return builder.toString();
    }

    /* 
     * Delegate to wrapped ModelObject 
     */
    public boolean isJson() {
        return model.isJsonResponse();
    }
    
}