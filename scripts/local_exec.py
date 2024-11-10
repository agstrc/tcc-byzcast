import json
import os
import shutil
import subprocess
from pathlib import Path

# TODO: allow for a different number of faulty servers
topology = [[1, 2], [], [3]]

group_size = 4
group_count = 4

exec_dir = Path(__file__).parent.joinpath("lexec")
project_dir = Path(__file__).parent.parent

shutil.rmtree(exec_dir, ignore_errors=True)
exec_dir.mkdir(parents=True)

current_port = 40000

for group in range(group_count):
    group_id_in_dir = str(group).zfill(2)
    group_cfg_dir = exec_dir.joinpath(f"g{group_id_in_dir}")
    group_cfg_dir.mkdir()

    hosts_cfg_path = group_cfg_dir.joinpath("hosts.config")
    system_cfg_path = group_cfg_dir.joinpath("system.config")

    with open(hosts_cfg_path, "w") as f:
        for sid in range(group_size):
            f.write(f"{sid} 127.0.0.1 {current_port}\n")
            current_port += 10

    cfg_path = project_dir.joinpath("infra/assets/example_system_config.txt")
    shutil.copy(
        cfg_path,
        system_cfg_path,
    )

topology_path = exec_dir.joinpath("topology.json")
with open(topology_path, "w") as f:
    json.dump(topology, f)

java_path = shutil.which("java")
assert java_path

for group in range(group_count):
    for sid in range(group_size):
        log_file_path = exec_dir.joinpath(f"g{group}_s{sid}.log")
        log_file = open(log_file_path, "w")

        jar_path = project_dir.joinpath(
            "target/byzcast-tcc-1.0-SNAPSHOT-jar-with-dependencies.jar"
        )
        process = subprocess.Popen(
            [
                java_path,
                "-jar",
                str(jar_path),
                "--groups-configs",
                exec_dir,
                "--topology",
                topology_path,
                "server",
                "--server-id",
                str(sid),
                "--group-id",
                str(group),
            ],
            stdout=log_file,
            stderr=log_file,
            preexec_fn=os.setsid,
        )

        pids_path = exec_dir.joinpath("pids")
        with open(pids_path, "a") as f:
            f.write(f"{process.pid}\n")
