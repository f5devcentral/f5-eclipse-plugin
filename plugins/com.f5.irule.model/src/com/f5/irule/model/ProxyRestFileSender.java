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

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.f5.rest.common.CompletionHandler;
import com.f5.rest.common.RestFileSender;
import com.f5.rest.common.RestFileTransferInformation;
import com.f5.rest.common.RestHelper;
import com.f5.rest.common.RestOperation;
import com.f5.rest.common.RestReference;

/**
 * Helper that uploads files to the Big-ip with raw data from the local file system.<br>
 * Original code: {@link RestFileSender}
 */
public class ProxyRestFileSender {

    private static Logger logger = Logger.getLogger(ProxyRestFileSender.class);

    private HttpRequestBase request;
    private HttpClient proxyClient;
    private String remoteHost;
    private String proxyHost;
    private int proxyPort;

    private RestFileTransferInformation state = new RestFileTransferInformation();
    private AtomicInteger startCount = new AtomicInteger();
    private CompletionHandler<RestFileTransferInformation> progressCompletion;
    private RestFrameworkCompletionHandler finalCompletion;

    ProxyRestFileSender(String localFilePath, HttpRequest request, HttpClient proxyClient,
            String remoteHost, String proxyHost, int proxyPort,
            RestFrameworkCompletionHandler finalCompletion,
            CompletionHandler<RestFileTransferInformation> progressCompletion) {
        this.request = (HttpRequestBase) request;
        this.proxyClient = proxyClient;
        this.remoteHost = remoteHost;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.finalCompletion = finalCompletion;
        this.progressCompletion = progressCompletion;
        
        state.localFilePath = localFilePath;
        state.targetReference = new RestReference(this.request.getURI());
        state.lastUpdateMicros = RestHelper.getNowMicrosUtc();
    }

    /**
     * Schedule a {@link FileChunkTransferJob} to send the request to remote server.<br>
     * The request body is set with data from local file chunk.<br>
     * While there are still remaining bytes to send from the local file,<br>
     * it would keep scheduling jobs to send the rest of remaining bytes. 
     */
    void start() {
        if (this.startCount.incrementAndGet() > 1) {
            throw new IllegalStateException("Already started");
        }
        logger.trace("Schedule File Chunk Transfer");
        Job job = new FileChunkTransferJob(this);
        job.schedule();
    }

    /**
     * A {@link Job} that clones the sender request.<br>
     * Set its body with data from the local file.<br>
     * Sends the request and processes the response.
     */
    private static class FileChunkTransferJob extends Job {
        private ProxyRestFileSender sender;

        FileChunkTransferJob(ProxyRestFileSender sender) {
            super(Ids.SCHEDULE_FILE_CHUNK_TRANSFER);
            this.sender = sender;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            try {
                sender.transferFileChunk();
                return Status.OK_STATUS;
            } catch (Exception ex) {
                ex.printStackTrace();
                Status status = new Status(IStatus.ERROR, Ids.PLUGIN, RestHelper.throwableStackToString(ex), ex);
                return status;
            }
        }
        
    }
    
    /**
     * 1. Check and initialize this sender status.<br>
     * If error occurred then return and do not send chunk.<br>
     * 2. Clone Request<br>
     * 3. Read bytes from local file and set them on the clonedRequest body<br>
     * 4. Send Request<br>
     * 5. Get Response<br>
     * 6. Process Response<br>
     * 7. If the sender state remainingByteCount is bigger than 0
     * then schedule another transferFileChunk Job.
     */
    private void transferFileChunk() {
        logger.trace("Transfer File Chunk");
        if (!checkStatus()) {
            return;
        }

        // Clone Request
        HttpRequestBase clonedRequest = cloneRequest();
        if (clonedRequest == null) {
            return;
        }
                
        // Read bytes from local file and set them on the clonedRequest body
        final boolean isComplete;
        try {
            isComplete = readFileChunk(state, clonedRequest);
        } catch (Exception e) {
            fail(e);
            return;
        }
        
        // Send Request, Get Response
        try {
            HttpResponse response = ProxyUtil.executeRequest(proxyClient, clonedRequest, remoteHost, proxyHost, proxyPort);
            String responseBody = ProxyUtil.getResponse(response);
            logger.debug(clonedRequest + " Response:\n" + responseBody);
            // Process Response
            if (responseBody != null) {
                if (isComplete) {
                    state.localFile.close();
                    if (this.finalCompletion != null) {
                        this.finalCompletion.completed(this.state);
                    } else if (this.progressCompletion != null) {
                        this.progressCompletion.completed(this.state);
                    }
                } else {
                    // resume file transfer
                    logger.debug("Schedule File Chunk Transfer");
                    Job job = new FileChunkTransferJob(this);
                    job.schedule();
                }
            }
        } catch (IOException ex) {
            fail(ex);
        }
    }

