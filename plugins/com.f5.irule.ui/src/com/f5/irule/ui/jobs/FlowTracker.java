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

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import com.f5.irule.model.BigIPConnection;
import com.f5.irule.ui.Ids;
import com.f5.irule.ui.Strings;
import com.f5.irule.ui.views.Util;

public class FlowTracker {

    private static Logger logger = Logger.getLogger(FlowTracker.class);

    BigIPConnection connection;
    
    private List<String> successItems = new LinkedList<String>();
    private List<String> failedItems = new LinkedList<String>();
    
    public FlowTracker(BigIPConnection connection) {
        this.connection = connection;
    }

    /**
     * Add the item name to the the right list: success/failed according to the succcess flag.<br>
     * If connection job count is 0, submit the Finish Message and sync the UI. 
     */
    public int jobFinished(boolean success, String itemName, int jobCountValue) {
        if (success) {
            successItems.add(itemName);
        } else {
            failedItems.add(itemName);
        }
        //int jobCountValue = connection.getJobCount();
        if (jobCountValue == 0) {
            StringBuilder message = getFinishMessage();
            logger.debug(message);
            IStatus status = new Status(IStatus.INFO, Ids.PLUGIN, message.toString());
            StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.SHOW);
            Util.syncWithUi();
        }
        return jobCountValue;
    }

    private StringBuilder getFinishMessage() {
        StringBuilder message = new StringBuilder(Strings.LABEL_ONLINE_SYNCHRONIZATION_COMPLETED);
        appendItems(message, Strings.LABEL_ONLINE_SYNCHRONIZATION_SUCCESFUL_UPDATES, successItems);
        appendItems(message, Strings.LABEL_ONLINE_SYNCHRONIZATION_FAILED_UPDATES, failedItems);
        return message;
    }

    private static void appendItems(StringBuilder message, String listSubject, List<String> ruleNames) {
        int count = 1;
        if (ruleNames.size() > 0) {
            message.append("\n\n\t").append(listSubject).append(":");
            for (String successRuleName : ruleNames) {
                message.append("\n\t").append(count++).append(". ").append(successRuleName);
            }
        }
    }
}
