from py.lib import Host

# This file contains the configuration for the hosts that will be used in the
# ByzCast experiment. Given the defined hosts, groups will be created which look to use
# as many hosts as possible per group.

hosts = [
    Host(external="apt156.apt.emulab.net", internal="10.10.1.1"),
    Host(external="apt148.apt.emulab.net", internal="10.10.1.2"),
    # Host(external="apt173.apt.emulab.net", internal="10.1.1.3"),
    # Host(external="apt179.apt.emulab.net", internal="10.1.1.4"),
    # Host(external="apt192.apt.emulab.net", internal="10.1.1.5"),
]
num_groups = 4
group_size = 4
