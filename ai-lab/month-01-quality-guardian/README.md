# Quality Guardian — AI Lab (Mois 1)

[![Python](https://img.shields.io/badge/Python-3.9%2B-blue.svg)](https://www.python.org/)
[![Status](https://img.shields.io/badge/Status-Active-brightgreen.svg)]()
[![Type](https://img.shields.io/badge/Tool-Read--Only%20Linter-purple.svg)]()

**Quality Guardian** est un outil d'analyse statique et d'audit de sécurité automatisé développé dans le cadre du **Mois 1 de l'AI Lab**. Son objectif est de garantir l'absence de fuites de données sensibles (clés privées, tokens d'API, mots de passe) et de s'assurer de la présence des fichiers critiques du projet sans altérer le code existant.

---

## 📋 Fonctionnalités Clés

- **Détection des secrets en dur** :
  - Détection des assignations de mots de passe, clés d'API et tokens secrets (`SECRET_ASSIGNMENT`).
  - Détection des blocs de clés privées (`PRIVATE_KEY` : RSA, EC, OPENSSH).
- **Filtrage intelligent des faux-positifs** :
  - Tolérance automatique pour les gabarits et exemples documentaires francophones (`votre_password`, `votre_secret`, `mon_secret`, `a_definir`, etc.).
  - Prise en charge des variables d'environnement Spring Boot (`${DB_PASSWORD}`, `${JWT_SECRET}`).
- **Politique de non-divulgation (Zero-Leak)** :
  - Les valeurs suspectées ne sont **jamais stockées** dans les rapports JSON générés, uniquement le fichier et le numéro de ligne.
- **Audit d'intégrité structurelle** :
  - Vérification de l'existence de la racine du projet et des fichiers critiques indispensables (`pom.xml`, `application.properties`).
- **Autonomie complète** :
  - Conçu pour fonctionner sans dépendances externes lourdes (bibliothèque standard Python).

---

## 📁 Architecture du Module

```
ai-lab/month-01-quality-guardian/
├── src/
│   └── quality_guardian/
│       ├── __init__.py
│       └── guardian.py       # Moteur d'analyse statique et règles
├── tests/
│   └── test_guardian.py      # Suite de tests unitaires (5/5 tests validés)
├── reports/                  # Rapports d'audit générés (JSON)
├── cli.py                    # Point d'entrée en ligne de commande
├── requirements.txt          # Spécifications d'environnement Python
└── README.md                 # Documentation du composant
```

---

## 🚀 Utilisation

### 1. Activer l'environnement virtuel

Sous Windows PowerShell :
```powershell
cd ai-lab\month-01-quality-guardian
.\.venv\Scripts\Activate.ps1
```

### 2. Lancer un audit de sécurité

Pour scanner l'intégralité du projet Marketplace :

```powershell
python cli.py scan --root ../.. --report reports/quality-report.json
```

Options disponibles pour la commande `scan` :

| Option | Type | Description | Valeur par défaut |
|---|---|---|---|
| `--root` | `Path` | Chemin racine du projet à analyser | Racine du projet (`../..`) |
| `--report` | `Path` | Emplacement du rapport JSON de sortie | `reports/quality-report.json` |
| `--fail-on-warning`| `Flag` | Retourne le code d'erreur `1` si des avertissements sont détectés | Désactivé |

### 3. Consulter les résultats

Un rapport JSON structuré est généré :

```json
{
  "finding_count": 0,
  "findings": []
}
```

En cas d'anomalie détectée, chaque élément comprend :
- `rule` : Code de la règle déclenchée (`SECRET_ASSIGNMENT`, `PRIVATE_KEY`, etc.).
- `severity` : Niveau de criticité (`WARNING`, `ERROR`).
- `message` : Explication du risque sans exposition du secret.
- `file` : Chemin relatif du fichier source concerné.
- `line` : Numéro de la ligne incriminée.

---

## 🧪 Tests Unitaires

Une suite de tests exhaustifs vérifie la robustesse du moteur et le bon fonctionnement des règles et des exclusions :

```powershell
python -m unittest discover -s tests -v
```

Tests inclus :
- `test_scan_detects_secret_assignment` : Vérifie la détection d'un secret en clair.
- `test_scan_detects_private_key` : Vérifie la détection d'une clé privée.
- `test_scan_ignores_placeholders` : Vérifie l'exclusion des placeholders standards (`changeme`, etc.).
- `test_french_placeholder_is_ignored` : Vérifie l'exclusion des marqueurs documentaires francophones.
- `test_write_report_withholds_secrets` : Vérifie que le rapport n'écrit jamais de secret en clair.

---

## ⚙️ Règles et Extensions Analysées

### Extensions prises en charge :
`.java`, `.properties`, `.yml`, `.yaml`, `.json`, `.xml`, `.py`, `.dart`, `.js`, `.ts`, `.env`, `.sql`, `.sh`, `.ps1`, `.md`, `.txt`.

### Dossiers exclus par défaut :
`.git`, `.venv`, `node_modules`, `target`, `build`, `.gradle`, `__pycache__`, `reports`, `tests`, `configs_pre_fix`.
