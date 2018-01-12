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

import org.apache.log4j.Logger;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorMatchingStrategy;
import org.eclipse.ui.IEditorReference;

import com.f5.irule.model.DataGroup;

public class DataGroupEditorMatchingStrategy implements IEditorMatchingStrategy {

    private static Logger logger = Logger.getLogger(DataGroupEditorMatchingStrategy.class);

    @Override
    public boolean matches(IEditorReference editorRef, IEditorInput input) {
        DataGroupEditor editor = (DataGroupEditor) editorRef.getEditor(false);
        logger.trace("input=" + input + " editor=" + editor);
        IEditorInput editorInput = editor.getEditorInput();
        if (input.equals(editorInput)) {
            DataGroup dataGroup = (DataGroup) ((DataGroupEditorInput)input).getDataGroup();
            // update opened editor with the new Data-Group records
            editor.updateTree(dataGroup);
            return true;
        } else {
            return false;
        }
    }

}
