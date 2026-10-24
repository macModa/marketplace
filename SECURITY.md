# Politique de Sécurité — Marketplace

## Versions Prises en Charge

Nous assurons le support et les correctifs de sécurité pour les versions suivantes :

| Version | Prise en charge |
|---|:---:|
| 1.0.x (Current) | :white_check_mark: |
| < 1.0.0 | :x: |

---

## Signalement d'une Vulnérabilité

La sécurité de cette plateforme est une priorité absolue. Si vous découvrez une faille de sécurité ou une vulnérabilité potentielle, merci de suivre ce protocole :

1. **Divulgation Responsable** : Ne signalez **pas** la vulnérabilité publiquement via des issues GitHub ou sur les réseaux sociaux.
2. **Contact Privé** : Envoyez les détails techniques à l'équipe responsable du projet ou à l'administrateur du dépôt.
3. **Contenu du signalement** :
   - Description claire de la vulnérabilité
   - Étapes précises pour reproduire le problème (PoC / curl / payload)
   - Impact potentiel estimé sur les données ou le système
4. **Délai de réponse** : Nous nous engageons à accuser réception dans les 48 heures ouvrées et à proposer un correctif dans un délai raisonnable.

---

## Principes de Sécurité Implémentés

### 1. Gestion des Secrets (Zero Hardcoded Secret)
- **Externalisation systématique** : Tous les secrets, mots de passe et clés d'accès sont définis via des variables d'environnement (`.env`).
- **Protection Git** : Les fichiers contenant des secrets réels (`.env`, `run-marketplace.ps1`) sont ignorés par Git via `.gitignore`.
- **Modèle documenté** : Un fichier modèle sans secrets (`.env.example`) est mis à disposition.
- **Audit statique continu** : L'outil `Quality Guardian` analyse le code pour bloquer tout secret accidentellement committé.

### 2. Authentification et Autorisation
- **Stateless JWT** : Jetons d'accès signés cryptographiquement via HMAC SHA-256 avec une clé secrète externe d'au moins 256 bits (`JWT_SECRET`).
- **Hachage des mots de passe** : Chiffrement irréversible avec `BCryptPasswordEncoder` (facteur de coût adapté).
- **Contrôle d'accès basé sur les rôles (RBAC)** : Ségrégation stricte des permissions entre `CLIENT`, `ARTISAN` et `ADMIN`.

### 3. Protection de la Base de Données
- **Moindre privilège** : L'application n'utilise pas l'utilisateur `root` de MySQL en production ni en développement ; un compte dédié `marketplace_dev` dispose uniquement des droits sur sa base `marketplace_local`.
- **Prévention contre les injections SQL** : Utilisation exclusive de Spring Data JPA / Hibernate avec requêtes préparées et paramétrées.

### 4. Validation des Données
- Validation stricte des flux entrants avec Jakarta Bean Validation (`@Valid`, `@NotNull`, `@Size`, `@Email`).
- DTOs hermétiques pour empêcher toute fuite de colonnes sensibles ou attaques par assignation massive (*mass assignment*).
