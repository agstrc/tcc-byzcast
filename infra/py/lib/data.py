from .byzcastcfg import Host

# This file contains the configuration for the hosts that will be used in the
# ByzCast experiment. Given the defined hosts, groups will be created which look to use
# as many hosts as possible per group.
# As per the generate_configs function, the groups will start from index 0.

hosts = [
    Host(external="example1.com", internal="10.10.1.1"),
    Host(external="example2.com", internal="10.10.1.2"),
    Host(external="example3.com", internal="10.10.1.3"),
    Host(external="example4.com", internal="10.10.1.4"),
]
num_groups = 4
group_size = 4


# The topology list defines the connectivity between groups.
# Each index in the list represents a group, and the sublist at each index
# contains the indices of the groups that the group at that index is connected to.
# For example:
# topology[0] = [1, 2] means that group 0 is connected to groups 1 and 2.
# topology[1] = [3, 4] means that group 1 is connected to groups 3 and 4.
topology = [[1, 2], [3, 4]]
