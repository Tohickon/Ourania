"""
Graph assembly: wires Planner → Builder → Reviewer with a conditional
back-edge from Reviewer → Builder for revision loops.
"""
from langgraph.graph import StateGraph, START, END

from state import AgentState
from nodes import planner_node, builder_node, reviewer_node


def _route_after_review(state: AgentState) -> str:
    """Route back to the builder for revisions, or exit when approved."""
    if state.get("approved"):
        return "done"
    return "revise"


def build_graph():
    g = StateGraph(AgentState)

    g.add_node("planner", planner_node)
    g.add_node("builder", builder_node)
    g.add_node("reviewer", reviewer_node)

    # Fixed edges
    g.add_edge(START, "planner")
    g.add_edge("planner", "builder")
    g.add_edge("builder", "reviewer")

    # Conditional back-edge
    g.add_conditional_edges(
        "reviewer",
        _route_after_review,
        {
            "revise": "builder",   # send back for fixes
            "done":   END,         # exit loop
        },
    )

    return g.compile()


# Module-level compiled graph — import this from main.py
app = build_graph()
