package dev.agst.byzcast.client;

import java.util.List;
import java.util.UUID;

record Stat(UUID id, long beforeRequest, long afterRequest, int lca, List<Integer> targetGroups) {}
