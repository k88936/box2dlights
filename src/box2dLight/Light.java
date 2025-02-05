package box2dLight;

import com.badlogic.gdx.box2d.Box2d;
import com.badlogic.gdx.box2d.structs.b2BodyId;
import com.badlogic.gdx.box2d.structs.b2Filter;
import com.badlogic.gdx.box2d.structs.b2ShapeId;
import com.badlogic.gdx.box2d.structs.b2Vec2;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.jnigen.runtime.closure.ClosureObject;
import com.badlogic.gdx.jnigen.runtime.pointer.VoidPointer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntArray;

/**
 * Light is data container for all the light parameters. When created lights
 * are automatically added to rayHandler and could be removed by calling
 * {@link #remove()} and added manually by calling {@link #add(RayHandler)}.
 *
 * <p>Implements {@link Disposable}
 *
 * @author kalle_h
 */
public abstract class Light implements Disposable {

    /**
     * Dynamic shadows variables *
     */
    protected static final LightData tmpData = new LightData(0f);
    static final Color DefaultColor = new Color(0.75f, 0.75f, 0.5f, 0.75f);
    static final float zeroColorBits = Color.toFloatBits(0f, 0f, 0f, 0f);
    static final float oneColorBits = Color.toFloatBits(1f, 1f, 1f, 1f);
    static final int MIN_RAYS = 3;
    /**
     * Global lights filter
     **/
    static private b2Filter globalFilterA = null;
    protected final Color color = new Color();
    protected final Vector2 tmpPosition = new Vector2();
    protected final Array<Mesh> dynamicShadowMeshes = new Array<Mesh>();
    //Should never be cleared except when the light changes position (not direction). Prevents shadows from disappearing when fixture is out of sight.
    protected final Array<b2ShapeId> affectedFixtures = new Array<>();
    protected final Array<Vector2> tmpVerts = new Array<Vector2>();
    protected final IntArray ind = new IntArray();
    protected final Vector2 tmpStart = new Vector2();
    protected final Vector2 tmpEnd = new Vector2();
    protected final Vector2 tmpVec = new Vector2();
//    protected final b2Vec2 tmpb2Vec = new b2Vec2();
    protected final Vector2 center = new Vector2();
    protected RayHandler rayHandler;
    protected boolean active = true;
    protected boolean soft = true;
    protected boolean xray = false;
    protected boolean staticLight = false;
    protected boolean culled = false;
    protected boolean dirty = true;
    protected boolean ignoreBody = false;
    protected int rayNum;
    protected int vertexNum;
    protected float distance;
    protected float direction;
    protected float colorF;
    protected float softShadowLength = 2.5f;
    protected Mesh lightMesh;
    protected Mesh softShadowMesh;
    protected float segments[];
    protected float[] mx;
    protected float[] my;
    protected float[] f;
    protected int m_index = 0;
    protected float pseudo3dHeight = 0f;
    /**
     * This light specific filter
     **/
    private b2Filter filterA = null;

    final ClosureObject<Box2d.b2CastResultFcn> ray =ClosureObject.fromClosure(new Box2d.b2CastResultFcn() {

        @Override
        public float b2CastResultFcn_call(b2ShapeId shapeId, b2Vec2 point, b2Vec2 normal, float fraction, VoidPointer context) {


            if ((globalFilterA != null) && !globalContactFilter(shapeId))
                return -1;

            if ((filterA != null) && !contactFilter(shapeId))
                return -1;

            if (ignoreBody && Box2d.b2Shape_GetBody(shapeId).equals(getBody()))
                return -1;

            // if (fixture.isSensor())
            // return -1;
            mx[m_index] = point.x();
            my[m_index] = point.y();
            f[m_index] = fraction;
            return fraction;
        }
    });
    final ClosureObject<Box2d.b2OverlapResultFcn> dynamicShadowCallback =ClosureObject.fromClosure( new Box2d.b2OverlapResultFcn() {

        @Override
        public boolean b2OverlapResultFcn_call(b2ShapeId shapeId, VoidPointer context) {

            if (!onDynamicCallback(shapeId)) {
                return true;
            }
            affectedFixtures.add(shapeId);
//            if (Box2d.b2Shape_GetUserData(shapeId) instanceof LightData) {
//        }todo can be null ?
                LightData data = LightData.mapper.get(shapeId);
                data.shadowsDropped++;
            return true;
        }

    });

