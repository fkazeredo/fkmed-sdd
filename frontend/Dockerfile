# FKMed frontend — SPA build + nginx reverse proxy (same-origin SPA + API + AS,
# DECISIONS-BASELINE §0018). Build context is the `frontend/` directory.
# BACKEND_HOST is resolved at container start by the official nginx template mechanism.

FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json .npmrc ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
ENV BACKEND_HOST=app:8080
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
