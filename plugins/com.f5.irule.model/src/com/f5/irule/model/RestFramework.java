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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.ui.statushandlers.StatusManager;

import com.f5.irule.model.jobs.ConnectionJob;
import com.f5.irule.model.jobs.SendRequestJob;
import com.f5.rest.common.CompletionHandler;
import com.f5.rest.common.RestFileSender;
import com.f5.rest.common.RestFileTransferInformation;
import com.f5.rest.common.RestHelper;
import com.f5.rest.common.RestOperation;
import com.f5.rest.common.RestReference;
import com.f5.rest.common.RestOperation.RestMethod;
import com.f5.rest.common.RestRequestCompletion;
import com.f5.rest.common.RestRequestSender;

/**
 * A bridge to the com.f5.rest package.<br>
 * The models in the com.f5.irule.model plugin use it to get their content from the Big-IP<br>
 * and to write content to the Big-IP.
 */
public class RestFramework {

    private static Logger logger = Logger.getLogger(RestFramework.class);
    
    private static final String FILE_TRANSFER_UPLOADS = "mgmt/shared/file-transfer/uploads";

    static final String IAPP_FILE_MANAGEMENT = "mgmt/shared/iapp/file-management";
    public static final String IAPP_DIRECTORY_MANAGEMENT_RECURSIVE = "mgmt/shared/iapp/directory-management-recursive";
    public static final String IAPP_DIRECTORY_MANAGEMENT = "mgmt/shared/iapp/directory-management";
    public static final String MGMT_TM_SYS_SERVICE = "mgmt/tm/sys/service";

    private static RestFramework instance = new RestFramework();

    public static RestFramework getInstance() {
        return instance;
    }
    
    /**
     * Mutex is required so that only one write occurs at a time.
     * TODO Once iControlRest is changed to allow simultaneous writes, this could be removed
     */
    private final Semaphore writeMutex = new Semaphore(1);
    private static final int LOCK_ACQUIRE_TIMEOUT = 15;  // seconds 

    void writeILXResource(BigIPConnection conn, IPath fullPath, IPath location, RequestCompletion externalCompletion) {
        String localFilePath = location.toString();
        boolean pathContainsIp = fullPath.segment(0).equals(conn.getAddress());
        String partition = fullPath.segment(pathContainsIp ? 1 : 0);
        String workspace = fullPath.segments()[pathContainsIp ? 3 : 2];  // TODO LAME
        String uploadFileCopy = fullPath.removeFirstSegments(pathContainsIp ? 4 : 3).toString(); // TODO LAME
        RequestCompletion finalCompletion = new InstallUploadedFileCompletion(uploadFileCopy, null, null, externalCompletion);
        writeILXResource(conn, localFilePath, workspace, partition, uploadFileCopy, finalCompletion);
    }

    /**
     * Synchronously upload the ILX file to the Big-IP.<br>
     * Acquire the mutex lock and then use the file-transfer/uploads REST api to upload the file.
     */
    public void writeILXResource(BigIPConnection conn, String localFilePath, String workspace, String partition,
            String uploadFileCopy, RequestCompletion finalCompletion) {
        RestURI restUri = conn.getURI(FILE_TRANSFER_UPLOADS);
        restUri.appendSlashFirst("ilx_workspace_file");
        String targetUri = restUri.toString();
        WriteILXResourceCompletionHandler completionHandler = new WriteILXResourceCompletionHandler(conn,
            partition, workspace, uploadFileCopy, finalCompletion, writeMutex);
        syncUploadResource(conn, targetUri, localFilePath, completionHandler, writeMutex);
    }

    /**
     * Synchronously upload the iAppsLx resource to the Big-IP.<br>
     * Use the /mgmt/shared/iapp/file-management upload REST api to upload the file.
     */
    public void writeIappLXResource(BigIPConnection conn, String localFilePath,
            IPath remotePath, RequestCompletion finalCompletion) throws URISyntaxException {
        RestURI restUri = conn.getURI(IAPP_FILE_MANAGEMENT);
        restUri.appendSlashFirst(remotePath.toString());
        RestFrameworkCompletionHandler completionHandler =
            new RestFrameworkCompletionHandler(finalCompletion, writeMutex);
        syncUploadResource(conn, restUri.toString(), localFilePath, completionHandler, writeMutex);
    }

