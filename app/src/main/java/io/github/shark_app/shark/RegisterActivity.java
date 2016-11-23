package io.github.shark_app.shark;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static io.github.shark_app.shark.MainActivity.PREFS_NAME;
import static io.github.shark_app.shark.MainActivity.PREFS_USER_EMAIL;
import static io.github.shark_app.shark.MainActivity.PREFS_USER_EXISTS_KEY;
import static io.github.shark_app.shark.MainActivity.PREFS_USER_NAME;
import static io.github.shark_app.shark.MainActivity.PREFS_USER_PRIVATE_KEY;
import static io.github.shark_app.shark.MainActivity.PREFS_USER_PUBLIC_KEY;

public class RegisterActivity extends AppCompatActivity {
    public static final int PERMISSIONS_REQUEST_CODE = 0;
    public static final int PUBLIC_KEY_FILE_PICKER_REQUEST_CODE = 1;
    public static final int PRIVATE_KEY_FILE_PICKER_REQUEST_CODE = 2;
    private int FILE_PICKER_REQUEST_CODE;
    public String userName;
    public String userEmail;
    public String publicKeyFilePath;
    public String privateKeyFilePath;
    private Boolean pickedPublicKeyFile = false;
    private Boolean pickedPrivateKeyFile = false;

    @BindView(R.id.nameField) EditText nameField;
    @BindView(R.id.emailField) EditText emailField;
    @BindView(R.id.publicKeyButton) Button publicKeyButton;
    @BindView(R.id.privateKeyButton) Button privateKeyButton;
    @BindView(R.id.registerButton) Button registerButton;

    @BindColor(R.color.fbutton_color_alizarin) int red;
    @BindColor(android.R.color.white) int white;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);
        nameField.clearFocus();
        emailField.clearFocus();
    }

    @OnClick(R.id.publicKeyButton)
    public void pickPublicKeyFile(View view) {
        FILE_PICKER_REQUEST_CODE = PUBLIC_KEY_FILE_PICKER_REQUEST_CODE;
        checkPermissionsAndOpenFilePicker();
    }

    @OnClick(R.id.privateKeyButton)
    public void pickPrivateKeyFile(View view) {
        FILE_PICKER_REQUEST_CODE = PRIVATE_KEY_FILE_PICKER_REQUEST_CODE;
        checkPermissionsAndOpenFilePicker();
    }

    @OnClick(R.id.registerButton)
    public void registerUser(View view) {
        boolean errorStatus = validateForm(view);
        if (errorStatus) return;
        setSharedPreferencesData();
    }

    private void setSharedPreferencesData(){
        String userName = nameField.getText().toString().trim();
        String userEmail = emailField.getText().toString().trim();
        String userPublicKey = getKeyFromFile(publicKeyFilePath);
        String userPrivateKey = getKeyFromFile(privateKeyFilePath);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREFS_USER_EXISTS_KEY, true);
        editor.putString(PREFS_USER_NAME, userName);
        editor.putString(PREFS_USER_EMAIL, userEmail);
        editor.putString(PREFS_USER_PUBLIC_KEY, userPublicKey);
        editor.putString(PREFS_USER_PRIVATE_KEY, userPrivateKey);
        editor.commit();
    }

    private String getKeyFromFile(String path){
        String line;
        String data = new String();
        try {
            File file = new File(path);
            FileInputStream fileInputStream = new FileInputStream(file);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
            while ((line = bufferedReader.readLine()) != null) {
                data += line + "\n";
            }
            bufferedReader.close();
        }
        catch (IOException e) {}
        return data;
    }

    private boolean validateForm(View view) {
        boolean setError = false;
        if (!checkEmptySetError(nameField)) {
            userName = nameField.getText().toString().trim();
        }
        else {
            setError = true;
        }
        if (!checkEmptySetError(emailField) && !checkEmailSetError(emailField)) {
            userEmail = emailField.getText().toString().trim();
        }
        else {
            setError = true;
        }
        if (!pickedPublicKeyFile && !pickedPrivateKeyFile) {
            publicKeyButton.setTextColor(red);
            privateKeyButton.setTextColor(red);
            makeSnackbar(view, "No public and private key file selected!");
            setError = true;
        }
        else if (!pickedPublicKeyFile) {
            publicKeyButton.setTextColor(red);
            makeSnackbar(view, "No public key file selected!");
            setError = true;
        }
        else if (!pickedPrivateKeyFile) {
            privateKeyButton.setTextColor(red);
            makeSnackbar(view, "No private key file selected!");
            setError = true;
        }
        return setError;
    }

    private void makeSnackbar(View view, String snackbarText) {
        Snackbar.make(view, snackbarText, Snackbar.LENGTH_LONG)
                .setAction("Dismiss", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {}
                })
                .show();
    }

    private boolean checkEmptySetError(EditText editText){
        String text = editText.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            editText.setError("This field is required");
            return true;
        }
        else return false;
    }

    private boolean checkEmailSetError(EditText editText){
        String text = editText.getText().toString().trim();
        if (isEmailValid(text)) return false;
        else {
            editText.setError("Invalid email address");
            return true;
        }
    }

    boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void checkPermissionsAndOpenFilePicker() {
        String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                showError(getWindow().getDecorView().getRootView());
            }
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSIONS_REQUEST_CODE);
        } else {
            openFilePicker(FILE_PICKER_REQUEST_CODE);
        }
    }

    private void showError(View view) {
        makeSnackbar(view, "Allow external storage reading");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openFilePicker(FILE_PICKER_REQUEST_CODE);
                } else {
                    showError(getWindow().getDecorView().getRootView());
                    checkPermissionsAndOpenFilePicker();
                }
            }
        }
    }

    private void openFilePicker(int FILE_PICKER_REQUEST_CODE) {
        new MaterialFilePicker()
                .withActivity(this)
                .withRequestCode(FILE_PICKER_REQUEST_CODE)
                .withFilter(Pattern.compile("(.*\\.txt$|.*\\.gpg$|.*\\.asc$)"))
                .withHiddenFiles(true)
                .start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
            String fullPath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            if (fullPath != null) {
                int index = fullPath.lastIndexOf(File.separator);
                String filename = fullPath.substring(index + 1);
                switch(FILE_PICKER_REQUEST_CODE) {
                    case 1: {
                        publicKeyFilePath = fullPath;
                        String buttonText = "Picked file " + filename;
                        publicKeyButton.setText(buttonText);
                        publicKeyButton.setTextColor(white);
                        pickedPublicKeyFile = true;
                        break;
                    }
                    case 2: {
                        privateKeyFilePath = fullPath;
                        String buttonText = "Picked file " + filename;
                        privateKeyButton.setText(buttonText);
                        privateKeyButton.setTextColor(white);
                        pickedPrivateKeyFile = true;
                        break;
                    }
                }
            }
            FILE_PICKER_REQUEST_CODE = -1;
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, ShareActivity.class);
        startActivity(intent);
        finish();
    }
}
