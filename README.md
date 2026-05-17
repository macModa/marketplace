Niveau PFE (À écrire dans le rapport)

« Afin d’optimiser la fiabilité des livraisons, un module interne de détection automatique des codes postaux tunisiens a été implémenté. Ce module repose sur une table normalisée et indexée, exposée via une API REST sécurisée, garantissant la cohérence des données entre l’application mobile Flutter et le backend Spring Boot. »

🚀 Option Avancée (Professionnelle)

Si vous voulez du niveau entreprise :

Cache Redis des codes postaux

Normalisation Unicode (suppression accents automatique)

Fuzzy search (Tolérance fautes frappe)

API interne /validate-address

Journalisation des tentatives invalides

https://chatgpt.com/share/697901ab-2868-800d-8b56-fcf4eac47f7e

# Marketplace Backend - Spring Boot Application
https://chatgpt.com/gg/v/697903fe500c81a0ab8de082084ccb9a?token=2Rn6pVK-4p9-fR9YbkjE3w
Backend REST API pour une application marketplace développée avec Spring Boot. Cette API est conçue pour être consommée par une application mobile Flutter.

## 🏗️ Architecture

L'application suit une architecture en couches professionnelle :

- **Controller** : Gestion des requêtes HTTP REST
- **Service** : Logique métier et transactions
- **Repository** : Accès aux données avec Spring Data JPA
- **Entity** : Modèles de domaine JPA
- **DTO** : Objets de transfert de données pour les API
- **Security** : Authentification JWT et autorisations
- **Exception** : Gestion globale des erreurs

## 📋 Prérequis

- Java 17 ou supérieur
- Maven 3.6+
- MySQL 8.0+
- IDE (IntelliJ IDEA, Eclipse, VS Code)

## 🚀 Installation et Démarrage

### 1. Cloner le projet

```bash
git clone <repository-url>
cd boot
```

### 2. Configurer la base de données

Créer une base de données MySQL :

```sql
CREATE DATABASE marketplace_db;
```

### 3. Configurer l'application

Modifier `src/main/resources/application.yml` avec vos paramètres de base de données :

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/marketplace_db
    username: votre_username
    password: votre_password
```

### 4. Lancer l'application

```bash
mvn spring-boot:run
```

L'application sera accessible sur `http://localhost:8080`

## 📚 API Endpoints

### Authentification

- `POST /api/auth/register` - Inscription (Artisan ou Client)
- `POST /api/auth/login` - Connexion

### Produits (Public)

- `GET /api/products` - Liste paginée des produits
- `GET /api/products/{id}` - Détails d'un produit
- `GET /api/products/category/{categoryId}` - Produits par catégorie
- `GET /api/products/artisan/{artisanId}` - Produits d'un artisan
- `GET /api/products/search?keyword=...` - Recherche de produits
- `GET /api/products/available` - Produits disponibles

### Produits (Artisan)

- `POST /api/products` - Créer un produit
- `PUT /api/products/{id}` - Modifier un produit
- `DELETE /api/products/{id}` - Supprimer un produit

### Catégories

- `GET /api/categories` - Liste des catégories
- `GET /api/categories/{id}` - Détails d'une catégorie
- `POST /api/categories` - Créer une catégorie (Admin)
- `PUT /api/categories/{id}` - Modifier une catégorie (Admin)
- `DELETE /api/categories/{id}` - Supprimer une catégorie (Admin)

### Commandes (Client)

- `POST /api/orders` - Créer une commande
- `GET /api/orders/{id}` - Détails d'une commande
- `GET /api/orders/my-orders` - Mes commandes
- `PUT /api/orders/{id}/cancel` - Annuler une commande

### Paiements (Client)

- `POST /api/payments/order/{orderId}` - Créer un paiement
- `GET /api/payments/order/{orderId}` - Paiement d'une commande
- `GET /api/payments/{id}` - Détails d'un paiement

## 🔐 Sécurité

L'application utilise JWT (JSON Web Token) pour l'authentification stateless.

### Rôles

- **ARTISAN** : Peut gérer ses produits
- **CLIENT** : Peut passer des commandes et effectuer des paiements
- **ADMIN** : Accès complet à toutes les fonctionnalités

### Utilisation du token

Inclure le token JWT dans le header Authorization :

```
Authorization: Bearer <token>
```

## 📦 Modèle de données

### Entités principales

- **User** (abstraite) : Base pour Artisan et Client
- **Artisan** : Utilisateur vendant des produits
- **Client** : Utilisateur achetant des produits
- **Category** : Catégorisation des produits
- **Product** : Produits vendus par les artisans
- **Order** : Commandes des clients
- **OrderLine** : Lignes de commande
- **Payment** : Paiements associés aux commandes

## 🛠️ Technologies utilisées

- **Spring Boot 3.2.0** : Framework principal
- **Spring Data JPA** : Accès aux données
- **Spring Security** : Sécurité et authentification
- **MySQL** : Base de données relationnelle
- **JWT (jjwt)** : Tokens d'authentification
- **Lombok** : Réduction du code boilerplate
- **SLF4J/Logback** : Logging

## 📝 Format de réponse API

Toutes les réponses suivent le format standard :

```json
{
  "success": true,
  "message": "Message descriptif",
  "data": { ... },
  "timestamp": "2024-01-01T12:00:00"
}
```

En cas d'erreur :

```json
{
  "success": false,
  "message": "Message d'erreur",
  "data": null,
  "timestamp": "2024-01-01T12:00:00"
}
```

## 🔄 Gestion des transactions

