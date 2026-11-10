# RouteCharge ⚡🚗

> **Aplicación móvil para planificar viajes en automóvil eléctrico**  
> **Materia:** Desarrollo de Aplicaciones I — 2° Cuatrimestre 2026 (Turno Noche)  
> **Institución:** Universidad Argentina de la Empresa (UADE) — Facultad de Ingeniería y Cs. Exactas  
> **Integrantes:**  
> • Vukusic Agustín — L.U: 1171288  
> • Aliano Juan Ignacio — L.U: 1141973  
> • Tomás Goncevatt Miranda — L.U: 1091465

---

## 1. Propósito y Propuesta de Valor

Quienes conducen un vehículo eléctrico y planifican viajes interurbanos sufren de **ansiedad de autonomía (*range anxiety*)** y alta carga cognitiva debido a la dispersión de información sobre estaciones de recarga, tipos de conectores y compatibilidad.

**RouteCharge** resuelve este problema integrando:
- Cálculo y trazado de rutas interurbanas optimizadas para vehículos eléctricos.
- Estimación precisa del consumo de energía según el vehículo y el uso de climatización (A/C).
- Detección y recomendación de paradas estratégicas en estaciones de recarga compatibles (ej. *Punto de carga Dolores* en Autovía 2 km 210 para el trayecto Buenos Aires → Mar del Plata).
- Arquitectura **Offline First**: la ruta, las estaciones y los cálculos se persisten localmente en SQLite mediante **Room**, permitiendo continuar la navegación y consulta en zonas sin cobertura 4G/5G en ruta sin cierres inesperados.
- Adaptación lumínica automática (**RNF04** / **RNF05**) mediante sensor de luz ambiental (`TYPE_LIGHT`) para alternar entre modo claro de alto contraste (luz solar directa) y modo oscuro (conducción nocturna).

---

## 2. Requisitos Funcionales Implementados

| ID | Requisito Funcional | Estado | Descripción y Criterio de Aceptación |
|---|---|:---:|---|
| **RF01** | **Configurar parámetros de viaje** | ✅ Cumplido | El conductor selecciona origen, destino, vehículo (catálogo precargado o personalizado con specs reales de batería y conectores), nivel de batería inicial (%) y switch de climatización. Calcula la distancia total y traza la ruta en el mapa. |
| **RF02** | **Visualizar estaciones de carga compatibles** | ✅ Cumplido | Muestra en el mapa los puntos de recarga filtrados según el conector del auto (CCS2, Type 2, etc.), su potencia en kW (DC rápida o AC) y distancia al recorrido. |
| **RF03** | **Actualizar nivel de batería en ruta** | ✅ Cumplido | Permite al conductor ajustar interactivamente el porcentaje real de batería desde el copiloto o tras una parada de carga, recalculando de inmediato la autonomía y advirtiendo en caso de batería baja (< 20%). |
| **RF04** | **Guardar ruta para consulta sin conexión (Offline)** | ✅ Cumplido | Al calcular la ruta, se almacena íntegramente en Room (Base de Datos SQLite local) junto con sus paradas y estaciones. Si el teléfono entra en modo avión o pierde señal, el viaje sigue disponible sin errores ni timeouts. |

---

## 3. Requisitos No Funcionales (RNF)

- **RNF01 - Comportamiento Offline:** Implementación estricta de *Offline First* coordinada por `RouteRepositoryImpl` y base de datos Room (`trips`, `routes`, `stations`, `trip_stops`).
- **RNF02 - Usabilidad e Interacción Táctil (Ley de Fitts):** Todos los componentes interactivos principales (botones de acción, marcadores, tarjetas) respetan un área táctil activa mínima de **44x44 dp**.
- **RNF03 - Rendimiento:** Cálculos y flujos reactivos desacoplados en corrutinas (`Dispatchers.IO`) con renderizado en menos de 5 segundos.
- **RNF04 - Accesibilidad y Adaptación Lumínica:** Paleta de alto contraste en Modo Claro y paleta balanceada en Modo Oscuro.
- **RNF05 - Uso responsable de sensores:** Sensor de luz (`SensorManager` - `TYPE_LIGHT`) y GPS (`FusedLocationProviderClient`) activos únicamente bajo demanda en primer plano.
- **RNF06 - Recuperación ante errores:** Banners no intrusivos que permiten reintentar o abrir la última ruta guardada ante fallos de conexión.

---

## 4. Flujo de Pantallas

La aplicación implementa las 8 pantallas documentadas en el prototipo Figma:

