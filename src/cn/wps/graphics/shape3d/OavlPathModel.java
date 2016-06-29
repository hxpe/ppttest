package cn.wps.graphics.shape3d;


import android.content.res.Resources;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Bitmap.Config;
import android.graphics.Path.Direction;
import android.util.Log;

public class OavlPathModel extends PolygonPathModel{
	public OavlPathModel(Resources res, Object3D object3d) {
		super(res, object3d);
	}
	
	@Override
	public Path getShapePath() {
		if (mPath != null) {
			return mPath;
		}
		
		mPath = new Path();
		mPath.addOval(mMatrixState.getViewPort(), Direction.CW);
		return mPath;
	}
}
