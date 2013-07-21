package CustomOreGen.Util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

public class SimpleProfiler
{
    public static final SimpleProfiler globalProfiler = new SimpleProfiler();
    private int _sectionCount = 0;
    private Section[] _sections = new Section[32];
    private int _openCount = 0;
    private Section[] _openSections = new Section[32];

    private int getSlot(Object key)
    {
        int slot;

        for (slot = Math.abs(key.hashCode()) % this._sections.length; this._sections[slot] != null && this._sections[slot].key != key; slot = (slot + 1) % this._sections.length)
        {
            ;
        }

        return slot;
    }

    private Section getSection(Object key)
    {
        if (this._sectionCount * 4 > this._sections.length)
        {
            Section[] slot = this._sections;
            this._sections = new Section[slot.length * 2];
            
            for (Section s : slot) {
            	if (s != null)
                {
                    s.slot = this.getSlot(s.key);
                    this._sections[s.slot] = s;
                }	
            }
        }

        int var7 = this.getSlot(key);

        if (this._sections[var7] == null)
        {
            this._sections[var7] = new Section(key, var7, ++this._sectionCount);
        }

        return this._sections[var7];
    }

    public void startSection(Object key)
    {
        long time = System.nanoTime();
        Section section;

        if (this._openCount > 0)
        {
            section = this._openSections[this._openCount - 1];

            if (section.running > 0 && --section.running == 0)
            {
                section.runTime += time;
            }
        }

        section = this.getSection(key);
        ++section.hits;

        if (this._openCount >= this._openSections.length)
        {
            this._openSections = (Section[])Arrays.copyOf(this._openSections, this._openCount * 2);
        }

        this._openSections[this._openCount++] = section;

        if (section.running++ == 0)
        {
            section.runTime -= time;
        }

        if (section.open++ == 0)
        {
            section.openTime -= time;
        }
    }

    public void pauseSection()
    {
        if (this._openCount > 0)
        {
            Section current = this._openSections[this._openCount - 1];

            if (current.running > 0 && --current.running == 0)
            {
                current.runTime += System.nanoTime();
            }
        }
    }

    public void unpauseSection()
    {
        if (this._openCount > 0)
        {
            Section current = this._openSections[this._openCount - 1];

            if (current.running++ == 0)
            {
                current.runTime -= System.nanoTime();
            }
        }
    }

    public void endSection()
    {
        long time = System.nanoTime();

        if (this._openCount <= 0)
        {
            throw new RuntimeException("Open/Close section mismatch.");
        }
        else
        {
            Section section = this._openSections[--this._openCount];

            if (--section.running == 0)
            {
                section.runTime += time;
            }

            if (--section.open == 0)
            {
                section.openTime += time;
            }

            if (this._openCount > 0)
            {
                Section current = this._openSections[this._openCount - 1];

                if (current.running++ == 0)
                {
                    current.runTime -= time;
                }
            }
        }
    }

    public void dumpSections(int sortBy)
    {
        double maxOpentime = 0.0D;
        Vector sections = new Vector(this._sectionCount);
        
        for (Section section : this._sections) {	
            if (section != null)
            {
                if ((double)section.openTime > maxOpentime)
                {
                    maxOpentime = (double)section.openTime;
                }

                sections.add(section);
            }
        }

        Section.sortField = sortBy;
        Collections.sort(sections);
        System.out.println("Open Sections (" + this._openCount + ") :");

        
        for (int i = 0; i < this._openCount; ++i)
        {
            System.out.println("  " + this._openSections[i].key);
        }

        System.out.format("%40s    %8s          %21s               %21s       \n", new Object[] {"Key", "Hits", "Run Time (us)", "Open Time (us)"});
        Iterator var7 = sections.iterator();

        while (var7.hasNext())
        {
            Section s = (Section)var7.next();
            System.out.format("%40s =  %8d  %12d (%8.0f) (%6.2f%%)  %12d (%8.0f) (%6.2f%%)\n", new Object[] {s.key, Integer.valueOf(s.hits), Long.valueOf(s.runTime / 1000L), Float.valueOf((float)s.runTime / 1000.0F / (float)s.hits), Double.valueOf(100.0D * (double)s.runTime / maxOpentime), Long.valueOf(s.openTime / 1000L), Float.valueOf((float)s.openTime / 1000.0F / (float)s.hits), Double.valueOf(100.0D * (double)s.openTime / maxOpentime)});
        }
    }

    public void clear()
    {
        this._sectionCount = 0;
        Arrays.fill(this._sections, (Object)null);
        this._openCount = 0;
        Arrays.fill(this._openSections, (Object)null);
    }
    
    private static class Section implements Comparable
    {
        public final Object key;
        public int slot;
        public int order;
        public int hits;
        public int running;
        public long runTime;
        public int open;
        public long openTime;
        public static int sortField = 2;

        private Section(Object key, int slot, int order)
        {
            this.key = key;
            this.slot = slot;
            this.order = order;
        }

        public int compareTo(Section compareTo)
        {
            int mult = (int)Math.signum((float)sortField);

            switch (Math.abs(sortField))
            {
                case 1:
                    return mult * Double.compare((double)this.runTime, (double)compareTo.runTime);

                case 2:
                    return mult * Double.compare((double)this.openTime, (double)compareTo.openTime);

                case 3:
                    return mult * Double.compare((double)this.hits, (double)compareTo.hits);

                case 4:
                    return mult * Double.compare((double)this.order, (double)compareTo.order);

                default:
                    return 0;
            }
        }

        public int compareTo(Object x0)
        {
            return this.compareTo((Section)x0);
        }
    }

}
