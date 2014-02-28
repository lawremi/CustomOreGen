All of these are defined in the original as inside of this construct:

    <IfCondition condition=':= dimension.generator = "RandomLevelSource" | dimension.generator = "ATGLevelSource"'>

I have used
    <IfCondition condition=':= (dimension.generator = "RandomLevelSource") 
                                 | ( dimension.generator = "TwilightLevelSource" )
    '>

Ultimately, I think we need something like many files, each of which looks like


    <IfCondition condition=':= (dimension.generator.class = "ChunkProviderTwilightForest") '>

Define ground level -- based on biomes for RandomLevelSource / ChunkProviderGenerate, or ATG values, etc.
    Some ores can occur above ground (coal, redstone vertical, oil, etc)
Define sea level -- hardcode 32 for ChunkProviderNether, or get it from dimension.xxx, etc.
Define "Roof" level -- might be 128 (normal), or 248 (some extreme mod gens), or ...
    Normally, nothing occurs above roof; roof is used instead of a percentage of ground in "ore rich" worlds.


    </IfCondition>

Going off the chunk provider is going to be better than going off the world type, just because worldtype
is not reliable, and chunk provider also implies what (if any) API exists for use for that world.

Currently, these are "unchanged" 

So after a good night's sleep, here's how "unchanged" works.

This version, version 1: Actually unchanged. Generate. Each one wrapped in the IfCondition.
Next version, version 2: Changed to be presets, not wrapped. Elsewhere, something else has the wraps and
calls each preset. This will demonstrate the need for preset groups.
Future version, version 3: Add in the ability to generate all groups at varying frequencies, instead of
"choose one, and not the others".

I already know that the current numbering is NOT going to work in the final round.
But I have to start somewhere. (Note that each file contains a zero ... none of the final ones will.)

What we need -- and I mentioned this to JRoush -- is a way to include files that are not XML units, but text units.
For example, the repetitive nature of the IfCondition for generation in version 1.
Or the mystcraft stablity calculations (granted, old school no longer of any use, but still ...)

NB: Why, after all the times that I've posted better instability formulas, are these still in use?

