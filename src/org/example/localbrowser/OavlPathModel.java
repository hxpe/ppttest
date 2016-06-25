package org.example.localbrowser;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Path;
import android.graphics.Path.Direction;
import cn.wps.graphics.shape3d.ModelBase;

public class OavlPathModel extends ModelBase{
	private Path mPath;
	private Resources mRes;
	
	public OavlPathModel(Resources res) {
		super();
		mRes = res;
	}
	
	@Override
	public Path getShapePath() {
		if (mPath != null) {
			return mPath;
		}
		
		mPath = new Path();
		mPath.addOval(mMatrixState.mViewPort, Direction.CW);
		return mPath;
	}
	
	private Bitmap cacheBitmap;
	protected Bitmap getTextureBitmap(){
		if (cacheBitmap == null) {
			cacheBitmap = BitmapFactory.decodeResource(mRes, R.drawable.mesh);
		}
		return cacheBitmap;
	}
}
