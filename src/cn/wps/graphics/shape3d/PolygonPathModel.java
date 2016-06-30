package cn.wps.graphics.shape3d;

import org.example.localbrowser.R;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.Path.Direction;

public class PolygonPathModel extends ModelBase {
	protected Path mPath;
	protected Resources mRes;
	
	public PolygonPathModel(Resources res, Object3D object3d) {
		super(object3d);
		mRes = res;
	}
	
	@Override
	public Path getShapePath() {
		if (mPath != null) {
			return mPath;
		}
		
		mPath = new Path();
		mPath.addRect(mMatrixState.getViewPort(), Direction.CW);
		return mPath;
	}
	
	private Bitmap mFrontTexture;
	public Bitmap getFrontTexture(){
		if (mFrontTexture == null) {
			mFrontTexture = Bitmap.createBitmap((int)Math.ceil(mMatrixState.getViewPort().width()), 
					(int)Math.ceil(mMatrixState.getViewPort().height()), Config.ARGB_8888);
			Bitmap res = BitmapFactory.decodeResource(mRes, R.drawable.front);
			Paint paint = new Paint();
			Canvas canvas = new Canvas(mFrontTexture);
			canvas.drawBitmap(res, new Rect(0, 0, res.getWidth(), res.getHeight()), 
					new RectF(0, 0, mFrontTexture.getWidth(), mFrontTexture.getHeight()), paint);
			res.recycle();
		}
		return mFrontTexture;
	}
	
	private Bitmap mBackTexture;
	public Bitmap getBackTexture(){
		if (mBackTexture == null) {
			mBackTexture = Bitmap.createBitmap((int)Math.ceil(mMatrixState.getViewPort().width()), 
					(int)Math.ceil(mMatrixState.getViewPort().height()), Config.ARGB_8888);
			Bitmap res = BitmapFactory.decodeResource(mRes, R.drawable.back);
			Paint paint = new Paint();
			Canvas canvas = new Canvas(mBackTexture);
			canvas.drawBitmap(res, new Rect(0, 0, res.getWidth(), res.getHeight()), 
					new RectF(0, 0, mBackTexture.getWidth(), mBackTexture.getHeight()), paint);
			res.recycle();
		}
		return mBackTexture;
	}
}
