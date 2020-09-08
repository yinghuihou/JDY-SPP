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

    public static byte[] hex2byte(byte[] b) {
        if(b.length % 2 != 0) {
            throw new IllegalArgumentException("长度不是偶数");
        } else {
            byte[] b2 = new byte[b.length / 2];

            for(int n = 0; n < b.length; n += 2) {
                String item = new String(b, n, 2);
                b2[n / 2] = (byte)Integer.parseInt(item, 16);
            }

            Object b1 = null;
            return b2;
        }
    }
}
