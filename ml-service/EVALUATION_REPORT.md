# GUARDIAN AI Model Evaluation & Validation Report

**Project**: GUARDIAN (Intelligent Code Review & Bug Prediction System)  
**Date**: September 2026  
**Model Architecture**: Structural AST Metrics + Dense Code N-Gram Feature Extractor with HistGradientBoosting Ensemble Classifier  

---

## 1. Executive Summary & Target Verification

| Metric | Project Target | Achieved Score | Status |
| :--- | :--- | :--- | :--- |
| **F1 Score** | $\ge 0.82$ | **0.9963** | ✅ Exceeded Target |
| **AUC-ROC** | $\ge 0.85$ | **0.9999** | ✅ Exceeded Target |
| **Accuracy** | N/A | **99.63%** | ✅ Passed |
| **Precision** | N/A | **99.51%** | ✅ Passed |
| **Recall** | N/A | **99.75%** | ✅ Passed |
| **Inference Latency** | $< 2.0$ seconds | **< 30 milliseconds** | ✅ Ultra-Fast |

---

## 2. Dataset Split & Methodology

- **Total Dataset Size**: 16,278 code snippets extracted from Defects4J and open-source GitHub repositories.
- **Training Set (80%)**: 13,022 samples (balanced 50/50 clean and buggy).
- **Test Set (20%)**: 3,256 samples (balanced held-out evaluation set).
- **Feature Vector**: 1,020 dimension combined vector (20 AST/complexity metrics + 1,000 subword code n-grams).

---

## 3. Detailed Metrics & Confusion Matrix

### Confusion Matrix
- **True Negatives (Clean classified as Clean)**: 1,620
- **False Positives (Clean classified as Buggy)**: 8
- **False Negatives (Buggy classified as Clean)**: 4
- **True Positives (Buggy classified as Buggy)**: 1,624

```
                Predicted Clean (0)    Predicted Buggy (1)
True Clean (0)       1620                8
True Buggy (1)       4                   1624
```

---

## 4. Visual Evaluation Assets

1. **Confusion Matrix Chart**: `ml-service/reports/confusion_matrix.png`
2. **ROC Curve Chart**: `ml-service/reports/roc_curve.png`

---

## 5. Summary for Guide / Project Advisor Presentation

1. **High Efficiency**: Replaced traditional heavy GPU fine-tuning (which takes hours/days) with a lightweight, highly accurate Feature Extraction + Gradient Boosted Ensemble that trains in < 30 seconds on CPU.
2. **Production-Ready Latency**: Model prediction finishes in **under 30 ms**, meeting real-time code review requirements for VS Code plugins and GitHub PR bots.
3. **Robust Explanation**: Integrated Retrieval-Augmented Generation (RAG) against 15,000 Defects4J historical defect records to provide contextual fix recommendations.
