# Сборки и релизы Marblegram

Push в `marblegram`, `main`, `master`, pull request и ручной запуск Actions →
**Build Marblegram** собирают debug APK. Скачивание: завершённая сборка →
Artifacts → `marblegram-<run-id>` (ZIP с APK и файлами проверки).

Тег `v*` собирает release APK и автоматически публикует GitHub Release
с APK, `SHA256SUMS.txt`, `signature.txt` и `build-info.txt`.
Поддерживаются armeabi-v7a, arm64-v8a, x86 и x86_64, Android 7.0+.
Ручной запуск workflow на существующем теге `v*` также публикует релиз.

## Настройка

В своём репозитории откройте Settings → Secrets and variables → Actions.
Добавьте Repository secrets:

| Secret | Значение |
| --- | --- |
| `MARBLE_APP_ID` | Числовой API ID приложения с my.telegram.org |
| `MARBLE_APP_HASH` | API hash приложения |
| `RELEASE_KEYSTORE_BASE64` | Собственный keystore в base64 одной строкой |
| `RELEASE_STORE_PASSWORD` | Пароль keystore |
| `RELEASE_KEY_ALIAS` | Alias ключа |
| `RELEASE_KEY_PASSWORD` | Пароль ключа |

Для релиза обязательны все шесть значений. Debug сборка может использовать
тестовую конфигурацию репозитория; без API credentials авторизация не работает.
Секреты недоступны pull request из форков.

Создание собственного ключа (пароли вводятся интерактивно):

```sh
keytool -genkeypair -v -keystore marblegram-release.jks -alias marblegram \
  -keyalg RSA -keysize 4096 -validity 10000
base64 -w 0 marblegram-release.jks
```

Скопируйте результат base64 в secret, не в исходники. Сохраните ключ и пароли:
следующие обновления APK должны подписываться тем же ключом.
В релизах публичный тестовый ключ репозитория не используется как запасной.

## Публикация

После загрузки изменений в свой GitHub-репозиторий создайте новый тег:

```sh
git tag v13.0.2-marblegram.1
git push marble v13.0.2-marblegram.1
```

Версия приложения берётся из `APP_VERSION_NAME` и `APP_VERSION_CODE` в
`gradle.properties`. Перед следующим обновлением увеличьте version code.
Выберите свободный тег; имя тега само по себе не меняет версию APK.

Публикация запускается только после успешной сборки и проверки подписи.
Job публикации получает `contents: write`; сборка получает `contents: read`.

## Проверка скачанного файла

Скачайте APK и `SHA256SUMS.txt` из одного релиза в одну папку:

```sh
sha256sum --check SHA256SUMS.txt
apksigner verify --verbose --print-certs Marblegram-<commit>-release.apk
```

Сравните отпечаток сертификата с `signature.txt` и предыдущим доверенным
релизом. Описание релиза содержит ссылки на исходный коммит и журнал Actions.
Это позволяет проверить происхождение файла и целостность загрузки;
сам факт сборки через Actions не является аудитом безопасности приложения.
