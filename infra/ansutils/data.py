from ansutils.lib import Host

# This file contains the configuration for the hosts that will be used in the
# ByzCast experiment. Given the defined hosts, groups will be created which look to use
# as many hosts as possible per group.
# As per the generate_configs method, the groups will start from index 0.

hosts = [
    Host(external="example1.com", internal="10.10.1.1"),
    Host(external="example2.com", internal="10.10.1.2"),
    Host(external="example3.com", internal="10.10.1.3"),
    Host(external="example4.com", internal="10.10.1.4"),
]
num_groups = 4
group_size = 4
