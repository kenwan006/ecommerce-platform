FROM node:20-bookworm-slim AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY env.js.template /etc/nginx/templates/env.js.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080
