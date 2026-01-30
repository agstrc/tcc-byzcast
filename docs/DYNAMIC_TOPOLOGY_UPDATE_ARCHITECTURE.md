# Dynamic Topology Update Architecture

This document describes the architecture for the dynamic topology update feature in the ByzCast system.

## Overview

The dynamic topology update feature enables runtime reconfiguration of the ByzCast system's hierarchical group topology via an HTTP endpoint, with updates propagated through BFT-SMaRT consensus.

### Key Characteristics

- **Consensus-Driven**: Updates propagated through BFT-SMaRT total order multicast
- **Single Entry Point**: One designated replica hosts the HTTP endpoint
- **Request Rejection**: In-flight requests rejected during topology transitions
- **Atomic Updates**: Topology changes applied atomically using `AtomicReference`

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                  ByzCast System                                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   ┌─────────────────────────────────────────────────────────────────────────┐   │
│   │                           ByzCast Cluster                                │   │
│   │                                                                          │   │
│   │   ┌─────────────────┐  ┌─────────────┐  ┌─────────────┐  ┌───────────┐  │   │
│   │   │   Replica 0     │  │  Replica 1  │  │  Replica 2  │  │ Replica 3 │  │   │
│   │   │                 │  │             │  │             │  │           │  │   │
│   │   │ ┌─────────────┐ │  │             │  │             │  │           │  │   │
│   │   │ │ HTTP Server │ │  │             │  │             │  │           │  │   │
│   │   │ │ (Jetty)     │ │  │             │  │             │  │           │  │   │
│   │   │ │ POST /topo  │ │  │             │  │             │  │           │  │   │
│   │   │ └──────┬──────┘ │  │             │  │             │  │           │  │   │
│   │   │        │        │  │             │  │             │  │           │  │   │
│   │   │ ┌──────▼──────┐ │  │ ┌─────────┐ │  │ ┌─────────┐ │  │ ┌───────┐ │  │   │
│   │   │ │ Replica     │ │  │ │ Replica │ │  │ │ Replica │ │  │ │Replica│ │  │   │
│   │   │ │ Node        │ │  │ │ Node    │ │  │ │ Node    │ │  │ │Node   │ │  │   │
│   │   │ └──────┬──────┘ │  │ └────┬────┘ │  │ └────┬────┘ │  │ └───┬───┘ │  │   │
│   │   │        │        │  │      │      │  │      │      │  │     │     │  │   │
│   │   │ ┌──────▼──────┐ │  │ ┌────▼────┐ │  │ ┌────▼────┐ │  │ ┌───▼───┐ │  │   │
│   │   │ │ Topology    │ │  │ │Topology │ │  │ │Topology │ │  │ │Topo   │ │  │   │
│   │   │ │ Manager     │ │  │ │Manager  │ │  │ │Manager  │ │  │ │Manager│ │  │   │
│   │   │ └─────────────┘ │  │ └─────────┘ │  │ └─────────┘ │  │ └───────┘ │  │   │
│   │   └────────┬────────┘  └──────┬──────┘  └──────┬──────┘  └─────┬─────┘  │   │
│   │            │                  │                │               │        │   │
│   │            └──────────────────┼────────────────┼───────────────┘        │   │
│   │                               │                │                        │   │
│   │                    ┌──────────▼────────────────▼──────────┐             │   │
│   │                    │         BFT-SMaRT Consensus          │             │   │
│   │                    │    (Total Order Multicast Layer)     │             │   │
│   │                    └──────────────────────────────────────┘             │   │
│   │                                                                          │   │
│   └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
        ▲                           ▲                           ▲
        │                           │                           │
        │ HTTP POST                 │ Requests                  │ Requests
        │ /topology                 │                           │
        │                           │                           │
   ┌────┴────┐                 ┌────┴────┐                 ┌────┴────┐
   │  Admin  │                 │ Client  │                 │ Client  │
   └─────────┘                 └─────────┘                 └─────────┘
