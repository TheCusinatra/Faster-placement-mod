# Faster Placement (Fabric 1.21.11)

Клиентский Fabric-мод для максимально отзывчивой постановки блоков на слабом ПК и при низком FPS.

## Что делает мод
- Работает только с постановкой блоков (`BlockItem`).
- Не вмешивается в еду, луки, щиты, ведра, зелья, жемчуг, инструменты и другие обычные use-item сценарии.
- Сохраняет vanilla-совместимость с точки зрения сервера: без packet-spam и без обхода серверных ограничений.

## Как уменьшена задержка
- Intent постановки захватывается сразу на raw-событии нажатия правой кнопки мыши.
- Есть ранний fast-path: мод пытается выполнить валидную постановку сразу, не дожидаясь только конца тика.
- Если кадр или тик был неудачным, intent сохраняется в bounded ring-buffer и быстро доигрывается в ближайший допустимый момент.
- Повторные одинаковые intent'ы дедуплицируются, а vanilla-дубль в тот же тик подавляется, чтобы не было случайной двойной постановки.

Полностью убрать сетевую или серверную задержку мод не может, но лишняя клиентская задержка сведена к минимуму.

## Режимы работы
- `Strict Vanilla Compatible`
- `Balanced`
- `Aggressive Low-FPS Smoothing`

`Aggressive` сильнее удерживает intent при просадках FPS и быстрее разгребает очередь после фризов, оставаясь в рамках безопасной vanilla-совместимой логики.

## Хоткей включения/выключения
- У мода есть отдельный keybind в меню Minecraft: `Настройки -> Управление`.
- Категория: `Faster Placement`
- Действие: `Переключить Faster Placement`
- По умолчанию клавиша не назначена, ты ставишь ее сам в настройках.

## Конфиг
Файл: `config/fasterplacement-client.json`

Параметры:
- `enabled`
- `placementQueueSize`
- `aggressiveLowFpsModeEnabled`
- `strictModeEnabled`
- `offhandEnabled`
- `requireSneakingForInteractableBypass`
- `mouseMovementThreshold`
- `debugLoggingEnabled`

## Отладочные метрики
Логируются при `debugLoggingEnabled=true`:
- queued placement intents
- flushed placements
- rejected duplicates
- skipped due to cooldown
- skipped due to invalid target
- recovered placements after FPS drop

## Сборка
```bash
./gradlew build
```

## Запуск в dev-среде
```bash
./gradlew runClient
```

## Совместимость
- Minecraft: `1.21.11`
- Fabric Loader: `0.18.4+`
- Fabric API: `0.141.0+1.21.11`
- Java: `21`

## Ограничения
- Это клиентский мод только для улучшения отзывчивости постановки блоков.
- Если сервер отклоняет действие по своим правилам, мод это не обходит.

<10 пх>