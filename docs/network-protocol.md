# Transmutation network protocol

ProjectEX registers bounded Fabric play payloads in common initialization. Protocol
version 1 uses `projectex:alchemy_action_v1` (client to server) and
`projectex:alchemy_result_v1` / `projectex:alchemy_session_v1` (server to client).

The session payload is sent only after the server opens an authorized menu. It carries
the nonce, authoritative EMC revision, balance, and learned-item count. The client
resets its request sequence whenever a new valid session arrives.

## Action payload

| Field | Meaning |
| --- | --- |
| `protocolVersion` | Must equal `AlchemyNetworkProtocol.VERSION` |
| `sessionId` | Non-zero random nonce created only by the active server menu |
| `requestId` | Non-negative, strictly increasing within the session |
| `operationId` | `0` learn, `1` burn, `2` create |
| `itemId` | Loader-neutral identifier, at most 256 characters |
| `count` | Learn requires exactly 1; burn/create domain limit is 1–64 |
| `emcRevision` | Snapshot revision observed by the client; stale requests fail |

No payload contains an EMC amount, player UUID, position, distance, permission, or
menu authorization. Those facts are resolved on the logical server. Version, session,
replay, operation, identifier, access, distance, rate, inventory, knowledge, balance,
and EMC snapshot checks happen before commit.

## Result payload

The response echoes session/request ids and reports protocol version, typed failure,
authoritative EMC revision, authoritative balance, and learned-item count. Clients
must discard responses for another session/request and require
`AlchemyResultPayload.isStructurallyValid()` before updating UI state.
`ClientAlchemySessionState` additionally requires the active session id, a request id
that was actually sent, and a response id newer than the last accepted response.
Late, duplicate, unsolicited, malformed, and cross-session responses are discarded.

## Abuse controls and lifecycle

- Raw traffic is limited to 40 packets per player per second before session parsing.
- Valid domain traffic is additionally limited to 20 operations per second.
- Replayed or reordered request ids are rejected even when the original operation failed.
- A new server menu replaces the previous session and resets limiter/replay state.
- Disconnect and menu close remove session and limiter state.
- Client disconnect clears all cached authoritative state; screens never retain a
  balance or failure from the previous server.
- Access-supplier failure closes the session instead of running a transaction.
- Payload strings are length bounded during decode; malformed identifiers/operations
  never become domain transactions.

Breaking field or semantic changes require a new payload identifier and protocol
version. Old versions must fail with `UNSUPPORTED_PROTOCOL`, not be guessed or coerced.

## Knowledge pages

`alchemy_knowledge_request_v1` carries a separate monotonic query id, a search string
of at most 64 characters, a zero-based page, and a requested page size of 1–54.
`alchemy_knowledge_page_v1` returns only learned identifiers that still have a positive
value in the current authoritative EMC snapshot. Filtering and paging happen on the
server; responses contain at most 54 identifier/value entries. Action and query replay
sequences are independent so search latency cannot invalidate create/burn requests.
