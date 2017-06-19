package com.nilhcem.ledcontrol;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int NB_DEVICES = 1;

    private static final int HANDLER_MSG_SHOW = 1;
    private static final int HANDLER_MSG_STOP = 2;
    private static final int FRAME_DELAY_MS = 125;

    private LedControl ledControl;

    private int index;
    private Handler handler;
    private HandlerThread handlerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            ledControl = new LedControl("SPI0.0", NB_DEVICES);
            for (int i = 0; i < ledControl.getDeviceCount(); i++) {
                ledControl.setIntensity(i, 13);
                ledControl.shutdown(i, false);
                ledControl.clearDisplay(i);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error initializing LED matrix", e);
        }

        handlerThread = new HandlerThread("FrameThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what != HANDLER_MSG_SHOW) {
                    return;
                }

                try {
                    byte[] frame;

                    if (NB_DEVICES == 4) {
                        for (int device = 0; device < NB_DEVICES; device++) {
                            frame = Invaders.FRAMES_ALIENS[device][index];
                            for (int row = 0; row < 8; row++) {
                                ledControl.setRow(device, row, frame[row]);
                            }
                        }
                        index = (index + 1) % Invaders.FRAMES_ALIENS[0].length;
                    } else {
                        frame = Invaders.FRAMES[index];
                        for (int row = 0; row < 8; row++) {
                            ledControl.setRow(0, row, frame[row]);
                        }
                        index = (index + 1) % Invaders.FRAMES.length;
                    }
                    handler.sendEmptyMessageDelayed(HANDLER_MSG_SHOW, FRAME_DELAY_MS);
                } catch (IOException e) {
                    Log.e(TAG, "Error displaying frame", e);
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        handler.sendEmptyMessage(HANDLER_MSG_SHOW);
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.sendEmptyMessage(HANDLER_MSG_STOP);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            handlerThread.quitSafely();

            ledControl.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing LED matrix", e);
        } finally {
            handler = null;
            handlerThread = null;
        }
    }
}
