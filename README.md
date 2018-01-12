# F5 Programmability for iRules, iRules LX, iControl LX, and iApps LX

The *F5 Programmability Development Environment for Eclipse* allows you to use the Eclipse IDE to manage iRules, iRules LX, iControl LX, and iApps LX development. By using *Eclipse*, you can connect to one or more BIG-IP &reg; devices to view, modify or create iRules, iRules LX workspaces, iControl LX, or iApps LX applications. The editor functionality includes TCL/iRules and JavaScript language syntax highlighting, code completion, and hover documentation for the iRules API. Editor functionality includes syntax support for TCL/iRules, JavaScript, and JavaScript Object Notation (JSON) format, including syntax highlighting and code completion, as well as API documentation.

With this release, *F5 Programmability Development Environment for Eclipse* includes iControl LX and iApps LX development. The LX development environment supports opening an LX application, adding files or deleting files, and downloading files for offine mode editing. This release also includes support for proxy server use and offline-online mode that lets you edit files when not connected to a BIG-IP system (offline mode) and synchronize with a BIG-IP system when you reconnect (online mode). 

## Contents
+ F5 Programmability for Eclipse Workflow
+ System Requirements
+ Tested Configurations
+ Installation
+ Common Configuration Tasks
+ General Information about iRules editing 
+ General Information about iControl LX & iApps LX editing 
+ Data Group Editor
+ Online-Offline Mode
+ Common Edit Tasks
+ JavaScript Editing
+ File Editing
+ Developer Documentation
+ Filing an Issue
+ Known Issues


## F5 Programmability for Eclipse Workflow

The workflow for *F5 Programmability for Eclipse* allows you to load iControl LX, iApps LX, and iRules LX applications from a BIG-IP system, modify an application or create a new application, and save the application to local disk or to a BIG-IP system, or both. Eclipse lets you work using a connection to a BIG-IP system, or work offline with files stored on a local disk. When you reconnect to a BIG-IP system, Eclipse lets you choose which files to synchronize with a BIG-IP. Whether you are working on an iControl LX, iApps LX, or iRules LX application, Eclipse supports the structure of the application, allowing you to create workspaces, extensions, and rules for iRules LX, or files and folders for iControl LX & iApps LX.


## System Requirements

The system requirements for the *F5 Programmability Development Environment for Eclipse* are:

- Minimum Eclipse installed base version: Luna (v4.4).  Recommended: Neon (v4.6) or Oxygen (v4.7).  

- Java version 1.7 or later

- Network access to one or more F5 Networks BIG-IP systems: TMOS version 12.1 or later; iControl LX and iAppsLX require version 13.1

- Approximately 300 MB free disk space



## Tested Configurations

F5 Programmability for Eclipse, version 2.0, has been tested for compatibility with the following software :

OS | Eclipse Version | Java Version | TMOS Version  
---|-----------------|-----------------|--------------         
Linux (CentOS 6.8)  | Neon 4.6.3 | 1.7.0_111| 12.1, 13.0, 13.1
Linux (CentOS 6.9)  | Neon 4.6.3 | 1.8.0_131| 13.1
MacOS (v10.11.5) | Neon 4.6.1 | 1.8.0_91 | 12.1, 13.0, 13.1
MacOS (v10.11.5) | Oxygen 4.7.0 | 1.8.0_91 | 12.1, 13.0, 13.1
MacOS (v10.12.6) | Neon 4.6.3 | 1.8.0_131 | 13.1
Windows (v7)  | Luna 4.4.2 | 1.8.0_91 | 12.1, 13.0, 13.1
Windows (v7) | Neon 4.6.3 | 1.8.0_131| 13.1
Windows (v7) | Oxygen 4.7.0 | 1.8.0_91 | 13.1
Windows (v10) | Neon 4.6.3 | 1.8.0_131 | 13.1

## Installation

To install the *F5 Programmability for Eclipse* plug-in, 

1. Start *Eclipse*

2. Click on **Help > Install New Software**

3. Add a repository for the F5 plug-in  

4. Click **Add**  Type the text "http://cdn.f5.com/product/f5-eclipse/v2" into the *Location* field (omit the quotation marks).  Click **OK**

5. After you add the repository, **F5 Networks** or **F5 Programmability for Eclipse** should apppear in the **Available Software** dialog. Check the box next to the F5 item.

6. Check the **Contact all update sites during install to find required software** box and click **Next**.

7. Clear the **Group items by category** check box.

8. After you review the installation items, click **Next**.

9. Read the License Agreement, check **I accept the terms of the license agreement**, and click **Finish**

10. When prompted to restart Eclipse, click **Yes**


To upgrade an existing installation of the plug-in,

1. Start *Eclipse*

2. Click on **Help > Install New Software**

3. Click on the **already installed** link.

4. Select **F5 Programmability for Eclipse**

5. Click **Update...**


## Common Configuration Tasks

#### Select the **F5** perspective

To use the F5 plug-in, you must activate the F5 perspective. In Eclipse, 

1. Click on **Window > Perspective  > Open Perspective > Other**

2. Find and select the F5 perspective

