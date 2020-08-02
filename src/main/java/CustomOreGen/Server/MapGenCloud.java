package CustomOreGen.Server;

import java.util.Random;

import CustomOreGen.Server.DistributionSettingMap.DistributionSetting;
import CustomOreGen.Util.HeightScaledPDist;
import CustomOreGen.Util.IGeometryBuilder;
import CustomOreGen.Util.IGeometryBuilder.PrimitiveType;
import CustomOreGen.Util.NoiseGenerator;
import CustomOreGen.Util.PDist;
import CustomOreGen.Util.PDist.Type;
import CustomOreGen.Util.Transform;
import CustomOreGen.Util.WireframeShapes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

public class MapGenCloud extends MapGenOreDistribution
{
    @DistributionSetting(
            name = "CloudRadius",
            info = "Cloud horizontal radius, in meters"
    )
    public final PDist clRadius = new PDist(25.0F, 10.0F);
    @DistributionSetting(
            name = "CloudThickness",
            info = "Cloud thickness (vertical radius), in meters"
    )
    public final HeightScaledPDist clThickness = new HeightScaledPDist(14.0F, 6.0F);
    @DistributionSetting(
            name = "CloudSizeNoise",
            info = "Noise level added to cloud radius and thickness"
    )
    public final PDist clSizeNoise = new PDist(0.2F, 0.0F);
    @DistributionSetting(
            name = "CloudHeight",
            info = "Height of cloud, in meters"
    )
    public final HeightScaledPDist clHeight;
    @DistributionSetting(
            name = "CloudInclination",
            info = "Cloud angle from horizontal plane, in radians"
    )
    public final PDist clInclination;
    @DistributionSetting(
            name = "OreRadiusMult",
            info = "Radius multiplier for individual ore blocks"
    )
    public final PDist orRadiusMult;
    @DistributionSetting(
            name = "OreDensity",
            info = "Density multiplier for individual ore blocks"
    )
    public final HeightScaledPDist orDensity;
    @DistributionSetting(
            name = "OreVolumeNoiseCutoff",
            info = "Minimum threshold for density noise on individual ore blocks"
    )
    public final PDist orVolumeNoiseCutoff;
    protected static final DistributionSettingMap _cloudSettingsMap = new DistributionSettingMap(MapGenCloud.class);

    public MapGenCloud(int distributionID, boolean canGenerate)
    {
        super(_cloudSettingsMap, distributionID, canGenerate);
        this.clHeight = new HeightScaledPDist(32.0F, 16.0F, Type.normal);
        this.clInclination = new PDist(0.0F, 0.35F);
        this.orRadiusMult = new PDist(1.0F, 0.1F);
        this.orDensity = new HeightScaledPDist(0.1F, 0.0F);
        this.orVolumeNoiseCutoff = new PDist(0.5F, 0.0F);
        this.name = "Cloud_" + distributionID;
        this.frequency.set(0.001F, 0.0F, Type.uniform);
    }

    public boolean validate() throws IllegalStateException
    {
        float r = Math.max(this.clRadius.getMax(), this.clThickness.pdist.getMax());
        r *= 1.0F + this.clSizeNoise.getMax() * 2.0F;
        r *= this.orRadiusMult.getMax();
        range = (int)(r + 15.9999F) / 16;
        return super.validate();
    }

    public Component generateStructure(StructureGroup structureGroup, Random random)
    {
        float clX = (random.nextFloat() + (float)structureGroup.getChunkPosX()) * 16.0F;
        float clZ = (random.nextFloat() + (float)structureGroup.getChunkPosZ()) * 16.0F;
        float clY = this.clHeight.getValue(random, this.world.getWorld(), clX, clZ) + this.heightOffset.getValue(random);
        
        if (!structureGroup.canPlaceComponentAt(0, clX, clY, clZ, random))
        {
            return null;
        }
        else
        {
            Transform clMat = new Transform();
            clMat.translate(clX, clY, clZ);
            clMat.rotateZInto(0.0F, 1.0F, 0.0F);
            clMat.rotateZ(random.nextFloat() * ((float)Math.PI * 2F));
            clMat.rotateY(this.clInclination.getValue(random));
            float thickness = this.clThickness.getValue(random, this.world.getWorld(), clX, clZ);
            clMat.scale(this.clRadius.getValue(random), this.clRadius.getValue(random), thickness);
            DiffuseCloudComponent cloud = new DiffuseCloudComponent(structureGroup, clMat, random);
            structureGroup.addComponent(cloud, (Component)null);
            return cloud;
        }
    }
    
