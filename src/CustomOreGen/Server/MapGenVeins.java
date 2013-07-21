package CustomOreGen.Server;

import java.util.Random;

import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import CustomOreGen.Server.DistributionSettingMap.DistributionSetting;
import CustomOreGen.Util.IGeometryBuilder;
import CustomOreGen.Util.IGeometryBuilder.PrimitiveType;
import CustomOreGen.Util.PDist;
import CustomOreGen.Util.PDist.Type;
import CustomOreGen.Util.Transform;
import CustomOreGen.Util.WireframeShapes;

public class MapGenVeins extends MapGenOreDistribution
{
    @DistributionSetting(
            name = "branchType",
            info = "Vein branch type (Bezier or Ellipsoid)"
    )
    public BranchType brType;
    @DistributionSetting(
            name = "MotherlodeFrequency",
            info = "Number of motherlodes per 16x16 chunk"
    )
    public final PDist mlFrequency;
    @DistributionSetting(
            name = "MotherlodeRangeLimit",
            info = "Max horizontal distance that a motherlode may be from the parent distribution, in meters"
    )
    public final PDist mlRangeLimit;
    @DistributionSetting(
            name = "MotherlodeSize",
            info = "Motherlode size (radius), in meters"
    )
    public final PDist mlSize;
    @DistributionSetting(
            name = "MotherlodeHeight",
            info = "Height of motherlode, in meters"
    )
    public final PDist mlHeight;
    @DistributionSetting(
            name = "BranchFrequency",
            info = "Number of branches per motherlode"
    )
    public final PDist brFrequency;
    @DistributionSetting(
            name = "BranchInclination",
            info = "Branch angle from horizontal plane, in radians"
    )
    public final PDist brInclination;
    @DistributionSetting(
            name = "BranchLength",
            info = "Length of branches, in meters"
    )
    public final PDist brLength;
    @DistributionSetting(
            name = "BranchHeightLimit",
            info = "Max vertical distance that a branch may go above/below motherlode, in meters"
    )
    public final PDist brHeightLimit;
    @DistributionSetting(
            name = "SegmentForkFrequency",
            info = "Forking rate of each segment"
    )
    public final PDist sgForkFrequency;
    @DistributionSetting(
            name = "SegmentForkLengthMult",
            info = "Multiplier to remaining branch length for each fork"
    )
    public final PDist sgForkLenMult;
    @DistributionSetting(
            name = "SegmentLength",
            info = "Length of branch segments, in meters"
    )
    public final PDist sgLength;
    @DistributionSetting(
            name = "SegmentAngle",
            info = "Angle at which each segment diverges from the previous segment, in radians"
    )
    public final PDist sgAngle;
    @DistributionSetting(
            name = "SegmentRadius",
            info = "Cross-section radius of branch segments, in meters"
    )
    public final PDist sgRadius;
    @DistributionSetting(
            name = "OreDensity",
            info = "Density multiplier for individual ore blocks"
    )
    public final PDist orDensity;
    @DistributionSetting(
            name = "OreRadiusMult",
            info = "Radius multiplier for individual ore blocks"
    )
    public final PDist orRadiusMult;
    protected static final DistributionSettingMap _veinsSettingMap = new DistributionSettingMap(MapGenVeins.class);

