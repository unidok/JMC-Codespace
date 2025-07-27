# Этот мод помогает кодить на JustMC Creative+:
- Позволяет очищать код
- Позволяет сохранить код в файл
- Позволяет получить ссылку на загрузку кода
- Позволяет загрузить файл с кодом в мир
- Позволяет искать блоки в коде
- Позволяет получать текст большей длины, чем 256
- Позволяет получать шаблоны из модулей
- Позволяет получить код шаблона

# Использование
- `/codespace clear` - очистить код
- `/codespace config open` - открыть папку с конфигом
- `/codespace config reload` - перезагрузить конфиг
- `/codespace config reset` - сбросить конфиг
- `/codespace save file` - сохранить код в файл
- `/codespace save file <имя>` - сохранить код в файл с указанным именем
- `/codespace save upload` - получить ссылку на загрузку кода
- `/codespace save stop` - остановить сохранение кода
- `/codespace saved-codes delete <файл>` - удалить сохранённый код в указанном файле
- `/codespace saved-codes delete-all` - удалить все сохранённые коды
- `/codespace saved-codes load <файл>` - загрузить код из файла в мир
- `/codespace saved-codes load-force <файл>` - загрузить код из файла в мир с заменой текущего кода
- `/codespace saved-codes as-text <файл>` - получить содержимое файла как значение "текст"
- `/codespace saved-codes line-as-template <номер строки> <файл>` - получить строку модуля как шаблон
- `/codespace search <ввод>` - найти в коде блоки
- `/codespace search-page <страница>` - показать определённую страницу результатов поиска
- `/codespace template` - получить код шаблона

# Информация:
- Версия майнкрафт: `1.21`
- Версия мода: `1.2.1`

- Modrinth: https://modrinth.com/mod/jmc-codespace

- Сохранённые коды хранятся в **папке с запущенным майнкрафтом** в подкаталоге **config/jmc-codespace/saved**

# Зависимости:
- [Fabric API](https://modrinth.com/mod/fabric-api/versions)
- [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin/versions) (2.0.21)
