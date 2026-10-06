# Product / Order microservices POC

Registry + config server + API gateway + two services, each with its own MongoDB server, plus the three helper
scripts (start MongoDB, API tests, stop MongoDB). Built to meet the training completion criteria.

```
Browser / curl ──► API Gateway :8080 ──lb://product-service──► Product Service ──► MongoDB mongo1 :27017 (productdb)
                        │                                              ▲
                        │                                              │ REST (OpenFeign)
                        └────lb://order-service─────► Order Service ──► MongoDB mongo2 :27018 (orderdb)

Service registry (Eureka) :8761  - every service registers here and finds the others here
Config server             :8888  - serves each service's MongoDB URI and shared settings
```

| Module | Port | Role |
|---|---|---|
| `service-registry` | 8761 | Eureka registry |
| `config-server` | 8888 | Central configuration (`config-server/src/main/resources/config-repo`) |
| `api-gateway` | **8080** (only public API port) | Routing, plus a JSON overview at `/` |
| `product-service` | random | Products, stock, reviews. Own server `mongo1` / `productdb` |
| `order-service` | random | Customers, orders. Own server `mongo2` / `orderdb`. Calls product-service |

---------------------------------------------------------------------------------------------------

## Step by step

Run everything in **Git Bash** from the project root (the folder with the root `pom.xml`).

### 1. Prerequisites
Java 17 or newer, Maven 3.9+, Docker Desktop running.

### 2. Free port 27017
If the old `poc-mongo` container is still running, remove it:
```bash
docker rm -f poc-mongo
```

### 3. Start the two MongoDB servers
```bash
./1_docker-start-mongodb.sh
docker ps          # mongo1 (27017) and mongo2 (27018) must be listed
```
If you get "permission denied", run `bash 1_docker-start-mongodb.sh`.

### 4. Build
```bash
mvn clean install -DskipTests
```
Wait for `BUILD SUCCESS`.

### 5. Start the five applications, one terminal each, in this order
```bash
mvn -pl service-registry spring-boot:run     # 1. wait until started
mvn -pl config-server    spring-boot:run     # 2. wait until started (the services need it)
mvn -pl product-service  spring-boot:run     # 3.
mvn -pl order-service    spring-boot:run     # 4.
mvn -pl api-gateway      spring-boot:run     # 5.
```
Wait about 30 seconds after the last one, then open http://localhost:8761: `PRODUCT-SERVICE`,
`ORDER-SERVICE` and `API-GATEWAY` must be UP.

On first start each service loads sample data (5 products, 3 customers, 3 orders).
Open **http://localhost:8080/** to see everything as JSON.

### 6. Run the API tests
```bash
./2_api-tests.sh
```
It deletes all data, then posts 2 products, 2 customers and 2 orders and prints them, all through port 8080.
The last step shows the laptop's stock dropping from 5 to 4: order-service asked product-service to reduce it over REST.

### 7. Show the actuator health checks
```bash
curl localhost:8761/actuator/health
curl localhost:8888/actuator/health
curl localhost:8080/actuator/health
curl localhost:8080/health/product     # product-service, through the gateway
curl localhost:8080/health/order       # order-service, through the gateway
```
Stop a database (`docker stop mongo2`) and call `/health/order` again: it becomes DOWN.

### 8. Prove database-per-service
```bash
docker exec -it mongo1 mongosh --quiet --eval "show dbs"                      # productdb
docker exec -it mongo2 mongosh --quiet --eval "show dbs"                      # orderdb
docker exec -it mongo1 mongosh --quiet productdb --eval "db.products.getIndexes()"
docker exec -it mongo2 mongosh --quiet orderdb --eval "db.orders.findOne()"
```
In the order, `customer` is stored as an id only (reference) and `items` are inside the document (embedded).

### 9. Prove high availability / no hardcoded ports
- Start a second product-service (`mvn -pl product-service spring-boot:run` in a new terminal). Eureka shows two
  instances and requests are load balanced. Stop one and the app keeps working.
