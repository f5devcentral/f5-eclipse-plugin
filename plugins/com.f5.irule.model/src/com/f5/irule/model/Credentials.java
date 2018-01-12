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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;

public class Credentials {

    private String user;
    private String password;
    private boolean secureStore;

    public Credentials (String user, String password, boolean secureStore) {
        set(user, password, secureStore);
    }
    
    public Credentials (IProject project) {
        load(project);
    }
    
    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public boolean isSecureStore() {
        return secureStore;
    }
    
    /*
     * Set but don't store on filesystem (use store method for that)
     */
    public void set(String user, String password, boolean secureStore) {
        this.user = user;
        this.password = password;
        this.secureStore = secureStore;
    }
    
    /*
     * Load persisted creds from filesystem
     */
    public Credentials load(IProject project) {
        try {
            String secure = project.getPersistentProperty(new QualifiedName(
                    Ids.PLUGIN, Ids.SECURE_CREDENTIALS));
            if (secure != null && secure.equals(Ids.TRUE)) {
                ISecurePreferences root = SecurePreferencesFactory.getDefault();
                ISecurePreferences node = root.node(Ids.PLUGIN + "/" + project.getName());
                user = node.get(Ids.USER, null);
                password = node.get(Ids.PASSWORD, null);
                secureStore = true;
            } else {
                user = project.getPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.USER));
                password = project.getPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.PASSWORD));
                secureStore = false;
            }
        } catch (StorageException e) {
            e.printStackTrace();
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return this;
    }
    
    /*
     * Store set credentials on filesystem
     */
    public boolean store(IProject project) throws CoreException {
        // If user elected to store credentials in secure store, do that
        // Otherwise, store as a persistent property
        // Either way, store the user preference as a persistent property
        if (secureStore) {            
            ISecurePreferences root = SecurePreferencesFactory.getDefault();
            ISecurePreferences node = root.node(Ids.PLUGIN + "/" + project.getName());
            try {
                node.put(Ids.USER, user, false);
                node.put(Ids.PASSWORD, password, true);
            } catch (StorageException e) {
                e.printStackTrace();
                return false;
            }
            // clear user/password from standard project props
            project.setPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.USER), null);
            project.setPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.PASSWORD), null);
            project.setPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.SECURE_CREDENTIALS), Ids.TRUE);
        } else {
            project.setPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.USER), user);
            project.setPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.PASSWORD), password);
            project.setPersistentProperty(new QualifiedName(Ids.PLUGIN, Ids.SECURE_CREDENTIALS), Ids.FALSE);
        }
        return true;    
    }
}
