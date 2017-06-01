package CustomOreGen.Server;

import java.util.ArrayList;
import java.util.Random;

import CustomOreGen.Config.BlockAndPosition;
import CustomOreGen.Server.DistributionSettingMap.DistributionSetting;
import CustomOreGen.Util.HeightScaledPDist;
import CustomOreGen.Util.IGeometryBuilder;
import CustomOreGen.Util.IGeometryBuilder.PrimitiveType;
import CustomOreGen.Util.PDist;
import CustomOreGen.Util.PDist.Type;
import CustomOreGen.Util.Transform;
import CustomOreGen.Util.VolumeHelper;
import CustomOreGen.Util.WireframeShapes;
import CustomOreGen.Util.BlockDescriptor.BlockInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

public class MapGenClusters extends MapGenOreDistribution
{
    @DistributionSetting(
            name = "Size",
            info = "Roughly the number of blocks in every deposit.  No range."
    )
    public final PDist clSize = new PDist(8.0F, 0.0F);
    @DistributionSetting(
            name = "Frequency",
            info = "Number of deposits per 16x16 chunk.  No range."
    )
    public final HeightScaledPDist clFreq;
    @DistributionSetting(
            name = "Height",
            info = "Vertical height of the deposits.  Normal distributions are approximated."
    )
    public final HeightScaledPDist clHeight;
    
    protected static final DistributionSettingMap _clusterSettingsMap = new DistributionSettingMap(MapGenClusters.class);

    public MapGenClusters(int distributionID, boolean canGenerate)
    {
        super(_clusterSettingsMap, distributionID, canGenerate);
        this.clFreq = this.frequency;
        this.clHeight = new HeightScaledPDist(64.0F, 64.0F, Type.uniform);
        this.name = "StandardGen_" + distributionID;
        this.frequency.set(20.0F, 0.0F, Type.uniform);
    }

    public boolean validate() throws IllegalStateException
    {
        int maxClusterSize = (int)Math.ceil((double)(this.clSize.getMax() / 4.0F));
        super.range = (maxClusterSize + 15) / 16;
        return super.validate();
    }

    public Component generateStructure(StructureGroup structureGroup, Random random)
    {
        float clX = (random.nextFloat() + (float)structureGroup.chunkX) * 16.0F;
        float clZ = (random.nextFloat() + (float)structureGroup.chunkZ) * 16.0F;
        float clY = this.clHeight.getValue(random, this.worldObj, clX, clZ) + this.heightOffset.getValue(random);
        
        if (!structureGroup.canPlaceComponentAt(0, clX, clY, clZ, random))
        {
            return null;
        }
        else
        {
            ClusterComponent cluster = new ClusterComponent(structureGroup, clX, clY, clZ, random);
            structureGroup.addComponent(cluster, (Component)null);
            return cluster;
        }
    }
    
    private class ClusterComponent extends Component
    {
        protected final int size;
        protected final float[] ptA;
        protected final float[] ptB;
        protected final float[] rad;

        public ClusterComponent(StructureGroup structureGroup, float x, float y, float z, Random random)
        {
            super(structureGroup);
            this.size = Math.max(0, clSize.getIntValue(random));
            double horizAngle = (double)random.nextFloat() * Math.PI;
            this.ptA = new float[3];
            this.ptB = new float[3];
            float segmentXOffset = (float)Math.sin(horizAngle) * (float)this.size / 8.0F;
            float segmentZOffset = (float)Math.cos(horizAngle) * (float)this.size / 8.0F;
            this.ptA[0] = x + segmentXOffset;
            this.ptB[0] = x - segmentXOffset;
            this.ptA[2] = z + segmentZOffset;
            this.ptB[2] = z - segmentZOffset;
            this.ptA[1] = y + (float)random.nextInt(3) - 2.0F;
            this.ptB[1] = y + (float)random.nextInt(3) - 2.0F;
            super.boundingBox = StructureBoundingBox.getNewBoundingBox();
            this.rad = new float[this.size + 1];

            for (int s = 0; s < this.rad.length; ++s)
            {
                float ns = (float)s / (float)(this.rad.length - 1);
                float baseRadius = (float)random.nextDouble() * (float)this.size / 32.0F;
                this.rad[s] = MapGenClusters.adjustRadius(baseRadius, ns);
                float xCenter = this.ptA[0] + (this.ptB[0] - this.ptA[0]) * ns;
                float yCenter = this.ptA[1] + (this.ptB[1] - this.ptA[1]) * ns;
                float zCenter = this.ptA[2] + (this.ptB[2] - this.ptA[2]) * ns;
                super.boundingBox.minX = Math.min(super.boundingBox.minX, MathHelper.floor_float(xCenter - this.rad[s]));
                super.boundingBox.minY = Math.min(super.boundingBox.minY, MathHelper.floor_float(yCenter - this.rad[s]));
                super.boundingBox.minZ = Math.min(super.boundingBox.minZ, MathHelper.floor_float(zCenter - this.rad[s]));
                super.boundingBox.maxX = Math.max(super.boundingBox.maxX, MathHelper.ceiling_float_int(xCenter + this.rad[s]));
                super.boundingBox.maxY = Math.max(super.boundingBox.maxY, MathHelper.ceiling_float_int(yCenter + this.rad[s]));
                super.boundingBox.maxZ = Math.max(super.boundingBox.maxZ, MathHelper.ceiling_float_int(zCenter + this.rad[s]));
            }
        }

