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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import com.f5.irule.model.BigIPConnection;
import com.f5.irule.model.IAppsLxModelFile;
import com.f5.irule.model.RequestCompletion;
import com.f5.irule.model.ModelObject;
import com.f5.irule.model.ModelObject.Type;
import com.f5.irule.model.ModelParent;
import com.f5.irule.model.RestFramework;
import com.f5.irule.model.RuleProvider;
import com.f5.irule.ui.Ids;
import com.f5.irule.ui.Strings;
import com.f5.irule.ui.views.Util;
import com.f5.rest.common.RestOperation.RestMethod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The completed() method parses the response for the<br> 
 * GET mgmt/shared/iapp/directory-management request.<br><br>
 * The response contains a list of iAppsLx packages.<br>
 * For each package it gets its list of files by calling<br>
 * GET mgmt/shared/iapp/directory-management-recursive/[packageName]<br>
 * Then it convert the list of items for each package into a tree
 * in order to display it in the UI tree.
 */
public class LoadIAppsLXCompletion extends RequestCompletion {
    
    private static Logger logger = Logger.getLogger(LoadIAppsLXCompletion.class);

    private static List<String> IGNORE_LIST = Arrays.asList(new String []{"temp","tmp","RPMS","enable"});

    private BigIPConnection conn;

	public LoadIAppsLXCompletion(BigIPConnection conn) {
		this.conn = conn;
	}
	
	@Override
	public String toString() {
	    StringBuilder builder = new StringBuilder();
	    builder.append("[").append(getClass().getSimpleName());
	    builder.append(" ").append(conn);
	    builder.append("]");
	    return builder.toString();
	}
	
	@Override
	public void completed(String method, String uri, JsonObject jsonBody) {

	    JsonArray files = (JsonArray) RuleProvider.parseElement(jsonBody, "files");
        logger.debug("Completed " + method + " " + uri + "\nfiles: " + files);
        if (files == null) {
            logger.warn("No files data in response to " + method + " " + uri);
        }
	    ModelParent iAppsLxFolder = conn.getIAppsLxFolder(Strings.IAPPLX_FOLDER_LABEL);
	    iAppsLxFolder.clearChildren(null);
	    for (JsonElement jsonElement : files) {
            String packageName = jsonElement.getAsString();
            if (ignore(packageName)) {
                logger.debug(packageName + " is ignored");
                continue;
            }
            getFilesList(iAppsLxFolder, packageName);
        }
        Util.syncWithUi();
	}

