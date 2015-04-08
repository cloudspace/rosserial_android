package com.cloudspace.rosserial_android;

import android.app.PendingIntent;
import android.content.Context;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.cloudspace.rosserial_java.TopicRegistrationListener;

import org.ros.node.ConnectedNode;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import rosserial_msgs.TopicInfo;

public class ROSSerialADK {

    static final String TAG = "ROSSerialADK";
    public static final int ERROR_ACCESSORY_CANT_CONNECT = 0;
    public static final int ERROR_ACCESSORY_NOT_CONNECTED = 1;
    public static final int ERROR_UNKNOWN = 2;

    private ROSSerial rosserial;
    Thread ioThread;


    private Context mContext;
    private PendingIntent mPermissionIntent;
    boolean mPermissionRequestPending = false;

    UsbManager mUsbManager;
    UsbAccessory mAccessory;
    FileInputStream mInputStream;
    FileOutputStream mOutputStream;
    ParcelFileDescriptor mFileDescriptor;

    Handler errorHandler;

    private ConnectedNode node;

    public interface onConnectionListener {
        void trigger(boolean connection);
    }

    private onConnectionListener connectionCB;

    public void setOnConnectonListener(onConnectionListener onConnectionListener) {
        this.connectionCB = onConnectionListener;
    }


    public ROSSerialADK(Handler handler, Context context, ConnectedNode node, UsbAccessory accessory) throws IllegalStateException {
        errorHandler = handler;
        this.node = node;
        this.mContext = context;
        mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);

        if (!openAccessory(accessory)) {
            throw new IllegalStateException("Unable to open accessory.");
        }
    }

    private boolean openAccessory(UsbAccessory accessory) {
        Log.d(TAG, "Opening Accessory!");

        if (accessory != null) {
            mAccessory = accessory;

            mFileDescriptor = mUsbManager.openAccessory(mAccessory);

            if (mFileDescriptor != null && mFileDescriptor.getFileDescriptor() != null) {
                mInputStream = new FileInputStream(mFileDescriptor.getFileDescriptor());
                mOutputStream = new FileOutputStream(mFileDescriptor.getFileDescriptor());

                rosserial = new ROSSerial(errorHandler, node, mInputStream, mOutputStream);
                ioThread = new Thread(null, rosserial, "ROSSerialADK");
                ioThread.setContextClassLoader(ROSSerialADK.class.getClassLoader());
                ioThread.start();

                if (connectionCB != null) connectionCB.trigger(true);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }


    private void closeAccessory() {

        try {
            if (mFileDescriptor != null) {
                if (rosserial != null) {
                    rosserial.shutdown();
                    rosserial = null;
                }
                mFileDescriptor.close();
            }
        } catch (IOException e) {
        } finally {
            mFileDescriptor = null;
            mAccessory = null;
            if (connectionCB != null) connectionCB.trigger(false);

        }
    }

    public void shutdown() {
        closeAccessory();
    }

    public TopicInfo[] getSubscriptions() {
        return rosserial.getSubscriptions();
    }

    public TopicInfo[] getPublications() {
        return rosserial.getPublications();
    }

    //Set Callback function for new subscription
    public void setOnSubscriptionCB(TopicRegistrationListener listener) {
        if (rosserial != null) rosserial.setOnNewSubcription(listener);
    }

    //Set Callback for new publication
    public void setOnPublicationCB(TopicRegistrationListener listener) {
        if (rosserial != null) rosserial.setOnNewPublication(listener);
    }


    public static void sendError(Handler errorHandler, int errorCode, String message) {
        if (errorHandler != null) {
            Message error = new Message();
            error.what = errorCode;
            Bundle payload = new Bundle();
            payload.putString("error", message);
            error.setData(payload);
            errorHandler.sendMessage(error);
        }
    }
}