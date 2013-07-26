package CustomOreGen.Server;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetServerHandler;
import net.minecraft.src.ModLoader;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;
import CustomOreGen.CustomOreGenBase;
import CustomOreGen.CustomPacketPayload;
import CustomOreGen.CustomPacketPayload.PayloadType;
import CustomOreGen.Util.BiomeDescriptor;
import CustomOreGen.Util.BlockDescriptor;
import CustomOreGen.Util.ConsoleCommand;
import CustomOreGen.Util.ConsoleCommand.ArgName;
import CustomOreGen.Util.ConsoleCommand.ArgOptional;
import CustomOreGen.Util.ConsoleCommand.CommandDelegate;
import CustomOreGen.Util.PDist;
import CustomOreGen.Util.PDist.Type;

public class ConsoleCommands
{
    public static ConsoleCommands createAndRegister()
    {
        CustomOreGenBase.log.finer("Registering Console command interface ...");
        ConsoleCommands commands = new ConsoleCommands();
        
        for (Method method : ConsoleCommands.class.getMethods()) {
        	if (method.getAnnotation(CommandDelegate.class) != null)
            {
                ModLoader.addCommand(new ConsoleCommand(commands, method));
            }
        }

        return commands;
    }

    private static void resetClientGeometryCache()
    {
        (new CustomPacketPayload(PayloadType.DebuggingGeometryReset, (Serializable)null)).sendToAllClients();
    }

    private static void buildFieldValue(StringBuilder msg, String indent, String name, String desc, Object value)
    {
        msg.append('\n');
        msg.append(indent);
        msg.append(name);
        msg.append(" = ");

        if (value != null)
        {
            msg.append('[');
            msg.append(value.getClass().getSimpleName());
            msg.append("] ");
            msg.append(value);

            if (desc != null)
            {
            	if (value instanceof BlockDescriptor)
                {
                	for (String line : ((BlockDescriptor)value).toDetailedString()) {
                		msg.append('\n');
                        msg.append(indent);
                        msg.append("  ");
                        msg.append(line);
                    }
                }
                else if (value instanceof BiomeDescriptor)
                {
                	for (String line : ((BiomeDescriptor)value).toDetailedString()) {
                        msg.append('\n');
                        msg.append(indent);
                        msg.append("  ");
                        msg.append(line);
                    }
                }
            }
        }
        else
        {
            msg.append("[null]");
        }

        if (desc != null)
        {
            msg.append('\n');
            msg.append(indent);
            msg.append("  ");
            msg.append("\u00a77");
            msg.append(desc);
            msg.append("\u00a7r");
        }
    }

    @CommandDelegate(
            desc = "Dumps a summary of a distribution or a specific distribution setting to the console."
    )
    public String cogInfo(
            @ArgName(
                    name = "dimension"
            ) WorldServer world,
            @ArgName(
                    name = "distribution"
            ) @ArgOptional String distribution,
            @ArgName(
                    name = "setting"
            ) @ArgOptional String setting,
            @ArgName(
                    name = "detail"
            )
            @ArgOptional(
                    defValue = "false"
            ) boolean detail)
    {
        StringBuilder msg = new StringBuilder();
        WorldConfig config = ServerState.getWorldConfig(world);
        Collection allDists = config.getOreDistributions();
        Collection<IOreDistribution> dists = distribution != null && !distribution.isEmpty() ? config.getOreDistributions(distribution) : null;
        msg.append("CustomOreGen [1.4.6]v2 (");

        if (config.world != null)
        {
            msg.append("Dim " + config.world.provider.dimensionId + ", ");
        }

        if (dists != null)
        {
            msg.append(dists.size() + "/");
        }

        msg.append(allDists.size() + " distributions");
        msg.append(')');

        if (dists != null && dists.size() > 0)
        {
            msg.append(':');
            Pattern pattern = setting != null && !setting.isEmpty() ? Pattern.compile(setting, 2) : null;
            
            for (IOreDistribution dist : dists) {
            	msg.append("\n " + dist);

                if (pattern != null)
                {
                    Map<String,Object> descriptions = dist.getDistributionSettings();
                    LinkedHashMap<String,Object> values = new LinkedHashMap();
                    
                    for (Entry<String,Object> entry : descriptions.entrySet()) {
                    	if (pattern.matcher(entry.getKey()).matches())
                        {
                            Object value = dist.getDistributionSetting(entry.getKey());
                            values.put(entry.getKey(), value);
                        }
                    }

                    msg.append(" (" + values.size() + "/" + descriptions.size() + " settings)");

                    if (values.size() > 0)
                    {
                        msg.append(':');
                    }

                    for (Entry<String,Object> entry : values.entrySet()) {
                    	buildFieldValue(msg, "  ", (String)entry.getKey(), detail ? (String)descriptions.get(entry.getKey()) : null, entry.getValue());
                    }
                }
            }
        }

        CustomOreGenBase.log.finer(msg.toString());
        return msg.toString();
    }

