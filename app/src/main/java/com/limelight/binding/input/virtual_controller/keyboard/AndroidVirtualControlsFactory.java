package com.limelight.binding.input.virtual_controller.keyboard;

import android.widget.FrameLayout;
import androidx.fragment.app.FragmentActivity;

import com.limelight.binding.input.ControllerHandler;
import com.limelight.binding.input.StreamInputGateway;
import com.limelight.settings.input.InputSettingsState;
import com.limelight.settings.virtualcontrols.VirtualControlSettingsState;
import com.limelight.ui.StreamUiActions;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutRepository;

import java.util.Objects;

/** Android composition adapter for stream virtual-control overlays. */
public final class AndroidVirtualControlsFactory
        implements StreamVirtualControlsController.Factory {
    private final ControllerHandler controllerHandler;
    private final FrameLayout parent;
    private final FragmentActivity activity;
    private final InputSettingsState inputSettingsState;
    private final VirtualControlSettingsState virtualControlSettingsState;
    private final VirtualControlLayoutRepository layoutRepository;
    private final StreamInputGateway inputGateway;
    private final StreamUiActions uiActions;

    public AndroidVirtualControlsFactory(
            ControllerHandler controllerHandler,
            FrameLayout parent,
            FragmentActivity activity,
            InputSettingsState inputSettingsState,
            VirtualControlSettingsState virtualControlSettingsState,
            VirtualControlLayoutRepository layoutRepository,
            StreamInputGateway inputGateway,
            StreamUiActions uiActions) {
        this.controllerHandler = controllerHandler;
        this.parent = Objects.requireNonNull(parent, "parent");
        this.activity = Objects.requireNonNull(activity, "activity");
        this.inputSettingsState = Objects.requireNonNull(
                inputSettingsState,
                "inputSettingsState");
        this.virtualControlSettingsState = Objects.requireNonNull(
                virtualControlSettingsState,
                "virtualControlSettingsState");
        this.layoutRepository = Objects.requireNonNull(
                layoutRepository,
                "layoutRepository");
        this.inputGateway = Objects.requireNonNull(
                inputGateway,
                "inputGateway");
        this.uiActions = Objects.requireNonNull(uiActions, "uiActions");
    }

    @Override
    public EditableVirtualControlOverlay createVirtualGamepad() {
        return createEditableOverlay(true);
    }

    @Override
    public EditableVirtualControlOverlay createVirtualKeys() {
        return createEditableOverlay(false);
    }

    @Override
    public FullKeyboardOverlay createFullKeyboard() {
        return new KeyBoardLayoutController(
                controllerHandler,
                parent,
                activity,
                virtualControlSettingsState,
                inputGateway,
                uiActions);
    }

    private EditableVirtualControlOverlay createEditableOverlay(
            boolean gamepad) {
        return new KeyBoardController(
                controllerHandler,
                parent,
                activity,
                inputSettingsState,
                virtualControlSettingsState,
                layoutRepository,
                gamepad,
                inputGateway,
                uiActions);
    }
}
