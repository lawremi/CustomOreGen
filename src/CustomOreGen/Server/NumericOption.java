package CustomOreGen.Server;

public class NumericOption extends ConfigOption
{
    private double _value = 0.0D;
    private double _nvalue = 0.0D;
    private double _base = 0.0D;
    private double _range = 0.0D;
    private double _displayBase = Double.NaN;
    private double _displayRange = Double.NaN;
    private double _displayIncr = Double.NaN;

    public NumericOption(String name)
    {
        super(name);
    }

    public Object getValue()
    {
        return Double.valueOf(this._value);
    }

    public boolean setValue(Object value)
    {
        double newVal = 0.0D;

        if (value == null)
        {
            return false;
        }
        else
        {
            if (!Double.TYPE.isInstance(value) && !(value instanceof Double))
            {
                if (value instanceof Number)
                {
                    newVal = ((Number)value).doubleValue();
                }
                else
                {
                    if (!(value instanceof String))
                    {
                        return false;
                    }

                    try
                    {
                        newVal = Double.parseDouble((String)value);
                    }
                    catch (NumberFormatException var5)
                    {
                        return false;
                    }
                }
            }
            else
            {
                newVal = ((Double)value).doubleValue();
            }

            if (newVal >= this.getMin() && newVal <= this.getMax())
            {
                this._value = newVal;
                this._nvalue = this._range == 0.0D ? 0.0D : this.round((newVal - this._base) / this._range);
                return true;
            }
            else
            {
                return false;
            }
        }
    }

    public double getMin()
    {
        return this._base;
    }

    public double getMax()
    {
        return this._base + this._range;
    }

    public boolean setLimits(double min, double max)
    {
        if (max < min)
        {
            return false;
        }
        else
        {
            this._base = min;
            this._range = max - min;

            if (this._value < min)
            {
                this._value = min;
                this._nvalue = 0.0D;
            }
            else if (this._value > max)
            {
                this._value = max;
                this._nvalue = 1.0D;
            }

            return true;
        }
    }

    public double getDisplayMin()
    {
        return Double.isNaN(this._displayBase) ? this._base : this._displayBase;
    }

    public double getDisplayMax()
    {
        double range = Double.isNaN(this._displayRange) ? this._range : this._displayRange;
        return this.getDisplayMin() + range;
    }

    public double getDisplayIncr()
    {
        if (Double.isNaN(this._displayIncr))
        {
            double range = Double.isNaN(this._displayRange) ? this._range : this._displayRange;
            return range / 100.0D;
        }
        else
        {
            return this._displayIncr;
        }
    }

    public boolean setDisplayLimits(double min, double max, double incr)
    {
        double r = max - min;

        if (r < 0.0D)
        {
            return false;
        }
        else
        {
            double f = r / incr;

            if (!Double.isInfinite(f) && f >= 1.0D)
            {
                this._displayBase = min;
                this._displayRange = r;
                this._displayIncr = incr;
                return true;
            }
            else
            {
                return false;
            }
        }
    }

    public double getDisplayValue()
    {
        double range = Double.isNaN(this._displayRange) ? this._range : this._displayRange;
        return this.round(this.getDisplayMin() + this.getNormalizedDisplayValue() * range);
    }

    public double getNormalizedDisplayValue()
    {
        double range = Double.isNaN(this._displayRange) ? this._range : this._displayRange;

        if (range == 0.0D)
        {
            return 0.0D;
        }
        else
        {
            double factor = Double.isNaN(this._displayIncr) ? 100.0D : range / this._displayIncr;
            return this.round(this._nvalue, factor);
        }
    }

    public boolean setNormalizedDisplayValue(double value)
    {
        if (value >= 0.0D && value <= 1.0D)
        {
            this._nvalue = value;
            this._nvalue = this.getNormalizedDisplayValue();
            this._value = this.round(this._base + this._nvalue * this._range);
            return true;
        }
        else
        {
            return false;
        }
    }

    private double round(double value)
    {
        return (double)Math.round(value * 1000000.0D) / 1000000.0D;
    }

    private double round(double value, double factor)
    {
        factor = Math.min(Math.abs(factor), 1000000.0D);
        return (double)Math.round(value * factor) / factor;
    }
}
