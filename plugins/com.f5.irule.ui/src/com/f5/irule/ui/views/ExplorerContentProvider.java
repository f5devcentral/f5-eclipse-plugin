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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Version;

import com.f5.irule.core.TclIruleNature;
import com.f5.irule.model.BigIPConnection;
import com.f5.irule.model.BigIPConnection.Module;
import com.f5.irule.model.Credentials;
import com.f5.irule.model.DataGroup;
import com.f5.irule.model.DataGroupsModelParent;
import com.f5.irule.model.IAppsLxModelFile;
import com.f5.irule.model.ILXModelFile;
import com.f5.irule.model.ILXRuleModelFile;
import com.f5.irule.model.ItemData;
import com.f5.irule.model.ModelFilter;
import com.f5.irule.model.ModelListener;
import com.f5.irule.model.ModelObject;
import com.f5.irule.model.ModelObject.Type;
import com.f5.irule.model.ModelParent;
import com.f5.irule.model.ModelRoot;
import com.f5.irule.model.ProxyDetails;
import com.f5.irule.model.RequestCompletion;
import com.f5.irule.model.RestFramework;
import com.f5.irule.model.RestRule;
import com.f5.irule.model.RestURI;
import com.f5.irule.model.Rule;
import com.f5.irule.model.RuleProvider;
import com.f5.irule.ui.Ids;
import com.f5.irule.ui.Strings;
import com.f5.irule.ui.jobs.ConnectionInitializationCompletion;
import com.f5.irule.ui.jobs.LoadDataGroupsCompletion;
import com.f5.irule.ui.jobs.LoadIAppsLXCompletion;
import com.f5.irule.ui.jobs.LoadILXCompletion;
import com.f5.irule.ui.jobs.LoadIrulesCompletion;
import com.f5.rest.common.RestOperation.RestMethod;
import com.google.gson.JsonObject;

/**
 * Content Provider for the {@link IruleView}.<br>
 * Responsible for populating the model tree and listen for changes on the tree elements.
 */
public class ExplorerContentProvider implements IStructuredContentProvider, ITreeContentProvider, IResourceChangeListener {

    private static Logger logger = Logger.getLogger(ExplorerContentProvider.class);
    
    private static final Object[] EMPTY_OBJECTS_ARRAY = new Object[]{};

    public static final String SYS = "sys";
    public static final String VERSION = "version";
    private static final String WORKSPACE = "workspace";
    
    private Viewer thisView = null;
    ModelRoot invisibleRoot = null;
    RuleProvider ruleProvider = null;

    ExplorerListener listener = new ExplorerListener();

    private static HashMap<IPath, Rule> parsedRules = null;

    public static void addParsedRules(HashMap<IPath, Rule>rules) {
        if (parsedRules == null) {
            parsedRules = rules;                    
        } else {
            parsedRules.putAll(rules);
        }
    }

    // 3 methods from Mastering book
    public void dispose() {
        thisView = null;
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
    }

    public void inputChanged(Viewer v, Object old, Object noo) {
        this.thisView = v;
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
    }

    public void resourceChanged(IResourceChangeEvent event) {
        syncWithUi();
    }

    private class ExplorerListener implements ModelListener /* TODO , Runnable */ {
        public void childAdded(ModelObject parent, ModelObject child) {  // TODO actually called for deletes too, childUpdated?
            syncWithUi();
        }
    }

    /**
     * Refresh the {@link Viewer} of this provider
     * so it would show an updated UI model tree
     */
    private void refresh() {  // TODO XXX Perhaps the impl for this should be syncWithUi
        if (thisView != null) {
            Thread t = Thread.currentThread();
            if (t != null && t.getName().equals("main")) { // TODO is this a propper way to determine the ui thread?
                logger.trace("Refresh UI");
                thisView.refresh();
            }
        }
    }

    public Object[] getElements(Object parent) {
        if (invisibleRoot == null) {
            initialize(); // assumes invisibleRoot is set by initialize()
            parent = invisibleRoot;
        }
        // TODO this probably should return children of parent, not root
        // but that screws up refresh
        return getChildren(invisibleRoot);
    }

    public Object getParent(Object child) {
        if (child instanceof ModelObject) {
            return ((ModelObject) child).getParent();
        }
        return null;
    }

