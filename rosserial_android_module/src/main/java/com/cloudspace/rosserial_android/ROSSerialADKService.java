package com.cloudspace.rosserial_android;

import android.app.Service;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import org.ros.node.ConnectedNode;


public class ROSSerialADKService extends Service {
    static final String TAG = "ROSSerialSDKService";

    /**
     * ROS node name
     */
    private String node_name;

    /**
     * ROS node handle
     */
    ConnectedNode nh;

    /**
     * Reference to ADK
     */
    ROSSerialADK adk = null;

    /**
     * IBinder to connect to ADK Service
     */
    private IBinder mBinder;

    /**
     * Binder class for connecting to ADK service
     * Assumes that everything is in the same processes
     * and returns a copy of the ROSSerialADK
     *
     * @author Adam Stambler
     */
    public class LocalBinder extends Binder {
        ROSSerialADKService service;

        public LocalBinder(ROSSerialADKService s) {
            service = s;
        }

        public ROSSerialADK getADK() {
            return adk;
        }

        public ROSSerialADK setConnectedNode(ConnectedNode node, UsbAccessory accessory) {
            if (accessory == null) {
                Toast.makeText(ROSSerialADKService.this, "NULL ACCESSORY", Toast.LENGTH_LONG).show();
                return null;
            }
            nh = node;
            adk = new ROSSerialADK(ROSSerialADKService.this, nh, accessory);
            return adk;
        }

        ROSSerialADKService getService() {
            return service;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    /**
     * Starts up Service
     * Creates ROS node and registers with master
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null || intent.getExtras() == null || !intent.getExtras().containsKey("ROS_MASTER_URI")) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopSelf();
                }
            }, 3000);
            return START_STICKY;
        }
        String master_uri = intent.getExtras().getString("ROS_MASTER_URI");
        node_name = intent.getExtras().getString("name");
        if (node_name == null) node_name = "ROSSerialADK";

        Toast.makeText(this, "Starting ROSSerialADKService as node '" + node_name + "' with master '" + master_uri + "@" + System.currentTimeMillis(), Toast.LENGTH_LONG).show();

        mBinder = new LocalBinder(ROSSerialADKService.this);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (adk != null) this.adk.shutdown();
        if (nh != null) this.nh.shutdown();
        Toast t = Toast.makeText(this, node_name + " stopped.", Toast.LENGTH_LONG);
        t.show();

        super.onDestroy();
    }

    public ROSSerialADKService() {
        super();
    }

}