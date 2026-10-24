package CustomOreGen.Util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import CustomOreGen.Config.ConfigParser;
import CustomOreGen.Server.ConsoleCommands.ArgName;
import CustomOreGen.Server.ConsoleCommands.ArgOptional;
import CustomOreGen.Server.ConsoleCommands.CommandDelegate;
import CustomOreGen.Server.ServerState;
import net.minecraft.ResourceLocationException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/**
 * Exposes one @CommandDelegate method as a Brigadier command: "/name" with an optional greedy string of
 * whitespace-separated arguments, parsed against the method's parameters as the pre-1.13 CommandBase
 * wrapper did (ConfigParser.parseString, @ArgName / @ArgOptional, a trailing varargs parameter).
 *
 * A CommandSourceStack parameter receives the sender. A ServerLevel parameter receives the sender's
 * dimension when the sender is an entity; otherwise (the server console) it consumes an argument naming
 * the dimension, such as minecraft:overworld.
 */
public class ConsoleCommand
{
	private final Object _obj;
    private final Method _method;
    private final CommandDelegate _delegate;

    public ConsoleCommand(Object obj, Method method)
    {
        if ((method.getModifiers() & 8) == 0 && obj == null)
        {
            throw new RuntimeException("Method \'" + method.getName() + "\' for class " + method.getDeclaringClass().getSimpleName() + " requires an object instance");
        }

        this._obj = obj;
        this._method = method;
        this._delegate = method.getAnnotation(CommandDelegate.class);
    }

