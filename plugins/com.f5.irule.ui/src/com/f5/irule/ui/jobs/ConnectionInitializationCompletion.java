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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Version;

import com.f5.irule.model.BigIPConnection;
import com.f5.irule.model.RequestCompletion;
import com.f5.irule.model.RestFramework;
import com.f5.irule.model.RuleProvider;
import com.f5.irule.model.TmosVersion;
import com.f5.irule.ui.Ids;
import com.f5.irule.ui.Strings;
import com.f5.irule.ui.views.ExplorerContentProvider;
import com.f5.irule.ui.views.Util;
import com.f5.rest.common.RestOperation.RestMethod;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ConnectionInitializationCompletion extends RequestCompletion {
    
    private static Logger logger = Logger.getLogger(ConnectionInitializationCompletion.class);

    private static final String SYS = "sys";
    private static final String PROVISION = "provision";

    private BigIPConnection conn;
    private IProject project;
    private RequestCompletion parentCompletion;
    private Shell shell;

    public ConnectionInitializationCompletion(BigIPConnection conn, IProject project,
            RequestCompletion parentCompletion, Shell shell) {
        this.conn = conn;
        this.project = project;
        this.parentCompletion = parentCompletion;
        this.shell = shell;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(getClass().getSimpleName());
        builder.append(" ").append(conn);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public void completed(String method, String uri, JsonObject jsonBody) {
        
        logger.debug("Completed " + method + " " + uri);
        Version version = TmosVersion.parse(jsonBody);
        boolean versionOk = checkVersion(version);
        if (versionOk) {
            Job openProjectJob = new OpenProjectJob(conn, project);
            openProjectJob.schedule();
            conn.setVarsion(version);
            // Load from connection, get provisioning info, then load resources
            String loadUri = conn.getURI(SYS, PROVISION).toString();
            RequestCompletion loadCompletion = new LoadCompletion(conn);
            RestFramework.sendRequestJob(conn, RestMethod.GET, loadUri,
                null, null, loadCompletion, Util.getMutex());
        }
        if (parentCompletion != null) {
            parentCompletion.completed(method, uri, jsonBody);
        }
    }

    private static class OpenProjectJob extends Job {
        private BigIPConnection conn;
        private IProject project;
        OpenProjectJob(BigIPConnection conn, IProject project) {
            super(Strings.JOB_OPEN_PROJECT);
            this.conn = conn;
            this.project = project;
        }
        protected IStatus run(IProgressMonitor monitor) {
            ExplorerContentProvider.openProject(conn, project);
            return Status.OK_STATUS;
        }
    }

    @Override
    public void failed(Exception ex, String method, String uri, String responseBody) {
        String message = getMessage(ex);
        logger.warn("Failed connection " + (uri == null ? conn : uri) + ":" + message);
        if (shell != null) {
            // UnSet the dialog spinning cursor 
            ExplorerContentProvider.setWaitCursur(shell, false);
        }

        // invisibleRoot.addChild(conn);
        String failureMessage = Strings.msg(conn.getAddress(), Strings.ERROR_CANNOT_CONNECT, ":", message);
        IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN, failureMessage, ex);
        StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.SHOW);
        if (parentCompletion == null) {
            // old connection failed
        } else {
            // new connection failed. Don't add child on failure,
            // otherwise it would not reconnect on second press on Finish button
            parentCompletion.failed(ex, method, uri, responseBody);
        }
        
        Util.syncWithUi();
    }

    private static String getMessage(Exception ex) {
        if (ex == null) {
            return "";
        }
        String message = ex.getMessage();
        if (message == null) {
            return "";
        }
        int bodyIndex = message.indexOf("body:");
        if (bodyIndex != -1) {
            try {
                String body = message.substring(bodyIndex + 5);
                JsonElement element = RuleProvider.parseElement(body);
                if (element instanceof JsonObject) {
                    JsonObject bodyJson = (JsonObject) element;
                    JsonElement messageElement = bodyJson.get("message");
                    message = messageElement.getAsString();                    
                } else {
                    message = element.getAsString();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return message;
    }

    private boolean checkVersion(Version version) {
        IStatus status = null;
        if (version == null) {
            status = new Status(IStatus.ERROR, Ids.PLUGIN, Strings.ERROR_FAILED_TO_RETRIEVE_VERSION_INFO);
        }
        if (version.compareTo(TmosVersion.MINIMUM) < 0) {
            status = new Status(IStatus.ERROR, Ids.PLUGIN,
                    Strings.msg(Strings.ERROR_MINIMUM_VERSION, TmosVersion.MINIMUM.toString()));
        }
        else if (version.compareTo(TmosVersion.MAXIMUM) > 0) {
            status = new Status(IStatus.ERROR, Ids.PLUGIN,
                    Strings.msg(Strings.ERROR_MAXIMUM_VERSION, TmosVersion.MAXIMUM.toString()));
        }
        if (status == null) {
            return true;
        }                    
        StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.SHOW);
        return false;
    }

    private static class LoadCompletion extends RequestCompletion {

        private static final String PARTITION = "partition";
        private static final String AUTH = "auth";
        private BigIPConnection conn;

        private LoadCompletion(BigIPConnection conn) {
            this.conn = conn;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("[").append(getClass().getSimpleName());
            builder.append(" ").append(conn);
            builder.append("]");
            return builder.toString();
        }

        @Override
        public void completed(String method, String uri, JsonObject jsonBody) {
            logger.debug("Completed " + method + " " + uri);
            conn.parseProvisioningInfo(jsonBody);
            String partitionUri = conn.getURI(AUTH, PARTITION).toString();
            RequestCompletion completion = new PartitionsCompletion(conn);
            RestFramework.sendRequestJob(conn, RestMethod.GET, partitionUri, null, null, completion, Util.getMutex());
        }

        @Override
        public void failed(Exception ex, String method, String uri, String responseBody) {
            IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN, Strings.ERROR_FAILED_TO_RETRIEVE_PROVISIONING, ex);
            StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.SHOW);
        }
    }  
    
    private static class PartitionsCompletion extends RequestCompletion {

        private BigIPConnection conn;
        
        private PartitionsCompletion(BigIPConnection conn) {
            this.conn = conn;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("[").append(getClass().getSimpleName());
            builder.append(" ").append(conn);
            builder.append("]");
            return builder.toString();
        }

        @Override
        public void completed(String method, String uri, JsonObject jsonBody) {
            StringBuilder logMessage = new StringBuilder("Completed " + method +
                " " + uri + ". Add Partitions from response");
            JsonElement items = RuleProvider.parseItems(jsonBody);
            logMessage.append("\n").append(RuleProvider.formatJsonContent(items));
            logger.debug(logMessage.toString());
            conn.setPartitions(items);
            ExplorerContentProvider provider = Util.getExplorerContentProvider();
            provider.loadResources(conn);
        }

        @Override
        public void failed(Exception ex, String method, String uri, String responseBody) {
            IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN, Strings.ERROR_FAILED_TO_RETRIEVE_PARTITIONS, ex);
            StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.SHOW);
        }   
    }
}