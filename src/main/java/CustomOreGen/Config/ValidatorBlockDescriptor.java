package CustomOreGen.Config;

import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;

import org.w3c.dom.Node;

public class ValidatorBlockDescriptor extends ValidatorNode
{
    public String blocks = null;
    public float weight = 1.0F;
	public NBTTagCompound nbt;
	
    protected ValidatorBlockDescriptor(ValidatorNode parent, Node node)
    {
        super(parent, node);
    }

    protected boolean validateChildren() throws ParserException
    {
        super.validateChildren();
        this.blocks = this.validateRequiredAttribute(String.class, "Block", true);
        this.weight = this.validateNamedAttribute(Float.class, "Weight", this.weight, true);
        String nbtJson = this.validateNamedAttribute(String.class, "NBT", null, true);
        if (nbtJson != null) {
        	try {
        		NBTBase base = JsonToNBT.func_150315_a(nbtJson);
        		if (base instanceof NBTTagCompound) {
        			this.nbt = (NBTTagCompound)base;
        		} else {
        			throw new ParserException("NBT is not a compound tag");
        		}
			} catch (NBTException e) {
				throw new ParserException("Failed to parse JSON", e);
			}
        }
        return true;
    }
    
    public static class Factory implements IValidatorFactory<ValidatorBlockDescriptor>
    {
        public ValidatorBlockDescriptor createValidator(ValidatorNode parent, Node node)
        {
            return new ValidatorBlockDescriptor(parent, node);
        }
    }

}
