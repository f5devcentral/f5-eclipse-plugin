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
package com.f5.irule.ui.preferences;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.f5.irule.model.ModelUtils;
import com.f5.irule.ui.Strings;
import com.f5.irule.ui.wizards.NewConnectionPage;

public class ConnectionSettingsBlock { // extends AbstractConfigurationBlock {

    private Text userText;
    private Text passwordText;
    private Button secure;
    private Combo partitionCombo = null;

    public ConnectionSettingsBlock() {
    };

    public String getUser() {
        return getTextFrom(userText);
    }
    
    public void setUser(String user) {
        userText.setText(user);
    }

    public String getPassword() {
        return getTextFrom(passwordText);
    }
    
    public void setPassword(String password) {
        passwordText.setText(password);
    }

    public boolean isSecureStore() {
        return secure.getSelection();
    }
    
    public void setSecureStore(boolean selected) {
        secure.setSelection(selected);
    }
    
    public String getPartition() {
        int index = partitionCombo.getSelectionIndex();
        if (index < 0) {
            return com.f5.irule.model.Ids.PARTITION_COMMON;
        }
        return partitionCombo.getItem(index);
    }
    
    public void setPartitionOptions(String[] partitions) {
        partitionCombo.setItems(partitions);
    }
    
    public void setSelectedPartition(int index) {
        partitionCombo.select(index);
    }

    private String getTextFrom(Text text) {
        return text == null || text.isDisposed() ? null : text.getText();
    }

    public Control createControl(Composite parent) {

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout());

        Label projectLabel = new Label(composite, SWT.NONE);
        projectLabel.setText(Strings.LABEL_TMOS_PARTITION);
        
        partitionCombo = new Combo(composite, SWT.BORDER);
        partitionCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        addCredentialsControl(composite, null);

        return composite;
    }
    
    public void addCredentialsControl(Composite parent, KeyListener listener) {
        userText = NewConnectionPage.addText(parent, Strings.LABEL_USER, SWT.BORDER, listener);
        passwordText = NewConnectionPage.addText(parent, Strings.LABEL_PASSWORD, SWT.BORDER | SWT.PASSWORD | SWT.SINGLE, listener);
        secure = NewConnectionPage.addButton(parent, Strings.LABEL_SECURE_STORE, SWT.CHECK);
    }
    
    public static void addCredentialsControlHelp(Composite parent) {
        NewConnectionPage.addHelpItem(parent, Strings.LABEL_USER, Strings.LABEL_USER_HELP);
        NewConnectionPage.addHelpItem(parent, Strings.LABEL_PASSWORD, Strings.LABEL_PASSWORD_HELP);
        NewConnectionPage.addHelpItem(parent, Strings.LABEL_SECURE_STORE, Strings.LABEL_SECURE_STORE_HELP);
    }
    
    /* 
     * Slightly awkward place for this here but we'd like consistent error handling
     * whether checking for new connection (wizard) or existing (properties page)
     */
    public static boolean checkConnection(DialogPage page, String ip){
        try {      
            // Quick first stab at connecting, report to dialog if unsuccessful
            IStatus status = ModelUtils.isIpReachable(ip, 5000);
            if (!status.isOK()) {
                page.setMessage(Strings.msg(Strings.ERROR_CANNOT_CONNECT,status.getMessage()),
                        IMessageProvider.ERROR);
                return false;
            }         
            return true;
        } catch (Exception e) {
            page.setMessage(Strings.msg(Strings.ERROR_CANNOT_CONNECT,e.toString()),
                    IMessageProvider.ERROR);
            return false;
        }        
    }
}
