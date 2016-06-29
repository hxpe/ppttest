package cn.wps.graphics.shape3d;

public class Light {
	private static Vector3f sLightDirection = new Vector3f(0, 0, 1.0f); // 方向光方向
    private static Vector4f sLightDiffuse = new Vector4f(1.0f, 1.0f, 1.0f); // 散射光强度
    private static Vector4f sLightAmbient = new Vector4f(0f, 00f, 0f); // 环境光强度
    
	private Vector4f pvLight = new Vector4f();
	private Vector3f pvNormalTrans = new Vector3f();
	private ModelBase mModel;
	
	public Light(ModelBase model) {
		this.mModel = model;
	}
	
	public int calcLight(Vector3f normal) {
    	mModel.getMatrixState().normalMap(normal, pvNormalTrans);
    	pvNormalTrans.normalize();
    	pvLight.set2(sLightDiffuse).scale(Math.abs(pvNormalTrans.dotProduct(sLightDirection)));;
    	pvLight.add(sLightAmbient);
    	int color = toColor(pvLight);
    	return color;
	}
    
	private int toColor(Vector4f v) {
    	if (v.w > 1.0f) {
    		v.w  = 1.0f;
    	}
    	int color = ((int)(v.w * 255) << 24) | ((int)(v.x * 255) << 16) 
    			| ((int)(v.y * 255) << 8) | (int)(v.z * 255);
    	return color;
    }
    
    
}
