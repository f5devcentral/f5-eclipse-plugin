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
package com.f5.irule.ui;

public class Strings {
    public static final String ERROR_CANNOT_CONNECT = "Cannot connect";
    public static final String ERROR_CANNOT_DELETE_RESOURCE = "Cannot delete resource";
    public static final String ERROR_CONNECTION_UNREACHABLE_GOING_OFFLINE = "Connection unreachable, going offline.\n"
            + "Only the files which have been edited will be available locally.";
    public static final String ERROR_FAILED_SETTING_CONTENT = "Failed setting content of ";
    public static final String ERROR_FAILED_TO_CREATE_RESOURCE = "Failed to create resource";
    public static final String ERROR_FAILED_TO_DELETE_RESOURCE = "Failed to delete resource";
    public static final String ERROR_FAILED_TO_OPEN_EDITOR = "Failed to open editor";
    public static final String ERROR_FAILED_TO_RETRIEVE_FILE = "Failed to retrieve file";
    public static final String ERROR_FAILED_TO_RETRIEVE_PARTITIONS = "Failed to retrieve partition info";
    public static final String ERROR_FAILED_TO_RETRIEVE_PROVISIONING = "Failed to retrieve provisioning info";
    public static final String ERROR_FAILED_TO_RETRIEVE_VERSION_INFO = "Failed to retrieve version info";
    public static final String ERROR_FILE_RESOURCE_DOES_NOT_EXIST = "File resource does not exist";
    public static final String ERROR_GETTING_BLOCKS_INFO_FAILED = "Getting iAppLx Blocks info failed";
    public static final String ERROR_GETTING_LISTFILES_INFO_FAILED = "Getting LISTFILES info failed";
    public static final String ERROR_INVALID_INTEGER_FORMAT = "Invalid integer format ";
    public static final String ERROR_LISTFILES_OPERATION_FAILED = "LISTFILES operation failed";
    public static final String ERROR_LOADING_DATA_GROUPS_FAILED = "Loading Data Groups failed";
    public static final String ERROR_LOADING_ILX_CONTENT_FAILED = "Loading ILX content failed";
    public static final String ERROR_LOADING_ILX_WORKSPACES_FAILED = "Loading ILX Workspaces failed";
    public static final String ERROR_LOADING_IRULES_FAILED = "Loading iRules failed";
    public static final String ERROR_MAXIMUM_VERSION = "Cannot connect.  Maximum TMOS version is:";
    public static final String ERROR_MINIMUM_VERSION = "Cannot connect.  Minimum TMOS version is:";
    public static final String ERROR_MUST_FILL_DATA_GROUP_NAME = "Must specify Data Group Name";
    public static final String ERROR_OUT_OF_RANGE = " is out of range. Maximum allowed integer is " + Integer.MAX_VALUE;
    public static final String ERROR_PROXY_IP_MUST_BE_DEFINED = "Proxy ip must be defined";
    public static final String ERROR_PROXY_PORT_MUST_BE_DEFINED = "Proxy port must be defined";
    public static final String ERROR_RESOURCE_EXISTS = "A resource with that name already exists";

    public static final String IAPPLX_FOLDER_LABEL = "iApps LX";
    public static final String ILX_WORKSPACES_FOLDER = "iRules LX Workspaces";
    public static final String INFO_INIT_PROJECT = "Initializing Project: ";
    public static final String INFO_PLEASE_SELECT_PROJECT = "Please select a project";
    public static final String INFO_RESOURCE_DELETED = "Resource Deleted: ";
    public static final String IRULES_GTM_FOLDER_LABEL = "iRules (GTM)";
    public static final String IRULES_LTM_FOLDER_LABEL = "iRules (LTM)";    
    
