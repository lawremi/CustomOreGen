package CustomOreGen.Util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class CIStringMap<V> implements Map<String,V>
{
    protected final Map<String,V> backingMap;
    protected final Map<String,String> keyMap;

    public CIStringMap(Map<String,V> backingMap)
    {
        this.backingMap = backingMap;
        this.keyMap = new HashMap();
        
        for (Entry<String,V> entry : backingMap.entrySet()) {
        	
            String key = entry.getKey();
            String ukey = this.uniformKey(key);

            if (this.keyMap.containsKey(ukey))
            {
                throw new IllegalArgumentException("Backing set contains duplicate key \'" + key + "\'");
            }

            this.keyMap.put(ukey, key);
        }
    }

    public CIStringMap()
    {
        this.backingMap = new HashMap();
        this.keyMap = new HashMap();
    }

    public int size()
    {
        return this.backingMap.size();
    }

    public boolean isEmpty()
    {
        return this.backingMap.isEmpty();
    }

    public boolean containsKey(Object key)
    {
        if (key != null)
        {
            String ukey = this.uniformKey((String)key);
            key = this.keyMap.get(ukey);

            if (key == null)
            {
                return false;
            }
        }

        return this.backingMap.containsKey(key);
    }

    public String getCanonicalKey(String key)
    {
        String ukey = this.uniformKey(key);
        return (String)this.keyMap.get(ukey);
    }

    public boolean containsValue(Object value)
    {
        return this.backingMap.containsValue(value);
    }

    public V get(Object key)
    {
        if (key != null)
        {
            String ukey = this.uniformKey((String)key);
            key = this.keyMap.get(ukey);

            if (key == null)
            {
                return null;
            }
        }

        return this.backingMap.get(key);
    }

    public V put(String key, V value)
    {
        if (key != null)
        {
            String ukey = this.uniformKey(key);
            String oldKey = this.keyMap.get(ukey);
            this.keyMap.put(ukey, key);

            if (oldKey != null)
            {
                V oldValue = this.backingMap.remove(oldKey);
                this.backingMap.put(key, value);
                return oldValue;
            }
        }

        return this.backingMap.put(key, value);
    }

    public V remove(Object key)
    {
        if (key != null)
        {
            String ukey = this.uniformKey((String)key);
            key = this.keyMap.remove(ukey);

            if (key == null)
            {
                return null;
            }
        }

        return this.backingMap.remove(key);
    }

    public void putAll(Map<? extends String,? extends V> map)
    {
    	for (Entry<? extends String, ? extends V> entry : map.entrySet()) {
    		this.put(entry.getKey(), entry.getValue());
        }
    }

    public void clear()
    {
        this.keyMap.clear();
        this.backingMap.clear();
    }

    public Set<String> keySet()
    {
        return this.backingMap.keySet();
    }

    public Collection<V> values()
    {
        return this.backingMap.values();
    }

    public Set<Entry<String,V>> entrySet()
    {
        return this.backingMap.entrySet();
    }

    public int hashCode()
    {
        return this.backingMap.hashCode();
    }

    public boolean equals(Object obj)
    {
        return obj instanceof CIStringMap ? this.backingMap.equals(((CIStringMap)obj).backingMap) : false;
    }

    public String toString()
    {
        return this.backingMap.toString();
    }

    protected String uniformKey(String rawKey)
    {
        return rawKey == null ? null : rawKey.toLowerCase();
    }
}
