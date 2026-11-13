#!/usr/bin/env python3
"""Compile isolated documented contracts against the built SDK, with an actual-API control."""
import pathlib
import re
import subprocess

root = pathlib.Path(__file__).resolve().parents[2]
cache = pathlib.Path.home() / ".gradle/caches/modules-2/files-2.1"
dependencies = [
    ("org.jetbrains.kotlin", "kotlin-compiler-embeddable"),
    ("org.jetbrains.kotlin", "kotlin-stdlib"),
    ("org.jetbrains.kotlin", "kotlin-reflect"),
    ("org.jetbrains.kotlin", "kotlin-script-runtime"),
    ("org.jetbrains.kotlin", "kotlin-daemon-embeddable"),
    ("org.jetbrains.intellij.deps", "trove4j"),
    ("org.jetbrains.kotlinx", "kotlinx-coroutines-core-jvm"),
    ("org.jetbrains", "annotations"),
]
jars = []
for group, artifact in dependencies:
    matches = [p for p in (cache/group/artifact).rglob("*.jar")
               if not any(s in p.name for s in ("sources", "javadoc"))]
    jars.append(max(matches, key=lambda p: tuple(map(int, re.findall(r"\d+", p.parent.parent.name)))))
compiler_cp = ":".join(map(str, jars))
sdk_jar = root/"paymentsheet/build/intermediates/compile_library_classes_jar/debug/bundleLibCompileToJarDebug/classes.jar"
target_cp = compiler_cp + ":" + str(sdk_jar)
out = pathlib.Path("/tmp/checkout-foundation/docs-probe")
out.mkdir(parents=True, exist_ok=True)
for name, expected in [("ActualController.kt", 0), ("DocumentedController.kt", 1), ("DocumentedConfigureReturn.kt", 1)]:
    result = subprocess.run([
        "java", "-cp", compiler_cp, "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
        "-no-stdlib", "-no-reflect", "-Xskip-metadata-version-check",
        "-classpath", target_cp, "-d", str(out/name), str(pathlib.Path(__file__).with_name(name))
    ], capture_output=True, text=True)
    print(f"{name}: compiler exit {result.returncode}; expected {expected}")
    print(result.stdout + result.stderr)
    assert result.returncode == expected