    public Object[] getChildren(Object parent) {
        if (parent instanceof ModelRoot || parent instanceof BigIPConnection){
            return ((ModelParent) parent).getChildren();
        }
        if (parent instanceof ModelParent) {
            return getFilteredChildren(parent);
        }
        return new Object[0];
    }

    /**
     * Return the parents children with the same partition value
     * as the connection current partition<br>
     * Children with no partition (like data-group folder) are also added to the returned array.
     */
    private static Object[] getFilteredChildren(Object parent) {
        ModelParent modelParent = (ModelParent) parent;
        BigIPConnection connection = modelParent.getConnection();
        IProject project = connection.getProject();
        if (!project.isOpen()) {
            logger.warn("Project not open: " + project);
            return EMPTY_OBJECTS_ARRAY;
        }
        String currentPartition = connection.getCurrentPartition();
        ModelObject[] children = modelParent.getChildren();
        List<ModelObject> filteredList = new LinkedList<ModelObject>();
        for (ModelObject child : children) {
            String childPartition = child.getPartition();
            if (childPartition == null || childPartition.equals(currentPartition)) {
                filteredList.add(child);
            }
        }
        Object[] filteredArray = filteredList.toArray();
        return filteredArray;
    }

    public boolean hasChildren(Object parent) {
        if (parent instanceof ModelParent)
            return ((ModelParent) parent).hasChildren();
        return false;
    }

    public ModelRoot getRoot() {
        return invisibleRoot;
    }
    
    public RuleProvider getRuleProvider() {
        return ruleProvider;
    }

    public ExplorerListener getListener() {
        return listener;
    }

    private static HashMap<IPath, Rule> getRules(String ip) {
        if (parsedRules == null) {
            return null;
        }
        HashMap<IPath, Rule> rulesWithIp = new HashMap<IPath, Rule>();
        for (Map.Entry<IPath, Rule> entry : parsedRules.entrySet()) {
            IPath key = entry.getKey();
            String str = key.segment(0).toString();
            if (str.equals(ip)) {
                rulesWithIp.put(key, parsedRules.get(key));
            }
        }
        return rulesWithIp;
    }

    public void initialize() {
        IWorkspaceRoot ws = ResourcesPlugin.getWorkspace().getRoot();
        IProject[] projects = ws.getProjects();

        ruleProvider = new RuleProvider(listener);

        // Initialize invisible root on the first time only
        if (invisibleRoot == null) {
            invisibleRoot = ModelRoot.getInstance();
            invisibleRoot.clearChildren(null);
            invisibleRoot.addListener(listener);
        }

        parsedRules = null;

        for (int i = 0; i < projects.length; i++) {
            IProject project = projects[i];
            if (skipProject(project)) {
                continue;
            }
            String name = project.getName();
            IStatus status = new Status(IStatus.INFO, Ids.PLUGIN, Strings.INFO_INIT_PROJECT + name);
            StatusManager.getManager().handle(status, StatusManager.LOG);
            // creds object created from those stored on the filesystem
            Credentials credentials = new Credentials(project);
            ProxyDetails proxyDetails = new ProxyDetails(project);
            // Create connection and load it's contents
            loadConnection(name, credentials, proxyDetails, project, null, null);
        }
    }

    private static boolean skipProject(IProject project) {
        try {
            if (project.hasNature(com.f5.irule.model.Ids.NATURE_ID) == false) {
                // Only display F5 projects
                return true;
            }
        } catch (CoreException e) {
            return true;
        }
        return false;
    }

    class Mutex implements ISchedulingRule {
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("[Mutex ");
            builder.append(ExplorerContentProvider.class.getSimpleName()).append("]");
            return builder.toString();
        }

        // TODO this rule makes every job exclusive, see if we can be more nuanced
        public boolean isConflicting(ISchedulingRule rule) {
            boolean ans = rule == this;
            return ans;
        }

