package it.macisamuele.network;

/**
 * Copyright 2015 Samuele Maci (macisamuele@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
 
import android.content.Context;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.MySSLSocketFactory;
import com.loopj.android.http.PersistentCookieStore;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Client of a generic Web-Application. It should be the only point for which
 * the Android application contacts the remote Server of the application.
 * <p/>
 * The sent requests are managed from an {@link AsyncHttpClient}, so it is
 * useless to wrap the request inside {@link android.os.AsyncTask},
 * {@link android.content.Loader}, {@link java.lang.Thread} or similar
 * structures.
 * <p/>
 * The response of the request is managed from
 * {@link WebApplicationClient.Callback} which implements the basic methods for the
 * status checking (like {@link Callback#onSuccess}, {@link Callback#onFailure},
 * etc.)
 */

public abstract class WebApplicationClient {

    /**
     * Set it to false to disable all the Logging provided from this Client
     */
    public static boolean DEBUG = true;

    /**
     * Used for the logging actions inside the class
     */
    private static final String TAG = "ApplicationClient";

    /**
     * Manage the persistent cookie
     */
    private CookieStore cookieStore;

    /**
     * Communication Client, it send actually the requests and manage the
     * callbacks
     */
    private final AsyncHttpClient asyncHttpClient = new AsyncHttpClient();

    /**
     * Definition of the HTTP Request Types that is possible to manage with this
     * client
     */
    public enum TYPE {
        GET, POST
    }

    /**
     * Extract the base URL of the Web-Application.
     * <p/>
     * An URL is:
     * scheme://[user:password@]domain[:port]/path[?query_string][#fragment_id]
     *
     * @return the base URL of the Web-Application:
     * scheme://[user:password@]domain[:port]
     */
    protected abstract String getBaseUrl();

