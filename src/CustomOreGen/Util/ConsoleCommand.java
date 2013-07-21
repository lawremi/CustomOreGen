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
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.SyntaxErrorException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetServerHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatMessageComponent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import CustomOreGen.CustomPacketPayload;
import CustomOreGen.CustomPacketPayload.PayloadType;
import CustomOreGen.Config.ConfigParser;
import CustomOreGen.Server.ServerState;

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
            if (recipient instanceof EntityPlayerMP)
            {
                NetServerHandler handler = ((EntityPlayerMP)recipient).playerNetServerHandler;
                (new CustomPacketPayload(PayloadType.CommandResponse, text)).sendToClient(handler);
            }
            else
            {
                recipient.sendChatToPlayer(ChatMessageComponent.func_111066_d(text));
            }
        }
    }

    public static WorldServer getSenderWorld(ICommandSender sender)
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

            entityWorld = ((TileEntity)sender).worldObj;
        }

        if (entityWorld == null)
        {
            return null;
        }
        else
        {
            int dim = entityWorld.provider.dimensionId;
            return MinecraftServer.getServer().worldServerForDimension(dim);
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

    public String getCommandName()
    {
        CommandDelegate cmdDef = (CommandDelegate)this._method.getAnnotation(CommandDelegate.class);
        return cmdDef != null && cmdDef.names() != null && cmdDef.names().length > 0 ? cmdDef.names()[0] : this._method.getName();
    }

    public List getCommandAliases()
    {
        CommandDelegate cmdDef = (CommandDelegate)this._method.getAnnotation(CommandDelegate.class);
        return cmdDef != null && cmdDef.names() != null && cmdDef.names().length > 1 ? Arrays.asList(Arrays.copyOfRange(cmdDef.names(), 1, cmdDef.names().length)) : null;
    }

    public String getCommandUsage(ICommandSender sender)
    {
        return this.getCommandHelp(sender, false);
    }

    public String getCommandHelp(ICommandSender sender, boolean verbose)
    {
        StringBuilder out = new StringBuilder("/" + this.getCommandName());
        Class[] ptypes = this._method.getParameterTypes();
        Annotation[][] pantns = this._method.getParameterAnnotations();

        for (int cmdDef = 0; cmdDef < ptypes.length; ++cmdDef)
        {
            Class clazz = ptypes[cmdDef];
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
                    WorldServer var15 = getSenderWorld(sender);

                    if (var15 != null)
                    {
                        continue;
                    }

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

        CommandDelegate var14 = (CommandDelegate)this._method.getAnnotation(CommandDelegate.class);

        if (verbose && var14 != null && var14.desc() != null && !var14.desc().isEmpty())
        {
            out.append("\n  ");
            out.append(var14.desc());
        }

        return out.toString();
    }

    public void processCommand(ICommandSender sender, String[] args)
    {
        Class[] ptypes = this._method.getParameterTypes();
        Annotation[][] pantns = this._method.getParameterAnnotations();
        Object[] pvalues = new Object[ptypes.length];

        try
        {
            int ex = 0;

            for (int pidx = 0; pidx < ptypes.length; ++pidx)
            {
                Class clazz = ptypes[pidx];
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
                    pvalues[pidx] = getSenderWorld(sender);

                    if (pvalues[pidx] == null)
                    {
                        Integer var20 = (Integer)ConfigParser.parseString(Integer.class, ex < args.length ? args[ex++] : defValue);
                        pvalues[pidx] = var20 == null ? null : MinecraftServer.getServer().worldServerForDimension(var20.intValue());

                        if (pvalues[pidx] == null && required)
                        {
                            throw new SyntaxErrorException("Missing or invalid dimension ID for required argument \'" + name + "\'", new Object[0]);
                        }
                    }
                }
                else if (clazz.isArray() && pidx == ptypes.length - 1 && this._method.isVarArgs())
                {
                    pvalues[pidx] = Array.newInstance(clazz.getComponentType(), args.length - ex);

                    for (int var21 = 0; var21 < Array.getLength(pvalues[pidx]); ++var21)
                    {
                        Object var22 = ConfigParser.parseString(clazz.getComponentType(), args[ex++]);
                        Array.set(pvalues[pidx], var21, var22);
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
            Object var19 = this._method.invoke(this._obj, pvalues);

            if (var19 != null)
            {
                sendText(sender, var19.toString());
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

    public boolean canCommandSenderUseCommand(ICommandSender sender)
    {
        CommandDelegate cmdDef = (CommandDelegate)this._method.getAnnotation(CommandDelegate.class);

        if (cmdDef == null || cmdDef.isDebugging())
        {
            WorldServer senderWorld = getSenderWorld(sender);

            if (senderWorld != null)
            {
                ServerState.checkIfServerChanged(MinecraftServer.getServer(), senderWorld.getWorldInfo());

                if (!ServerState.getWorldConfig(senderWorld).debuggingMode)
                {
                    return false;
                }
            }
        }

        return super.canCommandSenderUseCommand(sender);
    }

    public int getRequiredPermissionLevel()
    {
        CommandDelegate cmdDef = (CommandDelegate)this._method.getAnnotation(CommandDelegate.class);
        return cmdDef != null && cmdDef.isCheat() ? 2 : 0;
    }
}
