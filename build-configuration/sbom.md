# Module SBOMs

Generate a CycloneDX JSON SBOM for each published SDK module:

```sh
./gradlew --no-configuration-cache --init-script build-configuration/sbom.init.gradle moduleSboms
python3 scripts/check_module_sboms.py build/reports/sbom
```

To generate only Identity, replace `moduleSboms` with `:identity:cyclonedxDirectBom`.

The `artifactId` defined by each module's publication selects the modules and names
`build/reports/sbom/<artifactId>.cdx.json`. Each report describes that module's
`releaseRuntimeClasspath`, including external dependencies reached through other
SDK modules. Examples, testing helpers, debug, test, and build dependencies are excluded.
Generation fails if a release dependency cannot be resolved; the checker requires
versioned components and a connected dependency graph.

The Dependency submission workflow uploads these files as the `module-sboms` artifact.
GitHub's dependency graph and its repository SBOM export remain repository-wide.
These reports inventory Gradle dependencies; they do not inventory native components
embedded inside dependency AARs.
