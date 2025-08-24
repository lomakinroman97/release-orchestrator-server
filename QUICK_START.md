# Быстрый старт Release Orchestrator

## Предварительные требования

- Java 17+
- Docker Desktop
- Git

## Быстрый запуск

1. **Клонируйте репозиторий:**
   ```bash
   git clone <your-repo-url>
   cd release-orchestrator
   ```

2. **Запустите готовый скрипт:**
   ```bash
   ./build-and-run.sh
   ```

   Скрипт автоматически:
   - Соберет проект
   - Скопирует зависимости
   - Соберет Docker образ
   - Запустит сервис

3. **Проверьте работу:**
   ```bash
   curl http://localhost:8080/health
   ```

## Ручной запуск

### Сборка проекта
```bash
./gradlew build
./gradlew copyDependencies
```

### Docker образ
```bash
docker build -t release-orchestrator .
```

### Запуск
```bash
docker compose up --build
```

## Тестирование API

### Проверка здоровья сервера
```bash
curl http://localhost:8080/health
```

### Запуск пайплайна релиза
```bash
curl -X POST http://localhost:8080/api/release/trigger \
  -H "Content-Type: application/json" \
  -d '{
    "repository": "https://github.com/lomakinroman97/simpleAppDay15",
    "branch": "main"
  }'
```

## Остановка

```bash
docker compose down
```

## Логи

```bash
docker compose logs -f
```
