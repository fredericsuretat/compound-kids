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