    @CommandDelegate(
            desc = "Sets the global wireframe rendering mode.  Omit mode to cycle through modes."
    )
    public void cogWireframeMode(ICommandSender sender,
            @ArgName(
                    name = "None|Polygon|Wireframe|WireframeOverlay"
            ) @ArgOptional String renderMode)
    {
        if (sender instanceof EntityPlayerMP)
        {
            NetServerHandler handler = ((EntityPlayerMP)sender).playerNetServerHandler;
            (new CustomPacketPayload(PayloadType.DebuggingGeometryRenderMode, renderMode)).sendToClient(handler);
        }
        else
        {
            throw new IllegalArgumentException("/cogWireframeMode is a client-side command and may only be used by a player.");
        }
    }

    @CommandDelegate(
            desc = "Clears cached structure information.  Omit distribution name to clear all distributions."
    )
    public String cogClear(
            @ArgName(
                    name = "dimension"
            ) WorldServer world,
            @ArgName(
                    name = "distribution"
            )
            @ArgOptional(
                    defValue = ".*"
            ) String distribution)
    {
        int count = 0;
        
        for (IOreDistribution dist : ServerState.getWorldConfig(world).getOreDistributions(distribution)) {
        	++count;
            dist.clear();
            dist.validate();
        }

        resetClientGeometryCache();
        return "Cleared " + count + " distributions";
    }

    @CommandDelegate(
            desc = "Repopulates chunks in the specified range around the player.  Omit distribution name to repopulate all distributions."
    )
    public String cogPopulate(ICommandSender sender,
            @ArgName(
                    name = "dimension"
            ) WorldServer world,
            @ArgName(
                    name = "chunkRange"
            ) int chunkRange,
            @ArgName(
                    name = "distribution"
            )
            @ArgOptional(
                    defValue = ".*"
            ) String distribution,
            @ArgName(
                    name = "centerX"
            ) @ArgOptional Integer centerX,
            @ArgName(
                    name = "centerZ"
            ) @ArgOptional Integer centerZ)
    {
        WorldConfig cfg = ServerState.getWorldConfig(world);
        Collection list = cfg.getOreDistributions(distribution);
        ChunkCoordinates senderPos = sender.getPlayerCoordinates();
        int cX = centerX == null ? senderPos.posX : centerX.intValue();
        int cZ = centerZ == null ? senderPos.posZ : centerZ.intValue();

        for (int chunkX = (cX >> 4) - chunkRange; chunkX <= (cX >> 4) + chunkRange; ++chunkX)
        {
            for (int chunkZ = (cZ >> 4) - chunkRange; chunkZ <= (cZ >> 4) + chunkRange; ++chunkZ)
            {
                cfg.world.getChunkFromChunkCoords(chunkX, chunkZ);
                ServerState.populateDistributions(list, cfg.world, chunkX, chunkZ);
            }
        }

        return "Populated " + list.size() + " distributions in " + (2 * chunkRange + 1) * (2 * chunkRange + 1) + " chunk(s) around (" + cX + ",0," + cZ + ")";
    }

