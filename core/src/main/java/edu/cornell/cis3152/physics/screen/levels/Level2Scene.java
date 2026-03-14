package edu.cornell.cis3152.physics.screen.levels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.cis3152.physics.InputController;
import edu.cornell.cis3152.physics.world.*;
import edu.cornell.cis3152.physics.screen.PhysicsScene;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.audio.SoundEffect;
import edu.cornell.gdiac.audio.SoundEffectManager;
import edu.cornell.gdiac.graphics.TextLayout;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.physics2.Obstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;

public class Level2Scene extends PhysicsScene implements ContactListener {
    private SoundEffect jumpSound;
    private SoundEffect fireSound;
    private SoundEffect plopSound;
    private float volume;

    private Zuko avatar;

    private Array<Picture> pictures = new Array<>();
    private Picture activePicture;

    private GameObject rock;
    private GameObject cloud;
    private GameObject ice;

    private float rockLiftCeilingY;
    private boolean rockLiftActive;

    private boolean cloudDropActive;
    private boolean cloudReturnActive;
    private float cloudLiftCeilingY;

    private boolean iceOnCloudActive;
    private boolean iceOnRockActive;
    private boolean rockOnIceActive;
    private boolean cloudOnIceActive;

    private static final float ROCK_DENSITY = 5.0f;
    private static final float ROCK_FRICTION = 5.0f;
    private static final float CLOUD_BASE_DENSITY = 2.0f;
    private static final float CLOUD_BASE_FRICTION = 0.0f;
    private static final float CLOUD_LIFT_GRAVITY = -0.5f;
    private static final float ICE_DENSITY = 3.0f;
    private static final float ICE_FRICTION = 0.0f;
    private static final float ICE_RESTITUTION = 0.3f;
    private static final float ICE_SLIDE_DENSITY = 0.3f;
    private static final float ICE_BOUNCE_RESTITUTION = 2.5f;
    private static final float ICE_DAMPING = 1.0f;
    private static final float NORMAL_DAMPING = 10.0f;

    private boolean playerOnIce;

    private float STICK_PICTURE_DISTANCE = 5.0f;
    private float TAKE_PICTURE_DISTANCE = 10.0f;
    private boolean showRange = false;
    private Array<GameObject> highlighted = new Array<>();
    private final Affine2 highlightTransform = new Affine2();

    private TextLayout cameraLabel;
    private OrthographicCamera textCamera;
    private BitmapFont font;

    protected ObjectSet<Fixture> sensorFixtures;

    public Level2Scene(AssetDirectory directory) {
        super(directory, "platform");
        world.setContactListener(this);
        sensorFixtures = new ObjectSet<Fixture>();

        jumpSound = directory.getEntry("platform-jump", SoundEffect.class);
        fireSound = directory.getEntry("platform-pew", SoundEffect.class);
        plopSound = directory.getEntry("platform-plop", SoundEffect.class);
        volume = constants.getFloat("volume", 1.0f);

        textCamera = new OrthographicCamera();
        textCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        font = new BitmapFont();
        cameraLabel = new TextLayout();
        cameraLabel.setFont( font );
    }

    public void reset() {
        JsonValue values = constants.get("world");
        Vector2 gravity = new Vector2(0, values.getFloat("gravity"));

        if (world != null) {
            for (ObstacleSprite sprite : sprites) {
                sprite.getObstacle().deactivatePhysics(world);
            }
        }
        sprites.clear();
        addQueue.clear();

        if (world != null) {
            Array<Body> bodies = new Array<>();
            world.getBodies(bodies);
            for (Body b : bodies) {
                world.destroyBody(b);
            }
        }

        if (world == null) {
            world = new World(gravity, false);
            world.setContactListener(this);
        }

        setComplete(false);
        setFailure(false);

        activePicture = null;
        if (pictures != null) {
            pictures.clear();
        }

        rockLiftActive = false;
        cloudDropActive = false;
        cloudReturnActive = false;
        iceOnCloudActive = false;
        iceOnRockActive = false;
        rockOnIceActive = false;
        cloudOnIceActive = false;
        playerOnIce = false;

        populateLevel();
    }

