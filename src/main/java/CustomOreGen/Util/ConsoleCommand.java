package CustomOreGen.Util;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import CustomOreGen.CustomPacketPayload;
import CustomOreGen.CustomPacketPayload.PayloadType;
import CustomOreGen.Config.ConfigParser;
import CustomOreGen.Server.ServerState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.SyntaxErrorException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class ConsoleCommand extends CommandBase
{
    private final Object _obj;
    private final Method _method;

    @Retention(RetentionPolicy.RUNTIME)
    @Target( {ElementType.PARAMETER})
    public @interface ArgName
    {
    	String name() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target( {ElementType.PARAMETER})
    public @interface ArgOptional
    {
    	String defValue() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target( {ElementType.METHOD})
    public @interface CommandDelegate
    {
    	String[] names() default {};
    	
    	boolean isCheat() default true;

    	boolean isDebugging() default true;

    	String desc() default "";
    }

    public static void sendText(ICommandSender recipient, String text)
    {
        if (text != null && recipient != null)
        {
        	for (String line : text.split("\n")) {
        		if (recipient instanceof EntityPlayerMP)
        		{
        			(new CustomPacketPayload(PayloadType.CommandResponse, line)).sendToClient((EntityPlayerMP)recipient);
        		}
	            else
	            {
	            	recipient.addChatMessage(new TextComponentString(line));
	            }
        	}
        }
    }

    public static WorldServer getSenderWorldServer(ICommandSender sender)
    {
        World entityWorld = null;

        if (sender instanceof Entity)
        {
            entityWorld = ((Entity)sender).worldObj;
        }
        else
        {
            if (!(sender instanceof TileEntity))
            {
                return null;
            }

            entityWorld = ((TileEntity)sender).getWorld();
        }

        if (entityWorld == null)
        {
            return null;
        }
        else
        {
        	int dim = entityWorld.provider.getDimension();
            return sender.getServer().worldServerForDimension(dim);
        }
    }

    public ConsoleCommand(Object obj, Method method)
    {
        if ((method.getModifiers() & 8) == 0 && obj == null)
        {
            throw new RuntimeException("Method \'" + method.getName() + "\' for class " + method.getDeclaringClass().getSimpleName() + " requires an object instance");
        }
        else
        {
            this._obj = obj;
            this._method = method;
        }
    }

    public ConsoleCommand(Method method)
    {
        this((Object)null, method);
    }

    @Override
    public String getCommandName()
    {
        CommandDelegate cmdDef = (CommandDelegate)this._method.getAnnotation(CommandDelegate.class);
        return cmdDef != null && cmdDef.names() != null && cmdDef.names().length > 0 ? cmdDef.names()[0] : this._method.getName();
    }

    @Override
    public List<String> getCommandAliases()
    {
        CommandDelegate cmdDef = (CommandDelegate)this._method.getAnnotation(CommandDelegate.class);
        return cmdDef != null && cmdDef.names() != null && cmdDef.names().length > 1 ? Arrays.asList(Arrays.copyOfRange(cmdDef.names(), 1, cmdDef.names().length)) : Collections.<String>emptyList();
    }

    @Override
    public String getCommandUsage(ICommandSender sender)
    {
        return this.getCommandHelp(sender, false);
    }

    public String getCommandHelp(ICommandSender sender, boolean verbose)
    {
        StringBuilder out = new StringBuilder("/" + this.getCommandName());
        Class<?>[] ptypes = this._method.getParameterTypes();
        Annotation[][] pantns = this._method.getParameterAnnotations();

        for (int cmdDef = 0; cmdDef < ptypes.length; ++cmdDef)
        {
            Class<?> clazz = ptypes[cmdDef];
            String name = "arg" + cmdDef + ":" + clazz.getSimpleName();
            boolean required = true;

            if (pantns != null && cmdDef < pantns.length && pantns[cmdDef] != null)
            {
                Annotation[] senderWorld = pantns[cmdDef];   
                for (Annotation annot : senderWorld) {
                	if (annot instanceof ArgName)
                    {
                        name = ((ArgName)annot).name();
                    }
                    else if (annot instanceof ArgOptional)
                    {
                        required = false;
                    }
                }
            }

            if (!clazz.isAssignableFrom(ICommandSender.class))
            {
                if (clazz.isAssignableFrom(WorldServer.class))
                {
                    if (getSenderWorldServer(sender) != null)
                    	continue;
                    
                    clazz = Integer.class;
                }
                else if (clazz.isArray() && cmdDef == ptypes.length - 1 && this._method.isVarArgs())
                {
                    name = " ... ";
                    required = false;
                }

                out.append(' ');

                if (required)
                {
                    out.append('<');
                }
                else
                {
                    out.append('[');
                }

                out.append(name);

                if (required)
                {
                    out.append('>');
                }
                else
                {
                    out.append(']');
                }
            }
        }

        CommandDelegate delegate = (CommandDelegate)this._method.getAnnotation(CommandDelegate.class);

        if (verbose && delegate != null && delegate.desc() != null && !delegate.desc().isEmpty())
        {
            out.append("\n  ");
            out.append(delegate.desc());
        }

        return out.toString();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        Class<?>[] ptypes = this._method.getParameterTypes();
        Annotation[][] pantns = this._method.getParameterAnnotations();
        Object[] pvalues = new Object[ptypes.length];

        try
        {
            int ex = 0;

            for (int pidx = 0; pidx < ptypes.length; ++pidx)
            {
                Class<?> clazz = ptypes[pidx];
                String name = "arg" + pidx + ":" + clazz.getSimpleName();
                String defValue = null;
                boolean required = true;

                if (pantns != null && pidx < pantns.length && pantns[pidx] != null)
                {
                    for (Annotation annot : pantns[pidx]) {
                    	if (annot instanceof ArgName)
                        {
                            name = ((ArgName)annot).name();
                        }
                        else if (annot instanceof ArgOptional)
                        {
                            required = false;
                            defValue = ((ArgOptional)annot).defValue();

                            if (defValue.isEmpty())
                            {
                                defValue = null;
                            }
                        }

                    }
                }

                if (clazz.isAssignableFrom(ICommandSender.class))
                {
                    pvalues[pidx] = sender;
                }
                else if (clazz.isAssignableFrom(WorldServer.class))
                {
                    pvalues[pidx] = getSenderWorldServer(sender);

                    if (pvalues[pidx] == null)
                    {
                    	Integer dimId = ConfigParser.parseString(Integer.class, ex < args.length ? args[ex++] : defValue);
                        pvalues[pidx] = dimId == null ? null : sender.getServer().worldServerForDimension(dimId.intValue());

                        if (pvalues[pidx] == null && required)
                        {
                            throw new SyntaxErrorException("Missing or invalid dimension ID for required argument \'" + name + "\'", new Object[0]);
                        }
                    }
                }
                else if (clazz.isArray() && pidx == ptypes.length - 1 && this._method.isVarArgs())
                {
                    pvalues[pidx] = Array.newInstance(clazz.getComponentType(), args.length - ex);

                    for (int i = 0; i < Array.getLength(pvalues[pidx]); ++i)
                    {
                        Object pvalue = ConfigParser.parseString(clazz.getComponentType(), args[ex++]);
                        Array.set(pvalues[pidx], i, pvalue);
                    }
                }
                else
                {
                    pvalues[pidx] = ConfigParser.parseString(clazz, ex < args.length ? args[ex++] : defValue);

                    if (pvalues[pidx] == null && required)
                    {
                        throw new SyntaxErrorException("Missing required argument \'" + name + "\'", new Object[0]);
                    }
                }
            }

            if (ex < args.length)
            {
                throw new SyntaxErrorException("Too many arguments", new Object[0]);
            }
        }
        catch (IllegalArgumentException var18)
        {
            throw new SyntaxErrorException(var18.getMessage(), new Object[0]);
        }

        try
        {
            Object result = this._method.invoke(this._obj, pvalues);

            if (result != null)
            {
                sendText(sender, result.toString());
            }
        }
        catch (InvocationTargetException var16)
        {
            throw new CommandException("Error: " + var16.getCause().getMessage(), new Object[0]);
        }
        catch (Exception var17)
        {
            throw new CommandException("Unkown Error: " + var17.getMessage(), new Object[0]);
        }
    }
    
	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
		CommandDelegate cmdDef = (CommandDelegate)this._method.getAnnotation(CommandDelegate.class);

        if (cmdDef == null || cmdDef.isDebugging())
        {
            WorldServer senderWorld = getSenderWorldServer(sender);

            if (senderWorld != null)
            {
                ServerState.checkIfServerChanged(server, senderWorld.getWorldInfo());

                if (!ServerState.getWorldConfig(senderWorld).debuggingMode)
                {
                    return false;
                }
            }
        }

        return super.checkPermission(server, sender);
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        CommandDelegate cmdDef = (CommandDelegate)this._method.getAnnotation(CommandDelegate.class);
        return cmdDef != null && cmdDef.isCheat() ? 2 : 0;
    }
}
