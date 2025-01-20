# KeyCloak Custom User Storage Provider

## Bước 1: Clean and Build
nvm clean install để có file jar trong thư mục target

## Bước 2: Kiểm tra Docker file, file jar
- Chạy lệnh `docker build -t keycloak-custom-user-provider .` để build image
- Chạy lệnh `docker run -p 8180:8080 keycloak-custom-user-provider` để chạy container