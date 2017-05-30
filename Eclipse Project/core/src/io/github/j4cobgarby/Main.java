package io.github.j4cobgarby;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.FloatCounter;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.btAxisSweep3;
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseProxy;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btConvexShape;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btGhostPairCallback;
import com.badlogic.gdx.physics.bullet.collision.btPairCachingGhostObject;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btKinematicCharacterController;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw.DebugDrawModes;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.PerformanceCounter;

public class Main implements ApplicationListener {
	public StringBuilder performance = new StringBuilder();
	public PerformanceCounter performanceCounter = new PerformanceCounter(this.getClass().getSimpleName());
	public FloatCounter fpsCounter = new FloatCounter(5);
	static PerspectiveCamera camera;
	
	BulletEntity ground;
	Player player;
	btGhostPairCallback ghostPairCallback;
	
	public static boolean shadows = true;

	public Environment environment;
	public DirectionalLight light;
	public ModelBatch shadowBatch;
	
	private BitmapFont JackInput;
	
	static BlenderPhysicsModel ico;

	static BulletWorld world;
	public ObjLoader objLoader = new ObjLoader();
	static ModelBuilder modelBuilder = new ModelBuilder(); 
	public ModelBatch modelBatch;
	static Array<Disposable> disposables = new Array<Disposable>();
	private int debugMode = DebugDrawModes.DBG_NoDebug;
	private SpriteBatch debugBatch;
	
	private final int shadowMapSize = (int) Math.pow(2, 10);
	private final float shadowViewportWidth = 60f;
	private final float shadowViewportHeight = 60f;
	private final float shadowNear = 1f;
	private final float shadowFar = 1000f;

	protected final static Vector3 tmpV1 = new Vector3(), tmpV2 = new Vector3();

	public BulletWorld createWorld() {
		btDefaultCollisionConfiguration collisionConfiguration = new btDefaultCollisionConfiguration();
		btCollisionDispatcher dispatcher = new btCollisionDispatcher(collisionConfiguration);
		btAxisSweep3 sweep = new btAxisSweep3(new Vector3(-1000, -1000, -1000), new Vector3(1000, 1000, 1000));
		btSequentialImpulseConstraintSolver solver = new btSequentialImpulseConstraintSolver();
		btDiscreteDynamicsWorld collisionWorld = new btDiscreteDynamicsWorld(dispatcher, sweep, solver,
				collisionConfiguration);
		ghostPairCallback = new btGhostPairCallback();
		sweep.getOverlappingPairCache().setInternalGhostPairCallback(ghostPairCallback);
		return new BulletWorld(collisionConfiguration, dispatcher, sweep, solver, collisionWorld);
	}

	@Override
	public void create() {
		Gdx.input.setCursorCatched(true);
		
		Bullet.init();
		Resources.Assets.init();
		
		FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("jifont.TTF"));
		FreeTypeFontParameter par = new FreeTypeFontParameter();
		par.size = 20;
		par.color = Color.WHITE;
		par.borderColor = Color.BLACK;
		par.borderWidth = 1;
		JackInput = gen.generateFont(par);
		gen.dispose();