    public MapGenVeins(int distributionID, boolean canGenerate)
    {
        super(_veinsSettingMap, distributionID, canGenerate);
        this.brType = BranchType.Bezier;
        this.mlFrequency = this.frequency;
        this.mlRangeLimit = this.parentRangeLimit;
        this.mlSize = new PDist(2.5F, 1.0F);
        this.mlHeight = new PDist(32.0F, 16.0F, Type.normal);
        this.brFrequency = new PDist(3.0F, 2.0F);
        this.brInclination = new PDist(0.0F, 0.55F);
        this.brLength = new PDist(120.0F, 60.0F);
        this.brHeightLimit = new PDist(16.0F, 0.0F);
        this.sgForkFrequency = new PDist(0.2F, 0.0F);
        this.sgForkLenMult = new PDist(0.75F, 0.25F);
        this.sgLength = new PDist(15.0F, 6.0F);
        this.sgAngle = new PDist(0.5F, 0.5F);
        this.sgRadius = new PDist(0.5F, 0.3F);
        this.orDensity = new PDist(1.0F, 0.0F);
        this.orRadiusMult = new PDist(1.0F, 0.1F);
        this.name = "Veins_" + distributionID;
        this.frequency.set(0.025F, 0.0F, Type.uniform);
    }

    public boolean validate() throws IllegalStateException
    {
        float r = this.mlSize.getMax() * this.orRadiusMult.getMax();

        if (this.brFrequency.getMax() > 0.0F)
        {
            r += this.brLength.getMax();
        }

        super.range = (int)(r + 15.9999F) / 16;
        return super.validate();
    }

    public Component generateStructure(StructureGroup structureGroup, Random random)
    {
        float mlX = (random.nextFloat() + (float)structureGroup.chunkX) * 16.0F;
        float mlY = this.mlHeight.getValue(random);
        float mlZ = (random.nextFloat() + (float)structureGroup.chunkZ) * 16.0F;

        if (!structureGroup.canPlaceComponentAt(0, mlX, mlY, mlZ, random))
        {
            return null;
        }
        else
        {
            Transform mlMat = new Transform();
            mlMat.translate(mlX, mlY, mlZ);
            mlMat.rotateZ(random.nextFloat() * ((float)Math.PI * 2F));
            mlMat.rotateY(random.nextFloat() * ((float)Math.PI * 2F));
            mlMat.scale(this.mlSize.getValue(random), this.mlSize.getValue(random), this.mlSize.getValue(random));
            SolidSphereComponent motherlode = new SolidSphereComponent(structureGroup, mlMat);
            structureGroup.addComponent(motherlode, (Component)null);

            for (int br = this.brFrequency.getIntValue(random); br > 0; --br)
            {
                Random brRandom = new Random(random.nextLong());
                Transform segMat = new Transform();
                segMat.translate(mlX, mlY, mlZ);
                segMat.rotateY(brRandom.nextFloat() * ((float)Math.PI * 2F));
                segMat.rotateX(-this.brInclination.getValue(brRandom));
                float maxHeight = mlY + this.brHeightLimit.getValue(brRandom);
                float minHeight = mlY - this.brHeightLimit.getValue(brRandom);
                this.generateBranch(structureGroup, this.brLength.getValue(brRandom), maxHeight, minHeight, segMat, motherlode, brRandom);
            }

            return motherlode;
        }
    }

