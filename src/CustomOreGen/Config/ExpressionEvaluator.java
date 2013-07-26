package CustomOreGen.Config;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class ExpressionEvaluator
{
    protected static Map _symbolMap = new HashMap();

    protected Object getIdentifierValue(String identifier)
    {
        return null;
    }

    public Object evaluate(String expression) throws EvaluatorException
    {
        Stack input = this.parse(expression);

        if (input.isEmpty())
        {
            return null;
        }
        else
        {
            Object result = null;

            try
            {
                result = evaluate(input);
            }
            catch (EmptyStackException var5)
            {
                throw new EvaluatorException("Incomplete expression.", new Token(expression, expression.length(), expression.length()));
            }

            if (!input.isEmpty())
            {
                throw new EvaluatorException("Expression contains too many values.", (Token)input.peek());
            }
            else
            {
                return result;
            }
        }
    }

    protected static Object evaluate(Stack input) throws EmptyStackException, EvaluatorException
    {
        Token token = (Token)input.pop();

        if (!token.type.retain)
        {
            throw new EvaluatorException("Un-evaluatable token.", token);
        }
        else
        {
            boolean rhs;
            boolean lhs;
            Object rhs1;
            double rhs2;
            Object lhs1;
            String lhs2;
            String rhs3;

            switch (token.type)
            {
                case SCIENTIFIC:
                    rhs2 = evaluateNumber(input);
                    return Double.valueOf(evaluateNumber(input) * Math.pow(10.0D, rhs2));

                case TO_STRING:
                    return evaluate(input).toString();

                case TO_NUMBER:
                    rhs1 = evaluate(input);

                    if (rhs1 instanceof Number)
                    {
                        return (Number)rhs1;
                    }
                    else if (rhs1 instanceof Boolean)
                    {
                        return Long.valueOf(((Boolean)rhs1).booleanValue() ? 1L : 0L);
                    }
                    else
                    {
                        if (rhs1 instanceof String)
                        {
                            try
                            {
                                if (rhs1.toString().contains("."))
                                {
                                    return Double.valueOf(Double.parseDouble(rhs1.toString()));
                                }

                                return Long.decode(rhs1.toString());
                            }
                            catch (NumberFormatException var5)
                            {
                                ;
                            }
                        }

                        throw new EvaluatorException("Cannot reduce \'" + rhs1 + "\' to a numerical value.", token);
                    }

                case TO_BOOLEAN:
                    rhs1 = evaluate(input);

                    if (rhs1 instanceof Boolean)
                    {
                        return (Boolean)rhs1;
                    }
                    else if (rhs1 instanceof Number)
                    {
                        return Boolean.valueOf(((Number)rhs1).doubleValue() != 0.0D);
                    }
                    else
                    {
                        if (rhs1 instanceof String)
                        {
                            if (rhs1.toString().equalsIgnoreCase("true"))
                            {
                                return Boolean.TRUE;
                            }

                            if (rhs1.toString().equalsIgnoreCase("false"))
                            {
                                return Boolean.FALSE;
                            }
                        }

                        throw new EvaluatorException("Cannot reduce \'" + rhs1 + "\' to a boolean value.", token);
                    }

                case LOGICAL_NOT:
                    return Boolean.valueOf(!evaluateBoolean(input));

                case UNARY_MINUS:
                    return Double.valueOf(-evaluateNumber(input));

                case UNARY_PLUS:
                    return Double.valueOf(evaluateNumber(input));

                case IF:
                    rhs1 = evaluate(input);
                    lhs1 = evaluate(input);
                    return evaluateBoolean(input) ? lhs1 : rhs1;

                case MATCH:
                    rhs3 = evaluateString(input);
                    lhs2 = evaluateString(input);
                    return Boolean.valueOf(lhs2.matches(rhs3));

                case REPLACE:
                    rhs3 = evaluateString(input);
                    lhs2 = evaluateString(input);
                    String equal1 = evaluateString(input);
                    return equal1.replaceAll(lhs2, rhs3);

                case MULTIPLY:
                    return Double.valueOf(evaluateNumber(input) * evaluateNumber(input));

                case DIVIDE:
                    rhs2 = evaluateNumber(input);
                    return Double.valueOf(evaluateNumber(input) / rhs2);

                case MODULUS:
                    rhs2 = evaluateNumber(input);
                    return Double.valueOf(evaluateNumber(input) % rhs2);

                case ADD:
                    return Double.valueOf(evaluateNumber(input) + evaluateNumber(input));

                case SUBTRACT:
                    rhs2 = evaluateNumber(input);
                    return Double.valueOf(evaluateNumber(input) - rhs2);

                case CONCATENATE:
                    rhs3 = evaluateString(input);
                    lhs2 = evaluateString(input);
                    return lhs2 + rhs3;

                case LESS:
                    rhs2 = evaluateNumber(input);
                    return Boolean.valueOf(evaluateNumber(input) < rhs2);

                case LESS_EQUAL:
                    rhs2 = evaluateNumber(input);
                    return Boolean.valueOf(evaluateNumber(input) <= rhs2);
                case GREATER:

                    rhs2 = evaluateNumber(input);
                    return Boolean.valueOf(evaluateNumber(input) > rhs2);

                case GREATER_EQUAL:
                    rhs2 = evaluateNumber(input);
                    return Boolean.valueOf(evaluateNumber(input) >= rhs2);

                case EQUAL:
                case NOT_EQUAL:
                    rhs1 = evaluate(input);
                    lhs1 = evaluate(input);
                    boolean equal = false;

                    if (lhs1 instanceof Number && rhs1 instanceof Number)
                    {
                        equal = ((Number)lhs1).doubleValue() == ((Number)rhs1).doubleValue();
                    }
                    else
                    {
                        if (!lhs1.getClass().isAssignableFrom(rhs1.getClass()))
                        {
                            throw new EvaluatorException("Cannot compare equality of \'" + lhs1.getClass().getSimpleName() + "\' and \'" + rhs1.getClass().getSimpleName() + "\'.", token);
                        }

                        equal = rhs1.equals(lhs1);
                    }

                    return Boolean.valueOf(token.type == TokenType.EQUAL ? equal : !equal);

                case AND:
                    rhs = evaluateBoolean(input);
                    lhs = evaluateBoolean(input);
                    return Boolean.valueOf(lhs && rhs);

                case OR:
                    rhs = evaluateBoolean(input);
                    lhs = evaluateBoolean(input);
                    return Boolean.valueOf(lhs || rhs);

                default:
                    if (token.data == null)
                    {
                        throw new EvaluatorException("Un-evaluatable token.", token);
                    }
                    else if (token.data instanceof EvaluationDelegate)
                    {
                        return ((EvaluationDelegate)token.data).evaluate(token, input);
                    }
                    else
                    {
                        return token.data;
                    }
            }
        }
    }

    protected static String evaluateString(Stack input) throws EmptyStackException, EvaluatorException
    {
        Token token = (Token)input.peek();
        Object value = evaluate(input);

        try
        {
            return (String)value;
        }
        catch (ClassCastException var4)
        {
            throw new EvaluatorException("Expected a string value.", token, var4);
        }
    }

    protected static double evaluateNumber(Stack input) throws EmptyStackException, EvaluatorException
    {
        Token token = (Token)input.peek();
        Object value = evaluate(input);

        try
        {
            return ((Number)value).doubleValue();
        }
        catch (ClassCastException var4)
        {
            throw new EvaluatorException("Expected a numeric value.", token, var4);
        }
    }

    protected static boolean evaluateBoolean(Stack input) throws EmptyStackException, EvaluatorException
    {
        Token token = (Token)input.peek();
        Object value = evaluate(input);

        try
        {
            return ((Boolean)value).booleanValue();
        }
        catch (ClassCastException var4)
        {
            throw new EvaluatorException("Expected a boolean value.", token, var4);
        }
    }

    public Stack parse(String expression) throws EvaluatorException
    {
        Stack output = new Stack();
        Stack held = new Stack();
        Token lastToken = null;
        Assoc lastAssoc = Assoc.RIGHT;
        Token heldTop;

        for (heldTop = this.getToken(expression, 0); heldTop != null; heldTop = this.getToken(expression, heldTop.end))
        {
            Assoc assoc = heldTop.type.associativity;

            if (assoc == Assoc.BRACKET_OPEN)
            {
                assoc = Assoc.RIGHT;
            }
            else if (assoc == Assoc.BRACKET_CLOSE)
            {
                assoc = Assoc.LEFT;
            }

            if (assoc == Assoc.LEFT != (lastAssoc == Assoc.NONE))
            {
                if (heldTop.type == TokenType.ADD)
                {
                    heldTop = new Token(heldTop.source, heldTop.start, heldTop.end, TokenType.UNARY_PLUS);
                }
                else
                {
                    if (heldTop.type != TokenType.SUBTRACT)
                    {
                        throw new EvaluatorException("\'" + heldTop + "\' cannot follow \'" + lastToken + "\'.", heldTop);
                    }

                    heldTop = new Token(heldTop.source, heldTop.start, heldTop.end, TokenType.UNARY_MINUS);
                }
            }

            lastAssoc = assoc;
            lastToken = heldTop;
            Token heldTop1;

            switch (heldTop.type.associativity)
            {
                case NONE:
                    if (heldTop.type.retain)
                    {
                        output.push(heldTop);
                    }

                    break;

                case BRACKET_OPEN:
                    held.push(heldTop);
                    break;

                case BRACKET_CLOSE:
                    heldTop1 = null;

                    while (heldTop1 == null && !held.isEmpty())
                    {
                        Token heldTop2 = (Token)held.pop();

                        if (heldTop2.type.associativity == Assoc.BRACKET_OPEN)
                        {
                            if (!heldTop2.toString().equals(heldTop.data))
                            {
                                throw new EvaluatorException("Missing corresponding " + heldTop2.data + ".", heldTop2);
                            }

                            heldTop1 = heldTop2;
                        }
                        else if (heldTop2.type.retain)
                        {
                            output.push(heldTop2);
                        }
                    }

                    if (heldTop1 == null)
                    {
                        throw new EvaluatorException("Missing corresponding " + heldTop.data + ".", heldTop);
                    }

                    if (heldTop.type.retain)
                    {
                        output.push(heldTop1);
                        output.push(heldTop);
                    }

                    lastAssoc = Assoc.NONE;
                    break;

                case LEFT:
                case RIGHT:
                    while (!held.isEmpty())
                    {
                        heldTop1 = (Token)held.peek();

                        if (heldTop1.type.associativity != Assoc.LEFT && heldTop1.type.associativity != Assoc.RIGHT || heldTop.type.associativity == Assoc.RIGHT && heldTop.type.precedence == heldTop1.type.precedence || heldTop.type.precedence < heldTop1.type.precedence)
                        {
                            break;
                        }

                        held.pop();

                        if (heldTop1.type.retain)
                        {
                            output.push(heldTop1);
                        }
                    }

                    held.push(heldTop);
            }
        }

        if (!output.isEmpty() && lastAssoc != Assoc.NONE)
        {
            throw new EvaluatorException("Incomplete expression.", new Token(expression, expression.length(), expression.length()));
        }
        else
        {
            while (!held.isEmpty())
            {
                heldTop = (Token)held.pop();

                if (heldTop.type.associativity == Assoc.BRACKET_OPEN)
                {
                    throw new EvaluatorException("Missing corresponding " + heldTop.data + ".", heldTop);
                }

                if (heldTop.type.retain)
                {
                    output.push(heldTop);
                }
            }

            return output;
        }
    }

    public Token getToken(String input, int start) throws EvaluatorException
    {
        if (input != null && !input.isEmpty())
        {
            while (input.length() > start && Character.isWhitespace(input.charAt(start)))
            {
                ++start;
            }

            if (input.length() <= start)
            {
                return null;
            }
            else
            {
                char ch = input.charAt(start);
                int end = start + 1;

                if (Character.isDigit(ch))
                {
                    boolean var9 = false;
                    char ex;

                    if (ch == 48 && input.length() > end && Character.toLowerCase(input.charAt(end)) == 120)
                    {
                        ++end;

                        while (input.length() > end)
                        {
                            ex = input.charAt(end);

                            if (!Character.isDigit(ex) && (ex < 97 || ex > 102) && (ex < 65 || ex > 70))
                            {
                                break;
                            }

                            ++end;
                        }
                    }
                    else
                    {
                        while (input.length() > end)
                        {
                            ex = input.charAt(end);

                            if (Character.isDigit(ex))
                            {
                                ++end;
                            }
                            else
                            {
                                if (ex != 46)
                                {
                                    break;
                                }

                                ++end;
                                var9 = true;
                            }
                        }
                    }

                    try
                    {
                        return var9 ? new Token(input, start, end, TokenType.NUMBER, Double.valueOf(Double.parseDouble(input.substring(start, end)))) : new Token(input, start, end, TokenType.NUMBER, Long.decode(input.substring(start, end)));
                    }
                    catch (NumberFormatException var7)
                    {
                        throw new EvaluatorException("Invalid number.", new Token(input, start, end));
                    }
                }
                else
                {
                    if (!Character.isLetter(ch) && ch != 95)
                    {
                        if (ch == 34 || ch == 39)
                        {
                            end = input.indexOf(ch, end);

                            if (end < 0)
                            {
                                throw new EvaluatorException("Unmatched string delimeter " + ch + ".", new Token(input, start, start + 1));
                            }

                            ++end;
                            return new Token(input, start, end, TokenType.STRING, input.substring(start + 1, end - 1));
                        }

                        if ((ch == 60 || ch == 62 || ch == 33) && input.length() > end && input.charAt(end) == 61)
                        {
                            ++end;
                        }
                    }
                    else
                    {
                        while (input.length() > end)
                        {
                            char type = input.charAt(end);

                            if (!Character.isLetterOrDigit(type) && type != 95 && type != 46)
                            {
                                break;
                            }

                            ++end;
                        }

                        Object var8 = this.getIdentifierValue(input.substring(start, end));

                        if (var8 != null)
                        {
                            if (var8 instanceof EvaluationDelegate && ((EvaluationDelegate)var8).explicitArguments() > 0)
                            {
                                return new Token(input, start, end, TokenType.FUNCTION, var8);
                            }

                            return new Token(input, start, end, TokenType.VARIABLE, var8);
                        }
                    }

                    TokenType var10 = (TokenType)_symbolMap.get(input.substring(start, end).toLowerCase());

                    if (var10 != null)
                    {
                        return new Token(input, start, end, var10);
                    }
                    else
                    {
                        throw new EvaluatorException("Unexpected token.", new Token(input, start, end));
                    }
                }
            }
        }
        else
        {
            return null;
        }
    }

    static
    {
    	for (TokenType type : TokenType.values()) {
        	if (type.symbol != null)
            {
                _symbolMap.put(type.symbol, type);
            }
        }
    }
    
    public static enum TokenType
    {
        TRUE("true", 0, Assoc.NONE, true, Boolean.valueOf(true)),
        FALSE("false", 0, Assoc.NONE, true, Boolean.valueOf(false)),
        NUMBER((String)null, 0, Assoc.NONE, true, (Object)null),
        HEXNUMBER((String)null, 0, Assoc.NONE, true, (Object)null),
        STRING((String)null, 0, Assoc.NONE, true, (Object)null),
        VARIABLE((String)null, 0, Assoc.NONE, true, (Object)null),
        PAREN_OPEN("(", 10, Assoc.BRACKET_OPEN, false, ")"),
        PAREN_CLOSE(")", 10, Assoc.BRACKET_CLOSE, false, "("),
        BRACKET_OPEN("[", 10, Assoc.BRACKET_OPEN, false, "]"),
        BRACKET_CLOSE("]", 10, Assoc.BRACKET_CLOSE, false, "["),
        BRACE_OPEN("{", 10, Assoc.BRACKET_OPEN, false, "}"),
        BRACE_CLOSE("}", 10, Assoc.BRACKET_CLOSE, false, "{"),
        EXPONENT("^", 20, Assoc.LEFT, true, new EvaluationDelegate(false, Math.class, "pow", new Class[]{Double.TYPE, Double.TYPE})),
        SCIENTIFIC("e", 20, Assoc.LEFT, true, (Object)null),
        TO_STRING("$", 20, Assoc.RIGHT, true, (Object)null),
        TO_NUMBER("#", 20, Assoc.RIGHT, true, (Object)null),
        TO_BOOLEAN("?", 20, Assoc.RIGHT, true, (Object)null),
        LOGICAL_NOT("!", 20, Assoc.RIGHT, true, (Object)null),
        UNARY_MINUS((String)null, 20, Assoc.RIGHT, true, (Object)null),
        UNARY_PLUS((String)null, 20, Assoc.RIGHT, true, (Object)null),
        IF("if", 20, Assoc.RIGHT, true, (Object)null),
        ABS("abs", 20, Assoc.RIGHT, true, new EvaluationDelegate(false, Math.class, "abs", new Class[]{Double.TYPE})),
        SIGN("sign", 20, Assoc.RIGHT, true, new EvaluationDelegate(false, Math.class, "signum", new Class[]{Double.TYPE})),
        MIN("min", 20, Assoc.RIGHT, true, new EvaluationDelegate(false, Math.class, "min", new Class[]{Double.TYPE, Double.TYPE})),
        MAX("max", 20, Assoc.RIGHT, true, new EvaluationDelegate(false, Math.class, "max", new Class[]{Double.TYPE, Double.TYPE})),
        FLOOR("floor", 20, Assoc.RIGHT, true, new EvaluationDelegate(false, Math.class, "floor", new Class[]{Double.TYPE})),
        CEILING("ceil", 20, Assoc.RIGHT, true, new EvaluationDelegate(false, Math.class, "ceil", new Class[]{Double.TYPE})),
        ROUND("round", 20, Assoc.RIGHT, true, new EvaluationDelegate(false, Math.class, "round", new Class[]{Double.TYPE})),
        EXP("exp", 20, Assoc.RIGHT, true, new EvaluationDelegate(false, Math.class, "exp", new Class[]{Double.TYPE})),
        LOG("log", 20, Assoc.RIGHT, true, new EvaluationDelegate(false, Math.class, "log", new Class[]{Double.TYPE})),
        SINE("sin", 20, Assoc.RIGHT, true, new EvaluationDelegate(false, Math.class, "sin", new Class[]{Double.TYPE})),
        COSINE("cos", 20, Assoc.RIGHT, true, new EvaluationDelegate(false, Math.class, "cos", new Class[]{Double.TYPE})),
        TANGENT("tan", 20, Assoc.RIGHT, true, new EvaluationDelegate(false, Math.class, "tan", new Class[]{Double.TYPE})),
        ARCSINE("asin", 20, Assoc.RIGHT, true, new EvaluationDelegate(false, Math.class, "asin", new Class[]{Double.TYPE})),
        ARCCOSINE("acos", 20, Assoc.RIGHT, true, new EvaluationDelegate(false, Math.class, "acos", new Class[]{Double.TYPE})),
        ARCTANGENT("atan", 20, Assoc.RIGHT, true, new EvaluationDelegate(false, Math.class, "atan", new Class[]{Double.TYPE})),
        MATCH("match", 20, Assoc.RIGHT, true, (Object)null),
        REPLACE("replace", 20, Assoc.RIGHT, true, (Object)null),
        FUNCTION((String)null, 20, Assoc.RIGHT, true, (Object)null),
        MULTIPLY("*", 30, Assoc.LEFT, true, (Object)null),
        DIVIDE("/", 30, Assoc.LEFT, true, (Object)null),
        MODULUS("%", 30, Assoc.LEFT, true, (Object)null),
        ADD("+", 40, Assoc.LEFT, true, (Object)null),
        SUBTRACT("-", 40, Assoc.LEFT, true, (Object)null),
        CONCATENATE("~", 40, Assoc.LEFT, true, (Object)null),
        LESS("<", 50, Assoc.LEFT, true, (Object)null),
        LESS_EQUAL("<=", 50, Assoc.LEFT, true, (Object)null),
        GREATER(">", 50, Assoc.LEFT, true, (Object)null),
        GREATER_EQUAL(">=", 50, Assoc.LEFT, true, (Object)null),
        EQUAL("=", 60, Assoc.LEFT, true, (Object)null),
        NOT_EQUAL("!=", 60, Assoc.LEFT, true, (Object)null),
        AND("&", 70, Assoc.LEFT, true, (Object)null),
        OR("|", 75, Assoc.LEFT, true, (Object)null),
        COMMA(",", 80, Assoc.LEFT, false, (Object)null),
        SEMICOLON(";", 80, Assoc.LEFT, false, (Object)null),
        COLON(":", 80, Assoc.LEFT, false, (Object)null);
        public final String symbol;
        public final int precedence;
        public final Assoc associativity;
        public final boolean retain;
        public final Object data;

        private TokenType(String symbol, int precedence, Assoc associativity, boolean retain, Object data)
        {
            this.symbol = symbol;
            this.precedence = precedence;
            this.associativity = associativity;
            this.retain = retain;
            this.data = data;
        }
    }

    public static enum Assoc
    {
        NONE,
        LEFT,
        RIGHT,
        BRACKET_OPEN,
        BRACKET_CLOSE;
    }

    public static class EvaluationDelegate
    {
        private final Object _obj;
        private final Method _method;
        private final boolean _passToken;

        public EvaluationDelegate(boolean passToken, Object obj, String methodName, Class ... argTypes)
        {
            this(passToken, obj, obj.getClass(), methodName, argTypes);
        }

        public EvaluationDelegate(boolean passToken, Class clazz, String methodName, Class ... argTypes)
        {
            this(passToken, (Object)null, clazz, methodName, argTypes);
        }

        private EvaluationDelegate(boolean passToken, Object obj, Class clazz, String methodName, Class ... argTypes)
        {
            Method method = null;

            if (argTypes != null)
            {
                try
                {
                    method = clazz.getMethod(methodName, argTypes);
                }
                catch (NoSuchMethodException var11)
                {
                    ;
                }
            }
            else
            {
                Method[] valid = clazz.getMethods();
                for (Method m : valid) {
                	if (m.getName().equals(methodName))
                    {
                        if (method != null)
                        {
                            throw new IllegalArgumentException("Method \'" + methodName + "\' is ambiguous for class " + clazz.getSimpleName());
                        }

                        method = m;
                    }
                }
            }

            if (method == null)
            {
                throw new IllegalArgumentException("Method \'" + methodName + "\' is not defined for class " + clazz.getSimpleName());
            }
            else if ((method.getModifiers() & 8) == 0 && (obj == null || !clazz.isInstance(obj)))
            {
                throw new IllegalArgumentException("Method \'" + methodName + "\' for class " + clazz.getSimpleName() + " requires an object instance");
            }
            else
            {
                if (passToken)
                {
                    boolean var12 = false;

                    if (method.getParameterTypes().length > 0)
                    {
                        Class var13 = method.getParameterTypes()[0];

                        if (var13.isAssignableFrom(String.class) || var13.isAssignableFrom(Token.class))
                        {
                            var12 = true;
                        }
                    }

                    if (!var12)
                    {
                        throw new IllegalArgumentException("Method \'" + methodName + "\' for class " + clazz.getSimpleName() + " must take a String or Token as the first parameter");
                    }
                }

                this._obj = obj;
                this._method = method;
                this._passToken = passToken;
            }
        }

        protected Object evaluate(Token token, Stack argumentStack) throws EmptyStackException, EvaluatorException
        {
            Class[] argTypes = this._method.getParameterTypes();
            Object[] argValues = new Object[argTypes.length];

            for (int ex = argValues.length - 1; ex >= 0; --ex)
            {
                if (ex == 0 && this._passToken)
                {
                    if (argTypes[0].isAssignableFrom(Token.class))
                    {
                        argValues[0] = token;
                    }
                    else
                    {
                        argValues[0] = token.toString();
                    }
                }
                else if (argTypes[ex].isAssignableFrom(String.class))
                {
                    argValues[ex] = ExpressionEvaluator.evaluateString(argumentStack);
                }
                else if (!argTypes[ex].isAssignableFrom(Double.class) && !argTypes[ex].isAssignableFrom(Double.TYPE))
                {
                    if (argTypes[ex].isAssignableFrom(Boolean.class))
                    {
                        argValues[ex] = Boolean.valueOf(ExpressionEvaluator.evaluateBoolean(argumentStack));
                    }
                    else
                    {
                        argValues[ex] = ExpressionEvaluator.evaluate(argumentStack);
                    }
                }
                else
                {
                    argValues[ex] = Double.valueOf(ExpressionEvaluator.evaluateNumber(argumentStack));
                }
            }

            try
            {
                Object var9 = this._method.invoke(this._obj, argValues);

                if (var9 == null)
                {
                    throw new EvaluatorException("Delegate evaluation produced null value.", token);
                }
                else
                {
                    return var9;
                }
            }
            catch (IllegalAccessException var6)
            {
                throw new EvaluatorException("Cannot evaluate delegate.", token, var6);
            }
            catch (IllegalArgumentException var7)
            {
                throw new EvaluatorException(var7.getMessage(), token, var7);
            }
            catch (InvocationTargetException var8)
            {
                throw new EvaluatorException(var8.getCause().getMessage(), token, var8);
            }
        }

        public int explicitArguments()
        {
            return this._method.getParameterTypes().length - (this._passToken ? 1 : 0);
        }

        public String toString()
        {
            String prefix = this._method.getDeclaringClass().getSimpleName() + "." + this._method.getName();
            return this._obj != null ? prefix + "(" + this._obj + ")" : prefix;
        }
    }

    public static class EvaluatorException extends Exception
    {
        public final Token token;

        public EvaluatorException(String message, Token token, Throwable cause)
        {
            super(message, cause);
            this.token = token;
        }

        public EvaluatorException(String message, Token token)
        {
            super(message);
            this.token = token;
        }

        public EvaluatorException(String message, Throwable cause)
        {
            this(message, (Token)null, cause);
        }

        public EvaluatorException(String message)
        {
            this(message, (Token)null);
        }

        public String getMessage()
        {
            if (this.token == null)
            {
                return super.getMessage();
            }
            else
            {
                String name = this.token.type == null ? "" : this.token.type.name();
                return String.format("At %s(%d-%d) %s : %s", new Object[] {name, Integer.valueOf(this.token.start), Integer.valueOf(this.token.end), this.token, super.getMessage()});
            }
        }

        public String toString()
        {
            StringBuilder msg = new StringBuilder(this.getMessage());

            if (this.token != null && this.token.source != null && !this.token.source.isEmpty())
            {
                int quoteStart = (this.token.start + this.token.end - 60) / 2;
                String quotePrefix = "... ";

                if (quoteStart < 0)
                {
                    quotePrefix = "    ";
                    quoteStart = 0;
                }

                int quoteEnd = (this.token.start + this.token.end + 60) / 2;
                String quoteSuffix = " ...";

                if (quoteEnd > this.token.source.length())
                {
                    quoteSuffix = "";
                    quoteEnd = this.token.source.length();
                }

                msg.append("\n  ");
                msg.append(quotePrefix);
                msg.append(this.token.source.substring(quoteStart, quoteEnd).replace("\n", " "));
                msg.append(quoteSuffix);
                msg.append("\n   at ");

                for (int i = quoteStart; i < this.token.end; ++i)
                {
                    if (i != this.token.start && i != this.token.end - 1)
                    {
                        msg.append('-');
                    }
                    else
                    {
                        msg.append('^');
                    }
                }
            }

            return msg.toString();
        }
    }

    public static class Token
    {
        public final String source;
        public final int start;
        public final int end;
        public final TokenType type;
        public final Object data;
        private String _str;

        public String toString()
        {
            if (this._str == null)
            {
                this._str = this.source.substring(this.start, this.end);
            }

            return this._str;
        }

        protected Token(String source, int start, int end, TokenType type, Object data)
        {
            this._str = null;
            this.source = source;
            this.start = start;
            this.end = end;
            this.type = type;
            this.data = data;
        }

        protected Token(String source, int start, int end, TokenType type)
        {
            this(source, start, end, type, type.data);
        }

        protected Token(String source, int start, int end)
        {
            this(source, start, end, (TokenType)null, (Object)null);
        }
    }
}

