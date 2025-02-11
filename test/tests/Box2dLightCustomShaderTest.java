package tests;

import batchs.NormalBatch;
import box2dLight.*;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.box2d.Box2d;
import com.badlogic.gdx.box2d.Box2dPlus;
import com.badlogic.gdx.box2d.enums.b2BodyType;
import com.badlogic.gdx.box2d.structs.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import shaders.LightShaderWithNormal;
import shaders.NormalShader;

import java.util.ArrayList;

public class Box2dLightCustomShaderTest extends InputAdapter implements ApplicationListener {

    public static final float SCALE = 1.f / 16.f;
    public static final float viewportWidth = 48;
    public static final float viewportHeight = 32;
    static final int RAYS_PER_BALL = 64;
    static final int BALLSNUM = 12;
    static final float LIGHT_DISTANCE = 6f;
    static final float RADIUS = 0.5f;
    private final static int MAX_FPS = 30;
    public final static float TIME_STEP = 1f / MAX_FPS;
    private final static int MIN_FPS = 15;
    private final static float MAX_STEPS = 1f + MAX_FPS / MIN_FPS;
    //	TextureRegion textureRegion;
    private final static float MAX_TIME_PER_FRAME = TIME_STEP * MAX_STEPS;
    OrthographicCamera camera;
    Viewport viewport;
    SpriteBatch spriteBatch;
    NormalBatch normalBatch;
    BitmapFont font;
    /** our box2D world **/
    b2WorldId world;
    /** our boxes **/
    ArrayList<b2BodyId> balls = new ArrayList<b2BodyId>(BALLSNUM);
    /** our ground box **/
    b2BodyId groundBody;
    /** pixel perfect projection for font rendering */
    Matrix4 normalProjection = new Matrix4();
    boolean showText = true;
    /** BOX2D LIGHT STUFF */
    Vector3 testPoint = new Vector3();
    RayHandler rayHandler;
    ArrayList<Light> lights = new ArrayList<Light>(BALLSNUM);
    float sunDirection = -90f;
    Texture bg, bgN;
    TextureRegion objectReg, objectRegN;
    FrameBuffer normalFbo;
    Array<DeferredObject> assetArray = new Array<DeferredObject>();
    DeferredObject marble;
    ShaderProgram lightShader;
    boolean drawNormals = false;
    Color bgColor = new Color();
    float physicsTimeLeft;
    long aika;
    int times;
    /**
     * we instantiate this vector and the callback here so we don't irritate the
     * GC
     **/

    /** another temporary vector **/
    Vector2 target = new Vector2();
    /**
     * Type of lights to use:
     * 0 - PointLight
     * 1 - ConeLight
     * 2 - ChainLight
     * 3 - DirectionalLight
     */
    int lightsType = 0;
    boolean once = true;

    @Override
    public void create() {
        bg = new Texture(Gdx.files.internal("test/data/bg-deferred.png"));
        bgN = new Texture(Gdx.files.internal("test/data/bg-deferred-n.png"));

        MathUtils.random.setSeed(Long.MIN_VALUE);

        camera = new OrthographicCamera(viewportWidth, viewportHeight);
        camera.update();

        viewport = new ExtendViewport(viewportWidth, viewportHeight, camera);

        normalBatch = new NormalBatch();
        spriteBatch =new SpriteBatch();
        font = new BitmapFont();
        font.setColor(Color.RED);

        TextureRegion marbleD = new TextureRegion(new Texture(
                Gdx.files.internal("test/data/marble.png")));

        TextureRegion marbleN = new TextureRegion(new Texture(
                Gdx.files.internal("test/data/marble-n.png")));

        marble = new DeferredObject(marbleD, marbleN);
        marble.width = RADIUS * 2;
        marble.height = RADIUS * 2;

        createPhysicsWorld();
        Gdx.input.setInputProcessor(this);

        normalProjection.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        /** BOX2D LIGHT STUFF BEGIN */
//        normalShader = NormalShader.createNormalShader();

        lightShader = LightShaderWithNormal.createLightShader();
        RayHandlerOptions options = new RayHandlerOptions();
        options.setDiffuse(true);
        options.setGammaCorrection(true);
        rayHandler = new RayHandler(world, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), options) {
            @Override
            protected void updateLightShader() {
            }

            @Override
            protected void updateLightShaderPerLight(Light light) {
                // light position must be normalized
                float x = (light.getX()) / viewportWidth;
                float y = (light.getY()) / viewportHeight;
                lightShader.setUniformf("u_lightpos", x, y, 0.05f);
                lightShader.setUniformf("u_intensity", 5);
                lightShader.setUniformf("u_falloff",0,0,1);
            }
        };
        rayHandler.setLightShader(lightShader);
        rayHandler.setAmbientLight(0.3f, 0.3f, 0.1f, 0.5f);
        rayHandler.setBlurNum(0);

//        initPointLights();
        new PointLight(rayHandler, 128, Color.WHITE, 24, 16, 16);
        /** BOX2D LIGHT STUFF END */


