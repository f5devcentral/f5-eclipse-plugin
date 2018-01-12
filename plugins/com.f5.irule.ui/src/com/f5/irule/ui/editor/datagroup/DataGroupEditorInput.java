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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.f5.irule.model.DataGroup;
import com.f5.irule.ui.Strings;

public class DataGroupEditorInput implements IEditorInput {

    private DataGroup dataGroup;

    DataGroupEditorInput(DataGroup model) {
        this.dataGroup = model;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class clazz) {
        return null;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    @Override
    public String getName() {
        String name = dataGroup.getName();
        return name;
    }

    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    @Override
    public String getToolTipText() {
        return Strings.LABEL_DATA_GROUP_RECORDS_EDITOR;
    }
    
    public DataGroup getDataGroup() {
        return dataGroup;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof DataGroupEditorInput)) {
            return false;
        }
        DataGroupEditorInput otherInput = (DataGroupEditorInput) other;
        boolean ans = dataGroup.equals(otherInput.dataGroup);
        return ans;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[DataGroupEditorInput ");
        builder.append(dataGroup);
        builder.append("]");
        return builder.toString();
    }
}
