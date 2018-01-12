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
package com.f5.irule.model;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/*
 * Singleton Root object
 */
public class ModelRoot extends ModelParent {

    private static ModelRoot instance = new ModelRoot("root");

    private ModelRoot(String name) {
        super(name, null, null, ModelObject.Type.ROOT, new Path(""));
    }

    public static ModelRoot getInstance() {
        return instance;
    }

    /*
     * Finds the Connection associated with the specified path.  If not found, return null
     */
    public Connection findConnection(IPath path) {
        if (!hasChildren()) {
            return null;
        }
        String connectionName = path.segment(0);
        ModelObject[] children = getChildren();
        for (ModelObject child : children) {
            if (child.getType() == ModelObject.Type.CONNECTION) {
                if (child.getName().equals(connectionName)) {
                    return (Connection) child;
                }
            }
        }
        return null;
    }
}
