Ниже подробная спецификация light theme для Repair System.

## Общая идея

Light theme не должен быть полностью белым. Основной фон — светло-серо-синий, карточки — немного светлее фона, sidebar — отдельный холодный оттенок. Основной акцент — синий вместо фиолетового.

Стиль:

- спокойный enterprise SaaS;
- минимум чистого `#ffffff`;
- мягкие границы;
- небольшие тени;
- синий используется для активных элементов и действий;
- текст — тёмно-синий, а не чисто чёрный.

## Основные цвета

```css
:root {
  /* Основной фон приложения */
  --rs-bg: #e8eef7;

  /* Фон основного контента */
  --rs-content-bg: #eef3f9;

  /* Sidebar */
  --rs-sidebar-bg: #dce6f2;

  /* Header */
  --rs-header-bg: #edf2f8;

  /* Карточки */
  --rs-panel: #f5f8fc;

  /* Вложенные блоки и input */
  --rs-panel-2: #e8eef6;

  /* Hover */
  --rs-hover: #dce8f7;

  /* Активный элемент */
  --rs-active-bg: #d5e5fb;

  /* Главный синий */
  --rs-primary: #2563eb;

  /* Более тёмный синий */
  --rs-primary-dark: #1d4ed8;

  /* Светлый синий */
  --rs-primary-soft: #dbeafe;

  /* Основной текст */
  --rs-text: #172554;

  /* Вторичный текст */
  --rs-text-2: #334e75;

  /* Приглушённый текст */
  --rs-muted: #7183a1;

  /* Границы */
  --rs-border: #c5d2e3;

  /* Более мягкие границы */
  --rs-border-soft: #dbe4ef;

  /* Успешное состояние */
  --rs-success: #15803d;
  --rs-success-bg: #dcfce7;

  /* Предупреждение */
  --rs-warning: #b45309;
  --rs-warning-bg: #fef3c7;

  /* Ошибка */
  --rs-danger: #dc2626;
  --rs-danger-bg: #fee2e2;

  /* Информационное состояние */
  --rs-info: #0369a1;
  --rs-info-bg: #e0f2fe;

  /* Тени */
  --rs-shadow:
    0 8px 24px rgba(30, 64, 175, 0.08);

  --rs-shadow-hover:
    0 12px 32px rgba(30, 64, 175, 0.14);

  --rs-radius: 12px;
}
```

## Фон страницы

Фон не должен быть белым:

```css
body {
  background: var(--rs-bg);
  color: var(--rs-text);
}
```

Основной контент:

```css
.app-main {
  background: var(--rs-content-bg);
}
```

Не нужно использовать:

```css
background: #fff;
background: white;
```

Исключение — небольшие области, где нужен сильный контраст, например логотип или специальные уведомления.

## Sidebar

Sidebar должен визуально отличаться от основного контента, но не быть слишком тёмным:

```css
.app-sidebar {
  background: var(--rs-sidebar-bg);
  border-right: 1px solid var(--rs-border);
  box-shadow: 4px 0 18px rgba(30, 64, 175, 0.06);
}
```

Логотип:

```css
.sidebar-brand {
  background: #d7e2ef;
  border-bottom: 1px solid var(--rs-border);
}
```

Текст sidebar:

```css
.sidebar-menu .nav-link {
  color: var(--rs-text-2);
}
```

Hover:

```css
.sidebar-menu .nav-link:hover {
  background: var(--rs-hover);
  color: var(--rs-text);
}
```

Активный пункт:

```css
.sidebar-menu .nav-link.active {
  background: var(--rs-active-bg);
  color: var(--rs-primary-dark);
  font-weight: 600;
}

.sidebar-menu .nav-link.active .nav-icon {
  color: var(--rs-primary);
}
```

Заголовки разделов:

```css
.sidebar-menu .nav-header {
  color: #607697;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 0.7rem;
  font-weight: 700;
}
```

Активным должен быть только текущий пункт. Например, на `/admin/customers` Dashboard не должен подсвечиваться одновременно.

## Header

Header должен быть обычным, без `position: sticky`:

```css
.app-header {
  position: static;
  min-height: 58px;
  background: var(--rs-header-bg);
  border-bottom: 1px solid var(--rs-border-soft);
}
```

Все элементы header должны находиться на одной вертикальной линии:

```css
.app-header,
.app-header .container-fluid,
.app-header .navbar-nav {
  align-items: center;
}
```

Search:

