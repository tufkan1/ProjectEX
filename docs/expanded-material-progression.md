# Expanded material progression

The post-red-matter chain is deliberately linear:

`Aeternalis -> Magenta -> Pink -> Purple -> Violet -> Blue -> Cyan -> Green -> Lime -> Yellow -> Orange -> White`

Each fuel consumes exactly four units of the previous fuel. Each matching matter consumes six
units of its tier's fuel and three units of the previous matter. Fading Matter is the terminal
matter step and uses White Fuel plus White Matter with the same 6:3 ratio.

There are no reverse or lossy compression recipes in this chain. ProjectEX's recipe EMC mapper
therefore derives every value from its ingredients with arbitrary-precision integers, and a
server GameTest verifies the complete graph against those exact formulas. Expanded fuels burn
for 25,600 ticks, matching the Aeternalis-class fuel policy; Alchemical Coal and Mobius Fuel burn
for 1,600 and 6,400 ticks respectively.

All item identifiers are fixed for save compatibility. Recipe JSON, tags, translations, and
placeholder models are generated from the registered tier order. Server packs may still override
recipes or EMC values using the existing ProjectEX data-pack APIs.
