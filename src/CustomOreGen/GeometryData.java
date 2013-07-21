package CustomOreGen;

import CustomOreGen.Util.GeometryStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collection;

public class GeometryData extends GeometryRequestData
{
    public transient Collection geometry;
    private static final long serialVersionUID = 2L;

    public GeometryData() {}

    public GeometryData(GeometryRequestData request, Collection geometry)
    {
        super(request.world, request.chunkX, request.chunkZ, request.batchID);
        this.geometry = geometry;
    }

    private void writeObject(ObjectOutputStream out) throws IOException
    {
        GeometryStream.getStreamData(this.geometry, out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        this.geometry = Arrays.asList(new GeometryStream[] {new GeometryStream(in)});
    }
}
