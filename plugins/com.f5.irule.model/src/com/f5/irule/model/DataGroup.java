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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import com.f5.irule.model.BigIPConnection.Module;
import com.f5.rest.common.RestOperation.RestMethod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 *  A model file representing Data-Group,
 *  it keeps the Data-Group records,
 *  and responsible for data-group REST operations.
 */
public class DataGroup extends ModelFile {

    private static Logger logger = Logger.getLogger(DataGroup.class);

    // suffix of the workspace local file that stores the Data Group model
    public static final String FILE_SUFFIX = "dataGroup";

    // REST api key words
    public static final String DATA_GROUP = "data-group";
    private static final String RECORDS = "records";
    private static final String TYPE = "type";
    public static final String NAME = "name";
    public static final String DATA = "data";

    // The records of the Data-Group, in a Json Array object,
    // as received in the REST GET data-group api. 
    private JsonArray records;

    private String dataGroupType;
    private Module module;
    private IPath folder;

    public DataGroup(String name, String dataGroupType, BigIPConnection conn, String partition,
            ModelObject.Type type, Module module, IPath folder) {
        super(name, conn, partition, type, name == null ? null : folder.append(name).addFileExtension(FILE_SUFFIX));
        this.module = module;
        this.folder = folder;
        this.dataGroupType = dataGroupType;
    }
    
    public IPath getFolder() {
        return folder;
    }

    public Module getModule() {
        return module;
    }
    
    protected boolean isForceSetContent() {
        return true;
    }

    @SuppressWarnings("rawtypes")
    public Object getAdapter(Class adapter) {
        return null;
     }
    
    /* Send REST GET data-group request,
     * in order to display the Data-Group records in the editor */
    @Override
    public IStatus iControlRestGet(RequestCompletion completion) {
        BigIPConnection conn = getConnection();
        RestURI uri = conn.getURI(module.name(), DATA_GROUP);
        uri.append("internal");
        uri.appendPartitionedOID(getPartition(), getName());
        return RestFramework.sendRequest(conn, RestMethod.GET, uri.toString(), null, null, completion);
    }

    /* Delete this Data-Group from the Big-Ip server.
     * Send DELETE data-group request with this {@link DataGroup} name. */
    @Override
    public IStatus iControlRestDelete(RequestCompletion completion) {
        logger.debug("Delete " + getName());
        BigIPConnection conn = getConnection();
        RestURI uri = computeUri(conn);
        uri.append(getName());
        String uriString = uri.toString();
        return RestFramework.sendRequest(conn, RestMethod.DELETE, uriString, null, null, completion);
    }

    /**
     * Parse the REST data-group response.<br>
     * Update the {@link DataGroup} records and type from the response {@link JsonObject} data.<br>
     * Return a string representing the model data.
     */
    public String parseContent(JsonObject jsonBody) {
        update(jsonBody);
        String jsonContent = jsonBody.toString();
        String content = RuleProvider.formatJsonContent(jsonContent);
        return content;
    }

    /**
     * Update this Data-Group records and type from the {@link JsonObject} data
     */
    public void update(JsonObject root) {
        records = (JsonArray) root.get(RECORDS);
        JsonElement typeElement = root.get(TYPE);
        if (typeElement == null) {
            logger.warn("Failed to get type from " + root);
        }
        else{
            dataGroupType = typeElement.getAsString();            
        }
    }

    public String getContent(JsonArray tempRecords) {
        JsonObject root = new JsonObject();
        root.add(NAME, new JsonPrimitive(getName()));
        root.add(TYPE, new JsonPrimitive(dataGroupType));
        root.add(RECORDS, tempRecords);
        String content = RuleProvider.formatJsonContent(root.toString());
        return content;
    }

    public String getDataGroupType() {
        return dataGroupType;
    }

    private JsonArray tempRecords;
    public void setTempRecords(JsonArray tempRecords) {
        this.tempRecords = tempRecords;
    }
    
    /** 
     * Update the records of this Data-Group.
     * Send PATCH data-group request with the given records.
     */
    @Override
    public IStatus iControlRestPatch(RequestCompletion completion) {
        if (tempRecords == null) {
            IStatus status = getRecordsFromFile();
            if (status != null) {
                return status;
            }
        }
        logger.debug("Patch " + getName() + " records:" + tempRecords);
        BigIPConnection conn = getConnection();
        RestURI uri = computeUri(conn);
        uri.append(getName());
        String uriString = uri.toString();
        String jsonBody = createJsonBody(EMPTY_ARRAY, EMPTY_ARRAY, tempRecords);
        IStatus ans = RestFramework.sendRequest(conn, RestMethod.PATCH, uriString, null, jsonBody, completion);
        tempRecords = null;
        return ans;
    }
    private static final String[] EMPTY_ARRAY = new String[]{};

