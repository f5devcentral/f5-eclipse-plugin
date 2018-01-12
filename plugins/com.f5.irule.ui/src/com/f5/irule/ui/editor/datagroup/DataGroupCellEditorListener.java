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

import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import com.f5.irule.model.DataGroup;
import com.f5.irule.model.Messages;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * This listener applyEditorValue() method takes the new value from the {@link CellEditor}
 * and compares it to the old value.
 * The old value is determined according to the columIndex variable: 1-Name, Else-Data.
 * The listener checks if the new value is valid and if so, it updates the UI {@link Tree}
 */
public class DataGroupCellEditorListener implements ICellEditorListener {

    private static Logger logger = Logger.getLogger(DataGroupCellEditorListener.class);

    private DataGroupRecord record;
    private CellEditor editor;
    private int columnIndex;

    private DataGroupEditor dataGroupEditor;

    DataGroupCellEditorListener(DataGroupRecord record, CellEditor editor, int columnIndex, DataGroupEditor dataGroupEditor) {
        this.record = record;
        this.editor = editor;
        this.columnIndex = columnIndex;
        this.dataGroupEditor = dataGroupEditor;
    }

    @Override
    public void editorValueChanged(boolean oldValidState, boolean newValidState) {
        logger.trace("oldValidState=" + oldValidState + " newValidState=" + newValidState);
        dataGroupEditor.setDirty(true);
    }
    
    @Override
    public void cancelEditor() {
        logger.debug("cancel");
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(DataGroupCellEditorListener.class.getSimpleName());
        builder.append(" ").append(record);
        builder.append(" columnIndex=").append(columnIndex);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public void applyEditorValue() {
        String newValue = (String) editor.getValue();
        if (newValue == null) {
            return;
        }
        if (columnIndex == 1) {
            String recordName = record.getRecordName();
            if (!newValue.equals(recordName)) {
                updateKeyChange(newValue, recordName);
            }
        } else {
            String recordData = record.getRecordData();
            if (!newValue.equals(recordData)) {
                updateDataChange(newValue, recordData);
            }
        }
    }

    /**
     * Update the UI {@link Tree} with the record key change.<br>
     * If the new key value already exist in the table<br>
     * or the key has an invalid value then alert the user and return to the old value,<br>
     * Otherwise set the record TreeItem key column with the new value.
     */
    private void updateKeyChange(String newValue, String oldValue) {
        String recordName = record.getRecordName();
        DataGroup dataGroup = record.getDataGroup();
        logger.trace(recordName + " element of " + dataGroup + " Changed from " + oldValue + " to " + newValue);
        
        Text cellText = (Text) editor.getControl();
        Tree tree = (Tree) cellText.getParent();
        if (!newValue.equals("") && hasOtherItem(tree, newValue, record)) {
            logger.debug("Group-Data already contain record " + oldValue);
            String message = Messages.DATA_GROUP_ALREADY_CONTAIN_RECORD + " " + newValue;
            DataGroupEditor.logMessage(message, IStatus.ERROR, null);
            editor.setValue(oldValue == null ? "" : oldValue);
            record.setRecordName(oldValue);          
        }
        else {
            setItemText(1, newValue);
            record.setRecordName(newValue);
        }
    }

    /**
     * Iterate over the {@link Tree} items and check if there is an item,
     * wrapping a different {@link DataGroupRecord} from the fromRecord record,
     * that its key text (column 1) equal to the given key.
     */
    private static boolean hasOtherItem(Tree tree, String key, DataGroupRecord fromRecord) {
        
        TreeItem[] items = tree.getItems();
        for (int i = 0; i < items.length; i++) {
            TreeItem item = items[i];
            DataGroupRecord itemRecord = (DataGroupRecord) item.getData();
            if (itemRecord == fromRecord) {
                continue;
            }
            String itemKey = item.getText(1);
            if (key.equals(itemKey)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Update the UI {@link Tree} with the record data change.
     * Set the record TreeItem data column with the new value.
     */
    private void updateDataChange(String newValue, String oldValue) {
        String recordName = record.getRecordName();
        DataGroup dataGroup = record.getDataGroup();
        logger.debug("Change Data of record " + recordName + " of " + dataGroup + " from " + oldValue + " to " + newValue);
        setItemText(2, newValue);
        record.setRecordData(newValue);
    }

    /**
     * Set the record {@link TreeItem} text with the new value
     */
    private void setItemText(int columnIndex, String newValue) {
        Tree tree = (Tree) editor.getControl().getParent();
        int itemIndex = record.getItemIndex();
        TreeItem treeItem = tree.getItem(itemIndex);
        treeItem.setText(columnIndex, newValue);
    }

    static JsonArray toJsonArray(List<JsonObject> tempRecordsList) {
        JsonArray copyRecords = new JsonArray();
        for (JsonObject element : tempRecordsList) {
            copyRecords.add(element);
        }
        return copyRecords;
    }

}
