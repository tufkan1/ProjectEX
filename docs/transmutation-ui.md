# Transmutation UI interaction contract

The transmutation UI consumes `ClientAlchemySessionState` and
`ClientKnowledgeBrowserState`; it never calculates or predicts balance changes. Search
is performed by the server against learned identifiers and results arrive in bounded
pages. Favorites are a client preference layered over authoritative page entries.

## Keyboard and narration requirements

1. Opening the screen focuses search, then Tab moves through the result grid, previous,
   next, create, favorite, learn-held, and burn-held controls in visual order.
2. Arrow keys move within the result grid without requiring a mouse.
3. Enter selects a focused learned item; Tab reaches an explicit Create button whose
   activation remains server-authoritative and prevents accidental creation.
4. The favorite shortcut never triggers create/burn and announces its new state.
5. Page changes announce current page, total pages, result count, balance, and any
   typed server failure through translatable narration text.
6. No meaning is conveyed only by color; focus, disabled, favorite, success, and error
   states require shape/icon/text or narration equivalents.

## Latency and reconnect cases

- A newer query response arriving before an older response keeps the newer page.
- Duplicate, unsolicited, cross-session, and post-close responses change nothing.
- A stale-EMC action response refreshes revision/balance before retry is offered.
- Search input is debounced by the screen while protocol limits remain authoritative.
- Disconnect clears session/pages immediately; reconnect requires a new server session.
- Opening a second menu replaces the first nonce and resets both request sequences.

Pure client models and payload tests cover these ordering rules. A headless client
GameTest opens the real screen in singleplayer and completes learn, burn, and create
over the production networking path. Manual visual and screen-reader review remains a
release checklist item because automated narration text coverage cannot judge delivery
quality.

The server-owned menu is opened by using a Transmutation Table. During M2 development,
`/projectex transmutation` remains available as a fallback.
This exercises real player inventory, persistence, networking, search, learn, burn,
create, favorites, and reconnect behavior before the physical table/tablet content is
introduced. Component-bearing stacks are deliberately excluded from held-item actions
until an exact canonical component adapter is available.

Favorites are stored in `config/projectex-favorites.json`. The file is versioned,
bounded to 10,000 valid item identifiers, written through a temporary file, and treated
as an optional preference: a missing, newer, or damaged file never prevents startup.
