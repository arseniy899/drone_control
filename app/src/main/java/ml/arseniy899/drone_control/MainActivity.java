package ml.arseniy899.drone_control;

import android.Manifest;
import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.Arrays;

import io.github.controlwear.virtual.joystick.android.JoystickView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity
{
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * Some older devices needs a small delay between UI widget updates
	 * and a change of the status and navigation bar.
	 */
	private static final int UI_ANIMATION_DELAY = 300;
	private final Handler mHideHandler = new Handler();
	private View mContentView;
	private final Runnable mHidePart2Runnable = new Runnable()
	{
		@SuppressLint("InlinedApi")
		@Override
		public void run()
		{
			// Delayed removal of status and navigation bar

			// Note that some of these constants are new as of API 16 (Jelly Bean)
			// and API 19 (KitKat). It is safe to use them, as they are inlined
			// at compile-time and do nothing on earlier devices.
			mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
					| View.SYSTEM_UI_FLAG_FULLSCREEN
					| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
					| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
		}
	};
	private View mControlsView;
	private final Runnable mShowPart2Runnable = new Runnable()
	{
		@Override
		public void run()
		{
			// Delayed display of UI elements
			ActionBar actionBar = getSupportActionBar();
			if (actionBar != null)
			{
				actionBar.show();
			}
//			mControlsView.setVisibility(View.VISIBLE);
		}
	};
	private boolean mVisible;
	private final Runnable mHideRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			hide();
		}
	};
	
	String[] PERMISSIONS = {
			Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
	
	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener()
	{
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent)
		{
			if (AUTO_HIDE)
			{
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};
	SocketClient socketClient;
	
	private TextView debugTextView;
	private ImageView cameraView;
	private View noConnectionLay;
	private View reconnectBtn;
	private View adjustLay;
	private MaterialButtonToggleGroup modeToggle;
	// fly data
	int throttle = 127;
	int yaw = 127;
	int pitch = 127;
	int roll = 127;
	int flyMode = 0;
	int throttleOld = 127;
	int yawOld = 127;
	int pitchOld = 127;
	int rollOld = 127;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		boolean isGranted = true;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && PERMISSIONS != null)
		{
			for (String permission : PERMISSIONS) {
				if (ActivityCompat.checkSelfPermission(this, permission)
						!= PackageManager.PERMISSION_GRANTED)
				{
					isGranted = false;
					break;
				}
			}
		}
		
		if(!isGranted)
			ActivityCompat.requestPermissions (this, PERMISSIONS, 1);
		else
			new LogKeeper(this);
		setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		mVisible = true;
		mControlsView = findViewById(R.id.fullscreen_content_controls);
		mContentView = findViewById(R.id.fullscreenContent);
		
		debugTextView = findViewById(R.id.debugTextView);
		cameraView = findViewById(R.id.cameraView);
		noConnectionLay = findViewById(R.id.noConnectionLay);
		reconnectBtn = findViewById(R.id.reconnectBtn);
		adjustLay = findViewById(R.id.adjustLay);
		modeToggle = findViewById(R.id.toggleGroup);
		// Set up the user interaction to manually show or hide the system UI.
		mContentView.setOnClickListener(view -> hide());
		reconnectBtn.setOnClickListener((view -> {
			reconnectBtn.setVisibility(View.GONE);
			socketClient.connect();
		}));
		connectToCopter();
		findViewById(R.id.adjustBtn).setOnClickListener(view -> {
			if(adjustLay.getVisibility() == View.VISIBLE)
				adjustLay.setVisibility(View.GONE);
			else
				adjustLay.setVisibility(View.VISIBLE);
		});
		findViewById(R.id.adjustOpenSettingsBtn).setOnClickListener(view -> {
			openSettings();
			adjustLay.setVisibility(View.GONE);
		});
		findViewById(R.id.settingsRecconectBtn).setOnClickListener(view -> openSettings());
		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
		
		findViewById(R.id.engineStart).setOnClickListener((view)->{
			if(socketClient != null)
				socketClient.sendMotorOn(flyMode);
		});
		findViewById(R.id.engineStop).setOnClickListener((view)->{
			if(socketClient != null)
				socketClient.sendMotorOff(flyMode);
		});
		JoystickView joystickLeft = findViewById(R.id.joystickLeft);
		joystickLeft.setBackgroundSizeRatio(0.5f);
		joystickLeft.setButtonSizeRatio(0.15f);
		joystickLeft.setOnMoveListener((angle, strength) ->
		{
			
			double rad = Math.toRadians(angle);
			double x = strength * Math.cos(rad);
			double y = strength * Math.sin(rad);
//			debugTextView.setText(String.format("Angle : %d, \n Stren: %d; \n (%.2f,%.2f)", angle, strength, x, y));
			yaw = coordToUnsigned(x);
			pitch = coordToUnsigned(y);
		}, 50);
		JoystickView joystickRight = findViewById(R.id.joystickRight);
		joystickRight.setBackgroundSizeRatio(0.5f);
		joystickRight.setButtonSizeRatio(0.15f);
		joystickRight.setOnMoveListener((angle, strength) ->
		{
			
			double rad = Math.toRadians(angle);
			double x = strength * Math.cos(rad);
			double y = strength * Math.sin(rad);
//			debugTextView.setText(String.format("Angle : %d, \n Stren: %d; \n (%.2f,%.2f)", angle, strength, x, y));
			roll = coordToUnsigned(x);
			throttle = coordToUnsigned(y);
		}, 50);
		new Thread(()->{
			while (true)
			{
				try
				{
					Thread.sleep(20);
					/*runOnUiThread(()-> debugTextView.setText(String.format("yaw : %d, " +
							"throttle: %d; \n" +
							"roll : %d, " +
							"pitch: %d; ", yaw, throttle,roll, pitch, flyMode)));*/
					if (socketClient != null && (throttleOld != throttle  || yawOld != yaw ||
					pitchOld != pitch || rollOld != roll))
					{
						
						socketClient.sendFlyData(yaw, throttle,roll, pitch, flyMode);
						throttleOld = throttle;
						yawOld = yaw;
						pitchOld = pitch;
						rollOld = roll;
					}
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}).start();
		modeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) ->
		{
			
			if (isChecked)
//			if (true)
			{
				switch (checkedId)
				{
					case R.id.mode1:
						flyMode = 0;
					break;
					case R.id.mode2:
						flyMode = 1;
					break;
					case R.id.mode3:
						flyMode = 2;
					break;
					case R.id.mode4:
						flyMode = 3;
					break;
				}
				socketClient.sendFlyData(yaw, throttle,roll, pitch, flyMode);
				adjustLay.setVisibility(View.GONE);
			}
		});
	}
	
	private void connectToCopter()
	{
		String addr = MemoryWork.loadString(this, "connect-ip");
		if(addr.isEmpty())
		{
			Toast.makeText(getBaseContext(),"Адрес не задан",Toast.LENGTH_LONG).show();
			openSettings();
			
		}
		socketClient = new SocketClient(this, addr, new SocketClient.ClientCallback()
		{
			@Override
			public void onMessageRecived(String message)
			{
				LogKeeper.d("MainAct/socket", "received: " + message);
			}
			
			@Override
			void onMessageSent(byte[] message)
			{
				super.onMessageSent(message);
				String arrayRepr = "";
				for (byte byt : message)
				{
					if (!arrayRepr.isEmpty())
						arrayRepr += ",";
					arrayRepr += String.format("%02X", byt);
					
				}
				String finalArrayRepr = arrayRepr;
				runOnUiThread(() -> debugTextView.setText("[DEBUG] Отправлено: [" + finalArrayRepr + "]"));
			}
			
			@Override
			public void onMessageRecived(byte[] message)
			{
				String data = Arrays.toString(message);
				if(data.length() > 128)
					data = data.substring(0,128)+" ...";
				LogKeeper.d("MainAct/socket", "received: " + data);
//				Bitmap image = BitmapFactory.decodeByteArray(message, 0, message.length);
				
				int width = 640;
				int height = 480;
				int pixelData[] = new int[width * height];
				
				for (int i = 0; i < message.length; i += 3)
				{
					try
					{
						int alpha = 0 << 8 * 3;
						int red = message[i + 0];// << 8*2;
						int green = message[i + 1];// << 8*1;
						int blue = message[i + 2];// << 8*0;
						
						//color is an int with the format of TYPE_INT_ARGB (0xAARRGGBB)
						
						pixelData[i / 3] = ((0xFF & (byte) 255) << 24) // alpha, 8 bits
								| ((0xFF & (byte) red) << 16)      // red, 8 bits
								| ((0xFF & (byte) green) << 8)         // green, 8 bits
								| (0xFF & (byte) blue);               // blue, 8 bits
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					
				}
				
				Bitmap image = Bitmap.createBitmap(pixelData, width, height, Bitmap.Config.ARGB_8888);
				if(image != null)
				cameraView.setImageBitmap(Bitmap.createScaledBitmap(image, cameraView.getWidth(),
						cameraView.getHeight(), false));
//			cameraView.setImageBitmap(image);
			}
			
			@Override
			void onConnectionStateCh(boolean isConnected)
			{
				super.onConnectionStateCh(isConnected);
				if (isConnected)
				{
					LogKeeper.i("MainAct/onConnStatCh","Connected.. Disable overlay");
					noConnectionLay.setVisibility(View.GONE);
					hide();
				}
				else
				{
					LogKeeper.i("MainAct/onConnStatCh","Not connected =( Enabling overlay");
					runOnUiThread(() ->
					{
						noConnectionLay.setVisibility(View.VISIBLE);
						reconnectBtn.setVisibility(View.VISIBLE);
						LogKeeper.i("MainAct/onConnStatCh","Enabled overlay");
					});
					show();
				}
			}
		});
	}
	
	int coordToUnsigned(double coord)
	{
		return (int) ((coord+100.0)/200.0 * 254.0);
	}
	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
	}
	void openSettings()
	{
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
	{
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		new LogKeeper(this);
		
	}
	
	private void toggle()
	{
		if (mVisible)
		{
			hide();
		}
		else
		{
			show();
		}
	}

	private void hide()
	{
		// Hide UI first
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
		{
			actionBar.hide();
		}
		mControlsView.setVisibility(View.GONE);
		mVisible = false;

		// Schedule a runnable to remove the status and navigation bar after a delay
		mHideHandler.removeCallbacks(mShowPart2Runnable);
		mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
	}

	@SuppressLint("InlinedApi")
	private void show()
	{
		// Show the system bar
		mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
		mVisible = true;

		// Schedule a runnable to display UI elements after a delay
		mHideHandler.removeCallbacks(mHidePart2Runnable);
		mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
	}

	/**
	 * Schedules a call to hide() in delay milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis)
	{
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		if(socketClient != null && socketClient.isConnected())
		{
			socketClient.disconnect();
		}
		connectToCopter();
		noConnectionLay.setVisibility(View.VISIBLE);
	}
}
