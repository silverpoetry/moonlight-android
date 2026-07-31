package com.limelight.ui.gamemenu;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.os.Build;
import android.view.DragEvent;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.Space;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.settings.ui.GameMenuCardLayout;
import com.limelight.utils.UiHelper;

import java.util.ArrayList;
import java.util.List;

final class GameMenuCardEditor {
    interface Listener {
        void onSave(GameMenuCardLayout layout);

        void onDismissed();
    }

    private static final int COLUMN_COUNT = 4;

    private final Activity activity;
    private final Listener listener;
    private final List<GameMenuCardCatalog.Card> catalog;
    private final GameMenuCardConfiguration.State initialState;
    private final List<GameMenuCardCatalog.Card> visible =
            new ArrayList<>();
    private final List<GameMenuCardCatalog.Card> hidden =
            new ArrayList<>();

    private AlertDialog dialog;
    private GridLayout visibleGrid;
    private GridLayout hiddenGrid;
    private TextView visibleCount;
    private TextView hiddenCount;
    private TextView visibleEmpty;
    private TextView hiddenEmpty;

    GameMenuCardEditor(
            Activity activity,
            List<GameMenuCardCatalog.Card> catalog,
            GameMenuCardConfiguration.State initialState,
            Listener listener) {
        this.activity = activity;
        this.catalog = new ArrayList<>(catalog);
        this.initialState = initialState;
        this.listener = listener;
    }

