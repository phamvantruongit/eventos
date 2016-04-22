package com.appvisor_event.master.modules;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;

import com.appvisor_event.master.Contents;

/**
 * Created by gomez on 2016/04/17.
 */
public class WebAppInterface {
    Context context;
    private ViewGroup linearLayout;
    private View imageView;

    public WebAppInterface(Context context){
        this.context = context;
    }

    @JavascriptInterface
    public void beacons(String data){
        ((Contents)context).startBeacon(data);
    }

    @JavascriptInterface
    public void openQRCodeScanner(){
        ((Contents)context).startQR();
    }

    @JavascriptInterface
    public void addNavigationBarButton(String fileName, String url){
        ((Contents)context).buttonBar(fileName, url);
    }

    @JavascriptInterface
    public void clipBoard(String txt){
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("number",txt);
        clipboard.setPrimaryClip(clip);
    }
}
