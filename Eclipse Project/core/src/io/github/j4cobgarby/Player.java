package io.github.j4cobgarby;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback;
import com.badlogic.gdx.physics.bullet.collision.btBroadphasePair;
import com.badlogic.gdx.physics.bullet.collision.btBroadphasePairArray;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btConvexShape;
import com.badlogic.gdx.physics.bullet.collision.btManifoldPoint;
import com.badlogic.gdx.physics.bullet.collision.btPairCachingGhostObject;
import com.badlogic.gdx.physics.bullet.collision.btPersistentManifold;
import com.badlogic.gdx.physics.bullet.collision.btPersistentManifoldArray;
import com.badlogic.gdx.physics.bullet.dynamics.btKinematicCharacterController;

public class Player {
	public BulletEntity character;
	btPairCachingGhostObject ghostObject;
	btConvexShape ghostShape;
	public btKinematicCharacterController characterController;
	Matrix4 characterTransform;
	Vector3 characterDirection = new Vector3();
	Vector3 camDirection = new Vector3();
	private float camYAngle = 0.f;
	Vector3 walkDirection = new Vector3();
	
	private final float xScalar = 0.2f;
	private final float yScalar = 0.1f;
	
	// Raycast stuff
	ClosestRayResultCallback callback;
	Vector3 rayFrom = new Vector3();
	Vector3 rayTo = new Vector3();
	
	public Player() {
		final float playerRad = 0.7f;
		final float playerHeight = 5;
		
		final Material material = new Material(ColorAttribute.createDiffuse(Color.BLUE));
		final long attributes = Usage.Position | Usage.Normal;
		final Model capsule = Main.modelBuilder.createCapsule(playerRad, playerHeight, 16, material, attributes);
		Main.disposables.add(capsule);
		Main.world.addConstructor("capsule", new BulletConstructor(capsule, null));
		character = Main.world.add("capsule", 2.5f, 10f, 5f);
		character.invisible = true;
		characterTransform = character.transform;
		
		ghostObject = new btPairCachingGhostObject();
		ghostObject.setWorldTransform(characterTransform);
		ghostShape = new btCapsuleShape(playerRad, playerHeight - (2 * playerRad));
		ghostObject.setCollisionShape(ghostShape);
		ghostObject.setCollisionFlags(btCollisionObject.CollisionFlags.CF_CHARACTER_OBJECT);
		(characterController = new btKinematicCharacterController(ghostObject, ghostShape, .35f, 1)).setJumpSpeed(12);
	}
	
	public void update() {
		
		if (Gdx.input.isKeyJustPressed(Keys.SPACE)) {
			characterController.jump();
		}
				
		characterTransform.rotate(0, 1, 0, -Gdx.input.getDeltaX() * xScalar);
		ghostObject.setWorldTransform(characterTransform);
		
		characterDirection.set(-1, 0, 0).rot(characterTransform).nor();
		camDirection.set(characterDirection).rotate(camDirection.cpy().scl(1, 0, 1).rotate(Vector3.Y.cpy(), 90), camYAngle);

		camYAngle = MathUtils.clamp(camYAngle + Gdx.input.getDeltaY() * yScalar, -44.5f, 44.5f);
		
		walkDirection.set(0, 0, 0);
		if (Gdx.input.isKeyPressed(Keys.W))
			walk(FORWARDS);

		if (Gdx.input.isKeyPressed(Keys.S))
			walk(BACKWARDS);

		if (Gdx.input.isKeyPressed(Keys.A))
			walk(LEFT);

		if (Gdx.input.isKeyPressed(Keys.D))
			walk(RIGHT);

		walkDirection.scl((Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) ? 16.7f : 13.2f) * Gdx.graphics.getDeltaTime());

		characterController.setWalkDirection(walkDirection);
		
		character.modelInstance.transform.getTranslation(Main.camera.position);
		camDirection.rotate(camDirection.cpy().scl(1, 0, 1).rotate(Vector3.Y.cpy(), 90), camYAngle);

		Main.camera.direction.set(camDirection);
	}
	
	public void walk(DIRECTION direction) {
		switch (direction) {
		case FORWARDS:
			walkDirection.add(characterDirection);
			break;
		case BACKWARDS:
			walkDirection.add(characterDirection.cpy().scl(-1));
			break;
		case LEFT:
			walkDirection.add(Main.rotVec3By90Deg(characterDirection.cpy()));
			break;
		case RIGHT:
			walkDirection.add(Main.rotVec3By90Deg(characterDirection.cpy()).scl(-1));
			break;
		default: break;
		}
	}
	
	private enum DIRECTION {
		FORWARDS, BACKWARDS, LEFT, RIGHT
	}
	
	DIRECTION FORWARDS  = DIRECTION.FORWARDS;
	DIRECTION BACKWARDS = DIRECTION.BACKWARDS;
	DIRECTION LEFT      = DIRECTION.LEFT;
	DIRECTION RIGHT     = DIRECTION.RIGHT;
}
