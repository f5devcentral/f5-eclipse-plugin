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
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;

public class DataGroupEditorPartListener implements IPartListener {

    private static Logger logger = Logger.getLogger(DataGroupEditorPartListener.class);
    
    @Override
    public void partOpened(IWorkbenchPart part) {
        logger.trace("Opened: " + part);
    }
    
    @Override
    public void partClosed(IWorkbenchPart part) {
        logger.trace("Closed: " + part);
        if (part instanceof DataGroupEditor) {
            DataGroupEditor editor = (DataGroupEditor) part;
            editor.setClosed(true);
        }
    }

    @Override
    public void partActivated(IWorkbenchPart part) {
        logger.trace("Activated: " + part);
    }

    @Override
    public void partDeactivated(IWorkbenchPart part) {
        logger.trace("Deactivated: " + part);
    }
    
    @Override
    public void partBroughtToTop(IWorkbenchPart part) {
        logger.trace("Brought To Top: " + part);
    }        
}