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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;

import com.f5.irule.model.RequestCompletion;
import com.f5.irule.model.ModelObject;
import com.f5.irule.ui.Strings;
import com.f5.irule.ui.views.ProcessResponseJobCompletion;
import com.f5.irule.ui.views.Util;
import com.f5.rest.common.RestRequestCompletion;

/**
 * {@link Action} that updates the Big-IP with the model content
 * using the {@link ModelObject #iControlRestPatch(RestRequestCompletion)} method.<br>
 * If the model editor is opened and dirty then it saves it first.
 */
public class SyncToBigIPAction extends Action {

    private static Logger logger = Logger.getLogger(SyncToBigIPAction.class);

    private ModelObject model;
    private IWorkbenchPage page;
    private IEditorPart editor;

    public SyncToBigIPAction(ModelObject model, IWorkbenchPage page, IEditorPart editor) {
        this.model = model;
        this.page = page;
        this.editor = editor;
    }

    public void run() {
        logger.debug("Sync " + model + " to Big-IP");
        IFile file = model.getFile();
        model.setLocallyModified(true);// To Avoid sending from the DeltaModelFileVisitor
        if (editor != null && editor.isDirty()) {
            page.saveEditor(editor, false);
        }
        RequestCompletion completion = new ProcessResponseJobCompletion(
            model, page, file, Strings.LABEL_FAILED_SYNCING_BIG_IP);
        ISchedulingRule mutex = Util.getMutex();
        if (model.isLocallyAdded()) {
            model.iControlRestPostJob(completion, mutex);
        } else {
            model.iControlRestPatchJob(completion, mutex);
        }
    }

}