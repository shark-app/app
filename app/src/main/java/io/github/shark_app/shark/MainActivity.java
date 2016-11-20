package io.github.shark_app.shark;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    public static final String PREFS_NAME = "SHARK_PREFS";
    public static final String PREFS_USER_EXISTS_KEY = "userExists";
    public static final String PREFS_USER_NAME = "name";
    public static final String PREFS_USER_EMAIL = "email";
    public static final String PREFS_USER_PUBLIC_KEY = "publicKey";
    public static final String PREFS_USER_PRIVATE_KEY = "privateKey";

    @BindView(R.id.shareButton) Button shareButton;
    @BindView(R.id.scanButton) Button scanButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.shareButton)
    public void share(View view) {
        Intent intent = new Intent(this, ShareActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.scanButton)
    public void scan(View view) {
    }
}
