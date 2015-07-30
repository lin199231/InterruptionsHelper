package com.mk.interruptionhelper;

import android.content.Context;

import com.mk.interruptionhelper.provider.Interruption;

/**
 * Created by dhdev_000 on 2015/7/30.
 */
public interface InterruptionHelperExtensions {
    /**
     * Notify paired device that a new interruption has been created on the phone, so that the interruption can be
     * synced to the device.
     *
     * @param context  the application context.
     * @param newInterruption the interruption to add.
     */
    public void addInterruption(Context context, Interruption newInterruption);

    /**
     * Notify paired device that an interruption has been deleted from the phone so that it can also be
     * deleted from the device.
     *
     * @param context the application context.
     * @param interruptionId the interruption id of the interruption to delete.
     */
    public void deleteInterruption(Context context, long interruptionId);
}
