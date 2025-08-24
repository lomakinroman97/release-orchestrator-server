#!/bin/bash

echo "🚀 Building Release Orchestrator..."

# Очищаем предыдущую сборку
./gradlew clean

# Собираем проект
./gradlew build

# Копируем зависимости
./gradlew copyDependencies

echo "✅ Build completed!"

echo "🐳 Building Docker image..."
docker build -t release-orchestrator .

echo "🚀 Starting Release Orchestrator..."
docker compose up --build

echo "🎉 Release Orchestrator is running on http://localhost:8080"
echo "📝 Check logs with: docker-compose logs -f"