    /**
     * Check the {@link RestFileTransferInformation} state of this {@link ProxyRestFileSender}.<br>
     * Validate that it has no error and that its local file exists.<br>
     * If its the first chunk, then initialize the state. Otherwise call the progress completion.<br>
     * return true if state and initialization process had no errors. False Otherwise
     */
    private boolean checkStatus() {

        if (state.error != null) {
            return false;
        }
        try {
            File file = new File(state.localFilePath);
            checkFileExists(state, file);
            if (state.localFile == null) {
                doInitialize(state, file);
            }
            else if (file.lastModified() != state.lastModified) {
                // File is modified, close and reopen the file
                state.localFile.close();
                doInitialize(state, file);
            }
            else{
                // If state wasn't initialized then only a chunk of the message was sent.
                // Invole the progress completion.
                invokeProgressCompletion();                
            }
        } catch (Exception e) {
            fail(e);
            return false;
        }

        return true;
    }

    private HttpRequestBase cloneRequest() {
        HttpRequestBase clonedRequest;
        try {
            clonedRequest = (HttpRequestBase) request.clone();
        } catch (CloneNotSupportedException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
            return null;
        }
        Header contentRangeHeader = request.getFirstHeader("Content-Range");
        if (contentRangeHeader != null) {
            String contentRange = contentRangeHeader.getValue();
            buildContentRangeHeaderForNextChunk(state, clonedRequest, contentRange);
        }
        return clonedRequest;
    }

    /**
     * Read bytes from local file and set them on the {@link HttpRequest} body as Entity:<br>
     * Produce a chunk info according to the sender state,<br>
     * create a bytes buffer with data from the local file according to the chunk info<br>
     * and set the request Content-Range and Content-Type headers and its body with the file buffer info.
     * Original code: {@link RestFileSender #transferFileChunk} 
     */
    private static boolean readFileChunk(RestFileTransferInformation state, HttpRequest request) throws Exception {
        
        String contentRange = request.getFirstHeader("Content-Range").getValue();
        ChunkInfo chunkInfo = getChunkInfo(state, contentRange);
        // Size requested is probably more than rest javad can handle with the constraint on the memory.
        // Enforce requests from user to use smaller chunks
        if (chunkInfo.size > RestFileSender.MAX_CHUNK_SIZE_BYTES) {
            String message = String.format(ERROR_INVALID_REQUEST_SIZE_FMT, chunkInfo.size, RestFileSender.MAX_CHUNK_SIZE_BYTES);
            throw new IllegalArgumentException(message);
        }
        state.localFile.seek(chunkInfo.pos);
        byte[] buffer = new byte[chunkInfo.size];
        readBytes(state, chunkInfo.size, buffer);
        boolean isComplete = checkIfComplete(state, contentRange, chunkInfo.pos, chunkInfo.size, chunkInfo.rangeForChunkOffset);
        // First implementation does no overlapping. Next version will use batching and overlap
        // N chunks at the same time for increased throughput
        contentRange = RestHelper.buildContentRangeHeaderValue(chunkInfo.pos, chunkInfo.size, state.totalByteCount);
        request.setHeader("Content-Range", contentRange);
        String contentTypeValue = getContentTypeValue(state.localFilePath);
        request.setHeader("Content-Type", contentTypeValue);
        setBody(request, buffer, contentTypeValue);
        return isComplete;
    }
    private static final String ERROR_INVALID_REQUEST_SIZE_FMT =
        "Requested file size %s greater than maximum chunk size %s allowed";

