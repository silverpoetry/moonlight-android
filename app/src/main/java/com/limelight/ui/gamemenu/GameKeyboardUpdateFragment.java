package com.limelight.ui.gamemenu;

import android.content.res.Configuration;
import androidx.annotation.StringRes;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.gson.Gson;
import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.ui.BaseFragmentDialog.BaseGameMenuDialog;
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;
import com.limelight.utils.UiToast;

import java.util.ArrayList;
import java.util.List;

import static com.limelight.ui.gamemenu.GameListKeyBoardFragment.PREF_KEYBOARD_LIST_KEY;
import static com.limelight.ui.gamemenu.GameListQuickFragment.PREF_QUICK_LIST_KEY;

public class GameKeyboardUpdateFragment
        extends BaseGameMenuDialog implements View.OnClickListener {
    private static final int MAX_CHORD_KEYS = 5;
    private static final int KEY_FROM_BUTTON_LIST = 0;
    private static final int KEY_FROM_SHORTCUT_LIST = 1;

    private static final int BUTTON_TYPE_KEYBOARD = 4;

    private int titleRes = R.string.keyboard_chord_title;
    private int keyFrom = KEY_FROM_BUTTON_LIST;

    private EditText nameInput;
    private TextView chordContent;
    private GridView mouseButtonGrid;
    private GridView functionButtonGrid;
    private Listener listener;

    private final KeyChordSelection chordSelection =
            new KeyChordSelection(MAX_CHORD_KEYS);
    private final List<GameMenuQuickBean> mouseButtonItems = new ArrayList<>();
    private final List<GameMenuQuickBean> functionButtonItems = new ArrayList<>();

    @Override
    public int getLayoutRes() {
        return R.layout.dialog_game_menu_keyboard_group_add;
    }

    @Override
    public void bindView(View view) {
        super.bindView(view);

        TextView titleView = view.findViewById(R.id.tx_title);
        titleView.setText(titleRes);
        nameInput = view.findViewById(R.id.edt_name);
        chordContent = view.findViewById(R.id.tx_content);
        mouseButtonGrid = view.findViewById(R.id.rv_keyboard_mouse);
        functionButtonGrid = view.findViewById(R.id.rv_keyboard_function);

        view.findViewById(R.id.ibtn_back).setOnClickListener(this);
        view.findViewById(R.id.btn_right).setOnClickListener(this);
        view.findViewById(R.id.btn_reset).setOnClickListener(this);

        View fullKeyboard = view.findViewById(R.id.lv_keyboard);
        View numericKeyboard = view.findViewById(R.id.lv_keyboard_digitpad);
        attachKeyListeners(fullKeyboard);
        attachKeyListeners(numericKeyboard);

        configureInputTypeTabs(
                view, fullKeyboard, numericKeyboard);
        populateMouseButtonItems();
        populateFunctionButtonItems();
        configureItemGrids();
    }

    private void configureInputTypeTabs(
            View root, View fullKeyboard, View numericKeyboard) {
        RadioGroup keyboardTabs = root.findViewById(R.id.rg_keyboard);
        keyboardTabs.check(R.id.rbt_keyboard_1);
        keyboardTabs.setOnCheckedChangeListener(
                (group, checkedId) -> {
                    setVisible(
                            fullKeyboard,
                            checkedId == R.id.rbt_keyboard_1);
                    setVisible(
                            numericKeyboard,
                            checkedId == R.id.rbt_keyboard_2);
                    setVisible(
                            mouseButtonGrid,
                            checkedId == R.id.rbt_keyboard_3);
                    setVisible(
                            functionButtonGrid,
                            checkedId == R.id.rbt_keyboard_4);
                });

        boolean editingButton =
                keyFrom == KEY_FROM_BUTTON_LIST;
        setVisible(
                root.findViewById(R.id.rbt_keyboard_3),
                editingButton);
        setVisible(
                root.findViewById(R.id.rbt_keyboard_4),
                editingButton);
    }

    private static void setVisible(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void attachKeyListeners(View view) {
        if (view instanceof TextView && view.getTag() != null) {
            view.setOnClickListener(this::selectKey);
            view.setOnTouchListener(this::showKeyPressFeedback);
            return;
        }
        if (!(view instanceof ViewGroup)) {
            return;
        }

        ViewGroup group = (ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); index++) {
            attachKeyListeners(group.getChildAt(index));
        }
    }

    private boolean showKeyPressFeedback(
            View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                view.setBackgroundResource(
                        R.drawable.bg_ax_keyboard_button_confirm);
                return true;
            case MotionEvent.ACTION_UP:
                view.setBackgroundResource(
                        R.drawable.bg_ax_keyboard_button);
                view.performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                view.setBackgroundResource(
                        R.drawable.bg_ax_keyboard_button);
                return true;
            default:
                return true;
        }
    }

    private void selectKey(View view) {
        Object tag = view.getTag();
        if (tag == null || !(view instanceof TextView)) {
            return;
        }
        boolean added = chordSelection.add(
                String.valueOf(tag),
                ((TextView) view).getText().toString().trim());
        if (!added) {
            showToast(R.string.keyboard_maximum_keys);
            return;
        }
        updateChordContent();
    }

    private void updateChordContent() {
        if (chordSelection.isEmpty()) {
            chordContent.setText(
                    R.string.keyboard_chord_value_hint);
            return;
        }
        chordContent.setText(chordSelection.getDisplayName());
    }

    private void populateMouseButtonItems() {
        mouseButtonItems.clear();
        mouseButtonItems.addAll(
                KeyboardPresetFactory.createMouseAndTouchItems(
                        getActivity()));
    }

    private void populateFunctionButtonItems() {
        functionButtonItems.clear();
        functionButtonItems.addAll(
                KeyboardPresetFactory.createFunctionItems(
                        getActivity()));
    }

    private void configureItemGrids() {
        int columns = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE
                ? 5 : 3;
        mouseButtonGrid.setNumColumns(columns);
        functionButtonGrid.setNumColumns(columns);

        mouseButtonGrid.setAdapter(new KeyboardPresetAdapter(
                getActivity(), mouseButtonItems));
        mouseButtonGrid.setOnItemClickListener(
                (parent, view, position, id) ->
                        selectPreset(mouseButtonItems.get(position)));

        functionButtonGrid.setAdapter(new KeyboardPresetAdapter(
                getActivity(), functionButtonItems));
        functionButtonGrid.setOnItemClickListener(
                (parent, view, position, id) ->
                        selectPreset(functionButtonItems.get(position)));
    }

    private void selectPreset(GameMenuQuickBean item) {
        item.setId(PREF_KEYBOARD_LIST_KEY
                + System.currentTimeMillis());
        LimeLog.info("Virtual input preset selected: "
                + new Gson().toJson(item));
        if (listener != null) {
            listener.onSelected(item);
        }
        dismiss();
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.ibtn_back) {
            dismiss();
            return;
        }
        if (viewId == R.id.btn_reset) {
            resetEditor();
            return;
        }
        if (viewId == R.id.btn_right) {
            saveChord();
        }
    }

    private void resetEditor() {
        chordSelection.clear();
        nameInput.setText(null);
        updateChordContent();
    }

    private void saveChord() {
        String name = nameInput.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            showToast(R.string.keyboard_name_required);
            return;
        }
        if (chordSelection.isEmpty()) {
            showToast(R.string.keyboard_chord_required);
            return;
        }

        GameMenuQuickBean item = new GameMenuQuickBean();
        item.setName(name);
        item.setId((keyFrom == KEY_FROM_SHORTCUT_LIST
                ? PREF_QUICK_LIST_KEY
                : PREF_KEYBOARD_LIST_KEY)
                + System.currentTimeMillis());
        item.setBtnType(BUTTON_TYPE_KEYBOARD);
        item.setCodes(chordSelection.getEncodedKeyCodes());
        item.setDesc(chordSelection.getDisplayName());

        if (listener != null) {
            listener.onSelected(item);
        }
        dismiss();
    }

    private void showToast(@StringRes int messageRes) {
        UiToast.makeText(
                getActivity(), messageRes, UiToast.LENGTH_SHORT)
                .show();
    }

    @Override
    public float getDimAmount() {
        return 0.9f;
    }

    public void setTitle(@StringRes int titleRes) {
        this.titleRes = titleRes;
    }

    public void setKeyFrom(int keyFrom) {
        this.keyFrom = keyFrom;
    }

    public void setOnClick(onClick callback) {
        listener = callback == null
                ? null : callback::click;
    }

    public interface onClick {
        void click(GameMenuQuickBean item);
    }

    private interface Listener {
        void onSelected(GameMenuQuickBean item);
    }

}
