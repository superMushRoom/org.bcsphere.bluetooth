package org.bcsphere.bluetooth;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;

import android.content.Context;

public interface BluetoothAPI {
	void setContext(Context context);
	void startScan(JSONArray json,CallbackContext callbackContext);
	void getScanData(JSONArray json,CallbackContext callbackContext) ;
	void stopScan(JSONArray json,CallbackContext callbackContext);
	void connect(JSONArray json,CallbackContext callbackContext) ;
	void disconnect(JSONArray json,CallbackContext callbackContext) ;
	void getConnectedDevices(JSONArray json ,CallbackContext callbackContext) ;
	void getPairedDevices(JSONArray json,CallbackContext callbackContext)  ;
	void createPair(JSONArray json, CallbackContext callbackContext)  ;
	void removePair(JSONArray json , CallbackContext callbackContext) ;
	void writeValue(JSONArray json,CallbackContext callbackContext) ;
	void readValue(JSONArray json,CallbackContext callbackContext)  ;
	void setNotification(JSONArray json,CallbackContext callbackContext)  ;
	void getDeviceAllData(JSONArray json , CallbackContext callbackContext)  ;
	void addServices(JSONArray json, CallbackContext callbackContext);
	void removeService(JSONArray json,CallbackContext callbackContext)  ;
	void getRssi(JSONArray json, CallbackContext callbackContext)  ;
	void getServices(JSONArray json, CallbackContext callbackContext)  ;
	void getCharacteristics(JSONArray json, CallbackContext callbackContext)  ;
	void getDescriptors(JSONArray json, CallbackContext callbackContext)  ;
	void openBluetooth(JSONArray json, CallbackContext callbackContext);
	void getBluetoothState(JSONArray json, CallbackContext callbackContext);
	void addEventListener(JSONArray json, CallbackContext callbackContext)  ;
	
}
