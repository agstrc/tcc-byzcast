from py.lib import Host

# This file contains the configuration for the hosts that will be used in the
# ByzCast experiment. Given the defined hosts, groups will be created which look to use
# as many hosts as possible per group.

hosts = [
    Host(external="example.com", internal="10.10.1.1"),
    Host(external="example2.com", internal="10.10.1.2")
]
num_groups = 4
group_size = 4
