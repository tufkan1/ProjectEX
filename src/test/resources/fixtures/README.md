# Public persistence fixtures

Each directory is an immutable sample from a public ProjectEX release line. The build
must decode every player, block-entity, item-component, and client-preference fixture
before a release can advance the world format marker.

`0.1.0-alpha.1/` is format-0 input: player payloads use schema 0 and migrate to schema 1;
the first published machine item, storage block, and preference records already use their
version-1 codecs. Future releases add directories and migration assertions without editing
older fixtures.
