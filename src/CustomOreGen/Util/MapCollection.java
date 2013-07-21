package CustomOreGen.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public abstract class MapCollection<K,V> implements Collection<V>
{
    protected final Map<K,V> backingMap;

    public MapCollection(Map<K,V> backingMap)
    {
        this.backingMap = backingMap;
        for (Entry<K,V> entry : backingMap.entrySet()) {
        	K key = entry.getKey();
            V value = entry.getValue();
            K nKey = this.getKey(value);
            if (key != nKey && (key == null || !key.equals(nKey))) {
            	throw new IllegalArgumentException("Backing set contains inconsistent key/value pair \'" + key + "\' -> \'" + value + "\', expected \'" + nKey + "\' -> \'" + value + "\'");
            }
        }        
    }

    protected abstract K getKey(V var1);

    public int size()
    {
        return this.backingMap.size();
    }

    public boolean isEmpty()
    {
        return this.backingMap.isEmpty();
    }

    public boolean contains(Object o)
    {
        Object key = this.getKey((V)o);
        return this.backingMap.containsKey(key);
    }

    public Iterator iterator()
    {
        return this.backingMap.values().iterator();
    }

    public Object[] toArray()
    {
        return this.backingMap.values().toArray();
    }

    public <T> T[] toArray(T[] a)
    {
        return this.backingMap.values().toArray(a);
    }

    public boolean add(V v)
    {
        K key = this.getKey(v);

        if (v != null)
        {
            return this.backingMap.put(key, v) != v;
        }
        else
        {
            boolean hasKey = this.backingMap.containsKey(key);
            Object prev = this.backingMap.put(key, v);
            return !hasKey || v != prev;
        }
    }

    public boolean remove(Object o)
    {
        Object key = this.getKey((V)o);
        return this.backingMap.keySet().remove(key);
    }

    public boolean containsAll(Collection c)
    {
    	for (Object o : c) {
    		if (!this.contains(o))
    			return false;
    	}
    	return true;
    }

    public boolean addAll(Collection<? extends V> c)
    {
        boolean changed = false;
        
        for (V v : c) {
        	changed |= this.add(v);
        }
        return changed;
    }

    public boolean removeAll(Collection c)
    {
        boolean changed = false;

        for (Object o : c) {
        	changed |= this.remove(o);
        }
        
        return changed;
    }

    public boolean retainAll(Collection c)
    {
        ArrayList<K> keys = new ArrayList<K>(this.backingMap.size());
        
        for (Object o : c) {
        	keys.add(this.getKey((V)o));
        }
        return this.backingMap.keySet().retainAll(keys);
    }

    public void clear()
    {
        this.backingMap.clear();
    }

    public int hashCode()
    {
        return this.backingMap.hashCode();
    }

    public boolean equals(Object obj)
    {
        return obj instanceof MapCollection ? this.backingMap.equals(((MapCollection)obj).backingMap) : false;
    }

    public String toString()
    {
        return this.backingMap.values().toString();
    }
}
