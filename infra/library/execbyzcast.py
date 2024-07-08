import shutil
import os
import subprocess

from ansible.module_utils.basic import AnsibleModule

# DOCUMENTATION = """
# ---
# module: execbyzcast
# short_description: A simple Ansible module that prints a greeting message.
# description:
#     - This module takes a parameter 'gs' and prints a greeting message using the value of 'gs'.
# options:
#     gs:
#         description:
#             - The value of gs parameter.
#         required: true
# author:
#     - Your Name
# """


# def hello_world_module():
#     module = AnsibleModule(
#         argument_spec=dict(
#             gs=dict(
#                 type="str", required=True, documentation="The value of gs parameter."
#             )
#         ),
#         supports_check_mode=True,
#     )

#     gs = module.params["gs"]
#     message = f"Hello, {gs}!"

#     module.exit_json(changed=False, message=message)


# if __name__ == "__main__":
#     hello_world_module()


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

    java_path = shutil.which("java")
    if java_path is None:
        module.fail_json(msg="Java is not installed on the system.")
        return

    for pair in pairs:
        server_id, group_id = pair.split(",")

        log_file = open(f"g{group_id}_s{server_id}.log", "w")
        # subprocess.Popen(java_path)

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
        )
