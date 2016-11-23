package io.github.shark_app.shark;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    public static final String PREFS_NAME = "SHARK_PREFS";
    public static final String PREFS_FIRST_START_KEY = "isFirstStart";
    public static final String PREFS_USER_EXISTS_KEY = "userExists";
    public static final String PREFS_USER_NAME = "name";
    public static final String PREFS_USER_EMAIL = "email";
    public static final String PREFS_USER_PUBLIC_KEY = "publicKey";
    public static final String PREFS_USER_PRIVATE_KEY = "privateKey";
    private SharedPreferences settings;

    @BindView(R.id.shareButton) Button shareButton;
    @BindView(R.id.scanButton) Button scanButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (firstStart()) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(PREFS_FIRST_START_KEY, false);
            editor.commit();
            Intent intent = new Intent(this, IntroActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @OnClick(R.id.shareButton)
    public void share(View view) {
        startAppropriateActivityAfterUserExistsCheck(this, RegisterActivity.class, ShareActivity.class);
    }

    @OnClick(R.id.scanButton)
    public void scan(View view) {
        startAppropriateActivityAfterUserExistsCheck(this, RegisterActivity.class, ScanActivity.class);
    }

    private void startAppropriateActivityAfterUserExistsCheck(Context context, Class<?> one, Class<?> two){
        if (!userExists()) {
            final Intent intent = new Intent(context, one);
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Code retrieved")
                    .setTitle("Before you proceed")
                    .setMessage("You must register before being able to access any features of the application. Press OK to register now.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            startActivity(intent);
                            finish();
                        }
                    });
            builder.show();
        }
        else {
            Intent intent = new Intent(context, two);
            startActivity(intent);
            finish();
        }
    }

    private boolean userExists(){
        boolean userExists = settings.getBoolean(PREFS_USER_EXISTS_KEY, false);
        return userExists;
    }

    private boolean firstStart(){
        boolean isFirstStart = settings.getBoolean(PREFS_FIRST_START_KEY, true);
        return isFirstStart;
    }
}
