package com.limelight.ui.gamemenu;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowMetrics;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;

import com.limelight.R;
import com.limelight.shortcuts.GameMenuShortcut;
import com.limelight.ui.BaseFragmentDialog.BaseGameMenuDialog;
import com.limelight.ui.gamemenu.adapter.GameMenuQuickKeyboardAdapter;
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;
import com.limelight.utils.UiToast;

import java.util.ArrayList;
import java.util.List;

/**
 * Stream-menu shortcut picker and editor.
 *
 * <p>Persistence is owned by the attached {@link GameMenuHost}; this dialog
 * only renders immutable shortcut snapshots and emits user intents.</p>
 */
public class GameListQuickFragment extends BaseGameMenuDialog {
    private GameMenuHostProvider hostProvider;
    private GameMenuHost host;
    private String title;
    private boolean hideBuiltInShortcuts;
    private GameMenuQuickKeyboardAdapter adapter;
    private List<GameMenuShortcut> shortcuts =
            new ArrayList<>();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (!(context instanceof GameMenuHostProvider)) {
            throw new IllegalStateException(
                    "GameListQuickFragment host must provide GameMenuHost");
        }
        hostProvider = (GameMenuHostProvider) context;
    }

    @Override
    public void onDetach() {
        host = null;
        hostProvider = null;
        super.onDetach();
    }

    @Override
    public int getLayoutRes() {
        return R.layout.dialog_game_menu_list;
    }

    @Override
    public void bindView(View view) {
        super.bindView(view);
        host = resolveHost();
        if (host == null) {
            throw new IllegalStateException(
                    "GameMenuHost is not initialized");
        }
        ImageButton backButton =
                view.findViewById(R.id.ibtn_back);
        ListView shortcutList =
                view.findViewById(R.id.lv_menu);
        TextView titleView =
                view.findViewById(R.id.tx_title);
        Button addButton = view.findViewById(R.id.btn_right);

        if (!TextUtils.isEmpty(title)) {
            titleView.setText(title);
        }
        backButton.setOnClickListener(ignored -> dismiss());
        addButton.setVisibility(View.VISIBLE);
        addButton.setOnClickListener(
                ignored -> showShortcutEditor());

        shortcuts = loadShortcuts();
        adapter = new GameMenuQuickKeyboardAdapter(
                getActivity(), toRows(shortcuts));
        shortcutList.setAdapter(adapter);
        shortcutList.setOnItemClickListener(
                (parent, row, position, id) -> {
                    if (shortcutSelectedListener != null) {
                        shortcutSelectedListener.onShortcutSelected(
                                shortcuts.get(position));
                    }
                });
        shortcutList.setOnItemLongClickListener(
                this::requestShortcutDeletion);
    }

    private void showShortcutEditor() {
        GameKeyboardUpdateFragment fragment =
                new GameKeyboardUpdateFragment();
        if (isLandscape(getActivity())) {
            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.R) {
                WindowMetrics windowMetrics =
                        getActivity()
                                .getWindowManager()
                                .getCurrentWindowMetrics();
                Rect bounds = windowMetrics.getBounds();
                fragment.setWidth(bounds.width());
            }
            else {
                fragment.setWidth(
                        getActivity()
                                .getResources()
                                .getDisplayMetrics()
                                .widthPixels);
            }
        }
        else {
            fragment.setWidth(
                    (getActivity()
                            .getResources()
                            .getDisplayMetrics()
                            .heightPixels * 2) / 3);
        }
        fragment.setDimAmount(0.8f);
        fragment.setTitle(
                R.string.keyboard_shortcut_setup_title);
        fragment.setKeyFrom(1);
        fragment.setOnClick(bean -> {
            if (host == null) {
                return;
            }
            try {
                if (!host.saveGameMenuShortcut(
                        GameMenuShortcutMapper.fromEditor(bean))) {
                    showPersistenceFailure();
                    return;
                }
                updateData();
                notifyShortcutsChanged();
            }
            catch (IllegalArgumentException error) {
                showPersistenceFailure();
            }
        });
        fragment.show(getParentFragmentManager());
    }

    private boolean requestShortcutDeletion(
            AdapterView<?> parent,
            View view,
            int position,
            long id) {
        GameMenuShortcut shortcut = shortcuts.get(position);
        if (!shortcut.isEditable()) {
            return true;
        }
        new AlertDialog.Builder(getActivity())
                .setTitle(shortcut.getName())
                .setMessage(R.string.keyboard_delete_confirmation)
                .setPositiveButton(
                        R.string.keyboard_delete,
                        (dialog, which) -> {
                            if (host == null ||
                                    !host.deleteGameMenuShortcut(
                                            shortcut.getId())) {
                                showPersistenceFailure();
                                return;
                            }
                            updateData();
                            notifyShortcutsChanged();
                        })
                .setNegativeButton(
                        R.string.keyboard_cancel,
                        (dialog, which) -> dialog.dismiss())
                .create()
                .show();
        return true;
    }

    public void setHideBuiltInShortcuts(boolean hide) {
        hideBuiltInShortcuts = hide;
    }

    private void updateData() {
        shortcuts = loadShortcuts();
        adapter.setDatas(toRows(shortcuts));
        adapter.notifyDataSetChanged();
    }

    private List<GameMenuShortcut> loadShortcuts() {
        if (host == null) {
            return new ArrayList<>();
        }
        return GameMenuShortcutCatalog.loadShortcuts(
                host.getState().getShortcuts(),
                !hideBuiltInShortcuts);
    }

    private GameMenuHost resolveHost() {
        if (host == null && hostProvider != null) {
            host = hostProvider.getGameMenuHost();
        }
        return host;
    }

    private static List<GameMenuQuickBean> toRows(
            List<GameMenuShortcut> source) {
        List<GameMenuQuickBean> rows =
                new ArrayList<>(source.size());
        for (GameMenuShortcut shortcut : source) {
            rows.add(GameMenuShortcutMapper.toRow(shortcut));
        }
        return rows;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    private ShortcutSelectedListener
            shortcutSelectedListener;

    public interface ShortcutSelectedListener {
        void onShortcutSelected(
                GameMenuShortcut shortcut);
    }

    public void setOnShortcutSelectedListener(
            ShortcutSelectedListener listener) {
        shortcutSelectedListener = listener;
    }

    private Runnable shortcutsChangedListener;

    public void setOnShortcutsChangedListener(
            Runnable listener) {
        shortcutsChangedListener = listener;
    }

    private void notifyShortcutsChanged() {
        if (shortcutsChangedListener != null) {
            shortcutsChangedListener.run();
        }
    }

    private static boolean isLandscape(Context context) {
        return context.getResources()
                .getDisplayMetrics().widthPixels >
                context.getResources()
                        .getDisplayMetrics().heightPixels;
    }

    private void showPersistenceFailure() {
        if (getActivity() != null) {
            UiToast.makeText(
                    getActivity(),
                    R.string.keyboard_shortcut_save_failed,
                    UiToast.LENGTH_SHORT)
                    .show();
        }
    }
}
