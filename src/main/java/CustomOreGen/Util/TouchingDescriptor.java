package CustomOreGen.Util;

import java.util.HashSet;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class TouchingDescriptor {
    BlockDescriptor blockDescriptor;

    int minimumTouches;
    int maximumTouches;
    TouchingContactType contactType;
    TouchingDirection direction;
    HashSet<BlockPos> positionMap;
    boolean mandatory;
    boolean negate;

    // pre-calculate the search area for each kind of clause
    private static HashSet<BlockPos> positionMapContactTypeFace;
    private static HashSet<BlockPos> positionMapContactTypeEdge;
    private static HashSet<BlockPos> positionMapContactTypeVertex;
    private static HashSet<BlockPos> positionMapContactTypeFaceAndEdge;
    private static HashSet<BlockPos> positionMapContactTypeAny;

    private static HashSet<BlockPos> positionMapDirectionAny;
    private static HashSet<BlockPos> positionMapDirectionNorth;
    private static HashSet<BlockPos> positionMapDirectionEast;
    private static HashSet<BlockPos> positionMapDirectionSouth;
    private static HashSet<BlockPos> positionMapDirectionWest;
    private static HashSet<BlockPos> positionMapDirectionUp;
    private static HashSet<BlockPos> positionMapDirectionDown;
    private static HashSet<BlockPos> positionMapDirectionNorthSouth;
    private static HashSet<BlockPos> positionMapDirectionEastWest;
    private static HashSet<BlockPos> positionMapDirectionVertical;
    private static HashSet<BlockPos> positionMapDirectionHorizontal;
    private static HashSet<BlockPos> positionMapDirectionPlaneXY;
    private static HashSet<BlockPos> positionMapDirectionPlaneXZ;
    private static HashSet<BlockPos> positionMapDirectionPlaneYZ;

    static {
        createPositionMapConstant();
    }

    public TouchingDescriptor(BlockDescriptor blockDescriptor, int minimumTouches, int maximumTouches,
            TouchingContactType contactType, TouchingDirection direction, boolean mandatory, boolean negate) {
        this.blockDescriptor = blockDescriptor;
        this.minimumTouches = minimumTouches;
        this.maximumTouches = maximumTouches;
        this.contactType = contactType;
        this.direction = direction;
        this.mandatory = mandatory;
        this.negate = negate;

        createPositionMap();
    }

    private void createPositionMap() {
        HashSet<BlockPos> contactTypeMap = getPositionMapConstant(this.contactType);
        HashSet<BlockPos> directionMap = getPositionMapConstant(this.direction);

        // block positions are just get the set intersection of both maps
        positionMap = new HashSet<BlockPos>();
        positionMap.addAll(contactTypeMap);
        positionMap.retainAll(directionMap);
    }

    // pre-calculate delta positions for search area
    private static void createPositionMapConstant() {
        positionMapContactTypeFace = createPositionMapConstant(TouchingContactType.Face);
        positionMapContactTypeEdge = createPositionMapConstant(TouchingContactType.Edge);
        positionMapContactTypeVertex = createPositionMapConstant(TouchingContactType.Vertex);
        positionMapContactTypeFaceAndEdge = createPositionMapConstant(TouchingContactType.FaceAndEdge);
        positionMapContactTypeAny = createPositionMapConstant(TouchingContactType.Any);

        positionMapDirectionAny = createPositionMapConstant(EnumFacing.NORTH);
        positionMapDirectionAny.addAll(createPositionMapConstant(EnumFacing.EAST));
        positionMapDirectionAny.addAll(createPositionMapConstant(EnumFacing.SOUTH));
        positionMapDirectionAny.addAll(createPositionMapConstant(EnumFacing.WEST));
        positionMapDirectionAny.addAll(createPositionMapConstant(EnumFacing.UP));
        positionMapDirectionAny.addAll(createPositionMapConstant(EnumFacing.DOWN));

        positionMapDirectionNorth = createPositionMapConstant(EnumFacing.NORTH);
        positionMapDirectionEast = createPositionMapConstant(EnumFacing.EAST);
        positionMapDirectionSouth = createPositionMapConstant(EnumFacing.SOUTH);
        positionMapDirectionWest = createPositionMapConstant(EnumFacing.WEST);
        positionMapDirectionUp = createPositionMapConstant(EnumFacing.UP);
        positionMapDirectionDown = createPositionMapConstant(EnumFacing.DOWN);

        positionMapDirectionNorthSouth = createPositionMapConstant(EnumFacing.SOUTH);
        positionMapDirectionNorthSouth.addAll(createPositionMapConstant(EnumFacing.NORTH));
        positionMapDirectionEastWest = createPositionMapConstant(EnumFacing.EAST);
        positionMapDirectionEastWest.addAll(createPositionMapConstant(EnumFacing.WEST));
        positionMapDirectionVertical = createPositionMapConstant(EnumFacing.UP);
        positionMapDirectionVertical.addAll(createPositionMapConstant(EnumFacing.DOWN));
        positionMapDirectionHorizontal = createPositionMapConstant(EnumFacing.NORTH);
        positionMapDirectionHorizontal.addAll(createPositionMapConstant(EnumFacing.EAST));
        positionMapDirectionHorizontal.addAll(createPositionMapConstant(EnumFacing.SOUTH));
        positionMapDirectionHorizontal.addAll(createPositionMapConstant(EnumFacing.WEST));
        
        positionMapDirectionPlaneXY = createPositionMapConstantPlane(TouchingDirection.PlaneXY);
        positionMapDirectionPlaneXZ = createPositionMapConstantPlane(TouchingDirection.PlaneXZ);
        positionMapDirectionPlaneYZ = createPositionMapConstantPlane(TouchingDirection.PlaneYZ);
    }

    // hardcoded delta positions for the different search area types
    private static HashSet<BlockPos> createPositionMapConstant(TouchingContactType contactType) {
        switch (contactType) {
        default:
        case Face: {
            HashSet<BlockPos> returnValue = new HashSet<>();
            returnValue.add(new BlockPos(1, 0, 0));
            returnValue.add(new BlockPos(-1, 0, 0));
            returnValue.add(new BlockPos(0, 1, 0));
            returnValue.add(new BlockPos(0, -1, 0));
            returnValue.add(new BlockPos(0, 0, 1));
            returnValue.add(new BlockPos(0, 0, -1));
            return returnValue;
        }

        case Edge: {
            HashSet<BlockPos> returnValue = new HashSet<>();
            returnValue.add(new BlockPos(-1, 0, -1));
            returnValue.add(new BlockPos(-1, 0, 1));
            returnValue.add(new BlockPos(-1, -1, 0));
            returnValue.add(new BlockPos(-1, 1, 0));
            returnValue.add(new BlockPos(0, -1, -1));
            returnValue.add(new BlockPos(0, -1, 1));
            returnValue.add(new BlockPos(0, 1, -1));
            returnValue.add(new BlockPos(0, 1, 1));
            returnValue.add(new BlockPos(1, -1, 0));
            returnValue.add(new BlockPos(1, 1, 0));
            returnValue.add(new BlockPos(1, 0, -1));
            returnValue.add(new BlockPos(1, 0, 1));
            return returnValue;
        }

        case Vertex: {
            HashSet<BlockPos> returnValue = new HashSet<>();
            returnValue = new HashSet<>();
            returnValue.add(new BlockPos(-1, -1, -1));
            returnValue.add(new BlockPos(-1, -1, 1));
            returnValue.add(new BlockPos(-1, 1, -1));
            returnValue.add(new BlockPos(-1, 1, 1));
            returnValue.add(new BlockPos(1, -1, -1));
            returnValue.add(new BlockPos(1, -1, 1));
            returnValue.add(new BlockPos(1, 1, -1));
            returnValue.add(new BlockPos(1, 1, 1));
            return returnValue;
        }

        case Any: {
            HashSet<BlockPos> returnValue = createPositionMapConstant(TouchingContactType.Face);
            returnValue.addAll(createPositionMapConstant(TouchingContactType.Edge));
            returnValue.addAll(createPositionMapConstant(TouchingContactType.Vertex));
            return returnValue;
        }

        case FaceAndEdge: {
            HashSet<BlockPos> returnValue = createPositionMapConstant(TouchingContactType.Face);
            returnValue.addAll(createPositionMapConstant(TouchingContactType.Edge));
            return returnValue;
        }
        }
    }

    private static HashSet<BlockPos> getPositionMapConstant(TouchingContactType contactType) {
        switch (contactType) {
        default:
        case Face:
            return positionMapContactTypeFace;
        case Edge:
            return positionMapContactTypeEdge;
        case Vertex:
            return positionMapContactTypeVertex;
        case Any:
            return positionMapContactTypeAny;
        case FaceAndEdge:
            return positionMapContactTypeFaceAndEdge;
        }
    }

    // each direction is the 3x3 wall of blocks next to the player
    private static HashSet<BlockPos> createPositionMapConstant(EnumFacing direction) {
        switch (direction) {
        default:
        case NORTH: {
            HashSet<BlockPos> returnValue = new HashSet<>();
            returnValue.add(new BlockPos(-1, -1, -1));
            returnValue.add(new BlockPos(-1, 0, -1));
            returnValue.add(new BlockPos(-1, 1, -1));
            returnValue.add(new BlockPos(0, -1, -1));
            returnValue.add(new BlockPos(0, 0, -1));
            returnValue.add(new BlockPos(0, 1, -1));
            returnValue.add(new BlockPos(1, -1, -1));
            returnValue.add(new BlockPos(1, 0, -1));
            returnValue.add(new BlockPos(1, 1, -1));
            return returnValue;
        }

        case EAST: {
            HashSet<BlockPos> returnValue = new HashSet<>();
            returnValue.add(new BlockPos(1, -1, -1));
            returnValue.add(new BlockPos(1, -1, 0));
            returnValue.add(new BlockPos(1, -1, 1));
            returnValue.add(new BlockPos(1, 0, -1));
            returnValue.add(new BlockPos(1, 0, 0));
            returnValue.add(new BlockPos(1, 0, 1));
            returnValue.add(new BlockPos(1, 1, -1));
            returnValue.add(new BlockPos(1, 1, 0));
            returnValue.add(new BlockPos(1, 1, 1));
            return returnValue;
        }

        case SOUTH: {
            HashSet<BlockPos> returnValue = new HashSet<>();
            returnValue.add(new BlockPos(-1, -1, 1));
            returnValue.add(new BlockPos(-1, 0, 1));
            returnValue.add(new BlockPos(-1, 1, 1));
            returnValue.add(new BlockPos(0, -1, 1));
            returnValue.add(new BlockPos(0, 0, 1));
            returnValue.add(new BlockPos(0, 1, 1));
            returnValue.add(new BlockPos(1, -1, 1));
            returnValue.add(new BlockPos(1, 0, 1));
            returnValue.add(new BlockPos(1, 1, 1));
            return returnValue;
        }

        case WEST: {
            HashSet<BlockPos> returnValue = new HashSet<>();
            returnValue.add(new BlockPos(-1, -1, -1));
            returnValue.add(new BlockPos(-1, -1, 0));
            returnValue.add(new BlockPos(-1, -1, 1));
            returnValue.add(new BlockPos(-1, 0, -1));
            returnValue.add(new BlockPos(-1, 0, 0));
            returnValue.add(new BlockPos(-1, 0, 1));
            returnValue.add(new BlockPos(-1, 1, -1));
            returnValue.add(new BlockPos(-1, 1, 0));
            returnValue.add(new BlockPos(-1, 1, 1));
            return returnValue;
        }

        case UP: {
            HashSet<BlockPos> returnValue = new HashSet<>();
            returnValue.add(new BlockPos(-1, 1, -1));
            returnValue.add(new BlockPos(-1, 1, 0));
            returnValue.add(new BlockPos(-1, 1, 1));
            returnValue.add(new BlockPos(0, 1, -1));
            returnValue.add(new BlockPos(0, 1, 0));
            returnValue.add(new BlockPos(0, 1, 1));
            returnValue.add(new BlockPos(1, 1, -1));
            returnValue.add(new BlockPos(1, 1, 0));
            returnValue.add(new BlockPos(1, 1, 1));
            return returnValue;
        }

        case DOWN: {
            HashSet<BlockPos> returnValue = new HashSet<>();
            returnValue.add(new BlockPos(-1, -1, -1));
            returnValue.add(new BlockPos(-1, -1, 0));
            returnValue.add(new BlockPos(-1, -1, 1));
            returnValue.add(new BlockPos(0, -1, -1));
            returnValue.add(new BlockPos(0, -1, 0));
            returnValue.add(new BlockPos(0, -1, 1));
            returnValue.add(new BlockPos(1, -1, -1));
            returnValue.add(new BlockPos(1, -1, 0));
            returnValue.add(new BlockPos(1, -1, 1));
            return returnValue;
        }
        }
    }

    private static HashSet<BlockPos> createPositionMapConstantPlane(TouchingDirection plane) {
        switch (plane) {
        default: return null;
        
        case PlaneXY: {
            HashSet<BlockPos> returnValue = new HashSet<>();
            returnValue.add(new BlockPos(-1, -1, 0));
            returnValue.add(new BlockPos(-1, 0, 0));
            returnValue.add(new BlockPos(-1, 1, 0));
            returnValue.add(new BlockPos(0, -1, 0));
            returnValue.add(new BlockPos(0, 1, 0));
            returnValue.add(new BlockPos(1, -1, 0));
            returnValue.add(new BlockPos(1, 0, 0));
            returnValue.add(new BlockPos(1, 1, 0));
            return returnValue;
        }
        case PlaneXZ: {
            HashSet<BlockPos> returnValue = new HashSet<>();
            returnValue.add(new BlockPos(-1, 0, -1));
            returnValue.add(new BlockPos(-1, 0, 0));
            returnValue.add(new BlockPos(-1, 0, 1));
            returnValue.add(new BlockPos(0, 0, -1));
            returnValue.add(new BlockPos(0, 0, 1));
            returnValue.add(new BlockPos(1, 0, -1));
            returnValue.add(new BlockPos(1, 0, 0));
            returnValue.add(new BlockPos(1, 0, 1));
            return returnValue;
        }
        case PlaneYZ: {
            HashSet<BlockPos> returnValue = new HashSet<>();
            returnValue.add(new BlockPos(0, -1, -1));
            returnValue.add(new BlockPos(0, -1, 0));
            returnValue.add(new BlockPos(0, -1, 1));
            returnValue.add(new BlockPos(0, 0, -1));
            returnValue.add(new BlockPos(0, 0, 1));
            returnValue.add(new BlockPos(0, 1, -1));
            returnValue.add(new BlockPos(0, 1, 0));
            returnValue.add(new BlockPos(0, 1, 1));
            return returnValue;
        }
        }
    }
    private static HashSet<BlockPos> getPositionMapConstant(TouchingDirection direction) {
        switch (direction) {
        default:
        case Any:
            return positionMapDirectionAny;
        case North:
            return positionMapDirectionNorth;
        case East:
            return positionMapDirectionEast;
        case South:
            return positionMapDirectionSouth;
        case West:
            return positionMapDirectionWest;
        case Up:
            return positionMapDirectionUp;
        case Down:
            return positionMapDirectionDown;
        case NorthSouth:
            return positionMapDirectionNorthSouth;
        case EastWest:
            return positionMapDirectionEastWest;
        case Vertical:
            return positionMapDirectionVertical;
        case Horizontal:
            return positionMapDirectionHorizontal;
        case PlaneXY:
            return positionMapDirectionPlaneXY;
        case PlaneXZ:
            return positionMapDirectionPlaneXZ;
        case PlaneYZ:
            return positionMapDirectionPlaneYZ;
        }
    }

    public enum TouchingContactType {
        Any,
        Face,
        Edge,
        Vertex,
        FaceAndEdge,
    }

    public enum TouchingDirection {
        Any,
        North,
        East,
        South,
        West,
        Up,
        Down,
        NorthSouth,
        EastWest,
        Vertical,
        Horizontal,
        PlaneXY,
        PlaneXZ,
        PlaneYZ,
    }
}
