import os
import time
import json
import subprocess
import urllib.request
import urllib.error

ANTHROPIC_API_KEY = os.environ.get("ANTHROPIC_API_KEY")
FILE_PATH = "AGENT_DIALOGUE.md"

TOOLS = [
    {
        "name": "read_file",
        "description": "Read the contents of a file.",
        "input_schema": {
            "type": "object",
            "properties": {
                "path": {"type": "string", "description": "The absolute or relative path to the file."}
            },
            "required": ["path"]
        }
    },
    {
        "name": "write_file",
        "description": "Create or overwrite a file with new content.",
        "input_schema": {
            "type": "object",
            "properties": {
                "path": {"type": "string", "description": "The path to the file."},
                "content": {"type": "string", "description": "The content to write."}
            },
            "required": ["path", "content"]
        }
    },
    {
        "name": "run_command",
        "description": "Run a shell command on the Windows machine. Use this to compile code, run tests, or use grep/find.",
        "input_schema": {
            "type": "object",
            "properties": {
                "command": {"type": "string", "description": "The powershell command to run."}
            },
            "required": ["command"]
        }
    }
]

def execute_tool(tool_name, tool_input):
    print(f"  [Tool] Executing {tool_name}...")
    try:
        if tool_name == "read_file":
            with open(tool_input["path"], "r", encoding="utf-8") as f:
                return f.read()
        elif tool_name == "write_file":
            with open(tool_input["path"], "w", encoding="utf-8") as f:
                f.write(tool_input["content"])
            return "File written successfully."
        elif tool_name == "run_command":
            result = subprocess.run(
                ["powershell", "-Command", tool_input["command"]], 
                capture_output=True, text=True, timeout=60
            )
            output = result.stdout + "\n" + result.stderr
            return output if output.strip() else "Command executed with no output (Exit code: {})".format(result.returncode)
        else:
            return f"Error: Unknown tool {tool_name}"
    except Exception as e:
        return f"Error executing {tool_name}: {str(e)}"

def get_claude_response_loop(prompt):
    headers = {
        "x-api-key": ANTHROPIC_API_KEY,
        "anthropic-version": "2023-06-01",
        "content-type": "application/json"
    }
    
    system_prompt = (
        "You are Claude, an AI collaborating with another AI agent named Antigravity on a software project. "
        "You have access to tools to read files, write files, and run commands on the user's Windows machine. "
        "You are running on a scheduled heartbeat. "
        "If it is your turn, you must take action using your tools, verify your work using run_command, and then formulate a final text response. "
        "Before finishing your turn, your final text response MUST include a 'Ledger' containing:\n"
        "1. Files Modified\n2. Commands Run (and test results)\n3. Current Goal State.\n"
        "DO NOT output the ### @Antigravity header yourself, the system adds it. "
        "If it is not your turn, reply exactly with NO_REPLY."
    )
    
    messages = [{"role": "user", "content": prompt}]
    
    while True:
        data = {
            "model": "claude-3-5-sonnet-20240620",
            "max_tokens": 4096,
            "system": system_prompt,
            "messages": messages,
            "tools": TOOLS
        }
        
        req = urllib.request.Request(
            "https://api.anthropic.com/v1/messages",
            data=json.dumps(data).encode("utf-8"),
            headers=headers,
            method="POST"
        )
        
        try:
            with urllib.request.urlopen(req) as response:
                resp_json = json.loads(response.read().decode("utf-8"))
        except urllib.error.HTTPError as e:
            err_msg = e.read().decode("utf-8")
            raise Exception(f"API Error {e.code}: {err_msg}")
        stop_reason = resp_json.get("stop_reason")
        content_blocks = resp_json.get("content", [])
        
        # Add assistant message to history
        messages.append({"role": "assistant", "content": content_blocks})
        
        # Extract text if any
        text_response = next((block["text"] for block in content_blocks if block["type"] == "text"), "")
        if text_response:
            print(f"  [Claude] {text_response[:100]}...")
            
        if stop_reason == "tool_use":
            tool_results = []
            for block in content_blocks:
                if block["type"] == "tool_use":
                    tool_name = block["name"]
                    tool_input = block["input"]
                    tool_id = block["id"]
                    
                    result_text = execute_tool(tool_name, tool_input)
                    tool_results.append({
                        "type": "tool_result",
                        "tool_use_id": tool_id,
                        "content": str(result_text)[:10000] # truncate output to avoid context length issues
                    })
            
            messages.append({"role": "user", "content": tool_results})
            # Loop continues to send tool results back to Claude
        else:
            # Done!
            return text_response

def main():
    if not ANTHROPIC_API_KEY:
        print("ERROR: ANTHROPIC_API_KEY environment variable is not set.")
        return

    print(f"Agentic Bridge Active. Polling {FILE_PATH} every 60 seconds...")
    print("Press Ctrl+C to exit.\n")
    
    while True:
        try:
            if not os.path.exists(FILE_PATH):
                time.sleep(10)
                continue

            with open(FILE_PATH, "r", encoding="utf-8") as f:
                content = f.read()
            
            last_claude_idx = content.rfind("### @Claude")
            last_antigravity_idx = content.rfind("### @Antigravity")
            
            if last_claude_idx > last_antigravity_idx:
                print(f"[{time.strftime('%H:%M:%S')}] Baton held by Claude. Initiating agent loop...")
                
                prompt = f"Here is the shared dialogue file:\n\n{content}\n\nPlease take your turn."
                final_response = get_claude_response_loop(prompt)
                
                if final_response.strip() != "NO_REPLY":
                    print("Turn complete. Writing Ledger to file and passing Baton...")
                    with open(FILE_PATH, "a", encoding="utf-8") as f:
                        f.write(f"\n\n### @Antigravity\n\n{final_response}\n")
                    print("Baton passed to Antigravity. Sleeping.")
                else:
                    print("Claude replied NO_REPLY.")
                    
        except Exception as e:
            print(f"Bridge Error: {e}")
            
        time.sleep(60)

if __name__ == "__main__":
    main()
