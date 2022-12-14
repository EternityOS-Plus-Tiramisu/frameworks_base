/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.service.cloudsearch;

import static com.android.internal.util.function.pooled.PooledLambda.obtainMessage;

import android.annotation.CallSuper;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.app.Service;
import android.app.cloudsearch.ICloudSearchManager;
import android.app.cloudsearch.SearchRequest;
import android.app.cloudsearch.SearchResponse;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.cloudsearch.ICloudSearchService.Stub;
import android.util.Log;
import android.util.Slog;

/**
 * A service for returning search results from cloud services in response to an on device query.
 *
 * <p>To extend this class, you must declare the service in your manifest file with
 * the {@link android.Manifest.permission#MANAGE_CLOUDSEARCH} permission
 * and include an intent filter with the {@link #SERVICE_INTERFACE} action. For example:</p>
 * <pre>
 *  <uses-permission android:name="android.permission.MANAGE_CLOUDSEARCH"/>
 *  <application>
 *       <service android:name=".CtsCloudSearchService"
 *                android:exported="true"
 *                android:label="CtsCloudSearchService">
 *           <intent-filter>
 *               <action android:name="android.service.cloudsearch.CloudSearchService"/>
 *           </intent-filter>
 *       </service>
 *
 *      <uses-library android:name="android.test.runner"/>
 *
 *  </application>
 * </pre>
 *
 * @hide
 */
@SystemApi
public abstract class CloudSearchService extends Service {

    /**
     * The {@link Intent} that must be declared as handled by the service.
     *
     * <p>The service must also require the {@link android.permission#MANAGE_CLOUDSEARCH}
     * permission.
     *
     * @hide
     */
    public static final String SERVICE_INTERFACE =
            "android.service.cloudsearch.CloudSearchService";
    private static final boolean DEBUG = false;
    private static final String TAG = "CloudSearchService";
    private Handler mHandler;
    private ICloudSearchManager mService;

    private final android.service.cloudsearch.ICloudSearchService mInterface = new Stub() {
        @Override
        public void onSearch(SearchRequest request) {
            mHandler.sendMessage(
                    obtainMessage(CloudSearchService::onSearch,
                    CloudSearchService.this, request));
        }
    };

    @CallSuper
    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) {
            Log.d(TAG, "onCreate CloudSearchService");
        }
        mHandler = new Handler(Looper.getMainLooper(), null, true);

        IBinder b = ServiceManager.getService(Context.CLOUDSEARCH_SERVICE);
        mService = android.app.cloudsearch.ICloudSearchManager.Stub.asInterface(b);
    }

    /**
     * onSearch receives the input request, retrievals the search provider's own
     * corpus and returns the search response through returnResults below.
     *
     *@param request the search request passed from the client.
     *
     */
    public abstract void onSearch(@NonNull SearchRequest request);

    /**
     * returnResults returnes the response and its associated requestId, where
     * requestIs is generated by request through getRequestId().
     *
     *@param requestId the request ID got from request.getRequestId().
     *@param response the search response returned from the search provider.
     *
     */
    public final void returnResults(@NonNull String requestId,
                                    @NonNull SearchResponse response) {
        try {
            mService.returnResults(mInterface.asBinder(), requestId, response);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    @NonNull
    public final IBinder onBind(@NonNull Intent intent) {
        if (DEBUG) {
            Log.d(TAG, "onBind CloudSearchService");
        }
        if (SERVICE_INTERFACE.equals(intent.getAction())) {
            return mInterface.asBinder();
        }
        Slog.w(TAG, "Tried to bind to wrong intent (should be "
                + SERVICE_INTERFACE + ": " + intent);
        return null;
    }
}
