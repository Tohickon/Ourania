"""
The three agent nodes:
  - planner_node  : Searches the web for relevant docs, then writes a coding plan.
  - builder_node  : Writes Python code from the plan.
  - reviewer_node : Reviews the code and either approves or sends it back.
"""
from langchain_core.messages import HumanMessage
from langchain_community.tools import DuckDuckGoSearchRun

from state import AgentState
from config import llm, MAX_ITERATIONS

_search_tool = DuckDuckGoSearchRun()

# ---------------------------------------------------------------------------
# Planner
# ---------------------------------------------------------------------------

def planner_node(state: AgentState) -> dict:
    """
    Two-step process:
    1. Search the web for relevant library docs / syntax examples.
    2. Produce a step-by-step coding plan using the gathered context.
    """
    task = state["task"]
    current_context = state.get("context", "")
    iterations = state.get("iterations", 0)

    # ---- Step 1: web search -------------------------------------------------
    # Ask the LLM what search query would be most useful, then run it.
    query_prompt = f"""Given this coding task:
{task}

Reply with ONLY a short web search query string - no explanation, no code, no punctuation other than spaces.
Example reply: ccxt binance fetch ohlcv pandas dataframe
"""
    query_response = llm.invoke([HumanMessage(content=query_prompt)])
    # Take only the last non-empty line to avoid the model padding its answer
    lines = [l.strip().strip('"').strip("'") for l in query_response.content.splitlines() if l.strip()]
    query = lines[-1] if lines else task
    print(f"  [Planner] >> Searching: {query}")
    try:
        results = _search_tool.invoke(query)
        current_context += f"\n--- Search results for '{query}' ---\n{results}\n"
    except Exception as e:
        print(f"  [Planner] Search failed ({e}), continuing without web results.")

    # ---- Step 2: write the plan ---------------------------------------------
    plan_prompt = f"""You are a senior software architect. Create a concise, numbered
step-by-step Python coding plan - no actual code, just the plan.

TASK:
{task}

REFERENCE CONTEXT:
{current_context if current_context.strip() else "None provided - rely on your training knowledge."}

Write only the numbered plan steps.
"""
    plan_response = llm.invoke([HumanMessage(content=plan_prompt)])

    print(f"  [Planner] Plan written ({len(plan_response.content)} chars)")
    return {
        "plan": plan_response.content,
        "context": current_context,
        "iterations": iterations,
    }


# ---------------------------------------------------------------------------
# Builder
# ---------------------------------------------------------------------------

def builder_node(state: AgentState) -> dict:
    """
    Produces a complete, runnable Python script from the plan.
    On subsequent iterations it also receives the Reviewer's feedback.
    """
    task = state["task"]
    plan = state["plan"]
    context = state.get("context", "")
    feedback = state.get("feedback", "")
    code = state.get("code", "")

    fence = "```"
    if feedback and code:
        # Revision pass - fix according to reviewer feedback
        build_prompt = (
            "You are an expert Python developer. Revise the code below "
            "based on the reviewer's feedback. Return ONLY the corrected "
            f"Python code inside a single {fence}python ...{fence} fence, no extra explanation.\n\n"
            f"ORIGINAL TASK:\n{task}\n\n"
            f"REVIEWER FEEDBACK:\n{feedback}\n\n"
            f"CURRENT CODE:\n{fence}python\n{code}\n{fence}"
        )
    else:
        # First pass - build from the plan
        build_prompt = (
            "You are an expert Python developer. Write a complete, "
            "runnable Python script that satisfies the task below. "
            f"Return ONLY the Python code inside a single {fence}python ...{fence} fence.\n\n"
            f"TASK:\n{task}\n\n"
            f"PLAN:\n{plan}\n\n"
            f"REFERENCE CONTEXT:\n{context if context.strip() else 'None.'}"
        )

    build_response = llm.invoke([HumanMessage(content=build_prompt)])

    # Strip the markdown fence if present
    raw = build_response.content
    if "```python" in raw:
        raw = raw.split("```python", 1)[1]
        raw = raw.rsplit("```", 1)[0]
    elif "```" in raw:
        raw = raw.split("```", 1)[1]
        raw = raw.rsplit("```", 1)[0]

    code_out = raw.strip()
    print(f"  [Builder] Code written ({len(code_out)} chars)")
    return {"code": code_out}


# ---------------------------------------------------------------------------
# Reviewer
# ---------------------------------------------------------------------------

def reviewer_node(state: AgentState) -> dict:
    """
    Reviews the code for correctness, completeness, and safety.
    Sets approved=True when satisfied, or approved=False with detailed feedback.
    """
    task = state["task"]
    code = state["code"]
    iterations = state.get("iterations", 0) + 1

    # Hard stop - avoid infinite loops
    if iterations >= MAX_ITERATIONS:
        print(f"  [Reviewer] Max iterations ({MAX_ITERATIONS}) reached - force-approving.")
        return {"approved": True, "iterations": iterations, "feedback": ""}

    fence = "```"
    review_prompt = (
        "You are a Python code reviewer. Read the task and code below.\n\n"
        f"TASK:\n{task}\n\n"
        f"CODE:\n{fence}python\n{code}\n{fence}\n\n"
        "Check ONLY for:\n"
        "1. Missing import statements\n"
        "2. Undefined variable names\n"
        "3. Obvious syntax errors\n"
        "4. Hardcoded secrets that should come from environment variables\n\n"
        "Do NOT flag style issues, performance, or hypothetical runtime errors.\n\n"
        "If the code looks correct, reply with exactly one word: APPROVED\n"
        "Otherwise reply with: REVISION NEEDED\n"
        "Followed by a numbered list of only the concrete issues found."
    )

    review_response = llm.invoke([HumanMessage(content=review_prompt)])
    verdict = review_response.content.strip()

    if verdict.upper().startswith("APPROVED"):
        print(f"  [Reviewer] APPROVED on iteration {iterations}")
        return {"approved": True, "iterations": iterations, "feedback": ""}
    else:
        print(f"  [Reviewer] REVISION NEEDED (iteration {iterations})\n{verdict}\n")
        return {"approved": False, "iterations": iterations, "feedback": verdict}
