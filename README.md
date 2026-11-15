# RouteCharge

RouteCharge es una app Android para planificar viajes largos en auto eléctrico en Argentina. Uno elige su auto, la batería con la que sale y el origen y el destino, y la app calcula la ruta y dice dónde conviene parar a cargar, cuánto tiempo y con qué batería se llega a cada parada y al destino. Después acompaña el viaje con una pantalla de navegación que va actualizando la batería, y deja el viaje guardado en el teléfono para volver a abrirlo sin conexión.

El paquete se llama `com.chargeroute.app` porque el proyecto empezó llamándose ChargeRoute. Cambiarlo no aportaba nada, así que quedó así.

## Integrantes

- Agustín Vukusic (L.U. 1171288)
- Juan Ignacio Aliano (L.U. 1141973)
- Tomás Goncevatt Miranda (L.U. 1091465)

Materia: Desarrollo de Aplicaciones I, 2° cuatrimestre de 2026, turno noche.

## Cómo ejecutarla

Hace falta:

- Android Studio reciente.
- JDK 11 o superior.
- Un emulador o un teléfono con Android 8.0 (API 26) o más nuevo.

Pasos:

1. Clonar el repositorio y abrirlo con Android Studio (File > Open).
2. Pedir una clave gratuita de OpenChargeMap en openchargemap.org.
3. Crear o editar el archivo `local.properties` en la raíz del proyecto y agregar la línea `OCM_API_KEY=<tu clave>`.
4. Dejar que Gradle sincronice y correr la app (Run) en el emulador o en el teléfono.

La clave llega a la app a través de `BuildConfig`. Si no se carga, la app compila y funciona igual, pero no descarga cargadores nuevos: usa solo los que ya estén guardados en el dispositivo. En una instalación nueva eso significa que no va a encontrar paradas de carga.

Permisos que pide: internet, estado de la red y ubicación (precisa y aproximada). La ubicación es opcional. Se pide al entrar a la pantalla de la ruta ("¿A dónde vamos?") y, si se rechaza, la app avisa y el origen se escribe a mano.

El login es simulado. Hay un formulario con nombre y mail que se valida y se guarda en el teléfono, y un botón "Continuar como invitado" que entra sin guardar nada.

## Funcionalidades implementadas

Siguen el orden en que las usa una persona.

- **Login simulado.** Nombre y mail guardados en DataStore. Con sesión guardada la app abre directo en la pantalla del auto. El invitado entra sin sesión y al reabrir la app vuelve al login.
- **Datos del auto.** Se elige entre 22 modelos del catálogo (con su batería, consumo y conectores), se fija la batería inicial con un slider o con los botones − y + (de 5 en 5, entre 5 y 100 %) y se activa o no el aire acondicionado. El auto elegido se recuerda para la próxima vez.
- **Ruta.** El origen es la ubicación GPS actual o una dirección escrita a mano; el destino se busca por dirección. Hay una vista previa de la ruta en el mapa.
- **Resumen del viaje.** Distancia, duración, batería de llegada y las paradas de carga planificadas, con minutos de carga y batería antes y después de cada una. Si se llega con poca reserva, muestra un aviso.
- **Detalle de estación.** Potencia, conectores, cantidad de puntos y de dónde salen los datos, más un control para indicar la batería después de cargar.
- **Navegación.** Mapa con la ruta, indicación del próximo giro, avance con GPS real o simulado, y la batería se va actualizando según los kilómetros recorridos. Avisa mientras busca señal, si falta el permiso de ubicación o si te alejaste de la ruta.
- **Llegada.** Resumen del viaje terminado: distancia, batería restante (en kWh y en km de autonomía) y cantidad de paradas.
- **Mis viajes.** Lista de los viajes guardados. Se pueden abrir sin conexión con su ruta y sus cargadores.
- **Mis cargadores.** Lista de los cargadores de las paradas de los viajes guardados, con el último viaje en que se usó cada uno (nombre y fecha) y, si aparece en más de uno, en cuántos viajes.
- **Menú lateral.** Accesos a las pantallas anteriores, perfil, cerrar sesión y selector de tema: claro, oscuro o automático según el sensor de luz.
- **Sin conexión.** Un banner "Sin conexión" avisa cuando se corta la red. Los viajes guardados se siguen abriendo; para calcular uno nuevo hace falta internet.

