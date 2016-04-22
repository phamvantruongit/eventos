package com.appvisor_event.master;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.appvisor_event.master.modules.AssetsManager;
import com.appvisor_event.master.modules.JavascriptHandler.FavoritSeminarJavascriptHandler;
import com.appvisor_event.master.modules.JavascriptManager;
import com.appvisor_event.master.modules.WebAppInterface;
import com.google.zxing.Result;
import com.google.zxing.qrcode.encoder.QRCode;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.Region;

import java.util.HashMap;
import java.util.Map;

public class Contents extends Activity implements BeaconConsumer {

    private Uri m_uri;
    private static final String TYPE_IMAGE = "image/*";
    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mFilePathCallback;

    private WebView myWebView;
    private static final String TAG = "TAG";
    private BeaconManager beaconManager;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private String active_url = Constants.HOME_URL;
    private String device_id;
    private Map<String, String> extraHeaders;
    private MyHttpSender myJsonSender;
    private BluetoothAdapter bluetoothAdapter;
    //レイアウトで指定したWebViewのIDを指定する。
    private boolean mIsFailure = false;
    private String backurl = "#####";
    private String minor;
    private String mayor;
    private String UUID;
    private String region;
    private double longitude;
    private double latitude;
    private GPSManager gps;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //UUIDの取得
        device_id  = AppUUID.get(this.getApplicationContext()).replace("-","").replace(" ","").replace(">","").replace("<","");
        extraHeaders = new HashMap<String, String>();
        extraHeaders.put("user-id", device_id);
        Log.d("device_token",device_id);

        //ホーム画面の設定
        setContentView(R.layout.activity_main_contents);
        gps = new GPSManager(this);
        //タイトルバーを非表示
        findViewById(R.id.title_bar).setVisibility(View.GONE);
        //レイアウトで指定したWebViewのIDを指定する。
        myWebView = (WebView) findViewById(R.id.webView1);
        myWebView.addJavascriptInterface(new WebAppInterface(this),"Android");
        // JS利用を許可する
        myWebView.getSettings().setJavaScriptEnabled(true);

