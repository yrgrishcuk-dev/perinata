# Скопируйте в local.secrets.ps1 и заполните.
# local.secrets.ps1 добавлен в .gitignore и не должен попадать в репозиторий.
$env:TELEGRAM_BOT_TOKEN = "123456789:AA..."
$env:TELEGRAM_BOT_USERNAME = "my_antenatal_bot"
$env:TELEGRAM_WEBHOOK_SECRET = "change-me-random-hex"

$env:MAX_BOT_TOKEN = ""
$env:MAX_BOT_USERNAME = ""
$env:MAX_WEBHOOK_SECRET = "change-me-random-hex"