## Requisitos alcanzados

| ID | Requisito | Cómo se cumple |
|---|---|---|
| RF01 | Configurar parámetros de viaje | Las pantallas de auto y de ruta toman vehículo, batería inicial, aire acondicionado, origen y destino. Con eso se calcula la ruta y la distancia. |
| RF02 | Visualizar estaciones compatibles | El mapa y el resumen muestran solo los cargadores compatibles con los conectores del auto y a hasta 15 km de la ruta, con su potencia. |
| RF03 | Actualizar la batería en ruta | Durante la navegación y en el detalle de la estación se puede corregir el porcentaje real. La autonomía y la llegada se recalculan, y por debajo de 20 % se muestra el aviso de batería baja. |
| RF04 | Guardar la ruta para consultarla sin conexión | Al calcular, el viaje completo (ruta, paradas y estaciones) se guarda en Room en una sola transacción. Después se abre sin internet desde "Mis viajes". |

| ID | Requisito no funcional | Cómo se cumple |
|---|---|---|
| RNF01 | Comportamiento sin conexión | Sin red se pueden abrir los viajes guardados con su ruta y sus cargadores, que se leen de Room. Calcular una ruta nueva sí necesita internet (ver limitaciones). |
| RNF02 | Usabilidad táctil | Los controles principales (botones −/+, pestañas del selector de tema, botones de zoom del mapa, tarjetas) tienen un área táctil de al menos 44 dp. |
| RNF03 | Rendimiento | Las llamadas de red y de base de datos se hacen en corrutinas fuera del hilo principal (`Dispatchers.IO`). Las listas observan Room con `Flow`. |
| RNF04 | Adaptación lumínica | Hay tema claro y oscuro, y un modo automático que cambia según el sensor de luz ambiental. |
| RNF05 | Uso responsable de sensores | El sensor de luz y el GPS se pausan cuando la app no está en pantalla, y los flows de los ViewModels se leen con `collectAsStateWithLifecycle`. |
| RNF06 | Recuperación ante errores | Si falla el cálculo de una ruta, un banner ofrece "Reintentar" y "Abrir última ruta". Guardar el viaje también está protegido y muestra un mensaje si falla. |

## Arquitectura final

MVVM en tres capas: presentación, dominio y datos.

```
app/src/main/java/com/chargeroute/app/
├── ChargeRouteApp.kt        Application, crea el AppContainer
├── MainActivity.kt
├── core/
│   ├── theme/               colores, tema, tipografía
│   ├── network/             NetworkConnectivityObserver
│   ├── sensor/              AmbientLightSensorManager, LightThemeHysteresis
│   ├── location/            LocationClient, permiso de ubicación
│   └── di/                  AppContainer
├── domain/
│   ├── model/               Vehicle, Station, Trip, ChargingPlanner, RangeEstimator...
│   ├── repository/          interfaces: Route, Station, Trip, Vehicle, User
│   └── usecase/             CalculateRoute, GeocodeAddress, GetNavigationSteps,
│                            GetVehicleCatalog, SaveTrip, UpdateBattery
├── data/
│   ├── local/               Room (entity, dao) y DataStore
│   ├── remote/              Retrofit, api y dto
│   ├── repository/          implementaciones (RouteRepositoryImpl, etc.)
│   └── mapper/              entidades y DTOs a modelos de dominio
└── presentation/
    ├── navigation/          AppNavHost, Screen
    ├── components/          mapa, banners, tarjetas
    ├── screens/             login, carsetup, planning, summary, station,
    │                        navigation, arrival, savedtrips, chargers, sidebar
    └── preview/             datos de ejemplo para los @Preview
```

**Presentación.** Cada pantalla es un composable que observa un `StateFlow` de su ViewModel y le manda las acciones del usuario. Las cosas que pasan una sola vez, como navegar después de calcular una ruta, viajan como eventos (`SharedFlow`) y no como estado.