3.  Click **OK**  

In the F5 perspective, three views are visible: Explorer pane on the left hand side, Editor pane on the right hand side, and a Log panel along the bottom.  
 
#### Connect to a BIG-IP system

The user account must have been assigned a role of administrator. Every Big-IP system includes a user with the name admin which has an administrator role. For creating additional users with an administrator role, refer to the BIG-IP guide for User Account Administration.

1. Click the **New BIG-IP Connection** toolbar button (at the top of the Explorer pane)

2. When prompted, provide the IP address and credentials for the BIG-IP system. You may store your credentials in a *Secure Store*, which encrypts the credentials using the Master Password Provider. (As a separate step, you can click on **Window > Preferences > General > Security > Secure Storage** to configure the Master Password Provider)

3. Click **Finish**

You may repeat this process to connect to multiple BIG-IP systems simultaneously.


#### Connect via a proxy server

The Eclipse plug-in supports a connection to a BIG-IP server through a proxy server, which forwards the packets to a BIG-IP server. 

1. Click the **New BIG-IP Connection** toolbar button (at the top of the Explorer pane)

2. Provide the IP address and credentials for the BIG-IP server, as described in the previous procedure. You may store your credentials in a *Secure Store*, which encrypts the credentials, as explained in the previous topic. 

3. Check the **Use Proxy** checkbox, then provide the IP address, port number, and credentials for the proxy server. The Secure Store option applies separately to the credentials you provide for connecting to a BIG-IP system and the proxy server credentials. For this step, you are using Secure Store for the proxy server credentials.

4. Click **Finish**

When you connect to a BIG-IP system, all LTM iRules, Data Groups, GTM iRules, iRules LX workspaces, and iApps are loaded. The process may take a few seconds. When the process completes, you can expand the connection folder and subfolders. Note that folders exist for provisioned product modules on the BIG-IP system to which you connected. If you have not provisioned a particular module, no folder appears for that module.

#### Change the BIG-IP partition

The default partition **Common** is loaded when you connect to a BIG-IP system. If you would like to use a different partition,

1. In the Explorer, select the **BIG-IP Connection** 

2. Click either the **Gears** toolbar button, or use the context menu (right click) to open the Project Properties dialog.

3. Select a new partition

4. Click **OK**. 

Content will be loaded from the BIG-IP partition you selected.

## General information about iRule editing

The editor used for editing iRules includes code completion and hover documentation for iRule commands and events. Consistent with the default Eclipse behavior, typing **Ctrl-Space** will check for available completion proposals for the current word. Also, completions are invoked after adding a colon (":") to a word. This trick is handy for completion of namespaced iRule commands, e.g., TCP::. The same completion popup documentation can be seen by hovering over an iRule command or event.

