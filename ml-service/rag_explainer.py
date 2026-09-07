import os
import pandas as pd
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import requests

class RAGExplainer:
    def __init__(self, d4j_path: str = "data-collection/defects4j_raw.csv"):
        self.d4j_path = d4j_path
        self.vectorizer = TfidfVectorizer(max_features=500, token_pattern=r"(?u)\b\w+\b|[^\w\s]")
        self.snippets = []
        self.repos = []
        self.is_indexed = False
        self._build_index()

    def _build_index(self):
        if os.path.exists(self.d4j_path):
            try:
                df = pd.read_csv(self.d4j_path)
                df = df.dropna(subset=['code_snippet'])
                self.snippets = df['code_snippet'].tolist()
                self.repos = df['repo'].tolist() if 'repo' in df.columns else ['Defects4J'] * len(df)
                if self.snippets:
                    self.matrix = self.vectorizer.fit_transform(self.snippets)
                    self.is_indexed = True
                    print(f"RAG Index built with {len(self.snippets)} historical bug patterns.")
            except Exception as e:
                print(f"Warning: Failed to index RAG database ({e})")

    def retrieve_similar_bugs(self, code_snippet: str, top_k: int = 2):
        """Retrieve top-K similar historical bug patterns from Defects4J."""
        if not self.is_indexed or not self.snippets:
            return []

        try:
            vec = self.vectorizer.transform([code_snippet])
            sims = cosine_similarity(vec, self.matrix).flatten()
            top_indices = sims.argsort()[-top_k:][::-1]

            results = []
            for idx in top_indices:
                if sims[idx] > 0.05:
                    results.append({
                        "similarity": float(sims[idx]),
                        "repo": self.repos[idx],
                        "snippet": self.snippets[idx]
                    })
            return results
        except Exception:
            return []

    def generate_explanation(self, code_snippet: str, risk_score: float) -> dict:
        """Generate RAG-enhanced natural language bug explanation and fix suggestion."""
        similar_bugs = self.retrieve_similar_bugs(code_snippet, top_k=2)

        # Build prompt context
        few_shot_context = ""
        if similar_bugs:
            few_shot_context = "\nSimilar Historical Bugs Found:\n"
            for i, bug in enumerate(similar_bugs, 1):
                few_shot_context += f"Example {i} ({bug['repo']}):\n```java\n{bug['snippet'][:200]}...\n```\n"

        # Heuristic explanation fallback when API key isn't provided
        api_key = os.environ.get("GEMINI_API_KEY") or os.environ.get("OPENAI_API_KEY")

        if api_key and os.environ.get("GEMINI_API_KEY"):
            try:
                # Call Gemini API if key present
                url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key={api_key}"
                prompt = (
                    f"You are an expert code auditor. Analyze this code with predicted bug risk {risk_score*100:.1f}%.\n"
                    f"{few_shot_context}\n"
                    f"Code Snippet:\n```java\n{code_snippet}\n```\n"
                    f"Provide: 1) High-level issue, 2) Root cause, 3) Suggested Fix in Markdown."
                )
                payload = {"contents": [{"parts": [{"text": prompt}]}]}
                res = requests.post(url, json=payload, timeout=5)
                if res.status_code == 200:
                    text = res.json()['candidates'][0]['content']['parts'][0]['text']
                    return {
                        "explanation": text,
                        "retrieved_context_count": len(similar_bugs),
                        "similar_bugs": similar_bugs
                    }
            except Exception as e:
                print(f"LLM API call failed, falling back to local analysis: {e}")

        # Deterministic analysis based on structural indicators
        issue = "Potential Bug Detected"
        suggestion = "Review boundary conditions and check for null object references."

        if "null" in code_snippet or "NPE" in code_snippet:
            issue = "NullPointer Exception Risk"
            suggestion = "Add a null check validation `if (obj != null)` before accessing properties or methods."
        elif "length" in code_snippet or "charAt" in code_snippet or "index" in code_snippet:
            issue = "Index Out Of Bounds / Off-by-One Error Risk"
            suggestion = "Verify array or string boundaries before accessing elements by index."
        elif "catch" in code_snippet and "Exception" in code_snippet:
            issue = "Swallowing Exception / Silent Failure Risk"
            suggestion = "Ensure caught exceptions are properly logged or rethrown instead of ignored."

        explanation_markdown = (
            f"### Bug Risk Warning (Risk Score: {risk_score*100:.1f}%)\n"
            f"**Detected Risk Pattern**: {issue}\n\n"
            f"**Recommended Fix**: {suggestion}\n\n"
            f"**RAG Context**: Retrieved {len(similar_bugs)} matching patterns from historical defect logs."
        )

        return {
            "explanation": explanation_markdown,
            "retrieved_context_count": len(similar_bugs),
            "similar_bugs": similar_bugs
        }
