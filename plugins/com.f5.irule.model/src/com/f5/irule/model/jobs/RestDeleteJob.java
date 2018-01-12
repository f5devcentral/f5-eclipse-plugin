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
package com.f5.irule.model.jobs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import com.f5.irule.model.BigIPConnection;
import com.f5.irule.model.Ids;
import com.f5.irule.model.ModelObject;
import com.f5.irule.model.RequestCompletion;

/**
 * A {@link ConnectionJob} that executes the {@link ModelObject #iControlRestDelete} method
 */
public class RestDeleteJob extends ConnectionJob {

    private ModelObject model;

    public RestDeleteJob(ModelObject model, BigIPConnection conn, RequestCompletion completion, ISchedulingRule mutex) {
        super(Ids.REST_DELETE, conn, completion, mutex);
        this.model = model;
    }

    @Override
    protected IStatus doRestOperation(RequestCompletion jobCompletion) {
        IStatus status = model.iControlRestDelete(jobCompletion);
        return status;
    }
}