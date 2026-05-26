package com.laleme.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        PoopStore store = new PoopStore(context);
        if (store.isReminderEnabled()) {
            ReminderScheduler.schedule(context, store.getReminderHour(), store.getReminderMinute());
        }
    }
}
