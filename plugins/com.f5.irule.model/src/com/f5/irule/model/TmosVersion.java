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

import java.util.Map.Entry;

import org.osgi.framework.Version;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class TmosVersion {
   
    // This Eclipse plugin requires at least 
    public final static Version MINIMUM = new Version("12.1.0");
    
    // And isn't expected to work with versions greater than
    public final static Version MAXIMUM = new Version("14.9.9");
    
    // Classes for Json parsing
    public class VersionDesc {
        private String description;
        
        @Override
        public String toString() {
            return description;
        }        
    }
    
    public class Entries {
        private VersionDesc Version;
        
        @Override
        public String toString() {
            return Version.toString();
        }
    }
    public class NestedStats {
        private Entries entries;
        
        @Override
        public String toString() {
            return entries.toString();
        }
    }
    
    public class VersionEntries {
        private NestedStats nestedStats;
        
        @Override
        public String toString() {
            return nestedStats.toString();
        }
    } 
   
    /**
     * Extract the version string buried in the kludge json response which looks something like:
     * { "entries": { "https://localhost/mgmt/tm/sys/version/0": { "nestedStats": {
     *   "entries": { "Version": { "description": "12.1.0" } } } } } }
     */
    public static Version parse(JsonElement jsonBody) {
        JsonElement entries = RuleProvider.parseElement(jsonBody, "entries");
        if (entries != null) {
            JsonObject entriesJsonObject = entries.getAsJsonObject();
            for (Entry<String, JsonElement> en : entriesJsonObject.entrySet()) {
                Gson gson = new GsonBuilder().create(); 
                VersionEntries version = gson.fromJson(en.getValue(), VersionEntries.class);
                if (version != null) {
                    return new Version(version.toString());
                }
            }
        }
        return null;
    }
}
