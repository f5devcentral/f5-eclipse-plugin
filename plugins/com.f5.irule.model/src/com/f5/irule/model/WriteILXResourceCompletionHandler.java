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

import com.f5.rest.common.RestFileTransferInformation;
import com.f5.rest.common.RestOperation.RestMethod;
import com.google.gson.JsonObject;

public class WriteILXResourceCompletionHandler extends RestFrameworkCompletionHandler {

    private static Logger logger = Logger.getLogger(WriteILXResourceCompletionHandler.class);

    private BigIPConnection conn;
    private String partition;
    private String workspace;
    private String uploadFileCopy;

    WriteILXResourceCompletionHandler(BigIPConnection conn, String partition, String workspace, String uploadFileCopy,
            RequestCompletion finalCompletion, Semaphore mutex) {
        super(finalCompletion, mutex);
        this.conn = conn;
        this.partition = partition;
        this.workspace = workspace;
        this.uploadFileCopy = uploadFileCopy;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(getClass().getSimpleName()).append(" ");
        builder.append(uploadFileCopy);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public void completed(RestFileTransferInformation operation) {
        
        String uri = createUri();
        String body = createBody();
        logger.debug("Success " + operation.targetReference.link +
            "\n\tSend " + uri + " Completion: " + finalCompletion);
        RestFramework.sendRequestToBigIP(conn, RestMethod.PATCH, uri, body, finalCompletion, 2000);
        releaseMutex();
    }

    private String createBody() {
        JsonObject json = new JsonObject();
        json.addProperty("command", "modify");
        String body = json.toString();
        return body;
    }

    private String createUri() {
        RestURI uri = conn.getURI(BigIPConnection.Module.ilx.name(), "workspace");
        uri.appendPartitionedOID(partition, workspace);
        uri.addOption("upload-file-copy", uploadFileCopy);
        return uri.toString();
    }

}