```css
.app-header-search {
  background: var(--rs-panel);
  border: 1px solid var(--rs-border);
  border-radius: 8px;
}
```

Search input:

```css
.app-header-search .form-control {
  background: transparent;
  border: 0;
  color: var(--rs-text);
}
```

## Карточки

Карточки не должны быть чисто белыми:

```css
.card {
  background: var(--rs-panel);
  border: 1px solid var(--rs-border-soft);
  border-radius: var(--rs-radius);
  box-shadow: var(--rs-shadow);
}
```

При наведении на интерактивные карточки:

```css
.card.is-interactive:hover {
  border-color: #abc6eb;
  box-shadow: var(--rs-shadow-hover);
}
```

Header карточки:

```css
.card-header {
  background: #edf3fa;
  border-bottom: 1px solid var(--rs-border-soft);
  color: var(--rs-text);
}
```

Body карточки:

```css
.card-body {
  background: var(--rs-panel);
}
```

Не нужно делать каждую карточку сильно закруглённой. Оптимально:

- `border-radius: 10px–14px`;
- тонкая граница;
- мягкая тень;
- одинаковые внутренние отступы.

## Текст

Основной текст:

```css
body {
  color: var(--rs-text);
}
```

Заголовки:

```css
h1,
h2,
h3,
h4,
h5,
h6 {
  color: var(--rs-text);
}
```

Вторичный текст:

```css
.text-muted {
  color: var(--rs-muted) !important;
}
```

Описание, подписи и metadata должны быть темнее, чем в текущей версии, чтобы оставаться читаемыми:

```css
.form-label,
.table th,
.small,
.text-secondary {
  color: var(--rs-text-2);
}
```

## Кнопки

Primary button — синий:

```css
.btn-primary {
  background: var(--rs-primary);
  border-color: var(--rs-primary);
  color: #ffffff;
}

.btn-primary:hover {
  background: var(--rs-primary-dark);
  border-color: var(--rs-primary-dark);
}
```

Важно: белый текст на синей кнопке допустим, потому что это контрастный элемент.

Outline button:

```css
.btn-outline-primary {
  color: var(--rs-primary-dark);
  border-color: #7fa8df;
  background: transparent;
}

.btn-outline-primary:hover {
  background: var(--rs-primary-soft);
  border-color: var(--rs-primary);
  color: var(--rs-primary-dark);
}
```

Danger:

```css
.btn-outline-danger {
  color: var(--rs-danger);
  border-color: #f0a0a0;
}

.btn-outline-danger:hover {
  background: var(--rs-danger-bg);
}
```

Disabled:

```css
button:disabled,
.btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
```

## Input

Все input должны иметь одинаковый фон и рамку:

```css
.form-control,
.form-select,
input,
textarea,
select {
  background: var(--rs-panel-2);
  border: 1px solid var(--rs-border);
  color: var(--rs-text);
  border-radius: 9px;
}
```

Placeholder:

```css
.form-control::placeholder {
  color: #7d90ad;
}
```

Hover:

```css
.form-control:hover,
.form-select:hover,
input:hover,
textarea:hover,
select:hover {
  border-color: #8eadd8;
}
```

Focus:

```css
.form-control:focus,
.form-select:focus,
input:focus,
textarea:focus,
select:focus {
  background: #edf3fa;
  border-color: var(--rs-primary);
  color: var(--rs-text);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.16);
  outline: none;
}
```

## Select

Для всех select должна быть единая рамка:

```css
select,
.form-select,
.pagination-size {
  min-height: 40px;
  background-color: var(--rs-panel-2);
  border: 1px solid var(--rs-border) !important;
  color: var(--rs-text);
  border-radius: 9px;
}
```

Обычное состояние:

```css
select {
  border-color: var(--rs-border);
}
```

Hover:

```css
select:hover {
  border-color: #8eadd8 !important;
}
```

Focus:

```css
select:focus {
  border-color: var(--rs-primary) !important;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.16);
}
```

Disabled:

```css
select:disabled {
  background: #dce4ee;
  color: #8292a9;
  border-color: #cbd6e4 !important;
}
```

Размеры:

```css
.form-select {
  min-height: 42px;
  padding: 0.55rem 0.85rem;
}
```

## Options

Options должны соответствовать светлой теме:

```css
select option,
.form-select option,
.pagination-size option {
  background: #f1f5fa;
  color: var(--rs-text);
}
```

Selected option:

```css
select option:checked {
  background: var(--rs-primary-soft);
  color: var(--rs-primary-dark);
}
```

Однако нужно учитывать: внешний вид открытого native dropdown частично контролируется операционной системой и браузером. CSS не всегда полностью управляет выпадающим меню `<select>`.

Если требуется полный контроль над:

- фоном options;
- hover;
- selected state;
- иконкой стрелки;
- высотой выпадающего меню;

тогда лучше использовать кастомный компонент select, например собственный Vue-компонент или библиотеку.

Для обычных select достаточно native-элемента. Для сложных фильтров и языков можно использовать custom select.

## Стрелка select

Можно заменить стандартную стрелку:

```css
.form-select {
  appearance: none;
  background-image:
    linear-gradient(45deg, transparent 50%, #55749d 50%),
    linear-gradient(135deg, #55749d 50%, transparent 50%);
  background-position:
    calc(100% - 16px) 17px,
    calc(100% - 11px) 17px;
  background-size:
    5px 5px,
    5px 5px;
  background-repeat: no-repeat;
}
```

При focus стрелка может становиться синей:

```css
.form-select:focus {
  background-image:
    linear-gradient(45deg, transparent 50%, var(--rs-primary) 50%),
    linear-gradient(135deg, var(--rs-primary) 50%, transparent 50%);
}
```

## Таблицы

Table background:

```css
.table {
  --bs-table-bg: transparent;
  color: var(--rs-text);
}
```

Header:

```css
.table thead th {
  background: #e2eaf4;
  color: #516b8e;
  border-bottom: 1px solid var(--rs-border);
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}
```

Rows:

```css
.table tbody tr {
  background: var(--rs-panel);
  border-bottom: 1px solid var(--rs-border-soft);
}
```

Hover:

```css
.table tbody tr:hover {
  background: #eaf2fc;
}
```

Кликабельная строка:

```css
.table tbody tr.is-clickable {
  cursor: pointer;
}
```

Не нужно делать ссылки ярко-синими и подчёркнутыми внутри каждой ячейки, если вся строка является ссылкой.

## Badges и статусы

Новая заявка:

```css
.status-new {
  color: #075985;
  background: #e0f2fe;
  border: 1px solid #7dd3fc;
}
```

В работе:

```css
.status-in-progress {
  color: #1d4ed8;
  background: #dbeafe;
  border: 1px solid #93c5fd;
}
```

Ожидание запчастей:

```css
.status-waiting {
  color: #92400e;
  background: #fef3c7;
  border: 1px solid #fcd34d;
}
```

Завершено:

```css
.status-completed {
  color: #166534;
  background: #dcfce7;
  border: 1px solid #86efac;
}
```

Отменено:

```css
.status-cancelled {
  color: #991b1b;
  background: #fee2e2;
  border: 1px solid #fca5a5;
}
```

## Alert-сообщения

Успешное:

```css
.alert-success {
  background: var(--rs-success-bg);
  border: 1px solid #86efac;
  color: #166534;
}
```

Ошибка:

```css
.alert-danger {
  background: var(--rs-danger-bg);
  border: 1px solid #fca5a5;
  color: #991b1b;
}
```

Предупреждение:

```css
.alert-warning {
  background: var(--rs-warning-bg);
  border: 1px solid #fcd34d;
  color: #92400e;
}
```

Информация:

```css
.alert-info {
  background: var(--rs-info-bg);
  border: 1px solid #7dd3fc;
  color: #075985;
}
```

## Login page

Фон login page:

```css
.auth-page {
  background:
    radial-gradient(
      circle at 15% 80%,
      rgba(191, 219, 254, 0.42),
      transparent 32%
    ),
    var(--rs-bg);
}
```

Карточка login:

```css
.auth-card {
  background: var(--rs-panel);
  border: 1px solid var(--rs-border-soft);
  box-shadow: var(--rs-shadow);
  border-radius: 16px;
}
```

Логотип:

```css
.auth-logo {
  background: var(--rs-primary);
  color: #ffffff;
}
```

Login button:

```css
.auth-card .btn-primary {
  background: var(--rs-primary);
}
```

Поля email и password должны быть такого же цвета, как остальные input, а не белого:

```css
.auth-card .form-control {
  background: var(--rs-panel-2);
  border-color: var(--rs-border);
}
```

## Dashboard cards

Статистические карточки могут иметь разные мягкие оттенки:

```css
.stat-card-blue {
  background: #e4efff;
  border-color: #b9d4f7;
}

.stat-card-green {
  background: #e5f6ed;
  border-color: #b9e4ca;
}

.stat-card-orange {
  background: #fff3df;
  border-color: #f3d19a;
}

.stat-card-red {
  background: #ffe9e9;
  border-color: #f2b5b5;
}
```

Цветные карточки не должны быть слишком яркими. Лучше использовать пастельные фоны и тёмный текст.

## Pagination

Pagination controls:

```css
.pagination-size {
  background: var(--rs-panel-2);
  border: 1px solid var(--rs-border) !important;
  color: var(--rs-text);
}
```

Активная страница:

```css
.pagination .page-item.active .page-link {
  background: var(--rs-primary);
  border-color: var(--rs-primary);
  color: #ffffff;
}
```

Обычная страница:

```css
.pagination .page-link {
  background: var(--rs-panel);
  border-color: var(--rs-border);
  color: var(--rs-primary-dark);
}
```

Hover:

```css
.pagination .page-link:hover {
  background: var(--rs-primary-soft);
  border-color: #8eadd8;
}
```

## File input

File input не должен становиться белым при hover:

```css
.file-input-control {
  background: var(--rs-panel-2);
  border: 1px solid var(--rs-border);
  border-radius: 9px;
}

.file-input-button {
  background: #dce8f7;
  color: var(--rs-text);
  border-right: 1px solid var(--rs-border);
}

.file-input-button:hover {
  background: var(--rs-primary);
  color: #ffffff;
}

.file-input-name {
  color: var(--rs-text-2);
}

.file-input-name.is-empty {
  color: var(--rs-muted);
}
```

## Modal

Modal backdrop:

```css
.modal-backdrop {
  background: #172554;
}

.modal-backdrop.show {
  opacity: 0.35;
}
```

Modal:

```css
.modal-content {
  background: var(--rs-panel);
  border: 1px solid var(--rs-border);
  border-radius: 14px;
  box-shadow: var(--rs-shadow-hover);
}
```

Modal header:

```css
.modal-header {
  background: #eaf1f8;
  border-bottom: 1px solid var(--rs-border-soft);
}
```

Modal footer:

```css
.modal-footer {
  background: #edf3f9;
  border-top: 1px solid var(--rs-border-soft);
}
```

## Spinner и loading

Spinner должен быть синим:

```css
.spinner-border {
  color: var(--rs-primary) !important;
}
```

Loading overlay:

```css
.loading-overlay {
  background: rgba(232, 238, 247, 0.78);
  backdrop-filter: blur(3px);
}
```

## Мобильная адаптация

На мобильных:

- sidebar должен превращаться в drawer;
- header не должен быть sticky;
- search в header можно скрывать или переносить;
- select должен занимать всю ширину;
- карточки должны идти в одну колонку;
- таблицы должны иметь горизонтальный scroll;
- кнопки не должны выходить за пределы экрана.

```css
@media (max-width: 768px) {
  .app-main {
    padding: 16px;
  }

  .card {
    border-radius: 10px;
  }

  .form-select,
  .form-control,
  select {
    width: 100%;
  }

  .table-responsive {
    overflow-x: auto;
  }
}
```

## Что важно не делать

Не использовать повсеместно:

```css
#ffffff;
white;
background: white;
```

Не использовать фиолетовый:

```css
#cb3cff;
#c33cff;
#b026ff;
```

Не делать:

- белые input на серо-синем фоне;
- белые options в тёмном или синем select;
- слишком насыщенный синий на больших площадях;
- синий текст на синем фоне;
- одновременно активными Dashboard и текущую дочернюю страницу;
- разные цвета рамок у разных select;
- разные стили select на settings, filters и pagination;
- слишком сильные тени;
- чисто чёрный текст;
- полупрозрачные белые блоки с низким контрастом.

## Итоговая визуальная схема

```text
Страница:       #e8eef7
Контент:        #eef3f9
Sidebar:        #dce6f2
Header:         #edf2f8
Карточка:       #f5f8fc
Input/select:   #e8eef6
Hover:          #dce8f7
Active:         #d5e5fb
Border:         #c5d2e3
Text:           #172554
Muted text:     #7183a1
Primary blue:   #2563eb
Dark blue:      #1d4ed8
Soft blue:      #dbeafe
```

Такой вариант будет выглядеть как светлая тема, но не как полностью белая страница: фон останется серо-синим, интерфейс будет мягким, а синий цвет будет использоваться только для акцентов и важных действий.