# Marketplace Backend - Spring Boot Application

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

