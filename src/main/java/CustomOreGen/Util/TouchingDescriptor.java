package CustomOreGen.Util;

import java.util.HashSet;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public class TouchingDescriptor {
    BlockDescriptor blockDescriptor;

    int minimumTouches;
    int maximumTouches;
    TouchingVolume volume;
    TouchingContactType contactType;
    TouchingDirection direction;
    boolean mandatory;
    boolean negate;

    // positions to check for this distribution
    HashSet<BlockPos> deltaPositionMap;

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
    private static HashSet<BlockPos> positionMapDirectionNorthEast;
    private static HashSet<BlockPos> positionMapDirectionNorthWest;
    private static HashSet<BlockPos> positionMapDirectionSouthEast;
    private static HashSet<BlockPos> positionMapDirectionSouthWest;
    private static HashSet<BlockPos> positionMapDirectionVertical;
    private static HashSet<BlockPos> positionMapDirectionHorizontal;

    private static HashSet<BlockPos> positionMapVolumeCube;
    private static HashSet<BlockPos> positionMapVolumePlaneXY;
    private static HashSet<BlockPos> positionMapVolumePlaneXZ;
    private static HashSet<BlockPos> positionMapVolumePlaneYZ;

    static {
        createPositionMapConstant();
    }

    public TouchingDescriptor(BlockDescriptor blockDescriptor, int minimumTouches, int maximumTouches,
            TouchingVolume volume, TouchingContactType contactType, TouchingDirection direction, boolean mandatory,
            boolean negate) {
        this.blockDescriptor = blockDescriptor;
        this.minimumTouches = minimumTouches;
        this.maximumTouches = maximumTouches;
        this.volume = volume;
        this.contactType = contactType;
        this.direction = direction;
        this.mandatory = mandatory;
        this.negate = negate;

        createPositionMap();
    }

    private void createPositionMap() {
        HashSet<BlockPos> volumeMap = getPositionMapConstant(this.volume);
        HashSet<BlockPos> contactTypeMap = getPositionMapConstant(this.contactType);
        HashSet<BlockPos> directionMap = getPositionMapConstant(this.direction);

        // block positions are just get the set union of the 3 maps
        deltaPositionMap = new HashSet<BlockPos>();
        deltaPositionMap.addAll(volumeMap);
        deltaPositionMap.retainAll(contactTypeMap);
        deltaPositionMap.retainAll(directionMap);
    }

    // pre-calculate delta positions for search area
    private static void createPositionMapConstant() {
        positionMapContactTypeFace = createPositionMapConstant(TouchingContactType.Face);
        positionMapContactTypeEdge = createPositionMapConstant(TouchingContactType.Edge);
        positionMapContactTypeVertex = createPositionMapConstant(TouchingContactType.Vertex);
        positionMapContactTypeFaceAndEdge = createPositionMapConstant(TouchingContactType.FaceAndEdge);
        positionMapContactTypeAny = createPositionMapConstant(TouchingContactType.Any);

        positionMapDirectionAny = createPositionMapConstant(Direction.NORTH);
        positionMapDirectionAny.addAll(createPositionMapConstant(Direction.EAST));
        positionMapDirectionAny.addAll(createPositionMapConstant(Direction.SOUTH));
        positionMapDirectionAny.addAll(createPositionMapConstant(Direction.WEST));
        positionMapDirectionAny.addAll(createPositionMapConstant(Direction.UP));
        positionMapDirectionAny.addAll(createPositionMapConstant(Direction.DOWN));

        positionMapDirectionNorth = createPositionMapConstant(Direction.NORTH);
        positionMapDirectionEast = createPositionMapConstant(Direction.EAST);
        positionMapDirectionSouth = createPositionMapConstant(Direction.SOUTH);
        positionMapDirectionWest = createPositionMapConstant(Direction.WEST);
        positionMapDirectionUp = createPositionMapConstant(Direction.UP);
        positionMapDirectionDown = createPositionMapConstant(Direction.DOWN);

        positionMapDirectionNorthSouth = createPositionMapConstant(Direction.SOUTH);
        positionMapDirectionNorthSouth.addAll(createPositionMapConstant(Direction.NORTH));
        positionMapDirectionEastWest = createPositionMapConstant(Direction.EAST);
        positionMapDirectionEastWest.addAll(createPositionMapConstant(Direction.WEST));

        positionMapDirectionNorthEast = createPositionMapConstant(Direction.NORTH);
        positionMapDirectionNorthEast.addAll(createPositionMapConstant(Direction.EAST));
        positionMapDirectionNorthWest = createPositionMapConstant(Direction.NORTH);
        positionMapDirectionNorthWest.addAll(createPositionMapConstant(Direction.WEST));
        positionMapDirectionSouthEast = createPositionMapConstant(Direction.SOUTH);
        positionMapDirectionSouthEast.addAll(createPositionMapConstant(Direction.EAST));
        positionMapDirectionSouthWest = createPositionMapConstant(Direction.SOUTH);
        positionMapDirectionSouthWest.addAll(createPositionMapConstant(Direction.WEST));

        positionMapDirectionVertical = createPositionMapConstant(Direction.UP);
        positionMapDirectionVertical.addAll(createPositionMapConstant(Direction.DOWN));
        positionMapDirectionHorizontal = createPositionMapConstant(Direction.NORTH);
        positionMapDirectionHorizontal.addAll(createPositionMapConstant(Direction.EAST));
        positionMapDirectionHorizontal.addAll(createPositionMapConstant(Direction.SOUTH));
        positionMapDirectionHorizontal.addAll(createPositionMapConstant(Direction.WEST));

        positionMapVolumeCube = createPositionMapConstant(TouchingVolume.Any);
        positionMapVolumePlaneXY = createPositionMapConstant(TouchingVolume.PlaneXY);
        positionMapVolumePlaneXZ = createPositionMapConstant(TouchingVolume.PlaneXZ);
        positionMapVolumePlaneYZ = createPositionMapConstant(TouchingVolume.PlaneYZ);
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

    // hardcoded delta positions for the different search volumes
    private static HashSet<BlockPos> createPositionMapConstant(TouchingVolume volume) {
        switch (volume) {
        default:
        case Any: {
            HashSet<BlockPos> returnValue = new HashSet<>();

            // face
            returnValue.add(new BlockPos(1, 0, 0));
            returnValue.add(new BlockPos(-1, 0, 0));
            returnValue.add(new BlockPos(0, 1, 0));
            returnValue.add(new BlockPos(0, -1, 0));
            returnValue.add(new BlockPos(0, 0, 1));
            returnValue.add(new BlockPos(0, 0, -1));

            // edge
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

            // vertex
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

    private static HashSet<BlockPos> getPositionMapConstant(TouchingVolume volume) {
        switch (volume) {
        default:
        case Any:
            return positionMapVolumeCube;
        case PlaneXY:
            return positionMapVolumePlaneXY;
        case PlaneXZ:
            return positionMapVolumePlaneXZ;
        case PlaneYZ:
            return positionMapVolumePlaneYZ;
        }
    }

    // each direction is the 3x3 wall of blocks next to the player  
    private static HashSet<BlockPos> createPositionMapConstant(Direction direction) {
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
        case NorthEast:
            return positionMapDirectionNorthEast;
        case NorthWest:
            return positionMapDirectionNorthWest;
        case SouthEast:
            return positionMapDirectionSouthEast;
        case SouthWest:
            return positionMapDirectionSouthWest;
        case Vertical:
            return positionMapDirectionVertical;
        case Horizontal:
            return positionMapDirectionHorizontal;
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
        NorthEast,
        NorthWest,
        SouthEast,
        SouthWest,
        Vertical,
        Horizontal,
    }

    public enum TouchingVolume {
        Any,
        PlaneXY,
        PlaneXZ,
        PlaneYZ,
    }
}