    /**
     * Creates new active light and automatically adds it to the specified
     * {@link RayHandler} instance.
     *
     * @param rayHandler      not null instance of RayHandler
     * @param rays            number of rays - more rays make light to look more realistic
     *                        but will decrease performance, can't be less than MIN_RAYS
     * @param color           light color
     * @param distance        light distance (if applicable), soft shadow length is set to distance * 0.1f
     * @param directionDegree direction in degrees (if applicable)
     */
    public Light(RayHandler rayHandler, int rays, Color color,
                 float distance, float directionDegree) {
        rayHandler.lightList.add(this);
        this.rayHandler = rayHandler;
        setRayNum(rays);
        setColor(color);
        setDistance(distance);
        setSoftnessLength(distance * 0.1f);
        setDirection(directionDegree);
    }

    /**
     * Sets given contact filter for ALL LIGHTS
     */
    static public void setGlobalContactFilter(b2Filter filter) {
        globalFilterA = filter;
    }

    /**
     * Creates new contact filter for ALL LIGHTS with give parameters
     *
     * @param categoryBits - see {@link b2Filter#categoryBits}
     * @param groupIndex   - see {@link b2Filter#groupIndex}
     * @param maskBits     - see {@link b2Filter#maskBits}
     */
    static public void setGlobalContactFilter(short categoryBits, short groupIndex,
                                              short maskBits) {
        globalFilterA = new b2Filter();
        globalFilterA.groupIndex(groupIndex);
        globalFilterA.categoryBits(categoryBits);
        globalFilterA.maskBits(maskBits);
    }

    /**
     * Updates this light
     */
    abstract void update();

    /**
     * Render this light
     */
    abstract void render();

    /**
     * Render this light shadow
     */
    protected void dynamicShadowRender() {
        for (Mesh m : dynamicShadowMeshes) {
            m.render(rayHandler.lightShader, GL20.GL_TRIANGLE_STRIP);
        }
    }

    /**
     * Attaches light to specified body
     *
     * @param body that will be automatically followed, note that the body
     *             rotation angle is taken into account for the light offset
     *             and direction calculations
     */
    public abstract void attachToBody(b2BodyId body);

    /**
     * @return attached body or {@code null}
     * @see #attachToBody(b2BodyId)
     */
    public abstract b2BodyId getBody();

    /**
     * Sets light starting position
     *
     * @see #setPosition(Vector2)
     */
    public abstract void setPosition(float x, float y);

    /**
     * @return horizontal starting position of light in world coordinates
     */
    public abstract float getX();

    /**
     * @return vertical starting position of light in world coordinates
     */
    public abstract float getY();

    /**
     * @return starting position of light in world coordinates
     * <p>NOTE: changing this vector does nothing
     */
    public Vector2 getPosition() {
        return tmpPosition;
    }

    /**
     * Sets light starting position
     *
     * @see #setPosition(float, float)
     */
    public abstract void setPosition(Vector2 position);

    /**
     * Sets light color
     *
     * <p>NOTE: you can also use colorless light with shadows, e.g. (0,0,0,1)
     *
     * @param r lights color red component
     * @param g lights color green component
     * @param b lights color blue component
     * @param a lights shadow intensity
     * @see #setColor(Color)
     */
    public void setColor(float r, float g, float b, float a) {
        color.set(r, g, b, a);
        colorF = color.toFloatBits();
        if (staticLight) dirty = true;
    }

