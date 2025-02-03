# KeyCloak Custom User Storage Provider

## Bước 1: Clean and Build
nvm clean install để có file jar trong thư mục target

## Bước 2: Kiểm tra Docker file, file jar
- Chạy lệnh `docker build -t user-provider-kc .` để build image
- Chạy lệnh `docker run -p 8180:8080 user-provider-kc` để chạy container