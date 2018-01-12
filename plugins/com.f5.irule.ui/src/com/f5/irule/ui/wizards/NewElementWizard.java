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
package com.f5.irule.ui.wizards;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.wizards.newresource.BasicNewFolderResourceWizard;

import com.f5.irule.model.BigIPConnection;
import com.f5.irule.model.RequestCompletion;
import com.f5.irule.model.BigIPConnection.Module;
import com.f5.irule.model.IAppsLxModelFile;
import com.f5.irule.model.ILXModelFile;
import com.f5.irule.model.ILXRuleModelFile;
import com.f5.irule.model.Ids;
import com.f5.irule.model.Messages;
import com.f5.irule.model.ModelFile;
import com.f5.irule.model.ModelObject;
import com.f5.irule.model.ModelObject.Type;
import com.f5.irule.model.ModelParent;
import com.f5.irule.model.ModelUtils;
import com.f5.irule.model.RestFramework;
import com.f5.irule.model.RestRule;
import com.f5.irule.model.RuleProvider;
import com.f5.irule.model.WorkspaceModelParent;
import com.f5.irule.ui.Strings;
import com.f5.irule.ui.views.ExplorerContentProvider;
import com.f5.irule.ui.views.Util;
import com.f5.rest.common.RestOperation.RestMethod;
import com.google.gson.JsonObject;

public class NewElementWizard extends BasicNewFolderResourceWizard {

    private static Logger logger = Logger.getLogger(NewElementWizard.class);

    private NewElementPage newElementPage = new NewElementPage("newElement");
    private ModelObject.Type type = ModelObject.Type.UNKNOWN;
    private ModelObject obj = null;
    private ModelParent parent = null;

    public void init(Action action, IStructuredSelection selection, ModelObject.Type type) {
        this.selection = selection;
        this.type = type;
        newElementPage.setTitle(action.getText());
        newElementPage.setDescription(action.getDescription());
    }

    public void addPages() {
        addPage(newElementPage);
    }

    @Override
    public boolean performFinish() {
        String name = newElementPage.getName();
        if (name == null) {
            return false;
        }
        if (hasInvalidFilenameCharacters(name)) {
            newElementPage.setMessage(Strings.MESSAGE_ILLEGAL_NAME, IMessageProvider.ERROR);
            return false;
        }
        parent = (ModelParent) selection.getFirstElement();
        if (parent == null) {
            return false;
        }
        if (parent.getChild(name) != null) {
            newElementPage.setErrorMessage(Strings.ERROR_RESOURCE_EXISTS);
            return false;
        }
        BigIPConnection conn = (BigIPConnection) parent.findAncestorOfType(ModelObject.Type.CONNECTION);
        if (conn == null) {
            return false;
        }
        String partition = conn.getCurrentPartition();
        IPath filePath = computeFilePath(name, partition);
        if (filePath == null) {
            logger.warn("Failed to construct path to " + name + " and " + parent);
            return true;
        }
        createModel(name, conn, filePath);   
        addModel();
        return true;
    }

    private IPath computeFilePath(String name, String partition) {
        IPath filePath = null;
        switch (type) {
        case WORKSPACE:
            filePath = new Path(partition).append(com.f5.irule.model.Ids.IRULES_LX_FOLDER);
            break;
        case ILX_RULE:
        case EXTENSION:
        case EXTENSION_FILE:
        case NODE_MODULES_DIR:
        case IAPPLX_MODEL:
        case IAPPLX_MODEL_DIR:
            filePath = parent.getFilePath();
            break;
        default:
            String parentName = parent.getName();
            filePath = constructModuleFilePath(partition, parentName);
            break;
        }
        if (filePath != null) {
            filePath = filePath.append(name);
        }
        return filePath;
    }

    private static IPath constructModuleFilePath(String currentPartition, String parentName) {
        IPath filePath = null;
        if (parentName.equals(Strings.IRULES_LTM_FOLDER_LABEL)) {
            filePath = ExplorerContentProvider.getModuleFolderPath(currentPartition, Module.ltm);
        }
        if (parentName.equals(Strings.IRULES_GTM_FOLDER_LABEL)) {
            filePath = ExplorerContentProvider.getModuleFolderPath(currentPartition, Module.gtm);
        }
        return filePath;
    }

