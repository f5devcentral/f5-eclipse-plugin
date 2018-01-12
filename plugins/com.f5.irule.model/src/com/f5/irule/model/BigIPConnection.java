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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.Version;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class BigIPConnection extends Connection {

    private static Logger logger = Logger.getLogger(BigIPConnection.class);

    public enum Module  // only the ones we care about so far
    {
        gtm,
        ilx,
        ltm
    }

    private ModelParent gtmFolder = null;
    private ModelParent ltmFolder = null;
    private ModelParent iAppsLxFolder;

    private ArrayList<String> provisionedModules = new ArrayList<String>();
    private ArrayList<String> partitions = new ArrayList<String>();

    private boolean connected = false;
    private Version version;

    /**
     * Flag indicating the mode of this connection.
     * If true, then this connection is in the normal online mode.
     * If false, then the connection is in 'Offline' mode
     */
    private boolean onlineMode;

    /**
     * Counter of running jobs of this connection
     */
    private AtomicInteger jobCounter = new AtomicInteger(0);

    public BigIPConnection(String name, Credentials credentials, ProxyDetails proxyDetails, Path filePath) {
        super(name, credentials, proxyDetails);
        setConnection(this);
        this.onlineMode = PersistentPropertiesUtil.isOnlineMode(this);
    }
    
    public boolean isConnected() {
    	return connected;
    }
    public void setConnected(boolean connected) {
    	this.connected = connected;
    }

    public void setVarsion(Version version) {
        this.version = version;
    }

    public Version getVersion() {
        return version;
    }

    public boolean isOnlineMode() {
        return onlineMode;
    }
    
    public void setOnlineMode(boolean onlineMode) {
        this.onlineMode = onlineMode;
        PersistentPropertiesUtil.setOnlineMode(this, onlineMode);
    }

    /**
     * @return a list of all models that can be written with content from the Big-IP.<br>
     * i.e. models which are not locally edited or that are ReadOnly.
     */
    public List<ModelObject> getWriteableModels(){
        List<ModelObject> list = new LinkedList<ModelObject>();
        getWriteableModels(this, list);
        return list;
    }

    private void getWriteableModels(ModelObject model, List<ModelObject> list) {
        if (model instanceof ModelParent) {
            ModelParent modelParent = (ModelParent) model;
            ModelObject[] children = modelParent.getChildren();
            for (ModelObject child : children) {
                getWriteableModels(child, list);
            }
        } else {
            if (model.isLocallyEdited()) {
                logger.debug(model + " is Locally Edited.");
            }
            else if (model.isReadOnly() && model.getFile().exists()){
                logger.debug("ReadOnly - " + model);
            }
            else{
                list.add(model);                
            }
        }
    }

    @Override
    public List<ModelObject> clearChildren(ModelFilter filter) {
        List<ModelObject> removedList = super.clearChildren(filter);
        if (removedList.contains(gtmFolder)) {
            gtmFolder = null;            
        }
        if (removedList.contains(ltmFolder)) {
            ltmFolder = null;            
        }
        if (removedList.contains(iAppsLxFolder)) {        
            iAppsLxFolder = null;
        }

        version = null;
        provisionedModules = new ArrayList<String>();
        partitions.clear();
        return removedList;
    }

    public ModelParent getModel(Module module, String label) {
        switch (module) {
        case gtm:
            return getGtmFolder(label);
        case ltm:
            return getLtmFolder(label);
        default:
            break;
        }
        return null;
    }
    
    public ModelParent getIAppsLxFolder(String label) {
        if (iAppsLxFolder == null) {
            // Lazy creation of iAppsLxFolder.
            // This method is called only if an iAppLx TEMPLATE block exists.
            IPath path = new Path(Ids.IAPPLX_FOLDER);
            iAppsLxFolder = addModelChild(label, ModelObject.Type.IAPPLX_DIR, path);            
        }
		return iAppsLxFolder;
	}

    public ModelParent getGtmFolder(String label) {
        if (gtmFolder == null) {
            gtmFolder = addModelChild(label, ModelObject.Type.IRULE_DIR_GTM, null);
        }
        return gtmFolder;
    }

    public ModelParent getLtmFolder(String label) {
        if (ltmFolder == null) {
            ltmFolder = addModelChild(label, ModelObject.Type.IRULE_DIR_LTM, null);
        }
        return ltmFolder;
    }

    private ModelParent addModelChild(String label, Type ruleType, IPath path) {
        ModelParent modelChild = new ModelParent(label, this, null, ruleType, path);
        this.addChild(modelChild);
        return modelChild;
    }

    public boolean moduleProvisioned(Module module) {
        String name = module.name();
        return provisionedModules.contains(name);
    }
    
    public ArrayList<String> getPartitions() {
        return partitions;
    }

    public void parseProvisioningInfo(JsonElement jsonBody) {
        JsonElement items = RuleProvider.parseItems(jsonBody);
        if (items != null && items.isJsonArray()) {
            for (JsonElement je : items.getAsJsonArray()) {
                if (je.isJsonObject()) {
                    Set<Entry<String, JsonElement>> ens = ((JsonObject) je).entrySet();
                    String provisionedModule = getProvisionedModule(ens);
                    if (provisionedModule != null) {
                        provisionedModules.add(provisionedModule);
                    }
                }
            }
        }
    }

    private static String getProvisionedModule(Set<Entry<String, JsonElement>> entrySet) {
        
        // Iterate JSON Elements with Key values
        String module = getEntryValue(entrySet, "name");
        if (module.isEmpty()) {
            return null;
        }
        String level = getEntryValue(entrySet, "level");
        if (level.isEmpty() || level.equals("none")) {
            return null;
        }
        return module;
    }
    
    private static String getEntryValue(Set<Entry<String, JsonElement>> entrySet, String expectedKey) {
        for (Entry<String, JsonElement> entry : entrySet) {
            String key = entry.getKey();
            if (key.equals(expectedKey)) {
                JsonElement value = entry.getValue();
                String stringValue = value.getAsString();
                return stringValue;
            }
        }
        return null;
    }

    
    /**
     * Add the partitions items names to the partitions list.
     */
    public void setPartitions(JsonElement items) {
        if (!partitions.isEmpty()) {
            partitions.clear();
        }
        RuleProvider.iterateJsonElementItems(items, new JsonElementVisitor() {
            public void visit(ItemData itemData) {
                if (!itemData.name.isEmpty()) {
                    partitions.add(itemData.name);
                }
            }
        });
        logger.debug(this + " partitions: " + partitions);
    }

    public void addPartition(String partitionValue) {
        logger.debug("Add partition " + partitionValue + " to " + partitions);
        partitions.add(partitionValue);
    }
    
    /**
     * If this connection does not use a proxy, then return a {@link RestURI}<br>
     * containing this connection address and the module and component arguments.<br>
     * If this connection uses a proxy then return a {@link RestURI} without the connection address<br>
     * since the big-ip address is already known to the proxy.
     * Only path is needed.
     */
    @Override
    public RestURI getURI(String module, String component) {
        String serverAddress = isUseProxy() ? null : getAddress();
        RestURI restURI = new RestURI(serverAddress, module, component);
        return restURI;
    }

    /**
     * If this connection does not use a proxy then return a {@link RestURI}<br>
     * containing this connection address and the endpoint argument.<br>
     * If this connection uses a proxy then return a {@link RestURI} without the connection address<br>
     * since the big-ip address is already known to the proxy.
     * Only path is needed.
     */
    public RestURI getURI(String endpoint) {
        String serverAddress = isUseProxy() ? null : getAddress();
        RestURI restURI = new RestURI(serverAddress, endpoint);
        return restURI;
    }

    public void incrementJobCount() {
        jobCounter.incrementAndGet();
    }
    public int decrementJobCount() {
        int ans = jobCounter.decrementAndGet();
        return ans;
    }
    public int getJobCount() {
        int ans = jobCounter.intValue();
        return ans;
    }

    /**
     * Get all of this connection locally modified descendants.<br>
     * The returned list contain only edited file models or edited empty folders.
     */
    public List<ModelObject> getEditedModels() {
        List<ModelObject> list = new LinkedList<ModelObject>();
        getEditedModels(this, list);
        return list;
    }

    /**
     * Identify all the sub models under the given model (including the model itself)<br>
     * that were locally edited (added or modified) and add them to the list.<br>
     * Add only edited file models or edited empty folders.
     */
    private static void getEditedModels(ModelObject model, List<ModelObject> list) {
        if (model instanceof ModelParent) {
            // In case this model is a folder then check if it has any children
            // (i.e. nested files/folders)
            ModelParent modelParent = (ModelParent) model;
            ModelObject[] children = modelParent.getChildren();
            if (children.length == 0) {
                // In case the new dir has no children, it should be added to the list
                // in order to create the empty dir as is.
                if(model.isLocallyEdited()) {
                    list.add(model);
                }
            } else {
                // Otherwise, recursively get the edited models from the children
                // There is no need to add this folder as an edited model
                // since it will be added by the children of this model
                for (ModelObject child : children) {
                    getEditedModels(child, list);
                }
            }
        } else if(model.isLocallyEdited()) {
            // In case this is a file model that was locally edited (added or modified)
            // then add it to the list.
            list.add(model);
        }
    }
    
    /**
     * @return true if this connection has at least one locally edited (new or modified) descendant.
     */
    public boolean hasEditedChild() {
        ModelObject[] children = getChildren();
        return hasEditedChild(children);
    }

    private static boolean hasEditedChild(ModelObject[] array) {
        for (ModelObject model : array) {
            if (model instanceof ModelParent) {
                ModelParent modelParent = (ModelParent) model;
                if (hasEditedChild(modelParent.getChildren())) {
                    return true;
                }
            } else if (model.isLocallyEdited()) {
                return true;
            }
        }
        return false;
    }
}