    public boolean acceptAnyCertificate() {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            MySSLSocketFactory socketFactory = new MySSLSocketFactory(trustStore);
            socketFactory.setHostnameVerifier(MySSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            asyncHttpClient.setSSLSocketFactory(socketFactory);
            return true;
        } catch (IOException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException | KeyStoreException | KeyManagementException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Enable the persistent cookie store management for the {@code context}
     *
     * @param context Context to attach cookie store to
     */
    public void setPersistentCookieStore(Context context) {
        cookieStore = new PersistentCookieStore(context.getApplicationContext());
        asyncHttpClient.setCookieStore(cookieStore);
        if (DEBUG) {
            Log.d(TAG, "Set PersistentCookieStore");
        }
    }

    /**
     * @throws java.lang.IllegalStateException if the {@code cookieStore} is not yet enabled
     * @see com.loopj.android.http.PersistentCookieStore#addCookie(org.apache.http.cookie.Cookie)
     */
    public void addPersistentCookie(Cookie cookie) {
        if (cookieStore != null) {
            cookieStore.addCookie(cookie);
        } else {
            throw new IllegalStateException(
                    "Cannot add cookie without a persistent cookie store. Set the cookie store before calling the method setPersistentCookieStore");
        }
    }

    /**
     * @throws java.lang.IllegalStateException if the {@code cookieStore} is not yet enabled
     * @see com.loopj.android.http.PersistentCookieStore#getCookies()
     */
    public List<Cookie> getPersistentCookies() {
        if (cookieStore != null) {
            return cookieStore.getCookies();
        } else {
            throw new IllegalStateException(
                    "Cannot extract cookies without a persistent cookie store. Set the cookie store before calling the method setPersistentCookieStore");
        }
    }

    /**
     * @throws java.lang.IllegalStateException if the {@code cookieStore} is not yet enabled
     * @see com.loopj.android.http.PersistentCookieStore#clearExpired(java.util.Date)
     */
    public void clearPersistentCookie(Date date) {
        if (cookieStore != null) {
            cookieStore.clearExpired(date);
        } else {
            throw new IllegalStateException(
                    "Cannot clear expired cookies without a persistent cookie store. Set the cookie store before calling the method setPersistentCookieStore");
        }
    }

    /**
     * @throws java.lang.IllegalStateException if the {@code cookieStore} is not yet enabled
     * @see com.loopj.android.http.PersistentCookieStore#clear()
     */
    public void clearPersistentCookie() {
        if (cookieStore != null) {
            cookieStore.clear();
        } else {
            throw new IllegalStateException(
                    "Cannot clear cookies without a persistent cookie store. Set the cookie store before calling the method setPersistentCookieStore");
        }
    }

    /**
     * Custom implementation of AsyncHttpResponseHandler that exploits the
     * {@link Callback}.
     * <p/>
     * Simplify the building of the correct response needed for the different
     * kind requests.
     */
    private static class CustomAsyncHttpResponseHandler extends
            AsyncHttpResponseHandler {

        private Callback callback;

        @SuppressWarnings("unused")
        private CustomAsyncHttpResponseHandler() {
            throw new UnsupportedOperationException("Cannot initialize "
                    + getClass().getCanonicalName()
                    + " class without parameters");
        }

        public CustomAsyncHttpResponseHandler(Callback callback) {
            this.callback = callback;
        }

        /**
         * Extract the URL of the request. Method should be used only for
         * debugging
         */
        private String getSentUrl() {
            return getRequestURI().getScheme() + "://"
                    + getRequestURI().getAuthority()
                    + getRequestURI().getPath();
        }

        @Override
        public void onCancel() {
            if (DEBUG) {
                Log.d(TAG, "onCancel() " + getSentUrl());
                super.onCancel();
            }

            callback.onCancel();
        }

        @Override
        public void onFailure(int statusCode, Header[] headers,
                              byte[] responseBody, Throwable error) {
            if (DEBUG) {
                StringWriter errors = new StringWriter();
                error.printStackTrace(new PrintWriter(errors));
                Log.d(TAG, "onFailure(" + statusCode
                        + ", headers, responseBody, error) " + getSentUrl());
                Log.d(TAG,
                        "responseBody:\n"
                                + (responseBody != null ? new String(
                                responseBody) : ""));
                Log.e(TAG, "error:\n" + errors.toString());
            }

            callback.onFailure(statusCode, headers, responseBody, error);
        }

        @Override
        public void onFinish() {
            if (DEBUG) {
                Log.d(TAG, "onFinish() " + getSentUrl());
            }

            callback.onFinish();
        }

        @Override
        public void onProgress(int bytesWritten, int totalSize) {
            if (DEBUG) {
                Log.d(TAG, "onProgress(" + bytesWritten + ", " + totalSize
                        + ") " + getSentUrl());
                super.onProgress(bytesWritten, totalSize);
            }

            callback.onProgress(bytesWritten, totalSize);
        }

        @Override
        public void onRetry(int retryNo) {
            if (DEBUG) {
                Log.d(TAG, "onRetry(" + retryNo + ") " + getSentUrl());
                super.onRetry(retryNo);
            }

            callback.onRetry(retryNo);
        }

        @Override
        public void onStart() {
            if (DEBUG) {
                Log.d(TAG, "onStart() " + getSentUrl());
            }

            callback.onStart();
        }

        @Override
        public void onSuccess(int statusCode, Header[] headers,
                              byte[] responseBody) {
            if (DEBUG) {
                Log.d(TAG, "onSuccess(" + statusCode
                        + ", headers, responseBody) " + getSentUrl());
                Log.d(TAG,
                        "responseBody:\n"
                                + (responseBody != null ? new String(
                                responseBody) : ""));
            }

            callback.onSuccess(statusCode, headers, responseBody);
        }

    }

    /**
     * Extract the full URL for the request. Exploit the information from the
     * {@code getBaseUrl()} and from the API declared in the request.
     *
     * @param request - request to send
     */
    private String getUrl(Request request) {
        String api = request.getPath();
        String baseUrl = getBaseUrl();
        if (api.charAt(0) == '/') {
            api = api.substring(1);
            Log.v(TAG,
                    "the API of the request should not start with '/' (automatically removed)");
        }
        if (baseUrl.charAt(baseUrl.length()-1) == '/') {
            baseUrl = baseUrl.substring(0, baseUrl.length()-1);
            Log.v(TAG,
                    "the base url of the web application should not end with '/' (automatically removed)");
        }
        return baseUrl + "/" + api;
    }

    /**
     * Send the {@code request} to the {@code client}
     *
     * @param client   web-application Client
     * @param request  request to send
     * @param callback callback to manage the response
     */
    public static void send(WebApplicationClient client, Request request,
                            Callback callback) {
        switch (request.getType()) {
            case GET:
                client.asyncHttpClient.get(client.getUrl(request),
                        new RequestParams(request.getParameters()),
                        new CustomAsyncHttpResponseHandler(callback));
                break;
            case POST:
                client.asyncHttpClient.post(client.getUrl(request),
                        new RequestParams(request.getParameters()),
                        new CustomAsyncHttpResponseHandler(callback));
                break;
        }
    }

    /**
     * Wrapper class of the useful methods (in terms of management of the
     * connection states) in {@code AsyncHttpResponseHandler}
     * <p/>
     * Contains a default implementation of all the required methods, in order
     * to fast send request with a small effort in terms of methods definition.
     */
    public static class Callback {

        /**
         * @see com.loopj.android.http.AsyncHttpResponseHandler#onCancel()
         */
        public void onCancel() {
        }

        /**
         * @see com.loopj.android.http.AsyncHttpResponseHandler#onFailure(int,
         * org.apache.http.Header[], byte[], Throwable)
         */
        public void onFailure(int statusCode, Header[] headers,
                              byte[] responseBody, Throwable error) {
        }

        /**
         * @see com.loopj.android.http.AsyncHttpResponseHandler#onFinish()
         */
        public void onFinish() {
        }

        /**
         * @see com.loopj.android.http.AsyncHttpResponseHandler#onProgress(int,
         * int)
         */
        public void onProgress(int bytesWritten, int totalSize) {
        }

        /**
         * @see com.loopj.android.http.AsyncHttpResponseHandler#onRetry(int)
         */
        public void onRetry(int retryNo) {
        }

        /**
         * @see com.loopj.android.http.AsyncHttpResponseHandler#onStart()
         */
        public void onStart() {
        }

        /**
         * @see com.loopj.android.http.AsyncHttpResponseHandler#onSuccess(int,
         * org.apache.http.Header[], byte[])
         */
        public void onSuccess(int statusCode, Header[] headers,
                              byte[] responseBody) {
        }

    }

    /**
     * Generic definition of an HTTP Request
     */
    public abstract static class Request {

        /**
         * Extract the path for the request According to the RFC 3986 the path
         * is the part after the domain name.
         * <p/>
         * An URL is:
         * scheme://[user:password@]domain[:port]/path[?query_string][
         * #fragment_id]
         * <p/>
         * The path is used to specify and perhaps find the resource requested
         *
         * @return request's path
         */
        public abstract String getPath();

        /**
         * Extract the type of the request to perform (HTTP GET or HTTP POST)
         *
         * @return request type
         */
        public abstract TYPE getType();

        /**
         * Extract the parameters as (key, value) associations for the request
         *
         * @return a map containing parameters, null if there is no parameters
         */
        public Map<String, String> getParameters() {
            return null;
        }
    }

}
