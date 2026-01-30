#!/bin/bash

# =============================================================================
# Dynamic Topology Update Demo Script
# =============================================================================
# This script demonstrates the dynamic topology update feature:
# 1. Creates BFT-SMaRT configuration for a 4-replica cluster
# 2. Starts all 4 replicas (one with HTTP endpoint)
# 3. Sends topology update requests via curl
# =============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
CONFIG_DIR="$PROJECT_DIR/demo-config"
JAR_FILE="$PROJECT_DIR/target/byzcast-tcc-1.0-SNAPSHOT-jar-with-dependencies.jar"
HTTP_PORT=9080
BASE_PORT=60000
LOG_DIR="/tmp/byzcast-demo"

# Cleanup function
cleanup() {
    echo -e "\n${YELLOW}Cleaning up...${NC}"
    pkill -f "byzcast-tcc.*demo-config" 2>/dev/null || true
    rm -rf "$CONFIG_DIR"
    rm -rf "$LOG_DIR"
    echo -e "${GREEN}Cleanup complete${NC}"
}

# Trap to cleanup on exit
trap cleanup EXIT

# Print banner
print_banner() {
    echo -e "${BLUE}"
    echo "=============================================="
    echo "   ByzCast Dynamic Topology Update Demo"
    echo "=============================================="
    echo -e "${NC}"
}

# Check prerequisites
check_prerequisites() {
    echo -e "${YELLOW}Checking prerequisites...${NC}"
    
    if [ ! -f "$JAR_FILE" ]; then
        echo -e "${RED}JAR file not found. Building project...${NC}"
        cd "$PROJECT_DIR"
        mvn package -DskipTests -q
    fi
    
    if ! command -v curl &> /dev/null; then
        echo -e "${RED}curl is required but not installed.${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}Prerequisites OK${NC}"
}

# Create BFT-SMaRT configuration
create_config() {
    echo -e "${YELLOW}Creating BFT-SMaRT configuration...${NC}"
    
    mkdir -p "$CONFIG_DIR/g00"
    mkdir -p "$LOG_DIR"
    
    # Calculate ports (BASE_PORT + offset)
    local P0=$((BASE_PORT))
    local P0C=$((BASE_PORT + 1))
    local P1=$((BASE_PORT + 10))
    local P1C=$((BASE_PORT + 11))
    local P2=$((BASE_PORT + 20))
    local P2C=$((BASE_PORT + 21))
    local P3=$((BASE_PORT + 30))
    local P3C=$((BASE_PORT + 31))
    
    # Create hosts.config
    cat > "$CONFIG_DIR/g00/hosts.config" << EOF
# BFT-SMaRT hosts configuration
0 127.0.0.1 $P0 $P0C
1 127.0.0.1 $P1 $P1C
2 127.0.0.1 $P2 $P2C
3 127.0.0.1 $P3 $P3C
EOF
    
    # Create system.config (CFT mode for demo - no keys required)
    cat > "$CONFIG_DIR/g00/system.config" << EOF
# BFT-SMaRT system configuration
system.servers.num = 4
system.servers.f = 1
system.communication.useSenderThread = true
system.communication.defaultPort = $P0
system.initial.view = 0,1,2,3
system.communication.timeout = 10000
system.communication.useSignatures = 0
system.communication.useMACs = 0
system.totalordermulticast.timeout = 10000
system.totalordermulticast.period = 200
system.totalordermulticast.maxbatchsize = 400
system.bft = false
system.ttp.id = 7002
EOF
    
    # Create initial topology (single group)
    cat > "$CONFIG_DIR/topology.json" << EOF
[[]]
EOF
    
    echo -e "${GREEN}Configuration created at $CONFIG_DIR${NC}"
}

