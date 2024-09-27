import sys

def check_log_order(file_path):
    with open(file_path, 'r') as file:
        lines = file.readlines()

    found_not_in_cache = False
    for line in lines:
        if '[312ec936-bbb4-4e81-ad8e-de75aefd3e48]' in line:
            if 'Request not found in cache' in line:
                found_not_in_cache = True
            elif 'All requests found in cache' in line:
                if found_not_in_cache:
                    found_not_in_cache = False
                else:
                    return False
    return not found_not_in_cache

def main():
    if len(sys.argv) < 2:
        print("Usage: python check_logs.py <file1> <file2> ...")
        sys.exit(1)

    files = sys.argv[1:]
    for file in files:
        if not check_log_order(file):
            print(f"File {file} does not meet the criteria.")
            sys.exit(1)

    print("All files meet the criteria.")

if __name__ == "__main__":
    main()

