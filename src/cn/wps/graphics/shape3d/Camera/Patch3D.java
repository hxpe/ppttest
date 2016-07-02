package cn.wps.graphics.shape3d.Camera;

import cn.wps.graphics.shape3d.Matrix3D;
import cn.wps.graphics.shape3d.Vector3f;

public class Patch3D {
	public Vector3f mU = new Vector3f();
	public Vector3f mV = new Vector3f();
	public Vector3f mOrigin = new Vector3f();
	public Patch3D() {
		reset();
	}
	
	public void reset() {
	    mOrigin.set(0, 0, 0);
	    mU.set(1.0f, 0, 0);
	    mV.set(0, -1.0f, 0);
	}
	
	void transform(final Matrix3D m, Patch3D dst) {
	    if (dst == null) {
	        dst = this;
	    }
	    m.mapVector(mU, dst.mU);
	    m.mapVector(mV, dst.mV);
	    m.mapPoint(mOrigin, dst.mOrigin);
	}
	
	public void transform(final Matrix3D m) {
		transform(m, null);
	}
}
