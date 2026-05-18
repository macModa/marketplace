# Marketplace API — Spring Boot

Backend REST API d'une application marketplace artisanale tunisienne, conçu pour être consommé par une application mobile Flutter. Le système intègre une gestion sécurisée des commandes, un module de bons de livraison avec QR code, et une synchronisation robuste des données avec le frontend mobile.

---

## Architecture

L'application suit une architecture en couches respectant les principes SOLID :

```
Controller  →  Service  →  Repository  →  Base de données
     ↕              ↕
    DTO         Entity (JPA)
     ↕
  Security (JWT)
```

> **Note PFE** — Afin d'éviter les problèmes de sérialisation liés aux relations JPA et au lazy loading, l'API utilise des DTOs comme seule interface d'échange de données avec l'application mobile Flutter.

---

## Prérequis

| Outil | Version minimale |
|---|---|
| Java | 17 |
| Maven | 3.6 |
| MySQL | 8.0 |

---

## Installation

### 1. Cloner le dépôt

```bash
git clone <repository-url>
cd boot
```

### 2. Créer la base de données

```sql
CREATE DATABASE marketplace_db;
```

### 3. Configurer `application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/marketplace_db
    username: votre_username
    password: votre_password
```

### 4. Démarrer l'application

```bash
mvn spring-boot:run
```

L'API est accessible sur `http://localhost:8080`.

---

## Endpoints

### Authentification

| Méthode | Route | Description |
|---|---|---|
| POST | `/api/auth/register` | Inscription (Artisan ou Client) |
| POST | `/api/auth/login` | Connexion, retourne un token JWT |

### Produits

| Méthode | Route | Accès | Description |
|---|---|---|---|
| GET | `/api/products` | Public | Liste paginée |
| GET | `/api/products/{id}` | Public | Détail d'un produit |
| GET | `/api/products/category/{categoryId}` | Public | Par catégorie |
| GET | `/api/products/artisan/{artisanId}` | Public | Par artisan |
| GET | `/api/products/search?keyword=...` | Public | Recherche textuelle |
| GET | `/api/products/available` | Public | Produits en stock |
| POST | `/api/products` | Artisan | Créer un produit |
| PUT | `/api/products/{id}` | Artisan | Modifier un produit |
| DELETE | `/api/products/{id}` | Artisan | Supprimer un produit |

### Catégories

| Méthode | Route | Accès |
|---|---|---|
| GET | `/api/categories` | Public |
| GET | `/api/categories/{id}` | Public |
| POST | `/api/categories` | Admin |
| PUT | `/api/categories/{id}` | Admin |
| DELETE | `/api/categories/{id}` | Admin |

### Commandes

| Méthode | Route | Description |
|---|---|---|
| POST | `/api/orders` | Créer une commande |
| GET | `/api/orders/{id}` | Détail d'une commande |
| GET | `/api/orders/my-orders` | Historique du client connecté |
| PUT | `/api/orders/{id}/cancel` | Annuler une commande |

### Paiements

| Méthode | Route | Description |
|---|---|---|
| POST | `/api/payments/order/{orderId}` | Initier un paiement |
| GET | `/api/payments/order/{orderId}` | Paiement lié à une commande |
| GET | `/api/payments/{id}` | Détail d'un paiement |

---

## Sécurité

L'authentification est stateless via **JWT (JSON Web Token)**.

Inclure le token dans chaque requête protégée :

```
Authorization: Bearer <token>
```

### Rôles

| Rôle | Permissions |
|---|---|
| `CLIENT` | Passer des commandes, effectuer des paiements, scanner les bons de livraison |
| `ARTISAN` | Gérer ses produits, générer des bons de livraison pour ses commandes |
| `ADMIN` | Accès complet à toutes les ressources |

---

## Modèle de données

