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

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ProtocolException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.f5.irule.model.RestFramework.ProgressCompletionHandler;
import com.f5.rest.common.CompletionHandler;
import com.f5.rest.common.RestFileTransferInformation;
import com.f5.rest.common.RestOperation.RestMethod;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

@SuppressWarnings({ "restriction", "deprecation" })
public class ProxyUtil {

    private static Logger logger = Logger.getLogger(ProxyUtil.class);

    /**
     * Create an {@link HttpRequest} corresponding to the {@link RestMethod} argument.<br>
     * Set the request {@link RequestConfig} Proxy with the proxy address.<br>
     * Create a Proxy {@link HttpClient}, use it to open an SSL https tunnel<br>
     * and execute the request to the remote host.<br>
     * Read the response body of the received {@link HttpResponse} and<br>
     * execute the {@link RequestCompletion #completed(String, String, JsonObject)} method.<br>
     * In case of an error call the {@link RequestCompletion #failed(Exception, String, String, String)} method instead.<br>
     * Close the proxy client when finish
     */
    public static void sendRequest(Connection conn, RestMethod method, String uri, String jsonBody, RequestCompletion completion) {

        HttpRequestBase request = (HttpRequestBase) createRequestSafe(method, uri, conn, "application/json", null, jsonBody);
        if (request == null) {
            return;
        }
        HttpResponse response = null;
        HttpClient proxyclient = null;
        Exception sendException = null;
        try {
            String proxyHost = conn.getProxyIp();
            int proxyPort = conn.getProxyPort();
            String proxyUser = conn.getProxyUser();
            String proxyPassword = conn.getProxyPassword();
            proxyclient = createProxyClient(proxyHost, proxyPort, proxyUser, proxyPassword);
            response = executeRequest(proxyclient, request, conn.getAddress(), proxyHost, proxyPort);
        } catch (Exception ex) {
            logger.warn("Failed " + method + " uri", ex);
            sendException = ex;
        }
        doCompletion(response, completion, method.name(), uri, sendException);
        if (proxyclient != null && proxyclient instanceof Closeable) {
            try {
                ((Closeable) proxyclient).close();
            } catch (IOException ex) {
                logger.warn("Failed to close " + proxyclient, ex);
            }
        }
    }

    /**
     * Check the response has no errors and call the {@link RequestCompletion #completed(String, String, JsonObject)} method.<br>
     * In case of an error call the {@link RequestCompletion #failed(Exception, String, String, String)} method instead.
     */
    private static void doCompletion(HttpResponse response, RequestCompletion completion, String methodName, String uri, Exception sendException) {
        if (completion == null) {
            return;
        }
        if (sendException != null) {
            completion.failed(sendException, methodName, uri, null);           
            return;                
        }
        if (response == null) {
            completion.failed(null, methodName, uri, null);
            return;
        }
        
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        if (statusCode != SUCCESS_CODE) {
            // Failure http response
            String reasonPhrase = statusLine.getReasonPhrase();
            Exception ex = new ProtocolException("Error " + statusCode + ": " + reasonPhrase);
            completion.failed(ex, methodName, uri, null);
            return;
        }

        String responseBody = getResponse(response);
        if (responseBody == null) {
            completion.failed(null, methodName, uri, null);
            return;
        }
        logger.trace(methodName + " " + uri + " Response:\n" + responseBody);
        try {
            RestRequestCompletionBridge.doCompleted(completion, methodName, uri, responseBody);
        }
        catch (JsonSyntaxException mException){
            // Not Json, Check for connection error identification in the response
            handleJsonSyntaxException(mException, methodName, uri, responseBody, completion);
        }
        catch (Exception ex) {
            completion.failed(ex, methodName, uri, null);                    
        }
    }
    private static final int SUCCESS_CODE = 200;

