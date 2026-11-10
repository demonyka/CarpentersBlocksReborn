# Публикация на Modrinth и CurseForge

Проекты на площадках создаются вручную один раз. После этого каждый релиз на GitHub сам загружает оба jar (Forge и Fabric) на обе площадки через `.github/workflows/publish.yml`.

## 1. Проект на Modrinth

На https://modrinth.com/dashboard/projects нажми **Create a project**.

| Поле | Значение |
| --- | --- |
| Name | Carpenter's Blocks Reborn |
| URL | `carpenters-blocks-reborn` |
| Summary | Frame blocks, slopes, stairs, doors and redstone parts that take the look of any block. A rewrite of Carpenter's Blocks for Minecraft 1.16.5, Forge and Fabric. |
| Icon | `docs/icon.png` (логотип оригинала) или `docs/icon-roof.png` (скриншот крыши) |
| Description | текст из `docs/description.md` |
| Categories | Decoration, Utility |
| Client side / Server side | Required / Required |
| License | Custom: `Apache-2.0 AND LGPL-2.1-only`, ссылка https://github.com/demonyka/CarpentersBlocksReborn/blob/main/NOTICE |
| Source code | https://github.com/demonyka/CarpentersBlocksReborn |
| Issue tracker | https://github.com/demonyka/CarpentersBlocksReborn/issues |
| Gallery | `docs/screenshots/`. Первым поставь `cottage`, затем `cottage-front`, `yard`, `garage-door`, `roof-skylight`, `cottage-dusk`, `night-lanterns`, `overview`; дальше технические: `slopes-roof`, `slope-shapes`, `all-blocks`, `doors`, `connected-glass-optifine`, `translucent-covers`, `chisel-connected`, `jade-tooltip` |

ID проекта (8 символов) показан в настройках проекта. Он понадобится в шаге 3.

## 2. Проект на CurseForge

На https://authors.curseforge.com/#/projects нажми **Create project**, игра Minecraft, класс Mods.

| Поле | Значение |
| --- | --- |
| Name | Carpenter's Blocks Reborn |
| Summary | то же, что на Modrinth |
| Main category | Cosmetic |
| Additional categories | Redstone |
| Logo | `docs/icon.png` или `docs/icon-roof.png` (нужно не меньше 400×400, обе больше) |
| Description | `docs/description.html` (HTML, вставляется через кнопку исходного кода в редакторе) или `docs/description.md` в режиме Markdown |
| License | Apache License 2.0; в описании сказано, что текстуры и логотип остаются под LGPL-2.1 |
| Source | https://github.com/demonyka/CarpentersBlocksReborn |
| Issues | https://github.com/demonyka/CarpentersBlocksReborn/issues |
| Images | те же скриншоты из `docs/screenshots/` |

На CurseForge уже есть оригинальный Carpenter's Blocks. Название с «Reborn» и раздел Credits в описании нужны, чтобы модерация и игроки не приняли проект за копию.

Модерация CurseForge обычно занимает от нескольких часов до пары дней. Project ID виден справа на странице проекта в блоке About Project.

## 3. Доступ для автопубликации

Секреты GitHub нельзя прочитать обратно, поэтому из репозитория AE2 Stuff Reforged они не копируются. Сами токены те же, их нужно ввести ещё раз:

- Modrinth: https://modrinth.com/settings/pats, права **Create versions** и **Write projects**.
- CurseForge: https://authors.curseforge.com/account/api-tokens.

```
gh variable set MODRINTH_ID --repo demonyka/CarpentersBlocksReborn --body <ID с Modrinth>
gh variable set CURSEFORGE_ID --repo demonyka/CarpentersBlocksReborn --body <Project ID с CurseForge>
gh secret set MODRINTH_TOKEN --repo demonyka/CarpentersBlocksReborn
gh secret set CURSEFORGE_TOKEN --repo demonyka/CarpentersBlocksReborn
```

Площадка без токена просто пропускается, так что можно выкладывать и на одну из них.

## 4. Релиз

Опубликуй релиз `v4.0.0-alpha.1` на https://github.com/demonyka/CarpentersBlocksReborn/releases.
Тег должен быть `v` плюс `mod_version` из `gradle.properties`, иначе workflow остановится до сборки.
Workflow **publish** соберёт оба jar без тестового кода и загрузит их так:

| | Forge | Fabric |
| --- | --- | --- |
| Версия | `4.0.0-alpha.1+mc1.16.5-forge` | `4.0.0-alpha.1+mc1.16.5-fabric` |
| Канал | Alpha, пока в версии есть `alpha`; Beta для `beta`; иначе Release | то же |
| Игра | 1.16.5, Java 8 | 1.16.5, Java 8 |
| Changelog | раздел из `CHANGELOG.md` | то же |
| Зависимости | Jade — optional | Fabric API — required |

Если проекты ещё на модерации или токены не заданы, workflow упадёт. Его можно повторить вручную: вкладка Actions, **publish**, Run workflow.

## Следующие версии

1. Поменять `mod_version` в `gradle.properties` и добавить раздел `## <версия> - <дата>` в `CHANGELOG.md`.
2. Закоммитить, создать тег и опубликовать релиз на GitHub. Остальное сделает workflow.