    public static void sendRequestJob(Connection conn, RestMethod method, String uri,
            String contentType, String jsonBody, RequestCompletion completion, ISchedulingRule mutex) {
        ConnectionJob job = new SendRequestJob((BigIPConnection) conn, completion, method, uri, contentType, jsonBody, mutex);
        job.schedule();
    }

    /**
     * Check if the connection Ip is reachable and send the REST request to the Big-IP.<br>
     * If the connection uses proxy then use the ProxyUtil.sendRequest() to send the request.<br>
     * Otherwise use the RuleProvider.restSendRequest() method to send the request.
     */
    public static IStatus sendRequest(BigIPConnection conn, RestMethod method, String uri,
            String contentType, String jsonBody, RequestCompletion completion) {
        
        IStatus status = ModelUtils.isIpReachable(conn.getIp(), 5000);
        if (!status.isOK()) {
            return status;
        }
        status = sendRequestToBigIP(conn, method, uri, jsonBody, completion, 1000);
        return status;
    }

    /**
     * If the {@link BigIPConnection} uses proxy, then use the ProxyUtil.sendRequest() to send the request.<br>
     * Otherwise use the f5.rest.jar {@link RestRequestSender} to send a REST request to the Big-IP.
     */
    static IStatus sendRequestToBigIP(BigIPConnection conn, RestMethod method, String uri, String body,
            RequestCompletion completion, int sleepTime) {
        IStatus status;
        try {
            if (conn.isUseProxy()) {
                ProxyUtil.sendRequest(conn, method, uri, body, completion);
            } else {
                restSendRequest(conn, method, uri, body, completion);
            }
            Thread.sleep(sleepTime);
            return Status.OK_STATUS;
        } catch (Throwable ex) {
            status = new Status(IStatus.ERROR, Ids.PLUGIN, RestHelper.throwableStackToString(ex), ex);
            StatusManager.getManager().handle(status, StatusManager.LOG);
            return status;
        }
    }

    /**
     * Use the f5.rest.jar {@link RestRequestSender} to send a REST request to the Big-IP.<br>
     * A {@link RestRequestCompletionBridge} is created that wraps the method {@link RequestCompletion}<br>
     * So when an answer is received and the {@link RestRequestCompletion #completed(RestOperation)} is called<br>
     * it would delegate the response handling to the {@link RequestCompletion} method.
     */
    private static void restSendRequest(Connection conn, RestMethod method,
            String uri, String jsonBody, RequestCompletion completion) throws URISyntaxException {
        String contentType = "application/json";
        RestRequestCompletion restRequestCompletion = new RestRequestCompletionBridge(completion);
        RestOperation op = createRestOperation(method,
            uri, conn, contentType, null, jsonBody, restRequestCompletion);
        String message = method + " " + uri;
        if (jsonBody != null) {
            message += " Body: " + jsonBody;
        }
        message += " Completion: " + completion;
        logger.debug(message);
        RestRequestSender.sendRequest(op);
    }

    /**
     * Synchronously upload the resource to the Big-IP.<br>
     * If the {@link BigIPConnection} uses proxy, then use the ProxyUtil.uploadResource() to to upload the file.<br>
     * Otherwise use the f5.rest.jar RestFileSender file-transfer/uploads REST api to upload the file.
     */
    private static void syncUploadResource(Connection conn, String targetUri, String localFilePath,
            RestFrameworkCompletionHandler completionHandler, Semaphore mutex) {
        IStatus status = ModelUtils.isIpReachable(conn.getIp(), 5000);
        if (!status.isOK()) {
            RestFileTransferInformation info = createRestFileTransferInformation(localFilePath, targetUri);
            completionHandler.failed(null, info);
            return;
        }
        boolean acquired = tryAcquire(mutex);
        if (acquired) {
            try {
                if (conn.isUseProxy()) {
                    ProxyUtil.uploadResource(conn, targetUri, localFilePath, completionHandler);
                } else {
                    uploadResource(conn, targetUri, localFilePath, completionHandler);
                }
            } catch (Throwable ex) {
                handleError(Messages.FILE_UPLOAD_FAILED + ": " + RestHelper.throwableStackToString(ex), ex);
                mutex.release();
            }
        } else {
            handleError(Messages.TIMEOUT_TRYING_TO_UPLOAD + " : " + localFilePath, null);
            RestFileTransferInformation info = createRestFileTransferInformation(localFilePath, targetUri);
            completionHandler.failed(null, info);
        }
    }

