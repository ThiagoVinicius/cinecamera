package br.ufpb.lavid.cinecamera;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.FillType;
import android.graphics.PathEffect;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class CineCameraActivity extends Activity {
	private Preview mPreview;
	private DrawOnTop mDrawOnTop;
    Camera mCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Create a RelativeLayout container that will hold a SurfaceView,
        // and set it as the content of our activity.
        mPreview = new Preview(this);
        mDrawOnTop = new DrawOnTop(this);
        
        FrameLayout contentView = new FrameLayout(this);
        contentView.addView(mPreview);
        contentView.addView(mDrawOnTop);
        
        
        
        //mPreview.setVisibility(View.INVISIBLE);
        
        setContentView(contentView);
        contentView.setKeepScreenOn(true);
        
//        addContentView(mDrawOnTop, new LayoutParams 
//        		(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)); 
        //mPreview.addView(mDrawOnTop);
        //mPreview.add

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Open the default i.e. the first rear facing camera.
        mCamera = Camera.open();
        Parameters p = mCamera.getParameters();
        p.setExposureCompensation(0);
        mCamera.setParameters(p);
        mCamera.setPreviewCallback(new PreProcessor(mDrawOnTop));
        mPreview.setCamera(mCamera);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        if (mCamera != null) {
            mPreview.setCamera(null);
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

}

class DrawOnTop extends View {
	
	public Rect filter;
	public Path edges[];
	public int bigger;
	public PointF center;
	private Drawable image;
	
	public DrawOnTop(Context context) {
		super(context);
		image = context.getResources().getDrawable(R.drawable.cristo_color);
		image.setBounds(0, 0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		if (filter != null) {
		    Paint p = new Paint();
		    p.setColor(Color.argb(127, 201, 221, 238));
		    canvas.drawRect(filter, p);
		}
		if (edges != null) {
			Paint normalPaint = new Paint();
			normalPaint.setColor(Color.YELLOW);
			normalPaint.setStyle(Style.STROKE);
			normalPaint.setStrokeWidth(3);
			
			Paint biggerPaint = new Paint(normalPaint);
			biggerPaint.setColor(Color.RED);
			int i = 0;
			for (Path border : edges) {
				if (i == bigger) {
					canvas.drawPath(border, biggerPaint);
				} else {
					canvas.drawPath(border, normalPaint);
				}
				++i;
			}
		}
		if (center != null) {
			//Log.d("DrawOnTop", "desenhando!! (" + center.x + ", " + center.y + ")");
			//Log.d("DrawOnTop", "Imagem: " + image.getBounds());
			canvas.save();
			canvas.translate(-image.getIntrinsicWidth()/2, -image.getIntrinsicHeight()/2);
			//canvas.translate(/*getWidth()*/0 , getHeight());
			canvas.translate(center.x, center.y);
			image.draw(canvas);
			canvas.restore();
		}
	    invalidate();
	}
	
}

// ----------------------------------------------------------------------

/**
 * A simple wrapper around a Camera and a SurfaceView that renders a centered preview of the Camera
 * to the surface. We need to center the SurfaceView because not all devices have cameras that
 * support preview sizes at the same aspect ratio as the device's display.
 */
class Preview extends SurfaceView implements SurfaceHolder.Callback {
    private final String TAG = "Preview";

    SurfaceHolder mHolder;
    Size mPreviewSize;
    List<Size> mSupportedPreviewSizes;
    Camera mCamera;

    Preview(Context context) {
        super(context);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCamera(Camera camera) {
        mCamera = camera;
        if (mCamera != null) {
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
        	mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
        }
    }


    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        requestLayout();

        mCamera.setParameters(parameters);
        mCamera.startPreview();
    }
    
}



class PreProcessor implements Camera.PreviewCallback {
	
	private DrawOnTop drawer;
	
	private BorderWalker walker;
	
	public PreProcessor(DrawOnTop drawer) {
		this.drawer = drawer;
	}
	
	public void onPreviewFrame(byte[] data, Camera camera) {
		Size size = camera.getParameters().getPreviewSize();
		
		if (walker == null || walker.width != size.width || walker.height != size.height) {
			walker = new BorderWalker(size.width, size.height);
		}
		
		walker.reset();
		List<PixelBorder> borders = walker.findAllBorders(data);
		Log.d("Borders", "Borders found: "+borders.size());
		
		RectF bounds[] = new RectF[borders.size()];
		Path edges[] = new Path[borders.size()];
		PointF center = null;
		float maxArea = 0f;
		int maxAreaIdx = -1;
		Iterator<PixelBorder> iter = borders.iterator();
		for (int i = 0; i < bounds.length; ++i) {
			edges[i] = iter.next().asPolygonalPath();
			edges[i].computeBounds(bounds[i] = new RectF(), true);
			float area = bounds[i].width() * bounds[i].height();
			if (maxArea < area) {
				maxArea = area;
				maxAreaIdx = i;
				center = new PointF(bounds[i].centerX(), bounds[i].centerY());
			}
		}
		
		drawer.edges = edges;
		drawer.bigger = maxAreaIdx;
		drawer.center = center;
		
		
		//drawer.filter = new Rect(xc-RADIUS, yc-RADIUS, xc+RADIUS, yc+RADIUS);
		
	}
	
}