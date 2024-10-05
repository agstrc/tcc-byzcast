import argparse
import re
from dataclasses import dataclass
from pathlib import Path
from typing import TextIO, TypeVar

# This script validates whether the ByzCast protocol ensures a consistent message ordering by
# detecting potential cycles or causal violations across different nodes. It checks if the
# protocol adheres to the happens-before relation, as introduced by Lamport, ensuring that
# no cycles exist in the message dependencies between groups. By doing so, it guarantees that messages
# maintain a globally consistent causal order, preventing any out-of-order delivery or
# Byzantine faults that could compromise the protocol’s reliability.

T = TypeVar("T")


@dataclass
class ParsedLog:
    file_name: str
    requests: list[str]


def get_all_pairs(lst: list[T]) -> list[tuple[T, T]]:
    pairs: list[tuple[T, T]] = []
    for i in range(len(lst)):
        for j in range(i + 1, len(lst)):
            pairs.append((lst[i], lst[j]))
    return pairs


def unordered_common_elements(lst1: list[T], lst2: list[T]) -> tuple[T, T] | None:
    """Returns the first pair of elements that are in both lists but in different orders."""

    st1 = set(lst1)
    st2 = set(lst2)

    common_set = st1.intersection(st2)

    common_lst1 = [x for x in lst1 if x in common_set]
    common_lst2 = [x for x in lst2 if x in common_set]

    for x, y in zip(common_lst1, common_lst2):
        if x != y:
            return (x, y)

    return None


def parse_log_file(file: TextIO) -> list[str]:
    exp = re.compile(r"id=(\w{8}-\w{4}-\w{4}-\w{4}-\w{12}).+Request handled locally")

    matches: list[str] = []
    lines = file.readlines()
    for line in lines:
        match = exp.search(line)
        if match:
            matches.append(match.group(1))

    return matches


def find_cycles(mapping: dict[str, list[str]]) -> list[list[str]]:
    cycles = []
    visited = set()

    for start_node in mapping:
        if start_node in visited:
            continue

        stack = [(start_node, [start_node])]
        while stack:
            (node, path) = stack.pop()
            if node in visited:
                continue
            visited.add(node)
            for neighbor in mapping.get(node, []):
                if neighbor in path:
                    cycle_start_index = path.index(neighbor)
                    cycles.append(path[cycle_start_index:] + [neighbor])
                else:
                    stack.append((neighbor, path + [neighbor]))

    return cycles


def map_requests(parsed_logs: list[ParsedLog]) -> dict[str, list[str]]:
    """Maps request IDs to the requests that immediately follow them.
    Most messages should only have one following message, but some may have more than one,
    due to different groups receiving different messages.
    """

    request_to_next: dict[str, list[str]] = {}
    for pl in parsed_logs:
        for index, request_id in enumerate(pl.requests):
            next_request = (
                pl.requests[index + 1] if index + 1 < len(pl.requests) else None
            )
            if next_request is None:
                continue

            next_requests = request_to_next.setdefault(request_id, [])
            if next_request not in next_requests:
                next_requests.append(next_request)

    return request_to_next


def parse_all_files(file_paths: list[str]):
    for path in file_paths:
        with open(path, "r") as f:
            requests = parse_log_file(f)
            yield ParsedLog(file_name=path, requests=requests)


def get_all_log_files(base_path: str) -> list[str]:
    abs_path = Path(base_path).resolve()

    if not abs_path.is_dir():
        raise Exception(f"{base_path} is not a directory")

    log_files: list[str] = []
    for file in abs_path.iterdir():
        if file.is_file() and file.suffix == ".log":
            log_files.append(str(file))

    return log_files


def main() -> None:
    parser = argparse.ArgumentParser(
        description="""
                    Analyze ByzCast logs for cycles and differing orders.
                    It expects log names to be in the format provided by the local execution
                    script.
        """
    )
    parser.add_argument(
        "base_path",
        type=str,
        help="The base directory containing files ending with .log",
    )
    args = parser.parse_args()

    log_files = get_all_log_files(args.base_path)
    log_files.sort()

    parsed_logs = list(parse_all_files(log_files))
    group_id_exp = re.compile(r"g\d+")

    print("Checking for incorrect orders")
    for pls in get_all_pairs(parsed_logs):
        log1, log2 = pls

        log1_group_id = group_id_exp.search(log1.file_name)
        log2_group_id = group_id_exp.search(log2.file_name)

        if log1_group_id is None or log2_group_id is None:
            print(
                "Could not find group ID in file name", log1.file_name, log2.file_name
            )
            continue

        l1g1d = str(log1_group_id.group())
        l2g1d = str(log2_group_id.group())

        if l1g1d == l2g1d:
            continue

        s1, s2 = set(log1.requests), set(log2.requests)
        intersection = s1.intersection(s2)

        if len(intersection) == 0:
            print("NO INTERSECTION", log1.file_name, log2.file_name)
            continue

        unordered = unordered_common_elements(log1.requests, log2.requests)
        if unordered:
            print(
                "UNORDERED",
                log1.file_name,
                log2.file_name,
                f"intersection_size={len(intersection)}",
                unordered,
            )
        else:
            print(
                "OK",
                log1.file_name,
                log2.file_name,
                f"intersection_size={len(intersection)}",
            )

    print("Checking for cycles")

    mapping = map_requests(parsed_logs)
    cycles = find_cycles(mapping)

    if cycles:
        print("Cycles found:")
        for cycle in cycles:
            print(cycle)
    else:
        print("No cycles found")


if __name__ == "__main__":
    main()
