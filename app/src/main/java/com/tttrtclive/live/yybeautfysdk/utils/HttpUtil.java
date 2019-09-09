package com.tttrtclive.live.yybeautfysdk.utils;

import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HttpUtil {
    private static final String TAG = "HttpUtil";

    private OkHttpClient mClient;

    public HttpUtil() {
        mClient = new OkHttpClient();
    }

    public void request(final String url, final CallbackJSON callback) {
        Request request = new Request.Builder().url(url).build();

        mClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "http request error: " + url);

                if (callback != null) {
                    callback.onFailure();
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                ResponseBody body = response.body();
                if (body == null) {
                    Log.e(TAG, "response body is null");

                    if (callback != null) {
                        callback.onFailure();
                    }
                    return;
                }

                try {
                    String jsonStr = body.string();

                    Log.i(TAG, "onResponse: " + jsonStr);

                    JSONObject json = new JSONObject(jsonStr);

                    if (callback != null) {
                        callback.onComplete(json);
                    }
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void request(final String url, final String path, final CallbackFile callback) {
        Request request = new Request.Builder().url(url).build();

        mClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "http request error: " + url);

                if (callback != null) {
                    callback.onFailure();
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                ResponseBody body = response.body();
                if (body == null) {
                    Log.e(TAG, "response body is null");

                    if (callback != null) {
                        callback.onFailure();
                    }
                    return;
                }

                try {
                    InputStream is = body.byteStream();

                    HttpUtil.this.createDirectoryIfNotExist(path);

                    FileOutputStream os = new FileOutputStream(path);
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = is.read(buffer)) > 0) {
                        os.write(buffer, 0, read);
                    }
                    os.close();
                    is.close();

                    if (callback != null) {
                        callback.onComplete();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void createDirectoryIfNotExist(String filePath) {
        File dir = new File(filePath).getParentFile();
        if (!dir.exists()) {
            boolean ret = dir.mkdirs();
            if (!ret) {
                Log.e(TAG, "create file dir failed: " + dir.getPath());
            }
        }
    }

    public interface CallbackJSON {
        void onFailure();
        void onComplete(JSONObject json);
    }

    public interface CallbackFile {
        void onFailure();
        void onComplete();
    }
}