    @CommandDelegate(
            desc = "Sets the parent distribution.  Omit parent name to clear parent distribution."
    )
    public String cogParent(ICommandSender sender,
            @ArgName(
                    name = "dimension"
            ) WorldServer world,
            @ArgName(
                    name = "distribution"
            ) String distribution,
            @ArgName(
                    name = "parent"
            ) @ArgOptional String parent)
    {
        WorldConfig cfg = ServerState.getWorldConfig(world);
        IOreDistribution parentDist = null;

        if (parent != null)
        {
            Collection count = cfg.getOreDistributions(parent);

            if (count.isEmpty())
            {
                throw new IllegalArgumentException("Parent name \'" + parent + "\' does not match any distributions.");
            }

            if (count.size() > 1)
            {
                throw new IllegalArgumentException("Parent name \'" + parent + "\' is ambiguous (matches " + count.size() + " distributions).");
            }

            parentDist = (IOreDistribution)count.iterator().next();
        }

        int var12 = 0;
        
        for (IOreDistribution dist : cfg.getOreDistributions(distribution)) {
        	try
            {
                dist.setDistributionSetting(IOreDistribution.StandardSettings.Parent.name(), parentDist);
                ++var12;
                dist.clear();
                dist.validate();
            }
            catch (Exception var11)
            {
                ConsoleCommand.sendText(sender, "\u00a7c" + var11.getMessage());
            }
        }

        resetClientGeometryCache();
        return "Changed parent for " + var12 + " distributions";
    }

    private static int changeDescriptor(String settingName, ICommandSender sender, WorldServer world, String distribution, String descriptor, float weight, boolean clear)
    {
        int count = 0;
        
        for (IOreDistribution dist : ServerState.getWorldConfig(world).getOreDistributions(distribution)) {
        	try
            {
                Object ex = dist.getDistributionSetting(settingName);

                if (ex == null)
                {
                    throw new IllegalArgumentException("Distribution \'" + dist + "\' does not support descriptor " + settingName + ".");
                }

                if (ex instanceof BlockDescriptor)
                {
                    BlockDescriptor desc = (BlockDescriptor)ex;

                    if (clear)
                    {
                        desc.clear();
                    }

                    desc.add(descriptor, weight);
                    ++count;
                }
                else
                {
                    if (!(ex instanceof BiomeDescriptor))
                    {
                        throw new IllegalArgumentException("Setting " + settingName + " on Distribution \'" + dist + "\' is not a descriptor.");
                    }

                    BiomeDescriptor desc = (BiomeDescriptor)ex;

                    if (clear)
                    {
                        desc.clear();
                    }

                    desc.add(descriptor, weight);
                    ++count;
                }

                dist.clear();
                dist.validate();
            }
            catch (Exception var12)
            {
                ConsoleCommand.sendText(sender, "\u00a7c" + var12.getMessage());
            }
        }

        resetClientGeometryCache();
        return count;
    }

    @CommandDelegate(
            desc = "Adds an ore block."
    )
    public String cogAddOreBlock(ICommandSender sender,
            @ArgName(
                    name = "dimension"
            ) WorldServer world,
            @ArgName(
                    name = "distribution"
            ) String distribution,
            @ArgName(
                    name = "block"
            ) String block,
            @ArgName(
                    name = "weight"
            )
            @ArgOptional(
                    defValue = "1"
            ) float weight)
    {
        String setting = IOreDistribution.StandardSettings.OreBlock.name();
        int count = changeDescriptor(setting, sender, world, distribution, block, weight, false);
        return "Added ore block for " + count + " distributions";
    }

    @CommandDelegate(
            desc = "Sets the ore block (clearing any previous ore blocks)."
    )
    public String cogSetOreBlock(ICommandSender sender,
            @ArgName(
                    name = "dimension"
            ) WorldServer world,
            @ArgName(
                    name = "distribution"
            ) String distribution,
            @ArgName(
                    name = "block"
            ) String block,
            @ArgName(
                    name = "weight"
            )
            @ArgOptional(
                    defValue = "1"
            ) float weight)
    {
        String setting = IOreDistribution.StandardSettings.OreBlock.name();
        int count = changeDescriptor(setting, sender, world, distribution, block, weight, true);
        return "Set ore block for " + count + " distributions";
    }

