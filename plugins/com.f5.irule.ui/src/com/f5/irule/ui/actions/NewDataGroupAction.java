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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.f5.irule.model.BigIPConnection;
import com.f5.irule.model.BigIPConnection.Module;
import com.f5.irule.model.DataGroup;
import com.f5.irule.model.DataGroupsModelParent;
import com.f5.irule.model.ModelObject.Type;
import com.f5.irule.model.ModelUtils;
import com.f5.irule.model.RuleProvider;
import com.f5.irule.ui.Strings;
import com.f5.irule.ui.editor.datagroup.DataGroupEditor;

/**
 * Action for Adding a new {@link DataGroup}.<br>
 * It creates a nameless Data-Group and opens the {@link DataGroupEditor}<br>
 * in order to let the user fill the name and records details<br>
 * and then post the new Data-Group to the Big-Ip server.<br>
 * The data-group local file folder is the current partition folder
 */
public class NewDataGroupAction extends Action {

    private static Logger logger = Logger.getLogger(NewDataGroupAction.class);

    private BigIPConnection conn;
    private Type type;
    private Module module;
    private IPath folder;

    public NewDataGroupAction(DataGroupsModelParent parent) {
        conn = parent.getConnection();
        module = parent.getModule();
        type = RuleProvider.getDataGroupType(module);
        String partition = conn.getCurrentPartition();
        folder = new Path(partition);
        logger.debug("New-Data-Group Action module=" +
            module + " type=" + type + " folder=" + folder);
        ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
        Image addObjImage = sharedImages.getImage(ISharedImages.IMG_OBJ_ADD);
        ImageDescriptor imageDescriptor = ImageDescriptor.createFromImage(addObjImage);
        this.setImageDescriptor(imageDescriptor);
    }

    public void run() {

        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        String currentPartition = conn.getCurrentPartition();
        DataGroup dataGroup = new DataGroup(null, null, conn, currentPartition, type, module, folder);
        logger.debug("Created empty DataGroup. Open Editor");
        try {
            DataGroupEditor.openEditor(dataGroup, page);
        } catch (PartInitException ex) {
            logger.warn("Failed to open editor for " + dataGroup, ex);
            ModelUtils.logError(Strings.ERROR_FAILED_TO_OPEN_EDITOR + ": " + dataGroup, ex);
        }
    }
}
