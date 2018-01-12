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
package com.f5.irule.model.jobs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import com.f5.irule.model.BigIPConnection;
import com.f5.irule.model.Ids;
import com.f5.irule.model.RequestCompletion;
import com.f5.irule.model.RestFramework;
import com.f5.irule.model.RuleProvider;
import com.f5.rest.common.RestOperation.RestMethod;

/**
 * A {@link ConnectionJob} that executes the {@link RuleProvider #sendRestRequest} method
 */
public class SendRequestJob extends ConnectionJob {

    private RestMethod method;
    private String uri;
    private String contentType;
    private String jsonBody;

    public SendRequestJob(BigIPConnection connection, RequestCompletion completion,
            RestMethod method, String uri, String contentType, String jsonBody, ISchedulingRule mutex) {
        super(Ids.SEND_REST_REQUEST, connection, completion, mutex);
        this.method = method;
        this.uri = uri;
        this.contentType = contentType;
        this.jsonBody = jsonBody;
    }

    @Override
    protected IStatus doRestOperation(RequestCompletion jobCompletion) {
        return RestFramework.sendRequest(connection, method, uri, contentType, jsonBody, jobCompletion);
    }
    
}