package jp.hamasho.bettei;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
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
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                        return true;
                    } catch (Exception ignored) {
                        return false;
                    }
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String patch = "(function(){"
                        + "var hero=document.querySelector('.hero');"
                        + "if(hero){hero.style.height='390px';hero.style.background='#120d09';"
                        + "var bg=hero.querySelector('.bg');if(bg){var im=document.createElement('img');im.src='home_sprite_v09.jpg';im.alt='濱匠 店舗入口';im.style.cssText='position:absolute;right:0;top:0;width:150%;max-width:none;height:auto;filter:brightness(.60) saturate(.88);';bg.replaceWith(im);}"
                        + "var heroLogo=hero.querySelector('.hero-logo');if(heroLogo){heroLogo.innerHTML='<img src=\"hamasho_logo_gold.svg\" alt=\"濱匠 Hamasho\" style=\"display:block;width:100%;height:auto\">';heroLogo.style.left='50%';heroLogo.style.right='auto';heroLogo.style.bottom='18px';heroLogo.style.width='70%';heroLogo.style.maxWidth='310px';heroLogo.style.transform='translateX(-50%)';heroLogo.style.filter='drop-shadow(0 4px 12px #000)';}"
                        + "var heroCopy=hero.querySelector('.hero-copy');if(heroCopy){heroCopy.style.display='none';}}"
                        + "var storePhoto=document.querySelector('#store .store-photo');if(storePhoto){storePhoto.style.position='relative';storePhoto.style.height='220px';storePhoto.style.overflow='hidden';var s=storePhoto.querySelector('.sprite');if(s){var si=document.createElement('img');si.src='home_sprite_v09.jpg';si.alt='濱匠 店舗入口';si.style.cssText='position:absolute;right:0;top:0;width:150%;max-width:none;height:auto;filter:brightness(.72) saturate(.9);';s.replaceWith(si);}}"
                        + "var storeLogo=document.querySelector('#store .store-info > div:first-child');if(storeLogo){storeLogo.innerHTML='<img src=\"hamasho_logo_gold.svg\" alt=\"濱匠 Hamasho\" style=\"display:block;width:210px;max-width:72%;height:auto;margin:0 0 10px 0\">';}"
                        + "var memberHead=document.querySelector('.member-card .head');if(memberHead){memberHead.innerHTML='<img src=\"hamasho_logo_gold.svg\" alt=\"濱匠 Hamasho\" style=\"width:105px;height:auto;margin-right:9px\"><span style=\"font-family:sans-serif;letter-spacing:.09em;color:#d8cdbb\">MEMBER</span>';memberHead.style.display='flex';memberHead.style.alignItems='center';}"
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
