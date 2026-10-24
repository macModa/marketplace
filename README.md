# Marketplace API — Spring Boot

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![Security](https://img.shields.io/badge/Security-JWT%20%2B%20Zero--Leak-red.svg)](#sécurité-et-gestion-des-secrets)
[![Quality Guardian](https://img.shields.io/badge/AI%20Lab-Quality%20Guardian-purple.svg)](#-ai-lab--quality-guardian)

Backend REST API d'une application marketplace artisanale tunisienne, conçu pour être consommé par une application mobile Flutter. Le système intègre une gestion sécurisée des commandes, un module de bons de livraison avec QR code, une gestion des stocks en temps réel et une synchronisation robuste des données avec le frontend mobile.

---

## Sommaire

- [Architecture](#architecture)
- [Sécurité et Gestion des Secrets](#sécurité-et-gestion-des-secrets)
- [Variables d'Environnement](#variables-denvironnement)
- [Prérequis](#prérequis)
- [Installation et Configuration](#installation-et-configuration)
  - [1. Cloner le dépôt](#1-cloner-le-dépôt)
  - [2. Configurer la base de données MySQL](#2-configurer-la-base-de-données-mysql)
  - [3. Configurer le fichier d'environnement (.env)](#3-configurer-le-fichier-denvironnement-env)
- [Démarrage de l'Application](#démarrage-de-lapplication)
  - [Option A : Script Windows PowerShell (Recommandé)](#option-a--script-windows-powershell-recommandé)
  - [Option B : Lancement manuel Maven](#option-b--lancement-manuel-maven)
  - [Option C : Docker Compose](#option-c--docker-compose)
- [Endpoints API](#endpoints-api)
- [Format des Réponses](#format-des-réponses)
- [Module Bon de Livraison & QR Code](#module-bon-de-livraison--qr-code)
- [Synchronisation Flutter ↔ Backend](#synchronisation-flutter--backend)
- [AI Lab — Quality Guardian](#-ai-lab--quality-guardian)
- [Tests et Validation](#tests-et-validation)
- [Licence](#licence)

---

## Architecture

L'application suit une architecture en couches respectant les principes SOLID :

```
Controller  →  Service  →  Repository  →  Base de données MySQL
     ↕              ↕
    DTO         Entity (JPA)
     ↕
  Security (JWT Stateless)
```

> **Note d'ingénierie (PFE)** — Afin d'éviter les problèmes de sérialisation liés aux relations JPA et au lazy loading, l'API utilise des DTOs (Data Transfer Objects) comme unique interface d'échange avec l'application mobile Flutter.

---

## Sécurité et Gestion des Secrets

L'application applique une politique stricte de **Zero Hardcoded Secret** :

1. **Aucun secret dans le code source** : Aucun mot de passe, clé secrète JWT ou jeton API externe n'est présent dans les fichiers versionnés (`application.properties`, `application*.yml`, `docker-compose.yml`, code Java).
2. **Isolation par `.env`** : Toutes les valeurs sensibles sont externalisées dans un fichier `.env` local, strictement ignoré par Git (`.gitignore`).
3. **Modèle de référence `.env.example`** : Un modèle documenté est versionné pour faciliter le déploiement sur tout nouvel environnement.
4. **Validation au démarrage** : Le script de lancement et Spring Boot vérifient la présence des variables obligatoires sans jamais exposer leurs valeurs dans les logs ou les consoles.
5. **Authentification Stateless JWT** : Les routes protégées nécessitent le header standard :
   ```http
   Authorization: Bearer <votre_token_jwt>
   ```

### Rôles applicatifs

| Rôle | Périmètre d'accès |
|---|---|
| `CLIENT` | Passer des commandes, initier des paiements, scanner les bons de livraison QR |
| `ARTISAN` | Gérer ses produits, consulter ses commandes, générer des bons de livraison PDF |
| `ADMIN` | Accès complet aux ressources d'administration, catégories et utilisateurs |

---

## Variables d'Environnement

Le tableau ci-dessous récapitule les variables gérées par le backend :

| Variable | Description | Obligatoire | Valeur par défaut / Exemple |
|---|---|:---:|---|
| `DB_URL` | URL JDBC de la base de données MySQL | Oui | `jdbc:mysql://localhost:3306/marketplace_local?useSSL=false&serverTimezone=UTC` |
| `DB_USERNAME` | Utilisateur MySQL de l'application | Oui | `marketplace_dev` |
| `DB_PASSWORD` | Mot de passe du compte MySQL applicatif | **Oui** | *(Défini localement)* |
| `JWT_SECRET` | Clé secrète de signature HMAC (>= 256 bits) | **Oui** | *(Min 32 caractères aléatoires)* |
| `JWT_EXPIRATION` | Durée de validité du token en ms | Non | `86400000` (24 heures) |
| `PORT` | Port d'écoute du serveur Spring Boot | Non | `8080` |
| `SPRING_PROFILES_ACTIVE`| Profil Spring actif (`dev`, `prod`) | Non | Non défini (profil par défaut) |
| `CLOUDINARY_CLOUD_NAME` | Nom du cloud Cloudinary pour stockage médias | Non | Dépend de l'environnement |
| `CLOUDINARY_API_KEY` | Clé API Cloudinary | Non | Dépend de l'environnement |
| `CLOUDINARY_API_SECRET`| Secret API Cloudinary | Non | Dépend de l'environnement |
| `FCM_PROJECT_ID` | Identifiant du projet Firebase Cloud Messaging | Non | Dépend de l'environnement |
| `FCM_SERVICE_ACCOUNT_PATH` | Chemin vers la clé de service Firebase | Non | Dépend de l'environnement |

---

## Prérequis

| Composant | Version minimale requise |
|---|---|
| **Java JDK** | 17 (Eclipse Temurin, OpenJDK ou Oracle JDK) |
| **MySQL Server** | 8.0+ |
| **Maven** | 3.6+ *(ou utiliser le wrapper inclus `./mvnw` / `mvnw.cmd`)* |
| **Python** | 3.9+ *(optionnel, requis pour Quality Guardian)* |

---

## Installation et Configuration

### 1. Cloner le dépôt

```bash
git clone https://github.com/macModa/marketplace.git
cd marketplace
```

### 2. Configurer la base de données MySQL

Connectez-vous à MySQL en administrateur :

```sql
-- Création de la base de données locale
CREATE DATABASE marketplace_local CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Création d'un utilisateur dédié (bonnes pratiques : ne pas utiliser root)
CREATE USER 'marketplace_dev'@'localhost' IDENTIFIED BY 'VotreMotDePasseSecurise';

-- Attribution des privilèges sur la base applicative
GRANT ALL PRIVILEGES ON marketplace_local.* TO 'marketplace_dev'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Configurer le fichier d'environnement (.env)

Copiez le modèle fourni vers votre fichier local `.env` :

```bash
# Sous Windows PowerShell :
Copy-Item .env.example .env

# Sous Linux / macOS / Git Bash :
cp .env.example .env
```

Éditez le fichier `.env` avec vos identifiants réels :

```ini
DB_URL=jdbc:mysql://localhost:3306/marketplace_local?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=marketplace_dev
DB_PASSWORD=VotreMotDePasseSecurise
JWT_SECRET=CleSecreteTresLongueEtSecuriseeAuMoins256Bits123456
```

> **Attention** : Ne committez JAMAIS le fichier `.env`. Il est protégé par `.gitignore`.

---

## Démarrage de l'Application

### Option A : Script Windows PowerShell (Recommandé)

Un script automatisé est disponible pour charger le `.env`, tester la connectivité MySQL et lancer Spring Boot :

```powershell
.\run-marketplace.ps1
```

Le script réalise automatiquement :
1. La lecture sécurisée des variables depuis `.env`.
2. L'injection dans la portée du processus sans affichage console.
3. La vérification de la présence des variables obligatoires (`DB_PASSWORD`, `JWT_SECRET`).
4. Le test de connexion TCP vers MySQL sur le port 3306.
5. Le démarrage du serveur Spring Boot.

### Option B : Lancement manuel Maven

**Sous Windows PowerShell :**
```powershell
# Injection des variables dans la session
Get-Content .env | Where-Object { $_ -notmatch '^\s*#' -and $_ -match '=' } | ForEach-Object {
    $k, $v = $_ -split '=', 2
    [System.Environment]::SetEnvironmentVariable($k.Trim(), $v.Trim(), "Process")
}

# Lancement
.\mvnw.cmd spring-boot:run
```

**Sous Linux / macOS / Bash :**
```bash
set -a
source .env
set +a

./mvnw spring-boot:run
```

L'API est prête et accessible sur `http://localhost:8080`.

### Option C : Docker Compose

Pour démarrer un conteneur MySQL isolé avec vos variables `.env` :

```bash
docker-compose up -d
```

---

## Endpoints API

### Authentification (`/api/auth`)

| Méthode | Route | Accès | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Inscription (Artisan ou Client) |
| `POST` | `/api/auth/login` | Public | Connexion, retourne le jeton JWT |

### Produits (`/api/products`)

| Méthode | Route | Accès | Description |
|---|---|---|---|
| `GET` | `/api/products` | Public | Liste paginée des produits |
| `GET` | `/api/products/{id}` | Public | Consultation d'un produit par ID |
| `GET` | `/api/products/category/{categoryId}` | Public | Produits d'une catégorie |
| `GET` | `/api/products/artisan/{artisanId}` | Public | Produits publiés par un artisan |
| `GET` | `/api/products/search?keyword=...` | Public | Recherche plein texte |
| `GET` | `/api/products/available` | Public | Produits en stock disponible |
| `POST` | `/api/products` | `ARTISAN` | Création d'un produit |
| `PUT` | `/api/products/{id}` | `ARTISAN` | Modification d'un produit existant |
| `DELETE` | `/api/products/{id}` | `ARTISAN` | Suppression logique d'un produit |

### Catégories (`/api/categories`)

| Méthode | Route | Accès | Description |
|---|---|---|---|
| `GET` | `/api/categories` | Public | Liste des catégories actives |
| `GET` | `/api/categories/{id}` | Public | Détail d'une catégorie |
| `POST` | `/api/categories` | `ADMIN` | Création d'une catégorie |
| `PUT` | `/api/categories/{id}` | `ADMIN` | Mise à jour d'une catégorie |
| `DELETE` | `/api/categories/{id}` | `ADMIN` | Suppression d'une catégorie |

### Commandes (`/api/orders`)

| Méthode | Route | Accès | Description |
|---|---|---|---|
| `POST` | `/api/orders` | `CLIENT` | Créer une commande avec panier |
| `GET` | `/api/orders/{id}` | Connecté | Détail d'une commande (artisan/client) |
| `GET` | `/api/orders/my-orders` | `CLIENT` | Historique des commandes du client |
| `PUT` | `/api/orders/{id}/cancel` | `CLIENT` | Annulation d'une commande |

### Paiements (`/api/payments`)

| Méthode | Route | Accès | Description |
|---|---|---|---|
| `POST` | `/api/payments/order/{orderId}` | `CLIENT` | Initialiser le paiement d'une commande |
| `GET` | `/api/payments/order/{orderId}` | Connecté | Consulter le statut de paiement |
| `GET` | `/api/payments/{id}` | Connecté | Détail technique d'un paiement |

---

## Format des Réponses

Toutes les réponses de l'API suivent une structure normalisée et uniforme :

### Réponse avec succès :
```json
{
  "success": true,
  "message": "Opération réussie",
  "data": {
    "id": 1,
    "nom": "Poterie traditionnelle",
    "prix": 45.500
  },
  "timestamp": "2026-10-24T12:00:00"
}
```

### Réponse d'erreur :
```json
{
  "success": false,
  "message": "Ressource non trouvée avec l'identifiant : 42",
  "data": null,
  "timestamp": "2026-10-24T12:00:00"
}
```

---

## Module Bon de Livraison & QR Code

Un module de génération et de vérification de bons de livraison PDF est inclus :

- Génération PDF structurée (coordonnées expéditeur/destinataire, tableau des produits, poids).
- Montant en toutes lettres en français conforme aux normes bancaires et comptables tunisiennes.
- QR Code sécurisé contenant les informations de scellement de la livraison.
- Zone de signature électronique ou physique du client.

```
Artisan  → Génère le bon PDF avec QR code
   ↓
Livreur  → Remet le colis au destinataire
   ↓
Client   → Scanne le QR code via l'application mobile Flutter
   ↓
Backend  → Valide cryptographiquement l'intégrité et clôture la livraison
```

---

## Synchronisation Flutter ↔ Backend

- **Enums unifiés** :
  - `OrderStatus` : `PENDING`, `CONFIRMED`, `SHIPPED`, `DELIVERED`, `CANCELLED`
  - `PaymentStatus` : `PENDING`, `COMPLETED`, `FAILED`, `REFUNDED`
  - `PaymentMethod` : `CASH`, `CARD`, `MOBILE_MONEY`, `BANK_TRANSFER`
- **Gestion des valeurs nulles** : Champs optionnels systématiquement gérés pour prévenir toute interruption de parsing côté Flutter.
- **Codes Postaux Tunisiens** : Table normalisée et indexée pour l'autocomplétion des adresses dans l'application mobile.

---

## 🛡️ AI Lab — Quality Guardian

Dans le cadre du programme de fiabilité applicative (**AI Lab - Mois 1**), le projet intègre **Quality Guardian** (`ai-lab/month-01-quality-guardian`).

Cet outil autonome réalise des audits de sécurité et de conformité en lecture seule :
- Détection proactive des secrets en dur (`SECRET_ASSIGNMENT`, `PRIVATE_KEY`).
- Vérification des fichiers d'architecture obligatoires (`REQUIRED_FILE`, `PROJECT_ROOT`).
- Protection contre la divulgation : les valeurs détectées ne sont jamais écrites dans les rapports JSON.
- Tolérance aux faux-positifs documentaires (placeholders et exemples français).

### Exécuter un audit de sécurité :

```bash
cd ai-lab/month-01-quality-guardian
.\.venv\Scripts\python.exe cli.py scan --root ../.. --report reports/quality-report.json
```

Pour consulter la documentation complète du gardien, voir [README de Quality Guardian](ai-lab/month-01-quality-guardian/README.md).

---

## Tests et Validation

### 1. Exécuter la suite de tests Java Spring Boot

```powershell
# Charger l'environnement puis lancer les tests
Get-Content .env | Where-Object { $_ -notmatch '^\s*#' -and $_ -match '=' } | ForEach-Object {
    $k, $v = $_ -split '=', 2
    [System.Environment]::SetEnvironmentVariable($k.Trim(), $v.Trim(), "Process")
}
.\mvnw.cmd test
```

### 2. Exécuter les tests unitaires de Quality Guardian

```powershell
cd ai-lab\month-01-quality-guardian
.\.venv\Scripts\python.exe -m unittest discover -s tests -v
```

---

## Licence

Projet développé dans le cadre académique (PFE) — Marketplace artisanale. Tous droits réservés.