    /**
     * Check the response in order to identify the type of error<br>
     * and call the {@link RequestCompletion #failed(Exception, String, String, String)} method. 
     */
    private static void handleJsonSyntaxException(JsonSyntaxException mException, String method, String uri,
            String responseBody, RequestCompletion completion) {
        String[] lines = responseBody.split("\n");
        String line5 = lines.length > 5 ? lines[5] : null;
        Exception failedExeption = line5 != null && line5.equals(AUTHENTICATION_REQUIRED_TITLE) ?
            new ProtocolException(Messages.AUTHENTICATION_FAILURE) : mException;
        completion.failed(failedExeption, method, uri, null);
    }
    private static final String AUTHENTICATION_REQUIRED_TITLE = "<title>Authentication required!</title>";

    /**
     * Upload a local file to the Big-IP<br>
     * 1. Create a POST {@link HttpRequest} with 1024 * 1024 content-range.<br>
     * 2. Create a Proxy {@link HttpClient} to be used to open an SSL https tunnel to remote server.<br>
     * 3. Use the {@link ProxyRestFileSender} to upload the local file to the Big-ip.
     */
    public static void uploadResource(Connection conn, String uri, String localFilePath,
            RestFrameworkCompletionHandler completionHandler) throws GeneralSecurityException {
        logger.debug("Upload " + localFilePath + " to " + uri);
        String contentRange = RestFramework.getContentRange();
        HttpRequest request = createRequestSafe(RestMethod.POST, uri, conn, "application/octet-stream", contentRange, null);
        if (request == null) {
            return;
        }
        String remoteHost = conn.getAddress();
        String proxyHost = conn.getProxyIp();
        int proxyPort = conn.getProxyPort();
        String proxyUser = conn.getProxyUser();
        String proxyPassword = conn.getProxyPassword();
        HttpClient proxyClient = createProxyClient(proxyHost, proxyPort, proxyUser, proxyPassword);
        CompletionHandler<RestFileTransferInformation> progressCompletion = new ProgressCompletionHandler(localFilePath);
        ProxyRestFileSender sender = new ProxyRestFileSender(localFilePath, request, proxyClient,
            remoteHost, proxyHost, proxyPort, completionHandler, progressCompletion);
        logger.debug("Send " + uri + " Completion: " + completionHandler);
        sender.start();
    }

