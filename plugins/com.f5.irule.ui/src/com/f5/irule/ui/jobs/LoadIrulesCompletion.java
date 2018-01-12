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

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import com.f5.irule.model.BigIPConnection;
import com.f5.irule.model.BigIPConnection.Module;
import com.f5.irule.model.RequestCompletion;
import com.f5.irule.model.Rule;
import com.f5.irule.model.RuleProvider;
import com.f5.irule.ui.Ids;
import com.f5.irule.ui.Strings;
import com.f5.irule.ui.views.ExplorerContentProvider;
import com.google.gson.JsonObject;

public class LoadIrulesCompletion extends RequestCompletion {

    private static Logger logger = Logger.getLogger(LoadIrulesCompletion.class);

    private BigIPConnection conn;
    private Module module;
    private ExplorerContentProvider provider;

    public LoadIrulesCompletion(BigIPConnection conn, Module module, ExplorerContentProvider provider) {
        this.conn = conn;
        this.module = module;
        this.provider = provider;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(getClass().getSimpleName());
        builder.append(" ").append(conn);
        builder.append(" ").append(module);
        builder.append("]");
        return builder.toString();
    }

    public void completed(String method, String uri, JsonObject jsonBody) {
        logger.debug("Completed " + method + " " + uri);
        HashMap<IPath, Rule> rules = RuleProvider.parseIrules(conn, module, jsonBody);
        String logMessage = getLogMessage(rules, uri);
        logger.debug(logMessage);
        ExplorerContentProvider.addParsedRules(rules);
        // Populate the explorer with found iRules
        provider.fillExplorerIrules(conn, module);
    }

    private static String getLogMessage(HashMap<IPath, Rule> rules, String uri) {
        StringBuilder logMessage = new StringBuilder("Load Rules from response to " + uri);
        for (IPath path : rules.keySet()) {
            logMessage.append("\n\t").append(path);
        }
        return logMessage.toString();
    }

    public void failed(Exception ex, String method, String uri, String responseBody) {
        IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN, Strings.ERROR_LOADING_IRULES_FAILED, ex);
        StatusManager.getManager().handle(status, StatusManager.LOG);
    }
}