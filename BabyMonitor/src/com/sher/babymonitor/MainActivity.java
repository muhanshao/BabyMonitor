package com.sher.babymonitor;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import org.apache.http.conn.util.InetAddressUtils;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public final class MainActivity extends Activity
        implements SurfaceHolder.Callback, OnClickListener
{
    private static final String TAG = "BabyMonitorBaby";
    
    ///// Socket communication
    	// Server
    private Socket socket;
 	private final int SERVER_PORT = 8080; //Define the server port
 	
 		// Client
 	private Button CallMom = null;
    

    ///// Audio test
    int frequency = 22050;
    int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private RealDoubleFFT transformer;
    int blockSize = 512;
    
    private TextView BabyCry;
    private TextView CountTime;
    double[] toTransform = new double[blockSize];
    double CurrentVolume;
    double Energy = 3.5;
    
    private MediaPlayer mp;

    Button StartButton;
    Button StopButton;
    Button playMusic;
    Button stopMusic;
    
    boolean started = false;
    boolean isCry = false;
    int crycount;

    RecordAudio recordTask;

    ImageView imageView;
    Bitmap bitmap;
    Canvas canvas;
    Paint paint;
    
    ////// Ip camera
    private static final String WAKE_LOCK_TAG = "peepers";
    private static final String PREF_CAMERA = "camera";
    private static final int PREF_CAMERA_INDEX_DEF = 0;
    private static final String PREF_FLASH_LIGHT = "flash_light";
    private static final boolean PREF_FLASH_LIGHT_DEF = false;
    private static final String PREF_PORT = "port";
    private static final int PREF_PORT_DEF = 8080;
    private static final String PREF_JPEG_SIZE = "size";
    private static final String PREF_JPEG_QUALITY = "jpeg_quality";
    private static final int PREF_JPEG_QUALITY_DEF = 40;
    // preview sizes will always have at least one element, so this is safe
    private static final int PREF_PREVIEW_SIZE_INDEX_DEF = 0;

    private boolean mRunning = false;
    private boolean mPreviewDisplayCreated = false;
    private SurfaceHolder mPreviewDisplay = null;
    private CameraStreamer mCameraStreamer = null;

    private String mIpAddress = "";
    private int mCameraIndex = PREF_CAMERA_INDEX_DEF;
    private boolean mUseFlashLight = PREF_FLASH_LIGHT_DEF;
    private int mPort = PREF_PORT_DEF;
    private int mJpegQuality = PREF_JPEG_QUALITY_DEF;
    private int mPrevieSizeIndex = PREF_PREVIEW_SIZE_INDEX_DEF;
    private TextView mIpAddressView = null;
    private LoadPreferencesTask mLoadPreferencesTask = null;
    private SharedPreferences mPrefs = null;
    private MenuItem mSettingsMenuItem = null;
    private WakeLock mWakeLock = null;

    public MainActivity()
    {
        super();
    } // constructor()

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ////// Socket communication
        // Server
		// Call method
		getDeviceIpAddress();
		//New thread to listen to incoming connections
//		new Thread(new Runnable() {
//			private ServerSocket socServer;
//			@Override
//			public void run() {
//				
//				try {
//					//Create a server socket object and bind it to a port
//					socServer = new ServerSocket();
//					socServer.setReuseAddress(true);
//					socServer.bind(new InetSocketAddress(SERVER_PORT));
//					//Create server side client socket reference
//					Socket socClient = null;
//					//Infinite loop will listen for client requests to connect
//					while (true) {
//						//Accept the client connection and hand over communication to server side client socket
//						socClient = socServer.accept();
//						//For each client new instance of AsyncTask will be created
//						ServerAsyncTask serverAsyncTask = new ServerAsyncTask();
//						//Start the AsyncTask execution 
//						//Accepted client socket object will pass as the parameter
//						serverAsyncTask.execute(new Socket[] {socClient});
//					} // while
//				} catch (IOException e) {
//					e.printStackTrace();
//				} // catch
//			} // run
//		}).start();  // Thread
		
		// Client
		CallMom = (Button)this.findViewById(R.id.CallMom); 
		CallMom.setOnClickListener(this);   
       		
      
        ////// Audio test
        // Draw the frequency picture
        StartButton = (Button) this.findViewById(R.id.StartButton);
        StartButton.setOnClickListener(this);
        
        StopButton = (Button) this.findViewById(R.id.StopButton);
        StopButton.setOnClickListener(this); 

        transformer = new RealDoubleFFT(blockSize);

        imageView = (ImageView) this.findViewById(R.id.ImageView01);
        bitmap = Bitmap.createBitmap((int) blockSize, (int) 200,
                Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint = new Paint();
        paint.setColor(Color.GREEN);
        imageView.setImageBitmap(bitmap);
        
        // Test if baby is crying
        BabyCry = (TextView)findViewById(R.id.Babycry);

        // Play the lullaby
        playMusic = (Button)findViewById(R.id.PlayMusic);
        stopMusic = (Button)findViewById(R.id.StopMusic);
        mp = MediaPlayer.create(this, R.raw.lullaby);
        
		playMusic.setOnClickListener(new Button.OnClickListener(){
			@Override
		    public void onClick(View v)
		    {
		    	try{
		    		if(mp != null)
		    			mp.stop();
		    		mp.prepare();
		    		mp.start();
		    	} catch (Exception e){
		    		e.printStackTrace();
		    	}        	
		    }
		});
		
		stopMusic.setOnClickListener(this);  
        
        // Countdown time
        CountTime = (TextView)findViewById(R.id.CountTime);
        
        
        //////// Ip camera
        new LoadPreferencesTask().execute();

        mPreviewDisplay = ((SurfaceView) findViewById(R.id.camera)).getHolder();
        mPreviewDisplay.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mPreviewDisplay.addCallback(this);

        mIpAddress = tryGetIpV4Address();
        mIpAddressView = (TextView) findViewById(R.id.ip_address);
        updatePrefCacheAndUi();

        final PowerManager powerManager =
                (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
                WAKE_LOCK_TAG);
    } // onCreate(Bundle)
    
    
/**
 * Get ip address of the device 
 */
	public void getDeviceIpAddress() {
		try {
			//Loop through all the network interface devices
			for (Enumeration<NetworkInterface> enumeration = NetworkInterface
					.getNetworkInterfaces(); enumeration.hasMoreElements();) {
				NetworkInterface networkInterface = enumeration.nextElement();
				//Loop through all the ip addresses of the network interface devices
				for (Enumeration<InetAddress> enumerationIpAddr = networkInterface.getInetAddresses(); enumerationIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumerationIpAddr.nextElement();
					//Filter out loopback address and other irrelevant ip addresses 
//    					if (!inetAddress.isLoopbackAddress() && inetAddress.getAddress().length == 4) {
//    						//Print the device ip address in to the text view 
//    						tvServerIP.setText(inetAddress.getHostAddress());
//    					}
				} // for
			} // for
		} catch (SocketException e) {
			Log.e("ERROR:", e.toString());
		} // catch
	} // getDeviceIpAddress
	

/**
 * AsyncTask which handles the communication with the server 
 */
	class ClientAsyncTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... params) {
			String result = null;
			try {
				//Create a client socket and define internet address and the port of the server
				Socket socket = new Socket(params[0],
						Integer.parseInt(params[1]));
				
				//Get the output stream of the client socket
				PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
				//Write data to the output stream of the client socket
				out.println(params[2]);	
				
				//Close the client socket
				socket.close();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return result;
		} // doInBackground
		
	} // ClientAsyncTask
	