- No Java class contains a host or port: Feign uses `product-service`, the gateway uses `lb://...`.

### 10. Stop
Ctrl+C in each terminal, then `./3_docker-stop-mongodb.sh`.

---------------------------------------------------------------------------------------------------

## API (all through http://localhost:8080)

| Method and path | Description |
|---|---|
| `POST /api/product` | create a product (`sku`, `name`, `category`, `price`, `stock`, optional `specs`) |
| `GET /api/product/id/{id}`, `GET /api/product/sku/{sku}` | read one |
| `GET /api/products` (`?category=computers`) | list |
| `POST /api/product/id/{id}/review` | add an embedded review `{author, rating, comment}` |
| `PUT /api/product/id/{id}/reduce-stock?quantity=n` | used by order-service (atomic, never oversells) |
| `DELETE /api/products` | delete all (returns the count) |
| `POST /api/customer`, `GET /api/customer/id/{id}`, `GET /api/customers`, `DELETE /api/customers` | customers |
| `POST /api/order` | `{customerId, items:[{productId, quantity}]}` |
| `GET /api/order/id/{id}`, `GET /api/orders`, `DELETE /api/orders` | orders |

## Where each criterion is met

| Criterion | Where |
|---|---|
| **1. Database per service** | `config-repo/product-service.properties` -> `mongo1:27017/productdb`; `config-repo/order-service.properties` -> `mongo2:27018/orderdb`. Different servers, different entities, no shared code. |
| **1. REST between services** | `order-service/.../client/ProductClient.java` (OpenFeign). The gateway's `HomeController` uses WebClient. |
| **2. `@Indexed`** | `Product.sku` (unique), `name`, `category`; `Customer.email` (unique); `Order.status`, `Order.created`. Only fields that are queried. |
| **2. Embed vs reference** | Embed: `Product.specs`, `Product.reviews`, `Order.items`. Reference: `Order.customer` with `@DocumentReference`. |
| **3. Eureka, no hardcoded ports** | `server.port=0`, `@FeignClient(name="product-service")`, `lb://` gateway routes. |
| **3. Single public port** | The gateway on 8080 routes all `/api/...` calls. |
| **4. Actuator** | `spring-boot-starter-actuator` in every module pom; `/actuator/health` exposed. |

### Embed vs reference - reasoning
| Data | Choice | Why |
|---|---|---|
| `Product.specs` | Embed | Small, fixed, always shown with the product, never shared. |
| `Product.reviews` | Embed | A review belongs to one product and is read with it. Written with an atomic `$push`. If reviews grew to thousands, move them to their own collection. |
| `Order.items` | Embed | Items have no meaning without the order. They are a **snapshot** (name, price) so old orders stay correct if the product changes. |
| `Order.customer` | **Reference** | One customer has many orders. Copying customer data into every order would duplicate it and make updates painful. Only the `_id` is stored. |
| `Order.items[].productId` | Plain id string | The product lives in **another service's database**, so a `@DocumentReference` would break the rule. Details come over REST. |

## Troubleshooting
| Problem | Fix |
|---|---|
| Service fails at startup, cannot reach port 8888 | Start `config-server` first and wait until it is up. |
| `port is already allocated` on 27017 | `docker rm -f poc-mongo` or stop whatever uses the port. |
| Gateway returns 503 / 500 | Services not registered yet: wait 30 s and check http://localhost:8761. |
| Test script prints nothing | Services not ready yet; try `curl localhost:8080/api/products` first. |
| Duplicate SKU or email returns 409 | Expected: that is the unique index doing its job. |

## Known limits (good to mention)
- Placing an order is not a distributed transaction: if stock reduction works for item 1 but fails for item 2, item 1 is not rolled back (a Saga would solve it).
- No circuit breaker, no security, no automated tests yet.
