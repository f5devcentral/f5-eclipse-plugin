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
package com.f5.irule.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import com.f5.irule.model.ModelFile;
import com.f5.irule.model.ModelObject;
import com.f5.irule.model.ModelParent;
import com.f5.irule.model.RestRule;
import com.f5.irule.ui.Strings;
import com.f5.irule.ui.views.IruleView;

/**
 * {@link Action} that gets the file content from the Big-IP,
 * sets it in the local file and open the IDE editor.
 */
public class RestGetOpenFileEditorAction extends Action {
    private ModelObject element;

    public RestGetOpenFileEditorAction(ModelObject element) {
        this.element = element;
        this.setText(Strings.LABEL_OPEN);
    }

    public void run() {
        //Object ele = selection.getFirstElement();
        getElementResource(element);
    }

    public static void getElementResource(ModelObject element) {
        if (element instanceof RestRule || element instanceof ModelFile) {
            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            IruleView.restGetOpenFileEditor((ModelObject) element, page);
        }
        else if (element instanceof ModelParent) {
            ModelParent obj = (ModelParent) element;
            if (obj.getName().equals("node_modules") && (obj.getType() == ModelObject.Type.NODE_MODULES_DIR) &&
                (obj.getParent().getType() == ModelObject.Type.EXTENSION) && !obj.hasChildren()) {
                ExpandNodeModulesAction expandAction = new ExpandNodeModulesAction((ModelParent)element);
                expandAction.run();
            }
        }
    }
}
