from .byzcastcfg import Host

# This file contains the configuration for the hosts that will be used in the
# ByzCast experiment. Given the defined hosts, groups will be created which look to use
# as many hosts as possible per group.
# As per the generate_configs function, the groups will start from index 0.

hosts = [
    Host(external="apt144.apt.emulab.net", internal="10.10.1.1"),
    Host(external="apt148.apt.emulab.net", internal="10.10.1.2"),
    Host(external="apt066.apt.emulab.net", internal="10.10.1.3"),
    Host(external="apt129.apt.emulab.net", internal="10.10.1.4"),
    Host(external="apt156.apt.emulab.net", internal="10.10.1.5"),
    Host(external="apt139.apt.emulab.net", internal="10.10.1.6"),
    Host(external="apt149.apt.emulab.net", internal="10.10.1.7"),
    Host(external="apt157.apt.emulab.net", internal="10.10.1.8"),
    Host(external="apt142.apt.emulab.net", internal="10.10.1.9"),
    Host(external="apt146.apt.emulab.net", internal="10.10.1.10"),
    Host(external="apt075.apt.emulab.net", internal="10.10.1.11"),
    Host(external="apt159.apt.emulab.net", internal="10.10.1.12"),
    Host(external="apt132.apt.emulab.net", internal="10.10.1.13"),
    Host(external="apt153.apt.emulab.net", internal="10.10.1.14"),
    Host(external="apt174.apt.emulab.net", internal="10.10.1.15"),
    Host(external="apt133.apt.emulab.net", internal="10.10.1.16"),
]
num_groups = 4
group_size = 4


# The topology list defines the connectivity between groups.
# Each index in the list represents a group, and the sublist at each index
# contains the indices of the groups that the group at that index is connected to.
# For example:
# topology[0] = [1, 2] means that group 0 is connected to groups 1 and 2.
# topology[1] = [3, 4] means that group 1 is connected to groups 3 and 4.
topology = [[1, 2, 3]]