        public boolean contains(ISchedulingRule rule) {
            return rule == this;
        }
    }

    final ISchedulingRule mutex = new Mutex();

    public ISchedulingRule getMutex() {
        return mutex;
    }

    public static void reloadConnection(final BigIPConnection connection) {
        logger.debug("Reload " + connection + ", go Online");
        connection.setOnlineMode(true);
        IProject project = connection.getProject();
        ExplorerContentProvider provider = Util.getExplorerContentProvider();
        Credentials credentials = new Credentials(project);
        ProxyDetails proxyDetails = new ProxyDetails(project);
        provider.loadConnection(connection.getName(), credentials, proxyDetails, project, null, null);
    }

    public void loadConnection(final String name, final Credentials credentials, final ProxyDetails proxyDetails,
            final IProject project, final Shell shell, final RequestCompletion parentCompletion) {
        Job loadConnectionJob = new Job(Strings.JOB_LOAD_CONNECTION_JOB){
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                doLoadConnection(name, credentials, proxyDetails, project, shell, parentCompletion);
                return Status.OK_STATUS;
            }
        };
        loadConnectionJob.schedule();
    }

    /**
     * Load a {@link BigIPConnection} into the project.<br>
     * If it's a new connection add it to the explorer tree,
     * otherwise clear its children before filling it again with updated data.<br>
     * If the connection is in Online Mode, send a Rest get version request
     * in order to start process of getting data from the Big-IP.<br>
     * If the connection is in Offline Mode, then load the explorer from local files. 
     */
    void doLoadConnection(String name, Credentials credentials, ProxyDetails proxyDetails, IProject project,
        Shell shell, RequestCompletion parentCompletion) {
        // The project arg can be null if adding a connection which already has an associated project
        BigIPConnection conn = (BigIPConnection) invisibleRoot.getChild(name);
        if (conn == null) {
            logger.debug("Create Connection " + name);
            conn = new BigIPConnection(name, credentials, proxyDetails, new Path(name));
            invisibleRoot.addChild(conn);
        }
        else{
            int jobCount = conn.getJobCount();
            if (jobCount > 0) {
                // Start connection reloading only for the first reload button press.
                // If the user press the reload button multiple times the job count would be greater than 0.
                // In that case only start reloading for the first and ignore the subsequent times.
                logger.warn("Already loading " + conn + ". jobCount=" + jobCount);
                return;                
            }
        }
        loadFromFiles(conn);
        if (conn.isOnlineMode()) {
            logger.debug("Load " + conn);
            setWaitCursur(shell, true);
            String uriString = conn.getURI(SYS, VERSION).toString();
            RequestCompletion completion = new ConnectionInitializationCompletion(conn, project, parentCompletion, shell);
            RestFramework.sendRequestJob(conn, RestMethod.GET, uriString, null, null, completion, mutex);
        }
    }

    /**
     * Asynchronously set Cursor on given Shell.
     * if true, set it to {@link SWT #CURSOR_WAIT}
     * Otherwise set to null (back to regular cursor)
     */
    public static void setWaitCursur(final Shell shell, final boolean busy){
        if (shell == null) {
            return;
        }
    	Display.getDefault().asyncExec(new Runnable() {
    		@Override
    		public void run() {
    			if(busy){ //show Busy Cursor
    				Cursor cursor = Display.getDefault().getSystemCursor(SWT.CURSOR_WAIT);          
    				shell.setCursor(cursor);
    			}else{  
    				shell.setCursor(null);
    			}   
    		}
    	});
    }

    public void loadResources(final BigIPConnection conn) {

        // Load iRules and ILX Workspaces from the bigip
        if (conn.moduleProvisioned(Module.gtm)) {
            loadModuleJobs(conn, Module.gtm);
        }
        
        if (conn.moduleProvisioned(Module.ltm)) {
            loadModuleJobs(conn, Module.ltm);
        }

        if (conn.moduleProvisioned(Module.ilx)) {
            RestURI uri = conn.getURI(BigIPConnection.Module.ilx.name(), WORKSPACE);
            String uriValue = uri.toString();
            LoadILXCompletion loadILXCompletion = new LoadILXCompletion(conn, this);
            RestFramework.sendRequestJob(conn, RestMethod.GET, uriValue, null, null, loadILXCompletion, mutex);
        }

        Version version = conn.getVersion();
        if (shouldLoadIAppLXREadJob(version)) {
            String endpoint = RestFramework.IAPP_DIRECTORY_MANAGEMENT;
            String uri = conn.getURI(endpoint).toString();
            RequestCompletion completion = new LoadIAppsLXCompletion(conn);
            RestFramework.sendRequestJob(conn, RestMethod.GET, uri, null, null, completion, mutex);
        }
    }

    /** Schedule Load iRules Job.
     * Schedule Load Data-Groups Job. 
     * @param partition */
    private void loadModuleJobs(BigIPConnection conn, Module module) {
        RequestCompletion completion = new LoadIrulesCompletion(conn, module, this);
        String select = "name,apiAnonymous,partition,fullPath";
        RestURI ruleUri = conn.getURI(module.name(), RestRule.RULE);
        ruleUri.addSelect(select);
        String uriString = ruleUri.toString();
        RestFramework.sendRequestJob(conn, RestMethod.GET, uriString, null, null, completion, mutex);
        switch (module) {
		case ltm:
	        RestURI dataGroupUri = conn.getURI(module.name(), DataGroup.DATA_GROUP);
	        dataGroupUri.append("internal");
	        String uri = dataGroupUri.toString();
	        LoadDataGroupsCompletion loadDataGroupsCompletion = new LoadDataGroupsCompletion(conn, module, this);
            RestFramework.sendRequestJob(conn, RestMethod.GET, uri, null, null, loadDataGroupsCompletion, mutex);			
			break;
		default:
			break;
		}
    }

    public void fillExplorerIrules(BigIPConnection conn, Module module) {
        String address = conn.getAddress();
        HashMap<IPath, Rule> rules = getRules(address);
        if (rules == null) {
            return;
        }
        
        ModelParent tmModel = getModel(conn, module);
        Type tmType = tmModel.getType();

        // Clear the current rules from the rules folder
        // Before updating the folder with the items from the REST response
        // so in case a rule was deleted on the Big-IP it would be reflected in the UI tree.
        // Use the RuleModelFilter so it would clear just the rules children of the model dir
        // and not the Data-Group dir.
        tmModel.clearChildren(new RuleModelFilter(tmType));

        ModelObject.Type modelParentType = tmModel.getType();
        for (Map.Entry<IPath, Rule> entry : rules.entrySet()) {
            IPath key = entry.getKey();
            Rule parsedRule = parsedRules.get(key);
            addParsedModel(parsedRule, tmModel, modelParentType);
        }
        tmModel.addListener(listener);
        // Now sync the UI thread with the updated model
        syncWithUi();
    }
    
    /**
     * A {@link ModelFilter} that applies for iRules only.
     */
    private static class RuleModelFilter implements ModelFilter {
        private Type modelDirType;

        private RuleModelFilter(Type modelDirType) {
            this.modelDirType = modelDirType;
        }

        public boolean applyModel(ModelObject model) {
            Type type = model.getType();
            boolean ans =
                (type == Type.LTM_RULE && modelDirType == Type.IRULE_DIR_LTM) ||
                (type == Type.GTM_RULE && modelDirType == Type.IRULE_DIR_GTM);
            return ans;
        }        
    }

    /**
     * Fill the explorer Model tree with the data-groups that were received
     * in the REST response of the REST GET data-group request.
     */
    public void fillExplorerDataGroups(BigIPConnection conn, Module module, HashMap<IPath, DataGroup> dataGroups) {
        
        ModelParent tmModel = getModel(conn, module);
        ModelObject.Type tmType = tmModel.getType();
        
        // Add Data-Groups to explorer
        ModelParent dataGroupsModel = getDataGroupModel(conn, module, tmModel);
        
        // Add the ExplorerListener so after deleting a Data-Group using the right-click Delete action
        // it would refresh the view tree and remove the Data-Group item from the Data Groups sub list. 
        dataGroupsModel.addListener(listener);

        // Clear the current Data-Group models from the Data-Group folder
        // Before updating the folder with the items from the REST response
        // so in case a Data-Group model was deleted on the Big-IP side
        // it would be reflected in this plug-in.
        dataGroupsModel.clearChildren(null);
        
        if (dataGroups != null) {
            for (Map.Entry<IPath, DataGroup> entry : dataGroups.entrySet()) {
                IPath key = entry.getKey();
                DataGroup parsedDataGroup = dataGroups.get(key);
                addParsedModel(parsedDataGroup, dataGroupsModel, tmType);
            }
        }

        // Now sync the UI thread with the updated model
        syncWithUi();
    }

    private static ModelParent getDataGroupModel(BigIPConnection conn, Module module, ModelParent parentFolder) {

        ModelParent dataGroupsModel = (ModelParent) parentFolder.getChild(Strings.LABEL_DATA_GROUPS);
        if (dataGroupsModel == null) {
            dataGroupsModel = new DataGroupsModelParent(Strings.LABEL_DATA_GROUPS,
                conn, null, Type.DATA_GROUP_MODEL, module);            
            parentFolder.addChild(dataGroupsModel);
        }
        return dataGroupsModel;
    }

    private void addParsedModel(ModelObject model, ModelParent modelParent, ModelObject.Type tmType) {
        if (!modelRuleType(model, tmType)) {
            return;
        }
        String modelName = model.getName();
        ModelObject previousModel = modelParent.getChild(modelName);
        if (previousModel == null) {
            modelParent.addChild(model);                       
        } else {
            if (previousModel.isLocallyEdited()) {
                logger.debug("Locally Edited - " + previousModel);
            } else {
                modelParent.removeChild(previousModel);
                modelParent.addChild(model);
            }
        }
    }

    private static boolean modelRuleType(ModelObject model, ModelObject.Type modelParentType) {
        ModelObject.Type ruleType = model.getType();
        switch (ruleType) {
        case GTM_RULE:
        case GTM_DATA_GROUP:
            return modelParentType == ModelObject.Type.IRULE_DIR_GTM;
        case LTM_RULE:
        case LTM_DATA_GROUP:
            return modelParentType == ModelObject.Type.IRULE_DIR_LTM;
        case ILX_RULE:
            return modelParentType == ModelObject.Type.IRULE_DIR_ILX;
        default:
            break;
        }
        return false;
    }
    
    private static final String[] NATURE_IDS = {
        com.f5.irule.model.Ids.NATURE_ID,
        TclIruleNature.NATURE_ID,
        org.eclipse.wst.jsdt.core.JavaScriptCore.NATURE_ID
    };

    /**
     * Open the project on disk and store authentication
     */
    public static void openProject(BigIPConnection conn, IProject project) {
        if (project == null) {
            return;
        }
        try {
            String projectName = conn.getAddress();
            if (!project.exists()) {
                IProjectDescription desc = project.getWorkspace().newProjectDescription(projectName);
                desc.setNatureIds(NATURE_IDS);
                project.create(desc, null);
            }
            if (!project.isOpen()) {
                project.open(null);
            }            
            // Now that we have a project, store the creds
            conn.storeCredentials(project);
            conn.storeProxyDetails(project);
            conn.setConnected(true);
        } catch (Exception e) {
            IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN, Strings.ERROR_FAILED_TO_CREATE_RESOURCE);
            StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.SHOW);
            return;
        }
    }

    /**
     * Run the {@link #refresh()} method with the UI main thread<br>
     * so it would show an updated UI model tree.<br>
     * Use a counter so no more than 1 refresh runnable would be set at a time.
     */
    public void syncWithUi() {
        int counter = syncCounter.incrementAndGet();
        if (counter > 1) {
            logger.trace("Sync Counter: " + counter);
        } else {
            logger.trace("Sync With UI");
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    int finishCounter = syncCounter.get();
                    logger.trace("Sync Finish Counter: " + finishCounter);
                    syncCounter.set(0);
                    refresh();
                }
            });
        }
    }
    private AtomicInteger syncCounter = new AtomicInteger(0);

    private static Version iAppsLxMinVersion = new Version(13, 1, 0);
    private static boolean shouldLoadIAppLXREadJob(Version version) {
        if (version == null) {
            logger.warn("No Version");
            return false;
        }
        int versionMajor = version.getMajor();
        int iAppsLxMinVersionMajor = iAppsLxMinVersion.getMajor();
        if (versionMajor < iAppsLxMinVersionMajor) {
            return false;
        }
        if (versionMajor > iAppsLxMinVersionMajor) {
            return true;
        }
        int versionMinor = version.getMinor();
        int iAppsLxMinVersionMinor = iAppsLxMinVersion.getMinor();
        if (versionMinor < iAppsLxMinVersionMinor) {
            return false;
        }
        if (versionMinor > iAppsLxMinVersionMinor) {
            return true;
        }
        int versionMicro = version.getMicro();
        int iAppsLxMinVersionMicro = iAppsLxMinVersion.getMicro();
        if (versionMicro < iAppsLxMinVersionMicro) {
            return false;
        }
        if (versionMicro > iAppsLxMinVersionMicro) {
            return true;
        }
        return true;
    }

    /**
     * Iterate over the files of the connection {@link IProject}
     * and load the connection models from the file
     */
    public static void loadFromFiles(final BigIPConnection conn) {
        conn.clearChildren(null);
        logger.debug("Load models of " + conn + " from files");
        IProject proj = conn.getProject();
        
        File[] partitionsDirs = getPartitionsDirs(proj);
        if (partitionsDirs != null) {
            //String partition = conn.getCurrentPartition();
            for (File dir : partitionsDirs) {
                String dirName = dir.getName();
                IFolder folder = proj.getFolder(dirName);
                try {
                    if (dirName.equals(com.f5.irule.model.Ids.IAPPLX_FOLDER)) {
                        addIAppLX(conn, folder);
                    }
                    else if (folder.exists()) {
                        loadPartition(conn, dirName, folder);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        Util.syncWithUi();
    }

    private static File[] getPartitionsDirs(IProject proj) {
        IPath location = proj.getLocation();
        if (location == null) {
            return null;
        }
        File projectDir = location.toFile();
        File[] partitionsDirs = projectDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        return partitionsDirs;
    }

    private static void loadPartition(BigIPConnection conn, String partition, IFolder folder) throws CoreException, IOException {

        logger.debug("Load Partition " + partition + " of " + conn);
        conn.addPartition(partition);
        IResource[] members = folder.members();
        for (IResource iResource : members) {
            String resourceName = iResource.getName();
            if (iResource instanceof IFolder) {
                IFolder resourceFolder = (IFolder) iResource;
                if (resourceName.equals(com.f5.irule.model.Ids.IRULES_LTM_FOLDER)) {
                    addModuleFolder(conn, partition, resourceFolder,
                        Strings.IRULES_LTM_FOLDER_LABEL, Module.ltm, Type.LTM_RULE);
                }
                else if (resourceName.equals(com.f5.irule.model.Ids.IRULES_GTM_FOLDER)) {
                    addModuleFolder(conn, partition, resourceFolder,
                        Strings.IRULES_GTM_FOLDER_LABEL, Module.gtm, Type.GTM_RULE);
                }
                else if (resourceName.equals(com.f5.irule.model.Ids.IRULES_LX_FOLDER)) {
                    loadILX(conn, partition, resourceFolder);
                }
                else{
                    logger.warn("Unexpected resource " + resourceFolder);
                }
            }
        }
    }

    private static void loadILX(BigIPConnection conn, String partitionName, IFolder resourceFolder) throws CoreException {
        logger.debug("Load ILX from " + resourceFolder);
        ModelParent ilx = ExplorerContentProvider.getIlxModule(conn, partitionName);
        IPath ilxPath = ilx.getFilePath();
        IResource[] workspaceResources = resourceFolder.members();
        for (IResource workspaceResource : workspaceResources) {
            loadWorkspace(conn, partitionName, ilx, ilxPath, workspaceResource);
        }
    }

    public static ModelParent getIlxModule(BigIPConnection conn, String partition) {
        IPath workspacesPath = new Path(partition);
        workspacesPath = workspacesPath.append(com.f5.irule.model.Ids.IRULES_LX_FOLDER);
        ModelParent ilx = (ModelParent) conn.getChild(Strings.ILX_WORKSPACES_FOLDER);
        if (ilx == null) {
            ilx = new ModelParent(Strings.ILX_WORKSPACES_FOLDER,
                conn, partition, ModelObject.Type.WORKSPACE_DIR, workspacesPath);
            conn.addChild(ilx);            
        }
        return ilx;
    }

    private static void addIAppLX(BigIPConnection conn, IFolder resourceFolder) throws CoreException {

        ModelParent iAppsLxParent = conn.getIAppsLxFolder(Strings.IAPPLX_FOLDER_LABEL);
        logger.debug("Load IAppLX from " + resourceFolder + " to " + iAppsLxParent);
        IResource[] iAppsLxFolders = resourceFolder.members();
        for (IResource iAppsLxFolder : iAppsLxFolders) {
            String modelName = iAppsLxFolder.getName();
            IPath path = new Path(com.f5.irule.model.Ids.IAPPLX_FOLDER);
            IPath filePath = path.append(modelName);
            ModelParent iAppsLxModel = new ModelParent(modelName, conn, null, Type.IAPPLX_MODEL_PACKAGE, filePath);
            iAppsLxParent.addChild(iAppsLxModel);
            Path remoteDir = new Path(modelName);
            loadIAppsLxItems(conn, iAppsLxModel, (IFolder) iAppsLxFolder, remoteDir);
        }
    }

    private static void loadIAppsLxItems(BigIPConnection conn,
            ModelParent parentModel, IFolder iAppsLxFolder, IPath remoteDir) throws CoreException {

        IPath parentPath = parentModel.getFilePath();
        IResource[] children = iAppsLxFolder.members();
        for (IResource child : children) {
            String childName = child.getName();
            IPath remotePath = remoteDir.append(childName);
            ModelObject modelChild;
            if (child instanceof IFolder) {
                IPath path = parentPath.append(childName);
                modelChild = new ModelParent(childName, conn, null, Type.IAPPLX_MODEL_DIR, path);
                loadIAppsLxItems(conn, (ModelParent) modelChild, (IFolder) child, remotePath);
            } else {
                IPath path = parentPath.append(childName);
                modelChild = new IAppsLxModelFile(childName, conn, Type.IAPPLX_MODEL, remotePath, path);
            }
            parentModel.addChild(modelChild);
        }
    }

    private static void loadWorkspace(BigIPConnection conn, String partition, ModelParent ilxParent, IPath path, IResource resource) throws CoreException {
        String wsName = resource.getName();
        logger.debug("Load Workspace " + wsName + " of " + ilxParent);
        IPath workspacePath = path.append(wsName);
        ModelParent ws = RuleProvider.getModelParent(wsName,
            conn, partition, ModelObject.Type.WORKSPACE, workspacePath, ilxParent);

        IPath extensionsFolderPath = workspacePath.append(com.f5.irule.model.Ids.EXTENSIONS_FOLDER);
        ModelParent extensionsParent = RuleProvider.getModelParent(com.f5.irule.model.Ids.EXTENSIONS_FOLDER_LABEL,
            conn, partition, ModelObject.Type.EXTENSION_DIR, extensionsFolderPath, ws);

        IPath rulesFolderPath = workspacePath.append(com.f5.irule.model.Ids.RULES_FOLDER);
        ModelParent rulesParent = RuleProvider.getModelParent(com.f5.irule.model.Ids.RULES_FOLDER_LABEL,
            conn, partition, ModelObject.Type.IRULE_DIR_ILX, rulesFolderPath, ws);

        IFolder workspaceFolder = (IFolder) resource;
        IResource[] workspaceChildren = workspaceFolder.members();
        for (IResource workspaceChild : workspaceChildren) {
            String childName = workspaceChild.getName();
            if (childName.equals(com.f5.irule.model.Ids.EXTENSIONS_FOLDER)) {
                loadWorkspaceExtensions(conn, partition, extensionsFolderPath, extensionsParent, workspaceChild, wsName);
            }
            else if(childName.equals(com.f5.irule.model.Ids.RULES_FOLDER)){
                loadWorkspaceRules(conn, partition, rulesFolderPath, rulesParent, workspaceChild, wsName);
            }
        }
    }

    private static void loadWorkspaceExtensions(BigIPConnection conn, String partition, IPath path, ModelParent parent, IResource resource, String wsName) throws CoreException {
        logger.debug("Load extensions of " + wsName);
        IFolder extensionsFolder = (IFolder) resource;
        IResource[] extensionsFiles = extensionsFolder.members();
        for (IResource extension : extensionsFiles) {
            addIlxExtension(conn, partition, path, parent, extension);
        }
    }

    private static void loadWorkspaceRules(BigIPConnection conn, String partition, IPath path, ModelParent parent, IResource resource, String wsName) throws CoreException {
        logger.debug("Load rules of " + wsName);
        IFolder rulesFolder = (IFolder) resource;
        IResource[] rulesFiles = rulesFolder.members();
        for (IResource rule : rulesFiles) {
            String ruleName = rule.getName();
            String name = ruleName.substring(0, ruleName.length() - 4); // Remove .tcl suffix from name
            ILXRuleModelFile.processIlxRule(conn, partition, name, path, parent);
        }
    }

    private static void addIlxExtension(BigIPConnection conn, String partition, IPath path, ModelParent parent, IResource resource) throws CoreException {
        String extensionName = resource.getName();
        logger.debug("Load extension " + extensionName);
        IPath extensionPath = path.append(extensionName);
        ModelParent extensionParent = RuleProvider.getModelParent(
            extensionName, conn, partition, ModelObject.Type.EXTENSION, extensionPath, parent);
        IFolder extensionFolder = (IFolder) resource;
        IResource[] extensionFiles = extensionFolder.members();
        for (IResource extensionFile : extensionFiles) {
            String extensionFileName = extensionFile.getName();
            ILXModelFile.parseIlxItem(conn, partition, extensionParent, extensionPath, extensionFileName);
        }
    }

    private static void addModuleFolder(BigIPConnection conn, String partition, IFolder resourceFolder,
            String moduleLabel, Module module, Type ruleType) throws CoreException, IOException {

        IPath parentPath = getModuleFolderPath(partition, module);
        
        ModelParent parent = getModel(conn, module);
        IResource[] resourceFolderMembers = resourceFolder.members();
        for (IResource resourceFolderMember : resourceFolderMembers) {
            String memberName = resourceFolderMember.getName();
            if (resourceFolderMember instanceof IFolder) {
                if (memberName.equals(DataGroup.FOLDER_NAME)) {
                    addDataGroupFolder(conn, partition, module, parent, resourceFolderMember);
                }
            }
            if (resourceFolderMember instanceof IFile) {
                IFile memberFile = (IFile) resourceFolderMember;
                String fileExtension = resourceFolderMember.getFileExtension();
                if (fileExtension != null && fileExtension.equals(RuleProvider.TCL)) {
                    loadIRuleModel(memberName, conn, partition, parent, ruleType, memberFile, parentPath);
                }
            }
        }
    }

    public static IPath getModuleFolderPath(String partition, Module module) {
        if (module == null) {
            return null;
        }
        String folderId = module.equals(Module.gtm) ?
            com.f5.irule.model.Ids.IRULES_GTM_FOLDER :
            com.f5.irule.model.Ids.IRULES_LTM_FOLDER;
        IPath parentPath = new Path(partition).append(folderId);
        return parentPath;
    }

    private static ModelParent getModel(BigIPConnection conn, Module module) {
        String label = module.equals(Module.gtm) ?
            Strings.IRULES_GTM_FOLDER_LABEL :
            Strings.IRULES_LTM_FOLDER_LABEL;
        return conn.getModel(module, label);
    }

    private static void addDataGroupFolder(BigIPConnection conn, String partition, Module module,
            ModelParent parentFolder, IResource resource) throws CoreException {
        ModelParent dataGroupsModel = ExplorerContentProvider.getDataGroupModel(conn, module, parentFolder);
        IFolder dataGoupFolder = (IFolder) resource;
        IResource[] dataGroupMembers = dataGoupFolder.members();
        for (IResource dataGroupMember : dataGroupMembers) {
            loadDataGroupModel(conn, partition, module, dataGroupsModel, dataGroupMember);
        }
    }

    private static void loadIRuleModel(String ruleName, BigIPConnection conn, String partition,
            ModelParent parent, Type ruleType, IFile file, IPath modelPatentPath) throws IOException, CoreException {
        IPath path = modelPatentPath.append(ruleName);
        ModelObject model = conn.getModel(null, path);
        if (model != null) {
            logger.debug("Already have model " + model);
            return;
        }

        InputStream fileContents = file.getContents();
        String ruleBody = RestFramework.inputStreamToString(fileContents);
        String name = ruleName.substring(0, ruleName.length() - 4); // Remove .tcl suffix from name
        RestRule rule = RuleProvider.getRestRule(name, conn, ruleType, path, partition, ruleBody);                 
        parent.addChild(rule);
    }

    private static void loadDataGroupModel(BigIPConnection conn, String partition, Module module,
            ModelParent dataGroupsModel, IResource dataGroupMember) throws CoreException {

        IFile dataGroupFile = (IFile) dataGroupMember;
        InputStream fileContents = dataGroupFile.getContents();
        JsonObject jsonObject = RuleProvider.getRootObject(fileContents);
        ItemData itemData = ItemData.getData(jsonObject);
        Type type = RuleProvider.getDataGroupType(module);
        IPath dataGroupPath = getModuleFolderPath(partition, module).append(DataGroup.FOLDER_NAME);
        DataGroup dataGroup = new DataGroup(itemData.name, itemData.type, conn, partition, type, module, dataGroupPath);
        dataGroup.update(jsonObject);
        dataGroupsModel.addChild(dataGroup);
    }
}
