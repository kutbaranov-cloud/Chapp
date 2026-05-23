# ЭТАП 1: Сборка проекта
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Копируем только файл зависимостей для кэширования
COPY pom.xml .
# Скачиваем зависимости (они сохранятся в кэше Docker, если pom.xml не менялся)
RUN mvn dependency:go-offline -B

# Копируем исходный код и собираем проект
COPY src ./src
RUN mvn clean package -DskipTests

# ЭТАП 2: Финальный (легкий) образ
FROM amazoncorretto:21
WORKDIR /app

# Копируем только готовый JAR-файл из первого этапа
COPY --from=build /app/target/*.jar messenger.jar

# Запуск приложения
ENTRYPOINT ["java", "-jar", "messenger.jar"]