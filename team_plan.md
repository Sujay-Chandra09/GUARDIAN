# GUARDIAN: An Intelligent Code Review & Bug Prediction System
## Project Plan & Implementation Map

---

## 1. Project Overview & Target Metrics
Guardian is an automated system that scans codebase commits and pull requests to highlight potential bugs and code quality issues before they reach production. 

### Key Targets:
* **F1 Score**: $\ge 0.82$ (CodeBERT model accuracy target)
* **AUC-ROC**: $\ge 0.85$
* **API Response Latency**: $< 2.0$ seconds
* **User Satisfaction Rating**: $\ge 3.5 / 5$ (via user testing evaluations)
* **Total Project Duration**: 36 weeks

---

## 2. Project Folder Structure
The workspace is organized into five specialized directories matching the project architecture:
```
guardian/
├── data-collection/  # Scripts for scraping codebases (PyDriller) and Defect4J extraction
├── ml-service/       # FastAPI Python app serving the ML/LLM models
├── backend/          # Java Spring Boot backend storing repository & scan metadata
├── frontend/         # React SPA dashboard displaying risk heatmaps & repo insights
└── vscode-plugin/    # VS Code extension for inline developer warnings
```

---

## 3. Phase-by-Phase Roadmap

### Phase 0 — Setup (Week 1)
* **Local Tool Setup**: Verify Node.js, Python, Java 17, Maven, Git, and Docker.
* **Accounts Setup**: Github, HuggingFace, OpenAI/Gemini, Railway.
* **Project Initialization**: Structure folders and document requirements.

### Phase 1 — Data Collection (Weeks 2-4)
* **Goal**: Collect and balance 15,000–25,000 labelled samples.
* **Data Sources**: Extract from Defects4J dataset (835 real bugs across 17 open-source projects) and scrape repositories (Django, Flask, Requests, Spring Boot, Commons Lang) using PyDriller.
* **Preprocessing**: Clean duplicate instances, remove outliers, and balance classes.
* **Output**: Save clean and buggy samples splits to `train_dataset.csv` and `test_dataset.csv` (80% train / 20% test).

### Phase 2 — AI Model Training (Weeks 5-12)
* **Baseline Model**: Train a fast Random Forest classifier (expected F1: 65-70%).
* **Primary Model (Low-Risk Hybrid Ensemble)**: Feature extraction (AST/complexity metrics + frozen CodeBERT/Transformer dense embeddings) combined with an **XGBoost / LightGBM** classifier to achieve target F1 $\ge 0.82$ and AUC-ROC $\ge 0.85$ without full model fine-tuning overhead.
* **Optional PEFT/LoRA Fallback**: Low-Rank Adaptation (LoRA) adapter tuning if specialized parameter tweaking is required.
* **Evaluation**: Plot confusion matrices and compute Precision, Recall, F1, and AUC-ROC.

### Phase 3 — Python ML Service (Weeks 13-15)
* **FastAPI App**: Expose a `POST /predict` endpoint that receives code snippets and returns a bug probability risk score (0.0 to 1.0).
* **LLM Integration (RAG & Few-Shot)**: Retrieve similar historical bug/fix pairs from Defects4J via vector search and prompt Gemini/GPT API for natural language explanations and code fix recommendations.
* **Optimization**: Set up Redis caching to immediately return results for identical code snippets. Add a healthcheck endpoint (`GET /health`).

### Phase 4 — Java Spring Boot Backend (Weeks 16-20)
* **API Service**: Spring Boot with Java 17 and Maven.
* **Database**: PostgreSQL storing user data, repos, and historical scan results.
* **Security**: JWT-based authentication and secure session management.
* **Integration**: Call the Python FastAPI service on port 8000 via Spring WebClient, persist results, and serve clean JSON outputs.

### Phase 5 — User-Facing Interfaces (Weeks 21-27)
* **React Dashboard**: Interactive heatmaps, visual commit tracking, and metrics visualization.
* **VS Code Plugin**: Highlights buggy functions using red squiggly underlines and displays LLM recommendations on hover.
* **GitHub Bot**: Automatically runs on PR updates via a GitHub Action to add inline comments pointing out risky changes.

### Phase 6 — Deploy, Test & Document (Weeks 28-36)
* **Containerization**: Define `Dockerfile` definitions for all components and a central `docker-compose.yml`.
* **Deployment**: Deploy service pipelines onto cloud platforms (Railway, Vercel).
* **User Acceptance Testing (UAT)**: Collect reviews and feedback ratings from 10–15 developers.
* **Documentation**: Write the final thesis/report and prepare the live system demo.

---

## 4. Team Roles

| Role | Responsibility | Main Focus Phases |
| :--- | :--- | :--- |
| **Member 1 (AI/ML Engineer)** | Data scraping, preprocessing, model training & tuning | Phases 1, 2, 3 |
| **Member 2 (Backend Developer)** | Spring Boot API, DB integration, service orchestrations | Phase 4 |
| **Member 3 (Frontend Developer)**| React dashboards, VS Code plugins, client views | Phase 5 |
| **Member 4 (DevOps + QA)** | Docker configuration, deployments, tests & report writing | Phase 6 |
