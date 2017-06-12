package com.rnfs;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import java.util.Map;

/**
 * ArcaneNetwork Created by jin on 23/05/2017.
 */

public class UploaderParams {
    public interface BeginCallBack {
        void beginCallBack(WritableMap response);
    }

    public interface ProgressCallBack {
        void progressCallBack(WritableMap response);
    }

    public String url;
    public ReadableMap fields;
    public ReadableMap headers;
    public ReadableArray files;
    public UploaderParams.BeginCallBack beginCallBack;
    public UploaderParams.ProgressCallBack progressCallBack;
    public Promise promise;
    public int jobId;
}
