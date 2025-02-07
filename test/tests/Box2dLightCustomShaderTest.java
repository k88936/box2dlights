package tests;

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
    SpriteBatch batch;
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
    ShaderProgram normalShader;
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

        batch = new SpriteBatch();
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
        normalShader = createNormalShader();

        lightShader = createLightShader();
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
            }
        };
        rayHandler.setLightShader(lightShader);
        rayHandler.setAmbientLight(0.3f, 0.3f, 0.1f, 0.5f);
        rayHandler.setBlurNum(0);

//        initPointLights();
        new PointLight(rayHandler, 32, Color.WHITE, 24, 40, 8);
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

    private ShaderProgram createLightShader() {
        // Shader adapted from https://github.com/mattdesl/lwjgl-basics/wiki/ShaderLesson6
        final String vertexShader =
                "#version 330 core\n" +
                        "attribute vec4 vertex_positions;\n" //
                        + "attribute vec4 quad_colors;\n" //
                        + "attribute float s;\n"
                        + "uniform mat4 u_projTrans;\n" //
                        + "varying vec4 v_color;\n" //
                        + "void main()\n" //
                        + "{\n" //
                        + "   v_color = s * quad_colors;\n" //
                        + "   gl_Position =  u_projTrans * vertex_positions;\n" //
                        + "}\n";
        final String fragmentShader = "#version 330 core\n" +
                "#ifdef GL_ES\n" //
                + "precision lowp float;\n" //
                + "#define MED mediump\n"
                + "#else\n"
                + "#define MED \n"
                + "#endif\n" //
                + "varying vec4 v_color;\n" //
                + "uniform sampler2D u_normals;\n" //
                + "uniform vec3 u_lightpos;\n" //
                + "uniform vec2 u_resolution;\n" //
                + "uniform float u_intensity = 1.0;\n" //
                + "void main()\n"//
                + "{\n"
                + "  vec2 screenPos = gl_FragCoord.xy / u_resolution.xy;\n"
                + "  vec3 NormalMap = texture2D(u_normals, screenPos).rgb; "
                + "  vec3 LightDir = vec3(u_lightpos.xy - screenPos, u_lightpos.z);\n"

                + "  vec3 N = normalize(NormalMap * 2.0 - 1.0);\n"

                + "  vec3 L = normalize(LightDir);\n"

                + "  float maxProd = max(dot(N, L), 0.0);\n"
                + "" //
                + "  gl_FragColor = v_color * maxProd * u_intensity;\n" //
                + "}";

        ShaderProgram.pedantic = false;
        ShaderProgram lightShader = new ShaderProgram(vertexShader,
                fragmentShader);
        if (!lightShader.isCompiled()) {
            Gdx.app.log("ERROR", lightShader.getLog());
        }

        lightShader.begin();
        lightShader.setUniformi("u_normals", 1);
        lightShader.setUniformf("u_resolution", Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        lightShader.end();

        return lightShader;
    }

    private ShaderProgram createNormalShader() {
        String vertexShader = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
                + "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
                + "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
                + "uniform mat4 u_projTrans;\n" //
                + "uniform float u_rot;\n" //
                + "varying vec4 v_color;\n" //
                + "varying vec2 v_texCoords;\n" //
                + "varying mat2 v_rot;\n" //
                + "\n" //
                + "void main()\n" //
                + "{\n" //
                + "   vec2 rad = vec2(-sin(u_rot), cos(u_rot));\n" //
                + "   v_rot = mat2(rad.y, -rad.x, rad.x, rad.y);\n" //
//                + "   v_rot = mat2(u_projTrans[0].xy,u_projTrans[1].xy);\n" //
//                + "   v_rot = mat2(0,-1,1,0);\n" //
                + "   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
                + "   v_color.a = v_color.a * (255.0/254.0);\n" //
                + "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
                + "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
                + "}\n";
        String fragmentShader = "#ifdef GL_ES\n" //
                + "#define LOWP lowp\n" //
                + "precision mediump float;\n" //
                + "#else\n" //
                + "#define LOWP \n" //
                + "#endif\n" //
                + "varying LOWP vec4 v_color;\n" //
                + "varying vec2 v_texCoords;\n" //
                + "varying mat2 v_rot;\n" //
                + "uniform sampler2D u_texture;\n" //
                + "void main()\n"//
                + "{\n" //
                + "  vec4 normal = texture2D(u_texture, v_texCoords).rgba;\n" //
                // got to translate normal vector to -1, 1 range
                + "  vec2 rotated = v_rot * (normal.xy * 2.0 - 1.0);\n" //
                // and back to 0, 1
                + "  rotated = (rotated.xy / 2.0 + 0.5 );\n" //
                + "  gl_FragColor = vec4(rotated.xy, normal.z, normal.a);\n" //
                + "}";

        ShaderProgram shader = new ShaderProgram(vertexShader, fragmentShader);
        if (!shader.isCompiled()) throw new IllegalArgumentException("Error compiling shader: " + shader.getLog());
        return shader;
    }

    @Override
    public void render() {


        camera.update();

        boolean stepped = fixedStep(Gdx.graphics.getDeltaTime());
        Gdx.gl.glClearColor(1f, 1f, 1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        for (DeferredObject deferredObject : assetArray) {
            deferredObject.update();
        }
        normalFbo.begin();
        batch.disableBlending();
        batch.begin();
        batch.setShader(normalShader);
        normalShader.setUniformf("u_rot", 0f);
        float bgWidth = bgN.getWidth() * SCALE;
        float bgHeight = bgN.getHeight() * SCALE;
        for (int x = 0; x < 6; x++) {
            for (int y = 0; y < 6; y++) {
                batch.draw(bgN, x * bgWidth, y * bgHeight, bgWidth, bgHeight);
            }
        }
        batch.enableBlending();
        for (DeferredObject deferredObject : assetArray) {
            normalShader.setUniformf("u_rot", MathUtils.degreesToRadians * deferredObject.rotation);
            deferredObject.drawNormal(batch);
            // flush batch or uniform wont change
            // TODO this is baaaad, maybe modify SpriteBatch to add rotation in the attributes? Flushing after each defeats the point of batch
            batch.flush();
        }
        for (int i = 0; i < BALLSNUM; i++) {
            b2BodyId ball = balls.get(i);
            Vector2 position = Box2dPlus.b2ToGDX(Box2d.b2Body_GetPosition(ball), new Vector2());
            b2Rot rot = Box2d.b2Body_GetRotation(ball);
            float angle = MathUtils.atan2Deg(rot.s(), rot.c());
            marble.x = position.x - RADIUS;
            marble.y = position.y - RADIUS;
            marble.rotation = angle;
            normalShader.setUniformf("u_rot", MathUtils.degreesToRadians * marble.rotation);
            marble.drawNormal(batch);
            // TODO same as above
            batch.flush();
        }
        batch.end();
        normalFbo.end();

        Texture normals = normalFbo.getColorBufferTexture();

        batch.disableBlending();
        batch.begin();
        batch.setShader(null);
        if (drawNormals) {
            // draw flipped so it looks ok
            batch.draw(normals, 0, 0, // x, y
                    viewportWidth / 2, viewportHeight / 2, // origx, origy
                    viewportWidth, viewportHeight, // width, height
                    1, 1, // scale x, y
                    0,// rotation
                    0, 0, normals.getWidth(), normals.getHeight(), // tex dimensions
                    false, true); // flip x, y
        } else {
            for (int x = 0; x < 6; x++) {
                for (int y = 0; y < 6; y++) {
                    batch.setColor(bgColor.set(x / 5.0f, y / 6.0f, 0.5f, 1));
                    batch.draw(bg, x * bgWidth, y * bgHeight, bgWidth, bgHeight);
                }
            }
            batch.setColor(Color.WHITE);
            batch.enableBlending();
            for (DeferredObject deferredObject : assetArray) {
                deferredObject.draw(batch);
            }
            for (int i = 0; i < BALLSNUM; i++) {
                b2BodyId ball = balls.get(i);
                Vector2 position = Box2dPlus.b2ToGDX(Box2d.b2Body_GetPosition(ball), new Vector2());
                b2Rot rot = Box2d.b2Body_GetRotation(ball);
                float angle = MathUtils.atan2Deg(rot.s(), rot.c());
                marble.x = position.x - RADIUS;
                marble.y = position.y - RADIUS;
                marble.rotation = angle;
                marble.draw(batch);
            }
        }
        batch.end();

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
        batch.setProjectionMatrix(normalProjection);
        batch.begin();


        font.draw(batch,
                Integer.toString(Gdx.graphics.getFramesPerSecond())
                        + "mouse at shadows: " + atShadow
                        + " time used for shadow calculation:"
                        + aika / ++times + "ns", 0, 20);

        batch.end();
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