    private void populateLevel() {
        float units = height / bounds.height;

        Pixmap icePixmap = new Pixmap(128, 128, Pixmap.Format.RGBA8888);
        icePixmap.setColor(0.7f, 0.78f, 0.85f, 1.0f);
        icePixmap.fill();
        Texture iceGroundTexture = new Texture(icePixmap);
        icePixmap.dispose();

        Texture texture = directory.getEntry("shared-earth", Texture.class);
        Texture earthTexture = texture;

        Surface wall;
        String wname = "wall";
        JsonValue walls = constants.get("walls");
        JsonValue walljv = walls.get("positions");
        for (int ii = 0; ii < walljv.size; ii++) {
            wall = new Surface(walljv.get(ii).asFloatArray(), units, walls);
            wall.getObstacle().setName(wname + ii);
            wall.setTexture(texture);
            addSprite(wall);
        }

        Surface platform;
        String pname = "playgroundPlatforms";
        JsonValue plats = constants.get("playgroundPlatforms");
        JsonValue platjv = plats.get("positions");
        for (int ii = 0; ii < platjv.size; ii++) {
            float[] verts = platjv.get(ii).asFloatArray();
            platform = new Surface(verts, units, plats);
            platform.getObstacle().setName(pname + ii);

            float centerX = (verts[0] + verts[2]) / 2.0f;
            if (centerX < 16.0f) {
                platform.setTexture(iceGroundTexture);
                platform.getObstacle().setFriction(0.0f);
            } else {
                platform.setTexture(earthTexture);
            }
            addSprite(platform);
        }

        texture = directory.getEntry("platform-traci", Texture.class);
        avatar = new Zuko(units, constants.get("traci"));
        avatar.setTexture(texture);
        addSprite(avatar);
        avatar.createSensor();

        float rockSize = 1.5f;
        float cloudSize = 1.5f;

        rock = new GameObject(
                Obj.ROCK, constants.get("rock"), units,
                5.0f, 4.0f + rockSize / 2.0f,
                rockSize, rockSize,
                BodyDef.BodyType.DynamicBody, false
        );
        rock.getObstacle().setDensity(ROCK_DENSITY);
        rock.getObstacle().setFriction(ROCK_FRICTION);
        rock.getObstacle().setRestitution(0.0f);
        rock.setTexture(earthTexture);
        addSprite(rock);

        Texture cloudTexture = directory.getEntry("cloud", Texture.class);
        cloud = new GameObject(
                Obj.CLOUD, constants.get("cloud"), units,
                17.0f, 5.5f,
                cloudSize, cloudSize,
                BodyDef.BodyType.DynamicBody, false
        );
        cloud.getObstacle().setDensity(CLOUD_BASE_DENSITY);
        cloud.getObstacle().setFriction(CLOUD_BASE_FRICTION);
        cloud.getObstacle().setRestitution(0.0f);
        cloud.getObstacle().setGravityScale(0.0f);
        cloud.setTexture(cloudTexture);
        addSprite(cloud);

        float iceSize = 1.5f;
        ice = new GameObject(
                Obj.ICE, constants.get("ice"), units,
                25.0f, 4.0f + iceSize / 2.0f,
                iceSize, iceSize,
                BodyDef.BodyType.StaticBody, false
        );
        ice.getObstacle().setDensity(ICE_DENSITY);
        ice.getObstacle().setFriction(ICE_FRICTION);
        ice.getObstacle().setRestitution(ICE_RESTITUTION);
        ice.setTexture(iceGroundTexture);
        addSprite(ice);

        rockLiftCeilingY = bounds.height - rockSize / 2.0f;
        cloudLiftCeilingY = cloud.getObstacle().getY();
        cloudDropActive = false;
        cloudReturnActive = false;
    }

    public boolean preUpdate(float dt) {
        if (!super.preUpdate(dt)) {
            return false;
        }

        if (!isFailure() && avatar.getObstacle().getY() < -1) {
            setFailure(true);
            return false;
        }
        return true;
    }

