from dataclasses import dataclass
from itertools import cycle


@dataclass
class Host:
    external: str
    internal: str


@dataclass
class HostConfig(Host):
    server_id: int
    port: int


@dataclass
class GroupConfig:
    group_num: int
    config_entries: list[HostConfig]


class PortGenerator:
    _map: dict[str, int]

    def __init__(self):
        self._map = {}

    def get_port(self, host: str):
        port = self._map.get(host, None)
        if port is None:
            port = 10000
            self._map[host] = port + 10
        else:
            self._map[host] = port + 10

        return port


def generate_configs(hosts: list[Host], num_groups: int, group_size: int):
    cyclical_hosts = cycle(hosts)
    configs: "list[GroupConfig]" = []

    port_gen = PortGenerator()

    for group_num in range(num_groups):
        config_entries: "list[HostConfig]" = []

        for server_id in range(group_size):
            host = next(cyclical_hosts)
            port = port_gen.get_port(host.external)
            config_entries.append(
                HostConfig(
                    external=host.external,
                    internal=host.internal,
                    server_id=server_id,
                    port=port,
                )
            )

        configs.append(GroupConfig(group_num, config_entries))

    return configs
