package jp.hamasho.bettei;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(7, 7, 6));
        getWindow().setNavigationBarColor(Color.rgb(7, 7, 6));

        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(9, 8, 6));
        web.setFitsSystemWindows(true);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(true);
        web.getSettings().setAllowContentAccess(true);
        web.getSettings().setAllowFileAccessFromFileURLs(true);
        web.getSettings().setAllowUniversalAccessFromFileURLs(true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String patch = "(function(){"
                        + "var heroLogo=document.querySelector('.hero-logo');if(heroLogo){heroLogo.remove();}"
                        + "var storeLogo=document.querySelector('#store .store-info > div:first-child');"
                        + "if(storeLogo){storeLogo.innerHTML='<img src=\"hamasho_logo_gold.svg\" alt=\"濱匠 Hamasho\" style=\"display:block;width:190px;max-width:72%;height:auto;margin:0 0 10px 0\">';}"
                        + "var memberHead=document.querySelector('.member-card .head');"
                        + "if(memberHead){memberHead.innerHTML='<img src=\"hamasho_logo_gold.svg\" alt=\"濱匠\" style=\"width:88px;height:auto;vertical-align:middle;margin-right:8px\"><span style=\"font-family:sans-serif;letter-spacing:.09em;color:#d8cdbb\">MEMBER</span>';memberHead.style.display='flex';memberHead.style.alignItems='center';}"
                        + "})();";
                view.evaluateJavascript(patch, null);
            }
        });
        web.setWebChromeClient(new WebChromeClient());
        web.loadUrl("file:///android_asset/index.html");
        setContentView(web);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) {
            web.onResume();
            web.evaluateJavascript("if(typeof syncMember==='function'){syncMember();}", null);
        }
    }

    @Override
    protected void onPause() {
        if (web != null) web.onPause();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (web == null) {
            super.onBackPressed();
            return;
        }
        web.evaluateJavascript("(function(){var a=document.querySelector('.page.active');if(a&&a.id!=='home'){showPage('home');return 'handled';}return 'exit';})()", value -> {
            if (value != null && value.contains("exit")) MainActivity.super.onBackPressed();
        });
    }
}
