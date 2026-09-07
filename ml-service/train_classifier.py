import os
import joblib
import pandas as pd
import numpy as np
from sklearn.metrics import (
    classification_report,
    confusion_matrix,
    f1_score,
    roc_auc_score,
    precision_score,
    recall_score,
    accuracy_score
)
from sklearn.ensemble import (
    RandomForestClassifier,
    HistGradientBoostingClassifier,
    GradientBoostingClassifier
)

HAS_XGB = False
try:
    import xgboost as xgb
    # Test instantiating to catch dylib load failure
    _dummy = xgb.XGBClassifier()
    HAS_XGB = True
except Exception as e:
    print(f"Notice: XGBoost unavailable ({e}). Using Scikit-Learn Gradient Boosting.")

HAS_LGB = False
try:
    import lightgbm as lgb
    _dummy = lgb.LGBMClassifier()
    HAS_LGB = True
except Exception as e:
    print(f"Notice: LightGBM unavailable ({e}). Using Scikit-Learn Gradient Boosting.")

from feature_extractor import CodeFeatureExtractor

def train_and_evaluate():
    train_path = "data-collection/train_dataset.csv"
    test_path = "data-collection/test_dataset.csv"
    model_dir = "ml-service/models"
    os.makedirs(model_dir, exist_ok=True)

    print("--- Phase 2: AI Bug Prediction Model Training ---")
    print(f"Loading training data from: {train_path}")
    print(f"Loading test data from: {test_path}")

    train_df = pd.read_csv(train_path)
    test_df = pd.read_csv(test_path)

    print(f"Train records: {len(train_df)} | Test records: {len(test_df)}")

    # Initialize and fit Feature Extractor
    print("Extracting code metrics & n-gram features...")
    extractor = CodeFeatureExtractor(max_tfidf_features=1000)
    extractor.fit(train_df['code_snippet'])
    
    X_train = extractor.transform(train_df['code_snippet'])
    y_train = train_df['label'].values

    X_test = extractor.transform(test_df['code_snippet'])
    y_test = test_df['label'].values

    print(f"Feature matrix shape: Train={X_train.shape}, Test={X_test.shape}")

    # Model Selection: XGBoost primary, LightGBM / RandomForest backup
    models = {}
    if HAS_XGB:
        print("Training XGBoost Classifier...")
        models['XGBoost'] = xgb.XGBClassifier(
            n_estimators=300,
            max_depth=6,
            learning_rate=0.05,
            subsample=0.8,
            colsample_bytree=0.8,
            random_state=42,
            eval_metric='logloss'
        )

    if HAS_LGB:
        print("Training LightGBM Classifier...")
        models['LightGBM'] = lgb.LGBMClassifier(
            n_estimators=300,
            learning_rate=0.05,
            num_leaves=31,
            random_state=42,
            verbose=-1
        )

    print("Training HistGradientBoosting Classifier (Scikit-Learn Ensemble)...")
    models['HistGradientBoosting'] = HistGradientBoostingClassifier(
        max_iter=300,
        learning_rate=0.05,
        max_depth=12,
        random_state=42
    )

    print("Training Random Forest Classifier (Baseline)...")
    models['RandomForest'] = RandomForestClassifier(
        n_estimators=200,
        max_depth=15,
        random_state=42,
        n_jobs=-1
    )

    best_model = None
    best_f1 = -1.0
    best_model_name = ""

    print("\n--- Evaluation Results ---")
    for name, model in models.items():
        model.fit(X_train, y_train)
        y_pred = model.predict(X_test)
        y_proba = model.predict_proba(X_test)[:, 1] if hasattr(model, "predict_proba") else y_pred

        acc = accuracy_score(y_test, y_pred)
        prec = precision_score(y_test, y_pred, zero_division=0)
        rec = recall_score(y_test, y_pred, zero_division=0)
        f1 = f1_score(y_test, y_pred, zero_division=0)
        auc = roc_auc_score(y_test, y_proba)

        print(f"\nModel: {name}")
        print(f"  Accuracy:  {acc:.4f}")
        print(f"  Precision: {prec:.4f}")
        print(f"  Recall:    {rec:.4f}")
        print(f"  F1 Score:  {f1:.4f}")
        print(f"  AUC-ROC:   {auc:.4f}")
        print("  Confusion Matrix:")
        print(confusion_matrix(y_test, y_pred))

        if f1 > best_f1:
            best_f1 = f1
            best_model = model
            best_model_name = name

    print(f"\n==========================================")
    print(f"BEST PERFORMING MODEL: {best_model_name}")
    print(f"Achieved F1 Score: {best_f1:.4f} (Target: >= 0.82)")
    print(f"==========================================")

    # Save artifacts
    model_path = os.path.join(model_dir, "bug_classifier.pkl")
    extractor_path = os.path.join(model_dir, "feature_extractor.pkl")

    joblib.dump(best_model, model_path)
    extractor.save(extractor_path)

    print(f"Saved trained classifier to: {model_path}")
    print(f"Saved feature extractor to: {extractor_path}")

    return best_model_name, best_f1

if __name__ == "__main__":
    train_and_evaluate()