    @CommandDelegate(
            desc = "Adds a replaceable block."
    )
    public String cogAddReplaceable(ICommandSender sender,
            @ArgName(
                    name = "dimension"
            ) WorldServer world,
            @ArgName(
                    name = "distribution"
            ) String distribution,
            @ArgName(
                    name = "block"
            ) String block,
            @ArgName(
                    name = "weight"
            )
            @ArgOptional(
                    defValue = "1"
            ) float weight)
    {
        String setting = IOreDistribution.StandardSettings.ReplaceableBlock.name();
        int count = changeDescriptor(setting, sender, world, distribution, block, weight, false);
        return "Added replaceable block for " + count + " distributions";
    }

    @CommandDelegate(
            desc = "Sets the replaceable block (clearing any previous replaceable blocks)."
    )
    public String cogSetReplaceable(ICommandSender sender,
            @ArgName(
                    name = "dimension"
            ) WorldServer world,
            @ArgName(
                    name = "distribution"
            ) String distribution,
            @ArgName(
                    name = "block"
            ) String block,
            @ArgName(
                    name = "weight"
            )
            @ArgOptional(
                    defValue = "1"
            ) float weight)
    {
        String setting = IOreDistribution.StandardSettings.ReplaceableBlock.name();
        int count = changeDescriptor(setting, sender, world, distribution, block, weight, true);
        return "Set replaceable block for " + count + " distributions";
    }

    @CommandDelegate(
            desc = "Adds a target biome."
    )
    public String cogAddBiome(ICommandSender sender,
            @ArgName(
                    name = "dimension"
            ) WorldServer world,
            @ArgName(
                    name = "distribution"
            ) String distribution,
            @ArgName(
                    name = "biome"
            ) String biome,
            @ArgName(
                    name = "weight"
            )
            @ArgOptional(
                    defValue = "1"
            ) float weight)
    {
        String setting = IOreDistribution.StandardSettings.TargetBiome.name();
        int count = changeDescriptor(setting, sender, world, distribution, biome, weight, false);
        return "Added biome for " + count + " distributions";
    }

    @CommandDelegate(
            desc = "Sets the target biome (clearing any previous biomes)."
    )
    public String cogSetBiome(ICommandSender sender,
            @ArgName(
                    name = "dimension"
            ) WorldServer world,
            @ArgName(
                    name = "distribution"
            ) String distribution,
            @ArgName(
                    name = "biome"
            ) String biome,
            @ArgName(
                    name = "weight"
            )
            @ArgOptional(
                    defValue = "1"
            ) float weight)
    {
        String setting = IOreDistribution.StandardSettings.TargetBiome.name();
        int count = changeDescriptor(setting, sender, world, distribution, biome, weight, true);
        return "Set biome for " + count + " distributions";
    }

    @CommandDelegate(
            desc = "Sets a distribution setting.  Setting names are the same as the <Setting> names in the config file.  Range and Type are optional (default to 0 and \'uniform\', respectively)."
    )
    public String cogSetting(ICommandSender sender,
            @ArgName(
                    name = "dimension"
            ) WorldServer world,
            @ArgName(
                    name = "distribution"
            ) String distribution,
            @ArgName(
                    name = "setting"
            ) String setting,
            @ArgName(
                    name = "average"
            ) float average,
            @ArgName(
                    name = "range"
            )
            @ArgOptional(
                    defValue = "0"
            ) float range,
            @ArgName(
                    name = "type"
            )
            @ArgOptional(
                    defValue = "uniform"
            ) Type type)
    {
        int count = 0;
        Iterator i$ = ServerState.getWorldConfig(world).getOreDistributions(distribution).iterator();

        while (i$.hasNext())
        {
            IOreDistribution dist = (IOreDistribution)i$.next();

            try
            {
                dist.setDistributionSetting(setting, new PDist(average, range, type));
                ++count;
                dist.clear();
                dist.validate();
            }
            catch (Exception var12)
            {
                ConsoleCommand.sendText(sender, "\u00a7c" + var12.getMessage());
            }
        }

        resetClientGeometryCache();
        return "Changed \'" + setting + "\' for " + count + " distributions";
    }

