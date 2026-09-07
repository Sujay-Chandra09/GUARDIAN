import re
import numpy as np
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
import lizard
import joblib
import os

class CodeFeatureExtractor:
    def __init__(self, max_tfidf_features=500):
        self.max_tfidf_features = max_tfidf_features
        self.vectorizer = TfidfVectorizer(
            max_features=max_tfidf_features,
            token_pattern=r"(?u)\b\w+\b|[^\w\s]",
            ngram_range=(1, 2)
        )
        self.is_fitted = False

    def extract_structural_features(self, code_snippet: str) -> np.ndarray:
        """Extract domain-specific structural metrics and static analysis features."""
        if not isinstance(code_snippet, str) or not code_snippet.strip():
            return np.zeros(20, dtype=np.float32)

        lines = code_snippet.splitlines()
        num_lines = len(lines)
        num_chars = len(code_snippet)
        
        # Lizard complexity check
        try:
            analysis = lizard.analyze_file.analyze_source_code("snippet.java", code_snippet)
            complexity = analysis.average_cyclomatic_complexity if analysis.function_list else 1.0
            num_functions = len(analysis.function_list)
            max_nloc = max([f.nloc for f in analysis.function_list], default=num_lines)
        except Exception:
            complexity = 1.0
            num_functions = 1
            max_nloc = num_lines

        # Structural keyword frequencies
        count_if = len(re.findall(r'\bif\b', code_snippet))
        count_for = len(re.findall(r'\bfor\b', code_snippet))
        count_while = len(re.findall(r'\bwhile\b', code_snippet))
        count_try = len(re.findall(r'\btry\b', code_snippet))
        count_catch = len(re.findall(r'\bcatch\b', code_snippet))
        count_throw = len(re.findall(r'\bthrow\b', code_snippet))
        count_null = len(re.findall(r'\bnull\b', code_snippet))
        count_return = len(re.findall(r'\breturn\b', code_snippet))
        
        # Operators and checks
        count_equals = code_snippet.count("==")
        count_not_equals = code_snippet.count("!=")
        count_null_check = len(re.findall(r'==\s*null|!=\s*null|null\s*==|null\s*!=', code_snippet))
        count_and_or = code_snippet.count("&&") + code_snippet.count("||")
        count_brackets = code_snippet.count("{") + code_snippet.count("[")
        count_comments = sum(1 for line in lines if line.strip().startswith("//") or line.strip().startswith("*"))

        # Ratios
        comment_ratio = count_comments / max(num_lines, 1)
        avg_line_length = num_chars / max(num_lines, 1)

        features = [
            float(num_lines),
            float(num_chars),
            float(complexity),
            float(num_functions),
            float(max_nloc),
            float(count_if),
            float(count_for),
            float(count_while),
            float(count_try),
            float(count_catch),
            float(count_throw),
            float(count_null),
            float(count_return),
            float(count_equals),
            float(count_not_equals),
            float(count_null_check),
            float(count_and_or),
            float(count_brackets),
            float(comment_ratio),
            float(avg_line_length)
        ]
        return np.array(features, dtype=np.float32)

    def fit(self, code_snippets):
        """Fit the TF-IDF vectorizer on code snippets."""
        cleaned_snippets = [s if isinstance(s, str) else "" for s in code_snippets]
        self.vectorizer.fit(cleaned_snippets)
        self.is_fitted = True
        return self

    def transform(self, code_snippets):
        """Transform code snippets into combined structural + dense n-gram feature matrices."""
        cleaned_snippets = [s if isinstance(s, str) else "" for s in code_snippets]
        
        # 1. Extract TF-IDF n-gram features
        if not self.is_fitted:
            raise ValueError("CodeFeatureExtractor must be fitted before calling transform!")
            
        tfidf_feats = self.vectorizer.transform(cleaned_snippets).toarray()
        
        # 2. Extract structural metrics
        struct_feats_list = [self.extract_structural_features(s) for s in cleaned_snippets]
        struct_feats = np.vstack(struct_feats_list)
        
        # 3. Concatenate structural metrics and TF-IDF features
        combined = np.hstack([struct_feats, tfidf_feats])
        return combined

    def save(self, filepath: str):
        os.makedirs(os.path.dirname(filepath), exist_ok=True)
        joblib.dump(self, filepath)

    @classmethod
    def load(cls, filepath: str):
        return joblib.load(filepath)
