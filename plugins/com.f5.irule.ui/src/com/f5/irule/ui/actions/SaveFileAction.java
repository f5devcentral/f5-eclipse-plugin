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
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;

import com.f5.irule.model.ModelObject;

/**
 * {@link Action} that saves the editor content to the model file.<br>
 * if the workLocally flag is true then the model would only be saved to the local file
 * and not synced with the Big-IP.
 */
public class SaveFileAction extends Action {

    private static Logger logger = Logger.getLogger(SaveFileAction.class);
    
    private ModelObject model;
    private IWorkbenchPage page;
    private IEditorPart editor;
    private boolean workLocally;

    public SaveFileAction(ModelObject model, IWorkbenchPage page, IEditorPart editor, boolean workLocally) {
        this.model = model;
        this.page = page;
        this.editor = editor;
        this.workLocally = workLocally;
    }

    public void run() {
        logger.debug("Save to File. workLocally=" + workLocally);
        model.setLocallyModified(workLocally);
        page.saveEditor(editor, false /* confirm */);
    }        
}