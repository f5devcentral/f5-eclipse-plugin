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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.f5.irule.model.BigIPConnection;
import com.f5.irule.ui.Strings;
import com.f5.irule.ui.wizards.SyncModifiedResourcesWizard;

public class SyncModifiedResourcesAction extends Action {

    private BigIPConnection connection;

    public SyncModifiedResourcesAction(BigIPConnection connection) {
        this.setText(Strings.LABEL_SYNC_MODIFIED_RESOURCES);
        this.connection = connection;
        Image deleteImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_SAVEALL_EDIT);
        this.setImageDescriptor(ImageDescriptor.createFromImage(deleteImage));
    }

    public void run() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        SyncModifiedResourcesWizard.openWizard(connection, window, false);
    }
}
