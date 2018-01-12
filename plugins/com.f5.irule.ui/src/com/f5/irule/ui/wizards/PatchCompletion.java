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

import com.f5.irule.model.BigIPConnection;
import com.f5.irule.model.ModelObject;
import com.f5.irule.model.RequestCompletion;
import com.f5.irule.ui.jobs.FlowTracker;
import com.google.gson.JsonObject;

public class PatchCompletion extends RequestCompletion {

    private static Logger logger = Logger.getLogger(PatchCompletion.class);

    private FlowTracker flowTracker;
    private ModelObject model;
    private BigIPConnection connection;
    private boolean reloadConnectionOnCompletion;

    PatchCompletion(FlowTracker flowTracker, ModelObject model,
            BigIPConnection connection, boolean reloadConnectionOnCompletion) {
        this.flowTracker = flowTracker;
        this.model = model;
        this.connection = connection;
        this.reloadConnectionOnCompletion = reloadConnectionOnCompletion;
    }

    @Override
    public void completed(String method, String uri, JsonObject jsonBody) {
        logger.debug("Completed " + method + " " + uri);
        // Do the finishJob logic (reload connection) after the model processing
        // to make sure that the model modification stamp update 
        // is done before reloading the UI explorer tree so the model icon in the UI tree
        // would reflect the actual state of the model (not locally modified)
        model.processResponse(uri, jsonBody, new Runnable(){
            public void run() {
                int jobCountValue = flowTracker.jobFinished(true,
                    model.getFilePath().toString(), getConnectionJobCount());
                finishJob(jobCountValue);
            }});
    }

    @Override
    public void failed(Exception ex, String method, String uri, String responseBody) {
        logger.warn("Failed Patching " + model.getName(), ex);
        int jobCountValue = flowTracker.jobFinished(false, model.getFile().toString(), getConnectionJobCount());
        finishJob(jobCountValue);
    }
    
    private void finishJob(int jobCountValue) {
        if (jobCountValue == 0) {
            SyncModifiedResourcesWizard.reloadConnection(reloadConnectionOnCompletion, connection);
        }
    }
}