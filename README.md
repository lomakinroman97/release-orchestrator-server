# Release Orchestrator

Автоматизированный пайплайн релизов для Android-приложения с использованием MCP (GitHub), Yandex GPT API и GitHub REST API.

## Описание

Система автоматически анализирует новые коммиты в GitHub-репозитории, генерирует для них release notes с помощью ИИ и создает GitHub Release по команде через HTTP запрос.

## Архитектура

- **Kotlin + Ktor** - основной сервер
- **GitHub REST API** - работа с репозиторием и релизами
- **Yandex GPT API** - генерация release notes
- **Docker** - контейнеризация
- **Semantic Versioning** - автоматическое определение версии

## Основные компоненты

1. **ReleaseOrchestratorService** - основной оркестратор пайплайна
2. **GithubService** - взаимодействие с GitHub API
3. **YandexGptService** - генерация release notes через ИИ
4. **VersioningService** - управление семантическим версионированием

## API Endpoints

### POST /api/release/trigger
Запускает асинхронный пайплайн релиза.

**Request:**
```json
{
  "repository": "https://github.com/lomakinroman97/simpleAppDay15",
  "branch": "main",
  "forceVersion": "1.2.0" // опционально
}
```

**Response:**
```json
{
  "success": true,
  "message": "Release pipeline started",
  "version": null,
  "pipelineId": "1703123456789"
}
```

### POST /api/release/trigger/sync
Синхронный запуск пайплайна релиза (для тестирования).

### GET /health
Проверка состояния сервера.

## Запуск

### Локально

1. Убедитесь, что у вас установлена Java 17+
2. Запустите Gradle wrapper:
   ```bash
   ./gradlew build
   ./gradlew run
   ```

### Docker

1. Соберите образ:
   ```bash
   docker build -t release-orchestrator .
   ```

2. Запустите контейнер:
   ```bash
   docker run -p 8080:8080 release-orchestrator
   ```

### Docker Compose

1. Скопируйте файл с переменными окружения:
   ```bash
   cp env.example .env
   ```

2. Запустите сервис:
   ```bash
   docker compose up --build
   ```

### Быстрый запуск

Используйте готовый скрипт:
```bash
./build-and-run.sh
```

## Конфигурация

Основные настройки находятся в файле `.env` (создайте его на основе `env.example`):

- **GitHub Token**: Укажите ваш GitHub Personal Access Token
- **Yandex GPT API Key**: Укажите ваш Yandex GPT API ключ
- **Folder ID**: Укажите ваш Yandex GPT Folder ID

**Важно**: Никогда не коммитьте файл `.env` с реальными ключами в репозиторий!

## Логирование

Логи сохраняются в:
- Консоль (STDOUT)
- Файл: `logs/release-orchestrator.log`

## Тестирование

Для тестирования API можно использовать curl:

```bash
# Запуск пайплайна релиза
curl -X POST http://localhost:8080/api/release/trigger \
  -H "Content-Type: application/json" \
  -d '{
    "repository": "https://github.com/lomakinroman97/simpleAppDay15",
    "branch": "main"
  }'

# Проверка здоровья сервера
curl http://localhost:8080/health
```

## Планы развития

- [ ] Интеграция с Telegram Bot API
- [ ] Веб-интерфейс для мониторинга
- [ ] Поддержка различных стратегий версионирования
- [ ] Уведомления о статусе пайплайна
- [ ] Метрики и мониторинг
