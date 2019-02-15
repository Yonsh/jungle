package net.natruid.jungle.utils

import com.artemis.ArchetypeBuilder
import com.artemis.ComponentMapper
import com.artemis.World
import com.artemis.utils.IntBag
import com.badlogic.gdx.math.RandomXS128
import net.natruid.jungle.components.ObstacleComponent
import net.natruid.jungle.components.TextureComponent
import net.natruid.jungle.components.TileComponent
import net.natruid.jungle.components.TransformComponent
import net.natruid.jungle.systems.PathfinderSystem
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class MapGenerator(
    private val columns: Int,
    private val rows: Int,
    private val world: World,
    private val seed: Long = Random.nextLong()
) {
    private val tileArchetype = ArchetypeBuilder().add(
        TileComponent::class.java,
        TransformComponent::class.java,
        TextureComponent::class.java
    ).build(world)
    private lateinit var mTile: ComponentMapper<TileComponent>
    private lateinit var mObstacle: ComponentMapper<ObstacleComponent>
    private lateinit var sPathfinder: PathfinderSystem
    private lateinit var map: Array<IntArray>
    val random = RandomXS128(seed)
    private val emptyTiles = IntBag(columns * rows)
    private var initialized = false

    fun init(): Array<IntArray> {
        Logger.debug { "Map seed: $seed" }
        map = Array(columns) { x ->
            IntArray(rows) { y ->
                val entityId = world.create(tileArchetype)
                mTile[entityId].coord.set(x, y)
                emptyTiles.add(entityId)
                entityId
            }
        }
        initialized = true
        return map
    }

    private fun getTile(x: Int, y: Int, reversed: Boolean = false): Int {
        return if (!reversed) {
            map[x][y]
        } else {
            map[y][x]
        }
    }

    private fun createLine(
        terrainType: TerrainType = TerrainType.ROAD,
        minWidth: Int = 1,
        maxWidth: Int = 3,
        vertical: Boolean = random.nextBoolean(),
        fork: Boolean = false,
        mutationFactor: Long = 30L
    ) {
        val ref = if (vertical) columns else rows
        val length = if (vertical) rows else columns
        val startRange = ref / 2
        var wMid = random.nextInt(startRange) + (ref - startRange) / 2
        var width = random.nextInt(maxWidth - minWidth) + minWidth
        var mutateChance = 0L
        var lRange: IntProgression = 0 until length
        if (random.nextBoolean()) lRange = lRange.reversed()
        for (l in lRange) {
            if (fork && mTile[getTile(l, wMid, vertical)].terrainType == terrainType) {
                break
            }
            var noMutation = false
            for (w in 0 until width) {
                val cTile: TileComponent = mTile[getTile(l, wMid + w - width / 2, vertical)]
                if (cTile.terrainType == TerrainType.WATER && terrainType == TerrainType.ROAD) {
                    noMutation = true
                }
                replaceTile(cTile, terrainType)
            }
            if (noMutation) continue
            if (mutateChance >= 100L || random.nextLong(100) >= 100L - mutateChance) {
                mutateChance = 0L
                if (random.nextBoolean()) {     // direction mutation
                    if (random.nextBoolean()) {
                        if (wMid < ref - 1) {
                            wMid += 1
                            replaceTile(
                                mTile[getTile(l, min(ref - 1, wMid + width / 2 - 1 + width.rem(2)), vertical)],
                                terrainType
                            )
                        }
                    } else {
                        if (wMid > 0) {
                            wMid -= 1
                            replaceTile(
                                mTile[getTile(l, max(0, wMid - width / 2), vertical)],
                                terrainType
                            )
                        }
                    }
                } else {                        // width mutation
                    width += when {
                        width == maxWidth -> -1
                        width == minWidth || random.nextBoolean() -> 1
                        else -> -1
                    }
                }
            } else mutateChance += mutationFactor
        }
    }

    private val creationQueue = LinkedList<Int>()
    private val distanceMap = HashMap<Int, Int>()
    private fun createArea(
        terrainType: TerrainType,
        minRadius: Int = 1,
        maxRadius: Int = min(columns, rows) / 2
    ) {
        val start = map[random.nextInt(columns)][random.nextInt(rows)]
        creationQueue.addLast(start)
        distanceMap[start] = 1
        while (creationQueue.isNotEmpty()) {
            val tile = creationQueue.removeFirst()
            val distance = distanceMap[tile]!!
            mTile[tile].terrainType = terrainType
            if (distance >= maxRadius) continue
            val coord = mTile[tile].coord
            val chance = when {
                distance < minRadius -> 1f
                minRadius == maxRadius -> 0f
                else -> 1f - (distance + 1 - minRadius) / (maxRadius - minRadius).toFloat()
            }
            if (chance <= 0f) continue
            for (diff in -1..1 step 2) {
                for (i in 0..1) {
                    var x = coord.x
                    var y = coord.y
                    if (i == 0) x += diff else y += diff
                    if (x < 0 || y < 0 || x >= columns || y >= rows) continue
                    val next = map[x][y]
                    if (distanceMap.containsKey(next)) continue
                    if (chance >= 1f || random.nextFloat() < chance) {
                        creationQueue.add(next)
                        distanceMap[next] = distance + 1
                    }
                }
            }
        }
        distanceMap.clear()
    }

    private fun createPath(vertical: Boolean = false) {
        val ref = if (vertical) columns else rows
        val startRange = ref / 2
        var start = -1
        for (i in 1..10) {
            start = getTile(0, random.nextInt(startRange) + (ref - startRange) / 2, vertical)
            val cTile = mTile[start]
            if (cTile.terrainType != TerrainType.WATER && cTile.obstacle < 0) break
        }

        var minCost = (columns * rows).toFloat()
        var end: PathNode? = null
        val area = sPathfinder.area(start, minCost, false)
        for (node in area) {
            val coord = mTile[node.tile].coord
            if (node.cost < minCost && (vertical && coord.y == rows - 1 || !vertical && coord.x == columns - 1)) {
                minCost = node.cost
                end = node
            }
        }
        replaceTile(mTile[end?.tile ?: return], TerrainType.ROAD)
        while (end?.prev != null) {
            val coord = mTile[end.tile].coord
            end = end.prev!!
            val cTile = mTile[end.tile]
            if (vertical && coord.y == 0 && cTile.coord.y == coord.y) break
            if (!vertical && coord.x == 0 && cTile.coord.x == coord.x) break
            replaceTile(cTile, TerrainType.ROAD)
        }
    }

    private fun replaceTile(tileComponent: TileComponent, terrainType: TerrainType) {
        when (terrainType) {
            TerrainType.ROAD -> {
                if (tileComponent.terrainType == TerrainType.WATER) {
                    tileComponent.terrainType = TerrainType.BRIDGE
                } else if (tileComponent.terrainType != TerrainType.BRIDGE) {
                    tileComponent.terrainType = TerrainType.ROAD
                }
            }
            else -> {
                tileComponent.terrainType = terrainType
            }
        }
    }

    private fun createObstacles(amount: Int) {
        var count = 0
        while (emptyTiles.size() > 0 && count < amount) {
            var canCreate = true
            val tile = emptyTiles.remove(random.nextInt(emptyTiles.size()))
            val cTile = mTile[tile]
            var obstacleType = ObstacleType.ROCK
            var destroyable = false
            when (cTile.terrainType) {
                TerrainType.DIRT -> {
                    obstacleType = ObstacleType.ROCK
                }
                TerrainType.GRASS -> {
                    obstacleType = ObstacleType.TREE
                    destroyable = true
                }
                else -> {
                    canCreate = false
                }
            }
            if (canCreate) {
                val obstacle = world.create()
                mObstacle.create(obstacle).apply {
                    type = obstacleType
                    this.destroyable = destroyable
                    maxHp = 100f
                    hp = maxHp
                }
                count += 1
                cTile.obstacle = obstacle
            }
        }
    }

    private fun clean() {
        emptyTiles.clear()
    }

    fun generate(): Array<IntArray> {
        Logger.stopwatch("Map generation") {
            if (!initialized) init()
            repeat(random.nextInt(5) + 5) {
                createArea(TerrainType.fromByte((random.nextLong(2) + 1).toByte())!!, min(columns, rows) / 3)
            }
            repeat(random.nextInt(3)) {
                createArea(TerrainType.WATER, 2, 5)
            }
            var vertical = random.nextBoolean()
            repeat(random.nextInt(4)) {
                createLine(TerrainType.WATER, vertical = vertical, fork = true)
                vertical = !vertical
            }
            repeat(random.nextInt(3)) {
                createArea(TerrainType.WATER, 2, 5)
            }
            createPath(vertical)
            repeat(random.nextInt(2)) {
                vertical = !vertical
                createPath(vertical)
            }
            createObstacles(random.nextInt(columns * rows / 20) + columns * rows / 40)
            clean()
        }
        return map
    }
}
