.PHONY: build run test db-up
build:
	./gradlew build --no-daemon
run:
	SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
test:
	./gradlew test --no-daemon
db-up:
	docker compose up -d mysql
