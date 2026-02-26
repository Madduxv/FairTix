# Setup instructions

## Requirements:
 - [Docker](https://www.docker.com/get-started)
 - [Docker Compose](https://docs.docker.com/compose/install/)

## Setup Steps
1. **Clone the Repo**

```bash
git clone https://github.com/Madduxv/FairTix.git
cd FairTix
```

2. Create a .env file in the root of the project
 - Copy from the template:
```bash
cp .env.example .env
```
 - Change the environment variables
```bash
POSTGRES_DB=changeme
POSTGRES_USER: changeme
POSTGRES_PASSWORD: changeme
JWT_SECRET=superlongrandomsecuresecretkeythatnobodycommits
```
> [!NOTE]
> JWT_SECRET should be at least 64 characters long

To start:
```
docker compose up --build
```

To stop:
```
docker compose down
```
