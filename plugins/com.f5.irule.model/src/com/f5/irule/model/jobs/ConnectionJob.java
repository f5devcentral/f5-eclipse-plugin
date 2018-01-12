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

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

import com.f5.irule.model.BigIPConnection;
import com.f5.irule.model.RequestCompletion;
import com.f5.rest.common.RestOperation;
import com.f5.rest.common.RestRequestCompletion;
import com.google.gson.JsonObject;

/**
 * A {@link Job} that wraps a {@link BigIPConnection}.<br>
 * When the job is created, it increments the connection job counter.<br>
 * The counter is decremented in these cases:<br>
 * 1. The Rest operation fails to send.<br>
 * 2. The Rest operation is completed successfully.<br>
 * 3. The Rest operation is completed with failure.<br><br>
 * In the case that the rest operation fails,<br>
 * this job also executes the {@link RestRequestCompletion #failed(Exception, RestOperation)} method.
 */
public abstract class ConnectionJob extends Job {

    private static Logger logger = Logger.getLogger(ConnectionJob.class);

    protected BigIPConnection connection;
    private RequestCompletion jobCompletion;
    private RequestCompletion externalCompletion;

    public ConnectionJob(String name, BigIPConnection connection, RequestCompletion externalCompletion, ISchedulingRule mutex) {
        super(name);
        this.connection = connection;
        this.externalCompletion = externalCompletion;
        jobCompletion = new ConnectionJobCompletion(externalCompletion, this);
        connection.incrementJobCount();
        logger.trace("Set Rule " + mutex + " to " + this);
        setRule(mutex);
    }
    
    protected abstract IStatus doRestOperation(RequestCompletion jobCompletion);

    int decrementJobCount() {
        return connection.decrementJobCount();
    }

    public BigIPConnection getConnection() {
        return connection;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(getClass().getSimpleName());
        builder.append(" ").append(connection);
        builder.append(" ").append(externalCompletion);
        builder.append("]");
        return builder.toString();
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        IStatus status = doRestOperation(jobCompletion);
        if (!status.isOK()) {
            decrementJobCount();
            Throwable statusException = status.getException();
            Exception ex = statusException instanceof Exception ? (Exception) statusException : null;
            externalCompletion.failed(ex, null, null, null);
        }
        return status;
    }
    
    public static class ConnectionJobCompletion extends RequestCompletion {

        private ConnectionJob connectionJob;
        private RequestCompletion externalCompletion;

        private ConnectionJobCompletion(RequestCompletion completion, ConnectionJob connectionJob) {
            this.externalCompletion = completion;
            this.connectionJob = connectionJob;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("[").append(getClass().getSimpleName()).append(" ");
            builder.append(externalCompletion);
            builder.append("]");
            return builder.toString();
        }

        @Override
        public void completed(String method, String uri, JsonObject jsonBody) {
            int jobCount = connectionJob.decrementJobCount();
            externalCompletion.setConnectionJobCount(jobCount);
            externalCompletion.completed(method, uri, jsonBody);
        }

        @Override
        public void failed(Exception ex, String method, String uri, String body) {
            logger.warn("Failed " + method + " " + uri, ex);
            int jobCount = connectionJob.decrementJobCount();
            externalCompletion.setConnectionJobCount(jobCount);
            externalCompletion.failed(ex, method, uri, body);
        }

        public RequestCompletion getConnectionRestRequestCompletion() {
            return externalCompletion;
        }

        /** 
         * Delegate to wrapped RequestCompletion 
         */
        public boolean isJson() {
            return externalCompletion.isJson();
        };
    }
}
