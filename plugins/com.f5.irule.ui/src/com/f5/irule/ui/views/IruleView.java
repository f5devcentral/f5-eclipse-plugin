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
package com.f5.irule.ui.views;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.statushandlers.StatusManager;

import com.f5.irule.model.BigIPConnection;
import com.f5.irule.model.DataGroup;
import com.f5.irule.model.DataGroupsModelParent;
import com.f5.irule.model.ModelFile;
import com.f5.irule.model.ModelObject;
import com.f5.irule.model.ModelObject.Type;
import com.f5.irule.model.ModelParent;
import com.f5.irule.model.ModelUtils;
import com.f5.irule.model.Rule;
import com.f5.irule.model.RuleProvider;
import com.f5.irule.ui.Ids;
import com.f5.irule.ui.Strings;
import com.f5.irule.ui.actions.DeleteAction;
import com.f5.irule.ui.actions.ExpandNodeModulesAction;
import com.f5.irule.ui.actions.NewConnectionAction;
import com.f5.irule.ui.actions.NewDataGroupAction;
import com.f5.irule.ui.actions.NewElementAction;
import com.f5.irule.ui.actions.OpenLocalFileAction;
import com.f5.irule.ui.actions.ProjectPropertiesAction;
import com.f5.irule.ui.actions.RestGetOpenFileEditorAction;
import com.f5.irule.ui.actions.SaveFileAction;
import com.f5.irule.ui.actions.SwitchModeAction;
import com.f5.irule.ui.actions.SyncModifiedResourcesAction;
import com.f5.irule.ui.actions.SyncToBigIPAction;
import com.f5.irule.ui.editor.datagroup.CloseDataGroupEditorRunnable;
import com.f5.irule.ui.editor.datagroup.DataGroupEditor;
import com.f5.irule.ui.editor.datagroup.DataGroupEditorInput;
import com.google.gson.JsonObject;

/**
 * An org.eclipse.ui.views extension point that presents the left side navigation view.<br>
 * The navigation view let the user add a Big-IP and modify its resources.<br>
 * It displays all Big-Ip models (iRules, Data-Groups, ILX, iAppsLX) as a tree,<br>
 * where each model is represented by an object in the com.f5.irule.model plugin.
 * <p>
 * The view uses the {@link ViewLabelProvider} label provider<br>
 * to define how its model objects are presented in the tree. (label, icon, etc.)
 */

@SuppressWarnings("unused")
public class IruleView extends ViewPart {

    private static Logger logger = Logger.getLogger(IruleView.class);
    
    private static final String IMAGES_REFRESH_PNG = "/images/refresh.png";
    
    private static TreeViewer viewer;
    private DrillDownAdapter drillDownAdapter;
    private Action newConnectionAction;
    private Action projectPropertiesAction;
    private Action refreshAction;
    private Action doubleClickAction;

    /**
     * The constructor.
     */
    public IruleView() {
    }
    
    public static TreeViewer getTreeViewer() {
        return viewer;
    }

    private static class NameSorter extends ViewerComparator {

        @Override
        public void sort(Viewer viewer, Object[] elements) {            
            Arrays.sort(elements, MODEL_OBJECT_COMPARATOR);
            //super.sort(viewer, elements);
        }
    }
    
    private static final ModelObjectComparator MODEL_OBJECT_COMPARATOR = new ModelObjectComparator();
    /**
     * {@link Comparator} that sorts {@link DataGroupsModelParent} models before other {@link ModelObject} types.<br>
     * or by alphabetically order if its non data-group folder models.
     */
    private static class ModelObjectComparator implements Comparator<Object>{
        @Override
        public int compare(Object obj0, Object obj1) {
            ModelObject model0 = (ModelObject) obj0;
            ModelObject model1 = (ModelObject) obj1;
            String model0Name = model0.getName();
            String model1Name = model1.getName();
            if (model0 instanceof DataGroupsModelParent) {
                if (model1 instanceof DataGroupsModelParent) {
                    return model0Name.compareTo(model1Name);
                } else {
                    return -1;
                }
            } else {
                if (model1 instanceof DataGroupsModelParent) {
                    return 1;
                } else {
                    return model0Name.compareTo(model1Name);
                }
            }
        }
    }

