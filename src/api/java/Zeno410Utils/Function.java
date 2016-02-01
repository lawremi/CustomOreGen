
package Zeno410Utils;

/**
 *
 * @author Zeno410
 */
public abstract class Function<Src,Product> {
    public abstract Product result(Src source);

    public KeyedRegistry<Src,Product> registry(){
        return new KeyedRegistry<Src,Product>(this);
    }
}