Les opérations critiques (création de commande, paiement, gestion de stock) sont gérées avec `@Transactional` pour garantir la cohérence des données.

## 📊 Logging

Les logs sont configurés avec SLF4J et Logback :
- Console : Niveau INFO
- Fichier : `logs/marketplace.log` (rotation automatique)
- Niveau DEBUG en développement

## 🧪 Tests

Pour exécuter les tests :

```bash
mvn test
```

## 🌍 Environnements

- **Development** : `application-dev.yml`
- **Production** : `application-prod.yml`

Activer un profil :

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 📖 Bonnes pratiques implémentées

- ✅ Architecture en couches (Controller → Service → Repository)
- ✅ Séparation des responsabilités (SOLID)
- ✅ Validation des entrées (Bean Validation)
- ✅ Gestion globale des exceptions
- ✅ Transactions pour opérations critiques
- ✅ Pagination pour les listes
- ✅ Logging professionnel
- ✅ Sécurité JWT stateless
- ✅ DTOs pour isolation des entités
- ✅ Indexation des bases de données

## 🚧 Améliorations futures

- Tests unitaires et d'intégration complets
- Documentation API avec Swagger/OpenAPI
- Cache avec Redis
- Upload d'images pour les produits
- Notifications en temps réel
- Dockerisation complète

## 👥 Auteur

Développé selon les spécifications du guide de développement Spring Boot marketplace.

## 📄 Licence

Ce projet est un projet éducatif.
///
Bonus (niveau PFE ⭐)

Dans ton rapport tu peux écrire :

« Afin d’éviter les problèmes de sérialisation liés aux relations JPA et au lazy loading, l’API utilise des DTOs pour l’échange de données avec l’application mobile Flutter. »


 Payment Model & Null Safety - Résumé des Correctifs
🎯 Problèmes Résolus
Mismatch Enum : Le backend attendait CASH, CARD, etc., mais Flutter envoyait ESPECE, CARTE_BANCAIRE.
Null Safety : Le backend renvoyait dateModification: null après un POST, causant un crash car Flutter l'attendait non-nullable.
Inconsistences Provider : Collision entre deux définitions de paymentServiceProvider et mauvaise instanciation de 
DioClient
.
📁 Changements Majeurs
1️⃣ Refactorisation des Enums (Méthode PRO)
Tous les enums ont été mis à jour pour utiliser des noms anglais propres avec des mappers pour synchroniser avec le backend Spring Boot.

PaymentMethod
 : card, mobileMoney, bankTransfer, cash.
PaymentStatus
 : pending, completed, failed, refunded.
OrderStatus
 : pending, confirmed, etc.
2️⃣ 
PaymentDto
 & Null Safety
✅ Ajout de dateModification en nullable (String?).
✅ Utilisation des mappers fromJson/
toJson
 personnalisés pour les enums.
✅ Protection contre les valeurs null pour montant.
@JsonKey(defaultValue: 0.0)
final double montant;
@JsonKey(fromJson: paymentMethodFromString, toJson: paymentMethodToString)
final PaymentMethod methode;
final String? dateModification; // ✅ Nullable
3️⃣ Nettoyage des Providers
✅ Suppression de 
payment_service_provider.dart
 (doublon).
✅ Centralisation dans 
payment_providers.dart
.
✅ Correction de l'instanciation de 
DioClient
 dans 
dio_provider.dart
.
4️⃣ 
PaymentService
✅ Injection de dépendance via le constructeur.
✅ Utilisation de noms de méthodes cohérents (
getPaymentByOrderId
).
🧪 Résultats de la Vérification
Test	Résultat
Génération Build	✅ Succès (build_runner)
Analyse Statique	✅ Zéro Erreur Importante
Mapping JSON	✅ Stable & Safe
Sync Backend	✅ Support MAJUSCULES & null
🚀 Prochaines Étapes
Hot Restart : Appliquez les changements.
Test Complet : Effectuez un paiement en choisissant "Espèce" (CASH) ou "Virement".
Log : Vérifiez que dateModification est bien null dans les logs sans faire crasher l'app.
L'application est maintenant parfaitement synchronisée avec votre backend Spring Boot ! 🚀


Walkthrough - Secure Delivery Note System
I have successfully implemented the secure delivery note system for the Tunisian artisan marketplace.

Changes Made
Backend (Spring Boot)
DeliveryService Refactor:
Now uses BonLivraisonDTO as the single source of truth.
Implemented a professional CP1-style PDF layout including:
Structured Sender/Receiver boxes.
Detailed product table with weights.
COD amount in French words (e.g., "Cent quarante-cinq dinars et cinq cents millimes").
Secure QR code for delivery validation.
Signature areas for the client.
OrderController & ArtisanOrderController:
Integrated BonLivraisonService to assemble complete data before PDF generation.
Enforced strict ownership checks (Artisan-only access).
Unified the delivery token lifecycle.
Frontend (Flutter)
ClientScanScreen:
Implemented a robust QR code parser that handles both absolute URLs and relative paths.
Added comprehensive error handling and user feedback (SnackBars, Dialogs).
BonLivraisonScreen:
Polished the preview UI to match the marketplace theme.
Verification Results
Backend
✅ mvn compile successful.
✅ Logic verified for ownership and token expiration.
Frontend
✅ flutter analyze verified (no issues in the modified features).
✅ UI verified for responsive layout and loading states.
Security Overview
Artisan: Can generate the official document for their orders.
Client: Can validate the physical receipt by scanning the QR code.
Backend Enforcement: Validates that the scanner is the actual client of the order.