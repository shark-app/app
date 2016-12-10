package io.github.shark_app.shark;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSecretKey;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Iterator;
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
    public String publicKeyFilePath;
    public String privateKeyFilePath;
    private String getDataTaskOutput;
    private String postDataTaskOutput;
    private Boolean pickedPublicKeyFile = false;
    private Boolean pickedPrivateKeyFile = false;
    private ProgressDialog progressDialog;
    private SharedPreferences settings;
    private String globalName;
    private String globalEmail;
    private String globalPublicKey;
    private String globalPrivateKey;
    @BindView(R.id.nameField) EditText nameField;
    @BindView(R.id.emailField) EditText emailField;
    @BindView(R.id.publicKeyButton) Button publicKeyButton;
    @BindView(R.id.privateKeyButton) Button privateKeyButton;
    @BindView(R.id.registerButton) Button registerButton;

    @BindColor(R.color.fbutton_color_pomegranate) int red;
    @BindColor(android.R.color.white) int white;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);
        nameField.clearFocus();
        emailField.clearFocus();
        settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @OnClick(R.id.publicKeyButton)
    public void pickPublicKeyFile(View view) {
        hideSoftKeyboard();
        FILE_PICKER_REQUEST_CODE = PUBLIC_KEY_FILE_PICKER_REQUEST_CODE;
        checkPermissionsAndOpenFilePicker();
    }

    @OnClick(R.id.privateKeyButton)
    public void pickPrivateKeyFile(View view) {
        hideSoftKeyboard();
        FILE_PICKER_REQUEST_CODE = PRIVATE_KEY_FILE_PICKER_REQUEST_CODE;
        checkPermissionsAndOpenFilePicker();
    }

    @OnClick(R.id.registerButton)
    public void registerUser(View view) {
        hideSoftKeyboard();
        boolean errorStatus = validateForm(view);
        if (errorStatus) return;
        String userName = nameField.getText().toString().trim();
        String userEmail = emailField.getText().toString().trim();
        String userPublicKey = getKeyFromFile(publicKeyFilePath);
        String userPrivateKey = getKeyFromFile(privateKeyFilePath);
        globalName = userName;
        globalEmail = userEmail;
        globalPublicKey = userPublicKey;
        globalPrivateKey = userPrivateKey;
        uploadUserPublicData(userName, userEmail, userPublicKey);
    }

    private void uploadUserPublicData(String userName, String userEmail, String userPublicKey) {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            runGetDataTask(userName, userEmail, userPublicKey);
        } else {
            makeSnackbar(getWindow().getDecorView().getRootView(), "ERROR : Registration failed due to network connection unavailability!");
        }
    }

    private void runGetDataTask(String userName, String userEmail, String userPublicKey) {
        GetDataTaskRunner getDataTaskRunner = new GetDataTaskRunner();
        getDataTaskRunner.execute(userName, userEmail, userPublicKey);
    }

    private void setSharedPreferencesData(String userName, String userEmail, String userPublicKey, String userPrivateKey){
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
        String data = "";
        try {
            File file = new File(path);
            FileInputStream fileInputStream = new FileInputStream(file);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
            while ((line = bufferedReader.readLine()) != null) {
                data += line + "\n";
            }
            bufferedReader.close();
        }
        catch (IOException e) {e.printStackTrace();}
        return data;
    }

    private boolean validateForm(View view) {
        boolean setError = false;
        if (checkEmptySetError(nameField)) {
            setError = true;
        }
        if (checkEmptySetError(emailField) || checkEmailSetError(emailField)) {
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
        else if (pickedPublicKeyFile && pickedPrivateKeyFile) {
            if (!checkKeyBelongsToSameUser()) {
                publicKeyButton.setTextColor(red);
                privateKeyButton.setTextColor(red);
                makeSnackbar(view, "Public and private keys don't belong to same keypair!");
                setError = true;
            }
        }
        makeAlertDialog("", checkEmailBelongsToKey());
        return setError;
    }

    private String checkEmailBelongsToKey() {
        try {
            PGPPublicKey testPublicKey = PGPClass.getPublicKeyFromString(getKeyFromFile(publicKeyFilePath));
            StringBuilder stringBuilder = new StringBuilder();
            Iterator<String> iterator = testPublicKey.getUserIDs();
            while (iterator.hasNext()) {
                stringBuilder.append(iterator.next());
                stringBuilder.append("\n");
            }
            stringBuilder.delete(0, stringBuilder.indexOf("<"));
            stringBuilder.deleteCharAt(0);
            stringBuilder.setLength(stringBuilder.length() - 2);
            return(stringBuilder.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private boolean checkKeyBelongsToSameUser() {
        try {
            PGPPublicKey testPublicKey = PGPClass.getPublicKeyFromString(getKeyFromFile(publicKeyFilePath));
            String publicKeyId = String.valueOf(testPublicKey.getKeyID());
            PGPSecretKey testPrivateKey = PGPClass.getSecretKeyFromString(getKeyFromFile(privateKeyFilePath));
            String privateKeyId = String.valueOf(testPrivateKey.getKeyID());
            if (publicKeyId.equals(privateKeyId)) return true;
            else return false;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return true;
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
                makeSnackbar(getWindow().getDecorView().getRootView(), "Allow external storage reading");
            }
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSIONS_REQUEST_CODE);
        } else {
            openFilePicker(FILE_PICKER_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openFilePicker(FILE_PICKER_REQUEST_CODE);
                } else {
                    makeSnackbar(getWindow().getDecorView().getRootView(), "Cannot continue without storage access");
                }
            }
        }
    }

    private void openFilePicker(int FILE_PICKER_REQUEST_CODE) {
        new MaterialFilePicker()
                .withActivity(this)
                .withRequestCode(FILE_PICKER_REQUEST_CODE)
                .withFilter(Pattern.compile("(.*\\.asc$)"))
                .withHiddenFiles(true)
                .start();
    }

    private void makeAlertDialog(String title, String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.show();
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
                        publicKeyFilePath = null;
                        publicKeyButton.setText(R.string.public_key_hint);
                        publicKeyButton.setTextColor(white);
                        pickedPublicKeyFile = false;
                        try{
                            PGPPublicKey test = PGPClass.getPublicKeyFromString(getKeyFromFile(fullPath));
                            String keyId = String.valueOf(test.getKeyID());
                        }
                        catch (Exception e) {
                            makeAlertDialog("Error", "The selected file is not a valid PGP public key file. Please select a valid file.");
                            break;
                        }
                        publicKeyFilePath = fullPath;
                        String buttonText = "Picked file " + filename;
                        publicKeyButton.setText(buttonText);
                        publicKeyButton.setTextColor(white);
                        pickedPublicKeyFile = true;
                        break;
                    }
                    case 2: {
                        privateKeyFilePath = null;
                        privateKeyButton.setText(R.string.private_key_hint);
                        privateKeyButton.setTextColor(white);
                        pickedPrivateKeyFile = false;
                        try{
                            PGPSecretKey test = PGPClass.getSecretKeyFromString(getKeyFromFile(fullPath));
                            String keyId = String.valueOf(test.getKeyID());
                        }
                        catch (Exception e) {
                            makeAlertDialog("Error", "The selected file is not a valid PGP private key file. Please select a valid file.");
                            break;
                        }
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
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
        }
        return true;
    }

    private void getDataTaskComplete(int getDataTaskResult, String userName, String userEmail, String userPublicKey) {
        boolean proceed = false;
        switch (getDataTaskResult) {
            case 0: {
                if (!getDataTaskOutput.equals("")) {
                        makeSnackbar(getWindow().getDecorView().getRootView(), "Email address already in-use!");
                        emailField.setError("Please use a different email address");
                }
                else {
                    makeSnackbar(getWindow().getDecorView().getRootView(), "Verified! Proceeding with registration");
                    proceed = true;
                }
                break;
            }
            case 1: makeSnackbar(getWindow().getDecorView().getRootView(), "ERROR : Connection timed out!");
                    break;
            case 2: makeSnackbar(getWindow().getDecorView().getRootView(), "ERROR : Unable to retrieve data!");
                    break;
            case 3: makeSnackbar(getWindow().getDecorView().getRootView(), "ERROR : Unable to retrieve data!");
                    break;
            default: makeSnackbar(getWindow().getDecorView().getRootView(), "ERROR!");
                    break;
        }
        if (proceed) runPostDataTask(userName, userEmail, userPublicKey);
    }

    public void hideSoftKeyboard(){
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private class GetDataTaskRunner extends AsyncTask<String, Void, Integer> {
        String userName;
        String userEmail;
        String userPublicKey;
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(RegisterActivity.this, ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage("Verifying user");
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();
            super.onPreExecute();
        }
        @Override
        protected Integer doInBackground(String...params) {
            userName = params[0];
            userEmail = params[1];
            userPublicKey = params[2];
            InputStream inputStream = null;
            try {
                String getUrl = "http://192.168.55.157:3000/email/userDetails" + "/" + userEmail;
                URL url = new URL(getUrl);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setConnectTimeout(7000);
                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setRequestProperty("Accept", "application/json");
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();
                inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                String line;
                String buffer = "";
                while ((line = bufferedReader.readLine()) != null) {
                    buffer += line;
                    buffer += "\n";
                }
                httpURLConnection.disconnect();
                bufferedReader.close();
                getDataTaskOutput = buffer;
                return 0;
            } catch (SocketTimeoutException e) {
                return 1;
            } catch (IOException e) {
                return 2;
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    }
                    catch (IOException e) {
                        return 3;
                    }
                }
            }
        }
        @Override
        protected void onPostExecute(Integer result) {
            progressDialog.dismiss();
            getDataTaskComplete(result, userName, userEmail, userPublicKey);
        }
    }

    private void runPostDataTask(String userName, String userEmail, String userPublicKey) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", userName);
            jsonObject.put("email", userEmail);
            jsonObject.put("publickey", userPublicKey);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        PostDataTaskRunner postDataTaskRunner = new PostDataTaskRunner();
        postDataTaskRunner.execute(String.valueOf(jsonObject));
    }

    private class PostDataTaskRunner extends AsyncTask<String, Void, Integer> {
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(RegisterActivity.this, ProgressDialog.STYLE_SPINNER);
            progressDialog.setTitle(R.string.app_name);
            progressDialog.setMessage("Registering user");
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();
            super.onPreExecute();
        }
        @Override
        protected Integer doInBackground(String...params) {
            String jsonData = params[0];
            InputStream inputStream = null;
            try {
                String postUrl = "http://192.168.55.157:3000/userDetails";
                URL url = new URL(postUrl);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setConnectTimeout(7000);
                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setRequestProperty("Content-type", "application/json");
                httpURLConnection.setRequestProperty("Accept", "application/json");
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);
                Writer writer = new BufferedWriter(new OutputStreamWriter(httpURLConnection.getOutputStream(), "UTF-8"));
                writer.write(jsonData);
                writer.close();
                inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                String line;
                String buffer = "";
                while ((line = bufferedReader.readLine()) != null) {
                    buffer += line;
                    buffer += "\n";
                }
                httpURLConnection.disconnect();
                bufferedReader.close();
                postDataTaskOutput = buffer;
                System.out.println(buffer);
                return 0;
            } catch (SocketTimeoutException e) {
                return 1;
            } catch (IOException e) {
                return 2;
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    }
                    catch (IOException e) {
                        return 3;
                    }
                }
            }
        }
        @Override
        protected void onPostExecute(Integer result) {
            progressDialog.dismiss();
            postDataTaskComplete(result);
        }
    }

    private void postDataTaskComplete(int postDataTaskResult){
        switch (postDataTaskResult) {
            case 0: {
                if (postDataTaskOutput.equals("")) {
                    makeSnackbar(getWindow().getDecorView().getRootView(), "ERROR");
                }
                else {
                    setSharedPreferencesData(globalName, globalEmail, globalPublicKey, globalPrivateKey);
                    final Intent intent = new Intent(this, MainActivity.class);
                    AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this)
                            .setTitle("Success")
                            .setMessage("You have been successfully registered! You can access all of this application's functionality. Press OK to continue.")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    startActivity(intent);
                                    finish();
                                }
                            });
                    builder.show();
                    makeSnackbar(getWindow().getDecorView().getRootView(), "Registration successful!");
                }
                break;
            }
            case 1: makeSnackbar(getWindow().getDecorView().getRootView(), "ERROR : Connection timed out!");
                break;
            case 2: makeSnackbar(getWindow().getDecorView().getRootView(), "ERROR : Unable to retrieve data!");
                break;
            case 3: makeSnackbar(getWindow().getDecorView().getRootView(), "ERROR : Unable to retrieve data!");
                break;
            default: makeSnackbar(getWindow().getDecorView().getRootView(), "ERROR!");
                break;
        }
    }
}
