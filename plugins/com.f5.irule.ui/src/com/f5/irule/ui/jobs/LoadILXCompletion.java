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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import com.f5.irule.model.BigIPConnection;
import com.f5.irule.model.RequestCompletion;
import com.f5.irule.model.ILXModelFile;
import com.f5.irule.model.ILXRuleModelFile;
import com.f5.irule.model.Ids;
import com.f5.irule.model.ItemData;
import com.f5.irule.model.JsonElementVisitor;
import com.f5.irule.model.ModelObject;
import com.f5.irule.model.ModelParent;
import com.f5.irule.model.RuleProvider;
import com.f5.irule.ui.Strings;
import com.f5.irule.ui.views.ExplorerContentProvider;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class LoadILXCompletion extends RequestCompletion {

    private static Logger logger = Logger.getLogger(LoadILXCompletion.class);

    private String currentPartition;
    private BigIPConnection conn;
    private ExplorerContentProvider provider;

    public LoadILXCompletion(BigIPConnection conn, ExplorerContentProvider provider) {
        this.currentPartition = conn.getCurrentPartition();
        this.conn = conn;
        this.provider = provider;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(getClass().getSimpleName());
        builder.append(" ").append(conn);
        builder.append("]");
        return builder.toString();
    }

    public void completed(String method, String uri, JsonObject jsonBody) {
        ModelParent ilx = ExplorerContentProvider.getIlxModule(conn, currentPartition);
        logger.debug("Completed " + method + " " + uri +
            "\nParse ILX response for " + conn + " " + ilx);
        ilx.addListener(provider.getListener());
        parseILXWorkspaces(conn, ilx, jsonBody);
    }

    public void failed(Exception ex, String method, String uri, String responseBody) {
        IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN, Strings.ERROR_LOADING_ILX_WORKSPACES_FAILED, ex);
        StatusManager.getManager().handle(status, StatusManager.LOG);
    }

    /**
     * Iterate over the response items and from each one<br>
     * parse an ILX workspace that includes two children folders: extensions and rules.<br>
     * and in each one corresponding ILX resources.
     */
    private static void parseILXWorkspaces(final BigIPConnection conn,
            final ModelParent parent, JsonElement jsonBody) {
        JsonElement items = RuleProvider.parseItems(jsonBody);
        RuleProvider.iterateJsonElementItems(items, new JsonElementVisitor() {
            public void visit(ItemData itemData) {
                if (itemData != null &&
                        itemData.partition != null &&
                        itemData.name != null &&
                        itemData.name.length() > 0) {
                    parseIlxItem(itemData, conn, parent);
                }
            }
        });
    }

    /**
     * Parse the ILX item.<br>
     * Create a {@link ModelParent} dir with the item partition and name and add it to the tree.<br>
     * Create two children folders: extensions and rules.<br>
     * Iterate over the item extensions, process each one and add it to the extension dir.<br>
     * Iterate over the item rules, create an {@link ILXRuleModelFile} for each rule and add it to the rules dir. 
     */
    private static void parseIlxItem(ItemData itemData, final BigIPConnection conn, ModelParent parent) {

        final String partition = itemData.partition;
        IPath workspacePath = new Path(partition).append(com.f5.irule.model.Ids.IRULES_LX_FOLDER).append(itemData.name);
        logger.trace("Parse to " + workspacePath + ":\n" + itemData);
        ModelParent ws = RuleProvider.getModelParent(itemData.name,
            conn, partition, ModelObject.Type.WORKSPACE, workspacePath, parent);

        // Folders
        final IPath extensionsFolderPath = workspacePath.append(com.f5.irule.model.Ids.EXTENSIONS_FOLDER);
        final ModelParent extensionsParent = RuleProvider.getModelParent(Ids.EXTENSIONS_FOLDER_LABEL,
            conn, partition, ModelObject.Type.EXTENSION_DIR, extensionsFolderPath, ws);
        RuleProvider.iterateJsonElementItems(itemData.extensions, new JsonElementVisitor() {
            public void visit(ItemData itemData) {
                processIlxExtension(conn, partition, extensionsParent, extensionsFolderPath, itemData);
            }
        });

        final IPath rulesFolderPath = workspacePath.append(Ids.RULES_FOLDER);
        final ModelParent rulesFolder = RuleProvider.getModelParent(Ids.RULES_FOLDER_LABEL,
            conn, partition, ModelObject.Type.IRULE_DIR_ILX, rulesFolderPath, ws);
        RuleProvider.iterateJsonElementItems(itemData.rules, new JsonElementVisitor() {
            public void visit(ItemData itemData) {
                if (itemData.name != null) {
                    ILXRuleModelFile.processIlxRule(conn, partition, itemData.name, rulesFolderPath, rulesFolder);
                }
            }
        });
    }

    /**
     * Create a {@link ModelParent} for the extension item.<br>
     * iterate over the extension nested items. For each one:<br>
     * if it's an ILX module Dir then add a {@link ModelParent} dir to the extension folder,<br>
     * Otherwise add a {@link ILXModelFile} to the extension folder.
     */
    private static void processIlxExtension(BigIPConnection conn, String partition, ModelParent parent, IPath dirPath, ItemData itemData) {
        logger.debug("Process ILX Extension\ndirPath = " + dirPath + "\n" + itemData);
        String name = itemData.name;
        if (name == null) {
            return;
        }
        IPath filePath = dirPath.append(name);
        ModelParent extensionFolder = RuleProvider.getModelParent(name, conn, partition, ModelObject.Type.EXTENSION, filePath, parent);
        IPath path = dirPath.append(extensionFolder.getName());
        RuleProvider.processIlxResources(conn, partition, itemData.files, extensionFolder, path);
    }

}
