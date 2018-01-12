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

import java.net.URI;

import com.f5.rest.common.RestOperation;
import com.f5.rest.common.RestRequestCompletion;
import com.f5.rest.common.RestRequestSender;
import com.google.gson.JsonObject;

/**
 * A Bridge between {@link RestRequestCompletion} and {@link RequestCompletion}<br>
 * The Bridge delegates processing the {@link RestRequestSender #sendRequest(RestOperation)} response<br>
 * to the {@link RequestCompletion} interface used by the plug-in
 */
public class RestRequestCompletionBridge extends RestRequestCompletion {

    public static final String TEXT = "text";

    private RequestCompletion requestCompletion;

    RestRequestCompletionBridge(RequestCompletion requestCompletion) {
        this.requestCompletion = requestCompletion;
    }

    @Override
    public void completed(RestOperation response) {
        String method = response.getMethod().name();
        String uri = response.getUri().toString();
        String body = response.getBodyAsString();
        if (body == null) {
            byte[] binaryBody = response.getBinaryBody();
            if (binaryBody != null) {
                body = new String(binaryBody);
            }
        }
        doCompleted(requestCompletion, method, uri, body);
    }

    @Override
    public void failed(Exception ex, RestOperation response) {
        String method = response.getMethod().name();
        URI uri = response.getUri();
        String body = response.getBodyAsString();
        requestCompletion.failed(ex, method, uri.toString(), body);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(getClass().getSimpleName());
        builder.append(" ").append(requestCompletion);
        builder.append("]");
        return builder.toString();
    }

    /**
     * Execute the {@link RequestCompletion #completed(String, String, JsonObject)} method.<br>
     * If the {@link RequestCompletion} is expecting a json response then parse the response body as {@link JsonObject}.<br>
     * Otherwise, Wrap the response text in a {@link JsonObject}.
     */
    static void doCompleted(RequestCompletion completion, String methodName, String uri, String responseBody) {
        JsonObject root;
        if (completion.isJson()) {
            root = (JsonObject) RuleProvider.parseElement(responseBody);                
        }
        else{
            root = new JsonObject();
            if (responseBody != null) {
                root.addProperty(TEXT, responseBody);
            }    
        }
        completion.completed(methodName, uri, root);
    }

}
