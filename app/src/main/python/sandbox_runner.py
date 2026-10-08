"""Runs one user script for Dev → Sandbox and captures what it prints.

Called from SandboxPyService (its own process). Returns [ok, output]; output holds stdout,
stderr and any traceback, so the Sandbox screen shows the same thing a terminal would.
"""
import contextlib
import io
import traceback


def lint_file(path):
    """Static check with pyflakes: nothing runs. Returns [ok, report]; ok means no findings."""
    from pyflakes.api import check
    from pyflakes.reporter import Reporter

    report = io.StringIO()
    with open(path, encoding="utf-8") as f:
        source = f.read()
    findings = check(source, "main.py", Reporter(report, report))
    text = report.getvalue().replace(path, "main.py")
    return [findings == 0, text]


def run_file(path):
    out = io.StringIO()
    ok = True
    with contextlib.redirect_stdout(out), contextlib.redirect_stderr(out):
        try:
            with open(path, encoding="utf-8") as f:
                source = f.read()
            exec(compile(source, "main.py", "exec"), {"__name__": "__main__"})
        except BaseException:
            ok = False
            traceback.print_exc()
    return [ok, out.getvalue()]
