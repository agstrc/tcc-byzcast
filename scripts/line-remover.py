import sys

def remove_last_10_lines(file_path):
    with open(file_path, 'r') as file:
        lines = file.readlines()
    
    if len(lines) <= 10:
        print(f"File {file_path} has less than or equal to 10 lines, skipping.")
        return
    
    with open(file_path, 'w') as file:
        file.writelines(lines[:-10])

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python remove_last_10_lines.py <file1> <file2> ... <fileN>")
        sys.exit(1)

    file_paths = sys.argv[1:]
    for file_path in file_paths:
        remove_last_10_lines(file_path)

