# --- Build stage: install deps + build Next.js ---
FROM node:20-alpine AS build

WORKDIR /app

# 1) Copy only dependency files first
COPY package.json package-lock.json ./

# 2) Install dependencies in clean, reproducible way
RUN npm ci

# 3) Copy the rest of the app source code
COPY . .

# 4) Build the Next.js app
RUN npm run build



# --- Runtime stage: minimal image to run the built app ---
FROM node:20-alpine AS run

WORKDIR /app

# 7) Copy everything from the build's /app into this image
COPY --from=build /app ./

# 8) Document that this container listens on 3000
EXPOSE 3000

# 9) Start Next.js
CMD ["npm", "run", "start"]
