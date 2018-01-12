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
package com.f5.irule.ui.wizards;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.f5.irule.model.BigIPConnection;
import com.f5.irule.model.ModelObject;
import com.f5.irule.ui.Strings;

@SuppressWarnings("deprecation")
public class SyncModifiedResourcesPage extends WizardPage {

    //private static Logger logger = Logger.getLogger(SyncModifiedResourcesPage.class);

    private TableViewer tableViewer;

    private BigIPConnection connection;

    public SyncModifiedResourcesPage(String pageName, BigIPConnection connection) {
        super(pageName);
        this.connection = connection;
        setTitle(pageName);
        Image image = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_SAVE_EDIT);
        setImageDescriptor(ImageDescriptor.createFromImage(image));
    }

    @Override
    public void createControl(Composite parent) {
        Composite page = new Composite(parent, SWT.NONE);
        setControl(page);
        setPageComplete(true);

        // Layout
        page.setLayout(new GridLayout(1, false));
        page.setLayoutData(new GridData(GridData.FILL_BOTH));

        Label label = new Label(page, SWT.NONE);
        label.setText(Strings.LABEL_SELECET_RESOURCES_TO_SYNCHRONIZE);
        label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        addTableViewer(page);
        
        Composite buttonsComposite = new Composite(page, SWT.NONE);
        buttonsComposite.setSize(300, 200);
        buttonsComposite.setLayout(new RowLayout());

        addSetItemsCheckedButton(buttonsComposite, Strings.LABEL_SELECT_ALL, true);
        addSetItemsCheckedButton(buttonsComposite, Strings.LABEL_DESELECT_ALL, false);
    }

    private void addTableViewer(Composite page) {
        // Create the table viewer to display the players
        tableViewer = new TableViewer(page, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE);
        // Set the content and label providers
        tableViewer.setContentProvider(new SyncModifiedResourcesContentProvider());
        tableViewer.setLabelProvider(new ModifiedModelLabelProvider());
        tableViewer.setSorter(new ModifiedModelSorter());
        // Set up the table
        Table table = tableViewer.getTable();
        table.setLayoutData(new GridData(GridData.FILL_BOTH));
        addTableColumn(table, ModifiedModelLabelProvider.COLUMN_MODULE_NAME, "Name");
        addTableColumn(table, ModifiedModelLabelProvider.COLUMN_MODULE_TYPE, "Type");
        addTableColumn(table, ModifiedModelLabelProvider.COLUMN_MODULE_PATH, "Path");
        // Turn on the header and the lines
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        update(connection);
        setItemsChecked(true);
        // Pack the columns
        for (int i = 0, n = table.getColumnCount(); i < n; i++) {
            TableColumn column = table.getColumn(i);
            column.pack();
        }
    }

    private void addSetItemsCheckedButton(Composite buttonsComposite,
            String buttonText, boolean checked) {
        Button button = new Button(buttonsComposite, SWT.PUSH);
        button.setText(buttonText);
        button.addSelectionListener(new SetItemsCheckedSelectionListener(this, checked));
    }
    
    private static class SetItemsCheckedSelectionListener implements SelectionListener {
        private boolean checked;
        private SyncModifiedResourcesPage page;
        public SetItemsCheckedSelectionListener(SyncModifiedResourcesPage page, boolean checked) {
            this.page = page;
            this.checked = checked;
        }
        public void widgetSelected(SelectionEvent event) {
            page.setItemsChecked(checked);
        }
        public void widgetDefaultSelected(SelectionEvent event) {
            page.setItemsChecked(checked);
        }        
    }

    private void setItemsChecked(boolean checked) {
        Table table = tableViewer.getTable();
        TableItem[] items = table.getItems();
        for (TableItem item : items) {
            item.setChecked(checked);
        }
    }

    private void addTableColumn(Table table, int column, String columnText) {
        TableColumn tc = new TableColumn(table, SWT.LEFT);
        tc.setText(columnText);
        tc.addSelectionListener(new SelectionAdapterImpl(tableViewer, column));
    }

    private void update(BigIPConnection connection) {
        // Update the window's title bar with the new team
        String text = connection + ": Sync Modified Resources";
        Shell shell = getShell();
        shell.setText(text);
        // Set the table viewer's input to the team
        tableViewer.setInput(connection);
    }

    List<TableItem> getSelectedItems() {

        List<TableItem> list = new LinkedList<TableItem>();
        Table table = tableViewer.getTable();
        TableItem[] items = table.getItems();
        for (TableItem item : items) {
            if (item.getChecked()) {
                list.add(item);
            }
        }
        return list;
    }

    private static class SelectionAdapterImpl extends SelectionAdapter {
        private TableViewer tableViewer;
        private int column;

        public SelectionAdapterImpl(TableViewer tableViewer, int column) {
            this.tableViewer = tableViewer;
            this.column = column;
        }

        public void widgetSelected(SelectionEvent event) {
            ViewerSorter sorter = tableViewer.getSorter();
            ((ModifiedModelSorter) sorter).doSort(column);
            tableViewer.refresh();
        }   
    }

    private static class ModifiedModelSorter extends ViewerSorter {

        private static final int ASCENDING = 0;
        private static final int DESCENDING = 1;
        private int column;
        private int direction;

        /**
         * Does the sort. If it's a different column from the previous sort, do an
         * ascending sort. If it's the same column as the last sort, toggle the sort
         * direction.
         * @param column
         */
        public void doSort(int column) {
            if (column == this.column) {
                // Same column as last sort; toggle the direction
                direction = 1 - direction;
            } else {
                // New column; do an ascending sort
                this.column = column;
                direction = ASCENDING;
            }
        }

        /**
         * Compares the object for sorting
         */
        public int compare(Viewer viewer, Object e1, Object e2) {
            int rc = 0;
            ModelObject p1 = (ModelObject) e1;
            ModelObject p2 = (ModelObject) e2;
            // Determine which column and do the appropriate sort
            switch (column) {
            case ModifiedModelLabelProvider.COLUMN_MODULE_NAME:
                rc = collator.compare(p1.getName(), p2.getName());
                break;
            case ModifiedModelLabelProvider.COLUMN_MODULE_TYPE:
                rc = collator.compare(p1.getType().name(), p2.getType().name());
                break;
            case ModifiedModelLabelProvider.COLUMN_MODULE_PATH:
                rc = collator.compare(p1.getFilePath().toString(), p2.getFilePath().toString());
                break;
            }
            // If descending order, flip the direction
            if (direction == DESCENDING){
                rc = -rc;
            }
            return rc;
        }
    }

    private static class SyncModifiedResourcesContentProvider implements IStructuredContentProvider {

        private static Logger logger = Logger.getLogger(SyncModifiedResourcesContentProvider.class);

        @Override
        public Object[] getElements(Object obj) {
            // Returns all the players in the specified team
            BigIPConnection connection = (BigIPConnection) obj;
            List<ModelObject> modifiedModels = connection.getEditedModels();
            logger.debug("Modified Models : " + modifiedModels);
            return modifiedModels.toArray();
        }

        /**
         * Disposes any resources
         */
        public void dispose() {
            // We don't create any resources, so we don't dispose any
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            logger.debug("oldInput = " + oldInput + ", newInput = " + newInput);
        }
    }

}
