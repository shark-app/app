package io.github.shark_app.shark;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.View;

import com.google.android.gms.samples.vision.barcodereader.BarcodeCapture;
import com.google.android.gms.samples.vision.barcodereader.BarcodeGraphic;
import com.google.android.gms.vision.barcode.Barcode;

import java.util.List;

import xyz.belvi.mobilevisionbarcodescanner.BarcodeRetriever;

import static io.github.shark_app.shark.R.id.barcode;

public class ScanActivity extends AppCompatActivity implements BarcodeRetriever {
    private static final String TAG = "CAMERA";
    private static final int PERMISSIONS_CAMERA = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        checkPermissions();
    }

    @Override
    public void onRetrieved(final Barcode barcode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(ScanActivity.this)
                        .setTitle("Code retrieved")
                        .setMessage(barcode.displayValue);
                builder.show();
            }
        });
        Intent intent = new Intent(SignActivity.class);
        intent.putExtra("qrCodeValue", barcode.displayValue);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRetrievedFailed(final String reason) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(ScanActivity.this)
                        .setTitle("Code scanning failed")
                        .setMessage(reason);
                builder.show();
            }
        });
    }

    @Override
    public void onRetrievedMultiple(final Barcode closetToClick, final List<BarcodeGraphic> barcodeGraphics) {}

    @Override
    public void onBitmapScanned(SparseArray<Barcode> sparseArray) {}

    private void makeSnackbar(String snackbarText) {
        Snackbar.make(getWindow().getDecorView().getRootView(), snackbarText, Snackbar.LENGTH_LONG)
                .setAction("Dismiss", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {}
                })
                .show();
    }

    private void checkPermissions() {
        String permission = Manifest.permission.CAMERA;
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                makeSnackbar("Allow camera access to scan QR code");
            }
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSIONS_CAMERA);
        } else {
            instantiateBarcodeScanner();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    instantiateBarcodeScanner();
                } else {
                    makeSnackbar("Cannot continue without access to camera");
                }
            }
        }
    }

    public void instantiateBarcodeScanner(){
        final BarcodeCapture barcodeCapture = (BarcodeCapture) getSupportFragmentManager().findFragmentById(barcode);
        barcodeCapture.setRetrieval(this);
    }
}
