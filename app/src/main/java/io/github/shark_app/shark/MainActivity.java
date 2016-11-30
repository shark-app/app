package io.github.shark_app.shark;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
    private static final int PERMISSIONS_CAMERA = 1;
    private static final int PERMISSIONS_STORAGE = 2;
    private static Class<?> activityToStart;

    @BindView(R.id.shareButton) Button shareButton;
    @BindView(R.id.scanButton) Button scanButton;
    @BindView(R.id.fab) FloatingActionButton floatingActionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        floatingActionButton.setBackgroundColor(getResources().getColor(R.color.fbutton_color_emerald));
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
        activityToStart = ShareActivity.class;
        checkCameraPermission();
    }

    @OnClick(R.id.scanButton)
    public void scan(View view) {
        activityToStart = ScanActivity.class;
        checkCameraPermission();
    }

    @OnClick(R.id.fab)
    public void opensource(View view) {
        //Intent intent = new Intent(this, OpenSourceLicensesActivity.class);
        //startActivity(intent);
    }

    private void startAppropriateActivityAfterUserExistsCheck(Context context, Class<?> one, Class<?> two){
        if (!userExists()) {
            final Intent intent = new Intent(context, one);
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
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

    private boolean userExists() {
        boolean userExists = settings.getBoolean(PREFS_USER_EXISTS_KEY, false);
        return userExists;
    }

    private boolean firstStart() {
        boolean isFirstStart = settings.getBoolean(PREFS_FIRST_START_KEY, true);
        return isFirstStart;
    }

    private void checkCameraPermission() {
        String permission = Manifest.permission.CAMERA;
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                makeSnackbar("Allow camera access to scan QR code");
            }
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSIONS_CAMERA);
        }
        else {
            checkStoragePermission();
        }
    }

    private void checkStoragePermission() {
        String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                makeSnackbar("Allow access to storage");
            }
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSIONS_STORAGE);
        }
        else {
            startAppropriateActivityAfterUserExistsCheck(this, RegisterActivity.class, activityToStart);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkStoragePermission();
                }
                else {
                    makeAlertDialog("Error!", "The application cannot continue without access to the device camera. Press OK to exit.");
                }
            }
            case PERMISSIONS_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startAppropriateActivityAfterUserExistsCheck(this, RegisterActivity.class, ScanActivity.class);
                }
                else {
                    makeAlertDialog("Error!", "The application cannot continue without access to the device storage. Press OK to exit.");
                }
            }
        }
    }

    private void makeSnackbar(String snackbarText) {
        Snackbar.make(getWindow().getDecorView().getRootView(), snackbarText, Snackbar.LENGTH_LONG)
                .setAction("Dismiss", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {}
                })
                .show();
    }

    private void makeAlertDialog(String title, String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                });
        builder.show();
    }
}
