package org.bcsphere.bluetooth.tools;



import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;


/**
 * Beijing Juma Technology Co.LTD.detection API
 * @author huangshuwen
 *
 */
public class BluetoothDetection {
	private static SharedPreferences sp;
	private static Editor editor;
	public static final void detectionBluetoothAPI(final Context context)
	{
		 sp  = context.getSharedPreferences("VERSION_OF_API", 1);
		 editor = sp.edit();
		if (Tools.isSupportUniversalAPI()) {
			editor.putString("API", "google").commit();
		}else {
			if (Tools.isSupportSpecificAPI("samsung")) {
				editor.putString("API", "samsung").commit();
			}else if (Tools.isSupportSpecificAPI("htc")) {
				
			}else if (Tools.isSupportSpecificAPI("xiaomi")){
				
			}else if (Tools.isSupportSpecificAPI("motorola")) {
				
			}else {
				if (Tools.getSupportBasebandVersionBrand() != null) {
					if (Tools.getSupportBasebandVersionBrand().equals("xiaomi")) {
						
					}
				}else {
					editor.putString("API", "").commit();
				}
			}
		}
	}

	
	
	
}
