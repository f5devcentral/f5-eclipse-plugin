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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.f5.irule.model.BigIPConnection.Module;
import com.f5.irule.model.ModelObject.Type;
import com.f5.rest.common.RestOperation;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

/**
 * A Controller between the model and the UI view,<br>
 * providing methods to parse content that was received from the Big-IP<br>
 * and transform it into model objects that can be presented in the Explorer View.
 */
public class RuleProvider {

    static Logger logger = Logger.getLogger(RuleProvider.class);

    private static final String NAME = "name";
    static final String PARTITION = "partition";
    public static final String WORKSPACE = "workspace";
    public static final String EXTENSION = "extension";
    
    public static final String TCL = "tcl";

    private static ModelListener listener;

    public RuleProvider(ModelListener listener) {
        RuleProvider.listener = listener;
    };
    
    public static ModelListener getListener() {
        return listener;
    }

    /**
     * Parse the {@link RestOperation} response to a Map of {@link Rule} rules.<br>
     * Filter only rules that match with the connection current partition value.<br>
     * For each rule create an {@link IPath} composed from the filePath as prefix
     * and the rule name with .tcl ending as suffix.<br>
     * The map keys are the rules paths and the values are the {@link Rule} objects.
     */
    public static HashMap<IPath, Rule> parseIrules(BigIPConnection conn, Module module, JsonElement jsonBody) {
        List<JsonObject> jsonItemsList = getJsonItemsList(jsonBody);
        ModelObject.Type type;
        String folderId;
        if (module.equals(Module.gtm)){
            type = ModelObject.Type.GTM_RULE;
            folderId = Ids.IRULES_GTM_FOLDER;
        }
        else{
            type = ModelObject.Type.LTM_RULE;
            folderId = Ids.IRULES_LTM_FOLDER;
        }
    	HashMap<IPath, Rule> rules = new HashMap<IPath, Rule>();
    	for (JsonObject jsonObject : jsonItemsList) {
    	    ItemData itemData = ItemData.getData(jsonObject);
            if (itemData != null &&
    	          (itemData.name.length() > 0) &&
    	          (itemData.fullPath == null || itemData.fullPath.segmentCount() <= 2)) {
                String rulePartition = itemData.partition;
                IPath path = new Path(rulePartition).append(folderId).append(itemData.name + "." + TCL);
	            RestRule rule = getRestRule(itemData.name, conn, type, path, rulePartition, itemData.body);
	            IPath regPath = createRegPath(conn, path);// Add to rule registry
	            rules.put(regPath, rule); 
    	    }            
        }
        return rules;
    }

    public static ModelObject.Type getDataGroupType(Module module) {
        return (module.equals(Module.gtm)) ? ModelObject.Type.GTM_DATA_GROUP : ModelObject.Type.LTM_DATA_GROUP;
    }

    /**
     * parse the response json body and return its "items" array
     * as a List of JsonObject elements
     */
    public static List<JsonObject> getJsonItemsList(JsonElement root) {
        List<JsonObject> list = new ArrayList<JsonObject>();
        JsonElement items = parseItems(root);
        if (items != null && items.isJsonArray()) {
            JsonArray jsonArray = items.getAsJsonArray();
            for (JsonElement jsonElement : jsonArray) {
                if (jsonElement.isJsonObject()) {
                    list.add((JsonObject) jsonElement);
                }
            }            
        }
        return list;
    }

    public static RestRule getRestRule(String ruleName, BigIPConnection conn, ModelObject.Type type,
            IPath path, String partition, String ruleBody) {
        
        ModelObject model = conn.getModel(null, path);
        if (model != null && model.isLocallyModified()) {
            logger.debug("Edited locally - " + model + ". Don't create");
            return (RestRule) model;
        }

        RestRule rule = createRule(ruleName, conn, type, path, partition, ruleBody);
        return rule;            
    }

    /**
     * Create a {@link RestRule} with the given {@link Connection}
     */
    private static RestRule createRule(String ruleName, BigIPConnection conn, ModelObject.Type type, IPath path,
        String partition, String ruleBody) {
        RestRule rule = new RestRule(ruleName, conn, partition, type, path);
        if (ruleBody != null) {
            rule.setText(ruleBody);
            rule.setReadOnly(isReadOnly(ruleBody));
        }
        return rule;
    }

