# Camping API Java Backend

Java 17 / Jakarta EE backend intended for deployment as a WAR on JBoss EAP 8.

## 🎯 Features

This project provides a complete camping reservation system with:

- **RESTful API** endpoints for camping spot management and checkout
- **Kafka Integration** for event-driven architecture
- **Instana Tracing** for comprehensive application monitoring
- **React Frontend** built into the WAR

## 📡 API Endpoints

- `GET /api/` - API information and status
- `GET /api/health` - Health check endpoint
- `GET /api/spot` - List all camping spots
- `GET /api/spot/{spot_id}` - Get specific camping spot details
- `POST /api/checkout` - Process order checkout (sends to Kafka)
- `POST /api/send_src_email` - Newsletter subscription

The React frontend is built into the WAR root, so opening the deployed context path serves the web UI.

## 🔍 Instana Monitoring

This project includes **complete Instana tracing integration** with:

### Automatic Tracing (Instana Agent)
- ✅ All HTTP requests and responses
- ✅ JAX-RS endpoint calls
- ✅ Kafka Producer operations
- ✅ CDI Bean method invocations
- ✅ Exception and error tracking
- ✅ JVM metrics

### Manual Tracing (Instana SDK)
- ✅ Business logic step markers
- ✅ Order and product details
- ✅ Custom business events
- ✅ Business metrics and tags
- ✅ Detailed error context

### Quick Start with Instana

See [QUICK_START.md](QUICK_START.md) for 5-minute setup guide.

For detailed configuration, see [INSTANA_SETUP.md](INSTANA_SETUP.md).

## Build

```bash
mvn clean package
```

The WAR is generated at:

```text
target/camping-api.war
```

## Configuration

The application reads the same environment variable names as the Python backend:

```text
KAFKA_BOOTSTRAP_SERVER=10.107.85.239:9092
SCHEMA_REGISTRY_ENDPOINT=http://10.107.85.239:8081
KSQLDB_ENDPOINT=http://10.107.85.239:8088
KAFKA_TOPIC_NAME=raw_events
```

## Deploy To JBoss EAP 8

Copy the WAR to the JBoss deployments directory:

```bash
cp target/camping-api.war $JBOSS_HOME/standalone/deployments/
```

By default, the deployed context path is `/camping-api`, so the health endpoint is:

```text
http://localhost:8080/camping-api/api/health
```

The web UI is available at:

```text
http://localhost:8080/camping-api/
```

If you need the API to run at the server root path `/`, configure the context root in JBoss or deploy it as `ROOT.war`.
# Instana-SDK-Lab
