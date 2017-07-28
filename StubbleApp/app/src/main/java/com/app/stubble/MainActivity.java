package com.app.stubble;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import com.app.stubble.utils.CrashLogHandler;

public class MainActivity extends BaseActivity {

    ImageView mTestIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CrashLogHandler.getInstance().init(getApplicationContext());

        mTestIv = (ImageView) findViewById(R.id.main_test_iv);

        Intent intent = new Intent(MainActivity.this, FloatWindowService.class);
        startService(intent);
        finish();
    }
}