/**
 * AsyncTask which handles the communication with the client 
 */
	class ServerAsyncTask extends AsyncTask<Socket, Void, String> {
		//Background task which serve for the client
		@Override
		protected String doInBackground(Socket... params) {
			String result = null;
			//Get the accepted socket object 
			Socket mySocket = params[0];
			try {
				//Get the data input stream comming from the client 
				InputStream is = mySocket.getInputStream();
				//Get the output stream to the client
				PrintWriter out = new PrintWriter(
						mySocket.getOutputStream(), true);
				//Write data to the data output stream
				out.println("Hello from server");
				//Buffer the data input stream
				BufferedReader br = new BufferedReader(
						new InputStreamReader(is));
				//Read the contents of the data buffer
				result = br.readLine();
				//Close the client connection
				mySocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return result;
		}
		@Override
		protected void onPostExecute(String s) {
			//After finishing the execution of background task data will be write the text view
			stopMusic.performClick();
		}			
	}


	
	
    public class RecordAudio extends AsyncTask<Void, double[], Void> {
        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                // int bufferSize = AudioRecord.getMinBufferSize(frequency,
                // AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                int bufferSize = AudioRecord.getMinBufferSize(frequency, 
                        channelConfiguration, audioEncoding); 
                AudioRecord audioRecord = new AudioRecord( 
                        MediaRecorder.AudioSource.MIC, frequency, 
                        channelConfiguration, audioEncoding, bufferSize); 

                short[] buffer = new short[blockSize];

                audioRecord.startRecording();

                // started = true; hopes this should true before calling
                // following while loop

                while (started) {
                    int bufferReadResult = audioRecord.read(buffer, 0,blockSize);
                    
                    // Test current loudness
                    double mean;
                    long sum = 0;
                    for(int i = 0; i< buffer.length;i++)
                    {
                    	sum = sum + buffer[i]*buffer[i];
                    }
                    mean = sum/(double)bufferReadResult;
                    CurrentVolume = 10 * Math.log10(mean);
                    
                    // FFT transform
                    for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
                        toTransform[i] = (double) buffer[i] / 32768.0; // signed 16 bit                                                                      
                    }                                       
                        transformer.ft(toTransform);
                        publishProgress(toTransform);
                }

                audioRecord.stop();
                
            } catch (Throwable t) {
                t.printStackTrace();
                Log.e("AudioRecord", "Recording Failed");
            }
            return null;
        }
        
        CountDownTimer timer = new CountDownTimer(15000,10){
        	
        	@Override 
        	public void onTick(long millisUntilFinished){
        		CountTime.setText("Seconds remaining:"+millisUntilFinished / 1000);
        		////////// Change FrequencyRange
        		if((CurrentVolume > 40) && (toTransform[89]+toTransform[90]+toTransform[91]> Energy ))
        			crycount++;
        	}
        	
        	@Override
            public void onFinish() {
        		//////// change  ////////////////
        		if(crycount > 20)
        		{   
        			CallMom.performClick();
        			CallMom.setText("Mom!");
        		}
        		else
        			stopMusic.performClick();  
        		stopMusic.performClick(); 
            }
        };
              
        
        @Override
        protected void onProgressUpdate(double[]... toTransform) {

            canvas.drawColor(Color.WHITE);
            BabyCry.setText("");

            for (int i = 0; i < toTransform[0].length; i++) {
                int x = i;
                int downy = (int) (200 - (toTransform[0][i]*100));
                int upy = 200;

                canvas.drawLine(x, downy, x, upy, paint);
            }
            
            // Test if baby is crying 
            //////////// change FrequencyRange  ////////////////////
            if((CurrentVolume > 40) && (toTransform[0][89]+toTransform[0][90]+toTransform[0][91] > Energy ))
            {
            	BabyCry.setText("Yep~");
//            	CallMom.performClick();
            	if(mp.isPlaying() == false)
            	{
            		crycount = 1;
            		CallMom.setText("Call");
            		timer.start();

            	}
            	playMusic.performClick();
            }

            imageView.invalidate();                    

            // TODO Auto-generated method stub
            // super.onProgressUpdate(values);
        }

    }
    
    public void onClick(View v) {
        // TODO Auto-generated method stub
        if(v==StartButton){
        	started = true;
//    		StartButton.setText("Stop");
    		recordTask = new RecordAudio();
    		recordTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);  
        }
        else if(v==StopButton)
        {
        	started = false;
        	recordTask.cancel(true);
        }
        else if(v==CallMom)
        {
				new Thread(new Runnable() {
					private ServerSocket socServer;
					@Override
					public void run() {
						
						try {
							//Create a server socket object and bind it to a port
							socServer = new ServerSocket();
							socServer.setReuseAddress(true);
							socServer.bind(new InetSocketAddress(SERVER_PORT));
							//Create server side client socket reference
							Socket socClient = null;
							//Infinite loop will listen for client requests to connect
							while (true) {
								//Accept the client connection and hand over communication to server side client socket
								socClient = socServer.accept();
								//For each client new instance of AsyncTask will be created
								ServerAsyncTask serverAsyncTask = new ServerAsyncTask();
								//Start the AsyncTask execution 
								//Accepted client socket object will pass as the parameter
								serverAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Socket[] {socClient});
							} // while
						} catch (IOException e) {
							e.printStackTrace();
						} // catch
					} // run
				}).start();  // Thread
				
				ClientAsyncTask clientAST = new ClientAsyncTask();
	    		//Pass the server ip, port and client message to the AsyncTask
	    		////////// Change IPAddress
