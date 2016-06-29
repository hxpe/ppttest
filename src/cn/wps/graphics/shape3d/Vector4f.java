package cn.wps.graphics.shape3d;

/**
 *	四元空间向量类
 */
public class Vector4f {
	public float x;
    public float y;
    public float z;
    public float w;

    public Vector4f() {
        set(0, 0, 0, 1);
    }

    public Vector4f(float x, float y, float z, float w) {
        set(x, y, z, w);
    }
    
    public Vector4f(float x, float y, float z) {
        set(x, y, z);
    }

    public Vector4f(Vector4f v) {
        set(v);
    }
    
    public Vector4f(Vector3f v) {
        set(v);
    }

    static public Vector4f[] createArray(int size) {
        Vector4f[] array = new Vector4f[size];
        for (int i = 0; i < size; i++) {
            array[i] = new Vector4f();
        }

        return array;
    }

    public void set(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vector4f set2(float x, float y, float z, float w) {
        set(x, y, z, w);
        return this;
    }

    public void set(float x,float y) {
        set(x, y, 0, 1);
    }
    
    public void set(float x, float y, float z) {
        set(x, y, z, 1);
    }
    
    public void set(Vector3f v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        this.w = 1;
    }
    
    public Vector4f set2(Vector3f v) {
        set(v);
        return this;
    }

    public void set(Vector4f v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        this.w = v.w;
    }

    public Vector4f set2(Vector4f v) {
        set(v);
        return this;
    }

    // 求模
    public float module(){
        return (float) Math.sqrt(x * x + y * y + z * z + w * w);
    }

    // 求摸平方
    public float moduleSq(){
        return x * x + y * y + z * z + w * w;
    }

    // 向量减
    public void sub(Vector4f v){
        this.x -= v.x;
        this.y -= v.y;
        this.z -= v.z;
        this.w -= v.w;
    }

    public Vector4f sub2(Vector4f v){
        sub(v);
        return this;
    }

    // 向量加
    public void add(Vector4f v){
        this.x += v.x;
        this.y += v.y;
        this.z += v.z;
        this.w += v.w;
    }

    public Vector4f add2(Vector4f v){
        add(v);
        return this;
    }

    // 缩放
    public void scale(float s){
        this.x *= s;
        this.y *= s;
        this.z *= s;
        this.w *= s;
    }

    public Vector4f scale2(float s){
        scale(s);
        return this;
    }

    // 向量规格化
    public void normalize() {
        float mod = module();
        if (mod != 0) {
            x = x / mod;
            y = y / mod;
            z = z / mod;
            w = w / mod;
        }
    }

    public Vector4f normalize2() {
        normalize();
        return this;
    }
}
