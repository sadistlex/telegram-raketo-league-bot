# Telegram Raketo League Bot

A Telegram bot for managing tennis league tournaments with automated player pairing, scheduling, and match coordination.

## Features

- **Tournament Management**: Create and manage multiple tournaments with divisions
- **Automated Scheduling**: 2-week tour system with automatic player pairing
- **Availability Calendar**: Interactive web-based calendar for players to set their availability (green/yellow/red)
- **Match Coordination**: View schedules, track matches, and manage postponements
- **Admin Controls**: Full tournament and player management capabilities
- **Notifications**: Automated reminders and updates for players

## Technology Stack

- **Backend**: Java 21, Spring Boot 3.3.5
- **Database**: MySQL 8.0 (H2 for testing)
- **Telegram Integration**: TelegramBots Library (7.10.0)
- **Build Tool**: Maven
- **Deployment**: Docker & Docker Compose

## Project Structure

```
telegram-raketo-league-bot/
├── src/
│   ├── main/
│   │   ├── java/com/raketo/league/
│   │   │   ├── model/              # Domain entities
│   │   │   │   ├── Player.java
│   │   │   │   ├── Tournament.java
│   │   │   │   ├── Division.java
│   │   │   │   ├── Tour.java
│   │   │   │   ├── Match.java
│   │   │   │   ├── AvailabilitySlot.java
│   │   │   │   └── AdminUser.java
│   │   │   ├── repository/         # Data access layer
│   │   │   ├── service/           # Business logic
│   │   │   ├── controller/        # REST API & Web controllers
│   │   │   ├── telegram/          # Telegram bot integration
│   │   │   │   ├── TelegramBot.java
│   │   │   │   └── handler/
│   │   │   │       ├── AdminCommandHandler.java
│   │   │   │       └── PlayerCommandHandler.java
│   │   │   └── TelegramLeagueBotApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── templates/
│   │           └── calendar.html   # Web calendar UI
│   └── test/
│       └── resources/
│           └── application-test.yml
├── pom.xml
├── Dockerfile
├── docker-compose.yml
└── README.md
```

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.6+
- MySQL 8.0 (or Docker)
- Telegram Bot Token (from [@BotFather](https://t.me/botfather))

### Local Development Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd telegram-raketo-league-bot
   ```

2. **Configure environment variables**
   ```bash
   copy .env.example .env
   ```
   Edit `.env` and add your Telegram bot token:
   ```
   TELEGRAM_BOT_TOKEN=your_actual_bot_token
   TELEGRAM_BOT_USERNAME=your_bot_username
   ```

3. **Start MySQL (using Docker)**
   ```bash
   docker-compose up mysql -d
   ```

4. **Build and run the application**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

### Docker Deployment

1. **Configure environment variables**
   ```bash
   copy .env.example .env
   ```
   Edit `.env` with your settings.

2. **Start all services**
   ```bash
   docker-compose up -d
   ```

3. **View logs**
   ```bash
   docker-compose logs -f bot
   ```

4. **Stop services**
   ```bash
   docker-compose down
   ```

## Configuration

### Database

The application uses MySQL by default. Configuration is in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/raketo_league
    username: root
    password: password
```

For testing, H2 in-memory database is used automatically.

### Telegram Bot

Set these environment variables:

- `TELEGRAM_BOT_TOKEN`: Your bot token from BotFather
- `TELEGRAM_BOT_USERNAME`: Your bot username
- `WEBAPP_URL`: URL for the web calendar (default: http://localhost:8080/webapp)

## Usage

### Admin Commands

- `/start` - Initialize the bot
- `/admin` - Show admin commands
- `/createtournament` - Create a new tournament
- `/addplayer @username` - Add a player to a division
- `/viewschedule` - View tournament schedule
- `/listtournaments` - List all tournaments
- `/listplayers` - List all players

### Player Commands

- `/start` - Register or greet the player
- `/schedule` - View match schedule
- `/mymatches` - View your matches
- `/setavailability` - Open calendar to set availability
- `/help` - Show available commands

## API Endpoints

### Availability API

- `GET /api/availability/player/{playerId}` - Get player availability
- `GET /api/availability/match/{matchId}` - Get match availability
- `POST /api/availability` - Save availability slot
- `DELETE /api/availability/{slotId}` - Delete availability slot

### Web App

- `GET /webapp/calendar` - Interactive availability calendar

## Development Roadmap

### Phase 1: Core Structure ✅
- [x] Project setup
- [x] Database models
- [x] Repository layer
- [x] Service layer
- [x] Basic bot structure

### Phase 2: Admin Features (Next)
- [ ] Tournament creation and management
- [ ] Player registration system
- [ ] Division management
- [ ] Tour generation and scheduling
- [ ] Match pairing algorithm

### Phase 3: Player Features
- [ ] View match schedule
- [ ] Set availability via calendar
- [ ] Match result reporting
- [ ] Postponement requests
- [ ] View standings

### Phase 4: Advanced Features
- [ ] Automatic time slot matching
- [ ] Notification system
- [ ] Statistics and leaderboards
- [ ] Match reminders
- [ ] Admin dashboard

## Testing

Run tests with:

```bash
mvn test
```

Tests use H2 in-memory database and mock Telegram API.

## Deployment Options

### Option 1: Docker Compose (Recommended)
- Easy setup with included docker-compose.yml
- MySQL and app in isolated containers
- Persistent data storage

### Option 2: Standalone JAR
- Build: `mvn clean package`
- Run: `java -jar target/telegram-league-bot-1.0.0-SNAPSHOT.jar`
- Requires external MySQL instance

### Option 3: Cloud Platforms
- Deploy to any platform supporting Java applications
- Examples: Heroku, AWS, DigitalOcean, Railway
- Configure environment variables accordingly

## Contributing

This is a personal project, but suggestions and feedback are welcome!

## License

MIT License

## Support

For issues or questions, please open a GitHub issue.

