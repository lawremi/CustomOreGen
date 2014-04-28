package CustomOreGen.Client;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import CustomOreGen.CustomOreGenBase;
import CustomOreGen.CustomPacketPayload;
import CustomOreGen.CustomPacketPayload.PayloadType;
import CustomOreGen.GeometryData;
import CustomOreGen.GeometryRequestData;
import CustomOreGen.MystcraftInterface;
import CustomOreGen.MystcraftSymbolData;
import CustomOreGen.Util.GeometryStream;
import CustomOreGen.Util.GeometryStream.GeometryStreamException;
import CustomOreGen.Util.IGeometryBuilder.PrimitiveType;
import CustomOreGen.Util.Transform;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ClientState
{
    public static WireframeRenderMode dgRenderingMode = WireframeRenderMode.WIREFRAMEOVERLAY;
    public static boolean dgEnabled = true;
    private static World _world = null;
    private static int _dgScanCounter = 0;
    private static int _dgBatchID = 0;
    @SideOnly(Side.CLIENT)
    private static Map<Long,Integer> _dgListMap = new HashMap();
    @SideOnly(Side.CLIENT)
    private static Set _chunkDGRequests = new HashSet();
    @SideOnly(Side.CLIENT)
    private static IntBuffer _chunkDGListBuffer = null;

    public enum WireframeRenderMode
    {
        NONE,
        WIREFRAME,
        POLYGON,
        WIREFRAMEOVERLAY;
    }

    @SideOnly(Side.CLIENT)
    public static void onRenderWorld(Entity cameraPOV, double partialTicks)
    {
        if (_world != null && dgEnabled && dgRenderingMode != null && dgRenderingMode != WireframeRenderMode.NONE)
        {
            double posX = cameraPOV.lastTickPosX + (cameraPOV.posX - cameraPOV.lastTickPosX) * partialTicks;
            double posY = cameraPOV.lastTickPosY + (cameraPOV.posY - cameraPOV.lastTickPosY) * partialTicks;
            double posZ = cameraPOV.lastTickPosZ + (cameraPOV.posZ - cameraPOV.lastTickPosZ) * partialTicks;

            if (_dgScanCounter == 0)
            {
                _dgScanCounter = 60;
                byte scanRange = 16;
                int maxRequest = 50 - _chunkDGRequests.size() * 20;
                int chunkX = (int)posX / 16;
                int chunkZ = (int)posZ / 16;

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

                        if (!_dgListMap.containsKey(Long.valueOf(key)) && _chunkDGRequests.add(Long.valueOf(key)))
                        {
                            GeometryRequestData request = new GeometryRequestData(_world, iX, iZ, _dgBatchID);
                            (new CustomPacketPayload(PayloadType.DebuggingGeometryRequest, request)).sendToServer();
                            --maxRequest;
                        }
                    }
                }
            }
            else
            {
                --_dgScanCounter;
            }

            if (_chunkDGListBuffer != null)
            {
                GL11.glPushMatrix();
                GL11.glTranslated(-posX, -posY, -posZ);
                GL11.glDisable(2884);
                GL11.glDisable(3553);
                GL11.glEnable(3042);
                GL11.glBlendFunc(770, 771);

                if (dgRenderingMode == WireframeRenderMode.WIREFRAMEOVERLAY)
                {
                    GL11.glDisable(2929);
                }

                if (dgRenderingMode != WireframeRenderMode.POLYGON)
                {
                    GL11.glPolygonMode(1032, 6913);
                }

                _chunkDGListBuffer.rewind();
                GL11.glCallLists(_chunkDGListBuffer);

                if (dgRenderingMode != WireframeRenderMode.POLYGON)
                {
                    GL11.glPolygonMode(1032, 6914);
                }

                if (dgRenderingMode == WireframeRenderMode.WIREFRAMEOVERLAY)
                {
                    GL11.glEnable(2929);
                }

                GL11.glDisable(3042);
                GL11.glEnable(3553);
                GL11.glEnable(2884);
                GL11.glPopMatrix();
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public static boolean hasWorldChanged(World currentWorld)
    {
        return _world != currentWorld;
    }

    @SideOnly(Side.CLIENT)
    public static void onWorldChanged(World currentWorld)
    {
        _world = currentWorld;
        CustomOreGenBase.log.finer("Client world changed to " + (_world == null ? null : _world.getWorldInfo().getWorldName()));
        clearDebuggingGeometry();
    }

    @SideOnly(Side.CLIENT)
    public static void addDebuggingGeometry(GeometryData geometryData)
    {
        if (_world != null)
        {
            if (geometryData.batchID == _dgBatchID)
            {
                if (geometryData.dimensionID == _world.provider.dimensionId)
                {
                    GeometryRenderer renderer = new GeometryRenderer();

                    try
                    {
                        Iterator displayList = geometryData.geometry.iterator();

                        while (displayList.hasNext())
                        {
                            GeometryStream key = (GeometryStream)displayList.next();
                            key.executeStream(renderer);
                        }
                    }
                    catch (GeometryStreamException var8)
                    {
                        throw new RuntimeException(var8);
                    }

                    int var9 = GL11.glGenLists(1);

                    if (var9 != 0)
                    {
                        GL11.glNewList(var9, 4864);
                        renderer.setPositionTransform((new Transform()).translate((float)(geometryData.chunkX * 16 + 8), -1.0F, (float)(geometryData.chunkZ * 16 + 8)).scale(7.5F, 1.0F, 7.5F));
                        renderer.setColor(new float[] {0.0F, 1.0F, 0.0F, 0.15F});
                        renderer.setVertexMode(PrimitiveType.LINE, new int[] {1});
                        renderer.addVertex(new float[] { -1.0F, 0.0F, -1.0F});
                        renderer.addVertex(new float[] { -1.0F, 0.0F, 1.0F});
                        renderer.addVertex(new float[] {1.0F, 0.0F, 1.0F});
                        renderer.addVertex(new float[] {1.0F, 0.0F, -1.0F});
                        renderer.addVertexRef(4);
                        renderer.draw();
                        GL11.glEndList();
                        long var10 = (long)geometryData.chunkX << 32 | (long)geometryData.chunkZ & 4294967295L;
                        Integer curValue = (Integer)_dgListMap.get(Long.valueOf(var10));
                        int limit;

                        if (curValue != null && curValue.intValue() != 0)
                        {
                            for (limit = 0; limit < _chunkDGListBuffer.limit(); ++limit)
                            {
                                if (_chunkDGListBuffer.get(limit) == curValue.intValue())
                                {
                                    _chunkDGListBuffer.put(limit, var9);
                                    break;
                                }
                            }

                            GL11.glDeleteLists(curValue.intValue(), 1);
                        }
                        else if (_chunkDGListBuffer == null)
                        {
                            _chunkDGListBuffer = ByteBuffer.allocateDirect(512).order(ByteOrder.nativeOrder()).asIntBuffer();
                            _chunkDGListBuffer.limit(1);
                            _chunkDGListBuffer.put(0, var9);
                        }
                        else
                        {
                            limit = _chunkDGListBuffer.limit();

                            if (limit == _chunkDGListBuffer.capacity())
                            {
                                IntBuffer oldBuffer = _chunkDGListBuffer;
                                _chunkDGListBuffer = ByteBuffer.allocateDirect(limit * 8).order(ByteOrder.nativeOrder()).asIntBuffer();
                                oldBuffer.rewind();
                                _chunkDGListBuffer.put(oldBuffer);
                            }

                            _chunkDGListBuffer.limit(limit + 1);
                            _chunkDGListBuffer.put(limit, var9);
                        }

                        _dgListMap.put(Long.valueOf(var10), Integer.valueOf(var9));
                        _chunkDGRequests.remove(Long.valueOf(var10));
                    }
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public static void clearDebuggingGeometry()
    {
    	for (Integer list : _dgListMap.values()) {
    		if (list != null && list.intValue() != 0)
            {
                GL11.glDeleteLists(list.intValue(), 1);
            }
    	}
        
        _dgListMap.clear();
        _chunkDGRequests.clear();

        if (_chunkDGListBuffer != null)
        {
            _chunkDGListBuffer.limit(0);
        }

        dgEnabled = true;
        _dgScanCounter = (new Random()).nextInt(40);
        ++_dgBatchID;
    }

    @SideOnly(Side.CLIENT)
    public static void addMystcraftSymbol(MystcraftSymbolData symbolData)
    {
        if (CustomOreGenBase.hasMystcraft())
        {
            CustomOreGenBase.log.finer("Downloaded symbol \'" + symbolData.symbolName + "\' from remote server.");
            MystcraftInterface.addCOGSymbol(symbolData);
        }
    }
}