    /**
     * Produce a chunk info according to the request content range and this sender state.<br>
     * The chunk info holds the position, size and range of the next File chunk transfer
     */
    private static ChunkInfo getChunkInfo(RestFileTransferInformation state, String contentRange) {
        ChunkInfo chunkInfo = new ChunkInfo();
        if (contentRange == null) {
            // If Content-Range is not provided in the request,
            // attempt to return first chunk not greater than MAX_CHUNK_SIZE_BYTES.
            // If actual file size is less, then the whole file will be returned in response.
            chunkInfo.pos = 0;
            chunkInfo.size = state.totalByteCount <= RestFileSender.MAX_CHUNK_SIZE_BYTES ?
                (int) state.totalByteCount : RestFileSender.MAX_CHUNK_SIZE_BYTES;
        } else {
            // specific chunk is being requested through content-range header
            chunkInfo.pos = RestHelper.parseContentRangeHeaderStartValue(contentRange);
            long chunkByteCount = RestHelper.parseContentRangeHeaderChunkByteCount(contentRange);
            chunkInfo.rangeForChunkOffset = state.usedChunks.get(chunkInfo.pos);
            chunkInfo.size = chunkInfo.rangeForChunkOffset == null ?
                // If its a new chunk requested, Client would have computed chunk size and requested.
                // remainingByteCount should be used only to decide when to close file handle
                (int) Math.min(chunkByteCount, state.totalByteCount) :
                // Otherwise, duplicate chunk is being requested. Adjust the size based on what is sent last time.
                chunkInfo.rangeForChunkOffset.intValue();
        }
        return chunkInfo;
    }
    
    private static class ChunkInfo {
        long pos;
        int size;
        Long rangeForChunkOffset = null;
    }

    /**
     * Return True if the sender state remainingByteCount is 0. False otherwise
     */
    private static boolean checkIfComplete(RestFileTransferInformation state,
            String contentRange, long pos, int size, Long rangeForChunkOffset) {
        if (contentRange == null) {
            return true;
        }
        if (rangeForChunkOffset != null) {
            return false;
        }
        // its a new chunk requested, cache the information
        state.usedChunks.put(pos, (long) size);
        // decrement remaining count only when this is used to the send file chunks using postTemplate
        state.remainingByteCount -= size;
        boolean isComplete = state.remainingByteCount == 0;
        return isComplete;
    }

    /**
     * Read bytes from local file to buffer.
     */
    private static void readBytes(RestFileTransferInformation state, int size, byte[] buffer) throws IOException {
        int bytesRead = 0;
        while (bytesRead < size) {
            int readCount;
            readCount = state.localFile.read(buffer, bytesRead, size - bytesRead);
            if (readCount == -1) {
                throw new EOFException("Attempt to read past end of file:" + state.localFilePath);// we should never try to read past the end of file
            }
            bytesRead += readCount;
        }
    }


    /**
     * Set the buffer as the body entity of the request
     */
    private static void setBody(HttpRequest request, byte[] buffer, String contentTypeValue) {
        if (RestHelper.contentTypeUsesBinaryBody(contentTypeValue)) {
            // Set Binary Body
            HttpEntity entity = new ByteArrayEntity(buffer);
            ((HttpEntityEnclosingRequest) request).setEntity(entity);
            return;
        }
        try {
            HttpEntity entity = new StringEntity(new String(buffer, "UTF8"));
            ((HttpEntityEnclosingRequest) request).setEntity(entity);
        } catch (UnsupportedEncodingException e) {
            // we cannot convert non-unicode byte array into unicode, falling back to default charset and log a warning
            try {
                HttpEntity entity = new StringEntity(new String(buffer));
                ((HttpEntityEnclosingRequest) request).setEntity(entity);
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }
        }
    }

