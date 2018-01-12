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

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchWindow;

import com.f5.irule.model.BigIPConnection;
import com.f5.irule.model.ModelObject;
import com.f5.irule.model.RequestCompletion;
import com.f5.irule.ui.jobs.FlowTracker;
import com.f5.irule.ui.views.ExplorerContentProvider;
import com.f5.irule.ui.views.Util;
import com.f5.rest.common.RestRequestCompletion;

/**
 * This {@link Wizard} display a check-box list of all locally edited resources (new or modified).<br>
 * The user can then select the resources that have to be synched to the Big-IP<br>
 * it iterates over all the the selected resources and for each one schedule a job<br>
 * to update the BigIP about the model changed content.<br>
 * If it's new model it uses the {@link ModelObject #iControlRestPostJob(RestRequestCompletion)} method of the model.
 * Otherwise it uses the {@link ModelObject #iControlRestPatchJob(RestRequestCompletion)} method of the model
 */
public class SyncModifiedResourcesWizard extends Wizard {

    private static Logger logger = Logger.getLogger(SyncModifiedResourcesWizard.class);
    
    private static final String SYNC_MODIFIED_RESOURCES = "Sync Modified Resources";
    private BigIPConnection connection;

    private SyncModifiedResourcesPage page;
    private FlowTracker flowTracker;

    private boolean reloadConnectionOnCompletion;

    public SyncModifiedResourcesWizard(BigIPConnection connection, boolean reloadConnectionOnCompletion) {
        this.connection = connection;
        this.reloadConnectionOnCompletion = reloadConnectionOnCompletion;
        flowTracker = new FlowTracker(connection);
    }

    @Override
    public void addPages() {
        page = new SyncModifiedResourcesPage(SYNC_MODIFIED_RESOURCES, connection);
        addPage(page);
    }

    @Override
    public boolean performFinish() {
        List<ModelObject> selectedModels = getSelectedModels();
        logger.debug("Finishing Sync Wizard. selectedModels: " + selectedModels);
        if (selectedModels.isEmpty()) {
            reloadConnection(reloadConnectionOnCompletion, connection);
        } else {
            updateModels(selectedModels);
        }
        return true;
    }

    private void updateModels(List<ModelObject> selectedModels) {
        for (ModelObject model : selectedModels) {
            RequestCompletion completion = new PatchCompletion(flowTracker, model, connection, reloadConnectionOnCompletion);
            ISchedulingRule mutex = Util.getMutex();
            if (model.isLocallyAdded()) {
                model.iControlRestPostJob(completion, mutex);
            } else {
                model.iControlRestPatchJob(completion, mutex);
            }
        }
    }

    private List<ModelObject> getSelectedModels() {
        List<ModelObject> selectedModels = new LinkedList<ModelObject>();
        List<TableItem> selectedItems = page.getSelectedItems();
        for (TableItem item : selectedItems) {
            ModelObject model = (ModelObject) item.getData();
            selectedModels.add(model);
        }
        return selectedModels;
    }
    
    static void reloadConnection(boolean doReload, BigIPConnection connection) {
        if (doReload) {
            ExplorerContentProvider.reloadConnection(connection);                    
        } else {
            Util.syncWithUi();
        }
    }

    /**
     * Open the {@link SyncModifiedResourcesWizard} wizard.<br>
     * The wizard will present the user with a list of modified resources<br>
     * so the user would select the resources that will be synchronized to the Big-IP.<br>
     * When the user completes the wizard it will try to update all selected resources by calling<br>
     * the {@link ModelObject #iControlRestPatchJob(RestRequestCompletion)} method for each one<br>
     * @param connection The connection with modified resources
     * @param window
     * @param reloadConnectionOnCompletion flag for reloading the connection when finishing all updates.
     */
    public static void openWizard(BigIPConnection connection, IWorkbenchWindow window, boolean reloadConnectionOnCompletion) {
        Wizard wizard = new SyncModifiedResourcesWizard(connection, reloadConnectionOnCompletion);
        Shell shell = window.getShell();
        WizardDialog dialog = new WizardDialog(shell, wizard);
        dialog.open();
    }

}