    private static boolean ignore(String packageName) {
        for (String ignored : IGNORE_LIST) {
            if (packageName.startsWith(ignored)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Use the mgmt/shared/iapp/directory-management-recursive api to get the files list
     * of the specified iAppsLx package.<br>
     * Than convert the files list to a tree in order to display in the UI tree.
     */
    private void getFilesList(ModelParent iAppsLxFolder, String packageName) {
        logger.debug("Get file list for " + packageName);
        IPath parentPath = iAppsLxFolder.getFilePath();
        IPath path = parentPath.append(packageName);
        ModelParent iAppsLxModel = new ModelParent(packageName, conn, null, Type.IAPPLX_MODEL_PACKAGE, path);
        iAppsLxFolder.addChild(iAppsLxModel);
        String endpoint = RestFramework.IAPP_DIRECTORY_MANAGEMENT_RECURSIVE + "/" + packageName;
        String uriValue = conn.getURI(endpoint).toString();
        GetFileListCompletion completion = new GetFileListCompletion(conn, iAppsLxModel);
        RestFramework.sendRequestJob(conn,
            RestMethod.GET, uriValue, "application/json", null, completion, Util.getMutex());
    }

    @Override
	public void failed(Exception ex, String method, String uri, String responseBody) {
        ex.printStackTrace();
	    logger.warn("Failed to get iAppLx info " + uri + ":\n" + ex.getMessage());
	    IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN, Strings.ERROR_GETTING_BLOCKS_INFO_FAILED, ex);
	    StatusManager.getManager().handle(status, StatusManager.LOG);
        Util.syncWithUi();
	}

	private static class GetFileListCompletion extends RequestCompletion {

		private BigIPConnection conn;
		private ModelParent iAppsLxModel;

		private GetFileListCompletion(BigIPConnection conn, ModelParent iAppsLxModel) {
			this.conn = conn;
			this.iAppsLxModel = iAppsLxModel;
		}

		@Override
		public void completed(String method, String uri, JsonObject jsonBody) {
            List<String> filesList = getFilesList(jsonBody);
		    String files = toString(filesList);
		    logger.debug("Completed " + method + " " + uri + "\nfiles: " + files);
		    if (filesList == null) {
		        return;
		    }
			FileTree tree = new FileTree();
			tree.addFiles(filesList);
			String modelName = iAppsLxModel.getName();
			FileTreeNode subTree = tree.getRoot();
	        Path remoteDir = new Path(modelName);
			subTree.addModels(iAppsLxModel, conn, remoteDir);
			Util.syncWithUi();
		}

		private static String toString(List<String> filesList) {
		    StringBuilder builder = new StringBuilder();
		    int size = filesList.size();
		    for (int i = 0; i < size; i++) {
		        String item = filesList.get(i);
		        builder.append("\t");
		        builder.append(item);
		        if (i < size - 1) {
		            builder.append("\n");
		        }
		    }
		    return builder.toString();
		}

        @Override
		public void failed(Exception ex, String method, String uri, String responseBody) {
            IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN, Strings.ERROR_GETTING_LISTFILES_INFO_FAILED, ex);
            StatusManager.getManager().handle(status, StatusManager.LOG);
            iAppsLxModel.getParent().removeChild(iAppsLxModel);
            Util.syncWithUi();
		}

	}

    /**
     * Parse the response json body
     * and convert its "files" element to String list.
     */
    private static List<String> getFilesList(JsonElement jsonBody) {
        JsonArray array = (JsonArray) RuleProvider.parseElement(jsonBody, "files");
        if (array == null) {
            return null;
        }
        List<String> list = new ArrayList<String>();
        for (JsonElement element : array) {
            String elementValue = element.getAsString();
            list.add(elementValue);
        }
        return list;
    }

    private static class FileTree {
        private FileTreeNode root = new FileTreeNode("Root");

        void addFiles(List<String> filesList){
	        for (String filePath : filesList) {
	            add(filePath);
	        }
	    }

	    private void add(String filePath) {
	        FileTreeNode current = root;
	        StringTokenizer s = new StringTokenizer(filePath, "/");
	        while(s.hasMoreElements()) {
	            String nodeName = (String)s.nextElement();
	            FileTreeNode child = current.getChild(nodeName);
	            if(child == null) {
	                current.children.add(new FileTreeNode(nodeName));
	                child = current.getChild(nodeName);
	            }
	            current = child;
	        }
	    }
	    
	    /*FileTreeNode getSubTree(String name){
	        return root.getSubTree(name);
	    }*/

        FileTreeNode getRoot() {
            return root;
        }

	}
	
	private static class FileTreeNode {

        String nodeName;
        ArrayList<FileTreeNode> children;

        public FileTreeNode(String treeName) {
            this.nodeName = treeName;
            children = new ArrayList<FileTreeNode>();
        }

        /*public FileTreeNode getSubTree(String name) {
            if (name.equals(nodeName)) {
                return this;
            }
            for (FileTreeNode child : children) {
                FileTreeNode subTree = child.getSubTree(name);
                if (subTree != null) {
                    return subTree;
                }
            }
            return null;
        }*/

        public FileTreeNode getChild(String data) {
            for(FileTreeNode n : children){
                if(n.nodeName.equals(data)){
                    return n;
                }
            }
            return null;
        }

        private boolean isLeaf() {
            return children.isEmpty();
        }

        void addModels(ModelParent parentModel, BigIPConnection conn, IPath remoteDir){

            IPath parentPath = parentModel.getFilePath();
            for (FileTreeNode child : children) {
                String childName = child.nodeName;
                IPath path = parentPath.append(childName);
                ModelObject modelChild;
                IPath remotePath = remoteDir.append(childName);
                if (child.isLeaf()) {
                    if (childName.indexOf(".") > -1) {
                        modelChild = new IAppsLxModelFile(childName, conn, Type.IAPPLX_MODEL, remotePath, path);                        
                    } else {
                        modelChild = new ModelParent(childName, conn, null, Type.IAPPLX_MODEL_DIR, path);
                    }
                } else {
                    modelChild = new ModelParent(childName, conn, null, Type.IAPPLX_MODEL_DIR, path);      
                    child.addModels((ModelParent) modelChild, conn, remotePath);
                }
                parentModel.addChild(modelChild);
            }
        }
        
        @Override
        public String toString() {
            StringBuilder ans = new StringBuilder();
            ans.append("[");
            appendData(ans, "");
            ans.append("]");
            return ans.toString();
        }

        private void appendData(StringBuilder builder, String spaces) {
            builder.append(spaces);
            builder.append(nodeName);
            for (FileTreeNode child : children) {
                builder.append("\n");
                child.appendData(builder, spaces + " ");
            }
        }

	}

}