//	            clientAST.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new String[] { "172.27.35.5", "8080","Baby is crying" });    	
//				clientAST.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new String[] { "192.168.191.4", "8080","Baby is crying" });    	
				clientAST.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new String[] { "192.168.191.5", "8080","Baby is crying" });    	
				
        }
        else if(v==stopMusic)
        {
        	try{
	    		if(mp != null)
	    			mp.stop();
	    		
	    	} catch (Exception e){
	    		e.printStackTrace();
	    	}        	
        }
    	
                
        }
    
   
    
    ///// Ip camera
    @Override
    protected void onResume()
    {
        super.onResume();
        mRunning = true;
        if (mPrefs != null)
        {
            mPrefs.registerOnSharedPreferenceChangeListener(
                    mSharedPreferenceListener);
        } // if
        updatePrefCacheAndUi();
        tryStartCameraStreamer();
        mWakeLock.acquire();
    } // onResume()

    @Override
    protected void onPause()
    {
        mWakeLock.release();
        super.onPause();
        mRunning = false;
        if (mPrefs != null)
        {
            mPrefs.unregisterOnSharedPreferenceChangeListener(
                    mSharedPreferenceListener);
        } // if
        ensureCameraStreamerStopped();
    } // onPause()

    @Override
    public void surfaceChanged(final SurfaceHolder holder, final int format,
            final int width, final int height)
    {
        // Ingored
    } // surfaceChanged(SurfaceHolder, int, int, int)

    @Override
    public void surfaceCreated(final SurfaceHolder holder)
    {
        mPreviewDisplayCreated = true;
        tryStartCameraStreamer();
    } // surfaceCreated(SurfaceHolder)

    @Override
    public void surfaceDestroyed(final SurfaceHolder holder)
    {
        mPreviewDisplayCreated = false;
        ensureCameraStreamerStopped();
    } // surfaceDestroyed(SurfaceHolder)

    private void tryStartCameraStreamer()
    {
        if (mRunning && mPreviewDisplayCreated && mPrefs != null)
        {
            mCameraStreamer = new CameraStreamer(mCameraIndex, mUseFlashLight, mPort,
                    mPrevieSizeIndex, mJpegQuality, mPreviewDisplay);
            mCameraStreamer.start();
        } // if
    } // tryStartCameraStreamer()

    private void ensureCameraStreamerStopped()
    {
        if (mCameraStreamer != null)
        {
            mCameraStreamer.stop();
            mCameraStreamer = null;
        } // if
    } // stopCameraStreamer()

    @Override
    public boolean onCreateOptionsMenu(final Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        mSettingsMenuItem = menu.add(R.string.settings);
        mSettingsMenuItem.setIcon(android.R.drawable.ic_menu_manage);
        return true;
    } // onCreateOptionsMenu(Menu)

    @Override
    public boolean onOptionsItemSelected(final MenuItem item)
    {
        if (item != mSettingsMenuItem)
        {
            return super.onOptionsItemSelected(item);
        } // if
        startActivity(new Intent(this, PeepersPreferenceActivity.class));
        return true;
    } // onOptionsItemSelected(MenuItem)

    private final class LoadPreferencesTask
            extends AsyncTask<Void, Void, SharedPreferences>
    {
        private LoadPreferencesTask()
        {
            super();
        } // constructor()

        @Override
        protected SharedPreferences doInBackground(final Void... noParams)
        {
            return PreferenceManager.getDefaultSharedPreferences(
                    MainActivity.this);
        } // doInBackground()

        @Override
        protected void onPostExecute(final SharedPreferences prefs)
        {
            MainActivity.this.mPrefs = prefs;
            prefs.registerOnSharedPreferenceChangeListener(
                    mSharedPreferenceListener);
            updatePrefCacheAndUi();
            tryStartCameraStreamer();
        } // onPostExecute(SharedPreferences)


    } // class LoadPreferencesTask

    private final OnSharedPreferenceChangeListener mSharedPreferenceListener =
            new OnSharedPreferenceChangeListener()
    {
        @Override
        public void onSharedPreferenceChanged(final SharedPreferences prefs,
                final String key)
        {
            updatePrefCacheAndUi();
        } // onSharedPreferenceChanged(SharedPreferences, String)

    }; // mSharedPreferencesListener

    private final int getPrefInt(final String key, final int defValue)
    {
        // We can't just call getInt because the preference activity
        // saves everything as a string.
        try
        {
            return Integer.parseInt(mPrefs.getString(key, null /* defValue */));
        } // try
        catch (final NullPointerException e)
        {
            return defValue;
        } // catch
        catch (final NumberFormatException e)
        {
            return defValue;
        } // catch
    } // getPrefInt(String, int)

    private final void updatePrefCacheAndUi()
    {
        mCameraIndex = getPrefInt(PREF_CAMERA, PREF_CAMERA_INDEX_DEF);
        if (hasFlashLight())
        {
            if (mPrefs != null)
            {
                mUseFlashLight = mPrefs.getBoolean(PREF_FLASH_LIGHT,
                        PREF_FLASH_LIGHT_DEF);
            } // if
            else
            {
                mUseFlashLight = PREF_FLASH_LIGHT_DEF;
            } // else
        } //if
        else
        {
            mUseFlashLight = false;
        } // else

        // XXX: This validation should really be in the preferences activity.
        mPort = getPrefInt(PREF_PORT, PREF_PORT_DEF);
        // The port must be in the range [1024 65535]
        if (mPort < 1024)
        {
            mPort = 1024;
        } // if
        else if (mPort > 65535)
        {
            mPort = 65535;
        } // else if

        mPrevieSizeIndex = getPrefInt(PREF_JPEG_SIZE, PREF_PREVIEW_SIZE_INDEX_DEF);
        mJpegQuality = getPrefInt(PREF_JPEG_QUALITY, PREF_JPEG_QUALITY_DEF);
        // The JPEG quality must be in the range [0 100]
        if (mJpegQuality < 0)
        {
            mJpegQuality = 0;
        } // if
        else if (mJpegQuality > 100)
        {
            mJpegQuality = 100;
        } // else if
        mIpAddressView.setText("http://" + mIpAddress + ":" + mPort + "/");
    } // updatePrefCacheAndUi()

    private boolean hasFlashLight()
    {
        return getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_FLASH);
    } // hasFlashLight()

	private static String tryGetIpV4Address()
	{
	    try
	    {
	        final Enumeration<NetworkInterface> en =
	                NetworkInterface.getNetworkInterfaces();
	        while (en.hasMoreElements())
	        {
	            final NetworkInterface intf = en.nextElement();
	            final Enumeration<InetAddress> enumIpAddr =
	                    intf.getInetAddresses();
	            while (enumIpAddr.hasMoreElements())
	            {
	                final  InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress())
	                {
	                    final String addr = inetAddress.getHostAddress().toUpperCase();
	                    if (InetAddressUtils.isIPv4Address(addr))
	                    {
	                        return addr;
	                    }
	                } // if
	            } // while
	        } // for
	    } // try
	    catch (final Exception e)
	    {
	        // Ignore
	    } // catch
	    return null;
	} // tryGetIpV4Address()

} // class MainActivity


