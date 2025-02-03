# KeyCloak Custom User Storage Provider

## Bước 1: Clean and Build
nvm clean install để có file jar trong thư mục target

## Bước 2: Kiểm tra Docker file, file jar
- Chạy lệnh `docker build -t user-provider-kc .` để build image
- Chạy lệnh `docker run -p 8180:8080 user-provider-kc` để chạy container

## Bước 3: Cấu hình Keycloak
- Truy cập vào Keycloak: `http://localhost:8180`
- Đăng nhập với tài khoản admin và tạo tài khoản admin khác với full quyền vì tài khoản admin mặc định sẽ bị xóa
- Tạo realm mới hoặc sử dụng realm mặc định
- Tạo một user federation mới, chọn User Storage Provider là `user-provider-kc`
- Tạo client, thêm `manage-users` role vào service accounts roles trong client
- Setup realm, thời gian token tồn tại, thời gian refresh token tồn tại, tạo user và gán role mặc định, ...