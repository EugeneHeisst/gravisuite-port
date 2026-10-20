# Gravitation Suite UNOFFICIAL PORT

Неофициальный порт **Gravitation Suite 3.1.1** (SeNtiMeL / Chocohead) на **Minecraft 1.21.1 / NeoForge 21.1.256**, аддон **IndustrialCraft 2 UNOFFICIAL PORT 0.1.23**. Версия **0.1.5**.

Семь компонентов, Advanced/Ultimate Lappack, Advanced Drill с четырьмя режимами и добычей 3×3, Advanced Chainsaw, GraviTool с четырьмя режимами, Vajra, Advanced Jetpack, Advanced NanoChestplate и GraviChestPlate. Сохранены исходные ёмкости/уровни EU, рецепты, режимы полёта/hover/boost, HUD, окраска Gravi и настройки рецептов. Клавиша F переключает двигатель; IC2 mode-switch + jump переключает hover.

В версии 0.1.4 точный режим Vajra использует игровые зачарования без записи временного Silk Touch в предмет; отменённая добыча сохраняет собственные зачарования. Переключатель двигателя снова работает и при заряде Gravi ниже 10000 EU.

В версии0.1.5 пустая Vajra получает питание от надетого ранца до расчёта первого удара. Оплата6666EU выполняется один раз при попадании, а использованный бонус урона сразу обновляется. Проверены отмена, нехватка энергии, повторный удар без тика, снятие предмета и nativePartEntity с живым родителем.

## Совместимость

**Sodium не поддерживается**, как и Iris, который работает поверх него. С ними возможны визуальные баги: неправильная отрисовка блоков, моделей, эффектов и интерфейса. Если что-то выглядит не так, сначала проверьте без Sodium и Iris и только потом сообщайте о баге.

## Сборка и проверки

Нужна Java 21. `./build.sh` собирает `build/libs/gravisuite-modern-0.1.5.jar`; `ic2_version` в `gradle.properties` указывает соседний архив IC2. Ресурсы уже включены, оригинал для сверки хранится в gitignored `related/`.

`JAVA_HOME=/usr/lib/jvm/java-21-openjdk JAVA_TOOL_OPTIONS=-Dgravisuite.audit=server ./gradlew runServer -q`: 270 серверных проверок. `./audit-client.sh -PwithJei=true`: скрытый беззвучный клиент. `./mp-flight-audit.sh`: выделенный сервер allow-flight=false, настоящий сетевой переключатель F и полёт; контроль без сброса счётчика отключается сервером.

Проверены модели/надетые текстуры/HUD/окраска, сетевой подъём 103.2 блока без отключения и контрольный kick через 81 тик. Общий сервер со всеми портами и остальным набором модов: 29 проверок, клиент/JEI: 14. Архивы JAR/логи/скриншоты — `versions/<version>/`. Состояние и детали — [CODEX_HANDOFF.md](CODEX_HANDOFF.md), [PORT_TODO.md](PORT_TODO.md).
