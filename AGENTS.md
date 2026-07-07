# libGDX Project Guidelines

## What to Avoid

* Do not use standard Java `Thread.sleep()` inside libGDX core code; use Timers or Actions instead.
* Avoid using reflection since it is not supported by the `html` and `teavm` platforms.

# Gemini Interaction Logging Rule

You are required to strictly follow this audit rule for **every single interaction, code block generation, or refactoring task** you perform.

At the very end of your response, after your main output is complete, you must generate and append the output directly to `docs/ai-log.md`.

## Log Format Specification

The log entry must match this exact pipe-delimited Markdown table schema. Use placeholders matching the user's current session where specific information (like the exact Date or PR/Commit link) is unknown to you, so the user can quickly fill it in.

| Date | Tool | Purpose | Prompt Summary | Output Used? (Yes/No/Partial) | What You Changed/Validated | Link (PR/commit/asset) |
|---|---|---|---|---|---|---|

### Formatting Instructions:
- **Date**: Add a place holder for YYYY-MM-DD.
- **Tool**: Always set this to `Gemini (Android Studio)`.
- **Purpose**: A brief 2-4 word category (e.g., "UI Refactor", "Bug Fix", "Unit Test").
- **Prompt Summary**: A single sentence summarizing what the user asked you to do.
- **Output Used?**: Set this to `Yes` if the full output was used, `No` if scrapped, or `Partial` if modified.
- **What You Changed/Validated**: A brief summary of the exact changes you introduced, classes touched, or logic verified.
- **Link**: Leave a placeholder `[Insert PR/Commit Link]` for the user to append later.

## Example Output to Append

```markdown
| 2026-05-19 | Gemini (Android Studio) | Architecture | Set up an AI audit log protocol inside AGENTS.md | [Yes/No/Partial] | Defined a markdown schema for tracking assistant prompts and code impacts | [Insert PR/Commit Link] |