        //CATHEを使用する
        myWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);

        // Android 5.0以降は https のページ内に http のコンテンツがある場合に表示出来ない為設定追加。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            myWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // インテントを取得
        Intent intent = getIntent();
        // インテントに保存されたデータを取得
        active_url = intent.getStringExtra("key.url");
        Log.d("active_url_contents",active_url);

        if(!mIsFailure){
            if (device_id != null){
                //デバイストークンが取れていれば、URLをロードする。
                extraHeaders.put("user-id", device_id);
                myWebView.loadUrl(active_url,extraHeaders);
            }
        }
        //ズーム機能を有効にする
        myWebView.setVerticalScrollbarOverlay(true);
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setBuiltInZoomControls(true);
        myWebView.getSettings().setSupportZoom(true);

        myWebView.getSettings().setLoadWithOverviewMode(true);
        myWebView.getSettings().setUseWideViewPort(true);

        myWebView.setWebViewClient(mWebViewClient);

        myWebView.goBack();

        Button update_button = (Button)findViewById(R.id.update_button);

        update_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                // エラーをTRUEに戻す
                mIsFailure = false;
                // 更新を行う
                extraHeaders.put("user-id", device_id);
                myWebView.loadUrl(active_url,extraHeaders);
            }
        });

        // SwipeRefreshLayoutの設定
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(mOnRefreshListener);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.red, R.color.green, R.color.blue, R.color.yellow);

        Log.d("device_token", device_id);

        try {

            // 引数にサーバーのURLを入れる。
            myJsonSender = new MyHttpSender ( Constants.REGISTER_API_URL );
            myJsonSender.mData = device_id ;
            myJsonSender.start ();
            myJsonSender.join ();


            // responseがあればログ出力する。
            if ( myJsonSender.mResponse != null ) {
                Log.i ( "message", myJsonSender.mResponse );
            }

        } catch ( InterruptedException e ) {

            e.printStackTrace ();
            Log.i ( "JSON", e.toString () );

        }

        // お気に入りに登録しているセミナーの開始時間10分前にローカル通知を発行する準備
        this.setupFavoritSeminarAlarm();
    }

    public void onClickSearch(View view){
        view .setBackgroundColor(getResources().getColor(R.color.selected_color));
    }
    public void onClickButtonBack(View view) {
        // 端末の戻るボタンを押した時にwebviewの戻る履歴があれば1つ前のページに戻る
        if (myWebView.canGoBack() == true) {
            Log.d("戻る前URL",(myWebView.copyBackForwardList().getItemAtIndex(myWebView.copyBackForwardList().getCurrentIndex() -1).getUrl()));
//                        if (myWebView.copyBackForwardList().getItemAtIndex(myWebView.copyBackForwardList().getCurrentIndex() -1).getUrl().indexOf(Constants.FAVORITE_URL) != -1){
//                            myWebView.goBack();
//                            backurl = myWebView.getUrl();
//                        }else{
            view .setBackgroundColor(getResources().getColor(R.color.selected_color));
            myWebView.goBack();

        }else{
            view .setBackgroundColor(getResources().getColor(R.color.selected_color));
            Contents.this.finish();
        }
    }

    public void buttonBar(String fileName, String url){
        ViewGroup linearLayout = (ViewGroup) findViewById(R.id.title_bar);
        View view = (View)linearLayout.findViewById(R.id.menu_buttom);
        view.setVisibility(View.GONE);
        View searchLayout = LayoutInflater.from(Contents.this).inflate(R.layout.search_layout,linearLayout);
        linearLayout.addView(searchLayout,1);
    }

    public void onClickMenu(View view) {
        // インテントのインスタンス生成
        view.setBackgroundColor(getResources().getColor(R.color.selected_color));
        Intent intent = new Intent(Contents.this, SubMenu.class);
        int requestCode = 1;
        startActivityForResult(intent, requestCode);

    }

    public void startQR(){
        if(gps.canGetLocation){
            Intent intent = new Intent(this,QrCodeActivity.class);
            startActivityForResult(intent,3);
        }else{
            gps.showSettingsAlert();
        }
    }

    public void startBeacon(){
        beaconManager = BeaconManager.getInstanceForApplication(this);
        bluetoothAdapter = bluetoothAdapter.getDefaultAdapter();
        if(!bluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 2);
        }else if(beaconManager.checkAvailability()) {
            beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
            beaconManager.bind(this);
            beaconManager.setBackgroundMode(true);
        }else{
            Toast.makeText(this,"iBeacon is not supported on this device",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRestart(){
        super.onRestart();
        final ImageView btn_back_button = (ImageView)findViewById(R.id.btn_back_button);
        final ImageView menu_buttom = (ImageView)findViewById(R.id.menu_buttom);
        menu_buttom .setBackgroundColor(Color.TRANSPARENT);
        btn_back_button .setBackgroundColor(Color.TRANSPARENT);
        if(beaconManager!=null) {
            if (beaconManager.isBound(this)) beaconManager.setBackgroundMode(true);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 端末の戻るボタンを押した時にwebviewの戻る履歴があれば1つ前のページに戻る
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (myWebView.canGoBack() == true) {
//                if(myWebView.copyBackForwardList().getItemAtIndex(-1).getUrl().indexOf(Constants.FAVORITE_URL) != -1){
//                    extraHeaders.put("user-id", device_id);
                  myWebView.goBack();
                  Log.d("getUrl", myWebView.getUrl());
//                    myWebView.loadUrl(myWebView.copyBackForwardList().getItemAtIndex(myWebView.copyBackForwardList().getCurrentIndex() -1).getUrl(), extraHeaders);
//                }
//                myWebView.goBack();
                return true;
            }else{
                finish();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(beaconManager!=null) {
            if (beaconManager.isBound(this)) beaconManager.unbind(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    active_url = bundle.getString("key.url", "");
                    if(active_url.equals(Constants.HOME_URL)){
                        finish();
                    }

                    if (null != myWebView) {
                        Log.d("device_token",device_id);
                        extraHeaders.put("user-id", device_id);
                        myWebView.loadUrl(active_url,extraHeaders);
                    }
                }
                break;
            case 2:
                if(resultCode==RESULT_OK) {
                    this.recreate();
                }
                break;
            case 3:
                if(resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    String code = bundle.getString("data");
                    latitude = bundle.getDouble("lat");
                    longitude = bundle.getDouble("lon");
                    Toast.makeText(Contents.this, code+"  "+latitude+"-"+longitude, Toast.LENGTH_SHORT).show();
                    myWebView.loadUrl("javascript:CheckIn.scanQRCode('" + device_id + code + latitude + longitude + "')");
                }
            case 4:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (mFilePathCallback == null) {
                        super.onActivityResult(requestCode, resultCode, data);
                        return;
                    }
                    Uri[] results = null;
                    if (resultCode == RESULT_OK) {
                        String dataString = data.getDataString();
                        if (dataString != null) {
                            results = new Uri[]{Uri.parse(dataString)};
                        }
                    }
                    mFilePathCallback.onReceiveValue(results);
                    mFilePathCallback = null;
                } else {
                    if (mUploadMessage == null) {
                        super.onActivityResult(requestCode, resultCode, data);
                        return;
                    }
                    Uri result = null;
                    if (resultCode == RESULT_OK) {
                        if (data != null) {
                            result = data.getData();
                        }
                    }
                    mUploadMessage.onReceiveValue(result);
                    mUploadMessage = null;
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onPause(){
        super.onPause();
    }

    private SwipeRefreshLayout.OnRefreshListener mOnRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            // 更新処理
            if(beaconManager!=null) {
                if (beaconManager.isBound(Contents.this)) beaconManager.unbind(Contents.this);
            }
            extraHeaders.put("user-id", device_id);
            myWebView.loadUrl(active_url,extraHeaders);
        }
    };

    /** WebViewClientクラス */
    private WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            active_url = url;
            if((url.indexOf(Constants.APPLI_DOMAIN) != -1) || (url.indexOf(Constants.EXHIBITER_DOMAIN) != -1)) {
                extraHeaders.put("user-id", device_id);
                Contents.this.myWebView.loadUrl(url, Contents.this.extraHeaders);
                return false;
            }else{
                view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                return true;
            }
        }
        /**
         * @see android.webkit.WebViewClient#onPageFinished(android.webkit.WebView, java.lang.String)
         */
        @Override
        public void onPageFinished(WebView view, String url) {
            final ImageView btn_back_button = (ImageView)findViewById(R.id.btn_back_button);
            btn_back_button.setBackgroundColor(Color.TRANSPARENT);
            // ajax通信をキャッチしてレスポンスを受け取れるように準備する
            Contents.this.setupJavascriptHandler();

            super.onPageFinished(view, url);
            myWebView.loadUrl("javascript:NavigationSearchButton.run()");
            if(url.equals(Constants.ERROR_URL)){
                mIsFailure = true;
            }
            if (mIsFailure) {
                //ホーム以外の場合はタイトルバーを表示する
                findViewById(R.id.title_bar).setVisibility(View.GONE);
                //WEBVIEWを非表示にする。
                findViewById(R.id.webView1).setVisibility(View.GONE);
                //SWIPEを非表示にする。
                findViewById(R.id.swipe_refresh_layout).setVisibility(View.GONE);
                //エラーページを表示する
                findViewById(R.id.error_page).setVisibility(View.VISIBLE);
            }else {
                if (url.equals(Constants.HOME_URL)) {
                    active_url = url;
                    mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
                    mSwipeRefreshLayout.setEnabled(true);
                    //ホームの場合はタイトルバーを非表示にする
                    findViewById(R.id.title_bar).setVisibility(View.GONE);
                    //SWIPEを表示にする。
                    findViewById(R.id.swipe_refresh_layout).setVisibility(View.VISIBLE);
                    //WEBVIEWを表示にする
                    findViewById(R.id.webView1).setVisibility(View.VISIBLE);
                    //エラーページを非表示にする
                    findViewById(R.id.error_page).setVisibility(View.INVISIBLE);

                } else if ((url.indexOf(Constants.BOOTH) != -1) || (url.indexOf(Constants.HALL_URL) != -1)) {
                    active_url = url;
                    // SwipeRefreshLayoutの設定
                    mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
                    //SWIPEを表示にする。
                    findViewById(R.id.swipe_refresh_layout).setVisibility(View.VISIBLE);
                    //ホーム以外の場合はタイトルバーを表示する
                    findViewById(R.id.title_bar).setVisibility(View.VISIBLE);
                    //WEBVIEWを表示にする
                    findViewById(R.id.webView1).setVisibility(View.VISIBLE);
                    //エラーページを非表示にする
                    findViewById(R.id.error_page).setVisibility(View.INVISIBLE);
                    //更新処理はできなくする
                    mSwipeRefreshLayout.setEnabled(false);
                    // IDからTextViewインスタンスを取得
                    TextView textView = (TextView) findViewById(R.id.content_text);
                    // 表示するテキストの設定
                    if(myWebView.getTitle().length() >= 10){
                        textView.setText(myWebView.getTitle().substring(0,10) + "...");
                    }else{
                        textView.setText(myWebView.getTitle());
                    }
                } else {
                    active_url = url;
                    mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
                    mSwipeRefreshLayout.setEnabled(true);
                    //ホーム以外の場合はタイトルバーを表示する
                    findViewById(R.id.title_bar).setVisibility(View.VISIBLE);
                    //SWIPEを表示にする。
                    findViewById(R.id.swipe_refresh_layout).setVisibility(View.VISIBLE);
                    //WEBVIEWを表示にする
                    findViewById(R.id.webView1).setVisibility(View.VISIBLE);
                    //エラーページを非表示にする
                    findViewById(R.id.error_page).setVisibility(View.INVISIBLE);
                    // IDからTextViewインスタンスを取得
                    TextView textView = (TextView) findViewById(R.id.content_text);
                    // 表示するテキストの設定
                    if(myWebView.getTitle().length() >= 15){
                        textView.setText(myWebView.getTitle().substring(0,15) + "...");
                    }else{
                        textView.setText(myWebView.getTitle());
                    }
                }
                // 0.2秒待機
                if(backurl.equals(myWebView.getUrl())){
                    extraHeaders.put("user-id", device_id);
                    myWebView.loadUrl(myWebView.getUrl(),extraHeaders);
                    backurl = "####";

                }
            }
            mSwipeRefreshLayout.setRefreshing(false);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            mIsFailure = true;
            myWebView.loadUrl("");
            //WEBVIEWを非表示にする。
            findViewById(R.id.webView1).setVisibility(View.GONE);
            //SWIPEを非表示にする。
            findViewById(R.id.swipe_refresh_layout).setVisibility(View.GONE);
            //エラーページを表示する
            findViewById(R.id.error_page).setVisibility(View.VISIBLE);
        }
    };


    private void setupFavoritSeminarAlarm()
    {
        this.setupWebChromeClient();
        this.setupJavascriptManager();
    }

    private void setupWebChromeClient() {
        this.myWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                if (JavascriptManager.getInstance().onJsAlert(message)) {
                    result.cancel();
                    return true;
                }

                return super.onJsAlert(view, url, message, result);
            }

            // For Android < 3.0
            public void openFileChooser(ValueCallback<Uri> uploadFile) {
                openFileChooser(uploadFile, "");
            }

            // For 3.0 <= Android < 4.1
            public void openFileChooser(ValueCallback<Uri> uploadFile, String acceptType) {
                openFileChooser(uploadFile, acceptType, "");
            }

            // For 4.1 <= Android < 5.0
            public void openFileChooser(ValueCallback<Uri> uploadFile, String acceptType, String capture) {
                if (mUploadMessage != null) {
                    mUploadMessage.onReceiveValue(null);
                }
                mUploadMessage = uploadFile;

//                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                intent.addCategory(Intent.CATEGORY_OPENABLE);
//                intent.setType(TYPE_IMAGE);
//
//                startActivityForResult(intent, 4);
                openIntent();
            }

            // For Android 5.0+
            @Override
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = filePathCallback;

//                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                intent.addCategory(Intent.CATEGORY_OPENABLE);
//                intent.setType(TYPE_IMAGE);
//                startActivityForResult(intent, 4);
                openIntent();

                return true;
            }

            private void openIntent()
            {
                //カメラの起動Intentの用意
                String photoName = System.currentTimeMillis() + ".jpg";
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.Images.Media.TITLE, photoName);
                contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                m_uri = getContentResolver()
                        .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

                Intent intentCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, m_uri);

                // ギャラリー用のIntent作成
                Intent intentGallery;
                if (Build.VERSION.SDK_INT < 19) {
                    intentGallery = new Intent(Intent.ACTION_GET_CONTENT);
                    intentGallery.setType("image/*");
                } else {
                    intentGallery = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intentGallery.addCategory(Intent.CATEGORY_OPENABLE);
                    intentGallery.setType("image/jpeg");
                }
                Intent intent = Intent.createChooser(intentCamera, "画像の選択");
                intent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {intentGallery});
                startActivityForResult(intent, 4);
            }
        });
    }

    private void setupJavascriptManager()
    {
        JavascriptManager.getInstance().addHandler(new FavoritSeminarJavascriptHandler(this.getApplicationContext()));
    }

    private void setupJavascriptHandler()
    {
        this.myWebView.loadUrl(String.format("javascript: %s;", this.ajaxHandlerJavascript()));
    }

    private String ajaxHandlerJavascript()
    {
        return new AssetsManager(this).loadStringFromFile("ajax_handler.js");
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setMonitorNotifier(new MonitorNotifier() {

            @Override
            public void didEnterRegion(Region region) {
                myWebView.loadUrl("javascript:CheckIn.detectBeacon('" + device_id + region + UUID + mayor + minor + "')");
                Log.i(TAG, "I just saw a Beacon");
            }

            @Override
            public void didExitRegion(Region region) {
                Log.i(TAG, "I no longer see a Beacon");
            }

            @Override
            public void didDetermineStateForRegion(int i, Region region) {
                Log.i(TAG, "I switched  from seeing to not seeing beacons: ");
            }
        });

        try {
            mayor = "0.0";
            minor = "0.0";
            UUID = "ASVBRGBr";
            region= "number1";
            beaconManager.startMonitoringBeaconsInRegion(new Region("UniqueId",null,null,null));
        }catch (RemoteException e){}
    }
}
