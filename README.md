# Gravitation Suite UNOFFICIAL PORT

Неофициальный порт **Gravitation Suite 3.1.1** (SeNtiMeL / Chocohead) на **Minecraft 1.21.1 / NeoForge 21.1.256**. Текущая версия: **0.1.10**.

Семь компонентов, Advanced/Ultimate Lappack, Advanced Drill с четырьмя режимами и добычей 3×3, Advanced Chainsaw, GraviTool с четырьмя режимами, Vajra, Advanced Jetpack, Advanced NanoChestplate и GraviChestPlate. Сохранены исходные ёмкости и уровни EU, рецепты, режимы полёта, HUD, окраска Gravi и настройки рецептов. Клавиша F переключает двигатель; IC2 mode-switch + jump переключает hover.

## Совместимость

**Sodium и Iris не поддерживаются**: с ними возможны визуальные баги блоков, моделей, эффектов и интерфейса. Перед отправкой отчёта о такой проблеме проверьте игру без этих модов.

## Сборка

Нужны **Java 21** и JAR **IndustrialCraft 2 UNOFFICIAL PORT 0.1.26 или новее**. По умолчанию ожидается `libs/industrialcraft2-modern-0.1.26.jar`. Версию ожидаемого JAR задаёт `ic2_version` в `gradle.properties`; другой путь можно передать через `-Pic2LocalJar=libs/ic2.jar` или переменную `IC2_JAR`.

```sh
./gradlew build
```

Результат: `build/libs/gravisuite-modern-0.1.10.jar`. Все сгенерированные игровые ресурсы включены в репозиторий; для обычной сборки запуск генератора ресурсов не нужен.

JEI необязателен в игре. При сборке его API загружается из официального Maven-репозитория BlameJared; `-PwithJei=true` добавляет JEI в среду разработки. Для сборки без скачивания JEI можно указать `-PjeiLocalJar=libs/jei.jar` или переменную `JEI_JAR`. Версия Maven-зависимости задаётся свойством `jei_version`.

На Linux можно использовать `./build.sh`: скрипт сохраняет выбранный `JAVA_HOME`. На Windows используйте `gradlew.bat build` с Java 21. Локальные JAR в `libs/` не добавляются в Git.

## Проверки

`JAVA_TOOL_OPTIONS=-Dgravisuite.audit=server ./gradlew runServer` запускает серверные проверки инструментов, брони, рецептов и энергобаланса. Для серверных проверок требуется принять Minecraft EULA.

`./audit-client.sh -PwithJei=true` запускает клиентские проверки в Xvfb без звука; нужен подготовленный тестовый мир `GraviAudit` в `run-audit/saves/`. `./mp-flight-audit.sh` проверяет сетевое переключение двигателя и полёт на выделенном сервере с `allow-flight=false`. Клиентские проверки требуют Xvfb и OpenAL. Результаты создаются в локальных папках `run-*`.
