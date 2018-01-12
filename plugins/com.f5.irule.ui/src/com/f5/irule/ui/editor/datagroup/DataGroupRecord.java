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
package com.f5.irule.ui.editor.datagroup;

import org.eclipse.core.commands.common.EventManager;

import com.f5.irule.model.DataGroup;

public class DataGroupRecord extends EventManager {

    private String recordName;
    private String recordData;
    private int itemIndex;
    private DataGroup dataGroup;

    public DataGroupRecord(String recordName, String recordData, int itemIndex, DataGroup parent) {
        this.recordData = recordData;
        this.recordName = recordName;
        this.itemIndex = itemIndex;
        this.dataGroup = parent;
    }

    String getRecordName() {
        return recordName;
    }

    void setRecordName(String recordName) {
        this.recordName = recordName;
    }

    String getRecordData() {
        return recordData;
    }
    
    void setRecordData(String recordData) {
        this.recordData = recordData;
    }

    int getItemIndex() {
        return itemIndex;
    }

    DataGroup getDataGroup() {
        return dataGroup;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(getClass().getSimpleName());
        if (getRecordName() != null) {
            builder.append(" ").append(getRecordName());
        }
        if (recordData != null) {
            builder.append(" recordData=").append(recordData);
        }
        builder.append("]");
        return builder.toString();
    }
}
