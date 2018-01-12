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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public class DeletePage extends WizardPage {

    private String deleteMessage;

    protected DeletePage(String pageName, String deleteMessage) {
        super(pageName);
        this.deleteMessage = deleteMessage;
        setTitle(pageName);
        setImageDescriptor(ImageDescriptor.createFromImage(
                // TODO better image
                PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE)));
    }

    @Override
    public void createControl(Composite parent) {
        Composite page = new Composite(parent, SWT.NONE);
        setControl(page);
        setPageComplete(true);
        // Layout
        page.setLayout(new GridLayout(2, false));
        page.setLayoutData(new GridData(GridData.FILL_BOTH));
        Label label = new Label(page, SWT.NONE);
        label.setText(deleteMessage);
        label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }
}
