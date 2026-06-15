# FileManager Backend — Spring Boot + PostgreSQL

Backend REST pour la gestion de fichiers PDF avec authentification JWT.

---

## Stack technique

| Composant | Technologie |
|---|---|
| Framework | Spring Boot 3.2 |
| Sécurité | Spring Security + JWT (JJWT 0.11) |
| Base de données | PostgreSQL 15+ |
| ORM | Spring Data JPA / Hibernate |
| Chiffrement MDP | BCrypt (coût 12) |
| Java | 17+ |

---

## Structure du projet

```
src/main/java/com/filemanager/
├── FileManagerApplication.java       ← Point d'entrée
├── config/
│   └── SecurityConfig.java           ← Config Spring Security + CORS
├── controller/
│   ├── AuthController.java           ← POST /api/auth/register|login
│   └── FileController.java           ← CRUD /api/files
├── dto/
│   ├── LoginRequest.java
│   ├── RegisterRequest.java
│   ├── AuthResponse.java             ← { token, user }
│   ├── UserResponse.java             ← Correspond à User (frontend)
│   ├── FileResponse.java
│   └── ApiErrorResponse.java
├── entity/
│   ├── User.java                     ← Table "users"
│   ├── Role.java                     ← Enum USER | ADMIN
│   └── FileDocument.java             ← Table "file_documents"
├── exception/
│   ├── GlobalExceptionHandler.java   ← Mapping exceptions → HTTP
│   ├── EmailAlreadyExistsException.java
│   ├── PasswordMismatchException.java
│   ├── FileNotFoundException.java
│   └── InvalidFileException.java
├── repository/
│   ├── UserRepository.java
│   └── FileDocumentRepository.java
├── security/
│   ├── JwtService.java               ← Génération / validation JWT
│   └── JwtAuthenticationFilter.java  ← Filtre Bearer token
└── service/
    ├── AuthService.java              ← Logique register / login
    └── FileService.java              ← Logique upload / download / delete
```

---

## Installation et démarrage

### 1. Prérequis

- Java 17+
- Maven 3.8+
- PostgreSQL 15+

### 2. Créer la base de données

```bash
psql -U postgres
```

```sql
CREATE DATABASE filemanager_db WITH ENCODING = 'UTF8';
```

### 3. Configurer `application.properties`

```properties
# Adapter selon votre installation PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/filemanager_db
spring.datasource.username=postgres
spring.datasource.password=VOTRE_MOT_DE_PASSE

# Dossier où les PDF seront stockés sur le serveur
app.upload.dir=./uploads

# URL(s) de votre frontend React
app.cors.allowed-origins=http://localhost:5173

# Clé JWT — OBLIGATOIRE DE CHANGER EN PRODUCTION
# Générer avec : openssl rand -hex 32
app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
app.jwt.expiration=86400000   # 24h en millisecondes
```

### 4. Lancer le serveur

```bash
cd backend
mvn spring-boot:run
```

Le serveur démarre sur `http://localhost:8080`.
Hibernate crée automatiquement les tables au premier démarrage.

---

## API Endpoints

### Authentification (public)

| Méthode | URL | Body | Réponse |
|---|---|---|---|
| POST | `/api/auth/register` | `{ email, firstName, lastName, password, confirmPassword }` | `{ token, user }` |
| POST | `/api/auth/login` | `{ email, password }` | `{ token, user }` |

### Fichiers (JWT requis → `Authorization: Bearer <token>`)

| Méthode | URL | Description |
|---|---|---|
| POST | `/api/files/upload` | Upload un PDF (multipart/form-data, champ `file`) |
| GET | `/api/files` | Liste les fichiers de l'utilisateur connecté |
| GET | `/api/files/{id}` | Métadonnées d'un fichier |
| GET | `/api/files/{id}/download` | Télécharge le PDF |
| DELETE | `/api/files/{id}` | Supprime un fichier |

---

## Intégration côté React / Axios

### Login
```typescript
const response = await axios.post<AuthResponse>('/api/auth/login', credentials);
localStorage.setItem('token', response.data.token);
```

### Intercepteur Axios (ajouter à votre config)
```typescript
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
```

### Upload
```typescript
const formData = new FormData();
formData.append('file', selectedFile); // File doit être un PDF

const response = await axios.post<FileResponse>('/api/files/upload', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
});
```

### Téléchargement
```typescript
const response = await axios.get(`/api/files/${fileId}/download`, {
  responseType: 'blob'
});
const url = URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
// Pour lire au frontend :
window.open(url); // Ouvre le PDF dans un nouvel onglet
// Pour forcer le téléchargement :
const a = document.createElement('a');
a.href = url;
a.download = fileName;
a.click();
```

---

## Codes d'erreur API

| Code | HTTP | Description |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Champ de formulaire invalide (avec `field`) |
| `PASSWORD_MISMATCH` | 400 | Les mots de passe ne correspondent pas |
| `BAD_CREDENTIALS` | 401 | Email ou mot de passe incorrect |
| `EMAIL_ALREADY_EXISTS` | 409 | Email déjà utilisé |
| `FILE_NOT_FOUND` | 404 | Fichier inexistant ou non autorisé |
| `INVALID_FILE` | 400 | Fichier non PDF ou corrompu |
| `FILE_TOO_LARGE` | 413 | Fichier > 20 Mo |
| `INTERNAL_ERROR` | 500 | Erreur serveur |

---

## Sécurité — points clés

- **Mots de passe** : hachés avec BCrypt (coût 12), jamais stockés en clair
- **JWT** : signé avec HMAC-SHA256, expire après 24h
- **Isolation des fichiers** : un utilisateur ne peut voir/télécharger/supprimer que SES fichiers
- **Validation PDF** : vérification de l'extension, du Content-Type ET des magic bytes `%PDF-`
- **CORS** : configuré pour n'accepter que les origines déclarées

---

## Production — checklist

- [ ] Changer `app.jwt.secret` (minimum 32 octets, base64)
- [ ] Changer `spring.jpa.hibernate.ddl-auto` → `validate` ou `none`
- [ ] Configurer HTTPS
- [ ] Stocker les uploads sur un volume persistant (pas `./uploads`)
- [ ] Créer un utilisateur PostgreSQL dédié avec droits limités
- [ ] Externaliser les secrets via variables d'environnement