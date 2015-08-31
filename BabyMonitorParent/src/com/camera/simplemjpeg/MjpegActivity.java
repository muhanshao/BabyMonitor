package com.camera.simplemjpeg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MjpegActivity extends Activity implements OnClickListener{
	
	private static final boolean DEBUG=false;
    private static final String TAG = "BabyMonitorParent";
	
	//////// Socket communication
    private Button StartAlarm;
    private Button StopAlarm;
    private MediaPlayer alarm;
//    private SoundPool alarm;
    private int alarmID;
	// Server
	private final int SERVER_PORT = 8080; //Define the server port
	
	// Client
	private Button CalmDown = null;
      
    
	//////// Show Ip camera

    private MjpegView mv = null;
    String URL;
    
    // for settings (network and resolution)
    private static final int REQUEST_SETTINGS = 0;
    
    private int width = 640;
    private int height = 480;
    
//    private int ip_ad1 = 172;
//    private int ip_ad2 = 27;
//    private int ip_ad3 = 35;
//    private int ip_ad4 = 3;
    private int ip_ad1 = 192;
    private int ip_ad2 = 168;
    private int ip_ad3 = 191;
    private int ip_ad4 = 5;
    private int ip_port = 8080;
    private String ip_command = "videofeed";
    
    private boolean suspending = false;
 
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences preferences = getSharedPreferences("SAVED_VALUES", MODE_PRIVATE);
        width = preferences.getInt("width", width);
        height = preferences.getInt("height", height);
        ip_ad1 = preferences.getInt("ip_ad1", ip_ad1);
        ip_ad2 = preferences.getInt("ip_ad2", ip_ad2);
        ip_ad3 = preferences.getInt("ip_ad3", ip_ad3);
        ip_ad4 = preferences.getInt("ip_ad4", ip_ad4);
        ip_port = preferences.getInt("ip_port", ip_port);
        ip_command = preferences.getString("ip_command", ip_command);
                
        StringBuilder sb = new StringBuilder();
        String s_http = "http://";
        String s_dot = ".";
        String s_colon = ":";
        String s_slash = "/";
        sb.append(s_http);
        sb.append(ip_ad1);
        sb.append(s_dot);
        sb.append(ip_ad2);
        sb.append(s_dot);
        sb.append(ip_ad3);
        sb.append(s_dot);
        sb.append(ip_ad4);
        sb.append(s_colon);
        sb.append(ip_port);
        sb.append(s_slash);
        sb.append(ip_command);
        URL = new String(sb);

        setContentView(R.layout.main);
        
        //////// Socket communication
        StartAlarm = (Button)this.findViewById(R.id.StartAlarm);
//        StartAlarm.setVisibility(View.INVISIBLE);
        StopAlarm = (Button)this.findViewById(R.id.StopAlarm);
		alarm = MediaPlayer.create(this,R.raw.alarm);
//		alarm = new SoundPool(1,AudioManager.STREAM_MUSIC,5);
//		alarmID = alarm.load(this, R.raw.alarm,1);
//        
		StartAlarm.setOnClickListener(new Button.OnClickListener(){
			@Override
		    public void onClick(View v)
		    {
//				alarm.play(alarmID, 1, 1, 1, -1, 1);
		    	try{
		    		if(alarm != null)
		    			alarm.stop();
		    		alarm.prepare();
		    		alarm.setLooping(true);
		    		alarm.start();
		    		
		    	} catch (Exception e){
		    		e.printStackTrace();
		    	}        	
		    }
		});
		
		StopAlarm.setOnClickListener(new Button.OnClickListener(){
			@Override
		    public void onClick(View v)
		    {
//		    	try{
//		    		if(alarm != null)
//		    			alarm.stop();
//		    		
//		    	} catch (Exception e){
//		    		e.printStackTrace();
//		    	}       
				if(alarm.isPlaying()==true || alarm.isLooping()==true)
				{	alarm.stop();
				}
//				alarm.stop(alarmID);
		    }
		});
        

		// Client
		
		CalmDown = (Button)this.findViewById(R.id.StopMusic); 
		CalmDown.setOnClickListener(this); 
			
        
        ///////// Show Ip camera
        mv = (MjpegView) findViewById(R.id.mv);  
        if(mv != null){
        	mv.setResolution(width, height);
        }
        new DoRead().execute(URL);
    }

    public void onClick(View v) {
    	if(v==CalmDown){
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

//Pass the server ip, port and client message to the AsyncTask
//////////////// Change IPAddress
//		clientAST.execute(new String[] { "172.27.35.3", "8080","Don't play lullaby" });
//		clientAST.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new String[] { "192.168.191.3", "8080","Don't play lullaby" });
//		clientAST.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new String[] { ip_ad1+"."+ip_ad2+"."+ip_ad3+"."+ip_ad4, "8080","Don't play lullaby" });
		clientAST.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new String[] { "192.168.191.3", "8080","Don't play lullaby" });

    	}
    	}
    
    
    
    ///////// Socket communication
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
			// StartAlarm.performClick();
			try{
	    		if(alarm != null)
	    			alarm.stop();
	    		alarm.prepare();
	    		alarm.start();
	    		alarm.setLooping(true);
	    	} catch (Exception e){
	    		e.printStackTrace();
	    	}      
