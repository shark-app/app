package io.github.shark_app.shark;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;

import com.google.android.gms.samples.vision.barcodereader.BarcodeCapture;
import com.google.android.gms.samples.vision.barcodereader.BarcodeGraphic;
import com.google.android.gms.vision.barcode.Barcode;

import java.util.List;

import xyz.belvi.mobilevisionbarcodescanner.BarcodeRetriever;

import static io.github.shark_app.shark.R.id.barcode;

public class ScanActivity extends AppCompatActivity implements BarcodeRetriever {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        instantiateBarcodeScanner();
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
        Intent intent = new Intent(this, SignActivity.class);
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

    public void instantiateBarcodeScanner(){
        final BarcodeCapture barcodeCapture = (BarcodeCapture) getSupportFragmentManager().findFragmentById(barcode);
        barcodeCapture.setRetrieval(this);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
