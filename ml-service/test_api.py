from fastapi.testclient import TestClient
from main import app, load_artifacts

# Explicitly load artifacts
load_artifacts()

client = TestClient(app)

def test_health():
    response = client.get("/health")
    print("Health check response:", response.status_code, response.json())
    assert response.status_code == 200
    assert response.json()["model_loaded"] is True

def test_predict_buggy():
    payload = {
        "code_snippet": """
        public static boolean isBlank(CharSequence cs) {
            int strLen = cs.length(); // NPE risk if cs is null
            if (cs == null || strLen == 0) return true;
            return false;
        }
        """,
        "repo": "Commons-Lang",
        "filepath": "StringUtils.java"
    }
    response = client.post("/predict", json=payload)
    print("\n--- Buggy Snippet Prediction ---")
    print("Status:", response.status_code)
    data = response.json()
    print("Risk Score:", data["risk_score"])
    print("Is Buggy:", data["is_buggy"])
    print("Risk Level:", data["risk_level"])
    print("Latency (ms):", data["latency_ms"])
    print("Retrieved Context Count:", data["retrieved_context_count"])
    print("Explanation:\n", data["explanation"])
    assert response.status_code == 200
    assert data["is_buggy"] is True
    assert data["risk_score"] >= 0.5

def test_predict_clean():
    payload = {
        "code_snippet": """
        public static boolean isBlank(CharSequence cs) {
            if (cs == null || cs.length() == 0) return true;
            int strLen = cs.length();
            for (int i = 0; i < strLen; i++) {
                if (!Character.isWhitespace(cs.charAt(i))) return false;
            }
            return true;
        }
        """,
        "repo": "Commons-Lang",
        "filepath": "StringUtils.java"
    }
    response = client.post("/predict", json=payload)
    print("\n--- Clean Snippet Prediction ---")
    print("Status:", response.status_code)
    data = response.json()
    print("Risk Score:", data["risk_score"])
    print("Is Buggy:", data["is_buggy"])
    print("Risk Level:", data["risk_level"])
    print("Latency (ms):", data["latency_ms"])
    assert response.status_code == 200
    assert data["is_buggy"] is False
    assert data["risk_score"] < 0.5

if __name__ == "__main__":
    test_health()
    test_predict_buggy()
    test_predict_clean()