    public void generateBranch(StructureGroup structureGroup, float length, float maxHeight, float minHeight, Transform mat, Component parent, Random random)
    {
        float[] pos = new float[3];

        while (length > 0.0F)
        {
            float segLen = this.sgLength.getValue(random);

            if (segLen > length)
            {
                segLen = length;
            }

            length -= segLen;
            segLen /= 2.0F;
            float segRad = this.sgRadius.getValue(random);
            mat.translate(0.0F, 0.0F, segLen);
            Transform segMat = mat.clone().scale(segRad, segRad, segLen);
            Component component = null;

            switch (this.brType)
            {
                case Ellipsoid:
                    component = new SolidSphereComponent(structureGroup, segMat);
                    break;

                case Bezier:
                    component = new BezierTubeComponent(structureGroup, segMat);
            }

            structureGroup.addComponent((Component)component, (Component)parent);
            parent = component;
            mat.translate(0.0F, 0.0F, segLen);
            pos[0] = 0.0F;
            pos[1] = 0.0F;
            pos[2] = 0.0F;
            mat.transformVector(pos);

            if (pos[1] > maxHeight || pos[1] < minHeight)
            {
                return;
            }

            if (!structureGroup.canPlaceComponentAt(((Component)component).getComponentType() + 1, pos[0], pos[1], pos[2], random))
            {
                return;
            }

            if (length <= 0.0F)
            {
                return;
            }

            for (int axisTheta = this.sgForkFrequency.getIntValue(random); axisTheta > 0; --axisTheta)
            {
                Random fkRandom = new Random(random.nextLong());
                Transform fkMat = mat.clone();
                float axisTheta1 = fkRandom.nextFloat() * ((float)Math.PI * 2F);
                fkMat.rotate(this.sgAngle.getValue(fkRandom), MathHelper.cos(axisTheta1), MathHelper.sin(axisTheta1), 0.0F);
                float fkLenMult = this.sgForkLenMult.getValue(fkRandom);
                this.generateBranch(structureGroup, length * (fkLenMult > 1.0F ? 1.0F : fkLenMult), maxHeight, minHeight, fkMat, (Component)component, fkRandom);
            }

            float var18 = random.nextFloat() * ((float)Math.PI * 2F);
            mat.rotate(this.sgAngle.getValue(random), MathHelper.cos(var18), MathHelper.sin(var18), 0.0F);
        }
    }
    
    private class BezierTubeComponent extends Component
    {
        protected float[] mid;
        protected float[] end;
        protected final float rad;
        protected BezierTubeComponent prev;
        protected BezierTubeComponent next;
        protected final interpolationContext context;
        protected final Transform mat;

        public BezierTubeComponent(StructureGroup structureGroup, Transform transform)
        {
            super(structureGroup);
            this.mid = new float[] {0.0F, 0.0F, 0.0F};
            transform.transformVector(this.mid);
            this.end = new float[] {0.0F, 0.0F, 1.0F};
            transform.transformVector(this.end);
            float[] xunit = new float[] {1.0F, 0.0F, 0.0F, 0.0F};
            transform.transformVector(xunit);
            this.rad = MathHelper.sqrt_float(xunit[0] * xunit[0] + xunit[1] * xunit[1] + xunit[2] * xunit[2]);
            float rMax = this.rad * orRadiusMult.getMax();

            if (rMax < 0.0F)
            {
                rMax = 0.0F;
            }

            float[] bb = new float[] { -rMax, -rMax, -1.0F, rMax, rMax, 1.0F};
            transform.transformBB(bb);
            super.boundingBox = new StructureBoundingBox(MathHelper.floor_float(bb[0]), MathHelper.floor_float(bb[1]), MathHelper.floor_float(bb[2]), MathHelper.floor_float(bb[3]) + 1, MathHelper.floor_float(bb[4]) + 1, MathHelper.floor_float(bb[5]) + 1);
            this.context = new interpolationContext();
            this.mat = transform.identity();
        }

        public void setChild(Component comp)
        {
            super.setChild(comp);
            this.next = comp instanceof BezierTubeComponent ? (BezierTubeComponent)comp : null;

            if (this.next != null)
            {
                float rMax = this.interpolateRadius(0.5F) * orRadiusMult.getMax();

                if (rMax < 0.0F)
                {
                    rMax = 0.0F;
                }

                float[] pos = new float[3];
                this.interpolatePosition(pos, 0.5F);
                StructureBoundingBox bb = new StructureBoundingBox(MathHelper.floor_float(pos[0] - rMax), MathHelper.floor_float(pos[1] - rMax), MathHelper.floor_float(pos[2] - rMax), MathHelper.floor_float(pos[0] + rMax) + 1, MathHelper.floor_float(pos[1] + rMax) + 1, MathHelper.floor_float(pos[2] + rMax) + 1);
                super.boundingBox.expandTo(bb);
            }
        }

