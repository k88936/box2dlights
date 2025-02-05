package tests;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class CustomShaderDesktopLauncher {

	public static void main(String[] argv) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setTitle("box2d lights test");
		new Lwjgl3Application(new Box2dLightCustomShaderTest(), config);
	}

}
