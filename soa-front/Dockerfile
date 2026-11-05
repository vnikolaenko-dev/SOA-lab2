FROM node:24-bookworm-slim AS base

WORKDIR /app
RUN npm install --global pnpm@11.24.0

FROM base AS build

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile
COPY . .
RUN pnpm build

FROM base AS production

ENV NODE_ENV=production \
    HOME=/home/node \
    HOST=0.0.0.0 \
    PORT=3000

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN pnpm install --prod --frozen-lockfile
COPY --from=build /app/dist ./dist
COPY --chmod=755 docker-entrypoint.sh /usr/local/bin/frontend-entrypoint

EXPOSE 3000
ENTRYPOINT ["frontend-entrypoint"]
CMD ["node", "node_modules/srvx/bin/srvx.mjs", "--prod", "--tls", "--cert=/certs/service/fullchain.pem", "--key=/run/frontend-tls/tls.key", "-s", "../client", "dist/server/index.js"]
