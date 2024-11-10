import sys
import pandas as pd

def calculate_average_latency(file_paths):
    total_latency = 0
    total_entries = 0

    for file_path in file_paths:
        # Read the file into a DataFrame
        df = pd.read_csv(file_path, delimiter='\t')
        
        # Sum the latency and count the number of entries
        total_latency += df['LATENCY'].sum()
        total_entries += len(df)

    if total_entries == 0:
        print("No valid entries found.")
        return

    average_latency = total_latency / total_entries
    print(f"Average Latency: {average_latency:.2f}")
    print(f"Total Entries: {total_entries}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python calculate_average_latency.py <file1> <file2> ... <fileN>")
        sys.exit(1)

    file_paths = sys.argv[1:]
    calculate_average_latency(file_paths)