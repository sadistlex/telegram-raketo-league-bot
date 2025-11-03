# Quick Start Guide

## ⚡ Get Started in 5 Minutes

### Prerequisites Check
- ✅ Java 21 installed? (`java -version`)
- ✅ Maven installed? (`mvn -version`)
- ✅ Docker installed? (`docker --version`)

### Step 1: Load Dependencies (Required)

Open terminal in project directory and run:

```bash
mvn clean install
```

**This will download all dependencies (~150MB). Wait for "BUILD SUCCESS".**

If you see errors about dependencies not found, this is normal before running the command above.

### Step 2: Setup Telegram Bot

1. Message [@BotFather](https://t.me/botfather) on Telegram
2. Send: `/newbot`
3. Follow instructions to create bot
4. Copy the bot token (looks like: `123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11`)
5. Remember your bot username (e.g., `my_tennis_bot`)

### Step 3: Configure Environment

```bash
copy .env.example .env
```

Edit `.env` file:
```env
TELEGRAM_BOT_TOKEN=YOUR_ACTUAL_TOKEN_HERE
TELEGRAM_BOT_USERNAME=your_bot_username
```

### Step 4: Start Database

```bash
docker-compose up mysql -d
```

Wait 10 seconds for MySQL to initialize.

### Step 5: Run the Bot

```bash
mvn spring-boot:run
```

Wait for: `Started TelegramLeagueBotApplication`

### Step 6: Create Admin User

1. Get your Telegram ID from [@userinfobot](https://t.me/userinfobot)

2. Connect to database:
```bash
docker exec -it raketo-league-mysql mysql -u raketo -praketopassword raketo_league
```

3. Insert yourself as admin:
```sql
INSERT INTO admin_users (telegram_id, telegram_username, first_name, is_active, created_at)
VALUES (YOUR_ID, 'your_username', 'Your Name', 1, NOW());
```

4. Exit: `exit`

### Step 7: Test the Bot

1. Find your bot on Telegram
2. Send: `/start`
3. You should see: "Welcome Admin! Use /admin to see available commands."
4. Send: `/admin`
5. You should see the admin commands list

## ✅ Success!

Your bot is running! The structure is complete, now you can start implementing features.

---

## Alternative: Full Docker Setup

If you want everything in Docker:

```bash
docker-compose up --build
```

Then follow Step 6 to add admin user.

---

## Troubleshooting

### "Cannot resolve" errors in IDE
**Solution**: Run `mvn clean install` first, then reload project in IDE

### MySQL connection refused
**Solution**: 
```bash
docker-compose down
docker-compose up mysql -d
```
Wait 10 seconds before starting bot.

### Bot not responding
**Solution**: Check that bot token in `.env` is correct (no quotes, no spaces)

---

## Next: Start Implementing Features

See `PROJECT_SUMMARY.md` for what to implement next.

**Start with**: Tournament creation flow in `AdminCommandHandler.java`

