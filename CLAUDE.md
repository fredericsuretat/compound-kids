# Les sous qui poussent (compound-kids) — notes pour Claude

App Android éducative **pour enfants** (repo public). Simulations 100 % hors ligne.

- **Support** : seul usage réseau de l'app, derrière un contrôle parental (« Espace parents », multiplication) dans
  `ui/screens/LearnScreen.kt`. Module `support/` identique aux autres apps ; clé serveur `compound-kids`, tickets dans le
  repo **privé** `compound-kids-support` (jamais dans ce repo public).
- Toute nouvelle collecte de données ou permission : mettre à jour `/privacy/compound-kids` (site-ateris `apps.js`) et la
  fiche « Sécurité des données » / « Public cible » du Play Store.
- **Publier** : `./gradlew assembleRelease bundleRelease` dans une copie sur disque local (pas dans `/config/Dev`), avec
  `keystore.properties` + `compound-kids-release.jks` (non versionnés, signature SHA-256 `3509585a…`). Release GitHub :
  `les-sous-qui-poussent-X.Y.Z.apk` + `.aab` (pour le Play Store), puis `COMPOUND_KIDS_APK_URL` dans site-ateris `apps.js`.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- When graphify-out/graph.json exists, run `graphify query "<question>"` before your first grep, search or source-file read for a codebase question, and put this rule in every subagent prompt that explores code. Read raw files only for lines the graph or an earlier read already located. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
- A committed graphify-out/graph.json is rewritten whole on every build, so a large diff to it in a pull request is expected. Do not flag it as churn or suggest skipping the graph rebuild or its hooks.