    private class DiffuseCloudComponent extends Component
    {
        protected final Transform mat;
        protected final Transform invMat;
        protected final NoiseGenerator noiseGen;
        protected final float sizeNoiseMagnitude;
        protected final int noiseLevels;

        public DiffuseCloudComponent(StructureGroup structureGroup, Transform transform, Random random)
        {
            super(structureGroup);
            this.noiseGen = new NoiseGenerator(random);
            this.sizeNoiseMagnitude = Math.abs(clSizeNoise.getValue(random));
            float rMax = (1.0F + this.sizeNoiseMagnitude * 2.0F) * orRadiusMult.getMax();

            if (rMax < 0.0F)
            {
                rMax = 0.0F;
            }

            float[] bb = new float[] { -rMax, -rMax, -rMax, rMax, rMax, rMax};
            transform.transformBB(bb);
            super.boundingBox = new MutableBoundingBox(MathHelper.floor(bb[0]), MathHelper.floor(bb[1]), MathHelper.floor(bb[2]), MathHelper.floor(bb[3]) + 1, MathHelper.floor(bb[4]) + 1, MathHelper.floor(bb[5]) + 1);
            float maxSize = (float)Math.max(super.boundingBox.getXSize(), Math.max(super.boundingBox.getYSize(), super.boundingBox.getZSize())) * 0.2F;
            this.noiseLevels = maxSize <= 1.0F ? 0 : (int)(Math.log((double)maxSize) / Math.log(2.0D) + 0.5D);
            this.mat = transform.clone();

            if (transform.determinant() != 0.0F)
            {
                this.invMat = transform.inverse();
            }
            else
            {
                this.invMat = null;
            }
        }

        public float getNoise(float x, float y, float z)
        {
            double noise = 0.0D;

            for (int i = 0; i < this.noiseLevels; ++i)
            {
                float im = (float)(1 << i);
                noise += (double)(1.0F / im) * this.noiseGen.noise((double)(x * im), (double)(y * im), (double)(z * im));
            }

            return (float)noise;
        }

        @Override
        public boolean addComponentParts(IWorld world, Random random, MutableBoundingBox bounds, ChunkPos cpos)
        {
            if (this.invMat == null)
            {
                return true;
            }
            else
            {
                float maxR = Math.max(orRadiusMult.getMax(), 0.0F);
                float minR = Math.max(orRadiusMult.getMin(), 0.0F);
                float maxNoisyR2 = maxR * (1.0F + this.sizeNoiseMagnitude * 2.0F);
                float minNoisyR2 = minR * (1.0F - this.sizeNoiseMagnitude * 2.0F);
                maxNoisyR2 *= maxNoisyR2;
                minNoisyR2 *= minNoisyR2;
                float[] pos = new float[3];

                for (int x = Math.max(super.boundingBox.minX, bounds.minX); x <= Math.min(super.boundingBox.maxX, bounds.maxX); ++x)
                {
                    for (int y = Math.min(super.boundingBox.maxY, bounds.maxY); y >= Math.max(super.boundingBox.minY, bounds.minY); --y)
                    {
                        for (int z = Math.max(super.boundingBox.minZ, bounds.minZ); z <= Math.min(super.boundingBox.maxZ, bounds.maxZ); ++z)
                        {
                            pos[0] = (float)x + 0.5F;
                            pos[1] = (float)y + 0.5F;
                            pos[2] = (float)z + 0.5F;
                            this.invMat.transformVector(pos);
                            float r2 = pos[0] * pos[0] + pos[1] * pos[1] + pos[2] * pos[2];

                            if (r2 <= maxNoisyR2)
                            {
                                if (r2 > minNoisyR2)
                                {
                                    float r = MathHelper.sqrt(r2);
                                    float mult = 1.0F;

                                    if (r > 0.0F)
                                    {
                                        mult += this.sizeNoiseMagnitude * this.getNoise(pos[0] / r, pos[1] / r, pos[2] / r);
                                    }
                                    else
                                    {
                                        mult += this.sizeNoiseMagnitude * this.getNoise(0.0F, 0.0F, 0.0F);
                                    }

                                    if (mult <= 0.0F)
                                    {
                                        continue;
                                    }

                                    r /= mult;

                                    if (r > maxR || r > minR && r > orRadiusMult.getValue(random))
                                    {
                                        continue;
                                    }
                                }

                                if (orVolumeNoiseCutoff.getMin() <= 1.0F && 
                                	(orVolumeNoiseCutoff.getMax() <= 0.0F || 
                                	 (this.getNoise(pos[0], pos[1], pos[2]) + 1.0F) / 2.0F >= orVolumeNoiseCutoff.getValue(random)) && 
                                	orDensity.getIntValue(random, world.getWorld(), x, z) >= 1)
                                {
                                    this.attemptPlaceBlock(world, random, x, y, z, bounds);
                                }
                            }
                        }
                    }
                }

                super.addComponentParts(world, random, bounds, cpos);
                return true;
            }
        }

