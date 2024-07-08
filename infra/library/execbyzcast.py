import datetime
import os
import re
import shutil
import subprocess

from ansible.module_utils.basic import AnsibleModule


def execbyzcast():
    module = AnsibleModule(
        argument_spec=dict(
            gs=dict(
                type="str", required=True, documentation="The value of gs parameter."
            )
        ),
    )

    gs: str = module.params["gs"]
    pairs = gs.split(" ")

    log_file_pattern = re.compile(r"g\d+_s\d+\.log")
    log_files = [file for file in os.listdir() if log_file_pattern.match(file)]

    for log_file in log_files:
        creation_time = os.path.getctime(log_file)
        creation_date = datetime.datetime.fromtimestamp(creation_time).strftime(
            "%Y-%m-%d"
        )
        new_name = f"{creation_date}_{log_file}"
        os.rename(log_file, new_name)

    java_path = shutil.which("java")
    if java_path is None:
        module.fail_json(msg="Java is not installed on the system.")
        return

    for pair in pairs:
        server_id, group_id = pair.split(",")

        log_file = open(f"g{group_id}_s{server_id}.log", "w")

        subprocess.Popen(
            [
                java_path,
                "-jar",
                "byzcast/byzcast.jar",
                "--server-id",
                server_id,
                "--group-id",
                group_id,
            ],
            stdout=log_file,
            stderr=log_file,
            preexec_fn=os.setsid,
        )

    module.exit_json(changed=True)


if __name__ == "__main__":
    execbyzcast()
