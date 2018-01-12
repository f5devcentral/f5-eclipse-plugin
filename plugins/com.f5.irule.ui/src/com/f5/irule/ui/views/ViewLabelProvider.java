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

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.f5.irule.model.BigIPConnection;
import com.f5.irule.model.DataGroup;
import com.f5.irule.model.ModelObject;
import com.f5.irule.model.ModelParent;
import com.f5.irule.model.Rule;
import com.f5.irule.ui.Strings;

public class ViewLabelProvider extends LabelProvider {

    private static Logger logger = Logger.getLogger(LabelProvider.class);

    public static final String IMAGE_DEVICE_PNG = "/images/device.png";
    
    private static final String IMAGE_IRULE_FILE_PNG = "/images/irule-file.png";
    private static final String IMAGE_IRULE_FILE_MODIFIED_PNG = "/images/irule-file_modified.png";
    private static final String IMAGE_IRULE_FILE_NEW_PNG = "/images/irule-file_new.png";
    
    private static final String IMAGE_JAVASCRIPT_PNG = "/images/javascript.png";
    private static final String IMAGE_JAVASCRIPT_MODIFIED_PNG = "/images/javascript_modified.png";
    private static final String IMAGE_JAVASCRIPT_NEW_PNG = "/images/javascript_new.png";
    
    private static final String IMAGE_JSON_PNG = "/images/json.png";
    private static final String IMAGE_JSON_MODIFIED_PNG = "/images/json_modified.png";
    private static final String IMAGE_JSON_NEW_PNG = "/images/json_new.png";
    
    private static final String IMAGE_DATAGROUP_PNG = "/images/dataGroup.png";
    private static final String IMAGE_DATAGROUP_MODIFIED_PNG = "/images/dataGroup_modified.png";
    private static final String IMAGE_DATAGROUP_NEW_PNG = "/images/dataGroup_new.png";
    
    private static HashMap<String, Image> imageMap = new HashMap<String, Image>();

    public String getText(Object obj) {
        if (obj instanceof BigIPConnection) {
            BigIPConnection conn = (BigIPConnection) obj;
            return getBigIPConnectionText(conn);
        }
        if (obj instanceof ModelObject) {
            ModelObject modelObject = (ModelObject) obj;
            if (modelObject.isLocallyModified()) {
                String ans = "> " +  obj.toString();
                return ans;
            }
            if (modelObject.isLocallyAdded()) {
                String ans = "+ " +  obj.toString();
                return ans;
            }
        }
        return obj.toString();
    }

    private String getBigIPConnectionText(BigIPConnection connection) {
        String address = connection.getAddress();
        boolean onlineMode = connection.isOnlineMode();
        // If jobCount is greater than 0, then the plug-in is still executing jobs
        int jobCount = connection.getJobCount();
        boolean executingJobs = jobCount > 0;
        boolean connected = connection.isConnected();
        String ans;
        if (onlineMode) {            
            if (executingJobs) {
                String label = connected ? Strings.LABEL_LOADING : Strings.LABEL_DISCONNECTED;
                ans = Strings.msg(address, label);
            } else {
                ans = connected ? connection.toString() : Strings.msg(address, Strings.LABEL_DISCONNECTED);
            }
        } else {
            // The connection is in Offline mode, Add 'Offline' suffix to the label text            
            if (executingJobs) {
                ans = Strings.msg(address, Strings.LABEL_OFFLINE, Strings.LABEL_LOADING);
            } else {
                // In that case, Add 'Loading' to the label text 
                ans = Strings.msg(address, Strings.LABEL_OFFLINE);
            }    
        }
        logger.trace("ans = " + ans + " onlineMode=" + onlineMode +
            " jobCount=" + jobCount + " executingJobs=" + executingJobs + " connected=" + connected);
        return ans;
    }

    public Image getImage(Object obj) {
        return obj instanceof ModelParent ? obj instanceof BigIPConnection ?
            createImage(IMAGE_DEVICE_PNG) : // Big-IP Image
                getImage(ISharedImages.IMG_OBJ_FOLDER) : // Folder Image
                    getModelFileImage((ModelObject) obj); // Model Image
    }

    public static Image getModelFileImage(ModelObject model) {
        boolean modified = model.isLocallyModified();
        boolean locallyAdded = model.isLocallyAdded();
        if (model instanceof Rule || model.getType() == ModelObject.Type.ILX_RULE) {
            return createImage(locallyAdded ? IMAGE_IRULE_FILE_NEW_PNG :
                (modified ? IMAGE_IRULE_FILE_MODIFIED_PNG : IMAGE_IRULE_FILE_PNG));
        }
        String name = model.getName();
        if (name.endsWith(".js")) {
            return createImage(locallyAdded ? IMAGE_JAVASCRIPT_NEW_PNG :
                (modified ? IMAGE_JAVASCRIPT_MODIFIED_PNG : IMAGE_JAVASCRIPT_PNG));
        }
        if (name.endsWith(".json")) {
            return createImage(locallyAdded ? IMAGE_JSON_NEW_PNG :
                (modified ? IMAGE_JSON_MODIFIED_PNG : IMAGE_JSON_PNG));
        }
        if (model instanceof DataGroup) {
            return createImage(locallyAdded ? IMAGE_DATAGROUP_NEW_PNG :
                (modified ? IMAGE_DATAGROUP_MODIFIED_PNG : IMAGE_DATAGROUP_PNG));
        }
        return getImage(ISharedImages.IMG_OBJ_FILE);
    }
    
    private static Image getImage(String symbolicName) {
        return PlatformUI.getWorkbench().getSharedImages().getImage(symbolicName);
    }
    
    public static Image createImage(String fileName) {
        Image img = imageMap.get(fileName);
        if (img == null) {
            img = ImageDescriptor.createFromFile(IruleView.class, fileName).createImage();
            imageMap.put(fileName, img);
        }
        return img;
    }
}
