import binascii
import json
import os
import shutil
import subprocess
from base64 import b64decode
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path

from ansible.module_utils.basic import AnsibleModule  # type: ignore

DOCUMENTATION = """
---
module: startbyzcast
short_description: Start Byzcast servers on remote machines
description:
    - This module starts the Byzcast servers on remote machines based on their group IDs and server IDs.
options:
    gs:
        description:
            - Group and server IDs passed as base64 encoded JSON. For more information, check the inventory plugin.
              The 'gs' parameter stands for groups servers and must contain a base64 encoded JSON string. This JSON
              string should be an array of JSON objects, where each object must have a 'server_id' and 'group_id' key.
        required: true
        type: str
"""


@dataclass
class HostVar:
    server_id: int
    group_id: int


def main() -> None:
    module = AnsibleModule(
        argument_spec=dict(
            gs=dict(
                type="str",
                required=True,
                documentation="Group and server IDs as described in the inventory",
            )
        ),
    )

    java_path = shutil.which("java")
    if java_path is None:
        return module.fail_json(msg="Java was not found in PATH")

    gs: str = module.params["gs"]
    decoded_host_vars: list[HostVar] = []

    try:
        host_vars = b64decode(gs).decode()
        host_vars = json.loads(host_vars)
    except (json.JSONDecodeError, binascii.Error) as e:
        return module.fail_json(
            msg="Invalid JSON string",
            gs=gs,
            detail=e,
        )

    if not isinstance(host_vars, list):
        return module.fail_json(msg="JSON string is not an array", gs=gs)

    for host_var in host_vars:
        if not isinstance(host_var, dict):
            return module.fail_json(msg="Array contains non-object element", gs=gs)

        try:
            host_var = HostVar(**host_var)
            decoded_host_vars.append(host_var)
        except TypeError as e:
            return module.fail_json(
                msg="Array contains invalid object",
                gs=gs,
                object=json.dumps(host_var),
                detail=e,
            )

    # I'd like to set file and dir names constants within the distribute script in order
    # to reference them here, but this module runs by itself in the remote machines, so
    # it doesn't have access to the distribute script.

    base_dir = Path("byzcast")

    now = datetime.now()
    # The Ansible playbook clears the directory before running this module, through
    # rsync, so the logs must stay in a separate directory.
    log_dir = Path("byzcast-logs").joinpath(f"{now.isoformat()}.logs")
    os.makedirs(log_dir, exist_ok=True)

    # The symlinks makes it easier to access the logs of the last aexecution
    symlink_path = base_dir.joinpath("latest.logs")
    if symlink_path.exists():
        symlink_path.unlink()
    symlink_path.symlink_to(log_dir.resolve())

    for host_var in decoded_host_vars:
        server_id, group_id = host_var.server_id, host_var.group_id

        log_file_path = Path.joinpath(log_dir, f"g{group_id}_s{server_id}.log")
        log_file = open(log_file_path, "w")

        # We start each server in a separate process and detach them from the Python
        # process.
        subprocess.Popen(
            [
                java_path,
                "-jar",
                str(base_dir.joinpath("byzcast-tcc.jar")),
                "--groups-configs",
                str(base_dir.joinpath("configs")),
                "--topology",
                str(base_dir.joinpath("topology.json")),
                "server",
                "--server-id",
                str(server_id),
                "--group-id",
                str(group_id),
            ],
            stdout=log_file,
            stderr=log_file,
            preexec_fn=os.setsid,
        )

    module.exit_json(changed=True)


if __name__ == "__main__":
    main()
