#!/usr/bin/env bash
# =====================================================================
#  LÀM LẠI DỮ LIỆU TỪ ĐẦU (chế độ dev) — giống "php artisan migrate:fresh --seed" của Laravel
#    1. Xoá sạch: database MySQL, database MongoDB, dữ liệu Redis, ảnh trong uploads/
#    2. Khởi động lại app → Hibernate tạo lại bảng theo entity MỚI NHẤT → DataSeeder chèn dữ liệu mẫu
#  KHÁC "down -v": không xoá cache thư viện Gradle, không bắt MySQL khởi tạo lại → nhanh hơn nhiều
#
#  Chạy từ thư mục gốc project:   ./scripts/reset-db.sh       (hỏi lại trước khi xoá)
#                                 ./scripts/reset-db.sh -y    (không hỏi)
# =====================================================================
set -e   # một lệnh bị lỗi → dừng script ngay, không chạy tiếp các bước sau

cd "$(dirname "$0")/.."   # luôn đứng ở thư mục gốc project, dù gọi script từ đâu

# Đọc .env (nếu có) để dùng ĐÚNG tên DB / mật khẩu như docker-compose đang dùng
if [ -f .env ]; then
    set -a; . ./.env; set +a
fi
# Chưa khai báo trong .env → dùng giá trị mặc định, giống hệt docker-compose.dev.yml
DB_NAME=${DB_NAME:-demo_db}
DB_USERNAME=${DB_USERNAME:-demo_user}
DB_PASSWORD=${DB_PASSWORD:-demo_pass}
MONGO_DB=${MONGO_DB:-demo_db}
MONGO_USERNAME=${MONGO_USERNAME:-demo_user}
MONGO_PASSWORD=${MONGO_PASSWORD:-demo_pass}

COMPOSE="docker compose -f docker-compose.dev.yml"

if [ "$1" != "-y" ]; then
    read -p "⚠️  Xoá SẠCH dữ liệu MySQL, MongoDB, Redis và ảnh upload? Gõ y để tiếp tục: " answer
    if [ "$answer" != "y" ]; then
        echo "Đã huỷ."
        exit 0
    fi
fi

echo "==> [1/6] Bật các DB (nếu chưa chạy) và chờ tới khi healthy"
$COMPOSE up -d --wait mysql mongo redis

echo "==> [2/6] Dừng app, để nó không đọc/ghi DB trong lúc đang xoá"
$COMPOSE stop app-dev

echo "==> [3/6] MySQL: xoá database $DB_NAME rồi tạo lại (trống, chưa có bảng nào)"
# MYSQL_PWD: đưa mật khẩu qua biến môi trường → mysql không in cảnh báo "password on the command line"
docker exec -e MYSQL_PWD="$DB_PASSWORD" demo-mysql \
    mysql -u "$DB_USERNAME" -e "DROP DATABASE IF EXISTS $DB_NAME; CREATE DATABASE $DB_NAME;"

echo "==> [4/6] MongoDB: xoá database $MONGO_DB — Redis: xoá mọi key"
docker exec demo-mongo \
    mongosh --quiet -u "$MONGO_USERNAME" -p "$MONGO_PASSWORD" --authenticationDatabase admin \
    "$MONGO_DB" --eval "db.dropDatabase()"
docker exec demo-redis redis-cli FLUSHDB

echo "==> [5/6] Xoá ảnh đã upload (DB mới không còn sản phẩm nào trỏ tới chúng)"
rm -f uploads/*

echo "==> [6/6] Bật lại app, chờ seeder chạy xong (thường mất 30-60 giây)..."
since=$(date +%s)   # mốc "bây giờ" (số giây từ 1970) → chỉ đọc log từ đây, bỏ qua log của các lần chạy trước
$COMPOSE up -d app-dev

waited=0
for i in $(seq 1 60); do   # thử tối đa 60 lần × 5 giây = 5 phút
    sleep 5
    logs=$(docker logs --since "$since" demo-app-dev 2>&1)

    if echo "$logs" | grep -q "APPLICATION FAILED TO START"; then
        echo "❌ App không khởi động được. Xem lỗi: $COMPOSE logs app-dev"
        exit 1
    fi

    # Seeder chạy SAU dòng "Started DemoApplication", và sau cả NoSqlConnectionCheck (bài 14)
    # → chờ chính dòng log [Seed], không chờ dòng "Started"
    if echo "$logs" | grep -qi "\[seed\]"; then
        sleep 2   # cho seeder ghi nốt các dòng log còn lại
        echo "✅ Xong. Log của seeder:"
        docker logs --since "$since" demo-app-dev 2>&1 | grep -i "\[seed\]"
        exit 0
    fi

    # App đã "Started" được 1 phút (12 lần × 5 giây) mà vẫn không thấy [Seed] → nhiều khả năng chưa có DataSeeder
    if echo "$logs" | grep -q "Started DemoApplication"; then
        waited=$((waited + 1))
        if [ "$waited" -ge 12 ]; then
            echo "⚠️  App đã chạy nhưng không thấy log [Seed] nào. Đã làm bài 13 (DataSeeder) chưa?"
            exit 1
        fi
    fi
done

echo "⏳ Quá 5 phút app vẫn chưa lên. Xem log: $COMPOSE logs -f app-dev"
exit 1