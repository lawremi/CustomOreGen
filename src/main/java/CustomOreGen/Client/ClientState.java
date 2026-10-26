package CustomOreGen.Client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

import CustomOreGen.CustomOreGenBase;
import CustomOreGen.CustomPacketPayload;
import CustomOreGen.CustomPacketPayload.PayloadType;
import CustomOreGen.CustomPacketPayloadHandler;
import CustomOreGen.GeometryData;
import CustomOreGen.GeometryRequestData;
import CustomOreGen.Util.GeometryStream;
import CustomOreGen.Util.GeometryStream.GeometryStreamException;
import CustomOreGen.Util.IGeometryBuilder.PrimitiveType;
import CustomOreGen.Util.Transform;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public class ClientState
{
    public static WireframeRenderMode dgRenderingMode = WireframeRenderMode.WIREFRAMEOVERLAY;
    public static boolean dgEnabled = true;
    private static Level _world = null;
    private static int _dgScanCounter = 0;
    private static int _dgBatchID = 0;
    // Uploaded geometry per chunk; replaces the GL display lists used before 1.17 (core profile has none).
    @OnlyIn(Dist.CLIENT)
    private static Map<Long,ChunkGeometry> _dgChunkGeometry = new HashMap<Long, ChunkGeometry>();
    @OnlyIn(Dist.CLIENT)
    private static Set<Long> _chunkDGRequests = new HashSet<Long>();
    // Filled from the network thread's enqueued work, drained on the render thread.
    private static Queue<GeometryData> geometryDataQueue = new ConcurrentLinkedQueue<GeometryData>();
	private static boolean clearDisplayLists;

    public enum WireframeRenderMode
    {
        NONE,
        WIREFRAME,
        POLYGON,
        WIREFRAMEOVERLAY;
    }

    @OnlyIn(Dist.CLIENT)
    private static class ChunkGeometry
    {
    	final VertexBuffer lines;
    	final VertexBuffer polygons;

    	ChunkGeometry(VertexBuffer lines, VertexBuffer polygons)
    	{
    		this.lines = lines;
    		this.polygons = polygons;
    	}

    	void close()
    	{
    		if (this.lines != null) this.lines.close();
    		if (this.polygons != null) this.polygons.close();
    	}
    }

    @OnlyIn(Dist.CLIENT)
    public static void onRenderLevel(RenderLevelStageEvent event)
    {
    	if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
    	{
    		return;
    	}

    	if (clearDisplayLists) {
    		clearDebuggingDisplayLists();
    		clearDisplayLists = false;
    	}

        if (_world != null && dgEnabled && dgRenderingMode != null && dgRenderingMode != WireframeRenderMode.NONE)
        {
        	buildGeometryDisplayLists();
        	Vec3 camera = event.getCamera().getPosition();

            if (_dgScanCounter == 0)
            {
                _dgScanCounter = 60;
                requestNearbyGeometry(camera);
            }
            else
            {
                --_dgScanCounter;
            }

            if (!_dgChunkGeometry.isEmpty())
            {
            	// At this stage the model-view matrix is the camera rotation.
            	drawGeometry(event.getModelViewMatrix(), event.getProjectionMatrix(), camera);
            }
        }
    }

    private static void requestNearbyGeometry(Vec3 camera)
    {
    	byte scanRange = 16;
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
    				CustomPacketPayloadHandler.sendToServer(new CustomPacketPayload(PayloadType.DebuggingGeometryRequest, request));
    				--maxRequest;
    			}
    		}
    	}
    }

    private static void drawGeometry(Matrix4f cameraRotation, Matrix4f projection, Vec3 camera)
    {
    	Matrix4f modelView = new Matrix4f(cameraRotation).translate((float)-camera.x, (float)-camera.y, (float)-camera.z);
    	ShaderInstance shader = GameRenderer.getPositionColorShader();

    	RenderSystem.enableBlend();
    	RenderSystem.defaultBlendFunc();
    	RenderSystem.disableCull();
    	RenderSystem.depthMask(false);

    	if (dgRenderingMode == WireframeRenderMode.WIREFRAMEOVERLAY)
    	{
    		RenderSystem.disableDepthTest();
    	}

    	for (ChunkGeometry geometry : _dgChunkGeometry.values())
    	{
    		if (geometry.lines != null)
    		{
    			geometry.lines.bind();
    			geometry.lines.drawWithShader(modelView, projection, shader);
    		}
    	}

    	if (dgRenderingMode != WireframeRenderMode.POLYGON)
    	{
    		RenderSystem.polygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
    	}

    	for (ChunkGeometry geometry : _dgChunkGeometry.values())
    	{
    		if (geometry.polygons != null)
    		{
    			geometry.polygons.bind();
    			geometry.polygons.drawWithShader(modelView, projection, shader);
    		}
    	}

    	if (dgRenderingMode != WireframeRenderMode.POLYGON)
    	{
    		RenderSystem.polygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
    	}

    	VertexBuffer.unbind();
    	RenderSystem.enableDepthTest();
    	RenderSystem.depthMask(true);
    	RenderSystem.enableCull();
    	RenderSystem.disableBlend();
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

        ChunkGeometry geometry;

        try (ByteBufferBuilder lineBytes = new ByteBufferBuilder(256); ByteBufferBuilder polygonBytes = new ByteBufferBuilder(256))
        {
        	BufferBuilder lines = new BufferBuilder(lineBytes, VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        	BufferBuilder polygons = new BufferBuilder(polygonBytes, VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        	renderer.emit(lines, polygons);
        	geometry = new ChunkGeometry(upload(lines), upload(polygons));
        }
        long key = (long)geometryData.chunkX << 32 | (long)geometryData.chunkZ & 4294967295L;
        ChunkGeometry previous = _dgChunkGeometry.put(key, geometry);

        if (previous != null)
        {
        	previous.close();
        }

        _chunkDGRequests.remove(key);
	}

    private static VertexBuffer upload(BufferBuilder builder)
    {
    	MeshData mesh = builder.build();

    	if (mesh == null)
    	{
    		return null;
    	}

    	VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
    	buffer.bind();
    	buffer.upload(mesh);
    	VertexBuffer.unbind();
    	return buffer;
    }

    public static boolean hasLevelChanged(Level currentLevel)
    {
        return _world != currentLevel;
    }

    public static void onLevelChanged(Level currentLevel)
    {
        _world = currentLevel;
        CustomOreGenBase.log.debug("Client world changed to " + (_world == null ? null : _world.dimension().location()));
        clearDebuggingGeometry();
    }

    public static void addDebuggingGeometry(GeometryData geometryData)
    {
        if (_world != null && geometryData.batchID == _dgBatchID && _world.dimension().location().equals(geometryData.dimensionID))
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
    	for (ChunkGeometry geometry : _dgChunkGeometry.values())
    	{
    		geometry.close();
    	}

        _dgChunkGeometry.clear();
        _chunkDGRequests.clear();
        geometryDataQueue.clear();
        ++_dgBatchID;
        dgEnabled = true;
        _dgScanCounter = (RandomSource.create()).nextInt(40);
    }
}
