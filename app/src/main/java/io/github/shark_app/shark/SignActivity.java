package io.github.shark_app.shark;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SignActivity extends AppCompatActivity {
    @BindView(R.id.scannedNameField) TextView scannedNameField;
    @BindView(R.id.scannedEmailField) TextView scannedEmailField;
    @BindView(R.id.scannedPublicKeyField) TextView scannedPublicKeyField;
    @BindView(R.id.signButton) Button signButton;

    private ProgressDialog progressDialog;
    private String getDataTaskOutput;
    private String scannedUserName;
    private String scannedUserEmail;
    private String scannedUserPublicKey;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign);
        ButterKnife.bind(this);
        Intent intent = getIntent();
        String scannedUserEmail = intent.getStringExtra("qrCodeValue");
        getScannedUserPublicData(scannedUserEmail);
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
        String userName;
        String userEmail;
        String userPublicKey;
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
        boolean proceed = false;
        switch (getDataTaskResult) {
            case 0: {
                try {
                    JSONObject jsonObject = new JSONObject(getDataTaskOutput);
                    scannedUserEmail = jsonObject.getString("email");
                    scannedUserName = jsonObject.getString("name");
                    scannedUserPublicKey = jsonObject.getString("publickey");
                    scannedEmailField.setText(scannedUserEmail);
                    scannedNameField.setText(scannedUserName);
                    scannedPublicKeyField.setText(scannedUserPublicKey);
                }
                catch (Exception e) {}
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
        if (proceed) makeSnackbar("TIME TO SIGN!!!");
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, ScanActivity.class);
        startActivity(intent);
        finish();
    }
}
