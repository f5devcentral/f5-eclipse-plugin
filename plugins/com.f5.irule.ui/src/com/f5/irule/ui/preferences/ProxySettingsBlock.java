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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.f5.irule.ui.Strings;
import com.f5.irule.ui.wizards.NewConnectionPage;

/**
 * Container of Proxy details.<br>
 * Capable of adding swt control to a UI page/wizard
 */
public class ProxySettingsBlock {

    private Text ipText;
    private Text portText;
    private Text userText;
    private Text passwordText;
    private Button useProxyButton;
    private Button secure;

    public ProxySettingsBlock() {
    };

    public String getIp() {
        String ipValue = getTextFrom(ipText);
        if ((ipValue == null || ipValue.equals("")) && isUseProxy()) {
            throw new IllegalArgumentException(Strings.ERROR_PROXY_IP_MUST_BE_DEFINED);                
        }
        return ipValue;
    }

    public int getPort() {
        String portValue = getTextFrom(portText);
        if (portValue == null || portValue.equals("")) {
            if (isUseProxy()) {
                throw new IllegalArgumentException(Strings.ERROR_PROXY_PORT_MUST_BE_DEFINED);                
            } else {
                return -1;
            }
        }
        return Integer.parseInt(portValue);
    }

    public String getUser() {
        return getTextFrom(userText);
    }
    
    public String getPassword() {
        return getTextFrom(passwordText);
    }
    
    public boolean isUseProxy() {
        return useProxyButton.getSelection();
    }

    public boolean isSecureStore() {
        return secure.getSelection();
    }

    public void setIp(String ip) {
        ipText.setText(ip == null ? "" : ip);
    }

    public void setPort(int port) {
        portText.setText(port == -1 ? "" : String.valueOf(port));
    }
    
    public void setUser(String user) {
        userText.setText(user == null ? "" : user);
    }
    
    public void setPassword(String password) {
        passwordText.setText(password == null ? "" : password);
    }

    public void setSecureStore(boolean selected) {
        secure.setSelection(selected);
    }

    public void setUseProxy(boolean selected) {
        useProxyButton.setSelection(selected);
        setBlockEnabled(selected);
    }
    
    private String getTextFrom(Text text) {
        return text == null || text.isDisposed() ? null : text.getText();
    }

    public Control createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        addProxyControl(composite, null);
        return composite;
    }

    public void addProxyControl(Composite parent, KeyListener listener) {

        Label secureLabel = new Label(parent, SWT.NONE);
        secureLabel.setText(Strings.LABEL_USE_PROXY);
        useProxyButton = new Button(parent, SWT.CHECK);
        useProxyButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ipText = setLabel(parent, Strings.LABEL_PROXY_ADDRESS, SWT.BORDER, listener);
        portText = setLabel(parent, Strings.LABEL_PROXY_PORT, SWT.BORDER, listener);
        userText = setLabel(parent, Strings.LABEL_USER, SWT.BORDER, listener);
        passwordText = setLabel(parent, Strings.LABEL_PASSWORD, SWT.BORDER | SWT.PASSWORD | SWT.SINGLE, listener);
        secure = NewConnectionPage.addButton(parent, Strings.LABEL_SECURE_STORE, SWT.CHECK);
        SelectionListener selectionListener = new SelectionListener(){
            public void widgetDefaultSelected(SelectionEvent event) {
            }
            public void widgetSelected(SelectionEvent event) {
                Button button = (Button) event.widget;
                boolean selected = button.getSelection();
                setBlockEnabled(selected);
            }
        };
        useProxyButton.addSelectionListener(selectionListener);
    }

    public static void addProxyControlHelp(Composite parent) {
        NewConnectionPage.addHelpItem(parent, Strings.LABEL_USE_PROXY, Strings.LABEL_USE_PROXY_HELP);
        NewConnectionPage.addHelpItem(parent, Strings.LABEL_PROXY_ADDRESS, Strings.LABEL_PROXY_ADDRESS_HELP);
        NewConnectionPage.addHelpItem(parent, Strings.LABEL_PROXY_PORT, Strings.LABEL_PROXY_PORT_HELP);
        NewConnectionPage.addHelpItem(parent, Strings.LABEL_USER, Strings.LABEL_PROXY_USER_HELP);
        NewConnectionPage.addHelpItem(parent, Strings.LABEL_PASSWORD, Strings.LABEL_PROXY_PASSWORD_HELP);
        NewConnectionPage.addHelpItem(parent, Strings.LABEL_SECURE_STORE, Strings.LABEL_SECURE_STORE_HELP);
    }

    private Text setLabel(Composite parent, String labelValue, int border, KeyListener listener) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(labelValue);
        Text text = new Text(parent, border);
        text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (listener != null) {
            text.addKeyListener(listener);
        }
        text.setEnabled(false);
        return text;
    }

    void setBlockEnabled(boolean enabled) {
        ipText.setEnabled(enabled);
        portText.setEnabled(enabled);
        userText.setEnabled(enabled);
        passwordText.setEnabled(enabled);
        secure.setEnabled(enabled);
    }
    
}
