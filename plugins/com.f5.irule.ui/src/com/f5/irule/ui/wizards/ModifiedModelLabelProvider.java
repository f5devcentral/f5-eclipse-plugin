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
package com.f5.irule.ui.wizards;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

import com.f5.irule.model.ModelObject;
import com.f5.irule.model.ModelObject.Type;
import com.f5.irule.ui.views.ViewLabelProvider;

public class ModifiedModelLabelProvider implements ITableLabelProvider {

    // Column constants
    public static final int COLUMN_MODULE_NAME = 0;
    public static final int COLUMN_MODULE_TYPE = 1;
    public static final int COLUMN_MODULE_PATH = 2;

    public ModifiedModelLabelProvider() {
    }

    public Image getColumnImage(Object obj, int index) {
        switch (index) {
        case COLUMN_MODULE_TYPE:
            return ViewLabelProvider.getModelFileImage((ModelObject) obj);
        }
        return null;
    }

    public String getColumnText(Object obj, int index) {
        ModelObject model = (ModelObject) obj;
        String text = "";
        switch (index) {
        case COLUMN_MODULE_NAME:
            text = model.getName();
            break;
        case COLUMN_MODULE_TYPE:
            Type modelType = model.getType();
            text = toText(modelType);
            break;
        case COLUMN_MODULE_PATH:
            IPath filePath = model.getFilePath();
            text = filePath.toString();
            break;
        }
        return text;
    }

    private static String toText(Type modelType) {
        StringBuilder textBuilder = new StringBuilder();
        String[] array = modelType.name().split("_");
        textBuilder.append(capitalize(array[0]));
        for (int i = 1; i < array.length; i++) {
            String word = array[i];
            textBuilder.append(" ").append(capitalize(word));
        }
        String text = textBuilder.toString();
        return text;
    }

    private static String capitalize(String word) {
        String ans = word.substring(0,1).toUpperCase() + word.substring(1).toLowerCase();
        return ans;
    }

    public void addListener(ILabelProviderListener arg0) {
        // Throw it away
    }

    public void dispose() {
    }

    public boolean isLabelProperty(Object arg0, String arg1) {
        return false;
    }

    public void removeListener(ILabelProviderListener arg0) {
        // Do nothing
    }
}