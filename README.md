# java-movies-hub
Repository for homework project.

# MovieHub API

REST API для управления фильмами. Хранение в памяти, обмен JSON.

## Запуск

Запустите `ru.practicum.moviehub.MovieHubApp`

Сервер стартует на `http://localhost:8080`

## Эндпоинты

| Метод | Путь | Описание |
|-------|------|----------|
| GET | /movies | все фильмы |
| GET | /movies/{id} | фильм по ID |
| GET | /movies?year=YYYY | фильмы по году |
| POST | /movies | добавить фильм |
| DELETE | /movies/{id} | удалить фильм |

### Автор

Владислав Сакас, студент 74й когорты Java