    private static String getContentTypeValue(String filePath) {
        String contentTypeValue;
        if (jsFileNamePattern.matcher(filePath).matches()) {
            contentTypeValue = RestOperation.MIME_TYPE_APPLICATION_JAVASCRIPT;
        } else if (htmlFileNamePattern.matcher(filePath).matches()) {
            contentTypeValue = RestOperation.MIME_TYPE_TEXT_HTML;
        } else if (cssFileNamePattern.matcher(filePath).matches()) {
            contentTypeValue = RestOperation.MIME_TYPE_TEXT_CSS;
        } else if (bmpFileNamePattern.matcher(filePath).matches()) {
            contentTypeValue = RestOperation.MIME_TYPE_IMAGE_BMP;
        } else if (gifFilePattern.matcher(filePath).matches()) {
            contentTypeValue = RestOperation.MIME_TYPE_IMAGE_GIF;
        } else if (jpegFileNamePattern.matcher(filePath).matches()) {
            contentTypeValue = RestOperation.MIME_TYPE_IMAGE_JPEG;
        } else if (pngFileNamePattern.matcher(filePath).matches()) {
            contentTypeValue = RestOperation.MIME_TYPE_IMAGE_PNG;
        } else if (svgFileNamePattern.matcher(filePath).matches()) {
            contentTypeValue = RestOperation.MIME_TYPE_IMAGE_SVG;
        } else if (tiffFileNamePattern.matcher(filePath).matches()) {
            contentTypeValue = RestOperation.MIME_TYPE_IMAGE_TIFF;
        } else {
            contentTypeValue = RestOperation.MIME_TYPE_APPLICATION_OCTET_STREAM;
        }
        return contentTypeValue;
    }
    private static final Pattern jsFileNamePattern = Pattern.compile(".*\\.js", Pattern.CASE_INSENSITIVE);
    private static final Pattern htmlFileNamePattern = Pattern.compile(".*\\.html?", Pattern.CASE_INSENSITIVE);
    private static final Pattern cssFileNamePattern = Pattern.compile(".*\\.css", Pattern.CASE_INSENSITIVE);
    private static final Pattern bmpFileNamePattern = Pattern.compile(".*\\.bmp", Pattern.CASE_INSENSITIVE);
    private static final Pattern gifFilePattern = Pattern.compile(".*\\.gif", Pattern.CASE_INSENSITIVE);
    private static final Pattern jpegFileNamePattern = Pattern.compile(".*\\.jpe?g?", Pattern.CASE_INSENSITIVE);
    private static final Pattern pngFileNamePattern = Pattern.compile(".*\\.png", Pattern.CASE_INSENSITIVE);
    private static final Pattern svgFileNamePattern = Pattern.compile(".*\\.svg", Pattern.CASE_INSENSITIVE);
    private static final Pattern tiffFileNamePattern = Pattern.compile(".*\\.tiff?", Pattern.CASE_INSENSITIVE);

    
    private static void buildContentRangeHeaderForNextChunk(RestFileTransferInformation state,
            HttpRequest op, String contentRange) {
        long chunkByteCount = RestHelper.parseContentRangeHeaderChunkByteCount(contentRange); 
        int size = (int) Math.min(chunkByteCount, state.remainingByteCount);
        long pos = state.totalByteCount - state.remainingByteCount;
        contentRange = RestHelper.isContentRangeHeaderWithUnknownSize(contentRange) ?
            RestHelper.buildContentRangeHeaderValueWithUnknownSize(pos, size) :
            RestHelper.buildContentRangeHeaderValue(pos, size, state.totalByteCount);
        op.setHeader("Content-Range", contentRange);
    }

    private static void checkFileExists(RestFileTransferInformation state, File file) throws IOException, FileNotFoundException {
        boolean fileNotExist = !file.exists() || !file.isFile();
        if (fileNotExist) {
            if (state.localFile != null) {
                // File no longer exists, so close the file handle
                state.localFile.close();
                state.localFile = null;                
            }
            throw new FileNotFoundException(
                "File does not exist or file path is not a file: " + state.localFilePath);
        }
    }

    private void fail(Exception error) {
        state.error = error;
        if ((error instanceof CancellationException)) {
            logger.info("Cancelled, file:" + state.localFilePath);
        } else {
            logger.error(RestHelper.throwableStackToString(state.error));
        }
        if (this.finalCompletion != null) {
            this.finalCompletion.failed(error, state);
        } else if (this.progressCompletion != null) {
            this.progressCompletion.failed(error, state);
        }
    }

    /**
     * If progress completion exist then call its completed() method with a cloned status
     */
    private void invokeProgressCompletion() {
        if (this.progressCompletion != null) {
            if (state.error != null) {
                logger.warn("State Error: ", state.error);
            }
            else{
                try {
                    RestFileTransferInformation transferStatus = (RestFileTransferInformation) RestHelper.clone(state, state.getClass());
                    this.progressCompletion.completed(transferStatus);                    
                } catch (Throwable ex) {
                    logger.warn("Caught unexpected exception", ex);
                }
            }
        }
    }

    /**
     * Set the state lastModified to the {@link File #lastModified()}<br>
     * Initialize the state localFile and usedChunks map.<br>
     * Set the state remainingByteCount and totalByteCount to the file length 
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void doInitialize(RestFileTransferInformation state, File file) throws IOException {
        state.lastModified = file.lastModified();
        state.localFile = new RandomAccessFile(state.localFilePath, "r");
        state.usedChunks = new ConcurrentHashMap();
        state.remainingByteCount = state.totalByteCount = state.localFile.length();
    }

}
