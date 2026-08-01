# Data Collection Module

This module handles scraping codebase commit histories and extracting Defects4J data to assemble the dataset required for training code review and bug prediction models.

## Structure
- `scraper.py`: Uses `PyDriller` to crawl commit history across python and java repositories (Django, Flask, Requests, Spring Boot, Commons Lang). It flags buggy code snippets by checking commit messages for bug keywords and extracts function/method-level code snippets.
- `defects4j_extractor.py`: Extracts known java bug patterns corresponding to Defects4J database targets.
- `preprocess.py`: Deduplicates samples, balances the dataset to a 50/50 clean vs buggy ratio, performs an 80/20 train/test split, and outputs final files.
- `train_dataset.csv`: Processed training partition.
- `test_dataset.csv`: Processed test partition.

## Getting Started
1. Activate virtual environment:
   ```bash
   source venv/bin/activate
   ```
2. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
3. Run the collection & preprocessing pipeline:
   ```bash
   python scraper.py
   python defects4j_extractor.py
   python preprocess.py
   ```
