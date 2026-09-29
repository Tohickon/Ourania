"""
Central configuration.
Edit MODEL and MAX_ITERATIONS here — no other files need changing.
"""
from langchain_ollama import ChatOllama

# ── Model ────────────────────────────────────────────────────────────────────
# Any Ollama model works — no tool-calling API required.
MODEL = "dolphin3:8b"

# ── Loop guard ────────────────────────────────────────────────────────────────
MAX_ITERATIONS = 5

# ── LLM instance ─────────────────────────────────────────────────────────────
llm = ChatOllama(model=MODEL, temperature=0)
