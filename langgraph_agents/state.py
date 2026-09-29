"""
Shared state schema for the 3-agent LangGraph loop.
All nodes read from and write back into this TypedDict.
"""
from typing import TypedDict


class AgentState(TypedDict):
    """
    Fields
    ------
    task        : The natural-language request fed to the Planner.
    context     : Optional API docs, HTML snippets, or any reference
                  text injected before the loop starts — or accumulated
                  by the Planner's autonomous web search.
    plan        : Step-by-step coding plan produced by the Planner.
    code        : Python code produced by the Builder.
    feedback    : Review notes produced by the Reviewer.
    iterations  : Guard counter — stops the loop after MAX_ITERATIONS.
    approved    : Set to True by the Reviewer when the code passes.
    """
    task: str
    context: str
    plan: str
    code: str
    feedback: str
    iterations: int
    approved: bool
