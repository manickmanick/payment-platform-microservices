# Food delivery microservices

This repository is a Java 21 / Spring Boot 4.1.1 food delivery microservices learning project. Each backend service follows Controller → Service → Repository → Entity, with per-service MySQL schemas managed by Flyway. OpenFeign handles synchronous service calls; Kafka handles order-created and mail-requested asynchronous events. The API Gateway routes the frontend `/api/v1` calls and provides Resilience4j retry/circuit breaker policies. The frontend is in `food-delivery-frontend`.

## Create the MySQL databases

Run this once in MySQL. It creates databases only; Flyway in each service creates and evolves tables.

```sql
CREATE DATABASE ecommerce_user_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE order_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE payment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE catalog_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE delivery_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE mail_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

Configure the USER, ORDER, PAYMENT, CATALOG, DELIVERY and MAIL database URLs and credentials, SMTP settings, plus a random `JWT_SECRET` of at least 32 bytes. A local `.env.example` lists expected settings; Spring does not load `.env` automatically, so export the values or use a local env loader. Never commit `.env` or production credentials. Each service owns its Flyway migrations.

Delivery SMS is opt-in per saved address. To enable it, configure Twilio `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, and `TWILIO_FROM_NUMBER` in the local environment or deployment secret store. Phone numbers must use E.164 format (for example `+14155552671`). If Twilio is not configured, email and in-app notifications continue to work.

## Current API / known implementation boundary

Auth, profile, address, restaurant/menu/category catalog, optional Cloudinary image upload, cart, checkout, Stripe PaymentIntent and verification, driver assignment/location tracking, and order notifications are implemented across services.

The frontend stores a bearer token and expects the user-service access token on login. Gateway auth routes preserve `/api/auth/**` for the user service; the other backend routes drop `/api/v1`. Order, notification and driver screens poll their authenticated REST APIs for updates.

## Main API endpoints

All public application routes below are exposed through the gateway with `/api/v1` prepended. User sign-up is `POST /auth/register`; the separate admin user management API is `GET/POST /users`, `GET/PUT/DELETE /users/{id}`, `PUT /users/me`, `PUT /users/me/password`, and `PUT /users/{id}/password`. Bootstrap the first administrator by listing its email in `ADMIN_EMAILS` before registration.

Catalog supports category read/admin CRUD, restaurant read/create/update/delete, and menu item read/create/update/delete. Restaurant owners can manage their own restaurants and menu items; only admins can manage categories. Cart supports add/read/update/remove/clear; checkout validates live menu prices and availability before creating the payment intent. Orders expose checkout, customer history/details/cancel, owner-scoped order listing/status updates, and driver tracking. Payments expose intent creation, verification, client secret retrieval, and the signed Stripe webhook; customer payment history/details are available at `GET /payments` and `GET /payments/{id}`. Addresses are customer-scoped CRUD; delivery exposes driver availability, accept/location/status, and customer notifications. Internal mail is queued through the Kafka `mail-requested` topic and processed by mail-service.

## Local development

Create the databases above, configure `.env`, then run `docker compose up --build`. The frontend is at `http://localhost:3000`, gateway at `http://localhost:8080`, Kafka UI at `http://localhost:8084`, and Schema Registry at `http://localhost:8083`. Swagger UIs are available under each service port at `/swagger-ui.html`. The internal mail API is at port `8088`; Kafka consumers accept JSON `mail-requested` events.

## Deployment status

Kubernetes application manifests are in `deploy/k8s/apps.yaml`; `deploy/aws/push-and-deploy.sh` builds and publishes images to ECR and applies them to an existing EKS cluster. Provision EKS, MySQL-compatible RDS and Kafka first. Populate `food-delivery-secrets` from AWS Secrets Manager and configure `FRONTEND_URL`, Google OAuth and Stripe settings before applying. AWS account/region resources are not provisioned by this repository.
