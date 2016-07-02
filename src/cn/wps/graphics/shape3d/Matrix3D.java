package cn.wps.graphics.shape3d;

import android.opengl.Matrix;

public class Matrix3D {
    private final static float[] sTemp = new float[16];
    private float[] m = new float[16];

    public Matrix3D() {
        Matrix.setIdentityM(m, 0);
    }

    public Matrix3D(float[] value) {
        System.arraycopy(value, 0, m, 0, m.length);
    }

    public Matrix3D(Matrix3D src) {
        System.arraycopy(src.m, 0, m, 0, m.length);
    }

    public float[] getValues() {
        return m;
    }

    public static Matrix3D concat(Matrix3D lhs, Matrix3D rhs) {
        Matrix3D result = new Matrix3D();
        Matrix.multiplyMM(result.m, 0, lhs.m, 0, rhs.m, 0);
        return result;
    }
    public static float[] mapPoint(Matrix3D mat, float x, float y){
        float[] src = new float[] { x, y, 0, 1};
        Matrix.multiplyMV(src, 0, mat.getValues(), 0, src, 0);
        return src;
    }
    // points: {x0,y0, x1,y1, x2,y2, ...}
    public static void mapPoint(Matrix3D mat, float[] dst, int offset, float[] points){
        if (dst.length < points.length){
            throw new IllegalArgumentException("dst.length < points.length");
        }
        float[] src = new float[] {0, 0, 0, 1};
        for (int i = 0; i < points.length; i += 2){
            src[0] = points[i];
            src[1] = points[i + 1];
            Matrix.multiplyMV(src, 0, mat.getValues(), 0, src, 0);
            dst[offset + i] = src[0];
            dst[offset + i + 1] = src[1];
        }
    }
    
    public static void mapPoint(Matrix3D mat, float[] dst, float[] points){
        mapPoint(mat, dst, 0, points);
    }

    public void preConcat(Matrix3D hs) {
        synchronized(sTemp) {
            System.arraycopy(m, 0, sTemp, 0, m.length);
            Matrix.multiplyMM(m, 0, sTemp, 0, hs.m, 0);
        }
    }

    public void forwardConcat(Matrix3D hs) {
        synchronized(sTemp) {
            System.arraycopy(m, 0, sTemp, 0, m.length);
            Matrix.multiplyMM(m, 0, hs.m, 0, sTemp, 0);
        }
    }

    public void invertAndTranspose(Matrix3D dest) {
        synchronized(sTemp) {
            Matrix.invertM(sTemp, 0, m, 0);
            Matrix.transposeM(dest.m, 0, sTemp, 0);
        }
    }
    
    public void invertAndTranspose() {
        synchronized(sTemp) {
            Matrix.invertM(sTemp, 0, m, 0);
            Matrix.transposeM(m, 0, sTemp, 0);
        }
    }

    public void invert() {
        synchronized(sTemp) {
            Matrix.invertM(sTemp, 0, m, 0);
            System.arraycopy(sTemp, 0, m, 0, m.length);
        }
    }

    public void setOrtho(float left, float right, float bottom, float top, float near, float far) {
        Matrix.orthoM(m, 0, left, right, bottom, top, near, far);
    }

    public void setFrustum(float left, float right, float bottom, float top, float near, float far) {
        Matrix.frustumM(m, 0, left, right, bottom, top, near, far);
        m[8] /= 2.0f ; // bug,see http://code.google.com/p/android/issues/detail?id=35646
    }
    public void setPerspective(float angleInDegree, float aspect, float near, float far){
        Matrix.perspectiveM(m, 0, angleInDegree, aspect, near, far);
    }
    public void setLookAt(float eyex, float eyey, float eyez,
                          float viewx, float viewy, float viewz,
                          float upx, float upy, float upz){
        Matrix.setLookAtM(m, 0, eyex, eyey, eyez, viewx, viewy, viewz, upx, upy, upz);
    }

    public void setTranslate(float x, float y) {
        Matrix.setIdentityM(m, 0);
        translate(x, y);
    }
    public void setTranslate3d(float x, float y, float z) {
        Matrix.setIdentityM(m, 0);
        translate3d(x, y, z);
    }
    public void translate(float x, float y){
        Matrix.translateM(m, 0, x, y, 0);
    }
    public void translate3d(float x, float y, float z){
        Matrix.translateM(m, 0, x, y, z);
    }