    /**
     * Adds light to specified RayHandler
     */
    public void add(RayHandler rayHandler) {
        this.rayHandler = rayHandler;
        if (active) {
            rayHandler.lightList.add(this);
        } else {
            rayHandler.disabledLights.add(this);
        }
    }

    /**
     * Removes light from specified RayHandler and disposes it
     */
    public void remove() {
        remove(true);
    }

    /**
     * Removes light from specified RayHandler and disposes it if requested
     */
    public void remove(boolean doDispose) {
        if (active) {
            rayHandler.lightList.removeValue(this, false);
        } else {
            rayHandler.disabledLights.removeValue(this, false);
        }
        rayHandler = null;
        if (doDispose) dispose();
    }

    /**
     * Disposes all light resources
     */
    public void dispose() {
        affectedFixtures.clear();
        lightMesh.dispose();
        softShadowMesh.dispose();
        for (Mesh mesh : dynamicShadowMeshes) {
            mesh.dispose();
        }
        dynamicShadowMeshes.clear();
    }

    /**
     * @return if this light is active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Enables/disables this light update and rendering
     */
    public void setActive(boolean active) {
        if (active == this.active)
            return;

        this.active = active;
        if (rayHandler == null)
            return;

        if (active) {
            rayHandler.lightList.add(this);
            rayHandler.disabledLights.removeValue(this, true);
        } else {
            rayHandler.disabledLights.add(this);
            rayHandler.lightList.removeValue(this, true);
        }
    }

    /**
     * @return if this light beams go through obstacles
     */
    public boolean isXray() {
        return xray;
    }

    /**
     * Enables/disables x-ray beams for this light
     *
     * <p>Enabling this will allow beams go through obstacles that reduce CPU
     * burden of light about 70%.
     *
     * <p>Use the combination of x-ray and non x-ray lights wisely
     */
    public void setXray(boolean xray) {
        this.xray = xray;
        if (staticLight) dirty = true;
    }

    /**
     * @return if this light is static
     * <p>Static light do not get any automatic updates but setting
     * any parameters will update it. Static lights are useful for
     * lights that you want to collide with static geometry but ignore
     * all the dynamic objects.
     */
    public boolean isStaticLight() {
        return staticLight;
    }

    /**
     * Enables/disables this light static behavior
     *
     * <p>Static light do not get any automatic updates but setting any
     * parameters will update it. Static lights are useful for lights that you
     * want to collide with static geometry but ignore all the dynamic objects
     *
     * <p>Reduce CPU burden of light about 90%
     */
    public void setStaticLight(boolean staticLight) {
        this.staticLight = staticLight;
        if (staticLight) dirty = true;
    }

    /**
     * @return if tips of this light beams are soft
     */
    public boolean isSoft() {
        return soft;
    }

    /**
     * Enables/disables softness on tips of this light beams
     */
    public void setSoft(boolean soft) {
        this.soft = soft;
        if (staticLight) dirty = true;
    }

    /**
     * @return softness value for beams tips
     * <p>Default: {@code 2.5f}
     */
    public float getSoftShadowLength() {
        return softShadowLength;
    }

    /**
     * Sets softness value for beams tips
     *
     * <p>Default: {@code 2.5f}
     */
    public void setSoftnessLength(float softShadowLength) {
        this.softShadowLength = softShadowLength;
        if (staticLight) dirty = true;
    }

    /**
     * @return current color of this light
     */
    public Color getColor() {
        return color;
    }

    /**
     * Sets light color
     *
     * <p>NOTE: you can also use colorless light with shadows, e.g. (0,0,0,1)
     *
     * @param newColor RGB set the color and Alpha set intensity
     * @see #setColor(float, float, float, float)
     */
    public void setColor(Color newColor) {
        if (newColor != null) {
            color.set(newColor);
        } else {
            color.set(DefaultColor);
        }
        colorF = color.toFloatBits();
        if (staticLight) dirty = true;
    }

