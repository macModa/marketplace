# ====== BUILD STAGE ======
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copier le pom et télécharger les dépendances
COPY pom.xml .
RUN mvn -B -q -e -DskipTests dependency:resolve

# Copier le code source
COPY src ./src

# Build du projet
RUN mvn -B -e clean package -DskipTests

# ====== RUN STAGE ======
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copier le jar généré
COPY --from=build /app/target/*.jar app.jar

# Port utilisé par Railway
EXPOSE 8080

# Lancer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]
