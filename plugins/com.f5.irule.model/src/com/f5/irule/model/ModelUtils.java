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

import java.net.InetAddress;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import com.f5.irule.model.BigIPConnection.Module;

public class ModelUtils {

    private static Logger logger = Logger.getLogger(ModelUtils.class);

    /**
     * Call for creating the folder hierarchy above the given folder<br>
     * If the trigger to the folder creation is a received response from the Big-IP<br>
     * then set the its ContentFromResponse session property to true<br>
     * so the DeltaModelFileVisitor.processLastChange() would ignore the change.
     */
    public static void prepareFolder(IFolder folder, boolean contentFromResponse) {
        IContainer parent = folder.getParent();
        if (parent instanceof IFolder) {
            prepareFolder((IFolder) parent, contentFromResponse);
        }
        boolean exists = folder.exists();
        if (!exists) {
            try {
                setContentFromResponseQualifier(folder, contentFromResponse);
                folder.create(true, true, null);
                setContentFromResponseQualifier(folder, false);
            } catch (CoreException e) {
                String errorMessage = e.getMessage();
                if (errorMessage.endsWith("already exists.")) {
                    logger.debug("Resource already exists: " + folder);
                }
                else if(errorMessage.endsWith("does not exist.")){
                    logger.warn(errorMessage + " Cannot create " + folder);
                }
                else {
                    IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN, Messages.CANNOT_CREATE_RESOURCE, e);
                    StatusManager.getManager().handle(status, StatusManager.LOG);
                }
            }
        }
    }

    /**
     * Set the resource contentFromResponse property.<br>
     * The property value is set on the resource parent ContentFromResponse session property.
     */
    static void setContentFromResponseQualifier(IResource resource, boolean contentFromResponse) throws CoreException {
        QualifiedName qualifiedName = getContentFromResponseQualifier(resource.getName());
        IResource parent = resource.getParent();
        try {
            parent.setSessionProperty(qualifiedName, contentFromResponse);
        } catch (CoreException ex) {
            String message = ex.getMessage();
            if (message.endsWith("is not local.")) {
                logger.warn("Try again to set " + qualifiedName + " session property to " + contentFromResponse);
                // Try Again
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                parent.setSessionProperty(qualifiedName, contentFromResponse);
            }
            else if(message.endsWith("does not exist.")){
                logger.warn(message + " Cannot set " + qualifiedName + " session property to " + contentFromResponse);
            }
            else{
                logger.warn("Failed setting " + qualifiedName + " session property to " + contentFromResponse, ex);
                throw ex;
            }
        }
    }

    /**
     * Return true if the resource contentFromResponse property is true.<br>
     * The property value is taken from the resource parent ContentFromResponse session property<br>
     * Used by DeltaModelFileVisitor.processLastChange(), if true it ignores the change.
     */
    public static boolean contentFromResponse(IResource resource) {
        QualifiedName qualifiedName = ModelUtils.getContentFromResponseQualifier(resource.getName());
        try {
            IContainer resourceParent = resource.getParent();
            Boolean contentFromResponseObject = (Boolean) resourceParent.getSessionProperty(qualifiedName);
            if (contentFromResponseObject != null && contentFromResponseObject.booleanValue()) {
                return true;
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static final String ONLINE_MODE_QUALIFIER = ModelUtils.class.getName();
    private static final String CONTENT_FROM_RESPONSE = ".ContentFromResponse";
    private static QualifiedName getContentFromResponseQualifier(String name) {
        QualifiedName qualifiedName = new QualifiedName(ONLINE_MODE_QUALIFIER + CONTENT_FROM_RESPONSE, name);
        return qualifiedName;
    }
    
    /**
     * Below are some admittedly LAME utilities for extracting file attributes from
     * its corresponding path.  Assumptions are made about the path.
     * These should only be necessary when a operation
     * occurs outside the context of the Explorer (eg. when an edit buffer is saved)
     * TODO figure out something better.
     * Until then, this tomfoolery can at least all exist in one place.
     */   
    public static String connectionFromPath(IPath path) {
        if (path != null && path.segmentCount() > 0) {
            return path.segment(0);
        }
        return null;        
    }
    
    public static String moduleFromPath(IPath path) {
        if (path != null && path.segmentCount() > 2) {
            return path.segment(2).toString().equals(Ids.IRULES_GTM_FOLDER) ? Module.gtm.name()
                    : Module.ltm.name();
        }
        return null;        
    }
    
    public static String filenameFromPath(IPath path) {
        if (path == null) {
            return null;
        }
        IPath pathNoExtenstion = path.removeFileExtension();
        if (pathNoExtenstion == null) {
            return null;
        }
        return pathNoExtenstion.lastSegment();        
    }
    
    public static boolean isIrule(IPath path) {
        if (path != null && path.segmentCount() > 2) {
            String modulefolder = path.segment(2).toString();
            if (modulefolder.equals(Ids.IRULES_LTM_FOLDER)
             || modulefolder.equals(Ids.IRULES_GTM_FOLDER)) {
                return true;
            }
        }
        return false;    
    }

    public static IStatus isIpReachable(String ip, int timeout) {
        try{
            InetAddress address = InetAddress.getByName(ip);
            boolean reachable = address.isReachable(timeout);
            if (reachable) {
                 return new Status(IStatus.OK, Ids.PLUGIN, "");
            }
            return new Status(IStatus.WARNING, Ids.PLUGIN, ip + " : " + Messages.NOT_REACHABLE);
        } catch (Exception e){
            return new Status(IStatus.ERROR, Ids.PLUGIN, e.getMessage());
        }
    }
    
    /** 
     * Returns true if resource is non-null, the associated project is found
     * and contains this plugin's nature
     */
    public static boolean isResourceWithinIruleNature(IResource resource) {
        if (resource == null) {
            return false;
        }
        IProject proj = resource.getProject();
        if (proj == null) {
            return false;
        }
        try {
            if (proj.getNature(Ids.NATURE_ID) == null) {
                return false;
            }
        } catch (CoreException e) {
            return false;
        }
        return true;
    }

    public static void logError(String statusMessage, Throwable ex) {
        logError(statusMessage, StatusManager.LOG | StatusManager.SHOW, ex);
    }

    public static void logError(String statusMessage, int style, Throwable ex) {
        Status status = new Status(IStatus.ERROR, Ids.PLUGIN, statusMessage, ex);
        StatusManager.getManager().handle(status, style);
    }
}