    private IStatus getRecordsFromFile() {
        InputStream fileContents;
        try {
            fileContents = ModelObject.getFileContents(this);
        } catch (CoreException ex) {
            logger.warn("Failed to get contents of " + this, ex);
            IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN, Messages.FAILED_TO_RETRIEVE_FILE, ex);
            StatusManager.getManager().handle(status, StatusManager.LOG);
            return status;
        }
        JsonObject root = RuleProvider.getRootObject(fileContents);
        tempRecords = (JsonArray) root.get(RECORDS);
        return null;
    }

    public void setDataGroupType(String dataGroupType) {
        this.dataGroupType = dataGroupType;
    }

    /** 
     * Create this Data-Group on the Big-Ip server.<br>
     * Send POST data-group request with the given records.<br>
     * The request properties are name, type and partition
     */
    @Override
    public IStatus iControlRestPost(RequestCompletion completion) {
        logger.debug("Post Data-Group " + getName() + " records:" + tempRecords);
        BigIPConnection conn = getConnection();
        RestURI uri = computeUri(conn);
        String uriString = uri.toString();
        String jsonBody = createJsonBody(
            new String[] {"name", "type", "partition"},
            new String[] {getName(), dataGroupType, getPartition()},
            tempRecords);
        IStatus ans = RestFramework.sendRequest(conn, RestMethod.POST, uriString, null, jsonBody, completion);
        tempRecords = null;
        return ans;
    }

    private static String createJsonBody(String[] keys, String[] values, JsonArray records) {
        JsonObject bodyObject = new JsonObject();
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            String value = values[i];
            bodyObject.addProperty(key, value);
        }
        if (records != null) {
            bodyObject.add(RECORDS, records);
        }
        String jsonBody = String.valueOf(bodyObject);
        return jsonBody;
    }

    private RestURI computeUri(Connection conn) {
        RestURI uri = conn.getURI(null, null);
        uri.append(module.name());
        uri.append(DATA_GROUP);
        uri.append("internal");
        return uri;
    }
    
    /**
     * Transfer the records written in the editor to {@link JsonArray} records
     */
    static JsonArray createRecordsFromResource(IResourceDelta change) throws CoreException {
        IFile resource = (IFile) change.getResource();
        InputStream contents = resource.getContents();
        Properties properties = new Properties();
        try {
            properties.load(contents);
        } catch (IOException ex) {
            throw new CoreException(new Status(IStatus.ERROR,
                Ids.PLUGIN, "Failed to load properties from " + resource, ex));
        }
        JsonArray jsonArray = new JsonArray();
        Set<Object> keySet = properties.keySet();
        for (Object key : keySet) {
            String nameValue = (String) key;
            String propertyValue = (String) properties.get(key);
            JsonObject element = createJsonObject(nameValue, propertyValue);
            jsonArray.add(element);
        }
        JsonArray recordsFromResource = jsonArray;
        return recordsFromResource;
    }

    public static JsonObject createJsonObject(String nameValue, String dataValue) {
        JsonObject element = new JsonObject();
        element.addProperty(NAME, nameValue);
        if (dataValue != null && !dataValue.equals("")) {
            element.addProperty(DATA, dataValue);            
        }
        return element;
    }

    /**
     * Return the String value of the element in the json object with the given key.
     */
    public static String getElementValue(JsonObject jsonObject, String key) {
        JsonElement dataElement = jsonObject.get(key);
        if (dataElement == null) {
            return null;
        }
        String value = dataElement.getAsString();
        return value;
    }

    public JsonArray getJsonRecords() {
        return records;
    }

    public static final String FOLDER_NAME = "data_groups";

    /**
     * Get the record with name equal to the given id
     * and return its data value.
     */
    public String getRecordData(Object id) {
        JsonObject jsonObject = getElement(records, id);
        if (jsonObject == null) {
            return null;
        }
        String data = getElementValue(jsonObject, DATA);
        return data;
    }

    /**
     * Iterate over the elements in the json array and return the element
     * with name equal to the given id
     */
    private static JsonObject getElement(JsonArray array, Object id) {
        for (JsonElement jsonElement : array) {
            JsonObject jsonObject = (JsonObject) jsonElement;
            String recordName = getElementValue(jsonObject, NAME);
            if (id.equals(recordName)) {
                return jsonObject;
            }
        }
        return null;
    }

    public void setDataGroupFileContent(String content, boolean isNew) throws CoreException {
        IFile file = getFile();
        if (isNew) {
            setLocallyAdded(true);
        }
        setFileContent(file, content);
    }

}
