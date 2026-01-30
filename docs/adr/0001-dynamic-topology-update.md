 # ADR 0001: Dynamic Topology Updates at Runtime

 - Status: Accepted
 - Date: 2026-01-30

 ## Context
 The system previously relied on static topology loaded at startup. Updating the hierarchical group
 topology required restarts and manual coordination. The recent changes introduce runtime topology
 updates, an HTTP entry point, and a consensus-backed propagation path so all replicas apply updates
 consistently.

 ## Decision
 Introduce a runtime topology update flow with the following design:
 - Expose an optional HTTP endpoint (`POST /topology`) on a single replica to accept new topology
   definitions. The replica is chosen operationally by starting it with `--http-port`; only replicas
   started with this flag expose the endpoint, so the operator decides which replica receives the
   POST requests.
 - Serialize topology changes as `TopologyUpdateRequest` and broadcast them via BFT-SMaRT ordered
   invocations so all replicas apply updates in the same order.
 - Centralize topology access and state transitions in `TopologyManager`, backed by
   `AtomicReference<Topology>` and `AtomicReference<TopologyState>`.
 - Gate request processing in `RequestHandler` based on `TopologyState`, rejecting requests while
   the system is updating or in error.
 - Use `TopologyLoader.buildFromList(...)` for validation and construction, enabling both JSON
   loading and runtime updates to share the same validation path.

 ## Consequences
 - Consistent, consensus-backed updates across replicas with a single update entry point.
 - Clear operational states (`READY`, `UPDATING`, `ERROR`) and predictable rejection behavior during
   transitions.
 - Additional runtime components (Jetty HTTP server and Gson) and operational considerations
   (optional HTTP port, error handling on update failures).

 ## Alternatives Considered
 - **Out-of-band file reload with restart:** simpler but requires downtime and manual coordination.
 - **Direct replica-to-replica RPC without consensus:** lower latency but risks inconsistent
   application order and divergent topology.
 - **Client-side topology routing:** shifts complexity to clients and does not guarantee
   cluster-wide atomic update.

 ## Notes
 - Architecture details and diagrams are documented in
   `docs/DYNAMIC_TOPOLOGY_UPDATE_ARCHITECTURE.md`.
