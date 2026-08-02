package com.limelight.ui.filepush.android;

import android.content.Context;

import com.limelight.computers.ComputerDatabaseManager;
import com.limelight.computers.model.PersistedHost;
import com.limelight.ui.filepush.FilePushController;
import com.limelight.ui.filepush.FilePushTarget;
import com.limelight.ui.filepush.FilePushTargetFactory;
import com.limelight.ui.filepush.FilePushTargetSelection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** SQLite-backed host catalog for the desktop-file push screen. */
public final class AndroidFilePushHostCatalog
        implements FilePushController.HostCatalog {
    private final Context context;

    public AndroidFilePushHostCatalog(Context context) {
        this.context = Objects.requireNonNull(
                context,
                "context").getApplicationContext();
    }

    @Override
    public List<FilePushTarget> loadTargets() {
        List<FilePushTarget> targets = new ArrayList<>();
        ComputerDatabaseManager database =
                new ComputerDatabaseManager(context);
        try {
            for (PersistedHost host : database.getAllHosts()) {
                FilePushTargetSelection target =
                        FilePushTargetFactory.create(host);
                if (target.isFound()) {
                    targets.add(target.getTarget());
                }
            }
        }
        finally {
            database.close();
        }
        Collections.sort(
                targets,
                (left, right) -> left.getDisplayName()
                        .compareToIgnoreCase(right.getDisplayName()));
        return targets;
    }
}
