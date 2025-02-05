package tests;

import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.box2d.Box2d;
import com.badlogic.gdx.box2d.structs.b2WorldDef;
import com.badlogic.gdx.box2d.structs.b2WorldId;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;

public class SimpleTest extends ApplicationAdapter {
    /** the camera **/
    OrthographicCamera camera;
    RayHandler rayHandler;
    b2WorldId world;

    @Override
    public void create() {
        Box2d.initialize();
        camera = new OrthographicCamera(48, 32);
        camera.update();
        b2WorldDef b2WorldDef = Box2d.b2DefaultWorldDef();
        world = Box2d.b2CreateWorld(b2WorldDef.asPointer());
        rayHandler = new RayHandler(world);
        new PointLight(rayHandler, 32);

    }

    @Override
    public void render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Box2d.b2World_Step(world, 1 / 60f, 4);
        rayHandler.setCombinedMatrix(camera);
        rayHandler.updateAndRender();
    }
}
