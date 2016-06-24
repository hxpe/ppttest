package cn.wps.graphics.shape3d;


/**
 * 3元空间向量类
 */
public class Vector3f {
    public float x;
    public float y;
    public float z;
    
 // 下面信息为了缓存利用，避免碎片累积引起GC
    protected Vector3f next;
    private static final Object sPoolSync = new Object();
    private static Vector3f sPool;
    private static int sPoolSize = 0;
    private static int sMaxPoolSize = 16;

    public Vector3f() {
        set(0, 0, 0);
    }

    public Vector3f(float x, float y, float z) {
        set(x, y, z);
    }

    public Vector3f(Vector3f v) {
        set(v);
    }

    static public Vector3f[] createArray(int size) {
        Vector3f[] array = new Vector3f[size];
        for (int i = 0; i < size; i++) {
            array[i] = new Vector3f();
        }

        return array;
    }

    public void set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3f set2(float x, float y, float z) {
        set(x, y, z);
        return this;
    }

    public void set(float x,float y) {
        set(x, y, 0);
    }

    public void set(Vector3f v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
    }

    public Vector3f set2(Vector3f v) {
        set(v);
        return this;
    }

    // 求模
    public float module(){
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    // 求摸平方
    public float moduleSq(){
        return x * x + y * y + z * z;
    }

    // 向量减
    public void sub(Vector3f v){
        this.x -= v.x;
        this.y -= v.y;
        this.z -= v.z;
    }

    public Vector3f sub2(Vector3f v){
        sub(v);
        return this;
    }

    // 向量加
    public void add(Vector3f v){
        this.x += v.x;
        this.y += v.y;
        this.z += v.z;
    }

    public Vector3f add2(Vector3f v){
        add(v);
        return this;
    }

    // 缩放
    public void scale(float s){
        this.x *= s;
        this.y *= s;
        this.z *= s;
    }

    public Vector3f scale2(float s){
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
        }
    }

    public Vector3f normalize2() {
        normalize();
        return this;
    }

    // 点乘
    public float dotProduct(Vector3f v){
        return x * v.x + y * v.y + z * v.z;
    }

    // 叉乘
    public Vector3f crossProduct(Vector3f v) {
        return new Vector3f(
                y * v.z - z * v.y,
                z * v.x - x * v.z,
                x * v.y - y * v.x);
    }

    public Vector3f crossProduct2(Vector3f v) {
        set(y * v.z - z * v.y,
            z * v.x - x * v.z,
            x * v.y - y * v.x);
        return this;
    }

    static public float length(Vector3f p1, Vector3f p2) {
        return (float) Math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y) + (p1.z - p2.z) * (p1.z - p2.z));
    }
    
    public static void clearPool() {
        synchronized(sPoolSync) {
            while(null != sPool) {
            	Vector3f var1 = sPool;
                sPool = var1.next;
                var1.next = null;
                --sPoolSize;
            }
        }
    }

    public static Vector3f obtain() {
        synchronized(sPoolSync) {
            if(sPool != null) {
            	Vector3f var1 = sPool;
                sPool = var1.next;
                var1.next = null;
                --sPoolSize;
                var1.set(0, 0, 0);
                return var1;
            }
        }

        return new Vector3f();
    }

    public void recycle() {
        synchronized(sPoolSync) {
            if(sPoolSize < sMaxPoolSize) {
                this.next = sPool;
                sPool = this;
                ++sPoolSize;
            }
        }
    }
}
