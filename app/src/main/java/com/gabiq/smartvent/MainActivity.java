package com.gabiq.smartvent;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cloud.artik.api.MessagesApi;
import cloud.artik.api.UsersApi;
import cloud.artik.client.ApiCallback;
import cloud.artik.client.ApiClient;
import cloud.artik.client.ApiException;
import cloud.artik.model.MessageAction;
import cloud.artik.model.MessageIDEnvelope;

public class MainActivity extends AppCompatActivity {
    static final String TAG = "SVS";
    private static final String ARTIK_CLOUD_AUTH_BASE_URL = "https://accounts.artik.cloud";
    private static final String CLIENT_ID = "<< Enter Your Client ID Here >>";
    private static final String REDIRECT_URL = "http://localhost:8000/acdemo/index.php";
    private static final String DEVICE_ID = "<< Enter Your Device ID Here >>";
    public static final String KEY_ACCESS_TOKEN = "Access_Token";

    private WebView mWebView;
    private LinearLayout mControlView;
    private MessagesApi mMessagesApi = null;
    private String mAccessToken;

    Button openMsgBtn1;
    Button closeMsgBtn1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWebView = (WebView)findViewById(R.id.webView);
        mControlView = (LinearLayout)findViewById(R.id.controlView);

        openMsgBtn1 = (Button)findViewById(R.id.vent1_open);
        closeMsgBtn1 = (Button)findViewById(R.id.vent1_close);
        loadWebView();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void loadWebView() {
        mWebView.setVisibility(View.VISIBLE);
        mWebView.getSettings().setJavaScriptEnabled(true);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String uri) {
                if ( uri.startsWith(REDIRECT_URL) ) {
                    // Redirect URL has format http://localhost:8000/acdemo/index.php#expires_in=1209600&token_type=bearer&access_token=xxxx
                    // Extract OAuth2 access_token in URL
                    String[] sArray = uri.split("&");
                    for (String paramVal : sArray) {
                        if (paramVal.indexOf("access_token=") != -1) {
                            String[] paramValArray = paramVal.split("access_token=");
                            mAccessToken = paramValArray[1];
                            authenticationSucceded();
                            break;
                        }
                    }
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, uri);
            }
        });

        String url = getAuthorizationRequestUri();
        mWebView.loadUrl(url);
    }

    public String getAuthorizationRequestUri() {
        return ARTIK_CLOUD_AUTH_BASE_URL + "/authorize?client=mobile&response_type=token&" +
                "client_id=" + CLIENT_ID + "&redirect_uri=" + REDIRECT_URL;
    }

    private void authenticationSucceded() {
        Toast.makeText(this, "Authentication Succeded", Toast.LENGTH_SHORT).show();
        // hide login
        mWebView.setVisibility(View.GONE);
        // enable control
        mControlView.setVisibility(View.VISIBLE);

        setupArtikCloudApi();
        setupButtonListeners();
    }

    private void setupButtonListeners() {
        openMsgBtn1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                postAction("open");
            }
        });

        closeMsgBtn1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                postAction("close");
            }
        });
    }

    private void setupArtikCloudApi() {
        ApiClient mApiClient = new ApiClient();
        mApiClient.setAccessToken(mAccessToken);
        mApiClient.setDebugging(true);

        mMessagesApi = new MessagesApi(mApiClient);
    }

    private void postAction(String action) {
        final String tag = TAG + " sendMessageActionAsync";

        HashMap<String, Object> parameters = new HashMap<String, Object>();

        Map<String, Object> actionData = new HashMap<String, Object>();
        actionData.put("name", action);
        actionData.put("parameters", parameters);

        ArrayList<Object> actionList = new ArrayList<>();
        actionList.add(actionData);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("actions", actionList);

        MessageAction msg = new MessageAction();
        msg.setDdid(DEVICE_ID);
        msg.setData(data);
        msg.setType("action");

        try {
            mMessagesApi.sendMessageActionAsync(msg, new ApiCallback<MessageIDEnvelope>() {
                @Override
                public void onFailure(ApiException exc, int i, Map<String, List<String>> stringListMap) {
                    processFailure(tag, exc);
                }

                @Override
                public void onSuccess(MessageIDEnvelope result, int i, Map<String, List<String>> stringListMap) {
                    Log.v(tag, " onSuccess response to sending message = " + result.getData().toString());
                }

                @Override
                public void onUploadProgress(long bytes, long contentLen, boolean done) {
                }

                @Override
                public void onDownloadProgress(long bytes, long contentLen, boolean done) {
                }
            });
        } catch (ApiException exc) {
            processFailure(tag, exc);
        }
    }

    private void processFailure(final String context, ApiException exc) {
        String errorDetail = " onFailure with exception" + exc;
        Log.w(context, errorDetail);
        exc.printStackTrace();
        showErrorOnUIThread(context+errorDetail, MainActivity.this);
    }

    static void showErrorOnUIThread(final String text, final Activity activity) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(activity.getApplicationContext(), text, duration);
                toast.show();
            }
        });
    }

}
