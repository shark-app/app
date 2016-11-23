package io.github.shark_app.shark;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import net.glxn.qrgen.android.QRCode;
import net.glxn.qrgen.core.image.ImageType;

import butterknife.BindView;
import butterknife.ButterKnife;

import static io.github.shark_app.shark.MainActivity.PREFS_NAME;
import static io.github.shark_app.shark.MainActivity.PREFS_USER_EMAIL;
import static io.github.shark_app.shark.MainActivity.PREFS_USER_EXISTS_KEY;

public class ShareActivity extends AppCompatActivity {
    @BindView(R.id.qrCodeImageView) ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        ButterKnife.bind(this);
        boolean userExists = checkIfUserExists();
        if (!userExists) {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        }
        else {
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String userEmail = settings.getString(PREFS_USER_EMAIL, null);
            Bitmap qrCodeBitmap = QRCode.from(userEmail).to(ImageType.JPG).withSize(512, 512).bitmap();
            imageView.setImageBitmap(qrCodeBitmap);
        }
    }

    private boolean checkIfUserExists(){
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean userExists = settings.getBoolean(PREFS_USER_EXISTS_KEY, false);
        return userExists;
    }
}
