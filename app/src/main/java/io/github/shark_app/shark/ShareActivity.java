package io.github.shark_app.shark;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

import static io.github.shark_app.shark.MainActivity.PREFS_NAME;
import static io.github.shark_app.shark.MainActivity.PREFS_USER_EMAIL;
import static io.github.shark_app.shark.MainActivity.PREFS_USER_EXISTS_KEY;
import static io.github.shark_app.shark.MainActivity.PREFS_USER_NAME;
import static io.github.shark_app.shark.MainActivity.PREFS_USER_PRIVATE_KEY;
import static io.github.shark_app.shark.MainActivity.PREFS_USER_PUBLIC_KEY;

public class ShareActivity extends AppCompatActivity {
    @BindView(R.id.textView) TextView textView;

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
            String userName = settings.getString(PREFS_USER_NAME, null);
            String userEmail = settings.getString(PREFS_USER_EMAIL, null);
            String userPublicKey = settings.getString(PREFS_USER_PUBLIC_KEY, null);
            String userPrivateKey = settings.getString(PREFS_USER_PRIVATE_KEY, null);
            String text = userName + "\n"
                            + userEmail + "\n"
                            + userPublicKey + "\n"
                            + userPrivateKey + "\n";
            textView.setText(text);
        }
    }

    private boolean checkIfUserExists(){
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean userExists = settings.getBoolean(PREFS_USER_EXISTS_KEY, false);
        return userExists;
    }
}
