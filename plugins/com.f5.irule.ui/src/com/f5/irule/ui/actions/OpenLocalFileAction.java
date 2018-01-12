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
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPage;

import com.f5.irule.model.ModelObject;
import com.f5.irule.ui.views.IruleView;

/**
 * Action that opens the model IDE from the local file.
 */
public class OpenLocalFileAction extends Action {

    private static Logger logger = Logger.getLogger(OpenLocalFileAction.class);
    
    private ModelObject model;
    private IWorkbenchPage page;

    public OpenLocalFileAction(ModelObject model, IWorkbenchPage page) {
        this.model = model;
        this.page = page;
    }

    public void run() {
        IFile file = model.getFile();            
        logger.debug("Getting contect from local file " + file);
        IruleView.openIdeEditor(page, file, model);
    }        
}