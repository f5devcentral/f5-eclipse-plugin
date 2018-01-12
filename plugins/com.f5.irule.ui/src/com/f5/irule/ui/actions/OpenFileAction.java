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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import com.f5.irule.model.ModelFile;
import com.f5.irule.model.ModelObject;
import com.f5.irule.model.ModelParent;
import com.f5.irule.model.RestRule;
import com.f5.irule.ui.Strings;
import com.f5.irule.ui.views.IruleView;

public class OpenFileAction extends Action {
    private IStructuredSelection selection;

    public OpenFileAction(IStructuredSelection selection) {
        this.setText(Strings.LABEL_OPEN);
        this.selection = selection;
    }

    public void run() {
        Object ele = selection.getFirstElement();
        if (ele instanceof RestRule || ele instanceof ModelFile) {
            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            IruleView.restGetOpenFileEditor((ModelObject) ele, page);
        }
        else if (ele instanceof ModelParent) {
            ModelParent obj = (ModelParent) ele;
            if (obj.getName().equals("node_modules") && (obj.getType() == ModelObject.Type.NODE_MODULES_DIR) &&
                (obj.getParent().getType() == ModelObject.Type.EXTENSION) && !obj.hasChildren()) {
                ExpandNodeModulesAction expandAction = new ExpandNodeModulesAction((ModelParent)ele);
                expandAction.run();
            }
        }
    }
}
