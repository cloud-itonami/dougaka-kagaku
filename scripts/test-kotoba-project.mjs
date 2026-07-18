import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";

const artifactPath = process.argv[2];
assert.ok(artifactPath, "compiled Kotoba artifact path is required");

const generated = await import(pathToFileURL(artifactPath).href);
assert.match(generated.kotobaArtifact.moduleGraphDigest, /^[0-9a-f]{64}$/);
assert.match(generated.kotobaArtifact.packageLockDigest, /^[0-9a-f]{64}$/);
assert.match(generated.kotobaArtifact.trustPolicyDigest, /^[0-9a-f]{64}$/);
assert.match(generated.kotobaArtifact.packageReceiptDigest, /^[0-9a-f]{64}$/);
assert.deepEqual(Object.keys(generated.kotobaArtifact.moduleSourceDigests), [
  "kotoba.gftd.kagaku.time-constants",
  "kotoba.gftd.kagaku.time-units",
]);

const api = generated.instantiateKotoba({});
for (const [minutes, seconds] of [[0n, 0n], [1n, 60n], [3n, 180n], [60n, 3600n]]) {
  assert.equal(api["minutes-to-seconds"](minutes), seconds);
}

console.log("dougaka-kagaku Kotoba closed-project pilot passed");