    private void createModel(String name, BigIPConnection conn, IPath filePath) {
        logger.debug("Create " + type + " model: " + filePath);
        String partition = conn.getCurrentPartition();
        switch (type) {
        case WORKSPACE:
            obj = new WorkspaceModelParent(name, conn, partition, filePath);
            addWorkspaceChild(Ids.EXTENSIONS_FOLDER_LABEL, ModelObject.Type.EXTENSION_DIR, conn, partition, filePath, Ids.EXTENSIONS_FOLDER);
            addWorkspaceChild(Ids.RULES_FOLDER_LABEL, ModelObject.Type.IRULE_DIR_ILX, conn, partition, filePath, Ids.RULES_FOLDER);
            break;
        case ILX_RULE:
            filePath = filePath.addFileExtension(RuleProvider.TCL);
            obj = new ILXRuleModelFile(name, conn, partition, type, filePath);
            break;
        case EXTENSION:
            obj = new ModelParent(name, conn, partition, type, filePath);
            obj.addListener(RuleProvider.getListener());
            break;
        case EXTENSION_FILE:
            obj = new ILXModelFile(name, conn, partition, type, filePath);
            break;
        case NODE_MODULES_DIR:
            obj = new ModelParent(name, conn, partition, type, filePath);
            obj.addListener(RuleProvider.getListener());
            break;
        case IAPPLX_MODEL:
            IPath parentFilePath = parent.getFilePath();
            IPath remoteDir = parentFilePath.removeFirstSegments(1);
            IPath remotePath = remoteDir.append(name);
            obj = new IAppsLxModelFile(name, conn, type, remotePath, filePath);
            obj.setLocallyAdded(true);
            break;
        case IAPPLX_MODEL_DIR:
            obj = new ModelParent(name, conn, null, type, filePath);
            obj.setLocallyAdded(true);
            break;
        case GTM_RULE:
        case LTM_RULE:                
            filePath = filePath.addFileExtension(RuleProvider.TCL);
            obj = new RestRule(name, conn, partition, type, filePath);
            ((RestRule) obj).setText("");
            break;
        default:
            logger.warn("Unexpected type " + type + " for model " + name);
            break;
        }
    }

    private void addWorkspaceChild(String childName, Type childType, BigIPConnection conn, String partition, IPath filePath, String dirName) {
        ModelParent exts = new ModelParent(childName, conn, partition, childType, filePath.append(dirName));
        ((ModelParent) obj).addChild(exts);
        exts.addListener(RuleProvider.getListener());
    }

    private void addModel() {
        if (obj == null) {
            return;
        }
        parent.addChild(obj);     
        switch (type) {
        case IAPPLX_MODEL:
            logger.debug("Create file for " + obj);
            createFile(obj.getFile());
            break;
        default:
            BigIPConnection connection = obj.getConnection();
            if (connection.isOnlineMode()) {
                RequestCompletion completion = new NewElementCompletion(obj, parent);
                obj.iControlRestPostJob(completion, Util.getMutex());
            } else {
                logger.debug("Offline Mode. Don't Post " + obj);
                doOfflineAction();
                Util.syncWithUi();
            }
            break;
        }
    }

    private void doOfflineAction() {
        if (obj instanceof ModelParent) {
            IFolder folder = ((ModelParent) obj).getFolder();
            ModelUtils.prepareFolder(folder, false);
            return;
        }

        IFile resource = obj.getFile();
        if (resource == null) {
            logger.warn("No resource file to " + obj);
            return;
        }

        if (!resource.exists()) {
            if (obj instanceof IAppsLxModelFile) {
            }
            else if (obj instanceof ModelFile || obj instanceof RestRule) {
                obj.setLocallyAdded(true);
            }
            else{
                logger.warn("Unexpected model " + obj.getClass().getSimpleName() + " " + obj);
                return;
            }
            createFile(resource);
        }
        openEditor(resource);
    }

