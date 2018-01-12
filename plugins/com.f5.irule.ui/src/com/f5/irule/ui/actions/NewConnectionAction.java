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
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.f5.irule.ui.Strings;
import com.f5.irule.ui.wizards.NewConnectionPage;
import com.f5.irule.ui.wizards.NewConnectionWizard;

public class NewConnectionAction extends Action {

    public NewConnectionAction() {
        this.setText(Strings.LABEL_NEW_CONNECTION_ACTION);
        this.setImageDescriptor(ImageDescriptor.createFromFile(NewConnectionPage.class, "/images/new-connection.png"));
    }

    public void run() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        NewConnectionWizard wizard = new NewConnectionWizard();
        wizard.init(workbench, null);
        IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
		Shell activeWorkbenchShell = activeWorkbenchWindow.getShell();
		WizardDialog dialog = new WizardDialog(activeWorkbenchShell, wizard);
        dialog.open();
    }


}
