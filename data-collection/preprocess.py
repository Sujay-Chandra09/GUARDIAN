import os
import pandas as pd
from sklearn.model_selection import train_test_split

def main():
    raw_scraped_path = "data-collection/scraped_raw.csv"
    raw_d4j_path = "data-collection/defects4j_raw.csv"
    
    train_out_path = "data-collection/train_dataset.csv"
    test_out_path = "data-collection/test_dataset.csv"
    
    # Load raw data
    dfs = []
    if os.path.exists(raw_scraped_path):
        dfs.append(pd.read_csv(raw_scraped_path))
        print(f"Loaded {len(dfs[-1])} scraped records.")
    else:
        print(f"Warning: {raw_scraped_path} not found.")
        
    if os.path.exists(raw_d4j_path):
        dfs.append(pd.read_csv(raw_d4j_path))
        print(f"Loaded {len(dfs[-1])} Defects4J records.")
    else:
        print(f"Warning: {raw_d4j_path} not found.")
        
    if not dfs:
        raise ValueError("No raw dataset files found to preprocess!")
        
    # Merge datasets
    df = pd.concat(dfs, ignore_index=True)
    print(f"Merged raw dataset size: {len(df)}")
    
    # 1. Clean duplicates and missing values
    df.dropna(subset=['code_snippet', 'label'], inplace=True)
    df.drop_duplicates(subset=['code_snippet'], inplace=True)
    print(f"Size after deduplication: {len(df)}")
    
    # 2. Balance classes
    buggy_df = df[df['label'] == 1]
    clean_df = df[df['label'] == 0]
    
    print(f"Class counts - Clean (0): {len(clean_df)}, Buggy (1): {len(buggy_df)}")
    
    # Determine minority class count
    min_size = min(len(clean_df), len(buggy_df))
    
    # Undersample the majority class to get 50/50 balance
    clean_balanced = clean_df.sample(n=min_size, random_state=42)
    buggy_balanced = buggy_df.sample(n=min_size, random_state=42)
    
    balanced_df = pd.concat([clean_balanced, buggy_balanced], ignore_index=True)
    # Shuffle the dataset
    balanced_df = balanced_df.sample(frac=1, random_state=42).reset_index(drop=True)
    
    print(f"Balanced dataset size: {len(balanced_df)} (Clean: {min_size}, Buggy: {min_size})")
    
    # Ensure total samples are within our targets (15,000 - 25,000)
    # If the balanced dataset is slightly outside or larger, let's keep it capped to at most 20,000
    # to maintain model speed while satisfying targets.
    if len(balanced_df) > 20000:
        balanced_df = balanced_df.sample(n=20000, random_state=42).reset_index(drop=True)
        print(f"Capped balanced dataset to 20,000 records (Clean: {len(balanced_df[balanced_df['label']==0])}, Buggy: {len(balanced_df[balanced_df['label']==1])})")
    
    # 3. Train-Test Split (80% train / 20% test)
    train_df, test_df = train_test_split(
        balanced_df, 
        test_size=0.20, 
        random_state=42, 
        stratify=balanced_df['label']
    )
    
    print(f"Train dataset size: {len(train_df)}")
    print(f"Test dataset size: {len(test_df)}")
    
    # Save processed files
    train_df.to_csv(train_out_path, index=False)
    test_df.to_csv(test_out_path, index=False)
    
    print(f"Successfully exported datasets:\n  - {train_out_path}\n  - {test_out_path}")

if __name__ == "__main__":
    main()