    public String getName()
    {
        return this._delegate != null && this._delegate.names().length > 0 ? this._delegate.names()[0] : this._method.getName();
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
    	boolean cheat = this._delegate == null || this._delegate.isCheat();

    	for (String name : this.getNames())
    	{
    		LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name)
    				.requires(source -> !cheat || source.hasPermission(2))
    				.executes(ctx -> this.execute(ctx, ""))
    				.then(Commands.argument("args", StringArgumentType.greedyString())
    						.executes(ctx -> this.execute(ctx, StringArgumentType.getString(ctx, "args"))));
    		dispatcher.register(command);
    	}
    }

    private String[] getNames()
    {
    	return this._delegate != null && this._delegate.names().length > 0 ? this._delegate.names() : new String[] { this._method.getName() };
    }

    public String getCommandHelp(CommandSourceStack sender, boolean verbose)
    {
        StringBuilder out = new StringBuilder("/" + this.getName());
        Class<?>[] ptypes = this._method.getParameterTypes();

        for (int pidx = 0; pidx < ptypes.length; ++pidx)
        {
            Class<?> clazz = ptypes[pidx];
            String name = getArgName(pidx);
            boolean required = getArgOptional(pidx) == null;

            if (clazz == CommandSourceStack.class)
            {
            	continue;
            }
            if (clazz == ServerLevel.class && sender.getEntity() != null)
            {
            	continue;
            }
            if (clazz.isArray() && pidx == ptypes.length - 1 && this._method.isVarArgs())
            {
                name = " ... ";
                required = false;
            }

            out.append(required ? " <" : " [").append(name).append(required ? '>' : ']');
        }

        if (verbose && this._delegate != null && !this._delegate.desc().isEmpty())
        {
            out.append("\n  ").append(this._delegate.desc());
        }

        return out.toString();
    }

    private int execute(CommandContext<CommandSourceStack> ctx, String argString)
    {
    	CommandSourceStack sender = ctx.getSource();
    	String trimmed = argString.trim();
    	String[] args = trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
        Class<?>[] ptypes = this._method.getParameterTypes();
        Object[] pvalues = new Object[ptypes.length];
        ServerLevel level = null;

        try
        {
            int argIdx = 0;

            for (int pidx = 0; pidx < ptypes.length; ++pidx)
            {
                Class<?> clazz = ptypes[pidx];
                String name = getArgName(pidx);
                ArgOptional optional = getArgOptional(pidx);
                String defValue = optional == null || optional.defValue().isEmpty() ? null : optional.defValue();

                if (clazz == CommandSourceStack.class)
                {
                    pvalues[pidx] = sender;
                }
                else if (clazz == ServerLevel.class)
                {
                	if (sender.getEntity() != null)
                	{
                		pvalues[pidx] = sender.getLevel();
                	}
                	else
                	{
                		pvalues[pidx] = parseDimension(sender, argIdx < args.length ? args[argIdx++] : defValue);
                	}

                    if (pvalues[pidx] == null && optional == null)
                    {
                        throw new IllegalArgumentException("Missing or invalid dimension (e.g. minecraft:overworld) for required argument \'" + name + "\'");
                    }
                    level = (ServerLevel)pvalues[pidx];
                }
                else if (clazz.isArray() && pidx == ptypes.length - 1 && this._method.isVarArgs())
                {
                    pvalues[pidx] = Array.newInstance(clazz.getComponentType(), args.length - argIdx);

                    for (int i = 0; i < Array.getLength(pvalues[pidx]); ++i)
                    {
                        Array.set(pvalues[pidx], i, ConfigParser.parseString(clazz.getComponentType(), args[argIdx++]));
                    }
                }
                else
                {
                    pvalues[pidx] = ConfigParser.parseString(clazz, argIdx < args.length ? args[argIdx++] : defValue);

                    if (pvalues[pidx] == null)
                    {
                    	if (optional == null)
                    	{
                    		throw new IllegalArgumentException("Missing required argument \'" + name + "\'");
                    	}
                    	// Optional primitives with no default (e.g. boolean flags) get the zero value.
                    	pvalues[pidx] = defaultValue(clazz);
                    }
                }
            }

            if (argIdx < args.length)
            {
                throw new IllegalArgumentException("Too many arguments");
            }
        }
        catch (IllegalArgumentException e)
        {
        	sender.sendFailure(Component.literal(e.getMessage()));
        	sender.sendFailure(Component.literal("Usage: " + this.getCommandHelp(sender, true)));
            return 0;
        }

        if ((this._delegate == null || this._delegate.isDebugging()) && level != null)
        {
        	ServerState.checkIfServerChanged(sender.getServer(), sender.getServer().getWorldData());

        	if (!ServerState.getWorldConfig(level).debuggingMode)
        	{
        		sender.sendFailure(Component.literal("/" + this.getName() + " needs debugging mode; turn it on with /cogEnableDebugging"));
        		return 0;
        	}
        }

        try
        {
            Object result = this._method.invoke(this._obj, pvalues);

            if (result != null)
            {
            	for (String line : result.toString().split("\n"))
            	{
            		sender.sendSuccess(() -> Component.literal(line), false);
            	}
            }
            return 1;
        }
        catch (InvocationTargetException e)
        {
        	Throwable cause = e.getCause() == null ? e : e.getCause();
        	sender.sendFailure(Component.literal("Error: " + cause.getMessage()));
        	return 0;
        }
        catch (Exception e)
        {
        	sender.sendFailure(Component.literal("Unknown Error: " + e.getMessage()));
        	return 0;
        }
    }

    private static ServerLevel parseDimension(CommandSourceStack sender, String value)
    {
    	if (value == null)
    	{
    		return null;
    	}
    	try
    	{
    		return sender.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(value)));
    	}
    	catch (ResourceLocationException e)
    	{
    		return null;
    	}
    }

    private static Object defaultValue(Class<?> clazz)
    {
    	if (!clazz.isPrimitive()) return null;
    	if (clazz == boolean.class) return false;
    	if (clazz == char.class) return '\0';
    	return ConfigParser.parseString(clazz, "0");
    }

    private String getArgName(int pidx)
    {
    	for (Annotation annot : this._method.getParameterAnnotations()[pidx])
    	{
    		if (annot instanceof ArgName argName)
    		{
    			return argName.name();
    		}
    	}
    	return "arg" + pidx + ":" + this._method.getParameterTypes()[pidx].getSimpleName();
    }

    private ArgOptional getArgOptional(int pidx)
    {
    	for (Annotation annot : this._method.getParameterAnnotations()[pidx])
    	{
    		if (annot instanceof ArgOptional optional)
    		{
    			return optional;
    		}
    	}
    	return null;
    }
}
