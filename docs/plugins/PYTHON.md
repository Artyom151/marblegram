# Python-плагины

Движок — **CPython 3.11** (Chaquopy 17.1). Доступна вся стандартная библиотека Python
(`json`, `re`, `datetime`, `urllib`, `sqlite3`, `threading`…). pip-пакеты не встроены.

## Вызов методов `tg`

```python
tg.toast("Привет")
me = tg.self_id()
```

## Мост к Java

```python
from java import jclass, dynamic_proxy

LaunchActivity = jclass("org.telegram.ui.LaunchActivity")
fragment = LaunchActivity.getLastFragment()

Runnable = jclass("java.lang.Runnable")
class Task(dynamic_proxy(Runnable)):
    def run(self):
        tg.toast("на UI-потоке")

tg.run_on_ui(Task())
```

Java-коллекции поддерживают `len()`, итерацию и индексы. Строки Java автоматически становятся `str`.

## Потоки

Хуки вызываются на UI-потоке — не делайте в них долгих сетевых запросов. Используйте
`threading.Thread(target=...).start()` и возвращайтесь в UI через `tg.run_on_ui`.

## Модули

Папка плагина добавлена в `sys.path`: `import util` загрузит `util.py` рядом с `main.py`.

## Пример

См. `examples/translit_py`.