    public static final String JOB_PROVISIONING = "Connecting";
    public static final String JOB_CREATING_RESOURCE = "Creating resource";
    public static final String JOB_DELETING_RESOURCE = "Deleting resource";
    public static final String JOB_GETTING_IRULES = "Getting iRules";
    public static final String JOB_GETTING_DATA_GROUPS = "Getting Data Groups";
    public static final String JOB_GETTING_ILX_WORKSPACES = "Getting ILX Workspaces";
    public static final String JOB_GETTING_PARTITIONS = "Getting Partitions";
    public static final String JOB_GETTING_VERSION = "Getting Version";
    public static final String JOB_UPDATING_EXPLORER = "Updating Explorer";
    public static final String JOB_OPENING_EDITOR = "Opening Editor";
    public static final String JOB_POST_DATA_GROUP = "Post Data Group";
    public static final String JOB_PATCH_DATA_GROUP = "Patch Data Group";
    public static final String JOB_DELETE_DATA_GROUP = "Delete Data Group";
    public static final String JOB_WRITE_DATA_GROUP_FILE = "Write Data Group File";
    public static final String JOB_GETTING_IRULES_CONTENTS = "Getting IRules contents";
    public static final String JOB_OPEN_PROJECT = "Open Project";
    public static final String JOB_LOAD_CONNECTION_JOB = "Load connection job";
    public static final String JOB_GETTING_IAPPLX_CONTENT = "Getting IAppLX content";
    public static final String JOB_CREATE_LISTFILES_TASK = "Create List-Files task";
    public static final String JOB_GET_FILES_LIST = "Get files list";
    public static final String JOB_GETTING_IAPPLX_FILE_CONTENT = "Getting IAppLX file content";

    public static final String LABEL_DATA_GROUPS = "Data Groups";
    public static final String LABEL_DATA_GROUP = "Data Group";
    public static final String LABEL_NEW_DATA_GROUP = "New Data Group";
    public static final String LABEL_DATA_GROUP_PART_NAME = "Data Group : ";
    public static final String LABEL_DATA_GROUP_RECORDS_EDITOR = "Data Group records Editor";
    public static final String LABEL_DATA_GROUP_NAME = "Name:";
    public static final String LABEL_DATA_GROUP_KEY = "Key";
    public static final String LABEL_DATA_GROUP_TYPE = "Type:";
    public static final String LABEL_DATA_GROUP_STRING = "string";
    public static final String LABEL_DATA_GROUP_IP = "ip";
    public static final String LABEL_DATA_GROUP_INTEGER = "integer";
    public static final String LABEL_DATA_GROUP_DATA_COLUMN = "Data";
    public static final String LABEL_DATA_GROUP_INDEX_COLUMN = "#";
    
    public static final String LABEL_CREATE_A_NEW = "Create a new";    
    public static final String LABEL_DELETE = "Delete";
    public static final String LABEL_DELETE_CONFIRMATION = "Delete Confirmation";
    public static final String LABEL_DISCONNECTED = "(Disconnected)";
    public static final String LABEL_LOADING = "(Loading)";
    public static final String LABEL_CAPITALIZED_EXTENSION = "Extension";
    public static final String LABEL_CAPITALIZED_FILE = "File";
    public static final String LABEL_CAPITALIZED_FOLDER = "Folder";
    public static final String LABEL_CAPITALIZED_WORKSPACE = "Workspace";
    public static final String LABEL_CONNECTION_PROPERTIES = "Connection Properties";
    public static final String LABEL_DATA_GROUP_INVALID_DATA = "Data-Group invalid data:";
    public static final String LABEL_EDIT_CONNECTION_PROPERTIES = "Edit project (connection) properties";
    public static final String LABEL_EXPAND_FOLDER = "Expand Folder";
    public static final String LABEL_EXTENSION = "extension";
    public static final String LABEL_FILE = "file";
    public static final String LABEL_FOLDER = "folder";
    public static final String LABEL_IRULE= "iRule";
    public static final String LABEL_LINE = "\nline ";
    public static final String LABEL_MGMT_IP = "BIG-IP Management Address:";
    public static final String LABEL_MGMT_IP_HELP = "IP of Big-IP.\nFor example '10.11.12.13'";
    public static final String LABEL_NAME = "Name";
    public static final String LABEL_TYPE = "Type";
    public static final String LABEL_PATH = "Path";
    public static final String LABEL_NEW = "New";
    public static final String LABEL_NEW_CONNECTION_ACTION = "New BIG-IP Connection";
    public static final String LABEL_NEW_CONNECTION_TITLE = "Add New BIG-IP Project and Connection";
    public static final String LABEL_NEW_CONNECTION_MSG = "Enter project and authentication info";
    public static final String LABEL_OPEN = "Open";
    public static final String LABEL_PASSWORD = "Password:";
    public static final String LABEL_PASSWORD_HELP = "Credentials password.\nFor example '12345'";
    public static final String LABEL_PROXY_PASSWORD_HELP = "Credentials password for the proxy server.\nFor example 'qwerty'";
    public static final String LABEL_RELOAD = "Reload";
    public static final String LABEL_RELOAD_ALL = "Reload All";
    public static final String LABEL_SECURE_STORE = "Store credentials in Secure Store";
    public static final String LABEL_SECURE_STORE_HELP = "Check for Secure Store";
    
