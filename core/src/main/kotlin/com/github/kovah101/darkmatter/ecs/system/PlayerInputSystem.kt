package com.github.kovah101.darkmatter.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.Viewport
import com.github.kovah101.darkmatter.ecs.components.*
import ktx.ashley.addComponent
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.ashley.get
import ktx.log.debug
import ktx.log.logger

private val LOG = logger<PlayerInputSystem>()
private const val TOUCH_TOLERANCE_DISTANCE = 0.2f
private const val TILT_TOLERANCE = 0.35f
private const val ENEMY_SPAWN_DELAY = 3f
private const val LASER_FIRE_SPEED = 2f

class PlayerInputSystem(
    private val gameViewport: Viewport,
) : IteratingSystem(allOf(PlayerComponent::class, TransformComponent::class, FacingComponent::class).get()) {
    private val tmpVec = Vector2()
    private var enemySpawnTimer = 0f
    private var laserReloadTimer = 0f

    // by lazy to initialise later
    private val projectileEntities by lazy {
        engine.getEntitiesFor(
            allOf(ProjectileComponent::class).exclude(RemoveComponent::class).get()
        )
    }

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val facing = entity[FacingComponent.mapper]
        require(facing != null) { "Entity |entity| must have FacingComponent. entity=$entity" }
        val transform = entity[TransformComponent.mapper]
        require(transform != null) { "Entity |entity| must have TransformComponent. entity=$entity" }


        projectileEntities.forEach { projectile ->
            //LOG.debug { "There are ${enemyEntities.size()} enemies" }
            projectile[TransformComponent.mapper]?.let { enemyTrans ->
                if (enemyTrans.position.y >= 16f) {
                    projectile.addComponent<RemoveComponent>(engine)
                }
            }
        }

        //Takes mouse/touch input x coordinate and converts to world coordinate
        tmpVec.x = Gdx.input.x.toFloat()
        gameViewport.unproject(tmpVec)
        // finds difference in user input and player location
        val diffX = tmpVec.x - transform.position.x - transform.size.x * 0.5f
        // change facing depending on difference between position and input
        facing.direction = when {
            diffX < -TOUCH_TOLERANCE_DISTANCE -> FacingDirection.LEFT
            diffX > TOUCH_TOLERANCE_DISTANCE -> FacingDirection.RIGHT
            else -> FacingDirection.DEFAULT
        }
        // Tilt controls
        // Take accelerometer reading
        val tiltAvailable = Gdx.input.isPeripheralAvailable(Input.Peripheral.Accelerometer)
        if (tiltAvailable) {
            val tiltX = Gdx.input.accelerometerX
            facing.direction = when {
                tiltX > TILT_TOLERANCE -> FacingDirection.LEFT
                tiltX < -TILT_TOLERANCE -> FacingDirection.RIGHT
                else -> FacingDirection.DEFAULT
            }
        }
        // Laser on tap or button press
        // add fire delays
        // temp spawns asteriod
        enemySpawnTimer -= deltaTime
        laserReloadTimer -= deltaTime
        if (Gdx.input.isTouched && laserReloadTimer <= 0f) {
            laserReloadTimer = 1 / LASER_FIRE_SPEED
            engine.spawnLaser(transform)
            if (enemySpawnTimer <= 0f) {
                enemySpawnTimer = ENEMY_SPAWN_DELAY
                engine.spawnAsteroid(transform)
                LOG.debug { "asteroid spawned" }
            }
        }

    }

}