iRules syntax validation is a feature of the editor that validates an iRule when it is saved. In addition to syntax checking, validation detects encoding differences. For example, validation may fail for an iRule created using Eclipse on a Windows system that includes a line continuation character ('\'). To resolve the validation error,

1. Click **Window > Preferences**

2. Click **General** to display the menu items

3. Click **Workspace** to display the settings screen

4. Under *Text file encoding*, Click **Other**, then click **UTF-8** from the dropdown list

5. Under *New text file line delimiter*, Click **Other**, then click **Unix** from the dropdown list

6. Click **Apply and Close**

The F5 plug-in does not include the man pages for the standard Tcl commands. To enable hover documentation for those standard Tcl commands, you can download the man pages and link them in to the Tcl and iRule editors. The manual pages can be downloaded from here: https://www.mirrorservice.org/sites.ftp.tcl.tk/pub/tcl/tcl8_4/tcl8.4.0-html.tar.gz. Once uncompressed, the containing directory can be linked to Eclipse via the global preferences dialog (**Window > Preferences** for Linux and Windows, **Eclipse > Preferences** for MacOS). Under the **Tcl > Man Pages** section, click **Configure**, then click **Add**. Specify a name of your choosing for the documentation set and add the file path to the local directory which contains the docs.

## General information about iApps LX editing

The Eclipse project explorer displays any existing iControl LX & iApps LX projects discovered upon connection to a BIG-IP system. The structure of an iControl LX / iApps LX project differs from an iRule or iRules LX project. An iControl LX / iApps LX project consists of files and directories, and the context menu items in the IDE support the activities of adding and deleting files and directories.
Note that for the iControlLX projects to show up on disk, they must be located under: /var/config/rest/iapps

When you modify a file in online mode, the changes are written to the BIG-IP system. The **Save** action causes the daemon process to restart. Therefore, any changes you made to the application are in effect following the restart, with a notable exception. If you save changes to a file in the *presentation* folder, that change will not trigger a restart. 

## Data Group editor

A data group consists of key-value pairs, which represent configuration data that exists independently of an iRule. Because the data group exists independently, you can make changes to the data group without editing an iRule. Assuming that the changes do not introduce errors, revisions to a data group should not impact an iRule.

To add a new data group,

1. Right click on the *Data Group* folder and click on **New Data Group**.

2. On the **New Data Group** tab, supply a name for the data group.

3. For each item you add to the data group, select a type and then click on a cell to supply a key or data value.

4. Right click on the **New Data Group** tab to save (or close) the new data group.


## Online/offline mode

Eclipse supports two modes of operation: online and offline. Online mode requires a connection to a BIG-IP system. In online mode, the explorer displays all of the resources available on a BIG-IP system. When you open a resource, you download the resource to the local filesystem. 

You can perform any of the following while in online mode:

  * Open, create, or edit a resource
  * Save to a local file and update the file on the BIG-IP system
  * Save only to a local file
  * Reload the resource from the BIG-IP system
  * Delete the resource
  
In online mode, any save action saves the file locally and pushes the changes to the BIG-IP system. In offline mode, the action saves the file locally.

The transition to offline mode requires resources to be downloaded to the local filesystem when you switch from online mode. Use the context menu to choose **Offline** mode. Resource download may take quite a bit time, depending on the number of files to download. When you open a file in offline mode, Eclipse loads the file from disk. If you have not downloaded all of the projects, the only files available for editing in offline mode are those files you have edited previously.

When you transition back to online mode, Eclipse attempts to connect to a BIG-IP system. Use the context menu to choose **Online** mode. Validation of changes to files edited in offline mode occurs after the transition to online mode. Eclipse displays a list of file changes and a dialog to allow you to choose which files to sync with a BIG-IP system. You can choose to not sync a local copy of a file to a BIG-IP system, and you can choose to reload from a BIG-IP system and overwrite local files.



## Common Edit Tasks

#### Open a file to view or edit (Data groups, iRules, iRules LX, iControl LX, or iApps LX)

1. Click **Open** in the context menu, or double-click on the file in the Explorer. The content is pulled from the BIG-IP and an edit tab is created. This step creates a file on the local filesystem.

#### Add or delete iRules, ILX workspaces, extensions, and files (Data groups, iRules, iRules LX, iControl LX, or iApps LX)

1. Use the context menu (right click) for Add and Delete menu items. Menu items to add ILX workspaces or extensions are available in folders that contain iRules and iRules LX resources.

#### Saving a file (Data groups, iRules, iRules LX, iControl LX, or iApps LX)

1. Use **File > Save**, the **Save** button on the toolbar, or type **Ctrl-S**. A **Save** action validates iRules and iRules LX files.

#### Reloading a file (Data groups, iRules, iRules LX, iControl LX, or iApps LX)

1. Each file has a **Reload** context menu selection. Click **Reload** to reload the edit buffer and sync the changes with the BIG-IP system.

#### Reloading all BIG-IP content (Data groups, iRules, iRules LX, iControl LX, or iApps LX)

1. To reload all content from each connected BIG-IP system, click **Reload All** toolbar button in the Explorer. This action closes all open editors after prompting you to save any unsaved changes.

## JavaScript editing

Opening a JavaScript file within an iRules LX project will invoke a specific JavaScript editor (from the JSDT plug-in). This editor includes syntax highlighting, validation, completions, etc. The editing features can be configured via the global preferences dialog under the **JavaScript** section (**Window > Preferences** for Linux and Windows, **Eclipse > Preferences** for MacOS).

## File editing

File types other than iRule or JavaScript open in an appropriate editor as determined by Eclipse. Depending on the file associations you have configured for your operating system, Eclipse may open an external editor. In some situations, Eclipse may open an editor that is not appropriate for a file type. To override this setting and choose the editor type of your own choosing, you can define a file association via the global preferences dialog under the **General > Editors > File Associations** section.

To modify or add a file association or set a default editor,

1. Click on **Window > Preferences**

2. Expand **General**, then expand **Editors**

3. Click on **File Associations**

4. Select the file type from the list of *File types* then select an editor from the *Associated editors* list and click **Default** to set the default editor
    
5. To add a new editor, choose **Add** to add a new editor, either an internal editor or an external program.  Then select the new editor from the *Associated editors* list and click **Default** to set it as the default editor

Do not change the file association for Tcl files (\*.tcl). The default setting should remain *Tcl Source Editor*.

## Developer Documentation
If you would like to modify or improve the plug-in, developer documentation can be found on the [Wiki tab](https://github.com/f5devcentral/f5-eclipse-plugin/wiki) for this project.

## Filing an Issue
If you discover a problem with the plug-in, please file an issue on the [Issues tab](https://github.com/f5devcentral/f5-eclipse-plugin/issues) for this project. 

## Known Issues

ID 592459 Some F5 Perspective Dialogs do not include Help button content.

ID 594055 The formatter page within the Project Properties dialog can cause an error: "The currently displayed page contains invalid values". The Eclipse bug is tracked here: https://bugs.eclipse.org/bugs/show_bug.cgi?id=476261

ID 594413 Code folding within an iRule 'when' block only works for contained comment blocks, not code blocks.

ID 598035 iRule command completion does not work with Tcl execute statements. The workaround is to compose the statement outside the context of square brackets, then add the brackets later.

ID 676007 Attempt to expand ILX node_modules folder with substantial contents results in UI hanging