1. **Login:** Inicio de sesión federado simulado con Google / Cuenta de usuario local.
2. **Datos del auto (Paso 1 de 3):** Selección de vehículo (Hyundai Kona Electric 64 kWh, Tesla Model 3, Nissan Leaf, etc.), ficha técnica autocompletada, interruptor de aire acondicionado y barra interactiva de batería actual.
3. **Ruta A a B (Paso 2 de 3):** Origen (ej. *9 de Julio e Independencia*) y Destino (*Plaza Colón, Mar del Plata*), botón de GPS actual, intercambiador de puntos y vista previa en mapa.
4. **Resumen de ruta (Paso 3 de 3):** Mapa completo con polilínea verde, métricas críticas (414 km, 5 h 45 min, 22% al llegar), desglose de paradas de carga (*Punto de carga Dolores*, 35 min, 19% → 71%) y botón "Iniciar viaje".
5. **Detalle del cargador:** Información detallada de la estación, potencia y compatibilidad de conectores (CCS2, Type 2, CHAdeMO), servicios cercanos (24 h, café, baños) y formulario de actualización de batería post-carga (**RF03**).
6. **Navegación activa (Copiloto):** Vista de conducción en ruta con mapa en vivo, velocímetro (90 km/h), maniobras, alerta de batería baja si corresponde, y controles para ajustar batería en ruta (**RF03**). Incluye botón de avance simulado.
7. **Llegada:** Pantalla de arribo a destino con felicitaciones, estadísticas consolidadas del viaje y opciones para planificar un nuevo trayecto o revisar el resumen.
8. **Sidebar (Navigation Drawer):** Perfil de conductor (Sofía / Martín), accesos directos, selector de apariencia lumínica (Claro / Oscuro / Automático por sensor de luz) y acceso a "Mis viajes" guardados en Room.

---

## 5. Arquitectura del Software (Clean Architecture + MVVM)

```
app/src/main/java/com/chargeroute/app/
├── ChargeRouteApp.kt            # Application y AppContainer
├── MainActivity.kt               # Entrypoint con sensor de luz y Compose
│
├── core/                         # Fundaciones y sensores
│   ├── theme/                    # Color, Theme, Type, ThemeMode
│   ├── network/                  # NetworkConnectivityObserver
│   ├── sensor/                   # AmbientLightSensorManager (TYPE_LIGHT)
│   ├── location/                 # LocationClient (FusedLocationProviderClient)
│   └── di/                       # AppContainer (Inyección de dependencias limpia)
│
├── domain/                       # Lógica de negocio pura (independiente de UI/framework)
│   ├── model/                    # Vehicle, Station, Connector, RoutePlan, Trip, TripStop
│   ├── repository/               # Interfaces de repositorios (Route, Trip, Vehicle)
│   └── usecase/                  # CalculateRoute, UpdateBattery, GetOfflineRoute, SaveTrip
│
├── data/                         # Capa de datos y persistencia
│   ├── local/
│   │   ├── ChargeRouteDatabase   # Room SQLite Database
│   │   ├── entity/               # TripEntity, RouteEntity, StationEntity, TripStopEntity
│   │   ├── dao/                  # TripDao, RouteDao, StationDao, TripStopDao
│   │   └── datastore/            # UserPreferencesDataStore (Tema, Auto por defecto)
│   ├── remote/                   # Retrofit, OSRM API, OpenChargeMap API, DTOs
│   ├── repository/               # Implementaciones de repositorios (Offline-First)
│   └── mapper/                   # Mappers entre entidades, DTOs y modelos de dominio
│
└── presentation/                 # Capa de presentación (Jetpack Compose + MVVM)
    ├── navigation/               # AppNavHost, Screen definitions
    ├── components/               # RouteMapView (Canvas), TouchAccessibleCard, Badges
    └── screens/                  # Login, CarSetup, Planning, Summary, Station, Navigation, Arrival, Sidebar
```

---

## 6. Instrucciones de Ejecución en Android Studio

### Requisitos del Sistema
- **Android Studio:** Ladybug / Meerkat / Hedgehog (o posterior).
- **JDK:** Java 17 o Java 21 (configurado en Android Studio).
- **Dispositivo Virtual (AVD):**
  - **Dispositivo:** Pixel 7a (Pantalla 1080 x 2400, 420 dpi).
  - **Sistema Operativo:** Android 15.0 (API 35 "VanillaIceCream").
  - **Imagen del sistema:** Google Play Intel x86_64 Atom System Image (API 35).

### Pasos para Ejecutar
1. Abrir **Android Studio**.
2. Seleccionar **File > Open...** y elegir la carpeta del proyecto:
   `RouteCharge App`
3. Esperar que Gradle sincronice las dependencias automáticamente (tomará pocos segundos ya que el wrapper y las dependencias están resueltos).
4. En el selector superior de dispositivos de Android Studio, verificar que esté seleccionado el AVD **Pixel 7a (API 35)**.
5. Hacer clic en el botón verde **Run (Shift + F10)**.
6. La aplicación se compilará e instalará en el emulador Pixel 7a automáticamente.