    public void update(float dt) {
        InputController input = InputController.getInstance();
        findObjectNearZuko();
        if (input.didDropPhoto()) {
            activePicture = null;
            pictures.clear();
        }
        if (input.didToggleRange()) {
            showRange = !showRange;
        }

        if (input.didRegCamera()) {
            avatar.getCamera().setCameraType(CameraType.REGULAR);
        }
        if (input.didTherCamera()) {
            avatar.getCamera().setCameraType(CameraType.THERMAL);
        }
        if (input.didTexCamera()) {
            avatar.getCamera().setCameraType(CameraType.TEXTURE);
        }
        if (input.didCycleCamera()) {
            avatar.getCamera().cycleCameraType();
        }

        avatar.setMovement(input.getHorizontal() * avatar.getForce());
        avatar.setJumping(input.didPrimary());
        avatar.setShooting(input.didSecondary());

        Vector2 mouse = input.getCrossHair();
        GameObject target = findObjectUnderMouse(mouse.x, mouse.y);
        avatar.setCurrentTarget(target);

        if (input.didLeftClick()) {
            SoundEffectManager sounds = SoundEffectManager.getInstance();
            float picVolume = Math.min(1.0f, volume * 1.75f);

            if (target != null) {
                if (activePicture == null) {
                    if (avatar.getCamera().canTakePicture(target.getObstacle().getX(), target.getObstacle().getY(), avatar.getObstacle().getX(), avatar.getObstacle().getY())) {
                        avatar.getCamera().takePicture();
                        Picture picture = new Picture(target, avatar.getCamera().getCameraType());
                        pictures.clear();
                        pictures.add(picture);
                        activePicture = picture;
                        sounds.play("plop", plopSound, picVolume);
                    }
                } else {
                    if (activePicture.getSubject() != null && activePicture.getSubject() != target && avatar.getCamera().hasLineOfSight(target.getObstacle().getX(), target.getObstacle().getY(), avatar.getObstacle().getX(), avatar.getObstacle().getY(),  STICK_PICTURE_DISTANCE)) {
                        Obj src = activePicture.getSubjectType();
                        Obj dst = target.object;

                        if (src == Obj.CLOUD && dst == Obj.ROCK && rock != null && rock.getObstacle() != null && rock.getObstacle().getBody() != null) {
                            rockLiftActive = !rockLiftActive;
                            sounds.play("fire", fireSound, volume);
                            if (rockLiftActive) {
                                rock.putPicture(activePicture.getSubject(),CameraType.REGULAR);
                                float units = height/bounds.height;
                                activePicture.setTarget(rock, units);
                                addSprite(activePicture);
                            } else {
                                sprites.remove(activePicture);
                                activePicture = null;
                                rock.resetAttributes();
                                rock.getObstacle().getBody().setGravityScale(1.0f);
                            }
                        }

                        if (src == Obj.ROCK && dst == Obj.CLOUD && cloud != null && cloud.getObstacle() != null && cloud.getObstacle().getBody() != null) {
                            cloudDropActive = !cloudDropActive;
                            sounds.play("fire", fireSound, volume);
                            if (cloudDropActive) {
                                cloud.putPicture(activePicture.getSubject(),CameraType.REGULAR);
                                cloud.getObstacle().setDensity(ROCK_DENSITY);
                                cloud.getObstacle().setFriction(ROCK_FRICTION);
                                cloud.getObstacle().getBody().resetMassData();
                                float units = height/bounds.height;
                                activePicture.setTarget(cloud, units);
                                addSprite(activePicture);
                                cloudReturnActive = false;
                            } else {

                                sprites.remove(activePicture);
                                activePicture = null;
                                cloud.resetAttributes();
                                cloud.getObstacle().setDensity(CLOUD_BASE_DENSITY);
                                cloud.getObstacle().setFriction(CLOUD_BASE_FRICTION);
                                cloud.getObstacle().getBody().resetMassData();
                                cloudReturnActive = true;
                            }
                        }

                        if (src == Obj.ICE && dst == Obj.CLOUD && cloud != null && cloud.getObstacle() != null && cloud.getObstacle().getBody() != null) {
                            iceOnCloudActive = !iceOnCloudActive;
                            sounds.play("fire", fireSound, volume);
                            if (iceOnCloudActive) {
                                cloud.putPicture(activePicture.getSubject(),CameraType.REGULAR);
                                Body cb = cloud.getObstacle().getBody();
                                cb.setLinearVelocity(0, 0);
                                cb.setAngularVelocity(0);
                                cb.setGravityScale(0.0f);
                                cb.setType(BodyDef.BodyType.StaticBody);
                                float units = height/bounds.height;
                                activePicture.setTarget(cloud, units);
                                addSprite(activePicture);

                            } else {

                                sprites.remove(activePicture);
                                activePicture = null;
                                cloud.resetAttributes();
                                Body cb = cloud.getObstacle().getBody();
                                cb.setType(BodyDef.BodyType.DynamicBody);
                                cb.setGravityScale(0.0f);
                                cb.setLinearVelocity(0, 0);
                                cloud.getObstacle().setDensity(CLOUD_BASE_DENSITY);
                                cloud.getObstacle().setFriction(CLOUD_BASE_FRICTION);
                                cb.resetMassData();
                            }
                        }

                        if (src == Obj.ICE && dst == Obj.ROCK && rock != null && rock.getObstacle() != null && rock.getObstacle().getBody() != null) {
                            iceOnRockActive = !iceOnRockActive;
                            sounds.play("fire", fireSound, volume);
                            if (iceOnRockActive) {
                                rock.putPicture(activePicture.getSubject(),CameraType.REGULAR);
                                rock.getObstacle().setFriction(0.0f);
                                rock.getObstacle().setDensity(ICE_SLIDE_DENSITY);
                                rock.getObstacle().getBody().resetMassData();
                                float units = height/bounds.height;
                                activePicture.setTarget(rock, units);
                                addSprite(activePicture);

                            } else {

                                sprites.remove(activePicture);
                                activePicture = null;
                                rock.resetAttributes();
                                rock.getObstacle().setFriction(ROCK_FRICTION);
                                rock.getObstacle().setDensity(ROCK_DENSITY);
                                rock.getObstacle().getBody().resetMassData();
                            }
                        }

                        if (src == Obj.ROCK && dst == Obj.ICE && ice != null && ice.getObstacle() != null && ice.getObstacle().getBody() != null) {
                            rockOnIceActive = !rockOnIceActive;
                            sounds.play("fire", fireSound, volume);
                            if (rockOnIceActive) {
                                ice.putPicture(activePicture.getSubject(),CameraType.REGULAR);
                                ice.getObstacle().setFriction(ROCK_FRICTION);
                                float units = height/bounds.height;
                                activePicture.setTarget(ice, units);
                                addSprite(activePicture);
                            } else {
                                sprites.remove(activePicture);
                                activePicture = null;
                                ice.resetAttributes();
                                ice.getObstacle().setFriction(ICE_FRICTION);
                            }
                        }

                        if (src == Obj.CLOUD && dst == Obj.ICE && ice != null && ice.getObstacle() != null && ice.getObstacle().getBody() != null) {
                            cloudOnIceActive = !cloudOnIceActive;
                            sounds.play("fire", fireSound, volume);
                            if (cloudOnIceActive) {
                                ice.putPicture(activePicture.getSubject(),CameraType.REGULAR);
                                ice.getObstacle().setRestitution(ICE_BOUNCE_RESTITUTION);
                                float units = height/bounds.height;
                                activePicture.setTarget(ice, units);
                                addSprite(activePicture);
                            } else {
                                sprites.remove(activePicture);
                                activePicture = null;
                                ice.resetAttributes();
                                ice.getObstacle().setRestitution(ICE_RESTITUTION);
                            }
                        }
                    }
                }
            }
        }

        if (avatar.getCamera().isPictureTaken()) {
            avatar.getCamera().clearPictureTaken();
        }

        if (rockLiftActive && rock != null && rock.getObstacle() != null && rock.getObstacle().getBody() != null) {
            Body body = rock.getObstacle().getBody();
            if (body.getPosition().y < rockLiftCeilingY) {
                body.setGravityScale(-0.35f);
            } else {
                body.setGravityScale(0.0f);
                Vector2 v = body.getLinearVelocity();
                body.setLinearVelocity(v.x, 0.0f);
            }
        }

        if (cloud != null && cloud.getObstacle() != null && cloud.getObstacle().getBody() != null) {
            Body body = cloud.getObstacle().getBody();

            if (iceOnCloudActive) {
                // frozen in place -- nothing to do
            } else if (cloudDropActive) {
                body.setGravityScale(1.0f);
            } else if (cloudReturnActive) {
                if (body.getPosition().y < cloudLiftCeilingY) {
                    body.setGravityScale(CLOUD_LIFT_GRAVITY);
                } else {
                    body.setGravityScale(0.0f);
                    Vector2 v = body.getLinearVelocity();
                    body.setLinearVelocity(v.x, 0.0f);
                    cloudReturnActive = false;
                }
            } else {
                body.setGravityScale(0.0f);
            }
        }

        if (avatar.isGrounded() && playerOnIce && !rockOnIceActive) {
            avatar.setDamping(ICE_DAMPING);
        } else {
            avatar.setDamping(NORMAL_DAMPING);
        }

        avatar.applyForce();
        if (avatar.isJumping()) {
            SoundEffectManager sounds = SoundEffectManager.getInstance();
            sounds.play("jump", jumpSound, volume);
        }
    }

