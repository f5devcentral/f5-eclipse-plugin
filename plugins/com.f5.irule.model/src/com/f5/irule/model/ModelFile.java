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

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

import com.f5.rest.common.RestRequestCompletion;
import com.google.gson.JsonObject;

public abstract class ModelFile extends ModelObject {
    
    private static Logger logger = Logger.getLogger(ModelFile.class);

    public ModelFile(String name, BigIPConnection conn, String partition, Type type, IPath filePath) {
        super(name, conn, partition, type, filePath);
    }

    @Override
    public void setResponseContents(IFile file, String content) {
        if (content != null) {
            try {
                setFileContent(file, content);
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Set the content of a file resource.<br>
     * If the file does not exist, then first prepare its parent folders and then create the file.<br>
     * Set the ContentFromResponse session property of the created file and any additional folders<br>
     * according to the model contentFromResponse flag<br>
     * so it will be ignored by the DeltaModelFileVisitor.processLastChange() method.
     */
    protected void setFileContent(IFile file, String content) throws CoreException {
        InputStream source = new ByteArrayInputStream(content.getBytes());
        boolean contentFromResponse = isContentFromResponse();
        if (file.exists()) {
            ModelUtils.setContentFromResponseQualifier(file, contentFromResponse);
            logger.debug("Set Contents of " + file + " to " + content);
            file.setContents(source, isForceSetContent(), true, null);
            ModelUtils.setContentFromResponseQualifier(file, false);
        } else {
            IFolder parent = (IFolder) file.getParent();
            ModelUtils.prepareFolder(parent, contentFromResponse);
            ModelUtils.setContentFromResponseQualifier(file, contentFromResponse);
            logger.debug("Create file " + file + " with content " + content);
            file.create(source, true, null);
            ModelUtils.setContentFromResponseQualifier(file, false);
        }
    }

    protected boolean isForceSetContent() {
        return false;
    }
    
    public IStatus iControlRestGet(RestRequestCompletion completion) {
        throw new UnsupportedOperationException("iControlRestGet not implemented for " + getClass());
    }

    public IStatus iControlRestPatch(RestRequestCompletion completion) {
        throw new UnsupportedOperationException("iControlRestPatch(completion) not implemented for " + getClass());
    }
    
    public String parseContent(JsonObject jsonBody) {
        return getContent();
    }

    public String getContent() {
        String content = null;
        IFile file = getFile();
        if (file.exists()) {
            try {
                InputStream contents = file.getContents();
                content = RestFramework.inputStreamToString(contents);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Avoid an empty file which iControlRest refuses to transfer
            content = "\n\n";
        }
        return content;
    }
}
