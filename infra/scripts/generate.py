from pathlib import Path
from data import hosts, num_groups, group_size
from lib import generate_configs, HostConfig
import shutil
import os

configs = generate_configs(hosts, num_groups, group_size)

# infra dir
base_path = Path(__file__).parent.parent


configs_dir = base_path.joinpath("configs")
shutil.rmtree(configs_dir, ignore_errors=True)

for group_config in configs:
    group_num = str(group_config.group_num).zfill(2)
    group_cfg_dir = configs_dir.joinpath(f"g{group_num}")

    os.makedirs(group_cfg_dir, exist_ok=True)

    hosts_file = group_cfg_dir.joinpath("hosts.config")
    with open(hosts_file, "w") as file:
        for config_entry in group_config.config_entries:
            file.write(
                f"{config_entry.server_id} {config_entry.internal} {config_entry.port}\n"
            )

    system_file = group_cfg_dir.joinpath("system.config")
    shutil.copy(
        Path(__file__).parent.joinpath("example_system_config.txt"), system_file
    )


hosts = {host.external for host in hosts}
inventory_file_path = base_path.joinpath("inventory.ini")

# flattened_configs = [config for group in configs for config in group.config_entries]

flattened_configs: "list[tuple[int, HostConfig]]" = []
for group_config in configs:
    for config in group_config.config_entries:
        flattened_configs.append((group_config.group_num, config))

with open(inventory_file_path, "w") as inventory_file:
    inventory_file.write("[remote_hosts]\n")
    for host in hosts:
        inventory_file.write(f"{host}")

        host_configs = list(filter(lambda x: x[1].external == host, flattened_configs))
        if len(host_configs) == 0:
            inventory_file.write("\n")
            continue

        inventory_file.write(" params=")
        for group_num, config in host_configs:
            inventory_file.write(f"{config.server_id}-{group_num} ")
        inventory_file.write("\n")


# Add variable that contains group IDs and server IDs

configs_with_group_num = [(cfg.group_num, cfg.config_entries) for cfg in configs]
flattened_configs = [
    (group_num, config)
    for group_num, group in configs_with_group_num
    for config in group
]

hosts = {host[1].external for host in flattened_configs}

inventory_file_path = base_path.joinpath("inventory.ini")

with open(inventory_file_path, "w") as inventory_file:
    inventory_file.write("[remote_hosts]\n")

    for host in hosts:
        inventory_file.write(f"{host}")

        host_configs = list(filter(lambda x: x[1].external == host, flattened_configs))
        if len(host_configs) == 0:
            inventory_file.write("\n")
            continue

        inventory_file.write(" params=")

        param = " ".join(
            f"{cfg.server_id},{group_id}" for group_id, cfg in host_configs
        )
        inventory_file.write(repr(param))
        inventory_file.write("\n")
