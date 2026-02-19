
# Movie Discovery

[![Java](https://img.shields.io/badge/Java-17+-orange?style=flat-square)](https://www.java.com/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-blue?style=flat-square)](https://maven.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=flat-square)](https://www.docker.com/)
[![Status](https://img.shields.io/badge/Status-In%20Development-yellow?style=flat-square)]()
[![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)](LICENSE)

---

## 📋 Descripción

Movie Discovery es una aplicación de descubrimiento de películas que utiliza recomendaciones personalizadas basadas en la API de TasteDive. El sistema combina un backend robusto con Kafka para procesamiento asincrónico y un frontend de escritorio intuitivo.

---

## 🛠️ Tecnologías

| Categoría | Tecnologías |
|-----------|-------------|
| **Backend** | Java, Maven, Servlet, Kafka |
| **Frontend** | Java Swing |
| **Base de Datos** | PostgreSQL |
| **Integración** | TasteDive API |
| **Containerización** | Docker |
| **Mensajería** | Apache Kafka |

---

## ✨ Características Principales

- 🔐 **Autenticación segura** con gestión de sesiones
- 🎬 **Búsqueda de películas** mediante integración con TasteDive API
- 🤖 **Recomendaciones personalizadas** basadas en preferencias
- ⚡ **Procesamiento asincrónico** con Apache Kafka
- 💾 **Caché de imágenes** para optimización de rendimiento
- 🎨 **Interfaz gráfica intuitiva** con Java Swing
- 🐳 **Despliegue containerizado** con Docker

---

## 📂 Estructura del Proyecto

```
Movie-Discovery/
├── backend/
│   ├── src/main/java/com/tastedivekafka/
│   │   ├── api/              # Servlets y clientes API
│   │   ├── config/           # Configuración de la aplicación
│   │   ├── db/               # DAOs y conexión BD
│   │   └── kafka/            # Servicios Kafka
│   ├── src/main/resources/   # Archivos de configuración
│   ├── Dockerfile            # Containerización backend
│   └── pom.xml              # Dependencias Maven
├── frontend/
│   ├── src/main/java/com/tastedivekafka/
│   │   ├── session/          # Gestión de sesiones
│   │   └── ui/               # Componentes de interfaz
│   ├── Dockerfile            # Containerización frontend
│   └── pom.xml              # Dependencias Maven
├── db/
│   └── init.sql             # Scripts de inicialización BD
├── .gitignore               # Configuración Git
└── README.md                # Este archivo
```

---

## 🚀 Instalación

### Requisitos Previos

- **Java 17+**
- **Maven 3.8+**
- **MySQL 8.0+**
- **Docker y Docker Compose** (opcional)
- **Apache Kafka** (para desarrollo local)

### Paso 1: Clonar el repositorio

```bash
git clone https://github.com/sargon494/Movie-Discovery.git
cd Movie-Discovery
```

### Paso 2: Configurar la base de datos

```bash
psql -u root -p < db/init.sql
```

### Paso 3: Configurar variables de entorno

Crear archivo `backend/src/main/resources/config.properties`:

```properties
# Database
db.url=jdbc:mysql://localhost:3306/movie_discovery
db.user=root
db.password=your_password

# Kafka
kafka.bootstrap.servers=localhost:9092
kafka.group.id=movie-discovery-group

# API
tastdive.api.key=your_api_key
```

### Paso 4: Compilar el proyecto

```bash
# Backend
cd backend
mvn clean package

# Frontend
cd ../frontend
mvn clean package
```

### Paso 5: Ejecutar con Docker Compose (Recomendado)

```bash
docker-compose up -d
```

---

## ⚙️ Configuración

### Variables Requeridas

| Variable | Descripción | Tipo |
|----------|-------------|------|
| `db.url` | URL de conexión MySQL | String |
| `db.user` | Usuario de base de datos | String |
| `db.password` | Contraseña BD | String |
| `kafka.bootstrap.servers` | Brokers de Kafka | String |
| `tastdive.api.key` | API Key de TasteDive | String |

### Base de Datos

El schema se crea automáticamente al ejecutar:

```bash
psql < db/init.sql
```

Tablas principales:
- `users` - Gestión de usuarios
- `movies` - Catálogo de películas
- `recommendations` - Recomendaciones personalizadas

---

## 💻 Uso

### Ejecutar Backend

```bash
cd backend
mvn spring-boot:run
```

Backend disponible en: `http://localhost:8090`

### Ejecutar Frontend

```bash
cd frontend
mvn exec:java -Dexec.mainClass="com.tastedivekafka.FrontendApp"
```

### Endpoints Principales

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/auth/login` | Autenticación de usuario |
| POST | `/api/auth/register` | Registro de usuario |
| GET | `/api/search?q=term` | Búsqueda de películas |
| GET | `/api/recommendations` | Obtener recomendaciones |

---

## 👥 Roles de Usuario

- **Usuario Regular**: Acceso a búsqueda, recomendaciones personalizadas
- **Administrador**: Gestión completa de usuarios y contenido

---

## 🔄 Flujo de Procesamiento

1. Usuario realiza búsqueda desde el frontend
2. Request se envía al backend
3. Backend procesa y publica evento en Kafka
4. Consumer de Kafka procesa en background
5. Resultados se almacenan en BD
6. Frontend recibe recomendaciones personalizadas

---

## 🚧 Mejoras Futuras

- [ ] Aplicación móvil (Android/iOS)
- [ ] Sistema de valoraciones y reseñas
- [ ] Integración con múltiples APIs de películas
- [ ] Dashboards analíticos avanzados
- [ ] Exportación de listas de películas

---

## 📸 Screenshots

*Agregar capturas de pantalla de la aplicación*

```markdown
![Login Screen](docs/screenshots/login.png)
![Search Results](docs/screenshots/search.png)
![Recommendations](docs/screenshots/recommendations.png)
```

---

## 📝 Licencia

Este proyecto está bajo la licencia **MIT**. Ver archivo [LICENSE](LICENSE) para más detalles.

---

## 👨‍💻 Autor

**Sargon494**

- GitHub: [@sargon494](https://github.com/sargon494)

---


**Última actualización**: 2026 | **Versión**: 1.0.0-beta
