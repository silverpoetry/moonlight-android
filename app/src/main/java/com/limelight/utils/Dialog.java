package com.limelight.utils;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.limelight.R;

public class Dialog implements Runnable {
    private final String title;
    private final String message;
    private final Activity activity;
    private final Runnable runOnDismiss;

    private AlertDialog alert;

    private static final ArrayList<Dialog> rundownDialogs = new ArrayList<>();

    private Dialog(Activity activity, String title, String message, Runnable runOnDismiss)
    {
        this.activity = activity;
        this.title = title;
        this.message = message;
        this.runOnDismiss = runOnDismiss;
    }

    public static void closeDialogs()
    {
        synchronized (rundownDialogs) {
            for (Dialog d : rundownDialogs) {
                if (d.alert.isShowing()) {
                    d.alert.dismiss();
                }
            }

            rundownDialogs.clear();
        }
    }

    public static void displayDialog(final Activity activity, String title, String message, final boolean endAfterDismiss)
    {
        activity.runOnUiThread(new Dialog(activity, title, message, new Runnable() {
            @Override
            public void run() {
                if (endAfterDismiss) {
                    activity.finish();
                }
            }
        }));
    }

    public static void displayDialog(Activity activity, String title, String message, Runnable runOnDismiss)
    {
        activity.runOnUiThread(new Dialog(activity, title, message, runOnDismiss));
    }

    @Override
    public void run() {
        // If we're dying, don't bother creating a dialog
        if (activity.isFinishing())
            return;

        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_message_prompt, null, false);
        TextView titleView = dialogView.findViewById(R.id.tv_dialog_title);
        TextView messageView = dialogView.findViewById(R.id.tv_dialog_message);
        TextView okButton = dialogView.findViewById(R.id.btn_dialog_ok);
        TextView helpButton = dialogView.findViewById(R.id.btn_dialog_help);

        titleView.setText(title);
        messageView.setText(message);
        okButton.setText(activity.getResources().getText(android.R.string.ok));
        helpButton.setText(activity.getResources().getText(R.string.help));

        alert = new AlertDialog.Builder(activity)
                .setView(dialogView)
                .create();
        alert.setCancelable(false);
        alert.setCanceledOnTouchOutside(false);

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                synchronized (rundownDialogs) {
                    rundownDialogs.remove(Dialog.this);
                    alert.dismiss();
                }

                runOnDismiss.run();
            }
        });
        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                synchronized (rundownDialogs) {
                    rundownDialogs.remove(Dialog.this);
                    alert.dismiss();
                }

                runOnDismiss.run();

                HelpLauncher.launchTroubleshooting(activity);
            }
        });

        synchronized (rundownDialogs) {
            rundownDialogs.add(this);
            alert.show();
            Window window = alert.getWindow();
            if (window != null) {
                window.setBackgroundDrawableResource(android.R.color.transparent);
            }
            okButton.setFocusable(true);
            okButton.setFocusableInTouchMode(true);
            okButton.requestFocus();
        }
    }

}
