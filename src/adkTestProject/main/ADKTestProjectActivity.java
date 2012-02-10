package adkTestProject.main;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.PendingIntent;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import android.os.ParcelFileDescriptor;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.android.future.usb.UsbManager;
import com.android.future.usb.UsbAccessory;



public class ADKTestProjectActivity extends Activity implements Runnable, SeekBar.OnSeekBarChangeListener {
	
	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;
	
	private static final String ACTION_USB_PERMISSION = "com.google.android.ADKTestProject.action.USB_PERMISSION";
	private static final String TAG = "ADKTestProject";
	
	SeekBar mRed;
	SeekBar mBlue;
	SeekBar mGreen;
	TextView mRedText;
	TextView mBlueText;
	TextView mGreenText;
	
	int redValue = 255;
	int blueValue = 255;
	int greenValue = 255;
	
	FileInputStream mFileInputStream;
	FileOutputStream mFileOutputStream;
	ParcelFileDescriptor mFileDescriptor;	
	
	UsbAccessory mUsbAccessory;
	
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(ACTION_USB_PERMISSION.equals(action)){
				synchronized (this) {
					UsbAccessory accessory = UsbManager.getAccessory(intent);
					if (intent.getBooleanExtra(
							UsbManager.EXTRA_PERMISSION_GRANTED, false)){
						openAccessory(accessory);
					}
					else {
						Log.d(TAG,"Permission Denied For Accessory" 
								+ accessory);
					}
					mPermissionRequestPending = false;
				}
			}
			else if(UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = UsbManager.getAccessory(intent);
				if (accessory != null && accessory.equals(mUsbAccessory)){
					closeAccessory();
				}
			}
			
		}
		
	};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUsbManager = UsbManager.getInstance(this);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, 
        		new Intent(ACTION_USB_PERMISSION),0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver,filter);
        
        if(getLastNonConfigurationInstance() != null){
        	mUsbAccessory = (UsbAccessory) getLastNonConfigurationInstance();
        	openAccessory(mUsbAccessory);
        	sendCommand(redValue,blueValue,greenValue);
        }
        
        setContentView(R.layout.main);
        
        mRed = (SeekBar)findViewById(R.id.Red);
        mRed.setOnSeekBarChangeListener(this);
        mBlue = (SeekBar)findViewById(R.id.Blue);
        mBlue.setOnSeekBarChangeListener(this);
        mGreen = (SeekBar)findViewById(R.id.Green);
        mGreen.setOnSeekBarChangeListener(this);
        mRedText = (TextView)findViewById(R.id.RedText);
        mBlueText = (TextView)findViewById(R.id.BlueText);
        mGreenText = (TextView)findViewById(R.id.GreenText);
        mRedText.setText(getString(R.string.red_value_text) +
        		255);
        mBlueText.setText(getString(R.string.blue_value_text) +
        		255);
        mGreenText.setText(getString(R.string.green_value_text) +
        		255);
    }
    
    @Override
    public Object onRetainNonConfigurationInstance(){
    	if (mUsbAccessory != null){
    		return mUsbAccessory;
    	}
    	else{
    		return super.onRetainNonConfigurationInstance();
    	}
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	
    	Intent intent = getIntent();
    	
    	if (mFileInputStream != null && mFileOutputStream != null){
    		return ;
    	}
    	
    	UsbAccessory[] accessories = mUsbManager.getAccessoryList();
    	UsbAccessory accessory = (accessories == null ? null : accessories[0]);
    	
    	if (accessory != null){
    		if (mUsbManager.hasPermission(accessory)){
    			openAccessory(accessory);
    			sendCommand(redValue,blueValue,greenValue);
    		}
    		else{
    			synchronized(this){
    				if (!mPermissionRequestPending){
    					mUsbManager.requestPermission(accessory, 
    							mPermissionIntent);
    					mPermissionRequestPending = true;
    				}
    			}
    		}
    	}
    	else{
    		Log.d(TAG,"mAccessory is Null");
    	}
    }
    
    @Override
    public void onPause(){
    	super.onPause();
    	closeAccessory();
    }
    
    @Override
    public void onDestroy(){
    	unregisterReceiver(mUsbReceiver);
    	super.onDestroy();
    }
    
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// TODO Auto-generated method stub
		switch(seekBar.getId()){
		case R.id.Red:
			mRedText.setText(getString(R.string.red_value_text) + 
					progress);
			redValue = progress;
			break;
		case R.id.Blue:
			mBlueText.setText(getString(R.string.blue_value_text) + 
					progress);
			blueValue = progress;
			break;
		case R.id.Green:
			mGreenText.setText(getString(R.string.green_value_text) + 
					progress);
			greenValue = progress;
			break;
		}
		sendCommand(redValue,blueValue,greenValue);
	}
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
	}
	public void run() {
		int ret = 0;
		byte[] buffer = new byte[16384];
		int i;
		
		while (ret >= 0){
			try{
				ret = mFileInputStream.read(buffer);
			}
			catch(IOException e) {
				break;
			}
			i = 0;
			while (i < ret){
				int len = ret -i;
				switch (buffer[i]){
				default:
					Log.d(TAG,"unknown msg: " + buffer[i]);
					i = len;
					break;
				}
			}
		}
		
	}
	
	public void sendCommand(int red, int green, int blue){
		byte[] buffer = new byte[3];
		if(red > 255){
			red = 255;
		}
		if(green > 255){
			green = 255;
		}
		if(blue > 255){
			blue = 255;
		}
		buffer[0] = (byte)red;
		buffer[1] = (byte)green;
		buffer[2] = (byte)blue;
		
		if (mFileOutputStream != null && buffer[1] != -1){
			try{
				mFileOutputStream.write(buffer);
			}
			catch (IOException e){
				Log.e(TAG,"Write Faile",e);
			}
		}
	}
	
	private void openAccessory(UsbAccessory accessory){
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			mUsbAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mFileInputStream = new FileInputStream(fd);
			mFileOutputStream = new FileOutputStream(fd);
			
			Thread thread = new Thread(null,this,"ADKTestProject");
			thread.start();
			Log.d(TAG,"Accessory Opened");
			
		}
		else {
			Log.d(TAG,"Accessory Open Fail");
		}
	}
	
	private void closeAccessory(){
		try{
			if (mFileDescriptor != null){
				mFileDescriptor.close();
			}
		} 
		catch (IOException e) {
			
		}
		finally {
			mFileDescriptor = null;
			mUsbAccessory = null;
		}
		
	}
}