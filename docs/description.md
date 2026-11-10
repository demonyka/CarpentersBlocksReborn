A from-scratch rewrite of **Carpenter's Blocks** for Minecraft 1.16.5, on both **Forge** and **Fabric**. Frame blocks, slopes, stairs, doors and redstone parts that take the look of any block you put on them, with the controls and behaviour of the 1.7.10 version.

On Fabric it needs **Fabric API**. With Sodium, also install **Indium**, or the blocks are drawn without their covers.

## How it works

Place a Carpenter's block, then right-click it with almost any full block: it takes that block's texture. Sneak and right-click with a dye to tint it, or with glowstone dust to make it glow. The Carpenter's Hammer changes the shape and takes covers back off.

Covers look as they do on the real block: grass and leaves keep their biome colour, logs keep their rings, stained glass and ice stay see-through.

## Blocks

**Carpenter's Block**: a full block or a slab on any of the six sides.

**Slopes**: 65 shapes in five kinds (wedges, corners, oblique corners, prisms, prism wedges). They join their neighbours into roofs and corners as you place them. Hold a slope, sneak and scroll to switch the kind.

**Stairs** and the **Collapsible Block**, whose four corners you raise and lower with the hammer to shape terrain.

**Barrier and Gate** in several fence styles, **Door** in six styles, **Hatch**, **Garage Door** that hangs from the ceiling down to the floor, **Ladder**.

**Redstone**: button, lever, and a pressure plate that reacts to players, monsters, animals or everything. Each can invert its signal.

**Torch and Lantern**: the torch goes out in the rain and lights up again afterwards.

## Controls

| Action | Result |
| --- | --- |
| Right-click with a block | Cover the Carpenter's block |
| Sneak + right-click with a block on a covered block | Cover just that side with another block |
| Sneak + right-click with a dye / glowstone dust | Tint / make it glow |
| Sneak + left-click with the hammer | Take off the glow, dye or cover |
| Hammer on a slope or stairs | Left-click turns it, right-click picks the next shape |
| Hammer on other blocks | Change style, hinge, post, signal polarity or trigger |
| Sneak + scroll with a slope in hand | Switch the kind of slope |

## Works with

- **OptiFine** (Forge, tested with 1.16.5 HD U G7): connected textures apply to covers. A wall of glass and glass-covered Carpenter's blocks looks like one sheet.
- **Chisel / CTM** (Forge): covers made of connected-texture blocks join each other and the real blocks around them.
- **Jade / HWYLA** (Forge): the tooltip names the block a Carpenter's block is covered with.
- **Claim and protection mods**: every click goes through the normal interaction events, so protected areas stay protected.

## For server owners

- The server decides everything; clients only predict. Block data from clients and from saved items is validated.
- Recipes, drops and the lists of what counts as a cover are plain data: change them with a data pack and `/reload`.
- Every build is started on real dedicated Forge and Fabric servers and run through automated checks.

## Not included

The safe, bed, flower pot, tile, daylight sensor, chisel patterns and overlays of the 1.7.10 version are not part of this rewrite.

## Credits

Carpenter's Blocks was created by **Mineshopper** and contributors; the design, the textures and the logo are theirs and stay under LGPL-2.1. This rewrite is by **demonyga**, its code is under the Apache License 2.0. Source on [GitHub](https://github.com/demonyka/CarpentersBlocksReborn).