**Dominio.** Tiene los modelos, las interfaces de los repositorios y los casos de uso. Es Kotlin puro, sin dependencias de Android. La lógica de cálculo (planificador de cargas, estimación de autonomía, consumo con aire, filtro de estaciones, niveles de batería, antigüedad de los datos) vive en `domain/model` y tiene tests unitarios.

**Datos.** Implementa las interfaces del dominio. Room guarda viajes, rutas, estaciones y paradas; DataStore guarda el tema, el auto elegido y el perfil (nombre, mail y sesión); Retrofit habla con los servicios externos.

La inyección de dependencias es manual: `AppContainer` crea los repositorios y los casos de uso, y los ViewModels los reciben por constructor. No usamos Hilt.

Navegación con Navigation Compose. Las rutas son:

`login` → `car_setup` → `route_planning` → `route_summary/{tripId}` → `station_detail/{tripId}/{stopId}` → `active_navigation/{tripId}` → `arrival/{tripId}`

Aparte quedan `saved_trips` y `used_chargers`, que se abren desde el menú lateral. `SessionViewModel` decide si la app arranca en el login o en la pantalla del auto según haya una sesión guardada.

## Tecnologías utilizadas

| Tecnología | Versión | Para qué se usa |
|---|---|---|
| Kotlin | 2.0.21 | Lenguaje de toda la app |
| Gradle / AGP | 9.5.0 / 9.3.3 | Build |
| KSP | 2.0.21-1.0.28 | Genera el código de Room |
| Jetpack Compose (BOM) | 2024.11.00 | Interfaz, con Material 3 |
| Activity Compose | 1.9.3 | Integra Compose con la Activity |
| Navigation Compose | 2.8.4 | Navegación entre pantallas |
| Lifecycle | 2.8.7 | ViewModel y lectura de flows según el ciclo de vida |
| Room | 2.7.2 | Base local; esquema exportado en `app/schemas` |
| DataStore Preferences | 1.1.1 | Tema, auto elegido y perfil |
| Retrofit + Gson | 2.11.0 | Llamadas HTTP y parseo de JSON |
| OkHttp | 4.12.0 | Cliente HTTP, con log de red solo en debug |
| Coroutines | 1.9.0 | Trabajo asíncrono |
| osmdroid | 6.1.20 | Mapa con tiles de OpenStreetMap |
| Play Services Location | 21.3.0 | GPS (`FusedLocationProviderClient`) |
| JUnit / coroutines-test | 4.13.2 / 1.9.0 | Tests unitarios |

Valores de compilación: minSdk 26, targetSdk 35, compileSdk 37, Java 11, versionName 1.0.

Servicios externos, todos gratuitos:

- **OSRM** (`router.project-osrm.org`): calcula la ruta y las indicaciones de giro.
- **OpenChargeMap** (`api.openchargemap.io`): devuelve los cargadores. Necesita la clave del `local.properties`.
- **Nominatim** (`nominatim.openstreetmap.org`): convierte direcciones escritas en coordenadas.

## Decisiones relevantes

**Reserva de seguridad del 15 %.** El planificador nunca arma un plan que llegue a una parada o al destino con menos de 15 % de batería. El aviso del resumen usa esa misma constante (`ChargingPlanner.SAFETY_RESERVE_PERCENT`); antes usaba 10 y no coincidía con lo que planificaba. Se resigna algo de autonomía a cambio de no depender de que la estimación sea exacta.

**Parada lejana, salvo que haya un cargador mucho más rápido.** Por defecto se elige la estación alcanzable más lejana, para parar lo menos posible. Si hasta 50 km antes hay un cargador al menos 1,5 veces más rápido para ese auto, se para ahí, porque se pasa menos tiempo cargando. Eso puede sumar una parada en un viaje largo. El 1,5 evita cambiar de estación por diferencias chicas, como 50 contra 60 kW.

**Lógica de cálculo en el dominio.** Todo lo que calcula (planificador, autonomía, consumo, filtros) está en Kotlin puro, sin Android. Así se prueba con tests unitarios sin emulador y no queda mezclado con las pantallas.

**ViewModels sin `Context`.** Los ViewModels exponen el estado (booleanos y enums, como los errores de la pantalla de ruta) y la pantalla elige el texto con `stringResource`. Cuesta un `when` más en la pantalla, pero los textos quedan en `strings.xml` y el `when` sin `else` obliga a escribir uno nuevo si se agrega un caso.

