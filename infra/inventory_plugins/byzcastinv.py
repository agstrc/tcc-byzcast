import json
import sys
from pathlib import Path
from typing import TypedDict
from base64 import b64encode

from ansible.plugins.inventory import BaseInventoryPlugin  # type: ignore

infra_dir = Path(__file__).resolve().parents[1]
sys.path.append(str(infra_dir))

from py.lib.data import group_size, hosts, num_groups  # type: ignore  # noqa: E402
from py.lib.byzcastcfg import generate_configs  # type: ignore  # noqa: E402


class HostVar(TypedDict):
    group_id: int
    server_id: int


# This plugin generates hosts and their variables based on the "generate_configs" function
# and the hosts defined in the "hosts" variable.
class InventoryModule(BaseInventoryPlugin):
    NAME = "byzcastinv"

    def verify_file(self, path):
        # This plugin generates the whole of the inventory.
        return True

    def parse(self, inventory, loader, path, cache=True) -> None:
        super(InventoryModule, self).parse(inventory, loader, path, cache)

        configs = generate_configs(hosts, num_groups, group_size)
        hosts_vars: dict[str, list[HostVar]] = {}

        unique_hosts: set[str] = set()

        for group_config in configs:
            for host_config in group_config.config_entries:
                unique_hosts.add(host_config.external)

                host_vars = hosts_vars.get(host_config.external, None)
                if host_vars is None:
                    host_vars = []
                    hosts_vars[host_config.external] = host_vars
                host_vars.append(
                    {
                        "group_id": group_config.group_id,
                        "server_id": host_config.server_id,
                    }
                )

        inventory.add_group("remote_hosts")
        for host in unique_hosts:
            inventory.add_host(host, "remote_hosts")

        # The variable for each hosts defines all server workers they have to
        # execute. They consist of pairs of group IDs and server IDs.

        # Due this issue, the JSON gets encoded in base64
        # https://github.com/ansible/ansible/issues/35254
        for host, var in hosts_vars.items():
            json_string = json.dumps(var)
            b64_string = b64encode(json_string.encode()).decode()
            inventory.set_variable(
                host,
                "params",
                b64_string,
            )