    public void setRotation(float r, float x, float y){
        Matrix.setIdentityM(m, 0);
        rotate(r, x, y);
    }
    public void setRotation3d(float r, float x, float y, float z){
        Matrix.setIdentityM(m, 0);
        rotate3d(r, x, y, z);
    }
    public void rotate(float r, float x, float y){
        // 2d情况下：行向量
        //      1.先平移到原点:move(-x, -y)
        //      2.旋转:rotate(r)
        //      3.恢复原来位置:move(x, y)
        // 3d情况下：opengl是列向量,矩阵计算顺序跟2d相反(另一方面理解是3d中matrix变换的是物体坐标系，而不是物体)
        //      1.move(x, y)
        //      2.rotate(r)
        //      3.move(-x, -y)
        Matrix.translateM(m, 0, x, y, 0);
        Matrix.rotateM(m, 0, r, 0, 0, 1); // 绕z轴旋转
        Matrix.translateM(m, 0, -x, -y, 0);
    }
    public void rotate3d(float r, float x, float y, float z){
        // 绕轴（0,0,0)->(x,y,z)旋转
        Matrix.rotateM(m, 0, r, x, y, z);
    }

    public void setScale(float sx, float sy, float x, float y) {
        Matrix.setIdentityM(m, 0);
        scale(sx, sy, x, y);
    }
    public void setScale3d(float sx, float sy, float sz) {
        Matrix.setIdentityM(m, 0);
        scale3d(sx, sy, sz);
    }
    public void scale(float sx, float sy, float x, float y){
        Matrix.translateM(m, 0, x, y, 0);
        Matrix.scaleM(m, 0, sx, sy, 0);
        Matrix.translateM(m, 0, -x, -y, 0);
    }
    public void scale3d(float sx, float sy, float sz){
        Matrix.scaleM(m, 0, sx, sy, sz);
    }
    public void setSkew(float b, float d, float x, float y){
        Matrix.setIdentityM(m, 0);
        skew(b, d, x, y);
    }
    public void skew(float b, float d, float x, float y){
        Matrix.translateM(m, 0, x, y, 0);
        _skew(b, d, 0, 0, 0, 0);
        Matrix.translateM(m, 0, -x, -y, 0);
    }
    /*
    *   b:沿y含x错切
    *   d:沿x含y错切
    *   h:沿x含z错切
    *   i:沿y含z错切
    *   c:沿z含x错切
    *   f:沿z含y错切
    */
    private void _skew(float b, float d, float h, float i, float c, float f){
        float[] skew = new float[] {
                1, d, c, 0,
                b, 1, f, 0,
                h, i, 1, 0,
                0, 0, 0, 1
        };
        Matrix.multiplyMM(m, 0, m, 0, skew, 0);
    }

    public void setMatrix(Matrix3D src) {
        System.arraycopy(src.m, 0, m, 0, m.length);
    }

    public void reset(){
        Matrix.setIdentityM(m, 0);
    }

    public void rotYAxisAtXOffset(float degree, float xoffset) {
        translate(xoffset, 0);
        rotate3d(degree, 0, 1, 0);
        translate(-xoffset, 0);
    }

    public void rotXAxisAtYOffset(float degree, float yoffset) {
        translate(0, yoffset);
        rotate3d(degree, 1, 0, 0);
        translate(0, -yoffset);
    }
    
    public void mapPoint(final Vector3f src, Vector3f dst) {
    	float [] srcArray = new float[]{src.x, src.y, src.z};
        float x = scalarDot(3, m, 0, 4, srcArray, 0, 1) + m[3 * 4];
        float y = scalarDot(3, m, 1, 4, srcArray, 0, 1) + m[3 * 4 + 1];
        float z = scalarDot(3, m, 2, 4, srcArray, 0, 1) + m[3 * 4 + 2];
        dst.set(x, y, z);
    }

    public void mapVector(final Vector3f src, Vector3f dst) {
    	float [] srcArray = new float[]{src.x, src.y, src.z};
        float x = scalarDot(3, m, 0, 4, srcArray, 0, 1);
        float y = scalarDot(3, m, 1, 4, srcArray, 0, 1);
        float z = scalarDot(3, m, 2, 4, srcArray, 0, 1);
        dst.set(x, y, z);
    }
    
    public static float scalarDotDiv(int count, float a[], int offset_a, int step_a,
            float b[], int offset_b, int step_b, float denom) {
		float prod = 0;
		for (int i = 0; i < count; i++) {
			prod += a[offset_a] * b[offset_b];
			offset_a += step_a;
			offset_b += step_b;
		}
		return prod / denom;
	}
	
	public static float scalarDot(int count, float a[], int offset_a, int step_a,
            float b[], int offset_b, int step_b) {
		float prod = 0;
		for (int i = 0; i < count; i++) {
			prod += a[offset_a] * b[offset_b];
			offset_a += step_a;
			offset_b += step_b;
		}
		return prod;
	}
}
