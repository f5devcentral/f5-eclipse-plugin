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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;

/**
 * a Utility for caching in the project persistent properties the time stamp of Big-Ip resources responses.<br>
 * When the plug-in receives a REST response containing the content of a model resource,<br>
 * It saves the content in the local file system and cache its time stamp in the project persistent properties.<br>
 * The plug-in uses the cached time stamp to determine if a certain resource
 * was modified locally without synchronizing with the Big-IP.
 */
public class PersistentPropertiesUtil {

    private static Logger logger = Logger.getLogger(PersistentPropertiesUtil.class);

    /**
     * Compare the file resource time stamp with the resource response time stamp
     * that is cached in the project persistent properties.<br>
     * Return True if the resource time stamp is higher than the response time stamp.
     * False Otherwise.
     */
    static boolean isFileModified(BigIPConnection connection, IPath filePath) {
        String responseTimeStampValue = getResponseTimeStamp(connection, filePath);
        if (responseTimeStampValue == null) {
            // The resource was not downloaded from the Big-IP server
            logger.trace(filePath + ": The resource was not downloaded from the Big-IP server");
            return false;
        }
        IResource resource = connection.getProject().getFile(filePath);
        long modificationStamp = resource.getModificationStamp();
        // The resource was downloaded from the Big-IP server
        long responseTimeStamp = Long.parseLong(responseTimeStampValue);
        if (modificationStamp > responseTimeStamp) {
            // The Model was modified since it was downloaded from the server
            logger.trace(filePath + ": The resource was downloaded at " + responseTimeStamp +
                " and modified at " + modificationStamp);
            return true;
        }
        else if(modificationStamp < responseTimeStamp){
            logger.warn(filePath + ": Modification stamp " + modificationStamp
                + " is lower than response time " + responseTimeStamp);
            return false;
        }
        else {
            logger.trace(filePath + ": The resource was downloaded at " + responseTimeStamp + " and was not modified");
            // The Model was not modified since it was downloaded
            return false;
        }
    }

    public static boolean isNewFile(BigIPConnection connection, IPath filePath) {
        String responseTimeStampValue = getResponseTimeStamp(connection, filePath);
        if (responseTimeStampValue == null) {
            // The resource was not downloaded from the Big-IP server
            logger.debug(filePath + " is New");
            return true;
        }
        return false;
    }

    /**
     * Get the file cached project persistent property for the last response time stamp of the file.
     * @param connection
     * @param filePath
     */
    static long getResponseTimeStampLong(BigIPConnection connection, IPath filePath) {
        String value = getResponseTimeStamp(connection, filePath);
        if (value == null) {
            return -1;
        }
        long ans = Long.parseLong(value);
        return ans;
    }

    /**
     * Get the file cached project persistent property for the last response time stamp of the file.
     * @param connection
     * @param filePath
     */
    private static String getResponseTimeStamp(BigIPConnection connection, IPath filePath) {
        IProject project = connection.getProject();
        if (!project.isOpen()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (!project.isOpen()) {
                logger.warn("Not Open: " + project);
                return null;
            }
        }
        IPath location = project.getFullPath().append(filePath);
        QualifiedName qualifiedName = getResponseTimeStampQualifiedName(connection, location);
        String value = null;
        try {
            value = project.getPersistentProperty(qualifiedName);
        } catch (CoreException e) {
            logger.warn("Failed to get persistent property " + qualifiedName, e);
        }
        return value;
    }

    /**
     * Set the {@link IResource} modification stamp in the project persistent properties.
     */
    static void updateModificationStampMap(IResource file, BigIPConnection connection) {
        if (file == null) {
            return;
        }
        long modificationStamp = file.getModificationStamp();
        logger.trace("Update modification stamp of " + file + " : " + modificationStamp);
        IPath location = file.getFullPath();
        QualifiedName qualifiedName = getResponseTimeStampQualifiedName(connection, location);
        String value = String.valueOf(modificationStamp);
        setPersistentProperty(connection, qualifiedName, value);
        updateModificationStampMap(file.getParent(), connection);
    }

    private static final String MODIFICATION_STAMP_QUALIFIER = "com.f5.irule.model.ModificationStamp";
    private static QualifiedName getResponseTimeStampQualifiedName(BigIPConnection connection, IPath location) {
        String localName = connection.getName() + "_" + location.toString();
        QualifiedName name = new QualifiedName(MODIFICATION_STAMP_QUALIFIER, localName);
        return name;
    }
    
    public static boolean isOnlineMode(BigIPConnection connection) {
        QualifiedName qualifiedName = getConnectionOnlineModeQualifiedName(connection);
        String value = null;
        try {
            value = connection.getProject().getPersistentProperty(qualifiedName);
            boolean ans = Boolean.parseBoolean(value);
            return ans;
        } catch (CoreException e) {
            return true;
        }
    }

    static void setOnlineMode(BigIPConnection connection, boolean onlineMode) {
        QualifiedName qualifiedName = getConnectionOnlineModeQualifiedName(connection);
        logger.trace("Set Online Mode of " + connection + " to " + onlineMode);
        String value = String.valueOf(onlineMode);
        setPersistentProperty(connection, qualifiedName, value);
    }

    private static final String ONLINE_MODE_QUALIFIER = "com.f5.irule.model.OnlineMode";
    private static QualifiedName getConnectionOnlineModeQualifiedName(BigIPConnection connection) {
        String localName = connection.getName();
        QualifiedName name = new QualifiedName(ONLINE_MODE_QUALIFIER, localName);
        return name;
    }
    
    private static void setPersistentProperty(BigIPConnection connection, QualifiedName qualifiedName, String value) {
        IProject project = connection.getProject();
        if (!project.isOpen()) {
            logger.warn("Not Open: " + project +
                ". Cannot set persistent property " + qualifiedName + " to " + value);
            return;
        }
        try {
            project.setPersistentProperty(qualifiedName, value);
        } catch (CoreException ex) {
            logger.warn("Failed to set persistent property " + qualifiedName, ex);
        }
    }

}
