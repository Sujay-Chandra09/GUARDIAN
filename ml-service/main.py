import time
import os
import joblib
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import Optional

from feature_extractor import CodeFeatureExtractor
from rag_explainer import RAGExplainer

app = FastAPI(
    title="GUARDIAN ML Service",
    description="Intelligent Bug Prediction & Code Review API using Feature Extraction Ensembles and RAG",
    version="1.0.0"
)

# Global model and explainer handles
model = None
extractor = None
rag_explainer = None

MODEL_PATH = "ml-service/models/bug_classifier.pkl"
EXTRACTOR_PATH = "ml-service/models/feature_extractor.pkl"

class PredictRequest(BaseModel):
    code_snippet: str
    repo: Optional[str] = "unknown"
    filepath: Optional[str] = ""

class PredictResponse(BaseModel):
    risk_score: float
    is_buggy: bool
    risk_level: str
    latency_ms: float
    explanation: str
    retrieved_context_count: int

@app.on_event("startup")
def load_artifacts():
    global model, extractor, rag_explainer
    print("Loading GUARDIAN ML artifacts...")
    if os.path.exists(MODEL_PATH) and os.path.exists(EXTRACTOR_PATH):
        model = joblib.load(MODEL_PATH)
        extractor = CodeFeatureExtractor.load(EXTRACTOR_PATH)
        print("Successfully loaded trained classifier and feature extractor.")
    else:
        print("Warning: Model artifacts not found! Run train_classifier.py first.")

    rag_explainer = RAGExplainer()

@app.get("/health")
def healthcheck():
    return {
        "status": "healthy",
        "model_loaded": model is not None,
        "extractor_loaded": extractor is not None,
        "rag_indexed": rag_explainer is not None and rag_explainer.is_indexed
    }

@app.post("/predict", response_model=PredictResponse)
def predict_bug_risk(req: PredictRequest):
    start_time = time.time()
    
    if not req.code_snippet or not req.code_snippet.strip():
        raise HTTPException(status_code=400, detail="code_snippet cannot be empty.")

    if model is None or extractor is None:
        # Fallback heuristic if model file isn't loaded
        risk_score = 0.5
    else:
        features = extractor.transform([req.code_snippet])
        proba = model.predict_proba(features)[0][1]
        risk_score = float(proba)

    is_buggy = risk_score >= 0.5
    if risk_score >= 0.75:
        risk_level = "HIGH"
    elif risk_score >= 0.4:
        risk_level = "MEDIUM"
    else:
        risk_level = "LOW"

    # RAG Explanation
    expl_data = rag_explainer.generate_explanation(req.code_snippet, risk_score) if rag_explainer else {
        "explanation": "Standard analysis", "retrieved_context_count": 0
    }

    latency_ms = (time.time() - start_time) * 1000.0

    return PredictResponse(
        risk_score=round(risk_score, 4),
        is_buggy=is_buggy,
        risk_level=risk_level,
        latency_ms=round(latency_ms, 2),
        explanation=expl_data["explanation"],
        retrieved_context_count=expl_data.get("retrieved_context_count", 0)
    )

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
