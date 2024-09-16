import shutil
from pathlib import Path
import os
import json

from lib.data import group_size, hosts, num_groups, topology
from lib.byzcastcfg import generate_configs

# This scriptspacks and/or generates all required files to be sent to the remote
# hosts. It will generate the hosts.config and system.config files for each group
# and copy the required JAR file and configuration file to the distribution directory.


configs = generate_configs(hosts, num_groups, group_size)

infra_dir = Path(__file__).resolve().parents[1]
dist_path = infra_dir.joinpath("dist")

shutil.rmtree(dist_path, ignore_errors=True)
os.makedirs(dist_path, exist_ok=True)

configs_dir = dist_path.joinpath("configs")
shutil.rmtree(configs_dir, ignore_errors=True)

for group_config in configs:
    group_num = str(group_config.group_id).zfill(2)
    group_cfg_dir = configs_dir.joinpath(f"g{group_num}")

    os.makedirs(group_cfg_dir, exist_ok=True)

    hosts_file = group_cfg_dir.joinpath("hosts.config")
    with open(hosts_file, "w") as file:
        for config_entry in group_config.config_entries:
            file.write(
                f"{config_entry.server_id} {config_entry.internal} {config_entry.port}\n"
            )

    system_file = group_cfg_dir.joinpath("system.config")
    shutil.copy(infra_dir.joinpath("assets", "example_system_config.txt"), system_file)

jar_path = infra_dir.parent.joinpath(
    "target", "byzcast-tcc-1.0-SNAPSHOT-jar-with-dependencies.jar"
)
shutil.copy(jar_path, dist_path.joinpath("byzcast-tcc.jar"))

topology_path = dist_path.joinpath("topology.json")
with open(topology_path, "w") as file:
    file_contents = json.dumps(topology)
    file.write(file_contents)
