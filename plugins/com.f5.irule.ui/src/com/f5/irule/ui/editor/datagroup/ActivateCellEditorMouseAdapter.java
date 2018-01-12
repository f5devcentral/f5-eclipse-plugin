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

import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * {@link MouseAdapter} that activates the Cell Editor on {@link MouseListener #mouseDown(MouseEvent)} event.<br>
 * It retrieves the Data-Group record {@link TreeItem} and the column that were clicked.<br>
 * Then it creates a {@link TextCellEditor}, sets its value according to the column index: (1 - record key, Else - record data)<br>
 * and adds a {@link DataGroupCellEditorListener} listener to the cell editor<br>
 * in order to apply the logic when the user changes the cell value.
 */
public class ActivateCellEditorMouseAdapter extends MouseAdapter {

    private static Logger logger = Logger.getLogger(ActivateCellEditorMouseAdapter.class);

    private DataGroupEditor editor;

    ActivateCellEditorMouseAdapter(DataGroupEditor editor) {
        this.editor = editor;
    }

    @Override
    public void mouseDown(MouseEvent event) {
        Point point = getFixedPoint(event);
        TreeItem pointItem = point == null ? null : editor.getItem(point);             
        int columnIndex = 1;
        if (pointItem == null) {
            pointItem = editor.addEmptyEntry();
        }
        else{
            columnIndex = event.x > DataGroupEditor.COLUMN_2_LIMIT ? 2 : 1;                    
        }

        logger.trace("mouseDown" + pointItem.getData() + "\n\tcolumnToEdit=" + columnIndex);

        Tree tree = editor.getTree();
        tree.showSelection();// ensure the cell editor is visible
        CellEditor cellEditor = new TextCellEditor(tree);

        DataGroupRecord record = (DataGroupRecord) pointItem.getData();
        String key = record.getRecordName();
        String data = record.getRecordData();
        String value = columnIndex == 1 ? key : data;
        if (value != null) {
            cellEditor.setValue(value);
        }

        DataGroupCellEditorListener listener = new DataGroupCellEditorListener(record, cellEditor, columnIndex, editor);
        cellEditor.addListener(listener);

        editor.setLayout(cellEditor.getLayoutData());
        Control control = cellEditor.getControl();
        editor.setEditor(control, pointItem, columnIndex);
        cellEditor.setFocus(); // give focus to the cell editor
    }

    /**
     * Get the {@link Point} according to the user click on the editor table.
     * The x coordinate from the {@link MouseEvent} does not correspond to the table items,
     * so it is calculated from the items bounds.
     */
    private Point getFixedPoint(MouseEvent event) {
        TreeItem item = editor.getTopItem();
        if (item == null) {
            return null;
        }
        Rectangle itemBounds = item.getBounds();
        int itemBoundsX = itemBounds.x;
        int x = itemBoundsX + 1;
        Point fixedPoint = new Point(x, event.y);
        Point point = fixedPoint;
        return point;
    }
}