    /**
     * @return rays distance of this light (without gamma correction)
     */
    public float getDistance() {
        return distance / RayHandler.gammaCorrectionParameter;
    }

    /**
     * Sets light distance
     *
     * <p>NOTE: MIN value should be capped to 0.1f meter
     */
    public abstract void setDistance(float dist);

    /**
     * @return direction in degrees (0 if not applicable)
     */
    public float getDirection() {
        return direction;
    }

    /**
     * Sets light direction
     */
    public abstract void setDirection(float directionDegree);

    /**
     * Checks if given point is inside of this light area
     *
     * @param x - horizontal position of point in world coordinates
     * @param y - vertical position of point in world coordinates
     */
    public boolean contains(float x, float y) {
        return false;
    }

    /**
     * @return if the attached body fixtures will be ignored during raycasting
     */
    public boolean getIgnoreAttachedBody() {
        return ignoreBody;
    }

    /**
     * Sets if the attached body fixtures should be ignored during raycasting
     *
     * @param flag - if {@code true} all the fixtures of attached body
     *             will be ignored and will not create any shadows for this
     *             light. By default is set to {@code false}.
     */
    public void setIgnoreAttachedBody(boolean flag) {
        ignoreBody = flag;
    }

    public void setHeight(float height) {
        this.pseudo3dHeight = height;
    }

    /**
     * @return number of rays set for this light
     */
    public int getRayNum() {
        return rayNum;
    }

    /**
     * Internal method for mesh update depending on ray number
     */
    void setRayNum(int rays) {
        if (rays < MIN_RAYS)
            rays = MIN_RAYS;

        rayNum = rays;
        vertexNum = rays + 1;

        segments = new float[vertexNum * 8];
        mx = new float[vertexNum];
        my = new float[vertexNum];
        f = new float[vertexNum];
    }

    boolean contactFilter(b2ShapeId fixtureB) {
        b2Filter filterB = Box2d.b2Shape_GetFilter(fixtureB);

        int a = filterA.groupIndex();
        if (a != 0 &&
                a == filterB.groupIndex())
            return a > 0;

        long ma = filterA.maskBits();
        long lb = filterB.categoryBits();
        long la = filterA.categoryBits();
        long mb = filterB.maskBits();
        return (ma & lb) != 0 &&
                (la & mb) != 0;
    }

    /**
     * Sets given contact filter for this light
     */
    public void setContactFilter(b2Filter filter) {
        filterA = filter;
    }

    /**
     * Creates new contact filter for this light with given parameters
     *
     * @param categoryBits - see {@link b2Filter#categoryBits}
     * @param groupIndex   - see {@link b2Filter#groupIndex}
     * @param maskBits     - see {@link b2Filter#maskBits}
     */
    public void setContactFilter(short categoryBits, short groupIndex,
                                 short maskBits) {
        filterA = new b2Filter();
        filterA.categoryBits(categoryBits);
        filterA.groupIndex(groupIndex);
        filterA.maskBits(maskBits);
    }

    boolean globalContactFilter(b2ShapeId fixtureB) {
        b2Filter filterB = Box2d.b2Shape_GetFilter(fixtureB);

        int ia = globalFilterA.groupIndex();
        if (ia != 0 &&
                ia == filterB.groupIndex())
            return ia > 0;


        long ma = globalFilterA.maskBits();
        long lb = filterB.categoryBits();
        long la = globalFilterA.categoryBits();
        long mb = filterB.maskBits();
        return (ma & lb) != 0 &&
                (la & mb) != 0;
    }

    protected boolean onDynamicCallback(b2ShapeId fixture) {

        if ((globalFilterA != null) && !globalContactFilter(fixture)) {
            return false;
        }

        if ((filterA != null) && !contactFilter(fixture)) {
            return false;
        }

        if (ignoreBody && Box2d.b2Shape_GetBody(fixture).equals(getBody())) {
            return false;
        }
        //We only add the affectedFixtures once
        return !affectedFixtures.contains(fixture, true);
    }

}
