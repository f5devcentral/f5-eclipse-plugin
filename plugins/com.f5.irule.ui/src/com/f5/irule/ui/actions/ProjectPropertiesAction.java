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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.statushandlers.StatusManager;

import com.f5.irule.model.Connection;
import com.f5.irule.model.ModelObject;
import com.f5.irule.ui.Ids;
import com.f5.irule.ui.Strings;
import com.f5.irule.ui.preferences.ConnectionPropertiesPage;
import com.f5.irule.ui.views.IruleView;

public class ProjectPropertiesAction extends Action {
    
    private Connection conn = null;
    
    public void run() {

        // Determine which project
        TreeViewer treeview = IruleView.getTreeViewer();
        if (treeview != null) {
            IStructuredSelection selection = (IStructuredSelection) treeview.getSelection();
            Object ele = selection.getFirstElement();
            if (ele instanceof ModelObject) {
                conn = ((ModelObject) ele).getConnection();
            }
        }

        if (conn == null) {
            IStatus status = new Status(IStatus.OK, Ids.PLUGIN, Strings.INFO_PLEASE_SELECT_PROJECT);
            StatusManager.getManager().handle(status, StatusManager.SHOW);
            return;
        }
        
        String propertyPageId = ConnectionPropertiesPage.class.getName();
        PreferenceDialog dialog = PreferencesUtil.createPropertyDialogOn(Display.getCurrent().getActiveShell(),
            conn.getProject(), propertyPageId, null, conn);

        dialog.open();      
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(getClass().getSimpleName()).append(" ");
        builder.append(conn);
        builder.append("]");
        return builder.toString();
    }
}
