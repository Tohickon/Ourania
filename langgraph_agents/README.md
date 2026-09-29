# langgraph_agents

A self-contained **3-agent LangGraph micro-service factory** that runs entirely on local models via [Ollama](https://ollama.com). Point it at any modular coding task and it autonomously plans, writes, and reviews Python code until it passes.

```
┌─────────┐     ┌─────────┐     ┌──────────┐
│ Planner │────▶│ Builder │────▶│ Reviewer │
│         │     │         │     │          │
│ searches│     │ writes  │     │ approves │
│ the web │     │ code    │◀────│ or sends │
│ if needed     │         │     │ back     │
└─────────┘     └─────────┘     └──────────┘
                                      │
                                   approved
                                      │
                                     END
```

---

## Prerequisites

| Dependency | Version |
|------------|---------|
| Python     | 3.11+   |
| [Ollama](https://ollama.com) | latest |

Pull the required model once:

```bash
ollama pull llama3.1
```

---

## Setup

```bash
cd langgraph_agents
python -m venv .venv
# Windows
.venv\Scripts\activate
# macOS / Linux
source .venv/bin/activate

pip install -r requirements.txt
```

---

## Usage

### Run a built-in example

Three examples are included out of the box, mapping directly to the three monetization strategies:

```bash
# Algorithmic trading — CCXT + Binance OHLCV fetcher
python main.py --example trading

# Web scraping — requests + BeautifulSoup link extractor
python main.py --example scraping

# News sentiment — feedparser + VADER RSS analyser
python main.py --example sentiment
```

### Pass your own task

```bash
python main.py --task "Write a Python function that parses this specific RSS feed
and returns a VADER sentiment score for each headline."
```

### Inject reference context (API docs, HTML snippets)

```bash
# From a file
python main.py --task "Place a buy order using this API" --context-file api_docs.txt

# Inline
python main.py --task "Extract prices from this HTML" \
  --context "<html>... your HTML here ...</html>"
```

### Interactive mode

```bash
python main.py --interactive
# Prompts you for the task and optional context at runtime.
```

### Quiet mode (only print the final code)

```bash
python main.py --example trading --quiet > trading_bot.py
```

---

## Configuration

Edit [`config.py`](config.py) to change the model or loop limit:

```python
MODEL          = "llama3.1"   # any model pulled in Ollama
MAX_ITERATIONS = 5            # max Builder→Reviewer revision cycles
```

---

## Project structure

```
langgraph_agents/
├── config.py        # Model name, MAX_ITERATIONS, LLM instances
├── state.py         # AgentState TypedDict (shared schema)
├── nodes.py         # planner_node, builder_node, reviewer_node
├── graph.py         # StateGraph assembly and compile()
├── main.py          # CLI entrypoint + built-in example tasks
├── requirements.txt
└── README.md
```

---

## How the loop works

1. **Planner** receives the task and any injected context. It first asks the LLM (with a bound `DuckDuckGoSearchRun` tool) whether it needs external documentation. If the LLM fires a tool call, the results are appended to `state["context"]` before the plan is written.

2. **Builder** writes a complete Python script from the plan. On revision passes it receives the Reviewer's numbered feedback and rewrites accordingly.

3. **Reviewer** checks for syntax errors, missing imports, task completeness, and security issues. It replies `APPROVED` or `REVISION NEEDED <issues>`. The graph routes back to Builder on a revision, or exits on approval.

4. **Guard**: `MAX_ITERATIONS` prevents runaway loops — after that many cycles the Reviewer force-approves.

---

## Monetization use-cases

| Strategy | Example task to feed the loop |
|---|---|
| **Algorithmic trading** | "Write a CCXT script that backtests a simple moving-average crossover on BTC/USDT 1h candles" |
| **Web scraping** | "Write a BeautifulSoup scraper for this HTML that extracts product names and prices into CSV" |
| **Sentiment oracle** | "Write a feedparser + VADER pipeline that scores the last 20 Reuters headlines" |
| **Web components** | "Write an HTML Canvas script that renders a Conway's Game of Life grid at 60fps" |

Keep tasks **narrow and specific** — the factory excels at "write and test *one function*", not "build an app".
