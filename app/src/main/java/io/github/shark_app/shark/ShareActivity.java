package io.github.shark_app.shark;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class ShareActivity extends AppCompatActivity {
    public static final String PREFS_NAME = "SHARK_PREFS";
    public static final String PREFS_USER_EXISTS_KEY = "userExists";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        boolean userExists = checkIfUserExists();
        if (!userExists) {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        }
    }

    private boolean checkIfUserExists(){
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean userExists = settings.getBoolean(PREFS_USER_EXISTS_KEY, false);
        return userExists;
    }
}
