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
package com.f5.irule.ui.editor.datagroup;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import com.f5.irule.model.DataGroup;

public class CloseDataGroupEditorRunnable implements Runnable {

    private IWorkbenchPage page;
    private DataGroup dataGroup;

    public CloseDataGroupEditorRunnable(DataGroup dataGroup) {        
        this.dataGroup = dataGroup;
        this.page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    }

    @Override
    public void run() {
        IEditorReference[] editorReferences = page.getEditorReferences();
        for (IEditorReference editorReference : editorReferences) {
            IEditorPart editor = editorReference.getEditor(false);
            if (editor instanceof DataGroupEditor) {
                DataGroupEditor dataGroupEditor = (DataGroupEditor) editor;
                DataGroupEditorInput dataGroupEditorInput = (DataGroupEditorInput) editor.getEditorInput();
                DataGroup inputDataGroup = dataGroupEditorInput.getDataGroup();
                if (inputDataGroup.equals(dataGroup)) {
                    dataGroupEditor.closeEditor(page);
                }
            }
        }
    }

}
