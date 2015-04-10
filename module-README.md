# ROSSerial Android
Android specific components of ROSSerial.

ROSSerial is used to set up a bridge between the non network enabled Arduino, and the Ros connected Android device.

Usage is as follows.
```
try {
    adk = new ROSSerialADK(errorHandler, ExternalCoreActivity.this, connectedNode, mAccessory);
} catch (Exception e) {
    ROSSerialADK.sendError(errorHandler, ROSSerialADK.ERROR_ACCESSORY_CANT_CONNECT, e.getMessage());
    return;
}
adk.setOnSubscriptionCB(topicRegisteredListener);
adk.setOnSubscriptionCB(topicRegisteredListener);
```

Where errorHandler is null, or a Handler object setup to receive errors as follows
```
    Handler errorHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (isActive()) {
                if (msg.getData() != null && msg.getData().get("error") != null) {
                    String title;
                    String message = (String) msg.getData().get("error");
                    switch (msg.what) {
                        case ROSSerialADK.ERROR_ACCESSORY_CANT_CONNECT:
                            title = "Unable to connect";
                            break;
                        case ROSSerialADK.ERROR_ACCESSORY_NOT_CONNECTED:
                            title = "Unable to communicate";
                            break;
                        default:
                        case ROSSerialADK.ERROR_UNKNOWN:
                            title = "Unknown error";
                            break;
                    }
                    if (errorDialog == null) {
                        errorDialog = new AlertDialog.Builder(ExternalCoreActivity.this).setTitle(title).setMessage(message).create();
                    } else {
                        errorDialog.setTitle(title);
                        errorDialog.setMessage(message);
                    }
                    errorDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                            startActivity(new Intent(ExternalCoreActivity.this, AccessoryActivity.class));
                        }
                    });
                    errorDialog.show();
                }
            }
        }
    };
    ```

=====

