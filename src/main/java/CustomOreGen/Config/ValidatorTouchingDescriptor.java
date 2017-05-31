package CustomOreGen.Config;

import org.w3c.dom.Node;

import CustomOreGen.Util.TouchingDescriptor.TouchingContactType;
import CustomOreGen.Util.TouchingDescriptor.TouchingDirection;

public class ValidatorTouchingDescriptor extends ValidatorBlockDescriptor {
	private final int MINIMUM_TOUCHES = 0;
	private final int MAXIMUM_TOUCHES = 26;
	
	private final int DEFAULT_MINIMUM_TOUCHES = 1;
	private final int DEFAULT_MAXIMUM_TOUCHES = MAXIMUM_TOUCHES;
	private final TouchingContactType DEFAULT_CONTACT_TYPE = TouchingContactType.Face;
	private final TouchingDirection DEFAULT_DIRECTION = TouchingDirection.Any;
	private final boolean DEFAULT_MANDATORY = false;
	private final boolean DEFAULT_NEGATE = false;
	
	public int minimumTouches = DEFAULT_MINIMUM_TOUCHES;
	public int maximumTouches = DEFAULT_MAXIMUM_TOUCHES;
	public TouchingContactType contactType = DEFAULT_CONTACT_TYPE;
	public TouchingDirection direction = DEFAULT_DIRECTION;
	public boolean mandatory = DEFAULT_MANDATORY;
	public boolean negate = DEFAULT_NEGATE;
	
	protected ValidatorTouchingDescriptor(ValidatorNode parent, Node node) {
		super(parent, node);
	}

	@Override
	protected boolean validateChildren() throws ParserException {
		this.minimumTouches = this.validateNamedAttribute(Integer.class, "minimumTouches", DEFAULT_MINIMUM_TOUCHES, true);
		if (this.minimumTouches < MINIMUM_TOUCHES || this.minimumTouches > MAXIMUM_TOUCHES) {
            throw new ParserException("'minimumTouches' must be between " + Integer.toString(MINIMUM_TOUCHES) + " and " + Integer.toString(MAXIMUM_TOUCHES) + " inclusive.", this.getNode());
		}

		this.maximumTouches = this.validateNamedAttribute(Integer.class, "maximumTouches", DEFAULT_MAXIMUM_TOUCHES, true);
		if (this.maximumTouches < MINIMUM_TOUCHES || this.maximumTouches > MAXIMUM_TOUCHES) {
            throw new ParserException("'maximumTouches' must be between " + Integer.toString(MINIMUM_TOUCHES) + " and " + Integer.toString(MAXIMUM_TOUCHES) + " inclusive.", this.getNode());
		}
		
		if (this.minimumTouches > this.maximumTouches) {
            throw new ParserException("'minimumTouches' must be less than or equal to 'maximumTouches'.", this.getNode());
		}
		
		this.contactType = this.validateNamedAttribute(TouchingContactType.class, "contactType", this.contactType, true);
		this.direction = this.validateNamedAttribute(TouchingDirection.class, "direction", this.direction, true);
		
		this.mandatory = this.validateRequiredAttribute(Boolean.class, "mandatory", true);
		this.negate = this.validateRequiredAttribute(Boolean.class, "negate", true);
		
		return super.validateChildren();
	}
	
    public static class Factory implements IValidatorFactory<ValidatorTouchingDescriptor>
    {
        public ValidatorTouchingDescriptor createValidator(ValidatorNode parent, Node node)
        {
            return new ValidatorTouchingDescriptor(parent, node);
        }
    }	
}
