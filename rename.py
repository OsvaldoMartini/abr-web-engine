import os

def list_files(directory):
    """Lists files in the given directory."""
    try:
        return [f for f in os.listdir(directory) if os.path.isfile(os.path.join(directory, f))]
    except FileNotFoundError:
        print("Directory not found.")
        return []

def rename_abr_files(directory):
    """Renames all files starting with 'AR' to start with 'AR' instead."""
    files = list_files(directory)
    if not files:
        print("No files found.")
        return
    
    for file in files:
        if file.startswith("AR"):
            new_name = "AR" + file[3:]  # Replace 'AR' with 'AR'
            old_path = os.path.join(directory, file)
            new_path = os.path.join(directory, new_name)
            try:
                os.rename(old_path, new_path)
                print(f"Renamed {file} to {new_name}")
            except Exception as e:
                print(f"Error renaming {file}: {e}")

if __name__ == "__main__":
    folder_path = input("Enter the folder path: ").strip()
    rename_abr_files(folder_path)