**Interfaces en el dominio e inyección manual.** Los repositorios son interfaces del dominio y las implementa la capa de datos; `AppContainer` los arma a mano. Con Hilt habría menos código repetido, pero para un proyecto de este tamaño sumaba más piezas de las que resolvía. Las interfaces permiten probar los ViewModels con repositorios falsos.

**Room como fuente de lo que se ve sin conexión.** Se guarda el viaje completo con su ruta, paradas y estaciones, en una transacción para que nunca quede un viaje a medias. Cuando cambió el esquema escribimos la migración 1 a 2 a mano y exportamos el esquema, en vez de usar el borrado destructivo, que hubiera eliminado los viajes guardados. Para llegar a esto hubo que subir Room de 2.6.1 a 2.7.2, porque la anterior generaba mal los DAO `suspend` con KSP2.

**AC o DC según el conector.** La línea del conector en el detalle depende del tipo (un CCS2 de 25 kW sigue siendo corriente continua y un Type 2 de 60 kW sigue siendo alterna), no de la potencia. Si el texto de un conector guardado no se reconoce, se descarta en vez de asumir uno compatible, para no mostrarle al usuario un conector que su auto quizás no pueda usar.

**Login simulado.** No hay backend: nombre y mail se validan y se guardan en DataStore. Era el alcance acordado para el MVP, y alcanza para que el menú muestre el perfil real y la app recuerde la sesión. El costo es que no hay cuentas ni sincronización.

**Servicios abiertos y sin costo.** Mapa con osmdroid y OpenStreetMap, rutas con OSRM y cargadores con OpenChargeMap. Ninguno pide tarjeta y todos se pueden usar en un trabajo universitario. A cambio dependemos de servicios públicos, sin garantías de disponibilidad.

**Sensores atados al ciclo de vida.** El sensor de luz y el GPS se pausan cuando la app no está en pantalla, para gastar menos batería. El tema automático usa histéresis (oscuro por debajo de 30 lux, claro por encima de 60) para que no parpadee cuando la luz queda en el límite.

## Limitaciones conocidas

- **Mapa sin conexión.** La política de uso de los tiles de OpenStreetMap prohíbe la descarga masiva, así que no se precarga el mapa. Sin conexión se ven las zonas ya visitadas (caché de osmdroid); la ruta y los cargadores se dibujan igual porque están guardados en Room.
- **Calcular una ruta nueva necesita conexión.** Sin internet se muestra un error con "Reintentar" y "Abrir última ruta".
- **Disponibilidad de los cargadores.** OpenChargeMap no informa si un cargador está ocupado en tiempo real, por eso la app muestra "Disponibilidad no informada".
- **Indicaciones giro a giro.** El servicio de rutas las devuelve en código y se traducen al español dentro del repositorio de rutas (capa de datos), no en la pantalla.
- **Antigüedad de los datos.** No usamos la fecha de última actualización que informa OpenChargeMap para cada cargador. La antigüedad que se muestra es la de cuándo la app guardó el dato.
- **Contraste.** El verde de la marca con texto blanco tiene contraste bajo. Lo mantuvimos por identidad visual.
- **Login simulado.** No hay cuentas reales ni sincronización entre dispositivos.
- **Datos del catálogo.** Los km "declarados" de los autos mezclan ciclos de homologación (WLTP, NEDC, CLTC) y algunos conectores son estimados. El planificador usa el consumo de cada auto, no esos km.

Cómo se resolvería el mapa sin conexión en producción: lo mejor sería usar mapas vectoriales por región (archivos `.mbtiles` armados con datos abiertos de OSM, como hacen OsmAnd u Organic Maps). Otras opciones son un proveedor comercial de tiles que permita el uso offline, donde en osmdroid solo cambia el `TileSource` y se agrega una clave, o un servidor de tiles propio si hubiera muchos usuarios. No lo hicimos en el MVP para respetar los términos del servicio público y no depender de servicios pagos; lo importante, que es la ruta, dónde cargar y la batería, funciona sin conexión.
