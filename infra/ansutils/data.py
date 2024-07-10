from ansutils.lib import Host

# This file contains the configuration for the hosts that will be used in the
# ByzCast experiment. Given the defined hosts, groups will be created which look to use
# as many hosts as possible per group.

hosts = [
    Host(external="apt156.apt.emulab.net", internal="10.10.1.1"),
    Host(external="apt162.apt.emulab.net", internal="10.10.1.2"),
    Host(external="apt141.apt.emulab.net", internal="10.10.1.3"),
    Host(external="apt148.apt.emulab.net", internal="10.10.1.4")
]
num_groups = 4
group_size = 4
