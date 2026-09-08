build:
	./gradlew build --no-daemon
run:
	./gradlew bootRun
test:
	./gradlew test --no-daemon
db-up:
	docker compose up -d db
migrate:
	./gradlew flywayMigrate

