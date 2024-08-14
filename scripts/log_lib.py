import re
from dataclasses import dataclass
from typing import TextIO, TypeVar

T = TypeVar("T")


def get_all_pairs(lst: list[T]) -> list[tuple[T, T]]:
    pairs: list[tuple[T, T]] = []
    for i in range(len(lst)):
        for j in range(i + 1, len(lst)):
            pairs.append((lst[i], lst[j]))
    return pairs


def unordered_common_elements(lst1: list[T], lst2: list[T]) -> tuple[T, T] | None:
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
    exp = re.compile(r"RID=(\w{8}-\w{4}-\w{4}-\w{4}-\w{12}).+Request locally handled")

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


@dataclass
class ParsedLog:
    file_name: str
    requests: list[str]


def map_messages(parsed_logs: list[ParsedLog]) -> dict[str, list[str]]:
    request_to_next: dict[str, list[str]] = {}
    for pl in parsed_logs:
        for index, request_id in enumerate(pl.requests):
            next_request = (
                pl.requests[index + 1] if index + 1 < len(pl.requests) else None
            )
            next_request_list = request_to_next.get(request_id, None)

            if next_request_list is None:
                lst = [] if next_request is None else [next_request]
                request_to_next[request_id] = lst
            elif next_request is not None:
                next_request_list.append(next_request)

    return request_to_next


def parse_all_files(file_paths: list[str]):
    for path in file_paths:
        with open(path, "r") as f:
            requests = parse_log_file(f)
            yield ParsedLog(file_name=path, requests=requests)