    private void createFile(IFile resource) {
        String content = "\n\n";
        byte[] bytes = content.getBytes();
        ModelUtils.prepareFolder((IFolder) resource.getParent(), false);
        final InputStream source = new ByteArrayInputStream(bytes);
        try {
            resource.create(source, true, null);
        } catch (CoreException ex) {
            RestRule.handleError(Messages.CANNOT_CREATE_RESOURCE, ex);
        }
    }

    private class NewElementCompletion extends RequestCompletion {

        private ModelObject model;
        private ModelParent parent;

        public NewElementCompletion(ModelObject modelObject, ModelParent parent) {
            this.model = modelObject;
            this.parent = parent;
        }

        public void completed(String method, String uri, JsonObject jsonBody) {
            logger.debug("Completed " + method + " " + uri);
            IResource resource = null;
            if (model instanceof ModelFile || model instanceof RestRule) {
                model.processResponse(uri, jsonBody, null);
                resource = model.getFile();
            }
            openEditor(resource);

            if (model instanceof ModelParent) {
                if (model.getType().equals(ModelObject.Type.EXTENSION)) {
                    loadExtensionJob((ModelParent)model);                    
                }
                else{
                    model.processResponse(null, null, null);
                }
                Util.syncWithUi();
            }
        }

        public void failed(Exception e, String method, String uri, String responseBody) {
            if (parent != null && model != null) {
                parent.removeChild(model);
            }
            IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN, Strings.ERROR_FAILED_TO_CREATE_RESOURCE, e);
            StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.SHOW);
        }
        
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("[").append(getClass().getSimpleName()).append(" ");
            builder.append(model);
            builder.append(" ");
            builder.append(parent);
            builder.append("]");
            return builder.toString();
        }
    }

    private static void loadExtensionJob(final ModelParent extensionObj) {
        BigIPConnection conn = extensionObj.getConnection();
        ModelParent workspace = extensionObj.findAncestorOfType(ModelObject.Type.WORKSPACE);
        if (conn != null && workspace != null) {
            String currentPartition = conn.getCurrentPartition();
            RequestCompletion completion = new LoadExtensionCompletion(conn, currentPartition, extensionObj);
            String uri = RuleProvider.getIlxWorkspaceUri(conn, conn.getCurrentPartition(), workspace.getName(), extensionObj.getName());
            RestFramework.sendRequestJob(conn, RestMethod.GET, uri, null, null, completion, Util.getMutex());            
        }
    }
    
    private static class LoadExtensionCompletion extends RequestCompletion {
        
        private BigIPConnection conn;
        private String partition;
        private ModelParent extensionObj;
        
        private LoadExtensionCompletion(BigIPConnection conn, String partition, ModelParent extensionObj) {
            this.conn = conn;
            this.partition = partition;
            this.extensionObj = extensionObj;
        }

        public void completed(String method, String uri, JsonObject responseBody) {
            logger.debug("Completed " + method + " " + uri);
            RuleProvider.readILXExtension(conn, partition, extensionObj, responseBody);
        }

        public void failed(Exception ex, String method, String uri, String responseBody) {
            logger.warn("Failed " + method + " " + uri);
            IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN,
                Strings.ERROR_LOADING_ILX_WORKSPACES_FAILED, ex);
            StatusManager.getManager().handle(status, StatusManager.LOG);
        }        
    }
    
    // TODO refactor - make general util action or editor method and use for doubleclick
    private static void openEditor(IResource resource) {
        if (resource == null) {
            return;
        }
        if (!(resource instanceof IFile)) {
            logger.warn("Not and IFile: " + resource);
            return;
        }
        final IFile file = (IFile) resource;
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {                
                IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();             
                try {
                    IDE.openEditor(page, file, true);  // TODO specify editor ID?
                } catch (PartInitException e) {
                    IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN, Strings.ERROR_FAILED_TO_OPEN_EDITOR, e);
                    StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.SHOW);
                }
            }
        });      
    }
    
    private boolean hasInvalidFilenameCharacters(String value) {
        // This was taken verbatim from TMUI
        // Allow letters, digits, underscore, dash and period
        char array[] = value.toCharArray();
        for (int i = 0; i < array.length; i++) {
            if (!Character.isLetterOrDigit(array[i]) && array[i] != '_' && array[i] != '-' && array[i] != '.') {
                return true;
            }
        }
        return false;
    }
}