    /**
     * This is a callback that will allow us to create the viewer and initialize it.
     */
    public void createPartControl(Composite parent) {
        viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        drillDownAdapter = new DrillDownAdapter(viewer);
        ExplorerContentProvider provider = new ExplorerContentProvider();
        Util.setExplorerContentProvider(provider);
        viewer.setContentProvider(provider);
        viewer.setLabelProvider(new ViewLabelProvider());
        viewer.setComparator(new NameSorter());
        viewer.setInput(getViewSite());

        // Create the help context id for the viewer's control
        PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "com.f5.irule.ui.viewer");
        makeActions();
        hookContextMenu();
        viewer.addDoubleClickListener(new DoubleClickListener());
        contributeToActionBars();
        
        // In order to enable selection service,
        // so the plugin would list the properties of an element that is selected in the workbench view 
        // the workbench part should register the viewer as the selection provider with the respective view site.
        IWorkbenchPartSite site = getSite();
        site.setSelectionProvider(viewer);

        // Listen for tree updates
    }

    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new FillContextMenuMenuListener(this));
        Menu menu = menuMgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, viewer);
    }
    
    private static class FillContextMenuMenuListener implements IMenuListener{
        private IruleView iruleView;
        private FillContextMenuMenuListener(IruleView iruleView) {
            this.iruleView = iruleView;
        }
        public void menuAboutToShow(IMenuManager manager) {
            iruleView.fillContextMenu(manager);
        }        
    }

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalPullDown(IMenuManager manager) {
        manager.add(new Separator());
        manager.add(projectPropertiesAction);
        manager.add(refreshAction);
    }

    private void fillContextMenu(IMenuManager manager) {
        IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
        Object obj = selection.getFirstElement();
        if (!(obj instanceof ModelObject)) {
            return; // We only care about objects we put there
        }
        Action action;
        ModelObject treeNode = (ModelObject) obj;
        ModelObject.Type type = treeNode.getType();
        boolean onlineMode = treeNode.getConnection().isOnlineMode();
        switch (type) {
        case CONNECTION:
            BigIPConnection connection = (BigIPConnection) obj;
            action = projectPropertiesAction;
            manager.add(action);
            manager.add(new DeleteAction(selection));
            manager.add(new SwitchModeAction(connection));
            if (connection.isOnlineMode() && connection.hasEditedChild()) {
                manager.add(new SyncModifiedResourcesAction(connection));                
            }
            break;
        case DIRECTORY:
            break;
        case EXTENSION:
            action = new NewElementAction(selection, ModelObject.Type.EXTENSION_FILE);
            action.setText(Strings.msg(Strings.LABEL_NEW, Strings.LABEL_CAPITALIZED_FILE));
            action.setDescription(Strings.msg(Strings.LABEL_CREATE_A_NEW, Strings.LABEL_FILE));           
            manager.add(action);
            if (!treeNode.isReadOnly()) {
                manager.add(new DeleteAction(selection));
            }
            break;
        case EXTENSION_DIR:
            action = new NewElementAction(selection, ModelObject.Type.EXTENSION);
            action.setText(Strings.msg(Strings.LABEL_NEW, Strings.LABEL_CAPITALIZED_EXTENSION));
            action.setDescription(Strings.msg(Strings.LABEL_CREATE_A_NEW, Strings.LABEL_EXTENSION)); 
            manager.add(action);
            break;
        case EXTENSION_FILE:
        case GTM_RULE:
        case ILX_RULE:
        case LTM_RULE:
        case IAPPLX_MODEL:
            addFileActions(manager, selection, treeNode);
            if (!treeNode.isReadOnly()) {
                manager.add(new DeleteAction(selection));
            }
            break;
        case IAPPLX_MODEL_DIR:
            addNewElementAction(manager, selection, ModelObject.Type.IAPPLX_MODEL, Strings.LABEL_CAPITALIZED_FILE);
            addNewElementAction(manager, selection, ModelObject.Type.IAPPLX_MODEL_DIR, Strings.LABEL_FOLDER);
            manager.add(new DeleteAction(selection));
            break;
        case IAPPLX_MODEL_PACKAGE:
            addNewElementAction(manager, selection, ModelObject.Type.IAPPLX_MODEL, Strings.LABEL_CAPITALIZED_FILE);
            addNewElementAction(manager, selection, ModelObject.Type.IAPPLX_MODEL_DIR, Strings.LABEL_FOLDER);
            break;
        case IRULE_DIR_ILX:
            addNewElementAction(manager, selection, ModelObject.Type.ILX_RULE, Strings.LABEL_IRULE);
            break;            
        case IRULE_DIR_GTM:
            addNewElementAction(manager, selection, ModelObject.Type.GTM_RULE, Strings.LABEL_IRULE);
            break;
        case IRULE_DIR_LTM:
            addNewElementAction(manager, selection, ModelObject.Type.LTM_RULE, Strings.LABEL_IRULE);
            break;
        
        case DATA_GROUP_MODEL:
            // Add an Action for Adding new Data-Group
            addNewDataGroupAction(manager, selection);
            break;

        case LTM_DATA_GROUP:
        case GTM_DATA_GROUP:
            addFileActions(manager, selection, treeNode);
            addDeleteDataGroupAction(manager, selection);
            break;
            
        case NODE_MODULES_DIR:
            Object ele = selection.getFirstElement();
            if ((ele instanceof ModelParent)) {
                ModelParent nmd = (ModelParent) ele;
                if (!nmd.hasChildren() && nmd.getParent().getType() == ModelObject.Type.EXTENSION) {
                    // Do not expand modules in Offline mode
                    if (onlineMode) {
                        // show this menu item only while not already expanded
                        action = new ExpandNodeModulesAction(nmd);
                        action.setText(Strings.LABEL_EXPAND_FOLDER);
                        manager.add(action);                        
                    }
                } else {
                    // show these menu items only after already expanded
                    action = new NewElementAction(selection, ModelObject.Type.NODE_MODULES_DIR);
                    action.setText(Strings.msg(Strings.LABEL_NEW, Strings.LABEL_CAPITALIZED_FOLDER));
                    action.setDescription(Strings.msg(Strings.LABEL_CREATE_A_NEW, Strings.LABEL_FOLDER));
                    manager.add(action);

                    action = new NewElementAction(selection, ModelObject.Type.EXTENSION_FILE);
                    action.setText(Strings.msg(Strings.LABEL_NEW, Strings.LABEL_CAPITALIZED_FILE));
                    action.setDescription(Strings.msg(Strings.LABEL_CREATE_A_NEW, Strings.LABEL_FILE));
                    manager.add(action);
                }
            }
            break;
        case WORKSPACE:
            if (!treeNode.isReadOnly()) {
                manager.add(new DeleteAction(selection));
            }
            break;
        case WORKSPACE_DIR:
            action = new NewElementAction(selection, ModelObject.Type.WORKSPACE);
            action.setText(Strings.msg(Strings.LABEL_NEW, Strings.LABEL_CAPITALIZED_WORKSPACE));
            action.setDescription(Strings.msg(Strings.LABEL_CREATE_A_NEW, Strings.LABEL_WORKSPACE));           
            manager.add(action);
            break;
        default:
            break;
        }
        
        IContributionItem[] items = manager.getItems();
        logger.debug("Fill Context to " + obj + ". items: " + toString(items));

        /*
         * manager.add(new Separator()); drillDownAdapter.addNavigationActions(manager);
         */

        // Other plug-ins can contribute their actions here
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    /**
     * Add actions based on the connection onlineMode,
     * The model locallyModified flag and the editor state (closed, open or dirty).<br>
     * {@link OpenLocalFileAction} - Action that opens the model IDE from the local file.<br>
     * {@link SyncToBigIPAction} - Action that updates the Big-IP with the model content.<br>
     * {@link SaveFileAction} - Action that saves the editor content to the model file.<br>
     * {@link RestGetOpenFileEditorAction} - Action that gets the file content from the Big-IP,<br>
     * sets it in the local file and open the IDE editor.
     */
    @SuppressWarnings("deprecation")
    private void addFileActions(IMenuManager manager, IStructuredSelection selection, ModelObject treeNode) {

        final ModelObject model = (ModelObject) selection.getFirstElement();
        final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IEditorPart editor = getEditor(page, model);
        BigIPConnection connection = treeNode.getConnection();
        IProject project = connection.getProject();
        boolean onlineMode = connection.isOnlineMode();
        boolean locallyEdited = model.isLocallyEdited();
        boolean editorOpen = editor != null;
        boolean isDirty = editorOpen && editor.isDirty();
        logger.trace("onlineMode=" + onlineMode + " locallyEdited=" + locallyEdited + " isDirty=" + isDirty);
        
        String openFileText = editorOpen ? null :
            (onlineMode ? (locallyEdited ? Strings.LABEL_OPEN_LOCAL_FILE : null) : Strings.LABEL_OPEN);
        Action openFileAction = new OpenLocalFileAction(model, page);
        addAction(manager, openFileAction, openFileText, ISharedImages.IMG_OPEN_MARKER);
        
        String patchContentText = onlineMode ? (isDirty ? Strings.LABEL_SAVE_TO_BIG_IP :
            (locallyEdited ? Strings.LABEL_SYNC_FILE_TO_BIG_IP : null)) : null;
        Action syncToBigIPAction = new SyncToBigIPAction(model, page, editor);
        addAction(manager, syncToBigIPAction, patchContentText, ISharedImages.IMG_TOOL_FORWARD);
        
        String saveLocallyText = isDirty ? (onlineMode ? Strings.LABEL_SAVE_TO_LOCAL_FILE : Strings.LABEL_SAVE) : null;
        Action saveLocalFileAction = new SaveFileAction(model, page, editor, true);
        addAction(manager, saveLocalFileAction, saveLocallyText, ISharedImages.IMG_ETOOL_SAVE_EDIT);
        
        RestGetOpenFileEditorAction getContentAction = new RestGetOpenFileEditorAction(model);
        String getContentActionText = onlineMode ? (editorOpen ? (isDirty ? Strings.LABEL_RELOAD_DISCARD_LOCAL_CHANGES :
                    (locallyEdited ? Strings.LABEL_RELOAD_DISCARD_LOCAL_CHANGES : Strings.LABEL_RELOAD))// Not Dirty
                : (locallyEdited ? Strings.LABEL_RELOAD_DISCARD_LOCAL_CHANGES : Strings.LABEL_OPEN))// Editor not Open
            : null ; // Not Online - Offline Mode
        addAction(manager, getContentAction, getContentActionText, ISharedImages.IMG_TOOL_UNDO);
    }

    private void addAction(IMenuManager manager, Action action, String text, String imageName) {
        if (text == null) {
            return;
        }
        logger.trace("Add Action '" + text + "' " + action);
        action.setText(text);
        Image image = PlatformUI.getWorkbench().getSharedImages().getImage(imageName);
        action.setImageDescriptor(ImageDescriptor.createFromImage(image));
        manager.add(action);
    }

    /**
     * Return open editor of the rule if exists, null otherwise.
     */
    private static IEditorPart getEditor(IWorkbenchPage page, ModelObject model) {

        IFile ruleFile = model.getFile();
        for (IEditorReference iEditorReference : page.getEditorReferences()) {
            final IEditorPart editor = iEditorReference.getEditor(false);
            if (editor != null) {
                IEditorInput editorInput = getEditorInput(iEditorReference);
                if (editorInput != null) {
                    if (editorInput instanceof FileEditorInput) {
                        FileEditorInput fileEditorInput = (FileEditorInput) editorInput;
                        IFile editorFile = fileEditorInput.getFile();
                        if (ruleFile.equals(editorFile)) {
                            return editor;
                        }
                    }
                    if (editorInput instanceof DataGroupEditorInput) {
                        DataGroupEditorInput dataGroupEditorInput = (DataGroupEditorInput) editorInput;
                        DataGroup editorDataGroup = dataGroupEditorInput.getDataGroup();
                        if (model.equals(editorDataGroup)) {
                            return editor;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static IEditorInput getEditorInput(IEditorReference iEditorReference) {
        IEditorInput editorInput = null;
        try {
            editorInput = iEditorReference.getEditorInput();
        } catch (PartInitException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return editorInput;
    }

    protected void addNewElementAction(IMenuManager manager, IStructuredSelection selection, Type modelObjectType,
            String modelLabel) {
        Action action = new NewElementAction(selection, modelObjectType);
        String text = Strings.msg(Strings.LABEL_NEW, modelLabel);
        action.setText(text);
        String description = Strings.msg(Strings.LABEL_CREATE_A_NEW, modelLabel);
        action.setDescription(description);
        manager.add(action);
    }

    private void addNewDataGroupAction(IMenuManager manager, IStructuredSelection selection) {
        DataGroupsModelParent parent = (DataGroupsModelParent) selection.getFirstElement();
        Action action = new NewDataGroupAction(parent);
        String text = Strings.msg(Strings.LABEL_NEW, Strings.LABEL_DATA_GROUP);
        action.setText(text);
        String description = Strings.msg(Strings.LABEL_CREATE_A_NEW, Strings.LABEL_DATA_GROUP);
        action.setDescription(description);
        manager.add(action);
    }

    private void addDeleteDataGroupAction(IMenuManager manager, IStructuredSelection selection) {
        final DataGroup dataGroup = (DataGroup) selection.getFirstElement();
        ImageDescriptor imageDescriptor = getImageDescriptor(ISharedImages.IMG_TOOL_DELETE);
        DeleteAction action = new DeleteAction(selection);
        action.setCompleteRunnable(new CloseDataGroupEditorRunnable(dataGroup));
        manager.add(action);
    }

    private static ImageDescriptor getImageDescriptor(String imageName) {
        ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
        Image addObjImage = sharedImages.getImage(imageName);
        ImageDescriptor imageDescriptor = ImageDescriptor.createFromImage(addObjImage);
        return imageDescriptor;
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        manager.add(newConnectionAction);
        manager.add(projectPropertiesAction);
        manager.add(refreshAction);
        manager.add(new Separator());
        //drillDownAdapter.addNavigationActions(manager);
    }

    private void makeActions() {
        newConnectionAction = new NewConnectionAction();

        projectPropertiesAction = new ProjectPropertiesAction();
        projectPropertiesAction.setText(Strings.LABEL_CONNECTION_PROPERTIES);
        projectPropertiesAction.setToolTipText(Strings.LABEL_EDIT_CONNECTION_PROPERTIES);
        projectPropertiesAction.setImageDescriptor(ImageDescriptor.createFromFile(IruleView.class, "/images/gears.png"));

        refreshAction = new Action() {
            public void run() {
                // Need to close all existing editors before reloading to avoid out of sync issues
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(true);
                
                IContentProvider cp = viewer.getContentProvider();
                if (cp instanceof ExplorerContentProvider) {
                    ((ExplorerContentProvider) cp).initialize();
                }
                viewer.refresh();
            }
        };
        refreshAction.setText(Strings.LABEL_RELOAD_ALL);
        refreshAction.setImageDescriptor(ImageDescriptor.createFromFile(IruleView.class, IMAGES_REFRESH_PNG));

        doubleClickAction = new Action() {
            public void run() {
                ISelection selection = viewer.getSelection();
                Object obj = ((IStructuredSelection) selection).getFirstElement();
                showMessage("Double-click detected on " + obj.toString());
            }
        };
    }

    private static class DoubleClickListener implements IDoubleClickListener {
        public void doubleClick(DoubleClickEvent event) {
            if (event.getSelection() instanceof TreeSelection) {
                TreeSelection selected = (TreeSelection) event.getSelection();
                logger.debug("Double Click on " + selected);
                ModelObject element = (ModelObject) selected.getFirstElement();
                if (element.isLocallyEdited()) {
                    logger.debug(element + " is Edited. Open IDE Editor");
                    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                    IFile file = element.getFile();
                    openIdeEditor(page, file, element);
                } else {
                    RestGetOpenFileEditorAction.getElementResource(element);
                }
            }
        }        
    }

    /**
     * Set the UI thread to open the editor for the given file resource.<br>
     * If the file belongs to a {@link DataGroup} model then use the {@link DataGroupEditor}.<br>
     * Otherwise use the {@link IDE #openEditor(IWorkbenchPage, IFile, boolean)} method.<br>
     * In case of {@link DataGroup} model and connection in Offline mode
     * then update the data-group model from the local file first
     */
    public static void openIdeEditor(IWorkbenchPage page, IFile file, ModelObject model) {
        logger.trace("Async Open Editor for " + model);
        if (file == null) {
            logger.warn("File doesn't exist for " + model);
            ModelUtils.logError(Strings.ERROR_FILE_RESOURCE_DOES_NOT_EXIST + ": " + model, null);
            return;
        }
        IPath location = file.getLocation();
        if (location == null) {
            logger.warn("No location for file " + file);
            ModelUtils.logError(Strings.ERROR_FILE_RESOURCE_DOES_NOT_EXIST + ": " + file,
                StatusManager.LOG, null);
            return;
        }
        String fileExtension = location.getFileExtension();
        Runnable runnable = null;
        if (fileExtension != null && fileExtension.equals(DataGroup.FILE_SUFFIX)){
            DataGroup dataGroup = (DataGroup) model;
            if (dataGroup.isLocallyEdited()) {
                try {
                    logger.debug("Update Data-Group records from " + file);
                    InputStream contents = file.getContents();
                    JsonObject root = RuleProvider.getRootObject(contents);
                    dataGroup.update(root);
                } catch (CoreException ex) {
                    logger.warn("Failed to get contents of " + file, ex);
                }                
            }
            runnable = DataGroupEditor.getOpenEditorRunnable(dataGroup, page);
        }
        else {
            runnable = new OpenIDERunnable(page, file);
        }
        if (runnable != null) {
            Display.getDefault().asyncExec(runnable);
        }
    }
    /**
     * Open an editor on the given file resource.
     * Attempt to resolve the editor based on content-type and name/extension bindings. 
     */
    private static class OpenIDERunnable implements Runnable {

        private IWorkbenchPage page;
        private IFile file;

        private OpenIDERunnable(IWorkbenchPage page, IFile file) {
            this.page = page;
            this.file = file;
        }

        @Override
        public void run() {
            if (!file.exists()) {
                // File does not exist, inform the user with an error message
                logger.warn("Not exist: " + file);
                ModelUtils.logError(Strings.ERROR_FILE_RESOURCE_DOES_NOT_EXIST, null);
                return;
            }
            logger.debug("IDE.openEditor for " + file);
            try {
                IDE.openEditor(page, file, true);
            } catch (PartInitException ex) {
                logger.warn("Failed to open editor for " + file, ex);
                ModelUtils.logError(Strings.ERROR_FAILED_TO_OPEN_EDITOR, ex);
            }
        }
    }

    public static void restGetOpenFileEditor(ModelObject obj, IWorkbenchPage page) {
        IProject proj = obj.getProject();
        if (proj != null) {
            IFile file = obj.getFile();
            if (file != null) {
                if (obj instanceof ModelFile || obj instanceof Rule) {
                    ModelUtils.prepareFolder((IFolder) file.getParent(), false);
                    BigIPConnection connection = (BigIPConnection) obj.getConnection();
                    if (connection.isOnlineMode()) {
                        ProcessResponseJobCompletion completion = new ProcessResponseJobCompletion(
                            obj, page, file, Strings.ERROR_FAILED_TO_RETRIEVE_FILE);
                        obj.iControlRestGetJob(completion, Util.getMutex());
                    } else {
                        logger.debug("Offline Mode, Getting contect from local file " + file);
                        openIdeEditor(page, file, obj);
                    }
                }
            }
        }
    }
    
    private void showMessage(String message) {
        MessageDialog.openInformation(viewer.getControl().getShell(), "Irule View", message);
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    private static String toString(IContributionItem[] items) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            IContributionItem item = items[i];
            if (item instanceof ActionContributionItem) {
                ActionContributionItem actionItem = (ActionContributionItem) item;
                IAction action = actionItem.getAction();
                builder.append(action);
                if (i < items.length - 1) {
                    builder.append(",");
                }
            }
        }
        return builder.toString();
    }
}