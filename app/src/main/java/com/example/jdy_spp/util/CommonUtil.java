package com.example.jdy_spp.util;

import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

public class CommonUtil {

    private static Context mContext;

    public static void init(Context context) {
        mContext = context;
    }

    public static void showToast(String text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }

        int len = text.length();
        if (len < 20) {
            Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
        }
    }
}
