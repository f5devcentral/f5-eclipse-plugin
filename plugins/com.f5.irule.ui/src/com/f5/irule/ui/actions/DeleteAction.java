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

import org.apache.log4j.Logger;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.f5.irule.ui.Strings;
import com.f5.irule.ui.wizards.DeleteWizard;

public class DeleteAction extends Action {

    private static Logger logger = Logger.getLogger(DeleteAction.class);

    private IStructuredSelection selection;
    private Runnable completeRunnable;

    public DeleteAction(IStructuredSelection selection) {
        this.setText(Strings.LABEL_DELETE);
        this.selection = selection;
        Image deleteImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
        this.setImageDescriptor(ImageDescriptor.createFromImage(deleteImage));
    }

    public void run() {
        DeleteWizard wizard = new DeleteWizard(completeRunnable);
        logger.debug("Run " + DeleteWizard.class.getSimpleName() + " for " + selection);
        wizard.init(selection);

        IWorkbench workbench = PlatformUI.getWorkbench();
        WizardDialog dialog = new WizardDialog(workbench.getActiveWorkbenchWindow().getShell(), wizard);
        dialog.open();
    }

    public void setCompleteRunnable(Runnable completeRunnable) {
        this.completeRunnable = completeRunnable;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(getClass().getSimpleName()).append(" ");
        builder.append(selection.getFirstElement());
        builder.append("]");
        return builder.toString();
    }
}
