package com.limelight.grid;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.limelight.PcView;
import com.limelight.R;
import com.limelight.computers.model.HostConnectionState;
import com.limelight.computers.model.HostEndpoint;
import com.limelight.computers.model.HostRuntimeSnapshot;

import java.util.Collections;

public class PcGridAdapter extends GenericGridAdapter<PcView.ComputerObject> {
    public PcGridAdapter(Context context) {
        super(context, R.layout.pc_grid_item_new);
    }

    public void addComputer(PcView.ComputerObject computer) {
        itemList.add(computer);
        sortList();
    }

    private void sortList() {
        Collections.sort(
                itemList,
                (lhs, rhs) -> displayName(lhs.getSnapshot())
                        .compareToIgnoreCase(
                                displayName(rhs.getSnapshot())));
    }

    public boolean removeComputer(PcView.ComputerObject computer) {
        return itemList.remove(computer);
    }

    @Override
    public void populateView(
            View parentView,
            ImageView imgView,
            ProgressBar prgView,
            TextView txtView,
            ImageView overlayView,
            PcView.ComputerObject obj) {
        HostRuntimeSnapshot snapshot = obj.getSnapshot();
        HostConnectionState connection = snapshot.getConnectionState();
        imgView.setImageResource(R.drawable.ic_computer);
        if (connection.getReachability() ==
                HostConnectionState.Reachability.ONLINE) {
            imgView.setAlpha(1.0f);
        }
        else {
            imgView.setAlpha(0.4f);
        }

        if (connection.getReachability() ==
                HostConnectionState.Reachability.UNKNOWN) {
            prgView.setVisibility(View.VISIBLE);
        }
        else {
            prgView.setVisibility(View.INVISIBLE);
        }

        txtView.setText(displayName(snapshot));
        if (connection.getReachability() ==
                HostConnectionState.Reachability.ONLINE) {
            txtView.setAlpha(1.0f);
        }
        else {
            txtView.setAlpha(0.4f);
        }

        if (connection.getReachability() ==
                HostConnectionState.Reachability.OFFLINE) {
            overlayView.setImageResource(R.drawable.ic_pc_offline);
            overlayView.setAlpha(0.4f);
            overlayView.setVisibility(View.VISIBLE);
        }
        // We must check if the status is exactly online and unpaired
        // to avoid colliding with the loading spinner when status is unknown
        else if (connection.getReachability() ==
                        HostConnectionState.Reachability.ONLINE &&
                connection.getPairingStatus() ==
                        HostConnectionState.PairingStatus.NOT_PAIRED) {
            overlayView.setImageResource(R.drawable.ic_lock);
            overlayView.setAlpha(1.0f);
            overlayView.setVisibility(View.VISIBLE);
        }
        else if (connection.getReachability() ==
                        HostConnectionState.Reachability.ONLINE &&
                connection.getPairingStatus() ==
                        HostConnectionState.PairingStatus.PAIRED) {
            overlayView.setImageResource(R.drawable.ic_play);
            overlayView.setAlpha(1.0f);
            overlayView.setVisibility(View.VISIBLE);
        }
        else {
            overlayView.setVisibility(View.GONE);
        }

        TextView txIp = parentView.findViewById(R.id.tx_item_ip);
        txIp.setVisibility(View.VISIBLE);
        if (connection.getReachability() ==
                HostConnectionState.Reachability.ONLINE) {
            txIp.setAlpha(1.0f);
        }
        else {
            txIp.setAlpha(0.4f);
        }
        HostEndpoint endpoint = firstDisplayEndpoint(snapshot);
        if (endpoint != null) {
            txIp.setText(endpoint.getAddress());
            return;
        }
        txIp.setVisibility(View.GONE);
    }

    private static String displayName(HostRuntimeSnapshot snapshot) {
        return snapshot.getRecord().getIdentity().getDisplayName();
    }

    private static HostEndpoint firstDisplayEndpoint(
            HostRuntimeSnapshot snapshot) {
        HostEndpoint endpoint = snapshot.getRecord().getEndpoint(
                HostEndpoint.Kind.LOCAL_IPV4);
        if (endpoint == null) {
            endpoint = snapshot.getRecord().getEndpoint(
                    HostEndpoint.Kind.LOCAL_IPV6);
        }
        if (endpoint == null) {
            endpoint = snapshot.getRecord().getEndpoint(
                    HostEndpoint.Kind.REMOTE);
        }
        if (endpoint == null) {
            endpoint = snapshot.getRecord().getEndpoint(
                    HostEndpoint.Kind.MANUAL);
        }
        return endpoint;
    }
}
