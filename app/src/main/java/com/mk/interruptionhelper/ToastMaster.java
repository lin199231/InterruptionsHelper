package com.mk.interruptionhelper;

import android.widget.Toast;

/**
 * Created by dhdev_000 on 2015/7/24.
 * use to manage Toast
 */
public class ToastMaster {

    private static Toast sToast = null;

    private ToastMaster() {

    }

    public static void setToast(Toast toast) {
        if (sToast != null)
            sToast.cancel();
        sToast = toast;
    }

    public static void cancelToast() {
        if (sToast != null)
            sToast.cancel();
        sToast = null;
    }
}
