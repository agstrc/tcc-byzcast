from dataclasses import dataclass
from itertools import cycle


@dataclass
class Host:
    """
    Represents a host with external and internal addresses. The external addresses will
    be used by Ansible to connect and configure the hosts. The internal addresses will be
    used by the ByzCast system.

    Attributes:
        external (str): The external address of the host.
        internal (str): The internal address of the host.
    """

    external: str
    internal: str


@dataclass
class HostConfig(Host):
    """
    Represents the configuration of a host within the context of a BFT-SMaRt cluster.

    Attributes:
        server_id (int): The ID of the server.
        port (int): The port number.
    """

    server_id: int
    port: int


@dataclass
class GroupConfig:
    """
    Represents a group configuration. It may be used to generate a single hosts.config
    file for a group.

    Attributes:
        group_id (int): The ID of the group.
        config_entries (list[HostConfig]): A list of host configurations.
    """

    group_id: int
    config_entries: list[HostConfig]


class PortGenerator:
    """
    A class that generates unique ports for each host.

    Methods:
        __init__(): Initializes the PortGenerator object.
        get_port(host: str): Returns the port associated with the given host.
    """

    _map: dict[str, int]

    def __init__(self):
        self._map = {}

    def get_port(self, host: str):
        """
        Returns the port associated with the given host.

        Args:
            host (str): The host for which to retrieve the port.

        Returns:
            int: The port associated with the given host.
        """
        port = self._map.get(host, None)
        if port is None:
            port = 10000
            self._map[host] = port + 10
        else:
            self._map[host] = port + 10

        return port


def generate_configs(hosts: list[Host], num_groups: int, group_size: int):
    """
    Generate configurations for a given list of hosts, number of groups, and group size.

    Args:
        hosts (list[Host]): A list of Host objects representing the available hosts.
        num_groups (int): The number of groups to generate configurations for.
        group_size (int): The size of each group.

    Returns:
        list[GroupConfig]: A list of GroupConfig objects representing the generated configurations.
    """

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
