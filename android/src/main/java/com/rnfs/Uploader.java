package com.rnfs;

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableMap;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.Future;

/**
 * ArcaneNetwork Created by jin on 23/05/2017.
 */

public class Uploader extends AsyncTask<UploaderParams, Integer, UploaderResult> {

    private UploaderParams mParam;
    private Future<UploaderResult> future;
    private Activity activity;

    public Uploader(Activity activity) {
        this.activity = activity;
    }

    @Override
    protected void onPreExecute() {
        // setting progress bar to zero
        super.onPreExecute();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        Log.d("Progress: ", String.valueOf(values));

        WritableMap progressMap = Arguments.createMap();
        progressMap.putInt("jobId", mParam.jobId);
        progressMap.putString("progress", String.valueOf(values[0]));
        mParam.progressCallBack.progressCallBack(progressMap);
    }

    private void upload(final UploaderParams params, final UploaderResult result) throws Exception {
//        final ExecutorService executor = Executors.newCachedThreadPool(Executors.defaultThreadFactory());
//        future = executor.submit(new Callable<UploaderResult>() {
//            @Override
//            public UploaderResult call() throws Exception {
                int bytesRead;
                int bytesAvailable;
                int bufferSize;
                byte[] buffer;
                int maxBufferSize = 1024 * 1024;

                String lineEnd = "\r\n";
                String twoHyphens = "--";
                String boundary =  "*****";

                String uploadUrl = params.url;
                String method = "POST";

                ReadableMap headers = params.headers;
                ReadableArray files = params.files;
                ReadableMap fields = params.fields;

                HttpURLConnection connection = null;
                DataOutputStream outputStream = null;
                URL connectURL = null;
                FileInputStream fileInputStream = null;

                connectURL = new URL(uploadUrl);

                try {
                    connection = (HttpURLConnection) connectURL.openConnection();

                    // Allow Inputs &amp; Outputs.
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setUseCaches(false);

                    connection.setRequestMethod(method);

                    // set headers
                    ReadableMapKeySetIterator iterator = headers.keySetIterator();
                    while (iterator.hasNextKey()) {
                        String key = iterator.nextKey();
                        connection.setRequestProperty(key, headers.getString(key));
                    }

                    connection.setRequestProperty("Connection", "Keep-Alive");
                    connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                    outputStream = new DataOutputStream(connection.getOutputStream());

                    // set fields
                    ReadableMapKeySetIterator fieldIterator = fields.keySetIterator();
                    while (fieldIterator.hasNextKey()) {
                        outputStream.writeBytes(twoHyphens + boundary + lineEnd);

                        String key = fieldIterator.nextKey();
                        outputStream.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"" + lineEnd + lineEnd);
                        outputStream.writeBytes(fields.getString(key));
                        outputStream.writeBytes(lineEnd);
                    }

                    for (int i = 0; i < files.size(); i++) {

                        ReadableMap file = files.getMap(i);
                        String name = file.getString("name");
                        String filename = file.getString("filename");
                        String filepath = file.getString("filepath");

                        WritableMap begin = Arguments.createMap();
                        begin.putInt("jobId", mParam.jobId);
                        mParam.beginCallBack.beginCallBack(begin);

                        fileInputStream = (FileInputStream) activity.getContentResolver().openInputStream(Uri.parse(filepath));

                        outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                        outputStream.writeBytes("Content-Disposition: form-data; name=\"" + name + "\";filename=\"" + filename + "\"" + lineEnd);
                        outputStream.writeBytes(lineEnd);

                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        buffer = new byte[bufferSize];
                        int sentBytes = 0;

                        // Read file
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                        while (bytesRead > 0) {
                            outputStream.write(buffer, 0, bufferSize);
                            sentBytes += bufferSize;
                            publishProgress((int)(sentBytes * 100 / (bytesAvailable + sentBytes)));
                            bytesAvailable = fileInputStream.available();
                            bufferSize = Math.min(bytesAvailable, maxBufferSize);
                            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                        }

                        outputStream.writeBytes(lineEnd);
                    }

                    outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                    // Responses from the server (code and message)

                    int serverResponseCode = connection.getResponseCode();
                    String serverResponseMessage = connection.getResponseMessage();
                    result.statusCode = serverResponseCode;
                    if (serverResponseCode != 204) {
                        fileInputStream.close();
                        outputStream.flush();
                        outputStream.close();
                        mParam.promise.reject(String.valueOf(serverResponseCode), "Error happened: " + serverResponseMessage);
                    } else {
                        WritableMap response = Arguments.createMap();
                        response.putInt("statusCode", serverResponseCode);
                        mParam.promise.resolve(response);
                    }
                } finally {
                    if (outputStream != null) outputStream.close();
                    if (fileInputStream != null) fileInputStream.close();
                    if (connection != null) connection.disconnect();
                }
//
//                return result;
//            }
//        });
    }

    @Override
    protected UploaderResult doInBackground(UploaderParams... params) {
        mParam = params[0];

        UploaderResult res = new UploaderResult();

        try {
            this.upload(mParam, res);
        } catch (Exception ex) {
            res.exception = ex;
            mParam.promise.reject("", ex);
            return res;
        }

        return res;
    }

    public void stop() {
        if (future != null) {
            future.cancel(true);
        }
    }
}
