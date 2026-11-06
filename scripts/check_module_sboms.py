import json
from pathlib import Path
import sys

reports = sorted(Path(sys.argv[1]).glob('*.cdx.json'))
assert reports, 'No module SBOMs generated'
for report in reports:
    bom = json.loads(report.read_text())
    assert bom['bomFormat'] == 'CycloneDX', report
    root = bom['metadata']['component']
    assert root['name'] == report.name.removesuffix('.cdx.json'), report
    assert root['version'] and root['version'] != 'unspecified', report
    components = bom['components']
    assert components, f'{report}: empty dependency inventory'
    assert all(component.get('version') and component['version'] != 'unspecified'
               for component in components), f'{report}: unversioned dependency'
    refs = {root['bom-ref']} | {component['bom-ref'] for component in components}
    edges = {entry['ref']: set(entry.get('dependsOn', [])) for entry in bom['dependencies']}
    assert set(edges) <= refs, f'{report}: unknown dependency source'
    assert all(targets <= refs for targets in edges.values()), f'{report}: unknown dependency target'
    reached = set()
    pending = [root['bom-ref']]
    while pending:
        ref = pending.pop()
        if ref not in reached:
            reached.add(ref)
            pending.extend(edges.get(ref, []))
    assert reached == refs, f'{report}: disconnected dependency inventory'
    print(f'{report.name}: {len(components)} components')
