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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import com.google.gson.JsonObject;

/**
 * On completion, Set the {@link IFile} modification stamp in the project persistent properties<br>
 * and then call the external {@link RequestCompletion #completed(String, String, JsonObject)} method
 */
public class InstallUploadedFileCompletion extends RequestCompletion {

    private static Logger logger = Logger.getLogger(RuleProvider.class);

    private String uploadFileCopy;
    private IFile file;

    private BigIPConnection connection;

    private RequestCompletion externalCompletion;

    public InstallUploadedFileCompletion(String uploadFileCopy, IFile file, BigIPConnection connection, RequestCompletion externalCompletion) {
        this.uploadFileCopy = uploadFileCopy;
        this.file = file;
        this.connection = connection;
        this.externalCompletion = externalCompletion;
    }

    @Override
    public void completed(String method, String uri, JsonObject responseBody) {
        logger.debug("Finished uploading " + uploadFileCopy);
        logStatus();
        if (file != null) {
            PersistentPropertiesUtil.updateModificationStampMap(file, connection);
        }
        externalCompletion.completed(method, uri, responseBody);
    }

    private void logStatus() {
        String statusMessage = Messages.FILE_UPLOADED_SUCCESSFULLY + " : " + uploadFileCopy;
        IStatus status = new Status(IStatus.INFO, Ids.PLUGIN, statusMessage);
        StatusManager.getManager().handle(status, StatusManager.LOG);
    }

    @Override
    public void failed(Exception ex, String method, String uri, String body) {
        IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN, Messages.ILX_FILE_UPLOAD_ERROR, ex);
        StatusManager.getManager().handle(status, StatusManager.LOG);
        externalCompletion.failed(ex, method, uri, body);
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(getClass().getSimpleName()).append(" ");
        builder.append(uploadFileCopy);
        builder.append("]");
        return builder.toString();
    }
    
}