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

import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;

import com.f5.rest.common.CompletionHandler;
import com.f5.rest.common.RestFileTransferInformation;

public class RestFrameworkCompletionHandler extends CompletionHandler<RestFileTransferInformation> {

    private static Logger logger = Logger.getLogger(RestFrameworkCompletionHandler.class);

    RequestCompletion finalCompletion;
    private Semaphore mutex;

    protected RestFrameworkCompletionHandler(RequestCompletion finalCompletion, Semaphore mutex) {
        this.finalCompletion = finalCompletion;
        this.mutex = mutex;
    }

    void releaseMutex() {
        mutex.release();
    }

    @Override
    public void completed(RestFileTransferInformation info) {
        logger.trace("Completed transfer of " + info.localFilePath + " to " + info.targetReference.link);
        finalCompletion.completed(null, null, null);
        releaseMutex();
    }
    
    @Override
    public void failed(Exception ex, RestFileTransferInformation info) {
        logger.warn("Failed transfer of " + info.localFilePath, ex);
        finalCompletion.failed(ex, null, null, null);
        releaseMutex();
    }
 
}
