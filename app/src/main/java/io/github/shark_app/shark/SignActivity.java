package io.github.shark_app.shark;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.Security;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static io.github.shark_app.shark.MainActivity.PREFS_NAME;
import static io.github.shark_app.shark.MainActivity.PREFS_USER_PRIVATE_KEY;

public class SignActivity extends AppCompatActivity {
    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    @BindView(R.id.scannedNameField) TextView scannedNameField;
    @BindView(R.id.scannedEmailField) TextView scannedEmailField;
    @BindView(R.id.scannedPublicKeyField) TextView scannedPublicKeyField;
    @BindView(R.id.signButton) Button signButton;

    private ProgressDialog progressDialog;
    private String getDataTaskOutput;
    private String scannedUserName;
    private String scannedUserEmail;
    private String scannedUserPublicKey;
    private String currentUserPrivateKey;
    private String putDataTaskOutput;
    private String scannedUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign);
        ButterKnife.bind(this);
        signButton.setEnabled(false);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        currentUserPrivateKey = settings.getString(PREFS_USER_PRIVATE_KEY, null);
        Intent intent = getIntent();
        String scannedUserEmail = intent.getStringExtra("qrCodeValue");
        getScannedUserPublicData(scannedUserEmail);
    }

    @OnClick(R.id.signButton)
    public void signKey(View view) {
        final Dialog passphraseDialog = new Dialog(this);
            passphraseDialog.setContentView(R.layout.dialog_passphrase);
            Button buttonOk = (Button) passphraseDialog.findViewById(R.id.ok);
            Button buttonCancel = (Button) passphraseDialog.findViewById(R.id.cancel);
            final EditText passphraseText = (EditText) passphraseDialog.findViewById(R.id.passphrase);
            buttonOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideSoftKeyboard();
                    String passphrase = passphraseText.getText().toString().trim();
                    if(passphrase.length() > 0) {
                        passphraseDialog.dismiss();
                        startSigningProcedure(passphrase);
                    }
                    else {
                        makeSnackbar("Please enter passphrase!");
                    }
                }
            });
            buttonCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideSoftKeyboard();
                    passphraseDialog.dismiss();
                }
            });
            passphraseDialog.show();

    }

    private void startSigningProcedure(String passphrase) {
        boolean signingDone = false;
        try {
            progressDialog = new ProgressDialog(SignActivity.this, ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage("Signing key");
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();
            PGPPublicKey keyToBeSigned = PGPClass.getPublicKeyFromString(scannedUserPublicKey);
            PGPSecretKey secretKey = PGPClass.getSecretKeyFromString(currentUserPrivateKey);
            PGPPublicKeyRing signedRing = new PGPPublicKeyRing(
                    new ByteArrayInputStream(PGPClass.signPublicKey(secretKey, passphrase, keyToBeSigned, "TEST", "true", true)), new JcaKeyFingerprintCalculator());
            File sdcard = Environment.getExternalStorageDirectory();
            File directory = new File(sdcard.getAbsolutePath() + "/Signed_Keys/");
            directory.mkdir();
            String filename = scannedUserName + ".asc";
            File file = new File(directory, filename);
            ArmoredOutputStream armoredOutputStream = new ArmoredOutputStream(new FileOutputStream(file));
            signedRing.encode(armoredOutputStream);
            armoredOutputStream.flush();
            armoredOutputStream.close();
            signingDone = true;
            progressDialog.dismiss();
        }
        catch (PGPException p) {
            progressDialog.dismiss();
            makeAlertDialog("PGP error", "The application encountered an error. Please check the private key passphrase that was entered. Press OK to scan again.");
        }
        catch (Exception e) {
            progressDialog.dismiss();
            e.printStackTrace();
        }
        if (signingDone == true) {
            File sdcard = Environment.getExternalStorageDirectory();
            File directory = new File(sdcard.getAbsolutePath() + "/Signed_Keys/");
            String filename = scannedUserName + ".asc";
            File file = new File(directory, filename);
            String path = file.getAbsolutePath();
            final String scannedUserSignedPublicKey = getKeyFromFile(path);
            AlertDialog.Builder builder = new AlertDialog.Builder(SignActivity.this)
                    .setTitle("Success!")
                    .setMessage("The key signing procedure has been completed. Press OK to upload the signed key of user " + scannedUserName + " to our servers.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            putScannedUserPublicData(scannedUserSignedPublicKey);
                        }
                    });
            builder.show();
        }
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

    private void putScannedUserPublicData(String scannedUserSignedPublicKey) {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            runPutDataTask(scannedUserSignedPublicKey);
        } else {
            makeAlertDialog("ERROR", "Data transmission to server failed due to network connection unavailability");
        }
    }

    private void runPutDataTask(String scannedUserSignedPublicKey) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", scannedUserName);
            jsonObject.put("email", scannedUserEmail);
            jsonObject.put("publickey", scannedUserSignedPublicKey);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        PutDataTaskRunner postAgainDataTaskRunner = new PutDataTaskRunner();
        postAgainDataTaskRunner.execute(String.valueOf(jsonObject));
    }

    private class PutDataTaskRunner extends AsyncTask<String, Void, Integer> {
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(SignActivity.this, ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage("Posting data to server");
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
                String postUrl = "http://192.168.55.157:3000/userDetails/" + scannedUserId;
                URL url = new URL(postUrl);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setConnectTimeout(7000);
                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setRequestMethod("PUT");
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
                putDataTaskOutput = buffer;
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
            putDataTaskComplete(result);
        }
    }

    private void putDataTaskComplete(int postDataTaskResult){
        switch (postDataTaskResult) {
            case 0: {
                if (putDataTaskOutput.equals("")) {
                    makeAlertDialog("Error", "No data has been posted.");
                }
                else {
                    final Intent intent = new Intent(this, MainActivity.class);
                    AlertDialog.Builder builder = new AlertDialog.Builder(SignActivity.this)
                            .setTitle("Success!")
                            .setMessage("User " + scannedUserName + "\'s data has been sent to server. Press OK to continue.")
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
                break;
            }
            case 1: makeAlertDialog("Error", "Connection timed out!");
                break;
            case 2: makeAlertDialog("Error", "Unable to retrieve data!");
                break;
            case 3: makeAlertDialog("Error", "Unable to retrieve data!");
                break;
            default: makeAlertDialog("ERROR", "");
                break;
        }
    }

    private void getScannedUserPublicData(String email) {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            runGetDataTask(email);
        } else {
            makeSnackbar("ERROR : Retrieval failed due to network connection unavailability!");
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

    private void runGetDataTask(String email) {
        GetDataTaskRunner getDataTaskRunner = new GetDataTaskRunner();
        getDataTaskRunner.execute(email);
    }

    private class GetDataTaskRunner extends AsyncTask<String, Void, Integer> {
        String userEmail;
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(SignActivity.this, ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage("Verifying user");
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();
            super.onPreExecute();
        }
        @Override
        protected Integer doInBackground(String...params) {
            userEmail = params[0];
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
            getDataTaskComplete(result);
        }
    }

    private void getDataTaskComplete(int getDataTaskResult) {
        switch (getDataTaskResult) {
            case 0: {
                try {
                    JSONObject jsonObject = new JSONObject(getDataTaskOutput);
                    scannedUserEmail = jsonObject.getString("email");
                    scannedUserName = jsonObject.getString("name");
                    scannedUserPublicKey = jsonObject.getString("publickey");
                    scannedUserId = jsonObject.getString("_id");
                    scannedEmailField.setText(scannedUserEmail);
                    scannedNameField.setText(scannedUserName);
                    PGPPublicKey pgpPublicKey = PGPClass.getPublicKeyFromString(scannedUserPublicKey);
                    scannedPublicKeyField.setText(String.valueOf(pgpPublicKey.getKeyID()));
                    signButton.setEnabled(true);
                }
                catch (PGPException p) {
                    makeAlertDialog("PGP error", "The application encountered an error. The public key returned by scanning is not valid. Press OK to scan again.");
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
            case 1: makeSnackbar("ERROR : Connection timed out!");
                break;
            case 2: makeSnackbar("ERROR : Unable to retrieve data!");
                break;
            case 3: makeSnackbar("ERROR : Unable to retrieve data!");
                break;
            default: makeSnackbar("ERROR!");
                break;
        }
    }

    private void makeAlertDialog(String title, String message) {
        final Intent intent = new Intent(this, ScanActivity.class);
        AlertDialog.Builder builder = new AlertDialog.Builder(SignActivity.this)
                .setTitle(title)
                .setMessage(message)
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

    public void hideSoftKeyboard(){
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, ScanActivity.class);
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
}