    @CommandDelegate(
            desc = "Sets a simple numeric/boolean/string/enum setting.  Setting names are the same as the corresponding attributes in the config file."
    )
    public String cogSettingEx(ICommandSender sender,
            @ArgName(
                    name = "dimension"
            ) WorldServer world,
            @ArgName(
                    name = "distribution"
            ) String distribution,
            @ArgName(
                    name = "setting"
            ) String setting,
            @ArgName(
                    name = "value"
            ) String value)
    {
        int count = 0;
        Iterator i$ = ServerState.getWorldConfig(world).getOreDistributions(distribution).iterator();

        while (i$.hasNext())
        {
            IOreDistribution dist = (IOreDistribution)i$.next();

            try
            {
                dist.setDistributionSetting(setting, value);
                ++count;
                dist.clear();
                dist.validate();
            }
            catch (Exception var10)
            {
                ConsoleCommand.sendText(sender, "\u00a7c" + var10.getMessage());
            }
        }

        resetClientGeometryCache();
        return "Changed \'" + setting + "\' for " + count + " distributions";
    }

    @CommandDelegate(
            desc = "Dumps a summary of an Option to the console."
    )
    public String cogOptionInfo(
            @ArgName(
                    name = "dimension"
            ) WorldServer world,
            @ArgName(
                    name = "option"
            ) @ArgOptional String option,
            @ArgName(
                    name = "detail"
            )
            @ArgOptional(
                    defValue = "false"
            ) boolean detail)
    {
        StringBuilder msg = new StringBuilder();
        WorldConfig config = ServerState.getWorldConfig(world);
        Collection allOptions = config.getConfigOptions();
        Collection<ConfigOption> options = option != null && !option.isEmpty() ? config.getConfigOptions(option) : null;
        msg.append("CustomOreGen [1.4.6]v2 (");

        if (config.world != null)
        {
            msg.append("Dim " + config.world.provider.dimensionId + ", ");
        }

        if (options != null)
        {
            msg.append(options.size() + "/");
        }

        msg.append(allOptions.size() + " options");
        msg.append(')');

        if (options != null && options.size() > 0)
        {
            msg.append(':');
            
            for (ConfigOption opt : options) {
            	buildFieldValue(msg, "  ", opt.getName(), detail ? opt.getDescription() : null, opt.getValue());
            }
        }

        CustomOreGenBase.log.finer(msg.toString());
        return msg.toString();
    }

    @CommandDelegate(
            desc = "Set an option value for the current dimension.  The change lasts until the world configuration is reloaded."
    )
    public String cogOption(ICommandSender sender,
            @ArgName(
                    name = "dimension"
            ) WorldServer world,
            @ArgName(
                    name = "option"
            ) String option,
            @ArgName(
                    name = "value"
            ) String value)
    {
        WorldConfig cfg = ServerState.getWorldConfig(world);
        Collection options = cfg.getConfigOptions(option);
        int count = 0;
        Iterator path = options.iterator();

        while (path.hasNext())
        {
            ConfigOption opt = (ConfigOption)path.next();

            if (opt.setValue(value))
            {
                ++count;
            }
            else
            {
                ConsoleCommand.sendText(sender, "\u00a7cInvalid value \'" + value + "\' for Option \'" + opt.getName() + "\'");
            }
        }

        WorldConfig.loadedOptionOverrides[2] = cfg.getConfigOptions();
        ServerState.clearWorldConfig(world);
        cfg = ServerState.getWorldConfig(world);
        String var10 = cfg.dimensionDir.toString();
        WorldConfig.loadedOptionOverrides[2] = null;
        resetClientGeometryCache();
        return "Changed " + count + " options for " + var10;
    }

    @CommandDelegate(
            isDebugging = false,
            desc = "Enabled or disable debugging mode for the current dimension."
    )
    public String cogEnableDebugging(ICommandSender sender,
            @ArgName(
                    name = "dimension"
            ) WorldServer world,
            @ArgName(
                    name = "enable"
            )
            @ArgOptional(
                    defValue = "true"
            ) boolean enabled)
    {
        return this.cogOption(sender, world, "debugMode", Boolean.toString(enabled));
    }

    @CommandDelegate(
            desc = "Reloads the world configuration from disk.  This will reset any changes made via console commands."
    )
    public String cogLoadConfig(
            @ArgName(
                    name = "dimension"
            ) WorldServer world)
    {
        ServerState.clearWorldConfig(world);
        String path = ServerState.getWorldConfig(world).dimensionDir.toString();
        resetClientGeometryCache();
        return "Reloaded config data for " + path;
    }
}