        public void setParent(Component comp)
        {
            super.setParent(comp);
            this.prev = comp instanceof BezierTubeComponent ? (BezierTubeComponent)comp : null;

            if (this.prev != null)
            {
                float t = this.prev.next == this ? -0.5F : -1.0F;
                float rMax = this.interpolateRadius(t) * orRadiusMult.getMax();

                if (rMax < 0.0F)
                {
                    rMax = 0.0F;
                }

                float[] pos = new float[3];
                this.interpolatePosition(pos, t);
                StructureBoundingBox bb = new StructureBoundingBox(MathHelper.floor_float(pos[0] - rMax), MathHelper.floor_float(pos[1] - rMax), MathHelper.floor_float(pos[2] - rMax), MathHelper.floor_float(pos[0] + rMax) + 1, MathHelper.floor_float(pos[1] + rMax) + 1, MathHelper.floor_float(pos[2] + rMax) + 1);
                super.boundingBox.expandTo(bb);
            }
        }

        public void interpolatePosition(float[] pos, float t)
        {
            float nt;

            if (t > 0.0F && this.next != null)
            {
                nt = 1.0F - t;
                pos[0] = nt * nt * this.mid[0] + 2.0F * t * nt * this.end[0] + t * t * this.next.mid[0];
                pos[1] = nt * nt * this.mid[1] + 2.0F * t * nt * this.end[1] + t * t * this.next.mid[1];
                pos[2] = nt * nt * this.mid[2] + 2.0F * t * nt * this.end[2] + t * t * this.next.mid[2];
            }
            else if (t < 0.0F && this.prev != null)
            {
                nt = 1.0F + t;
                pos[0] = nt * nt * this.mid[0] - 2.0F * t * nt * this.prev.end[0] + t * t * this.prev.mid[0];
                pos[1] = nt * nt * this.mid[1] - 2.0F * t * nt * this.prev.end[1] + t * t * this.prev.mid[1];
                pos[2] = nt * nt * this.mid[2] - 2.0F * t * nt * this.prev.end[2] + t * t * this.prev.mid[2];
            }
            else
            {
                nt = 1.0F - 2.0F * t;
                pos[0] = nt * this.mid[0] + 2.0F * t * this.end[0];
                pos[1] = nt * this.mid[1] + 2.0F * t * this.end[1];
                pos[2] = nt * this.mid[2] + 2.0F * t * this.end[2];
            }
        }

        public void interpolateDerivative(float[] der, float t)
        {
            if (t > 0.0F && this.next != null)
            {
                der[0] = 2.0F * ((1.0F - t) * (this.end[0] - this.mid[0]) + t * (this.next.mid[0] - this.end[0]));
                der[1] = 2.0F * ((1.0F - t) * (this.end[1] - this.mid[1]) + t * (this.next.mid[1] - this.end[1]));
                der[2] = 2.0F * ((1.0F - t) * (this.end[2] - this.mid[2]) + t * (this.next.mid[2] - this.end[2]));
            }
            else if (t < 0.0F && this.prev != null)
            {
                der[0] = 2.0F * ((1.0F + t) * (this.mid[0] - this.prev.end[0]) - t * (this.prev.end[0] - this.prev.mid[0]));
                der[1] = 2.0F * ((1.0F + t) * (this.mid[1] - this.prev.end[1]) - t * (this.prev.end[1] - this.prev.mid[1]));
                der[2] = 2.0F * ((1.0F + t) * (this.mid[2] - this.prev.end[2]) - t * (this.prev.end[2] - this.prev.mid[2]));
            }
            else
            {
                der[0] = 2.0F * (this.end[0] - this.mid[0]);
                der[1] = 2.0F * (this.end[1] - this.mid[1]);
                der[2] = 2.0F * (this.end[2] - this.mid[2]);
            }
        }

        public float interpolateRadius(float t)
        {
            return t > 0.0F && this.next != null ? (1.0F - t) * this.rad + t * this.next.rad : (t < 0.0F && this.prev != null ? (1.0F + t) * this.rad - t * this.prev.rad : (t <= 0.0F && t > -1.0F ? this.rad : (t > 0.0F && t < 0.5F ? this.rad * MathHelper.sqrt_float(1.0F - 4.0F * t * t) : 0.0F)));
        }

