# 1. Build Stage
FROM node:22-alpine AS build
WORKDIR /app

RUN corepack enable && corepack prepare pnpm@10.17.1 --activate

COPY package.json pnpm-lock.yaml ./
# pnpm 패키지 저장소를 빌드 캐시에 두어, 잠금 파일이 바뀌어도 이미 받은 패키지는 다시 내려받지 않는다.
RUN --mount=type=cache,id=pnpm-store,target=/pnpm/store pnpm install --frozen-lockfile --store-dir /pnpm/store

COPY . .
ARG VITE_SHOW_DEMO_ACCOUNT=false
ENV VITE_SHOW_DEMO_ACCOUNT=$VITE_SHOW_DEMO_ACCOUNT
RUN pnpm run build

# 2. Production Static Serving Stage
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