        public void buildWireframe(IGeometryBuilder gb)
        {
            super.buildWireframe(gb);

            if (wfHasWireframe && this.mat != null)
            {
                int segments = Math.max(8, (1 << this.noiseLevels) * 4);
                int stacks = segments;
                gb.setPositionTransform(this.mat);
                gb.setVertexMode(PrimitiveType.QUAD, new int[] {segments + 1, segments + 2, 1});
                float[][] xycoords = WireframeShapes.getCirclePoints(segments, (float[][])null);
                float[][] zcoords = WireframeShapes.getCirclePoints(2 * segments, (float[][])null);
                float[] pos = new float[3];
                int offset;
                int s;
                float mult;

                for (offset = 1; offset < stacks; ++offset)
                {
                    for (s = 0; s < segments; ++s)
                    {
                        pos[0] = zcoords[offset][1] * xycoords[s][0];
                        pos[1] = zcoords[offset][1] * xycoords[s][1];
                        pos[2] = zcoords[offset][0];
                        mult = 1.0F + this.sizeNoiseMagnitude * this.getNoise(pos[0], pos[1], pos[2]);
                        pos[0] *= mult;
                        pos[1] *= mult;
                        pos[2] *= mult;
                        gb.addVertex(pos);
                    }

                    gb.addVertexRef(segments);
                }

                gb.setVertexMode(PrimitiveType.TRIANGLE, new int[] {1});
                offset = (segments + 1) * (stacks - 2) + 1;
                gb.addVertexRef(offset);

                for (s = 1; s <= segments; ++s)
                {
                    if (s == 1)
                    {
                        pos[0] = 0.0F;
                        pos[1] = 0.0F;
                        pos[2] = 1.0F;
                        mult = 1.0F + this.sizeNoiseMagnitude * this.getNoise(pos[0], pos[1], pos[2]);
                        pos[0] *= mult;
                        pos[1] *= mult;
                        pos[2] *= mult;
                        gb.addVertex(pos);
                    }
                    else
                    {
                        gb.addVertexRef(2);
                    }

                    gb.addVertexRef(offset + 3 * s);
                }

                gb.setVertexMode(PrimitiveType.TRIANGLE, new int[] {1});
                offset = 3 * segments + 2;
                gb.addVertexRef(offset);

                for (s = 1; s <= segments; ++s)
                {
                    if (s == 1)
                    {
                        pos[0] = 0.0F;
                        pos[1] = 0.0F;
                        pos[2] = -1.0F;
                        mult = 1.0F + this.sizeNoiseMagnitude * this.getNoise(pos[0], pos[1], pos[2]);
                        pos[0] *= mult;
                        pos[1] *= mult;
                        pos[2] *= mult;
                        gb.addVertex(pos);
                    }
                    else
                    {
                        gb.addVertexRef(2);
                    }

                    gb.addVertexRef(offset + s);
                }
            }
        }
    }

	@Override
	public String getStructureName() {
		return "COG:Cloud";
	}

	@Override
	public double getAverageOreCount() {
		float radius = this.clRadius.mean * this.orRadiusMult.mean;
		float thickness = this.clThickness.pdist.mean * this.orRadiusMult.mean;
		float v = (4.0F / 3.0F) * radius * radius * thickness;
		PDist simplex = new PDist(0.5F, 0.1F, Type.normal);
		double aboveNoiseCutoff = (1 - simplex.cdf(this.orVolumeNoiseCutoff.mean));
		return v * this.orDensity.pdist.mean * aboveNoiseCutoff;
	}

	@Override
	public BlockPos getNearestStructurePos(World worldIn, BlockPos pos, boolean findUnexplored) {
		return null;
	}
}
