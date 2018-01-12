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

import java.io.InputStream;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.statushandlers.StatusManager;

import com.f5.irule.model.BigIPConnection;
import com.f5.irule.model.BigIPConnection.Module;
import com.f5.irule.model.Connection;
import com.f5.irule.model.DataGroup;
import com.f5.irule.model.ModelObject;
import com.f5.irule.model.ModelObject.Type;
import com.f5.irule.model.ModelParent;
import com.f5.irule.model.ModelUtils;
import com.f5.irule.model.RuleProvider;
import com.f5.irule.ui.Ids;
import com.f5.irule.ui.Strings;
import com.f5.irule.ui.jobs.SetDataGroupContentJob;
import com.f5.irule.ui.views.ExplorerContentProvider;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Editor for the Data-Group Model.
 * This Editor serves both editing an existing Data-Group or creating a new one.
 * Its {@link IWorkbenchPage #createPartControl(Composite)} method builds the editor ui view.
 * It initialize a new Tree, set its text listeners
 * and update the tree with the records from the {@link DataGroup} in the editor input.
 * When used for creating a new Data-Group,
 * the editor also presents a 'Name' Text box to let the user set the Data-Group name
 * and a 'Type' combo (string, ip or number) to let the user set the Data-Group type.
 */
public class DataGroupEditor extends EditorPart {

    private static Logger logger = Logger.getLogger(DataGroupEditor.class);
    
    private static final int[] COLUMN_WIDTHS = {75, 150, 100};
    static final int COLUMN_2_LIMIT = COLUMN_WIDTHS[0] + COLUMN_WIDTHS[1];
    
    private static final String[] TYPE_ITEMS = new String[] {
        Strings.LABEL_DATA_GROUP_STRING, Strings.LABEL_DATA_GROUP_IP, Strings.LABEL_DATA_GROUP_INTEGER };

    private Tree tree;
    private TreeEditor treeEditor;
    private Composite parent;

    private Composite nameComposite;
    private Composite typeComposite;

    private boolean dirty;
    private boolean closed;

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        logger.trace("input=" + input);
        
        if (!(input instanceof DataGroupEditorInput)) {
            throw new PartInitException("Wrong input");
        }
        setSite(site);
        setInput(input);
        String inputName = input.getName();
        setPartName(inputName == null ? Strings.LABEL_NEW_DATA_GROUP :
            Strings.LABEL_DATA_GROUP_PART_NAME + inputName);
    }

    @Override
    public void setFocus() {
        logger.trace("setFocus() " + this);
    }

    /* 
     * First check that the Data-Group has a valid key values.
     * In case of invalid value for some record key then alert the user
     * and do not proceed with saving and updating the Big-IP.
     * Otherwise, If the connection is in Online Mode and the Data-Group is not flagged as locallyModified
     * then Update the BigIP with the Data-Group change.
     * Otherwise (Offline) Save the Data-Group to the local file.
     */
    @Override
    public void doSave(IProgressMonitor monitor) {
        DataGroupEditorInput editorInput = (DataGroupEditorInput) getEditorInput();
        DataGroup dataGroup = editorInput.getDataGroup();
        String name = dataGroup.getName();
        // If it's a new Data-Group then take the type from the combo box
        String dataGroupType = (name == null) ? getComboText(typeComposite) :
            dataGroup.getDataGroupType();
        
        JsonArray tempRecords;
        try {
            tempRecords = createTreeRecords(dataGroupType);
        } catch (IllegalAccessException ex) {
            logger.warn(ex.getMessage());
            logMessage(ex.getMessage(), IStatus.ERROR, null);
            return;
        }

        logger.debug("Save Data-Group " + name);
        boolean isNew;
        if (name == null) {
            // Post New Data-Group
            // The name is null when the editor is used for creating a new Data-Group.
            // Take the new data-group name from the nameComposite Text.
            String dataGroupName = getText(nameComposite);
            dataGroup = createNewDataGroup(dataGroup, tempRecords, dataGroupName, dataGroupType);
            addDataGroupToModelTree(dataGroup);
            isNew = true;
        } else {
            // Patch Data-Group
            logger.debug("Save " + dataGroup);
            isNew = false;
        }
        Job job = new SetDataGroupContentJob(dataGroup, tempRecords, this, isNew);
        job.schedule();
        setDirty(false);
    }
    
    /**
     * Add the {@link DataGroup} to the UI Irules tree view.
     */
    private static void addDataGroupToModelTree(DataGroup dataGroup) {
        Connection conn = dataGroup.getConnection();
        Module module = dataGroup.getModule();
        String moduleLabel = module == Module.ltm ? Strings.IRULES_LTM_FOLDER_LABEL : Strings.IRULES_GTM_FOLDER_LABEL;
        ModelParent model = (ModelParent) conn.getModel(moduleLabel, null);
        ModelParent dataGroupsModel = (ModelParent) model.getModel(Strings.LABEL_DATA_GROUPS, null);                
        dataGroupsModel.addChild(dataGroup);
    }

    private DataGroup createNewDataGroup(DataGroup dataGroup, JsonArray tempRecords, String dataGroupName,
        String dataGroupType) {
        if (dataGroupName == null || dataGroupName.equals("")) {
            logger.debug("Data-Group name not filled !!!");
            logMessage(Strings.ERROR_MUST_FILL_DATA_GROUP_NAME, IStatus.ERROR, null);
            return null;
        }
        else if (dataGroupType == null || dataGroupType.equals("")) {
            logger.debug("Data-Group type not filled !!!");
            logMessage(Strings.ERROR_MUST_FILL_DATA_GROUP_NAME, IStatus.ERROR, null);
            return null;
        }
        else{
            String comboType = getComboType();
            BigIPConnection connection = dataGroup.getConnection();
            Type type = dataGroup.getType();
            Module module = dataGroup.getModule();
            String partition = dataGroup.getPartition();
            //IPath folder = dataGroup.getFolder();
            IPath path = ExplorerContentProvider.getModuleFolderPath(partition, module);
            IPath folder = path.append(DataGroup.FOLDER_NAME);
            // Create a new Data-Group with the connection, type, module and folder of the given data-group
            DataGroup newDataGroup = new DataGroup(dataGroupName, dataGroupType, connection, partition, type, module, folder);
            newDataGroup.setDataGroupType(comboType);
            newDataGroup.setTempRecords(tempRecords);
            return newDataGroup;
        }
    }

    @Override
    public void doSaveAs() {
        logger.debug("DataGroupEditor.doSaveAs() " + this);
    }

    @Override
    public boolean isDirty() {
        logger.trace("dirty = " + dirty);
        return dirty;
    }

    /**
     * Set the dirty flag and fire a {@link IEditorPart #PROP_DIRTY} change event.
     * This would set/unset the * indication in the EditorPart
     */
    void setDirty(boolean value) {
        logger.trace("Set dirty to " + value);
        dirty = value;
        firePropertyChange(PROP_DIRTY);
    }

    void setClosed(boolean closed) {
        this.closed = closed;
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof DataGroupEditor) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[DataGroupEditor ");
        IEditorInput editorInput = getEditorInput();
        builder.append(editorInput);
        builder.append("]");
        return builder.toString();
    }

    Tree getTree() {
        return tree;
    }

    /**
     * Specify the Text control that is to be displayed in the tree
     * and the cell in the tree that it is to be positioned above. 
     */
    void setEditor(Control control, TreeItem item, int columnIndex) {
        treeEditor.setEditor(control, item, columnIndex);
    }

    void setLayout(CellEditor.LayoutData layout) {
        treeEditor.horizontalAlignment = layout.horizontalAlignment;
        treeEditor.grabHorizontal = layout.grabHorizontal;
        treeEditor.minimumWidth = layout.minimumWidth;
    }

    TreeItem getItem(Point point) {
        return tree.getItem(point);
    }

    TreeItem getTopItem() {
        return tree.getTopItem();
    }

    @Override
    public void createPartControl(Composite parent) {

        this.parent = parent;
        logger.debug("DataGroupEditor.createPartControl(" + parent + ")");
        DataGroupEditorInput input = (DataGroupEditorInput) getEditorInput();
        DataGroup dataGroup = (DataGroup) input.getDataGroup();
        parent.setLayout(new GridLayout(1, true));

        int emptyEntriesCount;
        String dataGroupType;
        String modelName = dataGroup.getName();
        if (modelName == null) {
            nameComposite = addCompositeLabel(parent, Strings.LABEL_DATA_GROUP_NAME, 400);
            new Text(nameComposite, SWT.SINGLE | SWT.BORDER);
            createTypeCombo(parent);
            dataGroupType = Strings.LABEL_DATA_GROUP_KEY;
            emptyEntriesCount = 20;
        } else {
            nameComposite = addCompositeLabel(parent, Strings.LABEL_DATA_GROUP + ":", -1);
            addLabel(nameComposite, modelName, true, 14);
            dataGroupType = retrieveDataGroupType(dataGroup);
            emptyEntriesCount = 5;
        }
        initTree(dataGroup, dataGroupType);
        updateTree(dataGroup);
        addEmptyEntries(emptyEntriesCount);
    }

    private void addLabel(Composite parent, String text, boolean bold, int height) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        if (bold) {
            Font font = new Font(label.getDisplay(), new FontData("Arial", height, SWT.BOLD));
            label.setFont(font);
        }
    }

    private Composite addCompositeLabel(Composite parent, String labelText, int keyWidth) {
        Composite labelComposite = new Composite(parent, parent.getStyle());
        GridLayout cellLayout = new GridLayout(2, true);
        labelComposite.setLayout(cellLayout);
        addLabel(labelComposite, labelText, false, 12);
        return labelComposite;
    }

    /**
     * Create a {@link Combo} widget with possible data-group types (string, ip or number)
     */
    private void createTypeCombo(Composite parent) {
        typeComposite = addCompositeLabel(parent, Strings.LABEL_DATA_GROUP_TYPE, 70);
        Combo typeCombo = new Combo(typeComposite, SWT.READ_ONLY);
        typeCombo.setItems(TYPE_ITEMS);
        typeCombo.setText(TYPE_ITEMS[0]);
    }

    /**
     * Initialize a new {@link Tree}.<br>
     * Add the {@link ActivateCellEditorMouseAdapter} as mouse listener to the tree,<br>
     * so on MouseListener.mouseDown(MouseEvent) event it would create and activate a {@link TextCellEditor}<br>
     * with a {@link DataGroupCellEditorListener} in order to apply the logic when the user changes the cell value.
     */
    private void initTree(DataGroup dataGroup, String dataGroupType) {
        
        ScrolledComposite composite = createComposite(parent);
        
        tree = new Tree(composite, SWT.MULTI | SWT.BORDER);
        composite.setMinSize(tree.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        composite.setContent(tree);
                
        tree.setLinesVisible(true);
        tree.setHeaderVisible(true);
        tree.addMouseListener(new ActivateCellEditorMouseAdapter(this));
        tree.setData(dataGroup);        
        treeEditor = new TreeEditor(tree);
        setColumns(tree, dataGroupType);
    }

    private static ScrolledComposite createComposite(Composite parent) {
        ScrolledComposite composite = new ScrolledComposite(parent, SWT.V_SCROLL);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        composite.setExpandHorizontal(true);
        composite.setExpandVertical(true);
        composite.setAlwaysShowScrollBars(true);
        return composite;
    }

    private static String retrieveDataGroupType(DataGroup dataGroup) {
        String dataGroupType = dataGroup.getDataGroupType();
        dataGroupType = dataGroupType.substring(0, 1).toUpperCase() + dataGroupType.substring(1);
        return dataGroupType;
    }

    /**
     * Update this editor {@link Tree} with the records of the given {@link DataGroup}.
     * First remove all existing items from the tree.
     * Then iterate on the Data-Group records and for each one,
     * add a {@link TreeItem} to the tree.
     */
    public void updateTree(DataGroup dataGroup) {

        if (closed) {
            return;
        }
        logger.debug("Update Editor with " + dataGroup);
        tree.removeAll();
        
        JsonArray jsonRecords = dataGroup.getJsonRecords();
        if (jsonRecords != null) {
            int size = jsonRecords.size();
            // Update the records in reverse to avoid O(N^2) time
            // since internally the tree uses a singly-linked list.
            for (int i = size - 1; i >= 0; i--) {
                JsonObject jsonObject = (JsonObject) jsonRecords.get(i);
                String name = DataGroup.getElementValue(jsonObject, DataGroup.NAME);
                String data = DataGroup.getElementValue(jsonObject, DataGroup.DATA);
                TreeItem item = new TreeItem(tree, SWT.NONE, 0);
                DataGroupRecord entry = new DataGroupRecord(name, data, i, dataGroup);
                item.setData(entry);
                item.setText(new String[] { String.valueOf(i + 1), name, data });
            }
        }   
        setDirty(false);
    }

    private void addEmptyEntries(int entriesCount) {
        for (int i = 0; i < entriesCount; i++) {
            addEmptyEntry();
        }
    }

    TreeItem addEmptyEntry() {
        // Add Empty Entry
        int itemCount = tree.getItemCount();
        DataGroup dataGroup = (DataGroup) tree.getData();
        TreeItem item = new TreeItem(tree, SWT.NONE, itemCount);
        DataGroupRecord entry = new DataGroupRecord(null, null, itemCount, dataGroup);
        item.setData(entry);
        item.setText(new String[] { "\t", null, null});
        return item;
    }

    /**
     * Iterate over the {@link Tree} items, create a {@link JsonObject} from each item
     * and return a {@link JsonArray} of all items objects.<br>
     * For each item, check that it has a valid key,
     * In case it has some records with invalid keys
     * then throw an {@link IllegalArgumentException}
     */
    private JsonArray createTreeRecords(String dataGroupType) throws IllegalAccessException {
        JsonArray tempRecords = new JsonArray();
        StringBuilder errorBuilder = null;
        int index = 1;
        for (TreeItem treeItem : tree.getItems()) {
            String nameValue = treeItem.getText(1);
            if (nameValue != null && !nameValue.equals("")) {
                String errorMessage = checkKey(nameValue, dataGroupType);
                if (errorMessage != null) {
                    if (errorBuilder == null) {
                        errorBuilder = new StringBuilder(Strings.LABEL_DATA_GROUP_INVALID_DATA);
                    }
                    errorBuilder.append(Strings.LABEL_LINE + index + ": " + errorMessage);
                }
                String dataValue = treeItem.getText(2);
                JsonObject element = DataGroup.createJsonObject(nameValue, dataValue);
                tempRecords.add(element);
            }
            index++;
        }
        if (errorBuilder != null) {
            throw new IllegalAccessException(errorBuilder.toString());
        }
        return tempRecords;
    }

    /**
     * Check the key for a valid value.<br>
     * If the {@link DataGroup} type is integer then try to parse it as int.<br>
     * In case of {@link NumberFormatException} produce an error message:<br>
     * Out of range Or Invalid integer format, according to the key value.
     */
    private static String checkKey(String key, String dataGroupType) {
        if (key.equals("")) {
            return null;
        }
        String errorMessage = null;
        if ("integer".equals(dataGroupType)) {
            try {
                Integer.parseInt(key);
            } catch (NumberFormatException e) {
                errorMessage = key.matches("[0-9]+") ? key + Strings.ERROR_OUT_OF_RANGE :
                    Strings.ERROR_INVALID_INTEGER_FORMAT + key;
            }
        }
        return errorMessage;
    }
    
    public static void logMessage(String message, int level, Exception ex) {
        IStatus status = ex == null ?
            new Status(level, Ids.PLUGIN, message) :
            new Status(level, Ids.PLUGIN, message, ex);
        // If its an ERROR message then also show the error message as a dialog in the UI
        int style = level == IStatus.ERROR ? StatusManager.LOG | StatusManager.SHOW : StatusManager.LOG;
        StatusManager.getManager().handle(status, style);
    }

    /**
     * Open the {@link DataGroupEditor} for the specified {@link DataGroup}
     */
    public static void openEditor(DataGroup dataGroup, IWorkbenchPage page) throws PartInitException{ 
        String editorId = DataGroupEditor.class.getName();
        IEditorInput input = new DataGroupEditorInput(dataGroup);
        page.addPartListener(new DataGroupEditorPartListener());
        page.openEditor(input, editorId);
    }
    
    /**
     * Set the columns of the {@link Tree}
     */
    private static void setColumns(Tree tree, String dataGroupType) {
        String[] columnLabels = {Strings.LABEL_DATA_GROUP_INDEX_COLUMN,
            dataGroupType, Strings.LABEL_DATA_GROUP_DATA_COLUMN};
        for (int i = 0; i < columnLabels.length; i++) {
            TreeColumn column = new TreeColumn(tree, SWT.LEFT);
            String text = columnLabels[i];
            column.setText(text);
            int width = COLUMN_WIDTHS[i];
            column.setWidth(width);
        }
    }

    public void closeEditor(final IWorkbenchPage page) {
        logger.debug("Close " + this);
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                page.closeEditor(DataGroupEditor.this, false);
            }
        });
    }

    private String getText(Composite composite) {
        Control[] children = composite.getChildren();
        Text labelText = (Text) getChild(children, Text.class);
        if (labelText == null) {
            return null;
        }
        String text = labelText.getText();
        return text;
    }

    private String getComboText(Composite composite) {
        Control[] children = composite.getChildren();
        Combo labelCombo = (Combo) getChild(children, Combo.class);
        if (labelCombo == null) {
            return null;
        }
        String text = labelCombo.getText();
        return text;
    }

    private String getComboType() {
        Control[] children = typeComposite.getChildren();
        Combo typeComno = (Combo) getChild(children, Combo.class);
        String text = typeComno.getText();
        return text;
    }

    private static Control getChild(Control[] children, Class<?> clazz) {
        for (Control child : children) {
            if (clazz.equals(child.getClass())) {
                return child;
            }
        }
        return null;
    }

    /**
     * Create a Runnable that opens the {@link DataGroupEditor} for the given {@link DataGroup}.<br>
     * If the connection is in Offline mode
     * then update the data-group model from the local file first
     */
    public static Runnable getOpenEditorRunnable(DataGroup dataGroup, IWorkbenchPage page) {
        BigIPConnection connection = (BigIPConnection) dataGroup.getConnection();
        boolean onlineMode = connection.isOnlineMode();
        if (!onlineMode) {
            IProject proj = dataGroup.getProject();
            IPath filePath = dataGroup.getFilePath();
            logger.debug("Build DataGroup " + dataGroup + " from file " + filePath);
            try {
                InputStream contents = ModelObject.getFileContents(proj, filePath);
                JsonObject root = RuleProvider.getRootObject(contents);
                dataGroup.update(root);
            } catch (CoreException ex) {
                logger.warn("Failed to get contents of " + filePath, ex);
                ModelUtils.logError(Strings.ERROR_FAILED_TO_OPEN_EDITOR, ex);
                return null;
            }
        }
        Runnable runnable = new OpenDataGroupEditorRunnable(dataGroup, page);
        return runnable;
    }

    
    /**
     * Open the {@link DataGroupEditor} for the specified {@link DataGroup}
     */
    private static class OpenDataGroupEditorRunnable implements Runnable {

        private DataGroup dataGroup;
        private IWorkbenchPage page;

        private OpenDataGroupEditorRunnable(DataGroup dataGroup, IWorkbenchPage page) {
            this.dataGroup = dataGroup;
            this.page = page;
        }

        @Override
        public void run() {
            try {
                // Need to figure out how to configure ui plugin.xml
                // to associate dataGroup file extension with the DataGroupEditor
                // meanwhile doing it here
                logger.trace("Open Editor for DataGroup " + dataGroup);
                DataGroupEditor.openEditor(dataGroup, page);
            } catch (PartInitException ex) {
                logger.warn("Failed to open editor for " + dataGroup, ex);
                ModelUtils.logError(Strings.ERROR_FAILED_TO_OPEN_EDITOR + ": " + dataGroup, ex);
            }
        }   
    }

}
