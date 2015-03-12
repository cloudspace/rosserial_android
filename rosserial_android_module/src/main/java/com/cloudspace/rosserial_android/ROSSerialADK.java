package com.cloudspace.rosserial_android;

import android.app.PendingIntent;
import android.content.Context;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import com.cloudspace.rosserial_java.*;

import org.ros.node.ConnectedNode;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import rosserial_msgs.TopicInfo;

public class ROSSerialADK {

    static final String TAG = "ROSSerialADK";


    private com.cloudspace.rosserial_java.ROSSerial rosserial;
    Thread ioThread;


    private Context mContext;
    private PendingIntent mPermissionIntent;
    boolean mPermissionRequestPending = false;

    UsbManager mUsbManager;
    UsbAccessory mAccessory;
    FileInputStream mInputStream;
    FileOutputStream mOutputStream;
    ParcelFileDescriptor mFileDescriptor;
    
    private ConnectedNode node;


    public interface onConnectionListener {
        public void trigger(boolean connection);
    }

    private onConnectionListener connectionCB;

    public void setOnConnectonListener(onConnectionListener onConnectionListener) {
        this.connectionCB = onConnectionListener;
    }


    public ROSSerialADK(Context context, ConnectedNode node, UsbAccessory accessory) {

        this.node = node;
        this.mContext = context;
        mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        openAccessory(accessory);
    }

    public ROSSerialADK(Context context, ConnectedNode node, UsbAccessory accessory, ParcelFileDescriptor fileDescriptor, FileInputStream input, FileOutputStream output) {

        this.node = node;
        this.mContext = context;
        mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        mFileDescriptor = fileDescriptor;
        mInputStream = input;
        mOutputStream = output;
        mAccessory = accessory;
        
        rosserial = new com.cloudspace.rosserial_java.ROSSerial(node, mInputStream, mOutputStream);
        ioThread = new Thread(null, rosserial, "ROSSerialADK");
        ioThread.setContextClassLoader(ROSSerialADK.class.getClassLoader());
        ioThread.start();

        if (connectionCB != null) connectionCB.trigger(true);
        Toast.makeText(mContext, "accessory opened", Toast.LENGTH_LONG).show();
                
    }

           
    private boolean openAccessory(UsbAccessory accessory) {
        Log.d(TAG, "Opening Accessory!");

        if (accessory != null) {
            mAccessory = accessory;

            mFileDescriptor = mUsbManager.openAccessory(mAccessory);

            mInputStream = new FileInputStream(mFileDescriptor.getFileDescriptor());
            mOutputStream = new FileOutputStream(mFileDescriptor.getFileDescriptor());

            rosserial = new com.cloudspace.rosserial_java.ROSSerial(node, mInputStream, mOutputStream);
            ioThread = new Thread(null, rosserial, "ROSSerialADK");
            ioThread.setContextClassLoader(ROSSerialADK.class.getClassLoader());
            ioThread.start();

            if (connectionCB != null) connectionCB.trigger(true);
            Toast.makeText(mContext, "accessory opened", Toast.LENGTH_LONG).show();
            return true;

        } else {
            Toast.makeText(mContext, "accessory open fail", Toast.LENGTH_LONG).show();
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

    public boolean isConnected() {
        return (mOutputStream != null);
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

}