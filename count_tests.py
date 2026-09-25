import glob
import xml.etree.ElementTree as ET

total = 0
failures = 0
for f in sorted(glob.glob("app/build/test-results/testDebugUnitTest/*.xml")):
    tree = ET.parse(f)
    root = tree.getroot()
    t = int(root.attrib.get("tests", 0))
    fail = int(root.attrib.get("failures", 0))
    err = int(root.attrib.get("errors", 0))
    total += t
    failures += (fail + err)
    suite_name = root.attrib.get("name", f).split(".")[-1]
    print(f"{suite_name:35} : {t:2} tests | {fail + err} failures")

print("-" * 50)
print(f"TOTAL ANDROID UNIT TESTS: {total} | FAILURES: {failures}")
