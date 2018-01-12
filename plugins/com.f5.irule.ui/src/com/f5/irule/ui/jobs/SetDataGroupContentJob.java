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
package com.f5.irule.ui.jobs;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.statushandlers.StatusManager;

import com.f5.irule.model.DataGroup;
import com.f5.irule.model.Ids;
import com.f5.irule.model.Messages;
import com.f5.irule.ui.Strings;
import com.f5.irule.ui.editor.datagroup.DataGroupEditor;
import com.google.gson.JsonArray;

/**
 * A {@link Job} that sets the {@link DataGroup} content on its local file
 */
public class SetDataGroupContentJob extends Job {

    private static Logger logger = Logger.getLogger(SetDataGroupContentJob.class);

    private DataGroup dataGroup;
    private JsonArray tempRecords;

    private boolean isNew;

    public SetDataGroupContentJob(DataGroup dataGroup, JsonArray tempRecords, DataGroupEditor editor, boolean isNew) {
        super(Strings.JOB_WRITE_DATA_GROUP_FILE);
        this.dataGroup = dataGroup;
        this.tempRecords = tempRecords;
        this.isNew = isNew;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        logger.debug("Set " + dataGroup + " file content using " + tempRecords);
        String content = dataGroup.getContent(tempRecords);
        try {
            dataGroup.setDataGroupFileContent(content, isNew);
        } catch (CoreException ex) {
            IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN, Messages.FAILED_SETTING_DATA_GROUP_CONTENT, ex);
            StatusManager.getManager().handle(status, StatusManager.LOG);
        }
        return Status.OK_STATUS;
    }
    
}