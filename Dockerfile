# 1. Слой сборки
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Копируем сначала ТОЛЬКО файл зависимостей
COPY pom.xml .

# ИСПОЛЬЗУЕМ ЭТУ КОМАНДУ!
# Она скачивает зависимости ОДИН РАЗ и кэширует их в Docker.
# Пока pom.xml не изменится, этот шаг будет пропускаться.
RUN mvn dependency:go-offline -B

# Теперь копируем исходный код
COPY src ./src

# Собираем (теперь Maven не будет ничего качать, он возьмет все из кэша)
RUN mvn clean package -DskipTests

# 2. Финальный слой
FROM amazoncorretto:21
WORKDIR /app
COPY --from=build /app/target/*.jar messenger.jar
ENTRYPOINT ["java", "-jar", "messenger.jar"]