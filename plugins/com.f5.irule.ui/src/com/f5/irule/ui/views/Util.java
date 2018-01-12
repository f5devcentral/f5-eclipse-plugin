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
package com.f5.irule.ui.views;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class Util {
    private static ExplorerContentProvider explorerContentProvider = null;
    public static void setExplorerContentProvider(ExplorerContentProvider provider) {
        explorerContentProvider = provider;
    }
    public static ExplorerContentProvider getExplorerContentProvider() {
        return explorerContentProvider;
    }

    public static void syncWithUi() {
        if (explorerContentProvider != null) {
            explorerContentProvider.syncWithUi();
        }
    }
    
    public static final boolean checkIPv4(final String ip) {
        boolean isIPv4;
        try {
            final InetAddress inet = InetAddress.getByName(ip);
            isIPv4 = inet.getHostAddress().equals(ip) && inet instanceof Inet4Address;
        } catch (final UnknownHostException e) {
            isIPv4 = false;
        }
        return isIPv4;
    }

    public static String getProjectNameFromSelected(ITreeSelection selected) {
        TreePath paths[] = selected.getPaths();
        if (selected.getFirstElement() != null && paths.length > 0) {
            // Assumes projections are top level tree objs
            Object projectElement = paths[0].getFirstSegment();
            return projectElement.toString();
        }
        return "";
    }
    
    /**
     * Extract the "message" element from the exception returned by {@link RestCompletion #Failed}
     * @param ex The exception 
     * @return Parsed message or the original if it cannot be parsed
     */
    public static String getMessageElementFromException(Exception ex) {
        String msg = ex.getMessage();
        JsonParser parser = new JsonParser();
        try {
            JsonObject o = parser.parse("{" + msg + "}").getAsJsonObject();
            JsonObject body = o.getAsJsonObject("body");
            if (body != null) {
                JsonElement message = body.get("message");
                if (message != null) {
                    return message.toString();
                }
            }
        } catch (Exception e){
            // couldn't parse it, just return the original message
        }
        return msg;
    }

    public static Object getSelectedParent(ITreeSelection selected) {
        TreePath paths[] = selected.getPaths();
        if (paths.length > 0) {
            return paths[0].getParentPath().getLastSegment();
        }
        return null;
    }

    public static ISchedulingRule getMutex() {
        return explorerContentProvider.getMutex();
    }
}
