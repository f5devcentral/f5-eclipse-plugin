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

import com.google.gson.JsonObject;

/**
 * Callback interface for processing a response
 * to a REST request that had been sent to the Big-IP.
 */
public abstract class RequestCompletion {

    private static Logger logger = Logger.getLogger(RequestCompletion.class);

    private int jobCount = -1;

    protected String headers;

    /**
     * Do logic for Successful request. the response body is parsed to {@link JsonObject}
     */
    public abstract void completed(String method, String uri, JsonObject responseBody);

    /**
     * Do logic for failed request. the response body is given as String
     */
    public abstract void failed(Exception ex, String method, String uri, String responseBody);

    /**
     * @return true if this completion is expecting a JSON response.<br>
     * Otherwise return false - textual response.
     */
    public boolean isJson() {
        return true;
    }

    public void setConnectionJobCount(int jobCount) {
        this.jobCount = jobCount;
    }

    public int getConnectionJobCount() {
        return jobCount;
    }

    protected static void logCompleted(String method, String uri, String additional) {
        logger.debug("Completed " + method + " " + uri + additional);
    }
}
