package io.github.shark_app.shark;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;

public class OpenSourceLicensesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_source_licenses);
        WebView view = (WebView) findViewById(R.id.webview);
        view.loadUrl("file:///android_asset/open_source_licenses.html");
    }
}