    void show() {
        replaceState(initialState);

        View content = activity.getLayoutInflater().inflate(
                R.layout.dialog_game_menu_action_editor, null);
        visibleGrid = content.findViewById(
                R.id.game_menu_action_visible_grid);
        hiddenGrid = content.findViewById(
                R.id.game_menu_action_hidden_grid);
        visibleCount = content.findViewById(
                R.id.game_menu_action_visible_count);
        hiddenCount = content.findViewById(
                R.id.game_menu_action_hidden_count);
        visibleEmpty = content.findViewById(
                R.id.game_menu_action_visible_empty);
        hiddenEmpty = content.findViewById(
                R.id.game_menu_action_hidden_empty);

        visibleGrid.setOnDragListener(this::handleVisibleGridDrag);
        content.findViewById(R.id.game_menu_action_reset)
                .setOnClickListener(view -> {
                    replaceState(
                            GameMenuCardConfiguration.defaults(catalog));
                    render();
                });
        content.findViewById(R.id.game_menu_action_cancel)
                .setOnClickListener(view -> dismiss());
        content.findViewById(R.id.game_menu_action_done)
                .setOnClickListener(view -> {
                    listener.onSave(
                            GameMenuCardConfiguration.toLayout(
                                    visible,
                                    hidden));
                    dismiss();
                });

        dialog = new AlertDialog.Builder(activity)
                .setView(content)
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnDismissListener(
                ignored -> listener.onDismissed());
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(
                    android.R.color.transparent);
            int availableWidth =
                    activity.getResources().getDisplayMetrics().widthPixels -
                            UiHelper.dpToPx(activity, 32);
            window.setLayout(
                    Math.max(1, Math.min(
                            availableWidth,
                            UiHelper.dpToPx(activity, 560))),
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
        render();
    }

    void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    private void replaceState(
            GameMenuCardConfiguration.State state) {
        visible.clear();
        visible.addAll(state.visible);
        hidden.clear();
        hidden.addAll(state.hidden);
    }

    private void render() {
        renderGrid(visibleGrid, visible, true);
        renderGrid(hiddenGrid, hidden, false);
        visibleCount.setText(activity.getString(
                R.string.game_menu_customize_count, visible.size()));
        hiddenCount.setText(activity.getString(
                R.string.game_menu_customize_count, hidden.size()));
        visibleEmpty.setVisibility(
                visible.isEmpty() ? View.VISIBLE : View.GONE);
        hiddenEmpty.setVisibility(
                hidden.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void renderGrid(
            GridLayout grid,
            List<GameMenuCardCatalog.Card> cards,
            boolean isVisible) {
        grid.removeAllViews();
        for (int index = 0; index < cards.size(); index++) {
            GameMenuCardCatalog.Card card = cards.get(index);
            View tile = createTile(grid, card, isVisible);
            grid.addView(tile, createGridLayoutParams(index));
        }
        int placeholders =
                (COLUMN_COUNT - cards.size() % COLUMN_COUNT) %
                        COLUMN_COUNT;
        for (int index = 0; index < placeholders; index++) {
            int position = cards.size() + index;
            Space placeholder = new Space(activity);
            grid.addView(
                    placeholder, createGridLayoutParams(position));
        }
    }

    private View createTile(
            GridLayout parent,
            GameMenuCardCatalog.Card card,
            boolean isVisible) {
        View tile = activity.getLayoutInflater().inflate(
                R.layout.item_game_menu_action_edit, parent, false);
        TextView label = tile.findViewById(
                R.id.game_menu_action_label);
        TextView badge = tile.findViewById(
                R.id.game_menu_action_badge);
        label.setText(card.label);
        label.setCompoundDrawablesWithIntrinsicBounds(
                0, card.iconRes, 0, 0);
        badge.setText(isVisible ?
                R.string.game_menu_customize_badge_remove :
                R.string.game_menu_customize_badge_add);
        badge.setBackgroundResource(
                isVisible ?
                        R.drawable.bg_game_menu_action_badge_remove :
                        R.drawable.bg_game_menu_action_badge_add);
        tile.setTag(card.id);
        tile.setOnClickListener(view -> {
            if (isVisible) {
                visible.remove(card);
                hidden.add(card);
            } else {
                hidden.remove(card);
                visible.add(card);
            }
            render();
        });
        if (isVisible) {
            tile.setOnLongClickListener(view -> {
                view.performHapticFeedback(
                        HapticFeedbackConstants.LONG_PRESS);
                ClipData clipData = ClipData.newPlainText(
                        "game-menu-card", card.id);
                View.DragShadowBuilder shadow =
                        new View.DragShadowBuilder(view);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    return view.startDragAndDrop(
                            clipData, shadow, card.id, 0);
                }
                return view.startDrag(
                        clipData, shadow, card.id, 0);
            });
            tile.setContentDescription(activity.getString(
                    R.string.game_menu_customize_move_or_remove,
                    card.contentDescription));
        } else {
            tile.setContentDescription(activity.getString(
                    R.string.game_menu_customize_add,
                    card.contentDescription));
        }
        return tile;
    }

    private boolean handleVisibleGridDrag(
            View view, DragEvent event) {
        if (!(event.getLocalState() instanceof String)) {
            return false;
        }
        String cardId = (String) event.getLocalState();
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                setDraggedTileAlpha(cardId, 0.45f);
                return true;
            case DragEvent.ACTION_DRAG_LOCATION:
                moveDraggedCard(
                        cardId, event.getX(), event.getY());
                return true;
            case DragEvent.ACTION_DROP:
                return true;
            case DragEvent.ACTION_DRAG_ENDED:
                setDraggedTileAlpha(cardId, 1f);
                return true;
            default:
                return true;
        }
    }

    private void moveDraggedCard(
            String cardId, float x, float y) {
        int sourceIndex = findVisibleCardIndex(cardId);
        int targetIndex = findNearestVisibleTile(x, y);
        if (sourceIndex < 0 || targetIndex < 0 ||
                sourceIndex == targetIndex) {
            return;
        }

        GameMenuCardCatalog.Card moved =
                visible.remove(sourceIndex);
        visible.add(targetIndex, moved);

        View movedView = visibleGrid.getChildAt(sourceIndex);
        visibleGrid.removeViewAt(sourceIndex);
        visibleGrid.addView(movedView, targetIndex);
        updateVisibleGridPositions();
    }

    private int findVisibleCardIndex(String cardId) {
        for (int index = 0; index < visible.size(); index++) {
            if (visible.get(index).id.equals(cardId)) {
                return index;
            }
        }
        return -1;
    }

    private int findNearestVisibleTile(float x, float y) {
        if (visible.isEmpty()) {
            return -1;
        }
        int nearest = 0;
        float nearestDistance = Float.MAX_VALUE;
        for (int index = 0; index < visible.size(); index++) {
            View child = visibleGrid.getChildAt(index);
            float deltaX =
                    x - (child.getLeft() + child.getRight()) / 2f;
            float deltaY =
                    y - (child.getTop() + child.getBottom()) / 2f;
            float distance = deltaX * deltaX + deltaY * deltaY;
            if (distance < nearestDistance) {
                nearest = index;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private void updateVisibleGridPositions() {
        for (int index = 0;
             index < visibleGrid.getChildCount(); index++) {
            View child = visibleGrid.getChildAt(index);
            child.setLayoutParams(createGridLayoutParams(index));
        }
        visibleGrid.requestLayout();
    }

    private void setDraggedTileAlpha(
            String cardId, float alpha) {
        for (int index = 0; index < visible.size(); index++) {
            View child = visibleGrid.getChildAt(index);
            if (cardId.equals(child.getTag())) {
                child.setAlpha(alpha);
                return;
            }
        }
    }

    private GridLayout.LayoutParams createGridLayoutParams(
            int index) {
        GridLayout.LayoutParams params =
                new GridLayout.LayoutParams(
                        GridLayout.spec(index / COLUMN_COUNT, 1),
                        GridLayout.spec(
                                index % COLUMN_COUNT, 1, 1f));
        params.width = 0;
        params.height = UiHelper.dpToPx(activity, 72);
        int margin = UiHelper.dpToPx(activity, 3);
        params.setMargins(margin, margin, margin, margin);
        return params;
    }
}
