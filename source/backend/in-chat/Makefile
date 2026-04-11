.PHONY: test run up down package

test:
	./mvnw -q test

run:
	./mvnw spring-boot:run

up:
	docker compose -f infra/docker/docker-compose.yml up --build

down:
	docker compose -f infra/docker/docker-compose.yml down

package:
	./mvnw -q -DskipTests package
