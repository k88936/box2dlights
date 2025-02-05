package box2dLight;

import com.badlogic.gdx.box2d.Box2dPlus;
import com.badlogic.gdx.box2d.structs.*;

public class LightData {

	static final Box2dPlus.BodyUserDataMapper<b2ShapeId,LightData> mapper =new Box2dPlus.BodyUserDataMapper<>();

	public Object userData = null;

	public float height;

	public boolean shadow;

	int shadowsDropped = 0;

	public LightData (float h) {
		height = h;
	}

	public LightData (float h, boolean shadow) {
		height = h;
		this.shadow = shadow;
	}

	public LightData (Object data, float h, boolean shadow) {
		height = h;
		userData = data;
		this.shadow = shadow;
	}

	public float getLimit (float distance, float lightHeight, float lightRange) {
		float l = 0f;
		if (lightHeight > height) {
			l = distance * height / (lightHeight - height);
			float diff = lightRange - distance;
			if (l > diff) {
				l = diff;
			}
		} else if (lightHeight == 0f) {
			l = lightRange;
		} else {
			l = lightRange - distance;
		}

		return l > 0 ? l : 0f;
	}



}
