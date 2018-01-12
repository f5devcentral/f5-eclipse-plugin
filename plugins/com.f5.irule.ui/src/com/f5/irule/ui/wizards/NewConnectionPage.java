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

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.f5.irule.ui.Strings;
import com.f5.irule.ui.preferences.ConnectionSettingsBlock;
import com.f5.irule.ui.preferences.ProxySettingsBlock;

public class NewConnectionPage extends WizardPage {

    private static Logger logger = Logger.getLogger(NewConnectionPage.class);

    private ConnectionSettingsBlock connectionSettingsBlock;
    private ProxySettingsBlock proxyBlock;
    private Text IPText;

    protected NewConnectionPage() {
        super("NewConnectionPage");
        setTitle(Strings.LABEL_NEW_CONNECTION_TITLE);
        setMessage(Strings.LABEL_NEW_CONNECTION_MSG);
        setImageDescriptor(ImageDescriptor.createFromFile(NewConnectionPage.class, "/images/f5-53.png"));
        connectionSettingsBlock = new ConnectionSettingsBlock();
        proxyBlock = new ProxySettingsBlock();
    }
    
    public String getIP() {
        return getTextFrom(IPText);
    }

    ConnectionSettingsBlock getSettingsBlock() {
        return connectionSettingsBlock;
    }

    ProxySettingsBlock getProxyBlock() {
        return proxyBlock;
    }

    private String getTextFrom(Text text) {
        return text == null || text.isDisposed() ? null : text.getText();
    }

    @Override
    public void createControl(final Composite parent) {
        logger.debug("Create Page " + getClass().getSimpleName());

        Composite page = new Composite(parent, SWT.NONE);
        page.setLayout(new GridLayout(1, false));
        page.setLayoutData(new GridData(GridData.FILL_BOTH));
        setControl(page);
        setPageComplete(false);
        CompleteListener listener = new CompleteListener();
        
        // Layout
        Composite bigIpComposite = new Composite(page, SWT.NONE);
        bigIpComposite.setLayout(new GridLayout(2, false));
        bigIpComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        IPText = addText(bigIpComposite, Strings.LABEL_MGMT_IP, SWT.BORDER, listener);
        connectionSettingsBlock.addCredentialsControl(bigIpComposite, listener);

        //Proxy parameters
        Composite proxyComposite = new Composite(page, SWT.NONE);
        proxyComposite.setLayout(new GridLayout(2, false));
        proxyComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        proxyBlock.addProxyControl(proxyComposite, listener);

        // Add a Help Listener that displays an information dialog when the user press the '?' sign
        final Dialog helpDialog = new HelpDialog(parent.getShell());
        page.addHelpListener(new HelpListener() {
            public void helpRequested(HelpEvent helpEvent) {
                /*int returnCode = */helpDialog.open();
            }
        });
    }

    public static Text addText(Composite composite, String labelValue, int style, KeyListener listener) {
        Label label = new Label(composite, SWT.NONE);
        Text text = new Text(composite, style);
        setControl(text, label, labelValue, listener);
        return text;
    }

    public static Button addButton(Composite composite, String labelValue, int style) {
        Label label = new Label(composite, SWT.NONE);
        Button button = new Button(composite, style);
        setControl(button, label, labelValue, null);
        return button;
    }

    public static void setControl(Control valueControl, Label key, String keylabel, KeyListener listener) {
        key.setText(keylabel);
        valueControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (listener != null) {
            valueControl.addKeyListener(listener);
        }
    }

    private class CompleteListener implements KeyListener {
        public void keyPressed(KeyEvent e) {
        }

        public void keyReleased(KeyEvent e) {
            boolean hasIP = !"".equals(getIP());
            boolean hasUser = !"".equals(connectionSettingsBlock.getUser());
            boolean hasPassword = !"".equals(connectionSettingsBlock.getPassword());
            setPageComplete(hasIP && hasUser && hasPassword);
        }
    }

    private static final int HELP_DIALOG_COLUMNS = 3;

    private static Composite createGridDataComposite(Composite parent, int style, GridData gridData) {
        Composite helpComposite = new Composite(parent, style);
        helpComposite.setLayout(new GridLayout(HELP_DIALOG_COLUMNS, false));
        helpComposite.setLayoutData(gridData);
        return helpComposite;
    }

    public static void addHelpItem(Composite parent, String key, String value) {
        Label userKey = new Label(parent, SWT.NONE);
        userKey.setText(key);
        Label emtyLabel = new Label(parent, SWT.NONE);
        emtyLabel.setText("\t");
        Label userHelp = new Label(parent, SWT.NONE);
        userHelp.setText(value);
    }
    
    /**
     * {@link Dialog} that shows Help information about 'New Connection Page' wizard dialog.
     */
    private static class HelpDialog extends Dialog {

        public HelpDialog(Shell parent) {
            super(parent);
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            
            Composite helpComposite = new Composite(parent, SWT.OK);
            helpComposite.setLayout(new GridLayout(1, false));
            helpComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
            // Credentials Info
            Composite credentialsComposite = createGridDataComposite(helpComposite, SWT.NONE,
                new GridData(SWT.FILL, SWT.FILL, true, true));
            addHelpItem(credentialsComposite, Strings.LABEL_MGMT_IP, Strings.LABEL_MGMT_IP_HELP);
            ConnectionSettingsBlock.addCredentialsControlHelp(credentialsComposite);
            // Proxy Info
            Composite proxyComposite = createGridDataComposite(helpComposite, SWT.NONE,
                new GridData(SWT.FILL, SWT.FILL, true, true));
            ProxySettingsBlock.addProxyControlHelp(proxyComposite);
            return helpComposite;
        }

        // Overriding this method allow setting the title of the dialog
        @Override
        protected void configureShell(Shell newShell) {
            super.configureShell(newShell);
            newShell.setText(Strings.LABEL_ADD_CONNECTION_HELP);
        }

        // Override this method to set the dialog OK button only.
        @Override
        protected void createButtonsForButtonBar(Composite parent) {
         createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
      }

        @Override
        protected Point getInitialSize() {
            return new Point(420, 380);
        }
        
    }
}
