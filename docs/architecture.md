# Arquitectura y Flujo del Sistema Movie-Discovery

## Diagrama de Secuencia

```mermaid
sequenceDiagram
    participant U as Usuario
    participant F as Frontend (Java Swing)
    participant B as Backend (Render)
    participant DB as Base de Datos (Supabase)
    participant K as Kafka (Aiven)
    participant T as TasteDive API

    U->>F: Abre aplicación
    F->>B: Solicitud de login (AuthServlet)
    B->>DB: Verifica credenciales
    DB-->>B: Respuesta autenticación
    B-->>F: Token de sesión
    F-->>U: Pantalla principal

    U->>F: Busca películas
    F->>B: Solicitud búsqueda (SearchServlet)
    B->>DB: Consulta películas
    DB-->>B: Resultados
    B-->>F: Lista de películas
    F-->>U: Muestra resultados

    U->>F: Solicita recomendaciones
    F->>B: Produce mensaje recomendación (KafkaProducerService)
    B->>K: Envía mensaje a topic
    K->>B: Confirma recepción
    B-->>F: Confirmación

    K->>B: Consumer procesa mensaje (KafkaConsumerService)
    B->>T: Consulta recomendaciones (TasteDiveClient)
    T-->>B: Datos recomendaciones
    B->>DB: Guarda recomendaciones
    B->>K: Produce respuesta (KafkaResponseConsumerService)
    K->>B: Consumer recibe respuesta
    B->>DB: Actualiza datos usuario
    F->>B: Polling o notificación para recomendaciones
    B-->>F: Recomendaciones actualizadas
    F-->>U: Muestra recomendaciones
```

## Descripción del Flujo

1. **Autenticación**: El usuario inicia sesión a través del frontend, que se comunica con el backend en Render. El backend verifica las credenciales en Supabase.

2. **Búsqueda de Películas**: El frontend solicita búsquedas al backend, que consulta la base de datos en Supabase.

3. **Recomendaciones**: Para recomendaciones, el backend usa Kafka en Aiven para procesar mensajes asincrónicamente. Envía consultas a la API de TasteDive y guarda los resultados en Supabase.

4. **Infraestructura**:
   - Backend: Desplegado en Render
   - Base de Datos: Supabase
   - Mensajería: Kafka en Aiven
   - API Externa: TasteDive para recomendaciones

Este diagrama muestra el flujo principal de la aplicación Movie-Discovery.