```

---

## Complete Flow Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Admin as Administrator
    participant HTTP as TopologyHttpServer<br/>(Replica 0)
    participant Proxy as ServiceProxy
    participant BFT as BFT-SMaRT<br/>Consensus
    participant R0 as Replica 0<br/>ReplicaNode
    participant R1 as Replica 1<br/>ReplicaNode
    participant R2 as Replica 2<br/>ReplicaNode
    participant R3 as Replica 3<br/>ReplicaNode
    participant TM as TopologyManager<br/>(Each Replica)

    rect rgb(240, 248, 255)
        Note over Admin,HTTP: Phase 1: HTTP Request Reception
        Admin->>HTTP: POST /topology<br/>Body: [[1,2], [], []]
        HTTP->>HTTP: Parse JSON
        HTTP->>HTTP: Validate topology not empty
        
        alt Invalid JSON
            HTTP-->>Admin: 400 Bad Request<br/>{"error": "Invalid JSON"}
        else Empty topology
            HTTP-->>Admin: 400 Bad Request<br/>{"error": "Empty topology"}
        end
    end

    rect rgb(255, 250, 240)
        Note over HTTP,BFT: Phase 2: Consensus Broadcast
        HTTP->>HTTP: Create TopologyUpdateRequest(UUID, topology)
        HTTP->>Proxy: invokeOrdered(serialized request)
        Proxy->>BFT: Propose to consensus
        Note over BFT: Total Order Multicast ensures<br/>all replicas receive in same order
    end

    rect rgb(240, 255, 240)
        Note over BFT,TM: Phase 3: Parallel Delivery & Processing
        par Deliver to all replicas
            BFT->>R0: Deliver TopologyUpdateRequest
            BFT->>R1: Deliver TopologyUpdateRequest
            BFT->>R2: Deliver TopologyUpdateRequest
            BFT->>R3: Deliver TopologyUpdateRequest
        end

        par Each replica processes update
            R0->>TM: updateTopology(topology)
            Note over TM: state = UPDATING
            TM->>TM: Build Group hierarchy
            TM->>TM: Validate (cycle detection)
            
            alt Validation fails (e.g., cycle detected)
                Note over TM: state = ERROR
                TM-->>R0: throw TopologyUpdateException
                R0-->>BFT: TOPOLOGY_UPDATE_FAILED
            else Validation succeeds
                TM->>TM: Apply new topology atomically
                Note over TM: state = READY
                R0-->>BFT: TOPOLOGY_UPDATED
            end
            
            R1->>TM: updateTopology(topology)
            R1-->>BFT: Reply
            R2->>TM: updateTopology(topology)
            R2-->>BFT: Reply
            R3->>TM: updateTopology(topology)
            R3-->>BFT: Reply
        end
    end

    rect rgb(255, 240, 245)
        Note over BFT,Admin: Phase 4: Response
        BFT-->>Proxy: Consensus reply (N-F responses)
        Proxy-->>HTTP: Response bytes
        HTTP->>HTTP: Deserialize Response
        
        alt Success
            HTTP-->>Admin: 200 OK<br/>{"status": "success"}
        else Failure
            HTTP-->>Admin: 200 OK<br/>{"error": "Update failed: ..."}
        end
    end

    rect rgb(245, 245, 245)
        Note over Admin,TM: During UPDATING/ERROR state
        Note over TM: Any client request during<br/>UPDATING or ERROR state<br/>is rejected with:<br/>"TOPOLOGY_UPDATING" or<br/>"TOPOLOGY_ERROR"
    end
```

---

## State Machine

```
                    ┌─────────────────────────────────────────┐
                    │            TopologyState                │
                    └─────────────────────────────────────────┘

                              ┌─────────┐
                              │  READY  │◄──────────────────────┐
                              └────┬────┘                       │
                                   │                            │
                        updateTopology()                        │
                             called                             │
                                   │                            │
                                   ▼                            │
                            ┌──────────┐                        │
                            │ UPDATING │                        │
                            └────┬─────┘                        │
                                 │                              │
                    ┌────────────┴────────────┐                 │
                    │                         │                 │
               Success                    Failure               │
                    │                         │                 │
                    ▼                         ▼                 │
              ┌─────────┐              ┌──────────┐             │
              │  READY  │              │  ERROR   │─────────────┘
              └─────────┘              └────┬─────┘
                                            │
                                   acknowledgeError()
                                         called
                                            │
                                            ▼
                                      ┌─────────┐
                                      │  READY  │
                                      └─────────┘

    ┌─────────┬───────────────────────────────┬─────────────────────────┐
    │  State  │  Description                  │  Request Handling       │
    ├─────────┼───────────────────────────────┼─────────────────────────┤
    │  READY  │  Normal operation             │  Process normally       │
    │ UPDATING│  Update in progress           │  Reject: TOPOLOGY_UPDATING │
    │  ERROR  │  Last update failed           │  Reject: TOPOLOGY_ERROR │
    └─────────┴───────────────────────────────┴─────────────────────────┘
```

---

## Key Components

| Component | Purpose |
|-----------|---------|
| **TopologyHttpServer** | Embedded Jetty server, exposes POST `/topology` endpoint |
| **TopologyUpdateRequest** | Message record with UUID and topology list for BFT-SMaRT |
| **TopologyManager** | Thread-safe topology holder with state machine |
| **TopologyState** | Enum: READY, UPDATING, ERROR |
| **ReplicaNode** | Deserializes messages, dispatches to handler or topology update |
| **RequestHandler** | Checks state before processing, rejects if not READY |

---

## Advantages

1. **Consistency** - BFT-SMaRT ensures all replicas update in same order
2. **Zero Downtime** - No restart required for topology changes
3. **Byzantine Fault Tolerant** - Up to F faulty replicas tolerated (N = 3F + 1)
4. **Simple API** - Single HTTP endpoint with JSON payload
5. **Atomic Updates** - All-or-nothing application via AtomicReference
6. **Validation** - Cycle detection prevents invalid topologies

## Disadvantages

1. **Request Rejection** - Clients get errors during updates (must retry)
2. **Single Entry Point** - Only one replica has HTTP endpoint
3. **No Rollback** - Failed updates require manual intervention
4. **Synchronous** - HTTP blocks until consensus completes

---

## Usage

```bash
# Update topology
curl -X POST http://localhost:9080/topology \
  -H "Content-Type: application/json" \
  -d '[[1, 2], [], []]'

# Response (success)
{"status": "success", "message": "Topology updated successfully"}

# Response (error)
{"error": "Topology update failed: Group 0 attempted to have ancestor 0 as child"}
```

### JSON Topology Format

```
Index = Group ID
Value = Array of child group IDs

Example: [[1, 2], [3], [], []]

    Group 0 (root)
       /    \
   Group 1  Group 2
      |
   Group 3
```
