"""
CLI entrypoint for the 3-agent LangGraph micro-service factory.

Usage
-----
# Run a built-in example task:
    python main.py

# Pass your own task interactively:
    python main.py --interactive

# Inject a context file (API docs / HTML snippet):
    python main.py --task "Parse product prices" --context-file page.html

# Suppress per-iteration output and only print final code:
    python main.py --quiet
"""
import argparse
import io
import sys
import textwrap

# Force UTF-8 on stdout/stderr so emoji-free status lines and any
# non-ASCII in generated code survive redirection on Windows cp1252.
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")

from graph import app


# ---------------------------------------------------------------------------
# Built-in example tasks — illustrate the three monetization use-cases
# ---------------------------------------------------------------------------
EXAMPLE_TASKS = {
    "trading": {
        "task": (
            "Write a Python function that connects to Binance via the CCXT library, "
            "fetches the last 100 OHLCV candles for BTC/USDT on the 1-hour timeframe, "
            "and returns a pandas DataFrame with columns: timestamp, open, high, low, "
            "close, volume.  Read the API key and secret from environment variables "
            "BINANCE_API_KEY and BINANCE_SECRET."
        ),
        "context": "",
    },
    "scraping": {
        "task": (
            "Write a Python script using requests and BeautifulSoup that accepts a "
            "URL as a command-line argument, scrapes every <a> tag on the page, and "
            "writes a CSV file (url, link_text, href) to stdout."
        ),
        "context": "",
    },
    "sentiment": {
        "task": (
            "Write a Python function that accepts an RSS feed URL, fetches up to 20 "
            "entries with feedparser, runs a VADER sentiment analysis on each entry's "
            "title using nltk, and returns a list of dicts with keys: title, url, "
            "compound_score, published."
        ),
        "context": "",
    },
}


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _print_divider(label: str = "") -> None:
    width = 72
    if label:
        pad = (width - len(label) - 2) // 2
        print("\n" + "─" * pad + f" {label} " + "─" * pad)
    else:
        print("\n" + "─" * width)


def run(task: str, context: str = "", quiet: bool = False) -> str:
    """
    Execute the agent loop and return the final approved code.

    Parameters
    ----------
    task    : Natural-language description of what to build.
    context : Optional reference material (API docs, HTML, etc.).
    quiet   : Suppress intermediate node output.
    """
    initial_state: dict = {
        "task":       task,
        "context":    context,
        "plan":       "",
        "code":       "",
        "feedback":   "",
        "iterations": 0,
        "approved":   False,
    }

    if not quiet:
        _print_divider("TASK")
        print(textwrap.fill(task, width=72))
        if context.strip():
            _print_divider("CONTEXT (first 300 chars)")
            print(context[:300] + ("…" if len(context) > 300 else ""))
        _print_divider("RUNNING AGENT LOOP")

    final_state = app.invoke(initial_state)

    if not quiet:
        _print_divider("FINAL APPROVED CODE")

    print(final_state["code"])
    return final_state["code"]


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="3-agent LangGraph micro-service factory",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=textwrap.dedent("""
        Examples
        --------
        # Run the built-in trading example:
            python main.py --example trading

        # Run the built-in scraping example:
            python main.py --example scraping

        # Custom task:
            python main.py --task "Write a function that ..."

        # Custom task with context file:
            python main.py --task "Parse prices from this HTML" --context-file page.html
        """),
    )
    parser.add_argument(
        "--example",
        choices=list(EXAMPLE_TASKS.keys()),
        default="trading",
        help="Run a built-in example task (default: trading)",
    )
    parser.add_argument(
        "--task",
        type=str,
        default=None,
        help="Custom task description (overrides --example)",
    )
    parser.add_argument(
        "--context-file",
        type=str,
        default=None,
        metavar="FILE",
        help="Path to a file whose contents are injected as reference context",
    )
    parser.add_argument(
        "--context",
        type=str,
        default=None,
        help="Inline context string (alternative to --context-file)",
    )
    parser.add_argument(
        "--interactive",
        action="store_true",
        help="Prompt for task and optional context at runtime",
    )
    parser.add_argument(
        "--quiet",
        action="store_true",
        help="Only print the final code; suppress all agent chatter",
    )
    return parser.parse_args()


def main() -> None:
    args = _parse_args()

    # ---- Resolve task -------------------------------------------------------
    if args.interactive:
        print("Enter your task (finish with a blank line):")
        lines = []
        while True:
            line = input()
            if line == "":
                break
            lines.append(line)
        task = " ".join(lines).strip()
        print("\nPaste reference context / API docs (finish with a blank line).\n"
              "Press Enter immediately to skip:")
        ctx_lines = []
        while True:
            line = input()
            if line == "":
                break
            ctx_lines.append(line)
        context = "\n".join(ctx_lines)
    elif args.task:
        task = args.task
        context = ""
    else:
        example = EXAMPLE_TASKS[args.example]
        task = example["task"]
        context = example["context"]

    # ---- Resolve context ----------------------------------------------------
    if args.context_file:
        try:
            with open(args.context_file, "r", encoding="utf-8") as fh:
                context = fh.read()
        except FileNotFoundError:
            print(f"ERROR: context file not found: {args.context_file}", file=sys.stderr)
            sys.exit(1)
    elif args.context:
        context = args.context

    # ---- Run ----------------------------------------------------------------
    run(task=task, context=context, quiet=args.quiet)


if __name__ == "__main__":
    main()