        public boolean addComponentParts(World world, Random random, StructureBoundingBox bounds)
        {
            ArrayList<BlockAndPosition> blockAndPositionList = new ArrayList();

            for (int s = 0; s < this.rad.length; ++s)
            {
                float ns = (float)s / (float)(this.rad.length - 1);
                float xCenter = this.ptA[0] + (this.ptB[0] - this.ptA[0]) * ns;
                float yCenter = this.ptA[1] + (this.ptB[1] - this.ptA[1]) * ns;
                float zCenter = this.ptA[2] + (this.ptB[2] - this.ptA[2]) * ns;
                int xMin = Math.max(MathHelper.floor_float(xCenter - this.rad[s]), bounds.minX);
                int xMax = Math.min(MathHelper.floor_float(xCenter + this.rad[s]), bounds.maxX);
                int yMin = Math.max(MathHelper.floor_float(yCenter - this.rad[s]), bounds.minY);
                int yMax = Math.min(MathHelper.ceiling_float_int(yCenter + this.rad[s]), bounds.maxY);
                int zMin = Math.max(MathHelper.ceiling_float_int(zCenter - this.rad[s]), bounds.minZ);
                int zMax = Math.min(MathHelper.ceiling_float_int(zCenter + this.rad[s]), bounds.maxZ);

                for (int tgtX = xMin; tgtX <= xMax; ++tgtX)
                {
                    double normXDist = ((double)tgtX + 0.5D - (double)xCenter) / (double)this.rad[s];

                    if (normXDist * normXDist < 1.0D)
                    {
                        for (int tgtY = yMin; tgtY <= yMax; ++tgtY)
                        {
                            double normYDist = ((double)tgtY + 0.5D - (double)yCenter) / (double)this.rad[s];

                            if (normXDist * normXDist + normYDist * normYDist < 1.0D)
                            {
                                for (int tgtZ = zMin; tgtZ <= zMax; ++tgtZ)
                                {
                                    double normZDist = ((double)tgtZ + 0.5D - (double)zCenter) / (double)this.rad[s];

                                    if (normXDist * normXDist + normYDist * normYDist + normZDist * normZDist < 1.0D)
                                    {
                                        // Just check if block should be placed and do the actual place later after all these
                                        // placement checks are done first.  That way you are not dealing with a dynamic
                                        // block structure as you do the placement restriction checks.
                                        BlockInfo match = this.testPlaceBlock(world, random, tgtX, tgtY, tgtZ, bounds);
                                        if (match != null) {
                                            blockAndPositionList.add(new BlockAndPosition(match, new BlockPos(tgtX, tgtY, tgtZ)));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // do the actual block creation after all placement restrictions have been checked first 
            for (BlockAndPosition blockAndPosition : blockAndPositionList) {
                this.placeBlock(world,
                        blockAndPosition.position.getX(), blockAndPosition.position.getY(), blockAndPosition.position.getZ(),
                        blockAndPosition.block);
            }

            super.addComponentParts(world, random, bounds);
            return true;
        }

        public void buildWireframe(IGeometryBuilder gb)
        {
            super.buildWireframe(gb);

            if (wfHasWireframe)
            {
                gb.setPositionTransform((Transform)null);
                gb.setVertexMode(PrimitiveType.LINE, new int[0]);
                gb.addVertex(this.ptA);
                gb.addVertex(this.ptB);
                float segLenX = this.ptB[0] - this.ptA[0];
                float segLenY = this.ptB[1] - this.ptA[1];
                float segLenZ = this.ptB[2] - this.ptA[2];
                float segLen = (float)Math.sqrt((double)(segLenX * segLenX + segLenY * segLenY + segLenZ * segLenZ));
                if (segLen == 0.0F) {
                	return;
                }
                int stepCount = MathHelper.ceiling_float_int(segLen);
                byte circleSides = 8;
                float[][] pts = WireframeShapes.getCirclePoints(circleSides, (float[][])null);
                float[] pos = new float[3];
                Transform trans = new Transform();
                gb.setVertexMode(PrimitiveType.QUAD, new int[] {circleSides + 1, circleSides + 2, 1});

                for (int step = 0; step <= stepCount; ++step)
                {
                    if (step == 0)
                    {
                        trans.translate(this.ptA[0], this.ptA[1], this.ptA[2]);
                        trans.rotateZInto(segLenX, segLenY, segLenZ);
                    }
                    else
                    {
                        trans.translate(0.0F, 0.0F, 1.0F);
                    }

                    gb.setPositionTransform(trans);
                    float radius = MapGenClusters.adjustRadius(this.size / 32.0F, (double)step/(double)stepCount);

                    for (int s = 0; s < circleSides; ++s)
                    {
                        pos[0] = pts[s][0] * radius;
                        pos[1] = pts[s][1] * radius;
                        pos[2] = pts[s][2];
                        gb.addVertex(pos, pos, (float[])null, (float[])null);
                    }

                    gb.addVertexRef(circleSides);
                }
            }
        }
    }

	@Override
	public String getStructureName() {
		return "COG:Clusters";
	}

	private static float adjustRadius(double baseRadius, double fraction) {
		return (float)((Math.sin(fraction * Math.PI) + 1.0F) * baseRadius + 0.5F);
	}
	
	@Override
	public double getAverageOreCount() {
		int segLen = MathHelper.ceiling_float_int(this.clSize.mean / 8.0F) * 2;
		double volume = 0;
		for (int s = 0; s < segLen; s++) {
			float rad = adjustRadius(this.clSize.mean / 64.0F, (double)s / (double)segLen);
			volume += VolumeHelper.cylindricalVolume(1.0F, rad);
		}
		return volume;
	}

}