        objectReg = new TextureRegion(new Texture(Gdx.files.internal("test/data/object-deferred.png")));
        objectRegN = new TextureRegion(new Texture(Gdx.files.internal("test/data/object-deferred-n.png")));

        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 3; y++) {
                DeferredObject deferredObject = new DeferredObject(objectReg, objectRegN);
                deferredObject.x = 4 + x * (deferredObject.diffuse.getRegionWidth() * SCALE + 8);
                deferredObject.y = 4 + y * (deferredObject.diffuse.getRegionHeight() * SCALE + 7);
                deferredObject.color.set(1f, 1f, 1f, 1);
                if (x > 0)
                    deferredObject.rot = true;
                deferredObject.rotation = MathUtils.random(90);
                assetArray.add(deferredObject);
            }
        }
        once = false;
        normalFbo = new FrameBuffer(Pixmap.Format.RGB565, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
    }


    @Override
    public void render() {


        camera.update();

        boolean stepped = fixedStep(Gdx.graphics.getDeltaTime());
        Gdx.gl.glClearColor(1f, 1f, 1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        normalBatch.setProjectionMatrix(camera.combined);
        spriteBatch.setProjectionMatrix(camera.combined);
        for (DeferredObject deferredObject : assetArray) {
            deferredObject.update();
        }
        normalFbo.begin();
        Gdx.gl.glClearColor(0, 0, 0,  0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        normalBatch.begin();
        float bgWidth = bgN.getWidth() * SCALE;
        float bgHeight = bgN.getHeight() * SCALE;
        for (int x = 0; x < 6; x++) {
            for (int y = 0; y < 6; y++) {
                normalBatch.draw(bgN, x * bgWidth, y * bgHeight, bgWidth, bgHeight);
            }
        }
        normalBatch.enableBlending();
        for (DeferredObject deferredObject : assetArray) {
            deferredObject.drawNormal(normalBatch);
            // flush batch or uniform wont change
        }
        for (int i = 0; i < BALLSNUM; i++) {
            b2BodyId ball = balls.get(i);
            Vector2 position = Box2dPlus.b2ToGDX(Box2d.b2Body_GetPosition(ball), new Vector2());
            b2Rot rot = Box2d.b2Body_GetRotation(ball);
            float angle = MathUtils.atan2Deg(rot.s(), rot.c());
            marble.x = position.x - RADIUS;
            marble.y = position.y - RADIUS;
            marble.rotation = angle;
            marble.drawNormal(normalBatch);
        }
        normalBatch.end();
        normalFbo.end();

        Texture normals = normalFbo.getColorBufferTexture();

        spriteBatch.disableBlending();
        spriteBatch.begin();
        if (drawNormals) {
            // draw flipped so it looks ok
            spriteBatch.draw(normals, 0, 0, // x, y
                    viewportWidth / 2, viewportHeight / 2, // origx, origy
                    viewportWidth, viewportHeight, // width, height
                    1, 1, // scale x, y
                    0,// rotation
                    0, 0, normals.getWidth(), normals.getHeight(), // tex dimensions
                    false, true); // flip x, y
        } else {
            for (int x = 0; x < 6; x++) {
                for (int y = 0; y < 6; y++) {
                    spriteBatch.setColor(bgColor.set(x / 5.0f, y / 6.0f, 0.5f, 1));
                    spriteBatch.draw(bg, x * bgWidth, y * bgHeight, bgWidth, bgHeight);
                }
            }
            spriteBatch.setColor(Color.WHITE);
            spriteBatch.enableBlending();
            for (DeferredObject deferredObject : assetArray) {
                deferredObject.draw(spriteBatch);
            }
            for (int i = 0; i < BALLSNUM; i++) {
                b2BodyId ball = balls.get(i);
                Vector2 position = Box2dPlus.b2ToGDX(Box2d.b2Body_GetPosition(ball), new Vector2());
                b2Rot rot = Box2d.b2Body_GetRotation(ball);
                float angle = MathUtils.atan2Deg(rot.s(), rot.c());
                marble.x = position.x - RADIUS;
                marble.y = position.y - RADIUS;
                marble.rotation = angle;
                marble.draw(spriteBatch);
            }
        }
        spriteBatch.end();

        /** BOX2D LIGHT STUFF BEGIN */
        if (!drawNormals) {
            rayHandler.setCombinedMatrix(camera);
            if (stepped) rayHandler.update();
            normals.bind(1);
            rayHandler.render();
        }
        /** BOX2D LIGHT STUFF END */

        long time = System.nanoTime();

        boolean atShadow = rayHandler.pointAtShadow(testPoint.x,
                testPoint.y);
        aika += System.nanoTime() - time;

        /** FONT */
        spriteBatch.setProjectionMatrix(normalProjection);
        spriteBatch.begin();


        font.draw(spriteBatch,
                Integer.toString(Gdx.graphics.getFramesPerSecond())
                        + "mouse at shadows: " + atShadow
                        + " time used for shadow calculation:"
                        + aika / ++times + "ns", 0, 20);

        spriteBatch.end();
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    void clearLights() {
        if (lights.size() > 0) {
            for (Light light : lights) {
                light.remove();
            }
            lights.clear();
        }
//        groundBody.setActive(true);
    }

    void initPointLights() {
        clearLights();
        for (int i = 0; i < BALLSNUM; i++) {
            PointLight light = new PointLight(
                    rayHandler, RAYS_PER_BALL, null, LIGHT_DISTANCE, 0f, 0f);
            light.attachToBody(balls.get(i), RADIUS / 2f, RADIUS / 2f);
            light.setColor(
                    1f, 1f,
                    1f,
                    1f);
            lights.add(light);
        }
    }

    void initConeLights() {
        clearLights();
        for (int i = 0; i < BALLSNUM; i++) {
            ConeLight light = new ConeLight(
                    rayHandler, RAYS_PER_BALL, null, LIGHT_DISTANCE,
                    0, 0, 0f, MathUtils.random(15f, 40f));
            light.attachToBody(
                    balls.get(i),
                    RADIUS / 2f, RADIUS / 2f, MathUtils.random(0f, 360f));
            light.setColor(
                    MathUtils.random(),
                    MathUtils.random(),
                    MathUtils.random(),
                    1f);
            lights.add(light);
        }
    }


    void initDirectionalLight() {
        clearLights();

//        groundBody.setActive(false);
        sunDirection = MathUtils.random(0f, 360f);

        DirectionalLight light = new DirectionalLight(
                rayHandler, 4 * RAYS_PER_BALL, null, sunDirection);
        lights.add(light);
    }

    private boolean fixedStep(float delta) {
        physicsTimeLeft += delta;
        if (physicsTimeLeft > MAX_TIME_PER_FRAME)
            physicsTimeLeft = MAX_TIME_PER_FRAME;

        boolean stepped = false;
        while (physicsTimeLeft >= TIME_STEP) {
            Box2d.b2World_Step(world, TIME_STEP, 4);
            physicsTimeLeft -= TIME_STEP;
            stepped = true;
        }
        return stepped;
    }

    private void createPhysicsWorld() {

        b2WorldDef def = Box2d.b2DefaultWorldDef();
        def.gravity().y(0);
        world = Box2d.b2CreateWorld(def.asPointer());

//        float halfWidth = viewportWidth / 2f;
//        ChainShape chainShape = new ChainShape();
//        chainShape.createLoop(new Vector2[]{
//                new Vector2(0, 0f),
//                new Vector2(viewportWidth, 0f),
//                new Vector2(viewportWidth, viewportHeight),
//                new Vector2(0, viewportHeight)});
//        BodyDef chainBodyDef = new BodyDef();
//        chainBodyDef.type = BodyType.StaticBody;
//        groundBody = world.createBody(chainBodyDef);
//        groundBody.createFixture(chainShape, 0);
//        chainShape.dispose();
        createBoxes();

    }

    private void createBoxes() {
        b2ShapeDef shape = Box2d.b2DefaultShapeDef();

        b2BodyDef bdef = Box2d.b2DefaultBodyDef();

        b2Circle cir = new b2Circle();
        cir.radius(RADIUS);
        bdef.type(b2BodyType.b2_dynamicBody);

        for (int i = 0; i < BALLSNUM; i++) {
            // Create the BodyDef, set a random position above the
            // ground and create a new body
            bdef.position().x(1 + (float) (Math.random() * (viewportWidth - 2)));
            bdef.position().y((1 + (float) (Math.random() * (viewportHeight - 2))));
            bdef.linearVelocity().x(MathUtils.random(1));
            bdef.linearVelocity().y(MathUtils.random(1));
            b2BodyId id = Box2d.b2CreateBody(world, bdef.asPointer());
            balls.add(id);
            Box2d.b2CreateCircleShape(id, shape.asPointer(), cir.asPointer());
        }
//        Box2dPlus.b2CreateBlock(world,new Affine2().translate(24,24),0.5f);
    }


    @Override
    public void dispose() {
        rayHandler.dispose();

        Box2d.b2DestroyWorld(world);
        objectReg.getTexture().dispose();
        objectRegN.getTexture().dispose();

        normalFbo.dispose();
    }


    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }


    private static class DeferredObject {
        TextureRegion diffuse;
        TextureRegion normal;
        Color color = new Color(Color.WHITE);
        float x, y;
        float width, height;
        float rotation;
        boolean rot;

        public DeferredObject(TextureRegion diffuse, TextureRegion normal) {
            this.diffuse = diffuse;
            this.normal = normal;
            width = diffuse.getRegionWidth() * SCALE;
            height = diffuse.getRegionHeight() * SCALE;
        }

        public void update() {
            if (rot) {
                rotation += 1f;
                if (rotation > 360)
                    rotation = 0;
            }
        }

        public void drawNormal(Batch batch) {
            batch.draw(normal, x, y, width / 2, height / 2, width, height, 1, 1, rotation);
        }

        public void draw(Batch batch) {
            batch.setColor(color);
            batch.draw(diffuse, x, y, width / 2, height / 2, width, height, 1, 1, rotation);
            batch.setColor(Color.WHITE);
        }
    }
}
