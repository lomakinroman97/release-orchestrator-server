# Используем Eclipse Temurin JRE 17 (современный и стабильный)
FROM eclipse-temurin:17-jre

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем собранный JAR файл
COPY build/libs/release-orchestrator-1.0.0.jar app.jar

# Создаем пользователя для безопасности
RUN addgroup --system app && adduser --system --ingroup app app

# Меняем владельца файлов
RUN chown -R app:app /app

# Переключаемся на пользователя app
USER app

# Открываем порт
EXPOSE 8080

# Запускаем приложение
CMD ["java", "-jar", "app.jar"]
