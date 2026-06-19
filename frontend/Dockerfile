# 第一階段：Vite 建置
FROM node:18 AS build
WORKDIR /app
COPY package*.json ./
RUN npm install --include=optional
# npm/cli#4828 workaround: oxide native binding 有時不會被自動安裝
RUN OXIDE_VER=$(node -e "const p=require('./node_modules/@tailwindcss/oxide/package.json');console.log((p.optionalDependencies||{})['@tailwindcss/oxide-linux-x64-gnu']||p.version)") \
    && npm install --no-save "@tailwindcss/oxide-linux-x64-gnu@${OXIDE_VER}"
COPY . .
RUN npm run build

# 第二階段：nginx 靜態服務 + API 代理
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
