package com.app.stubble;

import android.os.Bundle;
import android.widget.TextView;

public class PaintActivity extends BaseActivity {

    private TextView mTv;
    private MyView mMyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paint);

        mTv = (TextView) findViewById(R.id.location_tv);
        mMyView = (MyView) findViewById(R.id.rectangle_view_id);

        final StringBuilder sb = new StringBuilder();
        mMyView.setOnScreenShotListener(new MyView.OnScreenShotListener() {
            @Override
            public void onScreenShot(float left, float top, float right, float bottom) {
                sb.delete(0, sb.length());

                sb.append("left=").append(left).append("\n")
                        .append("top=").append(top).append("\n")
                        .append("right=").append(right).append("\n")
                        .append("bottom=").append(bottom);

                mTv.setText(sb.toString());
            }
        });
    }
}