    private static boolean isReadOnly(String ruleBody) {
        boolean ans = false;
        Scanner scanner = new Scanner(ruleBody);
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

    private static IPath createRegPath(Connection conn, IPath path) {
        IPath regPath = new Path("/");
        regPath = regPath.append(conn.getAddress());
        regPath = regPath.append(path);
        return regPath;
    }

    /**
     * Iterate over the {@link JsonElement} items in the array element.<br>
     * Convert each item to {@link ItemData} object
     * and call the {@link JsonElementVisitor #visit(ItemData)} with the item data.
     */
    public static void iterateJsonElementItems(JsonElement element, JsonElementVisitor visitor) {
        if (element != null && element.isJsonArray()) {
            for (JsonElement je : element.getAsJsonArray()) {
                ItemData itemData = ItemData.getData(je);
                if (itemData != null) {
                    visitor.visit(itemData);
                }
            }
        }
    }

    /**
     * parse the response json body and get its "items" element
     */
    public static JsonElement parseItems(JsonElement root) {
        JsonElement items = parseElement(root, "items");
        return items;
    }

    /**
     * Get the {@link ModelParent} with the given name.<br>
     * First check if it's already a child in the given parent.<br>
     * If not, add it as a child to the {@link ModelParent} parent
     */
    public static ModelParent getModelParent(String name, BigIPConnection connection,
            String partition, Type type, IPath filePath, ModelParent parent) {
        ModelParent childFolder = (ModelParent) parent.getChild(name);
        if (childFolder == null) {
            switch (type) {
            case WORKSPACE:
                childFolder = new WorkspaceModelParent(name, connection, partition, filePath);
                break;
            default:
                childFolder = new ModelParent(name, connection, partition, type, filePath);
                break;
            }
            childFolder.addListener(listener);            
            parent.addChild(childFolder);
        }
        return childFolder;
    }
    
    /**
     * Iterate over the extensions items in the json response.<br>
     * For each extension item, get its files items, iterate over the files<br>
     * and add ILX models to the tree according the files types.
     */
    public static boolean readILXExtension(final BigIPConnection conn, final String partition,
            final ModelParent extensionObj, JsonObject jsonBody) {
        // return true if extensionObj is found to contain children (a "files" element)
        final AtomicBoolean hasFiles = new AtomicBoolean(false);
        JsonElement extensionsItems = parseElement(jsonBody, "extensions");
        iterateJsonElementItems(extensionsItems, new JsonElementVisitor() {
            public void visit(ItemData itemData) {
                if (itemData != null && itemData.files != null) {
                    processIlxResources(conn, partition, itemData.files, extensionObj, extensionObj.getFilePath());
                    hasFiles.set(true);
                }
            }
        });
        return hasFiles.get();
    }

    public static String getIlxWorkspaceUri(Connection conn, String partition,
            String workspaceName, String extensionName) {
        RestURI uri = conn.getURI(BigIPConnection.Module.ilx.name(), RuleProvider.WORKSPACE);
        if (workspaceName != null) {
            if (partition == null) {
                uri.append(workspaceName);
            } else {
                uri.appendPartitionedOID(partition, workspaceName);
            }
        }
        if (extensionName != null) {
            uri.addOption(RuleProvider.EXTENSION, extensionName);
        }
        return uri.toString();
    }

    static String getIlxBody(String partition, String workspace) {
        JsonObject json = new JsonObject();
        json.addProperty(PARTITION, partition);
        if (workspace != null) {
            json.addProperty(NAME, workspace);
        }
        String body = json.toString();
        return body;
    }

    /**
     * Iterate over the json element items.<br>
     * For each one: if it's an ILX module Dir<br>
     * then add a {@link ModelParent} dir to the tree.<br>
     * Otherwise add a {@link ILXModelFile} to the tree.
     */
    public static void processIlxResources(final BigIPConnection conn, final String partition,
            JsonElement jsonElement, final ModelParent parent, final IPath path) {
        iterateJsonElementItems(jsonElement, new JsonElementVisitor() {
            public void visit(ItemData itemData) {
                String name = itemData.name;
                ILXModelFile.parseIlxItem(conn, partition, parent, path, name);
            }
        });
    }

    private static final JsonParser JSON_PARSER = new JsonParser();
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Parse the response body as a {@link JsonObject}
     * and return its child element corresponding to the string key.
     */
    public static JsonElement parseElement(JsonElement element, String key) {
        JsonObject jsonObject = element.getAsJsonObject();
        JsonElement childElement = jsonObject.get(key);
        return childElement;
    }

    /**
     * Use the {@link JsonParser} to parse the string into a {@link JsonElement}.<br>
     * Set the {@link JsonReader} Lenient to true in order to configure the parser to be liberal in what it accepts.
     */
    public static JsonElement parseElement(String str) {
        //JsonElement root = JSON_PARSER.parse(str);
        Reader in = new StringReader(str);
        JsonReader jsonReader = new JsonReader(in);
        jsonReader.setLenient(true);
        JsonElement root = JSON_PARSER.parse(jsonReader);
        return root;
    }

    public static JsonObject getRootObject(InputStream fileContents) {
        Reader reader = new InputStreamReader(fileContents);
        JsonElement element = JSON_PARSER.parse(reader);
        if (element instanceof JsonNull) {
            return null;
        }
        JsonObject root = (JsonObject) element;
        return root;
    }
    public static String formatJsonContent(JsonElement element) {
        String content = formatJsonContent(element.toString());
        return content;
    }
    static String formatJsonContent(String jsonContent) {
        JsonElement el = JSON_PARSER.parse(jsonContent);
        String content = gson.toJson(el); // done
        return content;
    }

}
