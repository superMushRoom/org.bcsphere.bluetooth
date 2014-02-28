package org.bcsphere.bluetooth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONObject;

import org.bcsphere.bluetooth.tools.Tools;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

@SuppressLint("NewApi")
public class BluetoothGoogleAPI implements BluetoothAPI{

	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothGattServer mBluetoothGattServer;
	private Context mContext;
	private boolean isScanning = false;
	private List<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();;
	private JSONArray deviceInfoList = new JSONArray() ;
	private int scanSum = 0;
	private boolean isOpenGattServer = false;
	private int gattServerSum = 0;
	private HashMap<String, CallbackContext> connectCC = new HashMap<String, CallbackContext>();
	private HashMap<String, CallbackContext> disconnectCC = new HashMap<String, CallbackContext>();
	private HashMap<String, CallbackContext> getServicesCC = new HashMap<String, CallbackContext>();
	private HashMap<String, CallbackContext> writeValueCC = new HashMap<String, CallbackContext>();
	private HashMap<String, CallbackContext> readValueCC = new HashMap<String, CallbackContext>();
	private HashMap<BluetoothGattCharacteristic, CallbackContext> setNotificationCC = new HashMap<BluetoothGattCharacteristic, CallbackContext>();
	private HashMap<String, CallbackContext> getDeviceAllDataCC = new HashMap<String, CallbackContext>();
	private HashMap<String ,CallbackContext> getRssiCC = new HashMap<String, CallbackContext>();
	private HashMap<String, CallbackContext> addEventListenerCC = new HashMap<String, CallbackContext>();
	private CallbackContext addServiceCC;
	private HashMap<String, BluetoothGattService> localServices = new HashMap<String, BluetoothGattService>();
	private HashMap<String, Integer> recordServiceIndex = new HashMap<String, Integer>();
	private HashMap<String, Integer> recordCharacteristicIndex = new HashMap<String, Integer>();
	private HashMap< String ,Boolean> connectedDevice = new HashMap<String, Boolean>(); 
	private HashMap<String, BluetoothGatt> mBluetoothGatts = new HashMap<String, BluetoothGatt>();
	private HashMap<String, List<BluetoothGattService>> deviceServices = new HashMap<String, List<BluetoothGattService>>();
	@Override
	public void setContext(Context context) {
		// TODO Auto-generated method stub
		this.mContext = context;
		mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = mBluetoothManager.getAdapter();
	}

	@Override
	public void startScan(JSONArray json, CallbackContext callbackContext) {
		// TODO Auto-generated method stub
		if (isScanning) {
			Tools.sendSuccessMsg(callbackContext);
			scanSum = scanSum + 1;
			return;
		}
		if (deviceList != null) {
			deviceList = null;
			deviceList = new ArrayList<BluetoothDevice>();
		}
		if (deviceInfoList != null) {
			deviceInfoList = null;
			deviceInfoList = new JSONArray();
		}
		if (Tools.getData(json, Tools.SERVICE_UUIDS) == null) {
			mBluetoothAdapter.startLeScan(mLeScanCallback);
			Tools.sendSuccessMsg(callbackContext);
			scanSum = scanSum + 1;
			isScanning = true;
		}else {
			mBluetoothAdapter.startLeScan(Tools.getUUIDs(json), mLeScanCallback);
			Tools.sendSuccessMsg(callbackContext);
			scanSum = scanSum + 1;	
			isScanning = true;
		}
	}

	@Override
	public void getScanData(JSONArray json, CallbackContext callbackContext) {
		// TODO Auto-generated method stub
		if (!isScanning) {
			Tools.sendErrorMsg(callbackContext);
			return;
		}
		callbackContext.success(deviceInfoList);
	}

	@Override
	public void stopScan(JSONArray json, CallbackContext callbackContext) {
		// TODO Auto-generated method stub
		if (!isScanning) {
			Tools.sendSuccessMsg(callbackContext);
			return;
		}
		scanSum = scanSum - 1;
		if (scanSum == 0 ) {
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
			isScanning = false;
			Tools.sendSuccessMsg(callbackContext);
		}else {
			Tools.sendSuccessMsg(callbackContext);
		}
	}

