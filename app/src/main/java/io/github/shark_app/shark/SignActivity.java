package io.github.shark_app.shark;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;
import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.bcpg.BCPGOutputStream;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketVector;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.jcajce.JcaPGPPublicKeyRingCollection;
import org.spongycastle.openpgp.jcajce.JcaPGPSecretKeyRingCollection;
import org.spongycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.Security;
import java.util.Iterator;

import butterknife.BindView;
import butterknife.ButterKnife;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign);
        ButterKnife.bind(this);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        currentUserPrivateKey = settings.getString(PREFS_USER_PRIVATE_KEY, null);
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
                    proceed = true;
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
        if (proceed) {
            try {
                PGPPublicKey keyToBeSigned = getPublicKeyFromString(scannedUserPublicKey);
                PGPSecretKey secretKey = getSecretKeyFromString(currentUserPrivateKey);
                PGPPublicKeyRing signedRing = new PGPPublicKeyRing(
                        new ByteArrayInputStream(signPublicKey(secretKey, "sahil", keyToBeSigned, "TEST", "true", true)), new JcaKeyFingerprintCalculator());
                File sdcard = Environment.getExternalStorageDirectory();
                File directory = new File(sdcard.getAbsolutePath() + "/Signed_Keys/");
                directory.mkdir();
                String filename = scannedUserName + ".asc";
                File file = new File(directory, filename);
                ArmoredOutputStream armoredOutputStream = new ArmoredOutputStream(new FileOutputStream(file));
                signedRing.encode(armoredOutputStream);
                armoredOutputStream.flush();
                armoredOutputStream.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static PGPPublicKey getPublicKeyFromString(String str) throws Exception {
        InputStream inputStream = new ByteArrayInputStream(str.getBytes());
        inputStream = org.spongycastle.openpgp.PGPUtil.getDecoderStream(inputStream);
        JcaPGPPublicKeyRingCollection jcaPGPPublicKeyRingCollection = new JcaPGPPublicKeyRingCollection(inputStream);
        inputStream.close();
        PGPPublicKey pgpPublicKey = null;
        Iterator<PGPPublicKeyRing> iterator = jcaPGPPublicKeyRingCollection.getKeyRings();
        while (pgpPublicKey == null && iterator.hasNext()) {
            PGPPublicKeyRing kRing = iterator.next();
            Iterator<PGPPublicKey> iteratorKey = kRing.getPublicKeys();
            while (pgpPublicKey == null && iteratorKey.hasNext()) {
                pgpPublicKey = iteratorKey.next();
            }
        }
        return pgpPublicKey;
    }

    public static PGPSecretKey getSecretKeyFromString(String str) throws Exception{
        InputStream inputStream = new ByteArrayInputStream(str.getBytes());
        inputStream = org.spongycastle.openpgp.PGPUtil.getDecoderStream(inputStream);
        JcaPGPSecretKeyRingCollection jcaPGPSecretKeyRingCollection = new JcaPGPSecretKeyRingCollection(inputStream);
        inputStream.close();
        PGPSecretKey pgpSecretKey = null;
        Iterator<PGPSecretKeyRing> iterator = jcaPGPSecretKeyRingCollection.getKeyRings();
        while (pgpSecretKey == null && iterator.hasNext()) {
            PGPSecretKeyRing kRing = iterator.next();
            Iterator<PGPSecretKey> iteratorKey = kRing.getSecretKeys();
            while (pgpSecretKey == null && iteratorKey.hasNext()) {
                pgpSecretKey = iteratorKey.next();
            }
        }
        return pgpSecretKey;
    }

    private static byte[] signPublicKey(PGPSecretKey secretKey, String secretKeyPass, PGPPublicKey keyToBeSigned, String notationName, String notationValue, boolean armor) throws Exception {
        OutputStream outputStream = new ByteArrayOutputStream();
        if (armor) {
            outputStream = new ArmoredOutputStream(outputStream);
        }
        PGPPrivateKey pgpPrivateKey = secretKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("SC").build(secretKeyPass.toCharArray()));
        PGPSignatureGenerator pgpSignatureGenerator = new PGPSignatureGenerator(new JcaPGPContentSignerBuilder(secretKey.getPublicKey().getAlgorithm(), PGPUtil.SHA1).setProvider("SC"));
        pgpSignatureGenerator.init(PGPSignature.DIRECT_KEY, pgpPrivateKey);
        BCPGOutputStream bcpgOutputStream = new BCPGOutputStream(outputStream);
        pgpSignatureGenerator.generateOnePassVersion(false).encode(bcpgOutputStream);
        PGPSignatureSubpacketGenerator pgpSignatureSubpacketGenerator = new PGPSignatureSubpacketGenerator();
        pgpSignatureSubpacketGenerator.setNotationData(true, true, notationName, notationValue);
        PGPSignatureSubpacketVector packetVector = pgpSignatureSubpacketGenerator.generate();
        pgpSignatureGenerator.setHashedSubpackets(packetVector);
        bcpgOutputStream.flush();
        if (armor) {
            outputStream.close();
        }
        return PGPPublicKey.addCertification(keyToBeSigned, pgpSignatureGenerator.generate()).getEncoded();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, ScanActivity.class);
        startActivity(intent);
        finish();
    }
}