    private static RestFileTransferInformation createRestFileTransferInformation(String localFilePath, String targetUri) {
        RestFileTransferInformation info = new RestFileTransferInformation();
        info.localFilePath = localFilePath;
        try {
            URI uri = new URI(targetUri);
            info.targetReference = new RestReference(uri);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        info.lastUpdateMicros = RestHelper.getNowMicrosUtc();
        return info;
    }

    private static void handleError(String message, Throwable ex) {
        Status status = ex == null ? 
            new Status(IStatus.ERROR, Ids.PLUGIN, message):
            new Status(IStatus.ERROR, Ids.PLUGIN, message, ex);
        StatusManager.getManager().handle(status, StatusManager.LOG);
    }

    /**
     * Each thread will try to acquire the mutex lock.<br>
     * If successful, upload and install the file.<br>
     * The lock is released by the completed/failed methods of the completion.<br>
     * If the lock is not available, wait until timeout or lock is release by owning thread
     */
    private static boolean tryAcquire(Semaphore mutex) {
        boolean acquired = false;
        try {
            acquired = mutex.tryAcquire(1, LOCK_ACQUIRE_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return acquired;
    }

    /**
     * Use the f5.rest.jar {@link RestFileSender} file-transfer/uploads REST api<br>
     * to upload a file by sending a POST REST request to the Big-IP.<br>
     * When the upload finishes, the {@link CompletionHandler #completed(Object)} method is called.
     * and delegates the response handling to the {@link RequestCompletion} completed method.
     */
    private static void uploadResource(Connection connection, String targetUri, String localFilePath,
            CompletionHandler<RestFileTransferInformation> completionHandler)
            throws URISyntaxException {
    
        String contentRange = getContentRange();
        RestOperation op = createRestOperation(RestMethod.POST, targetUri,
            connection, "application/octet-stream", contentRange, null, null);
        RestFileSender sender = RestFileSender.create(localFilePath, op);
        // .setChunkByteCount(Math.min(1024 * 1024, fileSize));
        CompletionHandler<RestFileTransferInformation> progressCompletion = new ProgressCompletionHandler(localFilePath);
        sender.setProgressCompletion(progressCompletion);
        sender.setFinalCompletion(completionHandler);
        logger.debug("Send " + targetUri + " Completion: " + completionHandler);
        sender.start();
    }

    static String getContentRange() {
        int chunkSize = 1024 * 1024; // RestFileSender.DEFAULT_CHUNK_SIZE_BYTES
        String contentRange = RestHelper.buildContentRangeHeaderValue(0, chunkSize, 1024);
        return contentRange;
    }

    public static String inputStreamToString(InputStream stream) throws IOException {
        Scanner scanner = new Scanner(stream);
        scanner.useDelimiter("\\A");
        String ans = scanner.hasNext() ? scanner.next() : "";
        scanner.close();
        return ans;
    }

    private static RestOperation createRestOperation(RestMethod method, String uri,
            Connection conn, String contentType, String contentRange, String body,
            RestRequestCompletion completion) throws URISyntaxException {

        String user = conn.getUser();
        String pwd = conn.getPassword();
        
        RestOperation op = RestOperation.createSigned();
        op.setMethod(method);
        op.setUri(new URI(uri));
        op.setBasicAuthorization(user, pwd);
        if (contentType != null) {
            op.setContentType(contentType);
        }
        if (contentRange != null) {
            op.setContentRange(contentRange);
        }
        if (body != null) {
            op.setBody(body);
        }
        if (completion != null) {
            op.setCompletion(completion);
        }
        return op;
    }
    
    public static class ProgressCompletionHandler extends CompletionHandler<RestFileTransferInformation>{

        private String fileName;

        ProgressCompletionHandler(String fileName) {
            this.fileName = fileName;
        }

        @Override
        public void completed(RestFileTransferInformation operation) {
        }

        @Override
        public void failed(Exception ex, RestFileTransferInformation operation) {
            IStatus status = new Status(IStatus.ERROR, Ids.PLUGIN,
                Messages.ILX_FILE_UPLOAD_ERROR + " : " + fileName, ex);
            StatusManager.getManager().handle(status, StatusManager.LOG);
        }
        
    }
}
