package io.github.shark_app.shark;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.samples.vision.barcodereader.BarcodeCapture;
import com.google.android.gms.samples.vision.barcodereader.BarcodeGraphic;
import com.google.android.gms.vision.barcode.Barcode;

import java.util.List;

import xyz.belvi.mobilevisionbarcodescanner.BarcodeRetriever;

import static io.github.shark_app.shark.R.id.barcode;

public class ScanActivity extends AppCompatActivity implements BarcodeRetriever {
    private static final String TAG = "CAMERA";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        BarcodeCapture barcodeCapture = (BarcodeCapture) getSupportFragmentManager().findFragmentById(barcode);
        barcodeCapture.setRetrieval(this);
        barcodeCapture.setShowDrawRect(true);
        barcodeCapture.setSupportMultipleScan(false);
        barcodeCapture.setTouchAsCallback(true);
        barcodeCapture.shouldAutoFocus(true);
        barcodeCapture.setShouldShowText(true);
    }

    @Override
    public void onRetrieved(final Barcode barcode) {
        Log.d(TAG, "Barcode read: " + barcode.displayValue);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(ScanActivity.this)
                        .setTitle("code retrieved")
                        .setMessage(barcode.displayValue);
                builder.show();
            }
        });
    }

    @Override
    public void onRetrievedFailed(final String reason) {
        Log.d(TAG, "Barcode reading failed : " + reason);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(ScanActivity.this)
                        .setTitle("Barcode failed")
                        .setMessage(reason);
                builder.show();
            }
        });
    }

    @Override
    public void onRetrievedMultiple(final Barcode closetToClick, final List<BarcodeGraphic> barcodeGraphics) {}

    @Override
    public void onBitmapScanned(SparseArray<Barcode> sparseArray) {}
}
