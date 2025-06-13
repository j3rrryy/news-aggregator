# News Aggregator

<p align="center">
  <a href="https://github.com/j3rrryy/news-aggregator/actions/workflows/main.yml">
    <img src="https://github.com/j3rrryy/news-aggregator/actions/workflows/main.yml/badge.svg" alt="СI/CD">
  </a>
  <a href="https://codecov.io/gh/j3rrryy/news-aggregator">
    <img src="https://codecov.io/gh/j3rrryy/news-aggregator/graph/badge.svg?token=7EOHJU2CCO" alt="Codecov">
  </a>
  <a href="https://www.oracle.com/java/technologies/downloads/#java21">
    <img src="https://img.shields.io/badge/Java-21-FFD64E.svg" alt="Java 21">
  </a>
  <a href="https://github.com/j3rrryy/news-aggregator/blob/main/LICENSE">
    <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="MIT License">
  </a>
</p>

## :book: Key features

- News collection from multiple sources (rt.ru, aif.ru, svpressa.ru)
- Different search and filtration options
- Real-time news analytics
- Data export to CSV, JSON or HTML
- Configurable automatic updates
- Main DB - PostgreSQL
- DB for cache - Redis

> [!NOTE]
> API located at `/api`
>
> Docs located at `/api/docs`

## :computer: Requirements

- Docker

## :hammer_and_wrench: Getting started

- Copy `.env` file from `examples/` to `docker/` folder and fill it in

- Copy `redis.conf` file from `examples/` to `docker/` folder and fill it in

### :rocket: Start

```shell
docker compose up --build -d
```

### :x: Stop

```shell
docker compose stop
```

## :sparkling_heart: HSE FCS IPDD