```
User (abstrait)
├── Artisan       → publie des produits
└── Client        → passe des commandes

Category          → classe les produits
Product           → rattaché à un Artisan et une Category
Order             → passée par un Client
├── OrderLine     → lignes de commande (produit + quantité)
└── Payment       → paiement associé à la commande
```

---

## Format des réponses

Toutes les réponses respectent une enveloppe standard :

```json
{
  "success": true,
  "message": "Opération réussie",
  "data": { ... },
  "timestamp": "2024-01-01T12:00:00"
}
```

En cas d'erreur :

```json
{
  "success": false,
  "message": "Description de l'erreur",
  "data": null,
  "timestamp": "2024-01-01T12:00:00"
}
```

---

## Module Bon de Livraison

Un système sécurisé de bons de livraison PDF est intégré, incluant :

- Mise en page structurée (expéditeur / destinataire, tableau produits avec poids)
- Montant en toutes lettres en français (ex. : *Cent quarante-cinq dinars et cinq cents millimes*)
- QR code de validation scannable par le client depuis l'application mobile
- Zone de signature client

**Flux de sécurité :**

```
Artisan  →  génère le bon PDF (vérification propriété commande)
Client   →  scanne le QR code pour valider la réception
Backend  →  vérifie que le scanner est bien le client de la commande
```

> **Note PFE** — Afin d'optimiser la fiabilité des livraisons, un module interne de détection automatique des codes postaux tunisiens a été implémenté. Ce module repose sur une table normalisée et indexée, exposée via une API REST sécurisée, garantissant la cohérence des données entre l'application mobile Flutter et le backend Spring Boot.

---

## Synchronisation Flutter ↔ Backend

La couche de communication avec l'application mobile a été renforcée sur plusieurs points :

**Enums** — Les valeurs sont désormais alignées entre les deux plateformes via des mappers dédiés :

| Domaine | Valeurs |
|---|---|
| `PaymentMethod` | `CASH`, `CARD`, `MOBILE_MONEY`, `BANK_TRANSFER` |
| `PaymentStatus` | `PENDING`, `COMPLETED`, `FAILED`, `REFUNDED` |
| `OrderStatus` | `PENDING`, `CONFIRMED`, `SHIPPED`, `DELIVERED`, `CANCELLED` |

**Null safety** — Les champs optionnels (ex. `dateModification`) sont correctement marqués nullable dans les DTOs pour éviter les crashes à la désérialisation.

---

## Technologies

| Composant | Technologie |
|---|---|
| Framework | Spring Boot 3.2.0 |
| ORM | Spring Data JPA / Hibernate |
| Sécurité | Spring Security + JWT (jjwt) |
| Base de données | MySQL 8 |
| Réduction boilerplate | Lombok |
| Logging | SLF4J / Logback |

---

## Bonnes pratiques implémentées

- Architecture en couches (Controller → Service → Repository)
- Séparation des responsabilités (principes SOLID)
- Validation des entrées via Bean Validation
- Gestion globale des exceptions avec `@ControllerAdvice`
- Transactions `@Transactional` sur les opérations critiques (commande, paiement, stock)
- Pagination sur toutes les listes
- DTOs pour l'isolation des entités JPA
- Indexation des colonnes fréquemment interrogées
- Logging structuré avec rotation automatique (`logs/marketplace.log`)

---

## Environnements

| Profil | Fichier |
|---|---|
| Développement | `application-dev.yml` |
| Production | `application-prod.yml` |

Activer un profil :

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## Tests

```bash
mvn test
```

---

## Pistes d'amélioration

- Tests unitaires et d'intégration (JUnit 5 / Mockito)
- Documentation interactive avec Swagger / OpenAPI 3
- Cache Redis pour les codes postaux et les données fréquentes
- Upload et stockage d'images produits
- Notifications en temps réel (WebSocket ou FCM)
- Dockerisation complète (Docker Compose)
- Fuzzy search sur les adresses (tolérance aux fautes de frappe)

---

## Licence

Projet éducatif — PFE.
