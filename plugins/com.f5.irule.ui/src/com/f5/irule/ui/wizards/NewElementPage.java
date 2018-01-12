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

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.f5.irule.ui.Strings;

public class NewElementPage extends WizardPage {

    protected NewElementPage(String pageName) {
        super(pageName);
    }
    
    private Text nameText;
    
    public String getName() {
        return getTextFrom(nameText);
    }

    private String getTextFrom(Text text) {
        return text == null || text.isDisposed() ? null : text.getText();
    }

    @Override
    public void createControl(Composite parent) {
        Composite page = new Composite(parent, SWT.NONE);
        setControl(page);
        setPageComplete(false);

        // Layout
        page.setLayout(new GridLayout(2, false));
        page.setLayoutData(new GridData(GridData.FILL_BOTH));

        Label nameLabel = new Label(page, SWT.NONE);
        nameLabel.setText(Strings.msg(getTitle(), Strings.LABEL_NAME));
        nameText = new Text(page, SWT.BORDER);
        nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Listener
        CompleteListener listener = new CompleteListener();
        nameText.addKeyListener(listener);
        
    }
    
    private class CompleteListener implements KeyListener {
        public void keyPressed(KeyEvent e) {
        }

        public void keyReleased(KeyEvent e) {
            boolean hasName = !"".equals(getTextFrom(nameText));
            setPageComplete(hasName);
        }
    }
}