    /**
     * Create an {@link HttpRequest} corresponding to the {@link RestMethod} argument.<br>
     * Add the Authorization, Content-Range and Content-Type headers and the body if exists.<br>
     * The Authorization header is composed of the connection user and password.
     */
    private static HttpRequest createRequestSafe(RestMethod method, String uri, Connection conn, String contentType,
            String contentRange, String jsonBody) {
        String user = conn.getUser();
        String password = conn.getPassword();
        HttpRequest request = null;
        try {
            request = createRequest(method, uri, user, password, contentType, contentRange, jsonBody);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return request;
    }

    /**
     * Get the response body from the {@link HttpResponse}
     */
    static String getResponse(HttpResponse response) {
        String responseBody = null;
        try {
            //StatusLine statusLine = response.getStatusLine();
            HttpEntity entity = response.getEntity();
            responseBody = EntityUtils.toString(entity);
        }
        catch(Throwable ex){
            ex.printStackTrace();
        }
        finally {
            if (response instanceof CloseableHttpResponse) {
                try {
                    ((CloseableHttpResponse) response).close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return responseBody;
    }

    /**
     * Set the request {@link RequestConfig} Proxy with the proxy address<br>
     * and use the proxy {@link HttpClient} to execute the request to the remote host<br>
     * Return the {@link HttpResponse} from the remote server.
     */
    static HttpResponse executeRequest(HttpClient proxyclient,
            HttpRequestBase request, String remoteHost, String proxyHost, int proxyPort) throws IOException, ClientProtocolException {
        HttpHost proxyAddress = new HttpHost(proxyHost, proxyPort, "http");
        RequestConfig config = RequestConfig.custom().setProxy(proxyAddress).build();
        request.setConfig(config);
        HttpHost targetAddress = new HttpHost(remoteHost, 443, "https");
        logger.debug("Sending " + request.getRequestLine() + " to " + targetAddress + " via " + proxyAddress);
        HttpResponse response = proxyclient.execute(targetAddress, request);
        return response;
    }

    /**
     * Create an {@link HttpRequestBase} corresponding to the {@link RestMethod} argument.<br>
     * ( {@link HttpGet}, {@link HttpPost}, {@link HttpPatch}, {@link HttpDelete}, {@link HttpPut} or {@link HttpOptions} )<br>
     * Add the Authorization, Content-Range and Content-Type headers and the body if exists
     */
    private static HttpRequest createRequest(RestMethod method, String url,
            String username, String password, String contentType, String contentRange, String body)
            throws UnsupportedEncodingException {
        HttpRequestBase request;
        switch (method) {
        case GET:
            request = new HttpGet(url);
            break;
        case POST:
            request = new HttpPost(url);
            break;
        case PATCH:
            request = new HttpPatch(url);
            break;
        case DELETE:
            request = new HttpDelete(url);
            break;
        case PUT:
            request = new HttpPut(url);
            break;
        case OPTIONS:
            request = new HttpOptions(url);
            break;
        default:
            return null;
        }
        request.addHeader("Authorization", computeAuthorization(username, password));
        if (contentRange != null) {
            request.addHeader("Content-Range", contentRange);
        }
        if (contentType != null) {
            request.addHeader("Content-Type", contentType);            
        }
        if (body != null) {
            if (request instanceof HttpEntityEnclosingRequestBase) {
                HttpEntity params = new StringEntity(body);
                ((HttpEntityEnclosingRequestBase) request).setEntity(params);                
            }
        }
        return request;
    }

    private static String computeAuthorization(String username, String password) {
        String userPass = String.format("%s:%s", username, password == null ? "" : password);
        String encoding = DatatypeConverter.printBase64Binary(userPass.getBytes());
        String authorization = "Basic " + encoding;
        return authorization;
    }

    /**
     * Create an {@link HttpClient} to be used to open an SSL https tunnel to the remote server.<br>
     * The {@link SSLSocketFactory} that is used to create the client uses an easy trust manger<br>
     * in order to avoid security certificate checks and allow connection to any remote server.<br>
     * If proxy user and password are supplied then set a {@link UsernamePasswordCredentials} to the client builder.
     */
    private static HttpClient createProxyClient(String proxyHost, int proxyPort, String proxyUser, String proxyPassword) throws GeneralSecurityException {
        SSLSocketFactory sslsf = createSSLSocketFactory();
        RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.<ConnectionSocketFactory>create();
        PlainConnectionSocketFactory socketFactory = new PlainConnectionSocketFactory();
        Registry<ConnectionSocketFactory> registry = registryBuilder.register("http", socketFactory).register("https", sslsf).build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
        cm.setMaxTotal(100);
        HttpClientBuilder builder = HttpClients.custom();
        if (proxyUser != null && proxyPassword != null) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            AuthScope authScope = new AuthScope(proxyHost, proxyPort);
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(proxyUser, proxyPassword);
            credsProvider.setCredentials(authScope, credentials);
            builder.setDefaultCredentialsProvider(credsProvider);
        }
        HttpClient httpClient = builder.setSSLSocketFactory(sslsf).setConnectionManager(cm).build();
        return httpClient;
    }

    /**
     * Create an SSL {@link SocketFactory} that trusts any remote server
     */
    private static SSLSocketFactory createSSLSocketFactory() throws GeneralSecurityException {
        SSLContext sslContext = SSLContext.getInstance("SSL");
        X509TrustManager x509TrustManager = new TrustEverythingX509TrustManager();
        TrustManager[] trustManagers = new TrustManager[] { x509TrustManager };
        sslContext.init(null, trustManagers, new SecureRandom());
        SSLSocketFactory sf = new SSLSocketFactory(sslContext);
        sf.setHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        return sf;
    }
    
    /**
     * TrustManager that trusts everything
     */
    private static class TrustEverythingX509TrustManager implements X509TrustManager {
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }
        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }        
    }

}
