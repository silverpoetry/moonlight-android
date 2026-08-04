package com.limelight.binding.input.driver;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.os.SystemClock;
import com.limelight.DebugLog;

import com.limelight.nvstream.input.ControllerPacket;
import com.limelight.nvstream.jni.MoonBridge;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractDualSenseController extends AbstractController {
    private static final int TOUCHPAD_FINGER_COUNT = 2;

    protected final UsbDevice device;
    protected final UsbDeviceConnection connection;

    private Thread inputThread;
    private boolean stopped;
    private final boolean[] activeTouchpadFingers = new boolean[TOUCHPAD_FINGER_COUNT];
    private final int[] activeTouchpadFingerIds = new int[TOUCHPAD_FINGER_COUNT];
    private final float[] activeTouchpadFingerX = new float[TOUCHPAD_FINGER_COUNT];
    private final float[] activeTouchpadFingerY = new float[TOUCHPAD_FINGER_COUNT];

    protected UsbEndpoint inEndpt, outEndpt;

    public AbstractDualSenseController(UsbDevice device, UsbDeviceConnection connection, int deviceId, UsbDriverListener listener) {
        super(deviceId, listener, device.getVendorId(), device.getProductId());
        this.device = device;
        this.connection = connection;
        this.type = MoonBridge.LI_CTYPE_PS;
        this.capabilities = MoonBridge.LI_CCAP_GYRO | MoonBridge.LI_CCAP_ACCEL |
                MoonBridge.LI_CCAP_RUMBLE | MoonBridge.LI_CCAP_TOUCHPAD;
        this.supportedButtonFlags =
                ControllerPacket.A_FLAG | ControllerPacket.B_FLAG | ControllerPacket.X_FLAG | ControllerPacket.Y_FLAG |
                        ControllerPacket.UP_FLAG | ControllerPacket.DOWN_FLAG | ControllerPacket.LEFT_FLAG | ControllerPacket.RIGHT_FLAG |
                        ControllerPacket.LB_FLAG | ControllerPacket.RB_FLAG |
                        ControllerPacket.LS_CLK_FLAG | ControllerPacket.RS_CLK_FLAG |
                        ControllerPacket.BACK_FLAG | ControllerPacket.PLAY_FLAG |
                        ControllerPacket.SPECIAL_BUTTON_FLAG | ControllerPacket.TOUCHPAD_FLAG;
        this.buttonFlags = supportedButtonFlags;
    }

    private Thread createInputThread() {
        return new Thread() {
            public void run() {
                try {
                    // Delay for a moment before reporting the new gamepad and
                    // accepting new input. This allows time for the old InputDevice
                    // to go away before we reclaim its spot. If the old device is still
                    // around when we call notifyDeviceAdded(), we won't be able to claim
                    // the controller number used by the original InputDevice.
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }

                // Report that we're added _before_ reporting input
                notifyDeviceAdded();

                while (!isInterrupted() && !stopped) {
                    byte[] buffer = new byte[64];

                    int res;

                    //
                    // There's no way that I can tell to determine if a device has failed
                    // or if the timeout has simply expired. We'll check how long the transfer
                    // took to fail and assume the device failed if it happened before the timeout
                    // expired.
                    //

                    do {
                        // Read the next input state packet
                        long lastMillis = SystemClock.uptimeMillis();
                        res = connection.bulkTransfer(inEndpt, buffer, buffer.length, 3000);

                        // If we get a zero length response, treat it as an error
                        if (res == 0) {
                            res = -1;
                        }

                        if (res == -1 && SystemClock.uptimeMillis() - lastMillis < 1000) {
                            DebugLog.debug("DualSenseController", "Detected device I/O error");
                            AbstractDualSenseController.this.stop();
                            break;
                        }
                    } while (res == -1 && !isInterrupted() && !stopped);

                    if (res == -1 || stopped) {
                        break;
                    }

                    if (handleRead(ByteBuffer.wrap(buffer, 0, res).order(ByteOrder.LITTLE_ENDIAN))) {
                        // Report input if handleRead() returns true
                        reportInput();
                        reportMotion();
                    }
                }
            }
        };
    }

    private static UsbInterface findInterface(UsbDevice device) {
        int count = device.getInterfaceCount();
        for (int i = 0; i < count; i++) {
            UsbInterface intf = device.getInterface(i);
            if (intf.getInterfaceClass() == UsbConstants.USB_CLASS_HID && intf.getEndpointCount()>=2) {
                DebugLog.debug("DualSenseController", "Found HID interface: " + i);
                return intf;
            }
        }
        return null;
    }

    private List<UsbInterface> ifaces=new ArrayList<>();

    public boolean start() {
        ifaces.clear();
        inEndpt = null;
        outEndpt = null;
        DebugLog.debug("DualSenseController", "start");
        // Force claim all interfaces
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface iface = device.getInterface(i);

            if (!connection.claimInterface(iface, true)) {
                DebugLog.debug("DualSenseController", "Failed to claim interfaces");
                return false;
            }else{
                ifaces.add(iface);
            }
        }
        DebugLog.debug("DualSenseController", "getInterfaceCount:" + device.getInterfaceCount());
        // Find the endpoints
        UsbInterface iface = findInterface(device);

        if (iface == null) {
            DebugLog.error("DualSenseController", "Failed to find interface");
            return false;
        }

        for (int i = 0; i < iface.getEndpointCount(); i++) {
            UsbEndpoint endpt = iface.getEndpoint(i);
            if (endpt.getDirection() == UsbConstants.USB_DIR_OUT) {
                if (outEndpt != null) {
                    DebugLog.debug("DualSenseController", "Found duplicate OUT endpoint");
                    return false;
                }
                outEndpt = endpt;
            } else if (endpt.getDirection() == UsbConstants.USB_DIR_IN) {
                if (inEndpt != null) {
                    DebugLog.debug("DualSenseController", "Found duplicate IN endpoint");
                    return false;
                }
                inEndpt = endpt;
            }
        }
        DebugLog.debug("DualSenseController", "inEndpt: " + inEndpt);
        DebugLog.debug("DualSenseController", "outEndpt: " + outEndpt);
        // Make sure the required endpoints were present
        if (inEndpt == null || outEndpt == null) {
            DebugLog.debug("DualSenseController", "Missing required endpoin");
            return false;
        }
        // Run the init function
        if (!doInit()) {
            return false;
        }
        // Start listening for controller input
        inputThread = createInputThread();
        inputThread.start();
        return true;
    }

    public void stop() {
        if (stopped) {
            return;
        }

        stopped = true;
        cancelActiveTouchpadFingers();
        // Cancel any rumble effects
        rumble((short)0, (short)0);

        // Stop the input thread
        if (inputThread != null) {
            inputThread.interrupt();
            inputThread = null;
        }

        if(!ifaces.isEmpty()&&connection!=null){
            for (int i = 0; i < ifaces.size(); i++) {
                UsbInterface iface = ifaces.get(i);
                connection.releaseInterface(iface);
            }
            ifaces.clear();
        }

        // Close the USB connection
        connection.close();

        // Report the device removed
        notifyDeviceRemoved();
    }

    protected void updateTouchpadFinger(int fingerIndex, boolean active, int pointerId, float x, float y) {
        if (fingerIndex < 0 || fingerIndex >= TOUCHPAD_FINGER_COUNT) {
            return;
        }

        boolean wasActive = activeTouchpadFingers[fingerIndex];
        int previousPointerId = activeTouchpadFingerIds[fingerIndex];
        float previousX = activeTouchpadFingerX[fingerIndex];
        float previousY = activeTouchpadFingerY[fingerIndex];

        float normalizedX = clampUnitRange(x);
        float normalizedY = clampUnitRange(y);

        if (active) {
            if (!wasActive) {
                reportTouchpadEvent(MoonBridge.LI_TOUCH_EVENT_DOWN, pointerId, normalizedX, normalizedY, 1.0f);
            }
            else if (previousPointerId != pointerId) {
                reportTouchpadEvent(MoonBridge.LI_TOUCH_EVENT_UP, previousPointerId, previousX, previousY, 0.0f);
                reportTouchpadEvent(MoonBridge.LI_TOUCH_EVENT_DOWN, pointerId, normalizedX, normalizedY, 1.0f);
            }
            else if (Float.compare(previousX, normalizedX) != 0 || Float.compare(previousY, normalizedY) != 0) {
                reportTouchpadEvent(MoonBridge.LI_TOUCH_EVENT_MOVE, pointerId, normalizedX, normalizedY, 1.0f);
            }

            activeTouchpadFingers[fingerIndex] = true;
            activeTouchpadFingerIds[fingerIndex] = pointerId;
            activeTouchpadFingerX[fingerIndex] = normalizedX;
            activeTouchpadFingerY[fingerIndex] = normalizedY;
        }
        else if (wasActive) {
            reportTouchpadEvent(MoonBridge.LI_TOUCH_EVENT_UP, previousPointerId, previousX, previousY, 0.0f);
            activeTouchpadFingers[fingerIndex] = false;
        }
    }

    protected void cancelActiveTouchpadFingers() {
        boolean hasActiveTouch = false;
        for (boolean activeTouchpadFinger : activeTouchpadFingers) {
            if (activeTouchpadFinger) {
                hasActiveTouch = true;
                break;
            }
        }

        if (hasActiveTouch) {
            reportTouchpadEvent(MoonBridge.LI_TOUCH_EVENT_CANCEL_ALL, 0, 0.0f, 0.0f, 0.0f);
        }

        Arrays.fill(activeTouchpadFingers, false);
    }

    protected float normalizeTouchCoordinate(int rawValue, float range) {
        return clampUnitRange(rawValue / range);
    }

    private float clampUnitRange(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 0.0f;
        }
        if (value < 0.0f) {
            return 0.0f;
        }
        if (value > 1.0f) {
            return 1.0f;
        }
        return value;
    }

    protected abstract boolean handleRead(ByteBuffer buffer);
    protected abstract boolean doInit();
}
