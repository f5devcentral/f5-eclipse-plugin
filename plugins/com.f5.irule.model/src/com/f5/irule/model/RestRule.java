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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import com.f5.rest.common.RestOperation.RestMethod;
import com.google.gson.JsonObject;

/**
 * {@link ModelFile} for Tcl resource file.<br>
 * Its {@link ModelObject #iControlRestPatch} method<br> 
 * Get the rule content from the local file and
 * send a PATCH rule REST request to the BigIP.<br>
 * Its {@link ModelObject #parseContent(JsonObject)} method gets the body of the json response
 */
public class RestRule extends Rule {

    private static Logger logger = Logger.getLogger(RestRule.class);
    
    public static final String RULE = "rule";

    protected String name;
    protected String body = null;
    protected String text = null;

    public RestRule(String name, BigIPConnection conn, String partition, Type type, IPath filePath) {
        super(name, conn, partition, type, filePath);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getText() {
        return text;
    }

    public void setText(String code) {
        text = code;
    }
    
    public String getModuleName() {
        return (getType() == ModelObject.Type.GTM_RULE) ? BigIPConnection.Module.gtm.name() :
            BigIPConnection.Module.ltm.name();    
    }

    @Override
    public boolean setCode(String code) {
        return false;
    }

    @Override
    public IStatus iControlRestDelete(RequestCompletion completion) {
        BigIPConnection conn = getConnection();
        RestURI uri = conn.getURI(getModuleName(), RULE);
        uri.append(getName());
        JsonObject json = new JsonObject();
        json.addProperty("partition", getPartition());
        return RestFramework.sendRequest(conn, RestMethod.DELETE, uri.toString(), null, json.toString(), completion);
    }

    @Override
    public IStatus iControlRestPost(RequestCompletion completion) {
        BigIPConnection conn = getConnection();
        RestURI uri = conn.getURI(getModuleName(), RULE);
        JsonObject json = new JsonObject();
        json.addProperty("name", getName());
        json.addProperty("apiAnonymous", "");
        json.addProperty("partition", getPartition());
        return RestFramework.sendRequest(conn, RestMethod.POST, uri.toString(), null, json.toString(), completion);
    }
    
    /**
     * Send a GET rule REST request with the name of this rule.
     */
    public IStatus iControlRestGet(RequestCompletion completion) {
        // Since this RestRule already exists, this method and the corresponding parser
        // are intended only for getting the rule content (the apiAnonymous property)
        // Additional properties could be added if needed
        BigIPConnection conn = getConnection();        
        RestURI uri = conn.getURI(getModuleName(), RULE);
        uri.appendPartitionedOID(getPartition(), getName());
        uri.addSelect("name,apiAnonymous");
        return RestFramework.sendRequest(conn, RestMethod.GET, uri.toString(), null, null, completion);
    }

    public IStatus iControlRestPatch(RequestCompletion completion) {

        String content;
        try {
            InputStream fileContents = ModelObject.getFileContents(this);
            content = RestFramework.inputStreamToString(fileContents);
        } catch (Throwable ex) {
            logger.warn("Failed to get contents of " + this, ex);
            String message = Messages.FAILED_TO_RETRIEVE_FILE;
            IStatus status = handleError(message, ex);
            return status;
        }        
        BigIPConnection connection = getConnection();
        String ruleName = getName();
        String moduleName = getModuleName();
        String partition = getPartition();
        String uri = createUri(connection, ruleName, moduleName);
        String body = createBody(content, partition);
        IStatus status = RestFramework.sendRequest(connection, RestMethod.PATCH,
            uri, "application/json", body, completion);
        return status;
    }

    private String createUri(BigIPConnection connection, String ruleName,
            String moduleName) {
        RestURI uri = connection.getURI(moduleName, RestRule.RULE);
        uri.append(ruleName);
        String uriValue = uri.toString();
        return uriValue;
    }

    private String createBody(String content, String partition) {
        JsonObject json = new JsonObject();
        json.addProperty("apiAnonymous", content);
        json.addProperty(RuleProvider.PARTITION, partition);
        String body = json.toString();
        return body;
    }

    public static IStatus handleError(String message, Throwable ex) {
        IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN, message, ex);
        handleError(status);
        return status;
    }

    private static void handleError(IStatus status) {
        StatusManager.getManager().handle(status, StatusManager.LOG);
    }

    @Override
    public void setResponseContents(final IFile file, String content) {
        if (checkReadOnly(content)) {
            setReadOnly(true);       
        }
        setText(content);
        try {
            setFileContent(file);
        } catch (CoreException e) {
            e.printStackTrace();
        }
        setAttributesReadOnly(file); // Set resource attr        
    }

    @Override
    public String parseContent(JsonObject jsonBody) {
        String ruleContent = null;
        if (jsonBody.isJsonObject()) {
            ItemData itemData = ItemData.getData(jsonBody);
            ruleContent = itemData.body;
        }
        if (ruleContent == null) {
            return "";
        }
        return ruleContent;
    }

    private void setAttributesReadOnly(final IFile file) {
        if (isReadOnly()) {
            ResourceAttributes attr = file.getResourceAttributes();
            if (attr != null) {
                attr.setReadOnly(true);
                try {
                    file.setResourceAttributes(attr);
                } catch (CoreException e) {
                }
            }
        }
    }

    /**
     * Sets the contents of the {@link IFile} to the this rule text.
     */
    private void setFileContent(final IFile file) throws CoreException {
        InputStream source = new ByteArrayInputStream(text.getBytes());
        boolean contentFromResponse = isContentFromResponse();
        if (file.exists()) {
            if (isReadOnly()) {
                logger.debug("Read Only: " + this);
            } else {
                ModelUtils.setContentFromResponseQualifier(file, contentFromResponse);
                logger.debug("Set Contents of " + file + " to " + text);
                file.setContents(source, false, true, null);
                ModelUtils.setContentFromResponseQualifier(file, false);
            }
        } else {
            ModelUtils.prepareFolder((IFolder) file.getParent(), contentFromResponse);
            ModelUtils.setContentFromResponseQualifier(file, contentFromResponse);
            logger.debug("Create file " + file + " with content " + text);
            file.create(source, true, null);
            ModelUtils.setContentFromResponseQualifier(file, false);
        }
    }

    /**
     * Return true if the content has a line that contains 'nowrite' or 'nodelete'
     */
    private static boolean checkReadOnly(String content) {
        // Check for read-only hints
        Scanner scanner = new Scanner(content);
        boolean ans = false;
        if (scanner.hasNextLine()) {
            // Check for read-only rules which have "nowrite" and/or "nodelete" on first line
            String line = scanner.nextLine();
            if (line.contains("nowrite") || line.contains("nodelete")) {
                ans = true;
            }
        }
        scanner.close();
        return ans;
    }

}
