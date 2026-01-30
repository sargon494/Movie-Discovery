🎬 MovieDiscovery

📌 Descripción

MovieDiscovery es una aplicación de escritorio desarrollada en Java que permite descubrir películas similares a partir de una búsqueda del usuario. El sistema utiliza una arquitectura orientada a eventos basada en Apache Kafka, integrando una API externa (TasteDive) y una base de datos PostgreSQL para ofrecer recomendaciones de forma asíncrona, escalable y desacoplada.

El proyecto ha sido diseñado específicamente como proyecto de portfolio, demostrando buenas prácticas de arquitectura, separación de responsabilidades y uso de tecnologías utilizadas en entornos reales.

🧠 Arquitectura General

MovieDiscovery sigue una arquitectura event-driven basada en productores y consumidores Kafka.

Flujo principal:

1. El usuario inicia sesión en la aplicación

2. Introduce el nombre de una película

3. La UI envía la petición a Kafka

4. Un backend consume la petición y procesa la lógica

5. Se consulta la API de TasteDive

6. Se guardan los datos en PostgreSQL

7. Se envían las recomendaciones de vuelta a Kafka

8. La UI recibe las respuestas y actualiza la vista

Diagrama lógico simplificado:

[UI Swing]
│
│ produce (movie-topic)
▼
[Kafka Backend Consumer]
│
├──► TasteDive API
│
├──► PostgreSQL
│
└──► produce (movie-responses)
│
▼
[UI Response Consumer]

⚙️ Tecnologías Utilizadas

Java 17

Apache Kafka (mensajería asíncrona)

Docker & Docker Compose

Kafka

Zookeeper

PostgreSQL

PostgreSQL (persistencia de datos)

Swing (interfaz gráfica)

TasteDive API (recomendaciones de películas)

Apache NetBeans (desarrollo Java)

Visual Studio Code (gestión del proyecto y Docker)

GitHub Projects

🧩 Componentes Principales
🔌 Kafka
KafkaProducerService

Envía las búsquedas de películas al topic movie-topic

Desacopla completamente la UI del backend

KafkaConsumerService (Backend)

Consume peticiones de búsqueda

Llama a la API de TasteDive

Procesa y valida la respuesta

Guarda datos en PostgreSQL

Publica resultados en movie-responses

Gestiona errores mediante movie-errors

✔ Commit manual de offsets ✔ Shutdown limpio con wakeup()

KafkaResponseConsumerService

Escucha respuestas del backend

Notifica a la UI mediante callbacks

Usa un Group ID dinámico para recibir siempre mensajes nuevos

🗄️ Base de Datos (PostgreSQL)
DBConnection

Centraliza la conexión JDBC a PostgreSQL

Pensado para ejecutarse dentro de contenedores Docker

MovieDAO

Gestiona la tabla movies

Inserta o recupera películas evitando duplicados

Seguro ante concurrencia (ON CONFLICT)

RecommendationDAO

Gestiona las recomendaciones asociadas a películas

Permite guardar y recuperar recomendaciones

UserDAO

Gestiona la autenticación de usuarios

Comprueba credenciales contra la base de datos

⚠️ En esta versión, las contraseñas se almacenan en texto plano (mejora futura)

🌐 API Externa
TasteDiveClient

Encapsula las llamadas HTTP a la API de TasteDive

Codifica parámetros de forma segura

Devuelve la respuesta en formato JSON

Aísla completamente la dependencia externa

🖥️ Interfaz Gráfica (Swing)
LoginFrame

Pantalla de autenticación

UI personalizada

Comunicación desacoplada mediante listeners

MainFrame

Pantalla principal de búsqueda

Envía peticiones a Kafka

Escucha respuestas de forma asíncrona

Muestra las recomendaciones en formato de tarjetas

Carga imágenes en segundo plano para no bloquear la UI

🚀 Ejecución del Proyecto

Levantar los contenedores Docker:

docker-compose up -d

Asegurarse de que Kafka, Zookeeper y PostgreSQL están activos

Ejecutar la aplicación Java desde NetBeans o línea de comandos

🔮 Posibles Mejoras Futuras

Respuestas Kafka en formato JSON (Jackson / Gson)

Hash de contraseñas (BCrypt)

Historial de búsquedas por usuario

Caché de resultados en base de datos

Microservicio backend independiente

Tests unitarios e integración

Dockerización completa de la aplicación Java
