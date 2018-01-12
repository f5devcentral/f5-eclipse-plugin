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

import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Path;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ItemData {

    public String name;
    public String type;
    public String partition;
    public Path fullPath;
    public String body;
    public JsonElement extensions;
    public JsonElement rules;
    public JsonElement files;
    public JsonArray records;

    private ItemData(String name, String body, String partition,
            JsonElement extensions, JsonElement rules, JsonElement files,
            Path fullPath, String type, JsonArray records) {
        this.name = name;
        this.body = body;
        this.partition = partition;
        this.extensions = extensions;
        this.rules = rules;
        this.files = files;
        this.fullPath = fullPath;
        this.type = type;
        this.records = records;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(getClass().getSimpleName());
        builder.append(" ").append(name);
        if (type != null) {
            builder.append(" ").append(type);
        }
        if (partition != null) {
            builder.append(" ").append(partition);
        }
        if (fullPath != null) {
            builder.append(" ").append(fullPath);
        }
        if (body != null) {
            builder.append(" ").append(body);
        }
        if (extensions != null) {
            builder.append("\nextensions: ").append(RuleProvider.formatJsonContent(extensions));
        }
        if (rules != null) {
            builder.append("\nrules: ").append(RuleProvider.formatJsonContent(rules));
        }
        if (files != null) {
            builder.append("\nfiles: ").append(RuleProvider.formatJsonContent(files));
        }
        if (records != null) {
            builder.append("\nrecords: ").append(records);
        }
        builder.append("]");
        return builder.toString();
    }
    
    public static ItemData getData(JsonElement jsonElement) {
        if (!jsonElement.isJsonObject()) {
            return null;
        }
        JsonObject jsonObject = (JsonObject) jsonElement;
        Set<Entry<String, JsonElement>> entrySet = jsonObject.entrySet();
        if (entrySet == null) {
            return null;
        }
        // Iterate JSON Elements with Key values
        String name = null;
        String body = null;
        String partition = null;
        JsonElement extensions = null;
        JsonElement rules = null;
        JsonElement files = null;
        Path fullPath = null;
        String type = null;
        JsonArray records = null;
        for (Entry<String, JsonElement> en : entrySet) {
            String key = en.getKey();
            JsonElement val = en.getValue();
            if (key.equals("name")) {
                name = val.getAsString();
            } else if (key.equals("apiAnonymous")) {
                body = val.getAsString();
            } else if (key.equals("partition")) {
                partition = val.getAsString();
            } else if (key.equals("extensions")) {
                extensions = val;
            } else if (key.equals("rules")) {
                rules = val;
            } else if (key.equals("files")) {
                files = val;
            } else if (key.equals("fullPath")) {
                fullPath = new Path(val.getAsString());
            } else if (key.equals("type")) {
                type = val.getAsString();
            } else if (key.equals("records")) {
                records = (JsonArray) val;
            }
        }
        ItemData itemData = new ItemData(name, body, partition, extensions, rules, files, fullPath, type, records);
        return itemData;
    }
}