# Start replicas
start_replicas() {
    echo -e "${YELLOW}Starting 4 replicas...${NC}"
    
    # Start replica 0 with HTTP endpoint
    java -cp "$JAR_FILE" dev.agst.byzcast.Main \
        --groups-configs "$CONFIG_DIR" \
        --topology "$CONFIG_DIR/topology.json" \
        server --server-id 0 --group-id 0 --http-port $HTTP_PORT \
        > "$LOG_DIR/replica0.log" 2>&1 &
    echo -e "  Replica 0 (HTTP on port $HTTP_PORT): ${GREEN}started${NC}"
    
    # Start replicas 1-3
    for i in 1 2 3; do
        java -cp "$JAR_FILE" dev.agst.byzcast.Main \
            --groups-configs "$CONFIG_DIR" \
            --topology "$CONFIG_DIR/topology.json" \
            server --server-id $i --group-id 0 \
            > "$LOG_DIR/replica$i.log" 2>&1 &
        echo -e "  Replica $i: ${GREEN}started${NC}"
    done
    
    echo -e "${YELLOW}Waiting for replicas to initialize...${NC}"
    sleep 8
    
    # Check if replicas are running
    RUNNING=$(pgrep -f "byzcast-tcc.*demo-config" | wc -l)
    if [ "$RUNNING" -lt 4 ]; then
        echo -e "${RED}Error: Only $RUNNING replicas running. Check logs at $LOG_DIR${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}All 4 replicas are running${NC}"
}

# Send topology update
send_topology_update() {
    local topology="$1"
    local description="$2"
    
    echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}Sending topology update: ${description}${NC}"
    echo -e "${BLUE}Topology: ${NC}$topology"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    
    RESPONSE=$(curl -s -X POST "http://localhost:$HTTP_PORT/topology" \
        -H "Content-Type: application/json" \
        -d "$topology" \
        --max-time 30)
    
    echo -e "${BLUE}Response:${NC} $RESPONSE"
    
    if echo "$RESPONSE" | grep -q '"status": "success"'; then
        echo -e "${GREEN}✓ Topology update successful${NC}"
    else
        echo -e "${RED}✗ Topology update failed${NC}"
    fi
    
    sleep 1
}

# Demo scenarios
run_demo() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}       Starting Demo Scenarios${NC}"
    echo -e "${BLUE}========================================${NC}"
    
    # Scenario 1: Simple two-level topology
    send_topology_update '[[1, 2], [], []]' \
        "Simple tree: Group 0 → [Group 1, Group 2]"
    
    # Scenario 2: Deep chain topology
    send_topology_update '[[1], [2], [3], []]' \
        "Chain: Group 0 → Group 1 → Group 2 → Group 3"
    
    # Scenario 3: Complex tree
    send_topology_update '[[1, 2], [3, 4], [5], [], [], []]' \
        "Complex tree with 6 groups"
    
    # Scenario 4: Invalid topology (circular - should fail)
    echo -e "\n${YELLOW}Testing error handling...${NC}"
    send_topology_update '[[0]]' \
        "Invalid: Circular dependency (should fail)"
    
    # Scenario 5: Back to simple topology
    send_topology_update '[[1], []]' \
        "Simple: Group 0 → Group 1"
    
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}       Demo Complete${NC}"
    echo -e "${BLUE}========================================${NC}"
}

# Show logs
show_logs() {
    echo -e "\n${YELLOW}Recent logs from Replica 0:${NC}"
    tail -20 "$LOG_DIR/replica0.log" 2>/dev/null || echo "No logs available"
}

# Main
main() {
    local auto_mode=false
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --auto|-a)
                auto_mode=true
                shift
                ;;
            *)
                shift
                ;;
        esac
    done
    
    print_banner
    
    # Clean any previous runs
    pkill -f "byzcast-tcc.*demo-config" 2>/dev/null || true
    rm -rf "$CONFIG_DIR" "$LOG_DIR"
    sleep 2  # Wait for ports to be released
    
    check_prerequisites
    create_config
    start_replicas
    run_demo
    show_logs
    
    if [ "$auto_mode" = true ]; then
        echo -e "\n${GREEN}Auto mode: Cleaning up automatically...${NC}"
    else
        echo -e "\n${YELLOW}Press Enter to stop the demo and cleanup...${NC}"
        read -r
    fi
    
    # Explicit cleanup before exit (trap will also run)
    echo -e "\n${YELLOW}Stopping replicas...${NC}"
    pkill -f "byzcast-tcc.*demo-config" 2>/dev/null || true
    sleep 2  # Wait for processes to terminate
    
    # Verify cleanup
    REMAINING=$(pgrep -f "byzcast-tcc.*demo-config" 2>/dev/null | wc -l)
    if [ "$REMAINING" -gt 0 ]; then
        echo -e "${RED}Warning: $REMAINING processes still running, forcing kill...${NC}"
        pkill -9 -f "byzcast-tcc.*demo-config" 2>/dev/null || true
        sleep 1
    fi
    
    echo -e "${GREEN}All processes stopped${NC}"
}

main "$@"
