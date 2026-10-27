package CustomOreGen.Client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;



import CustomOreGen.CustomOreGenBase;
import CustomOreGen.CustomPacketPayload;
import CustomOreGen.CustomPacketPayload.PayloadType;
import CustomOreGen.GeometryData;
import CustomOreGen.GeometryRequestData;
import CustomOreGen.Util.GeometryStream;
import CustomOreGen.Util.GeometryStream.GeometryStreamException;
import CustomOreGen.Util.IGeometryBuilder.PrimitiveType;
import CustomOreGen.Util.Transform;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ClientState
{
    public static WireframeRenderMode dgRenderingMode = WireframeRenderMode.WIREFRAMEOVERLAY;
    public static boolean dgEnabled = true;
    private static Level _world = null;
    private static int _dgScanCounter = 0;
    private static int _dgBatchID = 0;
    // Uploaded geometry per chunk; replaces the GL display lists used before 1.17 (core profile has none).
    private static Map<Long,ChunkGeometry> _dgChunkGeometry = new HashMap<Long, ChunkGeometry>();
    private static Set<Long> _chunkDGRequests = new HashSet<Long>();
    // Filled from the network thread's enqueued work, drained on the render thread.
    private static Queue<GeometryData> geometryDataQueue = new ConcurrentLinkedQueue<GeometryData>();
	private static boolean clearDisplayLists;
	private static final byte SCAN_RANGE = 16;
	/** Lines and polygons drawn per frame, well under the vertex buffer's limit (and cheap enough to rebuild). */
	private static final int MAX_PRIMITIVES_PER_FRAME = 500000;

    public enum WireframeRenderMode
    {
        NONE,
        WIREFRAME,
        POLYGON,
        WIREFRAMEOVERLAY;
    }

    /** A chunk's geometry: line vertex pairs and triangle vertex triples, drawn as gizmos each tick. */
    private record ChunkGeometry(List<GeometryRenderer.Vertex> lines, List<GeometryRenderer.Vertex> polygons) { }

    private static boolean isDrawing()
    {
        return _world != null && dgEnabled && dgRenderingMode != null && dgRenderingMode != WireframeRenderMode.NONE;
    }

    /** Called each client tick: builds the geometry that has arrived and requests geometry for nearby chunks. */
    public static void onClientTick(Minecraft mc)
    {
    	if (clearDisplayLists) {
    		clearDebuggingDisplayLists();
    		clearDisplayLists = false;
    	}

        if (isDrawing())
        {
        	buildGeometryDisplayLists();
        	Vec3 camera = mc.gameRenderer.mainCamera().position();

            if (_dgScanCounter == 0)
            {
                _dgScanCounter = 20;
                dropDistantGeometry(camera);
                requestNearbyGeometry(camera);
            }
            else
            {
                --_dgScanCounter;
            }
        }
    }

    /**
     * Called each frame, while Minecraft collects the frame's gizmos. (Per-tick gizmos aren't drained while the
     * game is paused or frozen, so drawing every tick piled them up until the vertex buffer overflowed.)
     */
    public static void onRenderFrame(Minecraft mc)
    {
        if (isDrawing() && !_dgChunkGeometry.isEmpty())
        {
        	drawGeometry(mc.gameRenderer.mainCamera().position());
        }
    }

    /** Forgets chunks well outside the scan range, so the drawn geometry stays bounded as the player travels. */
    private static void dropDistantGeometry(Vec3 camera)
    {
    	int chunkX = Mth.floor(camera.x) >> 4;
    	int chunkZ = Mth.floor(camera.z) >> 4;
    	int keepRange = SCAN_RANGE + 2;
    	_dgChunkGeometry.keySet().removeIf(key -> Math.abs((int)(key >> 32) - chunkX) > keepRange || Math.abs((int)(long)key - chunkZ) > keepRange);
    }

    private static void requestNearbyGeometry(Vec3 camera)
    {
    	byte scanRange = SCAN_RANGE;
    	int maxRequest = 50 - _chunkDGRequests.size() * 20;
    	int chunkX = Mth.floor(camera.x) >> 4;
    	int chunkZ = Mth.floor(camera.z) >> 4;

    	for (int r = 0; maxRequest > 0 && r <= scanRange; ++r)
    	{
    		int iX = chunkX - r;
    		int iZ = chunkZ - r;

    		for (int i = 0; maxRequest > 0 && i <= r * 8; ++i)
    		{
    			if (i < r * 2)
    			{
    				++iX;
    			}
    			else if (i < r * 4)
    			{
    				++iZ;
    			}
    			else if (i < r * 6)
    			{
    				--iX;
    			}
    			else if (i < r * 8)
    			{
    				--iZ;
    			}
    			else if (r != 0)
    			{
    				continue;
    			}

    			long key = (long)iX << 32 | (long)iZ & 4294967295L;

    			if (!_dgChunkGeometry.containsKey(key) && _chunkDGRequests.add(key))
    			{
    				GeometryRequestData request = new GeometryRequestData(_world, iX, iZ, _dgBatchID);
    				ClientPacketHandler.sendToServer(new CustomPacketPayload(PayloadType.DebuggingGeometryRequest, request));
    				--maxRequest;
    			}
    		}
    	}
    }

    /**
     * Draws the received chunks nearest first, until the frame's primitive budget is spent. (Gizmos are rebuilt
     * into one vertex buffer each frame, which holds at most 16.7M vertices; a full scan of dense ore is more.)
     */
    private static void drawGeometry(Vec3 camera)
    {
    	boolean onTop = dgRenderingMode == WireframeRenderMode.WIREFRAMEOVERLAY;
    	int chunkX = Mth.floor(camera.x) >> 4;
    	int chunkZ = Mth.floor(camera.z) >> 4;
    	List<Long> keys = new ArrayList<Long>(_dgChunkGeometry.keySet());
    	keys.sort(Comparator.comparingInt(key -> Math.max(Math.abs((int)(key >> 32) - chunkX), Math.abs((int)(long)key - chunkZ))));
    	int budget = MAX_PRIMITIVES_PER_FRAME;

    	for (Long key : keys)
    	{
    		ChunkGeometry geometry = _dgChunkGeometry.get(key);
    		int cost = geometry.lines().size() / 2 + geometry.polygons().size() / 3 * (dgRenderingMode == WireframeRenderMode.POLYGON ? 1 : 3);
    		if (cost > budget)
    		{
    			break;
    		}
    		budget -= cost;

    		List<GeometryRenderer.Vertex> lines = geometry.lines();
    		for (int i = 0; i + 1 < lines.size(); i += 2)
    		{
    			draw(Gizmos.line(vec(lines.get(i)), vec(lines.get(i + 1)), lines.get(i).argb()), onTop);
    		}

    		List<GeometryRenderer.Vertex> polygons = geometry.polygons();
    		for (int i = 0; i + 2 < polygons.size(); i += 3)
    		{
    			GeometryRenderer.Vertex a = polygons.get(i), b = polygons.get(i + 1), c = polygons.get(i + 2);
    			if (dgRenderingMode == WireframeRenderMode.POLYGON)
    			{
    				// A triangle, as a quad with its last corner repeated.
    				draw(Gizmos.rect(vec(a), vec(b), vec(c), vec(c), GizmoStyle.fill(a.argb())), onTop);
    			}
    			else
    			{
    				draw(Gizmos.line(vec(a), vec(b), a.argb()), onTop);
    				draw(Gizmos.line(vec(b), vec(c), a.argb()), onTop);
    				draw(Gizmos.line(vec(c), vec(a), a.argb()), onTop);
    			}
    		}
    	}
    }

    private static void draw(GizmoProperties gizmo, boolean onTop)
    {
    	if (onTop)
    	{
    		gizmo.setAlwaysOnTop();
    	}
    }

    private static Vec3 vec(GeometryRenderer.Vertex v)
    {
    	return new Vec3(v.x(), v.y(), v.z());
    }

    private static void buildGeometryDisplayLists() {
    	while(!geometryDataQueue.isEmpty()) {
    		buildGeometryDisplayList(geometryDataQueue.remove());
    	}
    }

    private static void buildGeometryDisplayList(GeometryData geometryData) {
        GeometryRenderer renderer = new GeometryRenderer();

        try
        {
            for (GeometryStream stream : geometryData.geometry)
            {
                stream.executeStream(renderer);
            }
        }
        catch (GeometryStreamException var8)
        {
            throw new RuntimeException(var8);
        }

        // Faint outline of the chunk itself, so it's visible which chunks have been received.
        renderer.setPositionTransform((new Transform()).translate((float)(geometryData.chunkX * 16 + 8), -1.0F, (float)(geometryData.chunkZ * 16 + 8)).scale(7.5F, 1.0F, 7.5F));
        renderer.setColor(new float[] {0.0F, 1.0F, 0.0F, 0.15F});
        renderer.setVertexMode(PrimitiveType.LINE, new int[] {1});
        renderer.addVertex(new float[] { -1.0F, 0.0F, -1.0F});
        renderer.addVertex(new float[] { -1.0F, 0.0F, 1.0F});
        renderer.addVertex(new float[] {1.0F, 0.0F, 1.0F});
        renderer.addVertex(new float[] {1.0F, 0.0F, -1.0F});
        renderer.addVertexRef(4);

        List<GeometryRenderer.Vertex> lines = new ArrayList<GeometryRenderer.Vertex>();
        List<GeometryRenderer.Vertex> polygons = new ArrayList<GeometryRenderer.Vertex>();
        renderer.emit(lines, polygons);
        ChunkGeometry geometry = new ChunkGeometry(lines, polygons);
        long key = (long)geometryData.chunkX << 32 | (long)geometryData.chunkZ & 4294967295L;
        _dgChunkGeometry.put(key, geometry);

        _chunkDGRequests.remove(key);
	}

    public static boolean hasLevelChanged(Level currentLevel)
    {
        return _world != currentLevel;
    }

    public static void onLevelChanged(Level currentLevel)
    {
        _world = currentLevel;
        CustomOreGenBase.log.debug("Client world changed to " + (_world == null ? null : _world.dimension().identifier()));
        clearDebuggingGeometry();
    }

    public static void addDebuggingGeometry(GeometryData geometryData)
    {
        if (_world != null && geometryData.batchID == _dgBatchID && _world.dimension().identifier().equals(geometryData.dimensionID))
        {
        	geometryDataQueue.add(geometryData);
        }
    }

    public static void clearDebuggingGeometry()
    {
    	clearDisplayLists = true;
    }

    // Runs on the render thread (from onRenderLevel), since it frees GPU buffers.
    private static void clearDebuggingDisplayLists()
    {

        _dgChunkGeometry.clear();
        _chunkDGRequests.clear();
        geometryDataQueue.clear();
        ++_dgBatchID;
        dgEnabled = true;
        _dgScanCounter = (RandomSource.create()).nextInt(40);
    }
}
