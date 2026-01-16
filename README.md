# Setup instructions

## Requirements:
 - [Docker](https://www.docker.com/get-started)
 - [Docker Compose](https://docs.docker.com/compose/install/)

## Setup Steps
1. **Clone the Repo**

```bash
git clone https://github.com/Madduxv/FairTix.git
```
or 
```bash
git clone git@github.com:Madduxv/FairTix.git
```

2. Create a .env file in the root of the project
 - Copy from the template:
```bash
cp .env.example .env
```
 - Change the password
```bash
POSTGRES_PASSWORD=changeme
```

To start the backend:
```
docker compose up --build
```

To stop:
```
docker compose down
```
