import argparse
import re
import sys
from pathlib import Path

project_root = Path(__file__).resolve().parent.parent.resolve()
sys.path.insert(0, str(project_root))


from scripts.log_lib import (  # noqa: E402
    find_cycles,
    get_all_pairs,
    map_messages,
    parse_all_files,
    unordered_common_elements,
)


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
        description="Analyze ByzCast logs for cycles and differing orders"
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

        l1g1d = str(log1_group_id)
        l2g1d = str(log2_group_id)

        if l1g1d == l2g1d:
            continue

        s1, s2 = set(log1.requests), set(log2.requests)
        intersection = s1.intersection(s2)

        if len(intersection) == 0:
            print("NO INTERSECTION", log1.file_name, log2.file_name)
            continue

        unordered = unordered_common_elements(log1.requests, log2.requests)
        if unordered:
            print("UNORDERED", log1.file_name, log2.file_name, f"intersection_size={len(intersection)}", unordered)
        else:
            print("OK", log1.file_name, log2.file_name, f"intersection_size={len(intersection)}")

    print("Checking for cycles")

    mapping = map_messages(parsed_logs)
    cycles = find_cycles(mapping)

    if cycles:
        print("Cycles found:")
        for cycle in cycles:
            print(cycle)
    else:
        print("No cycles found")


if __name__ == "__main__":
    main()