	@Override
	public void connect(JSONArray json, CallbackContext callbackContext) {
		// TODO Auto-generated method stub
		String deviceID = Tools.getData(json, Tools.DEVICE_ID);
		if (connectedDevice.get(deviceID) != null) {
			Tools.sendSuccessMsg(callbackContext);
			return;
		}
		connectCC.put(deviceID, callbackContext);
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceID);
		mBluetoothGatts.put(device.getAddress(), device.connectGatt(mContext, false, mGattCallback));
	}

	@Override
	public void disconnect(JSONArray json, CallbackContext callbackContext) {
		// TODO Auto-generated method stub
		String deviceID = Tools.getData(json, Tools.DEVICE_ID);
		if (connectedDevice.get(deviceID) == null) {
			Tools.sendSuccessMsg(callbackContext);
			return;
		}
		disconnectCC.put(deviceID, callbackContext);
		mBluetoothGatts.get(deviceID).disconnect();
	}

	@Override
	public void getConnectedDevices(JSONArray json,
			CallbackContext callbackContext) {
		// TODO Auto-generated method stub
		JSONArray ary = new JSONArray();
		List<BluetoothDevice> devices = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
		for (int i = 0; i < devices.size(); i++) {
			JSONObject obj = new JSONObject();
			Tools.addProperty(obj, Tools.DEVICE_ID, devices.get(i).getAddress());
			Tools.addProperty(obj, Tools.DEVICE_NAME, devices.get(i).getName());
			ary.put(obj);
		}
		callbackContext.success(ary);
	}

	@Override
	public void getPairedDevices(JSONArray json, CallbackContext callbackContext) {
		// TODO Auto-generated method stub
		JSONArray ary = new JSONArray();
		Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
		Iterator<BluetoothDevice> it = devices.iterator();
		while (it.hasNext()) {
			BluetoothDevice device = (BluetoothDevice) it.next();
			JSONObject obj = new JSONObject();
			Tools.addProperty(obj, Tools.DEVICE_ID, device.getAddress());
			Tools.addProperty(obj, Tools.DEVICE_NAME, device.getName());
			ary.put(obj);
		}
		callbackContext.success(ary);
	}

	@Override
	public void createPair(JSONArray json, CallbackContext callbackContext) {
		// TODO Auto-generated method stub
		String deviceID = Tools.getData(json, Tools.DEVICE_ID);
		JSONObject obj = new JSONObject();
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceID);
		try {
			if (Tools.creatBond(device.getClass(), device)) {
				Tools.addProperty(obj, Tools.DEVICE_ID, device.getAddress());
				callbackContext.success(obj);
			}else {
				Tools.addProperty(obj, Tools.DEVICE_ID, device.getAddress());
				callbackContext.error(obj);
			}
		} catch (Exception e) {

		}
	}

	@Override
	public void removePair(JSONArray json, CallbackContext callbackContext) {
		// TODO Auto-generated method stub
		String deviceID = Tools.getData(json, Tools.DEVICE_ID);
		JSONObject obj = new JSONObject();
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceID);
		try {
			if (Tools.removeBond(device.getClass(), device)) {
				Tools.addProperty(obj, Tools.DEVICE_ID, device.getAddress());
				callbackContext.success(obj);
			}else {
				Tools.addProperty(obj, Tools.DEVICE_ID, device.getAddress());
				callbackContext.error(obj);
			}
		} catch (Exception e) {

		}
	}

	@Override
	public void writeValue(JSONArray json, CallbackContext callbackContext) {
		// TODO Auto-generated method stub
		String deviceID = Tools.getData(json, Tools.DEVICE_ID);
		if (connectedDevice.get(deviceID) == null) {
			Tools.sendErrorMsg(callbackContext);
			return;
		}
		int serviceIndex = Integer.parseInt(Tools.getData(json, Tools.SERVICE_INDEX));
		int characteristicIndex = Integer.parseInt(Tools.getData(json, Tools.CHARACTERISTIC_INDEX));
		String  descriptorIndex =Tools.getData(json, Tools.DESCRIPTOR_INDEX);
		String writeValue = Tools.getData(json, Tools.WRITE_VALUE);
		String writeType = Tools.getData(json, Tools.WRITE_TYPE);
		writeValueCC.put(deviceID, callbackContext);
		if (descriptorIndex.equals("")) {
			BluetoothGattCharacteristic characteristic = deviceServices.get(deviceID).get(serviceIndex)
					.getCharacteristics().get(characteristicIndex);
			characteristic.setValue(Tools.parsingCodingFormat(writeValue, writeType));
			mBluetoothGatts.get(deviceID).writeCharacteristic(characteristic);
		}else {
			BluetoothGattDescriptor descriptor = deviceServices.get(deviceID).get(serviceIndex).getCharacteristics()
					.get(characteristicIndex).getDescriptors().get(Integer.parseInt(descriptorIndex));
			descriptor.setValue(Tools.parsingCodingFormat(writeValue, writeType));
			mBluetoothGatts.get(deviceID).writeDescriptor(descriptor);
		}
	} 

	@Override
	public void readValue(JSONArray json, CallbackContext callbackContext) {
		// TODO Auto-generated method stub
		String deviceID = Tools.getData(json, Tools.DEVICE_ID);
		if (connectedDevice.get(deviceID) == null ) {
			Tools.sendErrorMsg(callbackContext);
			return;
		}
		int serviceIndex = Integer.parseInt(Tools.getData(json, Tools.SERVICE_INDEX));
		int characteristicIndex = Integer.parseInt(Tools.getData(json, Tools.CHARACTERISTIC_INDEX));
		String  descriptorIndex =Tools.getData(json, Tools.DESCRIPTOR_INDEX);
		readValueCC.put(deviceID, callbackContext);
		if (descriptorIndex.equals("")) {
			mBluetoothGatts.get(deviceID).readCharacteristic(deviceServices.get(deviceID).get(serviceIndex)
					.getCharacteristics().get(characteristicIndex));
		}else {
			mBluetoothGatts.get(deviceID).readDescriptor(deviceServices.get(deviceID).get(serviceIndex)
					.getCharacteristics().get(characteristicIndex).getDescriptors().get(Integer.parseInt(descriptorIndex)));
		}
	}

	@Override
	public void setNotification(JSONArray json, CallbackContext callbackContext) {
		// TODO Auto-generated method stub
		String deviceID = Tools.getData(json, Tools.DEVICE_ID);
		if (connectedDevice.get(deviceID) == null ) {
			Tools.sendErrorMsg(callbackContext);
			return;
		}
		int serviceIndex = Integer.parseInt(Tools.getData(json, Tools.SERVICE_INDEX));
		int characteristicIndex = Integer.parseInt(Tools.getData(json, Tools.CHARACTERISTIC_INDEX));
		String enable = Tools.getData(json, Tools.ENABLE);
		BluetoothGattCharacteristic characteristic = deviceServices.get(deviceID).get(serviceIndex).getCharacteristics()
				.get(characteristicIndex);
		BluetoothGattDescriptor descriptor = characteristic.getDescriptor(Tools.NOTIFICATION_UUID);
		if (enable.equals("true")) {
			setNotificationCC.put(characteristic, callbackContext);
			descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			mBluetoothGatts.get(deviceID).writeDescriptor(descriptor);
			mBluetoothGatts.get(deviceID).setCharacteristicNotification(characteristic, true);
			recordServiceIndex.put(deviceID, serviceIndex);
			recordCharacteristicIndex.put(deviceID, characteristicIndex);
		}else {
			descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
			mBluetoothGatts.get(deviceID).writeDescriptor(descriptor);
			mBluetoothGatts.get(deviceID).setCharacteristicNotification(characteristic, false);
			Tools.sendSuccessMsg(setNotificationCC.get(characteristic));
			setNotificationCC.remove(characteristic);
			recordServiceIndex.remove(deviceID);
			recordCharacteristicIndex.remove(deviceID);
		}
	}

	@Override
	public void getDeviceAllData(JSONArray json, CallbackContext callbackContext) {
		// TODO Auto-generated method stub
		String deviceID = Tools.getData(json, Tools.DEVICE_ID);
		if (connectedDevice.get(deviceID) == null) {
			Tools.sendErrorMsg(callbackContext);
			return;
		}
		getDeviceAllDataCC.put(deviceID, callbackContext);
		mBluetoothGatts.get(deviceID).discoverServices();
	}


	@Override
	public void removeService(JSONArray json, CallbackContext callbackContext) {
		// TODO Auto-generated method stub
		String uniqueID = Tools.getData(json, Tools.UINQUE_ID);
		if (uniqueID.equals("")) {
			mBluetoothGattServer.clearServices();
			mBluetoothGattServer.close();
			isOpenGattServer = false;
			Tools.sendSuccessMsg(callbackContext);
		}else {
			if (mBluetoothGattServer.removeService(localServices.get(uniqueID))) {
				Tools.sendSuccessMsg(callbackContext);
			}else {
				Tools.sendErrorMsg(callbackContext);
			}
		}
	}

	@Override
	public void getRssi(JSONArray json, CallbackContext callbackContext) {
		// TODO Auto-generated method stub
		String deviceID = Tools.getData(json, Tools.DEVICE_ID);
		if (connectedDevice.get(deviceID) == null) {
			Tools.sendErrorMsg(callbackContext);
			return;
		}
		getRssiCC.put(deviceID, callbackContext);
		mBluetoothGatts.get(deviceID).readRemoteRssi();
	}

	@Override
	public void getServices(JSONArray json, CallbackContext callbackContext) {
		// TODO Auto-generated method stub
		String deviceID = Tools.getData(json, Tools.DEVICE_ID);
		if (connectedDevice.get(deviceID) == null ) {
			Tools.sendErrorMsg(callbackContext);
			return;
		}
		mBluetoothGatts.get(deviceID).discoverServices();
		getServicesCC.put(deviceID, callbackContext);
	}

	@Override
	public void getCharacteristics(JSONArray json,
			CallbackContext callbackContext) {
		// TODO Auto-generated method stub
		String deviceID = Tools.getData(json, Tools.DEVICE_ID);
		if (connectedDevice.get(deviceID) == null) {
			Tools.sendErrorMsg(callbackContext);
			return;
		}
		JSONObject obj = new JSONObject();
		JSONArray ary = new JSONArray();
		int serviceIndex = Integer.parseInt(Tools.getData(json, Tools.SERVICE_INDEX));
		Tools.addProperty(obj, Tools.DEVICE_ID, deviceID);
		List<BluetoothGattCharacteristic> characteristics = deviceServices.get(deviceID).get(serviceIndex).getCharacteristics();
		for (int i = 0; i < characteristics.size(); i++) {
			JSONObject infoObj = new JSONObject();
			Tools.addProperty(infoObj, Tools.CHARACTERISTIC_INDEX, i);
			Tools.addProperty(infoObj, Tools.CHARACTERISTIC_UUID, characteristics.get(i).getUuid());
			Tools.addProperty(infoObj, Tools.CHARACTERISTIC_NAME, Tools.lookup(characteristics.get(i).getUuid()));
			Tools.addProperty(infoObj, Tools.CHARACTERISTIC_PROPERTY, Tools.decodeProperty(characteristics.get(i).getProperties()));
			ary.put(infoObj);
		}
		Tools.addProperty(obj, Tools.CHARACTERISTICS, ary);
		callbackContext.success(obj);
	}

	@Override
	public void getDescriptors(JSONArray json, CallbackContext callbackContext) {
		// TODO Auto-generated method stub
		String deviceID = Tools.getData(json, Tools.DEVICE_ID);
		if (connectedDevice.get(deviceID) == null) {
			Tools.sendErrorMsg(callbackContext);
			return;
		}
		JSONObject obj = new JSONObject();
		JSONArray ary = new JSONArray();
		int serviceIndex = Integer.parseInt(Tools.getData(json, Tools.SERVICE_INDEX));
		int characteristicIndex = Integer.parseInt(Tools.getData(json, Tools.CHARACTERISTIC_INDEX));
		List<BluetoothGattDescriptor> descriptors = deviceServices.get(deviceID).get(serviceIndex).getCharacteristics().get(characteristicIndex).getDescriptors();
		for (int i = 0; i < descriptors.size(); i++) {
			JSONObject infoObj = new JSONObject();
			Tools.addProperty(infoObj, Tools.DESCRIPTOR_INDEX, i);
			Tools.addProperty(infoObj, Tools.DESCRIPTOR_UUID, descriptors.get(i).getUuid());
			Tools.addProperty(infoObj, Tools.DESCRIPTOR_NAME, Tools.lookup(descriptors.get(i).getUuid()));
			ary.put(infoObj);
		}
		Tools.addProperty(obj, Tools.DEVICE_ID, deviceID);
		Tools.addProperty(obj, Tools.DESCRIPTORS, ary);
		callbackContext.success(obj);
	}

	@Override
	public void openBluetooth(JSONArray json, CallbackContext callbackContext) {
		// TODO Auto-generated method stub
		mBluetoothAdapter.enable();
		Tools.sendSuccessMsg(callbackContext);
	}

	@Override
	public void getBluetoothState(JSONArray json,
			CallbackContext callbackContext) {
		// TODO Auto-generated method stub
		JSONObject obj = new JSONObject();
		if (mBluetoothAdapter.isEnabled()) {
			Tools.addProperty(obj, Tools.BLUETOOTH_STATE, Tools.BLUETOOTH_OPEN);
			callbackContext.success(obj);
		}else {
			Tools.addProperty(obj, Tools.BLUETOOTH_STATE, Tools.BLUETOOTH_CLOSE);
			callbackContext.success(obj);
		}
	}

	@Override
	public void addEventListener(JSONArray json, CallbackContext callbackContext) {
		// TODO Auto-generated method stub
		String eventName = Tools.getData(json, Tools.EVENT_NAME);
		if (eventName == null) {
			Tools.sendErrorMsg(callbackContext);
			return;
		}
		addEventListenerCC.put(eventName, callbackContext);
	}

	@Override
	public void addServices(JSONArray json, CallbackContext callbackContext) {
		// TODO Auto-generated method stub
		if (!isOpenGattServer) {
			mBluetoothGattServer = mBluetoothManager.openGattServer(mContext, mGattServerCallback);
			isOpenGattServer = true;
		}
		addServiceCC = callbackContext;
		JSONArray services  = Tools.getArray(json, Tools.SERVICES);
		gattServerSum = services.length();
		for (int i = 0; i < services.length(); i++) {
			String uniqueID = Tools.getData(services, i, Tools.UINQUE_ID);
			int serviceType = -1;
			if (Tools.getData(services, i , Tools.SERVICE_TYPE).equals("0")) {
				serviceType = BluetoothGattService.SERVICE_TYPE_PRIMARY;
			}else {
				serviceType = BluetoothGattService.SERVICE_TYPE_SECONDARY;
			}
			UUID serviceUUID = UUID.fromString(Tools.getData(services, i , Tools.SERVICE_UUID));
			BluetoothGattService service =  new BluetoothGattService(serviceUUID, serviceType);
			JSONArray characteristics = Tools.getArray(services, i, Tools.CHARACTERISTICS);
			for (int j = 0; j <characteristics.length(); j++) {
				String characteristicValueType = Tools.getData(characteristics, Tools.CHARACTERISTIC_VALUE_TYPE);
				byte[] characteristicValue = Tools.parsingCodingFormat(Tools.getData(characteristics, Tools.CHARACTERISTIC_VALUE), characteristicValueType);
				UUID characteristicUUID = UUID.fromString(Tools.getData(characteristics, Tools.CHARACTERISTIC_UUID));
				int characteristicProperty = Tools.encodeProperty(Tools.getArray(characteristics, Tools.CHARACTERISTIC_PROPERTY));
				int characteristicPermission = Tools.encodePermission(Tools.getArray(characteristics, Tools.CHARACTERISTIC_PERMISSION));
				BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(characteristicUUID, characteristicProperty, characteristicPermission);
				characteristic.setValue(characteristicValue);
				JSONArray descriptors = Tools.getArray(characteristics, j, Tools.DESCRIPTORS);
				for (int k = 0; k < descriptors.length(); k++) {
					String descriptorValueType = Tools.getData(descriptors, Tools.DESCRIPTOR_VALUE_TYPE);
					byte[] descriptorValue = Tools.parsingCodingFormat(Tools.getData(descriptors, Tools.DESCRIPTOR_VALUE), descriptorValueType);
					UUID descriptorUUID = UUID.fromString(Tools.getData(descriptors, Tools.DESCRIPTOR_UUID));
					int descriptorPermission = Tools.encodePermission(Tools.getArray(descriptors, Tools.DESCRIPTOR_PERMISSION));
					BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(descriptorUUID, descriptorPermission);
					descriptor.setValue(descriptorValue);
					characteristic.addDescriptor(descriptor);
				}
				service.addCharacteristic(characteristic);
			}
			if (mBluetoothGattServer.addService(service)) {
				localServices.put(uniqueID, service);
			}
		}
	}


	private BluetoothAdapter.LeScanCallback mLeScanCallback = new LeScanCallback() {

		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			// TODO Auto-generated method stub
			startScanManage(device, rssi, scanRecord);
		}
	};

	private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			// TODO Auto-generated method stub
			super.onCharacteristicChanged(gatt, characteristic);
			setNotificationManage(gatt, characteristic);
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			// TODO Auto-generated method stub
			super.onCharacteristicRead(gatt, characteristic, status);
			readValueManage( gatt, characteristic, status);
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			// TODO Auto-generated method stub
			super.onCharacteristicWrite(gatt, characteristic, status);
			writeValueManage(gatt,status);
		}

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
				int newState) {
			// TODO Auto-generated method stub
			super.onConnectionStateChange(gatt, status, newState);
			conncetManage(gatt,newState );
			disconnectManage(gatt,newState);
			addEventListenerManage(gatt ,newState);
		}

		@Override
		public void onDescriptorRead(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status) {
			// TODO Auto-generated method stub
			super.onDescriptorRead(gatt, descriptor, status);
			readValueManage( gatt, descriptor, status);
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status) {
			// TODO Auto-generated method stub
			super.onDescriptorWrite(gatt, descriptor, status);
			writeValueManage(gatt,status);
		}

		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
			// TODO Auto-generated method stub
			super.onReadRemoteRssi(gatt, rssi, status);
			getRssiManage(gatt , rssi ,status);
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			// TODO Auto-generated method stub
			super.onServicesDiscovered(gatt, status);
			getServicesManage(gatt , status);
			getDeviceAllDataManage(gatt, status);
		}
	};

	private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

		@Override
		public void onServiceAdded(int status, BluetoothGattService service) {
			// TODO Auto-generated method stub
			super.onServiceAdded(status, service);
			addServiceManage(status);
		}
	};


	private void startScanManage(BluetoothDevice device , int rssi , byte[] scanRecord){
		if (!deviceList.contains(device)) {
			deviceList.add(device);
			JSONObject obj = new JSONObject();
			Tools.addProperty(obj, Tools.DEVICE_ID, device.getAddress());
			Tools.addProperty(obj, Tools.DEVICE_NAME, device.getName());
			Tools.addProperty(obj, Tools.IS_CONNECTED, Tools.IS_FALSE);
			Tools.addProperty(obj, Tools.RSSI, rssi);
			Tools.addProperty(obj, Tools.ADVERTISEMENT_DATA, Tools.decodeAdvData(scanRecord));
			deviceInfoList.put(obj);
		}
	}

	private void conncetManage(BluetoothGatt gatt , int newState){
		String deviceID = getDeviceAddress(gatt);
		JSONObject obj = new JSONObject();
		if (connectCC.get(deviceID) != null) {
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				Tools.addProperty(obj, Tools.DEVICE_ID, deviceID);
				connectCC.get(deviceID).success(obj);
				connectCC.remove(deviceID);
				connectedDevice.put(deviceID, true);
			}else{
				Tools.addProperty(obj, Tools.DEVICE_ID, deviceID);
				connectCC.get(deviceID).error(obj);
				connectCC.remove(deviceID);
			}
		}
	}

	private void disconnectManage(BluetoothGatt gatt , int newStatus){
		String deviceID = getDeviceAddress(gatt);
		JSONObject obj = new JSONObject();
		if (disconnectCC.get(deviceID) != null) {
			if (newStatus ==  BluetoothProfile.STATE_DISCONNECTED) {
				Tools.addProperty(obj, Tools.DEVICE_ID, getDeviceAddress(gatt));
				disconnectCC.get(deviceID).success(obj);
				disconnectCC.remove(deviceID);
				connectedDevice.remove(deviceID);
				if (deviceServices.get(deviceID) != null) {
					deviceServices.remove(deviceID);
				}
			}else {
				Tools.addProperty(obj, Tools.DEVICE_ID, deviceID);
				disconnectCC.get(deviceID).error(obj);
				disconnectCC.remove(deviceID);
			}
		}
	}

	private void getServicesManage(BluetoothGatt gatt , int status){
		String deviceID = getDeviceAddress(gatt);
		JSONObject obj = new JSONObject();
		JSONArray ary = new JSONArray();
		if (getServicesCC.get(deviceID) !=null) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				if (deviceServices.get(deviceID)!=null) {
					deviceServices.get(deviceID).remove(deviceID);
					deviceServices.put(deviceID, gatt.getServices());
				}
				Tools.addProperty(obj, Tools.DEVICE_ID, deviceID);
				for (int i = 0; i <deviceServices.get(deviceID).size(); i++) {
					JSONObject infoObj = new JSONObject();
					Tools.addProperty(infoObj, Tools.SERVICE_INDEX, i);
					Tools.addProperty(infoObj, Tools.SERVICE_UUID, deviceServices.get(deviceID).get(i).getUuid());
					Tools.addProperty(infoObj, Tools.SERVICE_NAME, Tools.lookup(deviceServices.get(deviceID).get(i).getUuid()));
					ary.put(infoObj);
				}
				Tools.addProperty(obj, Tools.SERVICES, ary);
				getServicesCC.get(deviceID).success(obj);
				getServicesCC.remove(deviceID);
			}else {
				Tools.sendErrorMsg(getServicesCC.get(deviceID));
				getServicesCC.remove(deviceID);
			}
		}
	}

	private void writeValueManage(BluetoothGatt gatt , int status){
		String deviceID = getDeviceAddress(gatt);
		if (writeValueCC.get(deviceID) != null) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Tools.sendSuccessMsg(writeValueCC.get(deviceID));
				writeValueCC.remove(deviceID);
			}else {
				Tools.sendErrorMsg(writeValueCC.get(deviceID));
				writeValueCC.remove(deviceID);
			}
		}
	}

	private void readValueManage(BluetoothGatt gatt ,BluetoothGattCharacteristic characteristic ,int status){
		String deviceID = getDeviceAddress(gatt);
		JSONObject obj = new JSONObject();
		if (readValueCC.get(deviceID) != null) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Tools.addProperty(obj, Tools.DEVICE_ID, deviceID);
				Tools.addProperty(obj, Tools.VALUE, Tools.encodeBase64(characteristic.getValue()));
				Tools.addProperty(obj, Tools.DATE, Tools.getDateString());
				readValueCC.get(deviceID).success(obj);
				readValueCC.remove(deviceID);
			}else {
				Tools.sendErrorMsg(readValueCC.get(deviceID));
				readValueCC.remove(deviceID);
			}
		}
	}

	private void readValueManage(BluetoothGatt gatt ,BluetoothGattDescriptor descriptor ,int status){
		String deviceID = getDeviceAddress(gatt);
		JSONObject obj = new JSONObject();
		if (readValueCC.get(deviceID) != null) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Tools.addProperty(obj, Tools.DEVICE_ID, deviceID);
				Tools.addProperty(obj, Tools.VALUE, Tools.encodeBase64(descriptor.getValue()));
				Tools.addProperty(obj, Tools.DATE, Tools.getDateString());
				readValueCC.get(deviceID).success(obj);
				readValueCC.remove(deviceID);
			}else {
				Tools.sendErrorMsg(readValueCC.get(deviceID));
				readValueCC.remove(deviceID);
			}
		}
	}

	private void setNotificationManage(BluetoothGatt gatt , BluetoothGattCharacteristic characteristic){
		String deviceID = getDeviceAddress(gatt);
		if (setNotificationCC.get(characteristic) != null) {
			JSONObject obj = new JSONObject();
			Tools.addProperty(obj, Tools.DEVICE_ID, deviceID);
			Tools.addProperty(obj, Tools.SERVICE_INDEX, recordServiceIndex.get(deviceID));
			Tools.addProperty(obj, Tools.CHARACTERISTIC_INDEX, recordCharacteristicIndex.get(deviceID));
			Tools.addProperty(obj, Tools.VALUE, Tools.encodeBase64(characteristic.getValue()));
			Tools.addProperty(obj, Tools.DATE, Tools.getDateString());
			PluginResult pluginResult = new PluginResult(PluginResult.Status.OK , obj);
			pluginResult.setKeepCallback(true);
			setNotificationCC.get(characteristic).sendPluginResult(pluginResult);
		}
	}

	private void getDeviceAllDataManage(BluetoothGatt gatt , int status){
		String deviceID =  getDeviceAddress(gatt);
		if (getDeviceAllDataCC.get(deviceID) != null) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				JSONObject obj = new JSONObject();
				JSONArray servicesInfo = new JSONArray();
				List<BluetoothGattService> services = gatt.getServices();
				for (int i = 0; i < services.size(); i++) {
					JSONObject serviceInfo = new JSONObject();
					Tools.addProperty(serviceInfo, Tools.SERVICE_INDEX, i);
					Tools.addProperty(serviceInfo, Tools.SERVICE_UUID, services.get(i).getUuid());
					Tools.addProperty(serviceInfo, Tools.SERVICE_NAME, Tools.lookup(services.get(i).getUuid()));
					List<BluetoothGattCharacteristic>  characteristics = services.get(i).getCharacteristics();
					JSONArray characteristicsInfo = new JSONArray();
					for (int j = 0; j < characteristics.size(); j++) {
						JSONObject characteristicInfo = new JSONObject();
						Tools.addProperty(characteristicInfo, Tools.CHARACTERISTIC_INDEX, j);
						Tools.addProperty(characteristicInfo, Tools.CHARACTERISTIC_UUID, characteristics.get(j).getUuid());
						Tools.addProperty(characteristicInfo, Tools.CHARACTERISTIC_NAME,Tools.lookup(characteristics.get(j).getUuid()));
						Tools.addProperty(characteristicInfo, Tools.CHARACTERISTIC_PROPERTY, Tools.decodeProperty(characteristics.get(j).getProperties()));
						List<BluetoothGattDescriptor> descriptors = new ArrayList<BluetoothGattDescriptor>();
						JSONArray descriptorsInfo = new JSONArray();
						for (int k = 0; k < descriptors.size(); k++) {
							JSONObject descriptorInfo = new JSONObject();
							Tools.addProperty(descriptorInfo, Tools.DESCRIPTOR_INDEX, k);
							Tools.addProperty(descriptorInfo, Tools.DESCRIPTOR_UUID, descriptors.get(k).getUuid());
							Tools.addProperty(descriptorInfo, Tools.DESCRIPTOR_NAME, Tools.lookup(descriptors.get(k).getUuid()));
							descriptorsInfo.put(descriptorInfo);
						}
						Tools.addProperty(characteristicInfo, Tools.DESCRIPTORS, descriptorsInfo);
						characteristicsInfo.put(characteristicInfo);
					}
					Tools.addProperty(serviceInfo, Tools.CHARACTERISTICS, characteristicsInfo);
					servicesInfo.put(serviceInfo);
				}
				Tools.addProperty(obj, Tools.DEVICE_ID, deviceID);
				Tools.addProperty(obj, Tools.SERVICES, servicesInfo);
				getDeviceAllDataCC.get(deviceID).success(obj);
				getDeviceAllDataCC.remove(deviceID);
				deviceServices.put(deviceID, services);
			}else {
				Tools.sendErrorMsg(getDeviceAllDataCC.get(deviceID));
				getDeviceAllDataCC.remove(deviceID);
			}
		}
	}

	private void addServiceManage(int status){
		if (addServiceCC != null) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				gattServerSum = gattServerSum - 1 ;
				if (gattServerSum == 0) {
					Tools.sendSuccessMsg(addServiceCC);
					addServiceCC = null;
				}
			}else {
				gattServerSum = 0;
				Tools.sendErrorMsg(addServiceCC);
				addServiceCC = null;
			}
		}
	}

	private void getRssiManage(BluetoothGatt gatt , int rssi , int status){
		String deviceID = getDeviceAddress(gatt);
		if (getRssiCC.get(deviceID)!=null) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				JSONObject obj = new JSONObject();
				Tools.addProperty(obj, Tools.DEVICE_ID, deviceID);
				Tools.addProperty(obj, Tools.RSSI, rssi);
				getRssiCC.get(deviceID).success(obj);
				getRssiCC.remove(deviceID);
			}else {
				Tools.sendErrorMsg(getRssiCC.get(deviceID));
				getRssiCC.remove(deviceID);
			}
		}
	}

	private void addEventListenerManage(BluetoothGatt gatt, int newState){
		String deviceID = getDeviceAddress(gatt);
		if (newState == BluetoothGatt.STATE_DISCONNECTED) {
			JSONObject obj = new JSONObject();
			Tools.addProperty(obj, Tools.DEVICE_ID, deviceID);
			PluginResult pluginResult = new PluginResult(PluginResult.Status.OK , obj);
			pluginResult.setKeepCallback(true);
			addEventListenerCC.get(Tools.DISCONNECT).sendPluginResult(pluginResult);
		}
	}

	private String getDeviceAddress(BluetoothGatt gatt){
		return gatt.getDevice().getAddress();
	}
}
