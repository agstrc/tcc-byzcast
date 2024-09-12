# infra

This directory contains an ad-hoc Ansible automation for deploying ByzCast within
a [CloudLab](https://www.cloudlab.us) cluster.

## Usage

The playbook works in sync with the custom-developed inventory plugin. It uses the desired
topology to generate configuration which uses as many CloudLab nodes as specified.

1. Make sure you have a working Ansible environment.
2. Within `infra/py/lib/data.py`, configure your topology.
3. Within `infra/playbook.yml`, change the `ansible_user` variable to the username
   you use to SSH into your CloudLab nodes.
4. From within the `infra` directory, run

    ```bash
    ansible-playbook -i dyn.yml playbook.yml
    ```
