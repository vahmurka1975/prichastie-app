# Подготовка первого подписанного релиза

Это файлы настройки, НЕ собранный APK. Сборка и публикация не выполнялись.
Подготовлено по прочитанным из GitHub файлам main. Перед заменой сохраните свои локальные изменения.

1. Скопируйте app/build.gradle.kts, .gitignore и .github/workflows/publish-release.yml из архива в корень проекта с сохранением структуры. В Gradle сохранены SDK 37 и версия 1.0 / versionCode 1; удалены дубли зависимостей Media3 1.4.1 (оставлены 1.8.0).
2. Если ключ уже существует, используйте его. Иначе Android Studio: Build > Generate Signed App Bundle / APK > APK > Create new. Сохраните ключ ВНЕ проекта, например C:\AndroidKeys\prichastie-release.jks. Alias: prichastie, срок не менее 25 лет, надежные пароли. Данные сертификата заполните своими данными. Сделайте защищенную резервную копию ключа и отдельно паролей. Не присылайте их в чат.
3. В мастере можно сразу собрать release APK локально для проверки на телефоне. Без переменных RELEASE_* обычная CLI-сборка release остается неподписанной; мастер Android Studio передает собственные параметры подписи.
4. GitHub > репозиторий > Settings > Secrets and variables > Actions: создайте секреты ANDROID_KEYSTORE_BASE64, ANDROID_STORE_PASSWORD, ANDROID_KEY_ALIAS, ANDROID_KEY_PASSWORD. Последние три — пароль хранилища, alias и пароль ключа.

PowerShell для копирования содержимого ключа в Base64:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes('C:\AndroidKeys\prichastie-release.jks')) | Set-Clipboard
```

Вставьте буфер только в секрет ANDROID_KEYSTORE_BASE64; очистите его:

```powershell
Set-Clipboard -Value ''
```

Base64 не шифрует ключ! Не публикуйте значение. Ограничьте доступ на запись к репозиторию: редакторы workflows могут получить секреты при выполнении workflow.

5. Из корня проекта, убедившись, что git status не показывает посторонних изменений:

```powershell
git add .gitignore app/build.gradle.kts .github/workflows/publish-release.yml
git commit -m "Configure signed APK release workflow"
git push origin main
```

При защищенной main используйте отдельную ветку и Pull Request.

6. На GitHub: Actions > Publish signed APK > Run workflow > main > Run workflow. Публикация запускается вручную и только с main. После успешных тестов, сборки и проверки подписи workflow создаст Latest Release v1.0 с prichastie-1.0.apk и SHA256SUMS.txt. Это публичная публикация. Не запускайте до настройки ключа и проверки локальной release-сборки.

Если SDK/dependencies/tests/signing недоступны или завершаются ошибкой, публикация не выполняется. Смотрите первый упавший шаг; конфигурация еще не проверена реальной Android-сборкой. Настройка использует SDK 37 и Gradle из текущего проекта, а не заменяет их версии.

Для следующих версий увеличивайте versionCode и versionName и сохраняйте applicationId и ТОТ ЖЕ ключ. Workflow проверяет корректность подписи, но не сравнивает сертификат с прошлым релизом. Не заменяйте секреты новым ключом. Не перезаписывайте существующие теги.

Debug-сборка обычно имеет другую подпись: release APK поверх нее не установится. Не удаляйте приложение без сохранения данных: удаление может стереть прогресс. Первый release APK устанавливается вручную; последующие с тем же ключом и большим versionCode могут обновлять его без удаления.
