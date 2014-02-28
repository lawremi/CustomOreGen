Here are all the distributions for each type of ore.

We actually have three files for each ore, one for each of the three types of distributions
(clusters, clouds, veins).

There are many different types of veins. There's normal veins, sparse veins, pipe veins,
strategic veins, etc. In files for other mod ores, there's "triangular" veins where 2-3
diagonal veins run down from an upper motherlode. 

In breaking this up, I finally realized what seems to be the issue with mystcraft symbols.

"coal ore, veins", versus "coal ore, clouds", versus "redstone ore, veins", versus "wood, veins".

"veins" is not the right word.

"layered veins", or "pipe veins", or "sparse veins". Or "clouds", or "strategic veins", or "clusters".

Those are the right words.

"iron, sparse veins" should generate a sparse vein set for iron; yet note that there is no definition
for what that would look like. Nor what a triangular vein would look like.

Nor can you expect any one that provides a new vein layout to define it for all ores.

What ultimately would be needed is a "generic" layout for a given layout type, and then each
material/ore has "here's how I modify the generic layout".

Note that we are close to that now. We have the concept of a generic layout. But we don't
have "Here's how iron differs from generic". Instead, we have "Here's how iron clouds
differ from generic clouds", and "here's how iron normal veins differ from generic veins".

There is no "Here's how an iron strategic vein differs from a normal strategic vein".
Nor triangular vein, nor pipe vein (the diamond distribution), nor vertical vein (the redstone), etc.

Solve that, and you solve Mystcraft.
Lack that, and there is no good solution for Mystcraft.
