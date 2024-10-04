# Этот мод помогает кодить на JustMC Creative+:
- Позволяет очищать код
- Позволяет сохранить код в файл
- Позволяет получить ссылку на загрузку кода
- Позволяет загрузить файл с кодом в мир
- Позволяет искать блоки в коде

# Использование
`/codespace clear` - очистить код
`/codespace config open` - открыть папку с конфигом
`/codespace config reload` - перезагрузить конфиг
`/codespace config reset` - сбросить конфиг
`/codespace save file` - сохранить код в файл
`/codespace save file <имя>` - сохранить код в файл с указанным именем
`/codespace save upload` - получить ссылку на загрузку кода
`/codespace save stop` - остановить сохранение кода
`/codespace saved-codes delete <файл>` - удалить сохранённый код в указанном файле
`/codespace saved-codes delete-all` - удалить все сохранённые коды
`/codespace saved-codes load <файл>` - загрузить код из файла в мир
`/codespace saved-codes load-force <файл>` - загрузить код из файла в мир с заменой текущего кода
`/codespace search <ввод>` - найти в коде блоки
`/codespace search-page <страница>` - показать определённую страницу результатов поиска

# Информация:
Версия майнкрафт: `1.20.4`
Версия мода: `1.1` (Beta)

Сохранённые коды хранятся в **папке с запущенным майнкрафтом** в подкаталоге **config/jmc-codespace/saved**

**Мод находится в тестировании! Не несу ответственность за ваши коды.**

# Зависимости:
- [Fabric API](https://modrinth.com/mod/fabric-api/versions)
- [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin/versions) (2.0.20)
- [Client Command Extensions](https://github.com/unidok/ClientCommandExtensions/releases)
- [Fabric Scheduler](https://github.com/unidok/FabricScheduler/releases)