		// ENVIRONMENT AND LIGHTING
		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.3f, 0.3f, 0.3f, 1.f));
		//light = shadows ? new DirectionalShadowLight(1024, 1024, 20f, 20f, 1f, 300f) : new DirectionalLight();
		light = shadows ? new DirectionalShadowLight(shadowMapSize, shadowMapSize, shadowViewportWidth, shadowViewportHeight, shadowNear, shadowFar) : new DirectionalLight();
		light.set(1f, 1f, 1f, -0.5f, -1f, 0.7f);
		environment.add(light);
		if (shadows)
			environment.shadowMap = (DirectionalShadowLight) light;
		shadowBatch = new ModelBatch(new DepthShaderProvider());

		// MODEL BATCH INITIALISATION
		modelBatch = new ModelBatch();
		debugBatch = new SpriteBatch();

		// PHYSICS WORLD SETUP
		world = createWorld();
		world.collisionWorld.getPairCache().setInternalGhostPairCallback(new btGhostPairCallback());
		world.performanceCounter = performanceCounter;

		// CAMERA SETUP
		final float width = Gdx.graphics.getWidth();
		final float height = Gdx.graphics.getHeight();
		if (width > height)
			camera = new PerspectiveCamera(90f, 3f * width / height, 3f);
		else
			camera = new PerspectiveCamera(90f, 3f, 3f * height / width);
		camera.near = 0.01f;
		camera.update();
		
		// MAKE SOME PRIMITIVES
		final Model groundModel = modelBuilder.createRect(20f, 0f, -20f, -20f, 0f, -20f, -20f, 0f, 20f, 20f, 0f, 20f, 0,
				1, 0, new Material(ColorAttribute.createDiffuse(Color.BLUE)),
				Usage.Position | Usage.Normal);
		disposables.add(groundModel);
		final Model boxModel = modelBuilder.createBox(1f,
				1f, 1f, new Material(ColorAttribute.createDiffuse(Color.BLUE)),
				Usage.Position | Usage.Normal);
		disposables.add(boxModel);

		// CONSTRUCTORS FOR THE PRIMITIVES
		world.addConstructor("ground", new BulletConstructor(groundModel, 0f));
		world.addConstructor("staticbox", new BulletConstructor(boxModel, 0f)); // Static
		
		// SETTING UP THE PLAYER
		player = new Player();

		// ADDING PLAYER TO THE COLLISION WORLD
		world.collisionWorld.addCollisionObject(player.ghostObject,
				(short) btBroadphaseProxy.CollisionFilterGroups.CharacterFilter,
				(short) (btBroadphaseProxy.CollisionFilterGroups.StaticFilter
						| btBroadphaseProxy.CollisionFilterGroups.DefaultFilter));
		((btDiscreteDynamicsWorld) (world.collisionWorld)).addAction(player.characterController);

		// THE GROUND
		//ground = world.add("ground", 0f, 0f, 0f);
		//world.add("staticbox", 2, 0.5f, 2).setColor(Color.RED);;
		
		ico = new BlenderPhysicsModel("test.g3dj");
	}

	public void update() {
		player.update();

		// Update the world normally
		world.update();
		// This makes sure the model's in the right place
		player.ghostObject.getWorldTransform(player.characterTransform);
	}

	@Override
	public void render() {
		render(true);
		if (Gdx.input.isKeyPressed(Keys.ESCAPE)) Gdx.app.exit();
	}

	/* This is the actual render method. The other one just calls this. */
	public void render(boolean update) {
		fpsCounter.put(Gdx.graphics.getFramesPerSecond());

		if (update)
			update();

		Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		camera.update();

		renderWorld();

		Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
		if (debugMode != DebugDrawModes.DBG_NoDebug)
			world.setDebugMode(debugMode);
		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

		performance.setLength(0);
		performance.append("FPS: ").append(fpsCounter.value).append(", Bullet: ")
				.append((int) (performanceCounter.load.value * 100f)).append("%, ")
				.append(player.character.transform.getTranslation(tmpV1).toString());
		
		debugBatch.begin();
		JackInput.draw(debugBatch, performance, 10, Gdx.graphics.getHeight() - 10);
		debugBatch.end();
	}

	protected void renderWorld() {
		if (shadows) {
			((DirectionalShadowLight) light).begin(Vector3.Zero, camera.direction);
			shadowBatch.begin(((DirectionalShadowLight) light).getCamera());
			world.render(shadowBatch, null);
			shadowBatch.end();
			((DirectionalShadowLight) light).end();
		}

		modelBatch.begin(camera);
		world.render(modelBatch, environment);
		modelBatch.end();
	}

	@Override
	public void dispose() {
		world.collisionWorld.removeCollisionObject(player.ghostObject);
		world.dispose();
		world = null;

		for (Disposable disposable : disposables)
			disposable.dispose();
		disposables.clear();

		modelBatch.dispose();
		modelBatch = null;

		shadowBatch.dispose();
		shadowBatch = null;

		if (shadows)
			((DirectionalShadowLight) light).dispose();
		light = null;

		player.characterController.dispose();
		player.ghostObject.dispose();
		player.ghostShape.dispose();
		ghostPairCallback.dispose();
		ground = null;
	}

	static Vector3 rotVec3By90Deg(Vector3 in) {
		return new Vector3(in.z, in.y, -in.x);
	}

	@Override
	public void resize(int width, int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
		
	}
}