        public boolean addComponentParts(World world, Random random, StructureBoundingBox bounds)
        {
            float maxR = orRadiusMult.getMax();

            if (maxR < 0.0F)
            {
                maxR = 0.0F;
            }

            float maxR2 = maxR * maxR;
            float minR = orRadiusMult.getMin();

            if (minR < 0.0F)
            {
                minR = 0.0F;
            }

            float minR2 = minR * minR;
            float[] pos = new float[3];
            float[] bb = new float[6];
            boolean innerStep = true;
            this.context.init(0.0F, true);
            int var24;

            do
            {
                var24 = (int)this.context.radius / 4 + 1;

                if (this.context.radius > 0.0F)
                {
                    float step = 0.7F * (float)var24 / this.context.radius;
                    int stepCount = (int)(maxR / step) + 1;
                    boolean oneBlockThreshold = this.context.radius * maxR < 0.25F;
                    this.mat.identity();
                    this.mat.translate(this.context.pos[0], this.context.pos[1], this.context.pos[2]);
                    this.mat.rotateZInto(this.context.der[0], this.context.der[1], this.context.der[2]);
                    this.mat.scale(this.context.radius, this.context.radius, (float)var24);
                    bb[0] = -maxR;
                    bb[1] = -maxR;
                    bb[2] = -1.0F;
                    bb[3] = maxR;
                    bb[4] = maxR;
                    bb[5] = 1.0F;
                    this.mat.transformBB(bb);
                    boolean intersects = bb[3] >= (float)bounds.minX && bb[0] <= (float)bounds.maxX && bb[5] >= (float)bounds.minZ && bb[2] <= (float)bounds.maxZ && bb[4] >= (float)bounds.minY && bb[1] <= (float)bounds.maxY;

                    if (intersects)
                    {
                        for (int x = -stepCount; x < stepCount; ++x)
                        {
                            for (int y = -stepCount; y < stepCount; ++y)
                            {
                                pos[0] = (float)x * step;
                                pos[1] = (float)y * step;
                                pos[2] = 0.0F;
                                float r2 = pos[0] * pos[0] + pos[1] * pos[1];

                                if (r2 <= maxR2)
                                {
                                    if (r2 > minR2)
                                    {
                                        float baseX = orRadiusMult.getValue(random);

                                        if (r2 > baseX * baseX)
                                        {
                                            continue;
                                        }
                                    }

                                    if (!oneBlockThreshold || this.context.radius * maxR * 4.0F >= random.nextFloat())
                                    {
                                        this.mat.transformVector(pos);
                                        int var25 = MathHelper.floor_float(pos[0]) - var24 / 2;
                                        int baseY = MathHelper.floor_float(pos[1]) - var24 / 2;
                                        int baseZ = MathHelper.floor_float(pos[2]) - var24 / 2;

                                        for (int blockX = var25; blockX < var24 + var25; ++blockX)
                                        {
                                            for (int blockY = baseY; blockY < var24 + baseY; ++blockY)
                                            {
                                                for (int blockZ = baseZ; blockZ < var24 + baseZ; ++blockZ)
                                                {
                                                    if (orDensity.getIntValue(random) >= 1)
                                                    {
                                                        this.attemptPlaceBlock(world, random, blockX, blockY, blockZ, bounds);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            while (this.context.advance(0.7F * (float)var24));

            super.addComponentParts(world, random, bounds);
            return true;
        }

        public void buildWireframe(IGeometryBuilder gb)
        {
            super.buildWireframe(gb);

            if (wfHasWireframe)
            {
                gb.setPositionTransform((Transform)null);
                this.context.pos[0] = 2.0F * this.mid[0] - this.end[0];
                this.context.pos[1] = 2.0F * this.mid[1] - this.end[1];
                this.context.pos[2] = 2.0F * this.mid[2] - this.end[2];
                gb.setVertexMode(PrimitiveType.LINE, new int[0]);
                gb.addVertex(this.context.pos);
                gb.addVertex(this.end);
                byte segments = 10;
                gb.setVertexMode(PrimitiveType.QUAD, new int[] {segments + 1, segments + 2, 1});
                this.context.init(0.05F, true);

                do
                {
                    this.mat.identity();
                    this.mat.translate(this.context.pos[0], this.context.pos[1], this.context.pos[2]);
                    this.mat.rotateZInto(this.context.der[0], this.context.der[1], this.context.der[2]);
                    this.mat.scale(this.context.radius, this.context.radius, 0.0F);
                    gb.setPositionTransform(this.mat);
                    float[][] pts = WireframeShapes.getCirclePoints(segments, (float[][])null);

                    for (int s = 0; s < segments; ++s)
                    {
                        gb.addVertex(pts[s], pts[s], (float[])null, (float[])null);
                    }

                    gb.addVertexRef(segments);
                }
                while (this.context.advance(2.0F));
            }
        }
        
        class interpolationContext
        {
            public float[] pos;
            public float[] der;
            public float derLen;
            public float radius;
            public float err;
            public float t;
            public float dt;
            public boolean calcDer;

            public interpolationContext()
            {
                this.pos = new float[3];
                this.der = new float[3];
                this.t = 10.0F;
                this.dt = 0.05F;
            }

            public void init(float stepSize, boolean calculateDirection)
            {
                this.t = prev != null && prev.next != BezierTubeComponent.this ? -1.0F : -0.5F;

                if (stepSize > 0.0F)
                {
                    this.dt = stepSize;
                }

                interpolatePosition(this.pos, this.t);
                this.radius = interpolateRadius(this.t);
                this.calcDer = calculateDirection;

                if (this.calcDer)
                {
                    interpolateDerivative(this.der, this.t);
                    this.derLen = MathHelper.sqrt_float(this.der[0] * this.der[0] + this.der[1] * this.der[1] + this.der[2] * this.der[2]);
                    this.der[0] /= this.derLen;
                    this.der[1] /= this.derLen;
                    this.der[2] /= this.derLen;
                }
                else
                {
                    this.derLen = 0.0F;
                    this.der[0] = this.der[1] = this.der[2] = 0.0F;
                }

                this.err = 0.0F;
            }

            public boolean advance(float tolerance)
            {
                float pX = this.pos[0];
                float pY = this.pos[1];
                float pZ = this.pos[2];
                float dX = this.der[0];
                float dY = this.der[1];
                float dZ = this.der[2];
                float r = this.radius;

                do
                {
                    float nt = this.t + this.dt;
                    interpolatePosition(this.pos, nt);
                    float deltaX = pX - this.pos[0];
                    float deltaY = pY - this.pos[1];
                    float deltaZ = pZ - this.pos[2];
                    float d2 = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
                    this.err = d2;
                    this.radius = interpolateRadius(nt);
                    float avg2R = r + this.radius;
                    float maxErr;

                    if (this.calcDer)
                    {
                        interpolateDerivative(this.der, nt);
                        this.derLen = MathHelper.sqrt_float(this.der[0] * this.der[0] + this.der[1] * this.der[1] + this.der[2] * this.der[2]);
                        this.der[0] /= this.derLen;
                        this.der[1] /= this.derLen;
                        this.der[2] /= this.derLen;
                        deltaX = -dZ * this.der[1] + dY * this.der[2];
                        deltaY = dZ * this.der[0] - dX * this.der[2];
                        deltaZ = -dY * this.der[0] + dX * this.der[1];
                        maxErr = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
                        this.err += avg2R * avg2R * maxErr;
                    }

                    maxErr = tolerance * tolerance;

                    if (this.err > maxErr)
                    {
                        this.dt = (float)((double)this.dt * 0.6D);
                    }
                    else
                    {
                        if (this.err >= maxErr / 5.0F)
                        {
                            this.t += this.dt;
                            return this.t < 0.5F;
                        }

                        this.dt = (float)((double)this.dt * 1.8D);
                    }
                }
                while (this.dt >= Math.ulp(this.t) * 2.0F);

                throw new RuntimeException("CustomOreGen: Detected a possible infinite loop during bezier interpolation.  Please report this error.");
            }
        }

    }


    public enum BranchType	
    {
    	Ellipsoid,
    	Bezier;
    }

    class SolidSphereComponent extends Component
    {
        protected final Transform mat;
        protected final Transform invMat;

        public SolidSphereComponent(StructureGroup structureGroup, Transform transform)
        {
            super(structureGroup);
            float rMax = orRadiusMult.getMax();

            if (rMax < 0.0F)
            {
                rMax = 0.0F;
            }

            float[] bb = new float[] { -rMax, -rMax, -rMax, rMax, rMax, rMax};
            transform.transformBB(bb);
            super.boundingBox = new StructureBoundingBox(MathHelper.floor_float(bb[0]), MathHelper.floor_float(bb[1]), MathHelper.floor_float(bb[2]), MathHelper.floor_float(bb[3]) + 1, MathHelper.floor_float(bb[4]) + 1, MathHelper.floor_float(bb[5]) + 1);
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

        public boolean addComponentParts(World world, Random random, StructureBoundingBox bounds)
        {
            if (this.invMat == null)
            {
                return true;
            }
            else
            {
                float maxR2 = orRadiusMult.getMax();

                if (maxR2 < 0.0F)
                {
                    maxR2 = 0.0F;
                }

                maxR2 *= maxR2;
                float minR2 = orRadiusMult.getMin();

                if (minR2 < 0.0F)
                {
                    minR2 = 0.0F;
                }

                minR2 *= minR2;
                float[] pos = new float[3];

                for (int x = Math.max(super.boundingBox.minX, bounds.minX); x <= Math.min(super.boundingBox.maxX, bounds.maxX); ++x)
                {
                    for (int y = Math.max(super.boundingBox.minY, bounds.minY); y <= Math.min(super.boundingBox.maxY, bounds.maxY); ++y)
                    {
                        for (int z = Math.max(super.boundingBox.minZ, bounds.minZ); z <= Math.min(super.boundingBox.maxZ, bounds.maxZ); ++z)
                        {
                            pos[0] = (float)x + 0.5F;
                            pos[1] = (float)y + 0.5F;
                            pos[2] = (float)z + 0.5F;
                            this.invMat.transformVector(pos);
                            float r2 = pos[0] * pos[0] + pos[1] * pos[1] + pos[2] * pos[2];

                            if (r2 <= maxR2)
                            {
                                if (r2 > minR2)
                                {
                                    float rMax = orRadiusMult.getValue(random);

                                    if (r2 > rMax * rMax)
                                    {
                                        continue;
                                    }
                                }

                                if (orDensity.getIntValue(random) >= 1)
                                {
                                    this.attemptPlaceBlock(world, random, x, y, z, bounds);
                                }
                            }
                        }
                    }
                }

                super.addComponentParts(world, random, bounds);
                return true;
            }
        }

        public void buildWireframe(IGeometryBuilder gb)
        {
            super.buildWireframe(gb);

            if (wfHasWireframe)
            {
                gb.setPositionTransform(this.mat);
                WireframeShapes.addUnitWireSphere(gb, 8, 8);
                gb.setVertexMode(PrimitiveType.LINE, new int[0]);
                gb.addVertex(new float[] {0.0F, 0.0F, -1.0F});
                gb.addVertex(new float[] {0.0F, 0.0F, 1.0F});
            }
        }
    }

}