//			alarm.stop(alarmID);
		}			
	}

      
     
    
    public void onResume() {
    	if(DEBUG) Log.d(TAG,"onResume()");
        super.onResume();
        if(mv!=null){
        	if(suspending){
        		new DoRead().execute(URL);
        		suspending = false;
        	}
        }

    }

    public void onStart() {
    	if(DEBUG) Log.d(TAG,"onStart()");
        super.onStart();
    }
    public void onPause() {
    	if(DEBUG) Log.d(TAG,"onPause()");
        super.onPause();
        if(mv!=null){
        	if(mv.isStreaming()){
		        mv.stopPlayback();
		        suspending = true;
        	}
        }
    }
    public void onStop() {
    	if(DEBUG) Log.d(TAG,"onStop()");
        super.onStop();
    }

    public void onDestroy() {
    	if(DEBUG) Log.d(TAG,"onDestroy()");
    	
    	if(mv!=null){
    		mv.freeCameraMemory();
    	}
    	
        super.onDestroy();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.layout.option_menu, menu);
    	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case R.id.settings:
    			Intent settings_intent = new Intent(MjpegActivity.this, SettingsActivity.class);
    			settings_intent.putExtra("width", width);
    			settings_intent.putExtra("height", height);
    			settings_intent.putExtra("ip_ad1", ip_ad1);
    			settings_intent.putExtra("ip_ad2", ip_ad2);
    			settings_intent.putExtra("ip_ad3", ip_ad3);
    			settings_intent.putExtra("ip_ad4", ip_ad4);
    			settings_intent.putExtra("ip_port", ip_port);
    			settings_intent.putExtra("ip_command", ip_command);
    			startActivityForResult(settings_intent, REQUEST_SETTINGS);
    			return true;
    	}
    	return false;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch (requestCode) {
    		case REQUEST_SETTINGS:
    			if (resultCode == Activity.RESULT_OK) {
    				width = data.getIntExtra("width", width);
    				height = data.getIntExtra("height", height);
    				ip_ad1 = data.getIntExtra("ip_ad1", ip_ad1);
    				ip_ad2 = data.getIntExtra("ip_ad2", ip_ad2);
    				ip_ad3 = data.getIntExtra("ip_ad3", ip_ad3);
    				ip_ad4 = data.getIntExtra("ip_ad4", ip_ad4);
    				ip_port = data.getIntExtra("ip_port", ip_port);
    				ip_command = data.getStringExtra("ip_command");

    				if(mv!=null){
    					mv.setResolution(width, height);
    				}
    				SharedPreferences preferences = getSharedPreferences("SAVED_VALUES", MODE_PRIVATE);
    				SharedPreferences.Editor editor = preferences.edit();
    				editor.putInt("width", width);
    				editor.putInt("height", height);
    				editor.putInt("ip_ad1", ip_ad1);
    				editor.putInt("ip_ad2", ip_ad2);
    				editor.putInt("ip_ad3", ip_ad3);
    				editor.putInt("ip_ad4", ip_ad4);
    				editor.putInt("ip_port", ip_port);
    				editor.putString("ip_command", ip_command);

    				editor.commit();

    				new RestartApp().execute();
    			}
    			break;
    	}
    }

    
	public class DoRead extends AsyncTask<String, Void, MjpegInputStream> {
	    protected MjpegInputStream doInBackground(String... url) {
	        //TODO: if camera has authentication deal with it and don't just not work
	        HttpResponse res = null;
	        DefaultHttpClient httpclient = new DefaultHttpClient(); 
	        HttpParams httpParams = httpclient.getParams();
	        HttpConnectionParams.setConnectionTimeout(httpParams, 5*1000);
	        Log.d(TAG, "1. Sending http request");
	        try {
	            res = httpclient.execute(new HttpGet(URI.create(url[0])));
	            Log.d(TAG, "2. Request finished, status = " + res.getStatusLine().getStatusCode());
	            if(res.getStatusLine().getStatusCode()==401){
	                //You must turn off camera User Access Control before this will work
	                return null;
	            }
	            return new MjpegInputStream(res.getEntity().getContent());  
	        } catch (ClientProtocolException e) {
	            e.printStackTrace();
	            Log.d(TAG, "Request failed-ClientProtocolException", e);
	            //Error connecting to camera
	        } catch (IOException e) {
	            e.printStackTrace();
	            Log.d(TAG, "Request failed-IOException", e);
	            //Error connecting to camera
	        }
	        return null;
	    }
	
	    protected void onPostExecute(MjpegInputStream result) {
	        mv.setSource(result);
	        if(result!=null) result.setSkip(1);
	        mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
	        mv.showFps(false);
	    }
	}
    
    public class RestartApp extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... v) {
			MjpegActivity.this.finish();
            return null;
        }

        protected void onPostExecute(Void v) {
        	startActivity((new Intent(MjpegActivity.this, MjpegActivity.class)));
        }
    }
}
