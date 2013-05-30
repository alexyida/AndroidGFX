package com.qwertech.androidgfx;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.view.Menu;
import android.view.MotionEvent;

public class MainActivity extends Activity {
	
	private class MainGLSurfaceView extends GLSurfaceView{
		private CubeRenderer mRenderer;
		
		public MainGLSurfaceView(Context context) {
			super(context);
		}
		
		@Override
		public boolean onTouchEvent(MotionEvent event) {
			if (event != null)
				if (event.getAction() == MotionEvent.ACTION_DOWN)
					if (mRenderer != null) {
						// Ensure we call switchMode() on the OpenGL thread.
						// queueEvent() is a method of GLSurfaceView that will do this for us.
						queueEvent(new Runnable() {
							@Override
							public void run() {
								mRenderer.switchMode();
							}
						});
						
						return true;
					}
			
			return super.onTouchEvent(event);
		}
		
		// Hides superclass method.
		public void setRenderer(CubeRenderer renderer) {
			mRenderer = renderer;
			super.setRenderer(renderer);
		}
	}
	
	/** Hold a reference to our GLSurfaceView */
	private MainGLSurfaceView mGLSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mGLSurfaceView = new MainGLSurfaceView(this);
        
        // Check if the system supports OpenGL ES 2.0.
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;
        
        if (supportsEs2) {
        	// Request an OpenGL ES 2.0 compatible context.
        	mGLSurfaceView.setEGLContextClientVersion(2);
        	
        	// Set the renderer to our demo renderer, defined below.
        	mGLSurfaceView.setRenderer(new CubeRenderer(this));
        } else {
        	// This is where you could create an OpenGL ES 1.x compatible
            // renderer if you wanted to support both ES 1 and ES 2.
        	return;
        }
        
        setContentView(mGLSurfaceView);
    }
    
    @Override
    protected void onResume() {
    	// The activity must call the GL surface view's onResume() on activity onResume().
    	super.onResume();
    	mGLSurfaceView.onResume();
    }
    
    @Override
    protected void onPause() {
    	// The activity must call the GL surface view's onPause() on activity onPause().
    	super.onPause();
    	mGLSurfaceView.onPause();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
}
