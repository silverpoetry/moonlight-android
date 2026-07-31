package com.limelight.binding.input.virtual_controller.keyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class StreamVirtualControlsControllerTest {
    @Test
    public void overlaysAreLazyAndCreatedOnlyOnce() {
        FakeFactory factory = new FakeFactory();
        StreamVirtualControlsController controller =
                new StreamVirtualControlsController(factory);

        assertEquals(0, factory.gamepadCreates);
        controller.toggleVirtualGamepad();
        assertEquals(1, factory.gamepadCreates);
        assertTrue(controller.isVirtualGamepadVisible());

        controller.toggleVirtualGamepad();
        assertFalse(controller.isVirtualGamepadVisible());
        controller.toggleVirtualGamepad();
        assertEquals(1, factory.gamepadCreates);
        assertTrue(controller.isVirtualGamepadVisible());

        controller.toggleFullKeyboard();
        assertEquals(1, factory.fullKeyboardCreates);
        assertEquals(1, factory.fullKeyboard.refreshCount);
        assertTrue(factory.fullKeyboard.visible);
    }

    @Test
    public void editingAndModeChangesUseVisibleOwner() {
        FakeFactory factory = new FakeFactory();
        StreamVirtualControlsController controller =
                new StreamVirtualControlsController(factory);

        assertFalse(controller.setVirtualKeysMode(
                VirtualControlEditMode.MOVE_BUTTONS));
        controller.showVirtualKeys();
        assertTrue(controller.setVirtualKeysMode(
                VirtualControlEditMode.MOVE_BUTTONS));
        assertTrue(controller.isEditingLayout());

        controller.hideAll();
        assertFalse(controller.isEditingLayout());
        assertFalse(controller.setVirtualKeysMode(
                VirtualControlEditMode.ACTIVE));
        assertEquals(
                VirtualControlEditMode.NONE,
                controller.getVirtualKeysMode());
    }

    @Test
    public void refreshAndDestroyAffectOnlyCreatedOverlays() {
        FakeFactory factory = new FakeFactory();
        StreamVirtualControlsController controller =
                new StreamVirtualControlsController(factory);
        controller.showVirtualGamepad();

        controller.refreshCreatedLayouts();
        assertEquals(2, factory.gamepad.refreshCount);
        assertEquals(0, factory.keysCreates);
        assertEquals(0, factory.fullKeyboardCreates);

        controller.destroy();
        controller.destroy();
        assertEquals(1, factory.gamepad.destroyCount);

        controller.toggleVirtualGamepad();
        controller.toggleVirtualKeys();
        controller.toggleFullKeyboard();
        assertEquals(1, factory.gamepadCreates);
        assertEquals(0, factory.keysCreates);
        assertEquals(0, factory.fullKeyboardCreates);
    }

    private static final class FakeFactory
            implements StreamVirtualControlsController.Factory {
        final FakeEditableOverlay gamepad = new FakeEditableOverlay();
        final FakeEditableOverlay keys = new FakeEditableOverlay();
        final FakeFullKeyboardOverlay fullKeyboard =
                new FakeFullKeyboardOverlay();
        int gamepadCreates;
        int keysCreates;
        int fullKeyboardCreates;

        @Override
        public EditableVirtualControlOverlay createVirtualGamepad() {
            gamepadCreates++;
            return gamepad;
        }

        @Override
        public EditableVirtualControlOverlay createVirtualKeys() {
            keysCreates++;
            return keys;
        }

        @Override
        public FullKeyboardOverlay createFullKeyboard() {
            fullKeyboardCreates++;
            return fullKeyboard;
        }
    }

    private static final class FakeEditableOverlay
            implements EditableVirtualControlOverlay {
        boolean visible;
        int refreshCount;
        int destroyCount;
        VirtualControlEditMode mode = VirtualControlEditMode.NONE;

        @Override
        public void show() {
            visible = true;
            mode = VirtualControlEditMode.ACTIVE;
            refreshLayout();
        }

        @Override
        public void hide() {
            visible = false;
            mode = VirtualControlEditMode.NONE;
        }

        @Override
        public void toggleVisibility() {
            if (visible) {
                hide();
            }
            else {
                show();
            }
        }

        @Override
        public boolean isVisible() {
            return visible;
        }

        @Override
        public void refreshLayout() {
            refreshCount++;
        }

        @Override
        public void switchMode(VirtualControlEditMode mode) {
            this.mode = mode;
        }

        @Override
        public VirtualControlEditMode getControllerMode() {
            return mode;
        }

        @Override
        public void destroy() {
            destroyCount++;
            visible = false;
            mode = VirtualControlEditMode.NONE;
        }
    }

    private static final class FakeFullKeyboardOverlay
            implements FullKeyboardOverlay {
        boolean visible;
        int refreshCount;
        int destroyCount;

        @Override
        public void show() {
            visible = true;
        }

        @Override
        public void hide() {
            visible = false;
        }

        @Override
        public void toggleVisibility() {
            visible = !visible;
        }

        @Override
        public boolean isVisible() {
            return visible;
        }

        @Override
        public void refreshLayout() {
            refreshCount++;
        }

        @Override
        public void destroy() {
            destroyCount++;
            visible = false;
        }
    }
}