    public void beginContact(Contact contact) {
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        Object fd1 = fix1.getUserData();
        Object fd2 = fix2.getUserData();

        try {
            ObstacleSprite bd1 = (ObstacleSprite) body1.getUserData();
            ObstacleSprite bd2 = (ObstacleSprite) body2.getUserData();

            if (bd1.getName().equals("bullet") && bd2 != avatar) {
                removeBullet(bd1);
            }
            if (bd2.getName().equals("bullet") && bd1 != avatar) {
                removeBullet(bd2);
            }

            if ((avatar.getSensorName().equals(fd2) && avatar != bd1) ||
                    (avatar.getSensorName().equals(fd1) && avatar != bd2)) {
                avatar.setGrounded(true);
                sensorFixtures.add(avatar == bd1 ? fix2 : fix1);

                ObstacleSprite other = (ObstacleSprite) (avatar == bd1 ? bd2 : bd1);
                if (other instanceof GameObject && ((GameObject) other).object == Obj.ICE) {
                    playerOnIce = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void endContact(Contact contact) {
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        Object fd1 = fix1.getUserData();
        Object fd2 = fix2.getUserData();

        Object bd1 = body1.getUserData();
        Object bd2 = body2.getUserData();

        if ((avatar.getSensorName().equals(fd2) && avatar != bd1) ||
                (avatar.getSensorName().equals(fd1) && avatar != bd2)) {
            sensorFixtures.remove(avatar == bd1 ? fix2 : fix1);
            if (sensorFixtures.size == 0) {
                avatar.setGrounded(false);
            }

            Object other = (avatar == bd1) ? bd2 : bd1;
            if (other instanceof GameObject && ((GameObject) other).object == Obj.ICE) {
                playerOnIce = false;
            }
        }
    }

    public void postSolve(Contact contact, ContactImpulse impulse) {}
    public void preSolve(Contact contact, Manifold oldManifold) {}

    public void pause() {
        SoundEffectManager sounds = SoundEffectManager.getInstance();
        sounds.stop("plop");
        sounds.stop("fire");
        sounds.stop("jump");
    }

    public void removeBullet(ObstacleSprite bullet) {
        bullet.getObstacle().markRemoved(true);
        SoundEffectManager sounds = SoundEffectManager.getInstance();
        sounds.play("plop", plopSound, volume);
    }

    private GameObject findObjectUnderMouse(float mouseX, float mouseY) {
        for (ObstacleSprite sprite : sprites) {
            if (sprite == avatar) continue;
            if (!(sprite instanceof GameObject)) continue;

            GameObject go = (GameObject) sprite;
            Obstacle obj = go.getObstacle();
            float u = obj.getPhysicsUnits();

            Rectangle bounds = sprite.getMesh().computeBounds();
            float centerX = obj.getX() * u;
            float centerY = obj.getY() * u;

            float minX = centerX + bounds.x;
            float minY = centerY + bounds.y;
            float maxX = minX + bounds.width;
            float maxY = minY + bounds.height;

            float posX = mouseX * u;
            float posY = mouseY * u;

            if (posX >= minX && posX <= maxX && posY >= minY && posY <= maxY) {
                return go;
            }
        }
        return null;
    }

    private void findObjectNearZuko() {
        highlighted.clear();
        float range = (activePicture != null) ? STICK_PICTURE_DISTANCE : TAKE_PICTURE_DISTANCE;
        for (ObstacleSprite sprite : sprites) {
            if (sprite == avatar) continue;
            if (!(sprite instanceof GameObject go)) continue;

            float x = go.getObstacle().getX();
            float y = go.getObstacle().getY();
            if (avatar.getCamera().hasLineOfSight(x, y, avatar.getObstacle().getX(), avatar.getObstacle().getY(), range)) {
                highlighted.add(go);
            }
        }
    }

    @Override
    public void draw(float dt) {
        super.draw(dt);

        canvas.begin(camera);
        Color highlighter = (activePicture != null) ? Color.LIME : Color.CORAL;
        canvas.setColor(highlighter);

        for (GameObject go : highlighted) {
            Obstacle obj = go.getObstacle();
            float u = obj.getPhysicsUnits();
            float a = obj.getAngle();
            Vector2 p = obj.getPosition();

            highlightTransform.idt();
            highlightTransform.preRotate((float)(a * 180.0f/ Math.PI));
            highlightTransform.preTranslate(p.x * u, p.y * u);

            canvas.outline(obj.getOutline(), highlightTransform);
        }
        if (showRange) {
            Obstacle obj = avatar.getObstacle();
            Vector2 p = obj.getPosition();
            float u = obj.getPhysicsUnits();
            float cx = p.x * u;
            float cy = p.y * u;
            float dashSize = 20f;
            float gapSize = 10f;
            float total = dashSize + gapSize;
            PathFactory factory = new PathFactory();

            highlightTransform.idt();
            canvas.setColor(Color.LIME);
            for (float angle = 0; angle < 360; angle += total) {
                Path2 stickArc = factory.makeArc(cx,cy, (STICK_PICTURE_DISTANCE * u * 2) - 1, angle, dashSize, false);
                canvas.outline(stickArc, highlightTransform);
                Path2 stickArc2 = factory.makeArc(cx,cy, (STICK_PICTURE_DISTANCE * u * 2) , angle, dashSize, false);
                canvas.outline(stickArc2, highlightTransform);
                Path2 stickArc3 = factory.makeArc(cx,cy, (STICK_PICTURE_DISTANCE * u * 2) -2, angle, dashSize, false);
                canvas.outline(stickArc3, highlightTransform);
            }
            canvas.setColor(Color.CORAL);
            for (float angle = 0; angle < 360; angle += total) {
                Path2 takeArc = factory.makeArc(cx,cy, (TAKE_PICTURE_DISTANCE * u * 2) - 1, angle, dashSize, false);
                canvas.outline(takeArc, highlightTransform);
                Path2 takeArc2 = factory.makeArc(cx,cy, (TAKE_PICTURE_DISTANCE * u  * 2), angle, dashSize, false);
                canvas.outline(takeArc2, highlightTransform);
                Path2 takeArc3 = factory.makeArc(cx,cy, (TAKE_PICTURE_DISTANCE * u * 2) - 2, angle, dashSize, false);
                canvas.outline(takeArc3, highlightTransform);

            }
        }

        canvas.end();

        String label = avatar.getCamera().getCameraType().getLabel();

        cameraLabel.setText(label);

        canvas.begin(textCamera);
        canvas.setColor(Color.WHITE);
        canvas.drawText(cameraLabel, 50, canvas.getHeight()-20);
        canvas.end();
    }
}
