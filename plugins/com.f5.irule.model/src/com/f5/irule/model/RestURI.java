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

/**
 * Utility class for forming iControlRest tm URI's
 */

public class RestURI {
    
    private StringBuilder uri;
    private StringBuilder select = new StringBuilder();
    private StringBuilder options = new StringBuilder();
    
    public RestURI(String address, String module, String component) {
        setUri(address);
        uri.append("/mgmt/tm/");
        if (module != null) {
            uri.append(module + "/");
        }
        if (component != null) {
            uri.append(component + "/");
        }
    }
    
    public RestURI(String address, String endpoint) {
        setUri(address);
        uri.append("/" + endpoint); 
    }

    private void setUri(String address) {
        if (address == null) {
            uri = new StringBuilder("");
        } else {
            uri = new StringBuilder("https://");
            uri.append(address);
        }
    }
    
    public void append(String string) {
        uri.append(string + "/");
    }
    
    public void appendSlashFirst(String string) {
        uri.append("/" + string);
    }
    
    public void appendPartitionedOID(String partition, String string) {
        uri.append("~" + partition + "~");
        uri.append(string + "/");
    }
    
    public void addSelect(String selectString) {
        if (options.length() == 0){
            select.append("?$select=");
        } else {
            options.append(",");            
        }
        select.append(selectString);
    }
    
    public void addOption(String name, String value) {
        if (options.length() == 0){
            options.append("?options=");
        } else {
            options.append(",");
        }
        options.append(name + ",");
        options.append(value);
    }
    
    public void addParameter(String name, String value) {
        if (options.length() == 0){
            options.append("?");
        }
        else {
            options.append("&");
        }
        options.append(name + "=");
        options.append(value);
    }
    
    public String toString() {
        return uri.toString() + select.toString() + options.toString();
    }
}