    public static final String LABEL_USE_PROXY = "Use Proxy";
    public static final String LABEL_USE_PROXY_HELP = "Check if connection goes through http proxy";
    public static final String LABEL_PROXY_ADDRESS = "Proxy Address";
    public static final String LABEL_PROXY_ADDRESS_HELP = "Proxy Address IP.\nFor example '10.20.30.40'";
    public static final String LABEL_PROXY_PORT = "Proxy Port";
    public static final String LABEL_PROXY_PORT_HELP = "Proxy Address Port\nFor example '8888'";

    public static final String LABEL_USER = "User:";
    public static final String LABEL_USER_HELP = "Credentials user.\nFor example 'admin'";
    public static final String LABEL_PROXY_USER_HELP = "Credentials User for the proxy server.\nFor example 'proxyUser'";
    public static final String LABEL_TMOS_PARTITION = "TMOS Partition (default is \"Common\")";
    public static final String LABEL_WORKSPACE = "workspace";
    public static final String LABEL_ONLINE = "Online";
    public static final String LABEL_OFFLINE = "Offline";
    public static final String LABEL_ONLINE_SYNCHRONIZATION_COMPLETED = "Online Synchronization completed";
    public static final String LABEL_ONLINE_SYNCHRONIZATION_SUCCESFUL_UPDATES = "Succesful Updates";
    public static final String LABEL_ONLINE_SYNCHRONIZATION_FAILED_UPDATES = "Failed Updates";
    public static final String LABEL_SAVE = "Save";
    public static final String LABEL_SAVE_TO_LOCAL_FILE = "Save To Local File";
    public static final String LABEL_SYNC_FILE_TO_BIG_IP = "Sync File To Big-IP";
    public static final String LABEL_SAVE_TO_BIG_IP = "Save to Big-IP";
    public static final String LABEL_OPEN_LOCAL_FILE = "Open Local File";
    public static final String LABEL_RELOAD_DISCARD_LOCAL_CHANGES = "Reload (Discard Local Changes)";
    public static final String LABEL_SYNC_MODIFIED_RESOURCES = "Sync Modified Resources";
    public static final String LABEL_FAILED_SYNCING_BIG_IP = "Failed syncing Big-IP";
    public static final String LABEL_FAILED_PATCHING = "Failed updating ";
    public static final String LABEL_FAILED_POSTING = "Failed posting ";
    public static final String LABEL_SELECET_RESOURCES_TO_SYNCHRONIZE = "Select resources to synchronize";
    public static final String LABEL_SELECT_ALL = "Select All";
    public static final String LABEL_DESELECT_ALL = "Deselect All";
    public static final String LABEL_ADD_CONNECTION_HELP = "Add Connection Help";
    
    public static final String MESSAGE_DELETE_CONFIRM_PREAMBLE = "Are you sure you want to delete";
    public static final String MESSAGE_DELETE_CONNECTION = "?\nOnly the Eclipse project will be deleted, the Big-IP will remain unchanged";
    public static final String FROM_THE_CONTAINING_PROJECT = "from the containing project";
    public static final String AND_THE_CONNECTED_BIG_IP = " and the connected Big-IP ?";
    public static final String OFFLINE_MODE_ONLY_LOCAL_RESOURCE_WOULD_BE_DELETED = " ?\n\n* Offline Mode, Only local resource would be deleted";
    public static final String MESSAGE_ILLEGAL_NAME = "The name may only contain alphanumeric, underscore, hyphen and dot characters";
    public static final String MESSAGE_PROJECT_EXISTS = "A project with the specified address already exists";
    
    public static String msg(String... args) {
        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            sb.append(" ");
            sb.append(arg);
        }
        return sb.toString();
    }
}
