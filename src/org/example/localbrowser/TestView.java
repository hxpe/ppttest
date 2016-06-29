package org.example.localbrowser;

import java.io.File;

import org.example.localbrowser.pathgradfill.CircleGradFill;
import org.example.localbrowser.pathgradfill.LinGradFill;
import org.example.localbrowser.pathgradfill.PathGradFillBase;
import org.example.localbrowser.pathgradfill.RectGradFill;
import org.example.localbrowser.pathgradfill.ShapeGradFill;

import cn.wps.graphics.shape3d.ModelBase;
import cn.wps.graphics.shape3d.OavlPathModel;
import cn.wps.graphics.shape3d.Object3D;
import cn.wps.graphics.shape3d.PolygonPathModel;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.Path.Direction;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.graphics.SweepGradient;
import android.graphics.Xfermode;
import android.graphics.drawable.BitmapDrawable;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class TestView extends View {
	
	static public interface ILog {
		public void log(String msg);
	}
	
	private VerticesControl mVerControl;
	
	Bitmap mCache; 
	private Bitmap getCacheBitmap() {
		if (mCache == null) {
			mCache = Bitmap.createBitmap(this.getWidth(), this.getHeight(), 
					Config.ARGB_8888);
		} else if (mCache.getWidth() != this.getWidth() ||
				mCache.getHeight() != this.getHeight()) {
			mCache.recycle();
			mCache = Bitmap.createBitmap(this.getWidth(), this.getHeight(), 
					Config.ARGB_8888);
		}
		
		return mCache;
	}
	
	private Canvas getCacheCavas() {
		Canvas canvas = new Canvas(getCacheBitmap());
		return canvas;
	}

	private ILog mLog;
	public void setLog(ILog log) {
		this.mLog = log;
	}
	
	private void log(String msg) {
		if (mLog != null) {
			mLog.log(msg);
		}
	}
	
	public TestView(Context context) {
		super(context);
	}

	public TestView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	private void testRecolor(Canvas canvas) {
		File file = new File("/storage/sdcard1/hxpe/test1/test.png");
		if (file.exists()) {
			Bitmap temp = BitmapFactory.decodeFile("/storage/sdcard1/hxpe/test1/test.png");
			if (temp != null) {
				Paint p = new Paint();
				p.setColor(Color.RED);
				canvas.drawBitmap(temp, 0, 0, p);
			}
		}
	}

	public void onDraw(Canvas canvas) {
		long startCount = System.currentTimeMillis();
		testDrawOavl(canvas);
		long endCount = System.currentTimeMillis();
		long des = endCount - startCount;
		Log.d("onDraw", "testDrawVertices time:" + des);
	}
	
	private void testRectRotation(Canvas canvas) {
		final float rot = 130;
		RectF testRctF = new RectF(100, 100, 400, 300);
		Paint p = new Paint();
		p.setColor(Color.BLUE);
		canvas.drawRect(testRctF, p);
		p.setColor(Color.GREEN);
		p.setAlpha(50);
		canvas.save();
		canvas.rotate(rot, testRctF.centerX(), testRctF.centerY());
		canvas.drawRect(testRctF, p);
		canvas.restore();
		p.setColor(Color.RED);
		p.setAlpha(50);
		canvas.drawRect(PathGradFillBase.getRotationRect(testRctF, rot), p);
	}
	
	private void testLinGradFill(Canvas canvas) {
		Paint p = new Paint();

		int[] colors = new int[3];
		float[] positions = new float[3];

		colors[0] = Color.BLUE;
		colors[1] = Color.YELLOW;
		colors[2] = Color.GREEN;
		positions[0] = 0f;
		positions[1] = 0.5f;
		positions[2] = 1f;
		
		RectF dstRect = new RectF(100, 50, this.getHeight() + 100, this.getHeight()-100);
		Path path = new Path();
		path.addRect(dstRect, Direction.CW);
		
		RectF fillToRect = null;
		RectF tileRect = new RectF(1f/3, 1f/3, 1f/3, 1f/3);
		
		LinGradFill gradFill = new LinGradFill(path, canvas, p, dstRect, fillToRect, tileRect, colors, positions);
		gradFill.setLinParam(false, 45);
		gradFill.setRotParam(false, 30);
		
		canvas.save();
		Matrix matrix = new Matrix();
		matrix.setRotate(30, dstRect.centerX(), dstRect.centerY());
		canvas.concat(matrix);
		gradFill.gradFill();
		canvas.restore();
	}
	
	private void testShapeGradFill(Canvas canvas) {
		Paint p = new Paint();

		int[] colors = new int[3];
		float[] positions = new float[3];

		colors[0] = Color.BLUE;
		colors[1] = Color.YELLOW;
		colors[2] = Color.GREEN;
		positions[0] = 0f;
		positions[1] = 0.5f;
		positions[2] = 1f;
		
		RectF dstRect = new RectF(0, 0, this.getHeight(), this.getHeight());
		Path path = new Path();
		path.addRect(dstRect, Direction.CW);
		
		RectF fillToRect = new RectF(0.5f, 0.5f, 0.5f, 0.5f);
		RectF tileRect = new RectF(0, 0, 0, 0);
		
		ShapeGradFill gradFill = new ShapeGradFill(path, canvas, p, dstRect, fillToRect, tileRect, colors, positions);
		
		PointF[] points = getStar5Poins(dstRect);
		gradFill.setPoints(points);
		gradFill.gradFill();
	}
	
	private PointF createCenter(RectF dstRect, float xOffset, float yOffset){
		PointF centerF = new PointF(dstRect.centerX(), dstRect.centerY());
		centerF.offset(xOffset, yOffset);
		return centerF;
	}
	
	private PointF[] getStar5Poins(RectF dstRect) {
		float r54 = (float)Math.PI * 54/180;
		float r18 = (float)Math.PI * 18/180;
		PointF[] vertexes = new PointF[10];
		float radiusOut = dstRect.width() / 2;
		float radiusIn = 0.3819660112501f * radiusOut;
		vertexes[0] = createCenter(dstRect, 0, -radiusOut);
		vertexes[1] = createCenter(dstRect, (float)(Math.cos(r54) * radiusIn), (float)(-Math.sin(r54) * radiusIn));
		vertexes[2] = createCenter(dstRect, (float)(Math.cos(r18) * radiusOut), (float)(-Math.sin(r18) * radiusOut));
		vertexes[3] = createCenter(dstRect, (float)(Math.cos(r18) * radiusIn), (float)(Math.sin(r18) * radiusIn));
		vertexes[4] = createCenter(dstRect, (float)(Math.cos(r54) * radiusOut), (float)(Math.sin(r54) * radiusOut));
		vertexes[5] = createCenter(dstRect, 0, radiusIn);
		vertexes[6] = createCenter(dstRect, (float)(-Math.cos(r54) * radiusOut), (float)(Math.sin(r54) * radiusOut));
		vertexes[7] = createCenter(dstRect, (float)(-Math.cos(r18) * radiusIn), (float)(Math.sin(r18) * radiusIn));
		vertexes[8] = createCenter(dstRect, (float)(-Math.cos(r18) * radiusOut), (float)(-Math.sin(r18) * radiusOut));
		vertexes[9] = createCenter(dstRect, (float)(-Math.cos(r54) * radiusIn), (float)(-Math.sin(r54) * radiusIn));
		
		return vertexes;
	}
	
	private void testRectGradFill(Canvas canvas) {
		Paint p = new Paint();

		int[] colors = new int[3];
		float[] positions = new float[3];
		
		canvas.drawColor(Color.WHITE);

		colors[0] = 0;
		colors[1] = -6506386;
		colors[2] = -15373549;
		positions[0] = 0f;
		positions[1] = 0.5f;
		positions[2] = 1f;
		
		RectF dstRect = new RectF(100, 100, this.getHeight() + 100, this.getHeight() - 100);
		Path path = new Path();
		path.addRect(dstRect, Direction.CW);
		
		RectF fillToRect = new RectF(0.4f, 0.4f, 0.4f, 0.4f);
		RectF tileRect = new RectF(0, 0, 0, 0);
		
		Matrix matrix = new Matrix();
		matrix.preTranslate(dstRect.centerX(), dstRect.centerY());
		matrix.preRotate(0);
		matrix.preTranslate(-dstRect.centerX(), -dstRect.centerY());
		canvas.concat(matrix);
		
		RectGradFill gradFill = new RectGradFill(path, canvas, p, dstRect, fillToRect, tileRect, colors, positions);
		gradFill.setRotParam(true, 0);
		gradFill.gradFill();
	}
	
	private Bitmap getTextureBitmap(float width, float height) {
		Paint p = new Paint();

		int[] colors = new int[3];
		float[] positions = new float[3];

		colors[0] = Color.BLUE;
		colors[1] = Color.YELLOW;
		colors[2] = Color.GREEN;
		positions[0] = 0f;
		positions[1] = 0.4f;
		positions[2] = 1f;
		
		RectF dstRect = new RectF(0, 0, width, height);
		Path path = new Path();
		path.addRect(dstRect, Direction.CW);
		
		RectF fillToRect = new RectF(0.5f, 0.5f, 0.5f, 0.5f);
		RectF tileRect = new RectF(0, 0, 0, 0);
		
		Bitmap cacheBitmap = Bitmap.createBitmap((int)width, (int)height, Config.ARGB_8888);
		Canvas cacheCanvas = new Canvas(cacheBitmap);
		
		CircleGradFill gradFill = new CircleGradFill(path, cacheCanvas, p, dstRect, fillToRect, tileRect, colors, positions);
		gradFill.setRotParam(false, 0);
		gradFill.gradFill();
		
		return cacheBitmap;
	}
	
	private final Paint mPaint = new Paint();
	private final int VerTexCount = 10;
    private final float[] mVerts = new float[VerTexCount];
    private final float[] mTexs = new float[VerTexCount];
    private final int[] mColors = new int[VerTexCount]; // 虽然只用到一半，但要保留和顶点一样的长度，否则崩溃
    private final short[] mIndices = { 1, 4, 2};

    private Camera mCamera = new Camera();
    private Matrix mMatrix = new Matrix();
	
	private void testDrawVertices(Canvas canvas) {
		Bitmap textureBimatp = getDrawableBitmap();
		Shader s = new BitmapShader(textureBimatp, Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP);
		mPaint.setShader(s);
		
		int w = textureBimatp.getWidth();
        int h = textureBimatp.getHeight();
        
        // construct our mesh
        setXY(mTexs, 0, w/2, h/2);
        setXY(mTexs, 1, 0, 0);
        setXY(mTexs, 2, w, 0);
        setXY(mTexs, 3, w, h);
        setXY(mTexs, 4, 0, h);

        setXY(mVerts, 0, w/2, h/2);
        setXY(mVerts, 1, 300, 300);
        setXY(mVerts, 2, w, 0);
        setXY(mVerts, 3, w, h);
        setXY(mVerts, 4, 0, h);
        
        setXY(mColors, 0, 0x88FFFFFF);
        setXY(mColors, 1, 0x88FF0000);
        setXY(mColors, 2, 0x8800FF00);
        setXY(mColors, 3, 0x880000FF);
        setXY(mColors, 4, 0x8800FFFF);

        canvas.drawColor(0x00);
        canvas.save();
//        canvas.concat(getCameraMatrix(w, h));
        canvas.drawVertices(Canvas.VertexMode.TRIANGLE_FAN, VerTexCount, mVerts, 0,
        		mTexs, 0, mColors, 0, mIndices, 0, mIndices.length, mPaint);
        canvas.restore();

//        canvas.save();
//        canvas.translate(this.getWidth() / 2, 0);
//        mMatrix.reset();
//        mMatrix.setRotate(45);
//        canvas.concat(mMatrix);
//        canvas.drawVertices(Canvas.VertexMode.TRIANGLE_FAN, VerTexCount, mVerts, 0,
//                            mTexs, 0, mColors, 0, mIndices, 0, mIndices.length, mPaint);
//
//        canvas.restore();
	}
	
	private void testDrawVertices2(Canvas canvas) {
		Bitmap textureBimatp = getDrawableBitmap();
		Shader s = new BitmapShader(textureBimatp, Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP);
		mPaint.setShader(s);
		
		int w = textureBimatp.getWidth();
        int h = textureBimatp.getHeight();
        
        RectF renderRect = new RectF(0, 0, w, h);
        if (mVerControl == null) {
        	mVerControl = new VerticesControl();
        	mVerControl.init(10, 10, renderRect);
        }
        mVerControl.update(new RectF(0, 0, w, h));

        canvas.drawColor(0x00);
//        int restoreToCount = canvas.saveLayer(null, mSavePaint, Canvas.ALL_SAVE_FLAG);
        canvas.save();
        canvas.drawVertices(Canvas.VertexMode.TRIANGLES, mVerControl.mCacheArrayCount, mVerControl.mVerts, 0,
        		mVerControl.mTexs, 0, mVerControl.mColors, 0, mVerControl.mIndices, 0, mVerControl.mIndices.length, mPaint);
        canvas.restore();
//        Canvas cacheCavas = this.getCacheCavas();
//        cacheCavas.drawColor(0x00FFFFFF, PorterDuff.Mode.SRC);
//        cacheCavas.drawVertices(Canvas.VertexMode.TRIANGLE_STRIP, mVerControl.mCacheArrayCount, mVerControl.mVerts, 0,
//        		null, 0, mVerControl.mColors, 0, mVerControl.mIndices, 0, mVerControl.mIndicesRealCount, mPaint);
//        mPaint.setXfermode(multiplyMode);
//        canvas.drawBitmap(this.getCacheBitmap(), 0, 0, mPaint);
//        mPaint.setXfermode(null);
//        canvas.restoreToCount(restoreToCount);
	}
	
	ModelBase mOavl; 
	private void create3DModel() {
		if (mOavl == null) {
			mOavl  = new PolygonPathModel(this.getResources(), new Object3D());
			mOavl.init(new RectF(0, 0, 1000, 1000));
		}
	}
	private void testDrawOavl(Canvas canvas) {
		create3DModel();
		canvas.drawColor(0xffffffff);
		mOavl.draw(canvas);
	}
	
	private Paint mSavePaint = new Paint();
	private Xfermode multiplyMode = new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY);
	
	private Bitmap cacheBitmap;
	private Bitmap getDrawableBitmap() {
		if (cacheBitmap == null) {
			cacheBitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.front);
		}
		return cacheBitmap;
	}
	
	private Matrix getCameraMatrix(int w, int h) {
		mMatrix.reset();
		mCamera.save();
		mCamera.rotateY(15);
		mCamera.rotateX(30);
		mCamera.getMatrix(mMatrix);
		mMatrix.preTranslate(0, -h/2);
		mMatrix.postTranslate(0, h/2);
		mCamera.restore();
		return mMatrix;
	}
	
	private boolean mAniming = false;
	private boolean mStop = false;
	public void toggleAniation() {
		if (!mAniming) {
			startCuzierAnimation();
		} else {
			if (mOavl != null)
				mOavl.mAnimTest.stop();
			mStop = true;
		}
	}
	
	private void startCuzierAnimation() {
		create3DModel();
		mOavl.mAnimTest.start();
        
        mAniming = true;
        mStop = false;
        this.postInvalidate();
        this.postDelayed(new Runnable() {
        	public void run() {
        		mOavl.update();
        		if (!mStop) {
        			postInvalidate();
        			postDelayed(this, 40);
        		} else {
        			mAniming = false;
        		}
        	}
        }, 40);
	}
	
	private void testDrawVertices3(Canvas canvas) {
        if (mVerControl == null) {
        	return;
        }
        canvas.drawColor(0x00);
        canvas.save();
        canvas.drawVertices(Canvas.VertexMode.TRIANGLES, mVerControl.mCacheArrayCount, mVerControl.mVerts, 0,
        		mVerControl.mTexs, 0, mVerControl.mColors, 0, mVerControl.mIndices, 0, mVerControl.mIndicesRealCount, mPaint);
        canvas.restore();
	}
	
	private static void setXY(float[] array, int index, float x, float y) {
        array[index*2 + 0] = x;
        array[index*2 + 1] = y;
    }
	
	private static void setXY(int[] array, int index, int x) {
        array[index] = x;
    }
	
	private void testDrawLineLinearGradient(Canvas canvas) {
		Paint p = new Paint();

		int[] colors = new int[3];
		float[] positions = new float[3];

		colors[0] = Color.BLUE;
		colors[1] = Color.YELLOW;
		colors[2] = Color.GREEN;
		positions[0] = 0f;
		positions[1] = 0.5f;
		positions[2] = 1f;
		
		Path path = new Path();
		RectF dstRect = new RectF(0, 0, this.getHeight(), this.getHeight());
		PointF[] points = getStar5Poins(dstRect);
		path.moveTo(points[0].x, points[0].y);
		for (int i = 1; i < points.length; i++)
			path.lineTo(points[i].x, points[i].y);
		path.close();
		
		RectF fillToRect = new RectF(0.5f, 0.5f, 0.5f, 0.5f);
		RectF tileRect = new RectF(0, 0, 0, 0);
		
		ShapeGradFill gradFill = new ShapeGradFill(path, canvas, p, dstRect, fillToRect, tileRect, colors, positions);
		gradFill.gradFill();
	}
	
	private void testLinearGradient(Canvas canvas) {
		Paint p = new Paint();

		int[] colors = new int[3];
		float[] positions = new float[3];

		colors[0] = Color.BLUE;
		colors[1] = Color.YELLOW;
		colors[2] = Color.GREEN;
		positions[0] = 0f;
		positions[1] = 0.5f;
		positions[2] = 1f;
		float midY = this.getHeight() / 2;
		float len = this.getWidth() / 2;
		LinearGradient shader = new LinearGradient(0, midY, len, midY, colors,
				positions, TileMode.MIRROR);
		Matrix matrix = new Matrix();
		matrix.setRotate(0, len / 2, midY);
		shader.setLocalMatrix(matrix);

		p.setShader(shader);
		canvas.drawRect(0, 0, this.getWidth(), this.getHeight(), p);
	}
	
	private void testRadialGradient(Canvas canvas) {
		Paint p = new Paint();

		int[] colors = new int[3];
		float[] positions = new float[3];

		colors[0] = Color.BLUE;
		colors[1] = Color.YELLOW;
		colors[2] = Color.GREEN;
		positions[0] = 0f;
		positions[1] = 0.5f;
		positions[2] = 1f;
		float midY = this.getHeight() / 2;
		float len = this.getWidth() / 2;
		float radius = this.getWidth() - len / 2;
		RadialGradient shader = new RadialGradient(len / 2, midY,
				radius, colors, positions, TileMode.MIRROR);
		Matrix matrix = new Matrix();
		matrix.setRotate(0, len / 2, midY);
		shader.setLocalMatrix(matrix);

		p.setShader(shader);
		canvas.drawRect(0, 0, this.getWidth(), this.getHeight(), p);
	}
	
	private void testSweepGradient(Canvas canvas) {
		Paint p = new Paint();

		int[] colors = new int[3];
		float[] positions = new float[3];

		colors[0] = Color.BLUE;
		colors[1] = Color.YELLOW;
		colors[2] = Color.GREEN;
		positions[0] = 0f;
		positions[1] = 0.5f;
		positions[2] = 1f;
		float midY = this.getHeight() / 2;
		float len = this.getWidth() / 2;
		SweepGradient shader = new SweepGradient(len / 2, midY, colors,
				positions);
		Matrix matrix = new Matrix();
		matrix.setRotate(0, len / 2, midY);
		shader.setLocalMatrix(matrix);
		p.setShader(shader);
		canvas.drawRect(0, 0, this.getWidth(), this.getHeight(), p);
	}
}
