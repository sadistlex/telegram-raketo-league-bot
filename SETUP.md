# Setup Instructions

## Initial Setup Steps

### 1. Load Maven Dependencies

The project structure is created but Maven dependencies need to be loaded. In IntelliJ IDEA:

1. Open the project in IntelliJ IDEA
2. Wait for IntelliJ to detect the `pom.xml` file
3. Click "Load Maven Changes" when prompted (or right-click `pom.xml` → Maven → Reload Project)
4. Wait for all dependencies to download (this may take a few minutes)

Alternatively, from command line:
```bash
mvn clean install
```

### 2. Configure Telegram Bot

1. Create a bot via [@BotFather](https://t.me/botfather) on Telegram:
   - Send `/newbot`
   - Follow instructions to get your bot token
   - Note your bot username

2. Copy environment file:
   ```bash
   copy .env.example .env
   ```

3. Edit `.env` file and add your bot credentials:
   ```
   TELEGRAM_BOT_TOKEN=123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11
   TELEGRAM_BOT_USERNAME=your_bot_username
   ```

### 3. Setup Database

#### Option A: Using Docker (Recommended)

```bash
docker-compose up mysql -d
```

This will start MySQL on port 3306 with:
- Database: `raketo_league`
- Username: `raketo`
- Password: `raketopassword`

#### Option B: Using Local MySQL

1. Install MySQL 8.0
2. Create database:
   ```sql
   CREATE DATABASE raketo_league;
   CREATE USER 'raketo'@'localhost' IDENTIFIED BY 'raketopassword';
   GRANT ALL PRIVILEGES ON raketo_league.* TO 'raketo'@'localhost';
   FLUSH PRIVILEGES;
   ```

3. Update `application.yml` if needed

### 4. Run the Application

#### Option A: From IntelliJ
1. Right-click `TelegramLeagueBotApplication.java`
2. Select "Run 'TelegramLeagueBotApplication'"

#### Option B: From Command Line
```bash
mvn spring-boot:run
```

#### Option C: Using Docker
```bash
docker-compose up -d
```

### 5. Create First Admin User

Since authentication is based on Telegram username, you need to manually add your first admin:

1. Connect to MySQL:
   ```bash
   mysql -u raketo -p raketo_league
   ```

2. Insert your admin user:
   ```sql
   INSERT INTO admin_users (telegram_id, telegram_username, first_name, is_active, created_at)
   VALUES (YOUR_TELEGRAM_ID, 'your_telegram_username', 'Your Name', 1, NOW());
   ```

   To find your Telegram ID, use [@userinfobot](https://t.me/userinfobot)

### 6. Test the Bot

1. Open Telegram and find your bot by username
2. Send `/start` - you should see the admin welcome message
3. Send `/admin` - you should see available admin commands

## Project Structure Overview

```
telegram-raketo-league-bot/
├── src/main/java/com/raketo/league/
│   ├── model/                    # JPA entities (Player, Tournament, Match, etc.)
│   ├── repository/               # Spring Data JPA repositories
│   ├── service/                  # Business logic services
│   ├── controller/              # REST API and web controllers
│   ├── telegram/                # Telegram bot logic
│   │   ├── TelegramBot.java     # Main bot class
│   │   └── handler/             # Command handlers
│   └── TelegramLeagueBotApplication.java
├── src/main/resources/
│   ├── application.yml          # Main configuration
│   └── templates/
│       └── calendar.html        # Web calendar interface
├── src/test/                    # Unit tests
├── pom.xml                      # Maven configuration
├── docker-compose.yml           # Docker deployment
├── Dockerfile                   # Container build
└── .env.example                 # Environment template
```

## Common Issues

### Issue: Dependencies not resolving
**Solution**: 
- Run `mvn clean install`
- In IntelliJ: File → Invalidate Caches → Invalidate and Restart

### Issue: MySQL connection refused
**Solution**: 
- Check if MySQL is running: `docker ps`
- Verify connection settings in `.env`
- Check port 3306 is not in use

### Issue: Bot not responding
**Solution**:
- Verify bot token in `.env` is correct
- Check application logs for errors
- Ensure bot is not running elsewhere with same token

## Next Steps

1. **Implement Admin Features**:
   - Tournament creation
   - Player registration
   - Division management

2. **Implement Player Features**:
   - View schedule
   - Submit availability
   - Report match results

3. **Implement Scheduling Logic**:
   - Automatic player pairing
   - Time slot matching
   - Notification system

## Development Commands

```bash
# Build project
mvn clean package

# Run tests
mvn test

# Run application
mvn spring-boot:run

# Start with Docker
docker-compose up -d

# View logs
docker-compose logs -f bot

# Stop services
docker-compose down

# Rebuild Docker image
docker-compose up --build
```

## Database Schema

The application will automatically create tables on startup (via Hibernate DDL). Main tables:

- `tournaments` - Tournament information
- `divisions` - Divisions within tournaments
- `tours` - 2-week tour periods
- `players` - Player information
- `matches` - Match pairings and results
- `availability_slots` - Player availability (green/yellow/red)
- `admin_users` - Bot administrators

## Configuration Files

- `application.yml` - Main Spring Boot configuration
- `.env` - Environment-specific settings (not in git)
- `pom.xml` - Maven dependencies and build configuration
- `docker-compose.yml` - Docker deployment setup

## Support

For questions or issues:
1. Check the main README.md
2. Review application logs
3. Check database for data integrity

