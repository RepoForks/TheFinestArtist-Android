/**
 * Copyright 2013 The Finest Artist
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *limitations under the License.
 */

package com.utopia.thefinestartist;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

public class SimpleWebViewFragment extends Fragment implements OnClickListener {

    public static final String EXTRA_URL = "url";
    private String mUrl = null;
    private FrameLayout mView = null;
    private WebView mWebview = null;
    private ProgressBar mLbar = null;
    private ProgressBar mPbar = null;
    private FrameLayout mReload = null;
    private Button mReloadBtn = null;
    private int errorCount = 0;
    private boolean errored = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUrl = getArguments().getString(EXTRA_URL);
        if (!mUrl.startsWith("http"))
            mUrl = "http://" + mUrl;
    }

    @SuppressLint({
            "SetJavaScriptEnabled", "NewApi"
    })
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.web_view, container, false);

        mReload = (FrameLayout) view.findViewById(R.id.web_view_reload);

        mLbar = (ProgressBar) view.findViewById(R.id.web_view_loading);
        mPbar = (ProgressBar) view.findViewById(R.id.web_view_progress);

        mView = (FrameLayout) view.findViewById(R.id.web_view);
        mView.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        mWebview = new WebView(getActivity());
        mWebview.setVisibility(View.GONE);
        mWebview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        if (mUrl != null) {
            mWebview.setWebViewClient(new MyWebViewClient());
            mWebview.setWebChromeClient(new MyWebChromeClient());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO)
                mWebview.getSettings().setPluginState(PluginState.ON);
            mWebview.getSettings().setUseWideViewPort(false);
            mWebview.getSettings().setDefaultZoom(ZoomDensity.FAR);
            mWebview.getSettings().setBuiltInZoomControls(true);
            mWebview.getSettings().setSupportZoom(true);
            mWebview.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            mWebview.getSettings().setAllowFileAccess(true);
            mWebview.getSettings().setDomStorageEnabled(true);
            mWebview.getSettings().setJavaScriptEnabled(true);
            mWebview.addJavascriptInterface(new JavaScriptInterface(getActivity()), "Android");
            mWebview.getSettings().setAppCacheEnabled(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                mWebview.getSettings().setDisplayZoomControls(false);

            mWebview.loadUrl(mUrl);
        }
        mView.addView(mWebview);

        mReloadBtn = (Button) view.findViewById(R.id.reload_btn);
        mReloadBtn.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.reload_btn:
                errored = false;
                mWebview.reload();
                break;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mView.removeView(mWebview);
        mWebview.removeAllViews();
        mWebview.destroy();
    }

    public boolean hasBack() {
        if (mWebview == null)
            return false;
        return mWebview.canGoBack();
    }

    public void back() {
        if (mWebview != null)
            mWebview.goBack();
    }

    public class MyWebChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int progress) {
            if (progress < 100 && mPbar.getVisibility() == ProgressBar.GONE)
                mPbar.setVisibility(ProgressBar.VISIBLE);
            mPbar.setProgress(progress);

            if (progress == 100) {
                mPbar.setVisibility(ProgressBar.GONE);
                if (!errored) {
                    mReload.setVisibility(View.INVISIBLE);
                    errorCount = 0;
                }
            }
        }
    }

    public class MyWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            mReloadBtn.setClickable(false);
            mReloadBtn.setTextColor(getResources().getColor(R.color.white_overlay));
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            view.setVisibility(View.VISIBLE);
            final Animation fade = new AlphaAnimation(0.0f, 1.0f);
            fade.setDuration(200);
            view.startAnimation(fade);
            view.setVisibility(View.VISIBLE);
            mReloadBtn.setClickable(true);
            mReloadBtn.setTextColor(getResources().getColor(android.R.color.white));

            mLbar.setVisibility(View.INVISIBLE);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.endsWith(".mp4")) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(url), "video/*");
                view.getContext().startActivity(intent);
                return true;
            } else {
                return super.shouldOverrideUrlLoading(view, url);
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            errored = true;
            if (errorCount < 2)
                view.reload();
            else {
                mReload.setVisibility(View.VISIBLE);
                view.setVisibility(View.INVISIBLE);
            }
            errorCount++;

            super.onReceivedError(view, errorCode, description, failingUrl);
        }
    }

    public class JavaScriptInterface {

        Context mContext;

        JavaScriptInterface(Context c) {
            mContext = c;
        }
        //add other interface methods to be called from JavaScript

        public void playSoundEffect() {
            AudioManager am = (AudioManager) getActivity().getSystemService(Activity.AUDIO_SERVICE);
            am.playSoundEffect(AudioManager.FX_KEY_CLICK);
        }
    }
}