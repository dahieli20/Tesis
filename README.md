# Data Lake Filter

## Descripción

Data Lake Filter es una herramienta desarrollada para prevenir la formación de Data Swamps mediante la validación automática de datasets antes de su incorporación a un Data Lake.

La herramienta analiza archivos CSV y aplica diferentes reglas de validación para detectar:

* Duplicados exactos.
* Duplicados parciales.
* Posible redundancia.
* Valores nulos.
* Columnas duplicadas.
* Filas duplicadas internas.

A partir de estos análisis, el sistema clasifica los datasets como:

* ACCEPTED
* REVIEW
* REJECTED

y los almacena en distintas zonas dentro del Data Lake.

---

# Arquitectura

```text
Frontend Angular
        │
        ▼
Backend Spring Boot
        │
        ▼
MinIO (Data Lake)
        │
        ├── landing/
        ├── raw/
        ├── review/
        ├── rejected/
        └── metadata/
```

---

# Tecnologías utilizadas

## Backend

* Java 17
* Spring Boot 3.5.x
* Maven

## Frontend

* Angular
* TypeScript
* CSS

## Data Lake

* MinIO
* Docker

---

# Requisitos previos

Instalar:

## Java

```bash
java -version
```

Debe mostrar Java 17.

---

## Maven

```bash
mvn -version
```

---

## Node.js

```bash
node -v
npm -v
```

---

## Angular CLI

```bash
ng version
```

Si no está instalado:

```bash
npm install -g @angular/cli
```

---

## Docker Desktop

Verificar:

```bash
docker version
```

---

# Clonar proyecto

```bash
git clone <url-del-repositorio>
```

```bash
cd data-lake-filter
```

---

# Configuración de MinIO

## Levantar MinIO

Desde la raíz del proyecto:

```powershell
.\start-minio.ps1
```

o manualmente:

```powershell
cd docker

docker compose up -d
```

---

## Acceder a MinIO

URL:

```text
http://localhost:9001
```

Usuario:

```text
admin
```

Contraseña:

```text
admin123
```

---

## Crear bucket

Crear:

```text
data-lake
```

---

## Crear estructura

Dentro del bucket crear:

```text
landing/test.txt
raw/test.txt
review/test.txt
rejected/test.txt
metadata/test.txt
```

---

# Levantar Backend

Abrir una terminal:

```powershell
cd backend

mvn spring-boot:run
```

Backend disponible en:

```text
http://localhost:8080
```

Verificar:

```text
http://localhost:8080/api/minio/test
```

---

# Levantar Frontend

Abrir otra terminal:

```powershell
cd frontend

npm install

ng serve
```

Frontend disponible en:

```text
http://localhost:4200
```

---

# Flujo actual

El sistema:

1. Recibe un archivo CSV.
2. Analiza calidad básica.
3. Calcula hash global.
4. Detecta duplicados exactos.
5. Detecta coincidencias parciales.
6. Clasifica el dataset.

---

# Reglas actuales

## ACCEPTED

* Archivo nuevo.
* Calidad aceptable.

Destino:

```text
raw/
```

---

## REVIEW

* Posible duplicado parcial.
* Alto porcentaje de nulos.
* Columnas duplicadas.

Destino:

```text
review/
```

---

## REJECTED

* Duplicado exacto.
* CSV inválido.
* Archivo vacío.

Destino:

```text
rejected/
```

---

# Estructura del proyecto

```text
data-lake-filter
│
├── backend
│
├── frontend
│
├── docker
│
├── datasets-prueba
│
├── start-minio.ps1
│
├── stop-minio.ps1
│
└── README.md
```

---

# Estado actual del proyecto

Actualmente el prototipo implementa:

* Conexión Spring Boot ↔ MinIO.
* Subida de archivos CSV.
* Detección de duplicados exactos mediante SHA-256.
* Detección de coincidencias parciales mediante hashing por fila.
* Validaciones básicas de calidad.
* Clasificación automática ACCEPTED / REVIEW / REJECTED.
* Interfaz web Angular.

---

# Próximas mejoras

* Persistencia en PostgreSQL.
* Historial de análisis.
* Dashboard de datasets.
* Soporte para Excel.
* Métricas avanzadas de redundancia.
* Integración con IA para recomendaciones de calidad.
* Gestión de metadata.

```
```
