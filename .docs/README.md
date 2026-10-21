# Android — trạng thái dự án

Ứng dụng sử dụng **Kotlin**, **Jetpack Compose** và **Material 3**.

## Cấu hình hiện tại

| Mục | Giá trị |
|-----|---------|
| Application ID | `com.elp.datatool` |
| Module | `app` |
| Min SDK | 24 |
| Target / Compile SDK | 37 |
| Java compatibility | 11 |

## Trạng thái triển khai

Mã nguồn hiện mới có `MainActivity`, theme và màn hình Compose mẫu. Adapter, lưu trữ, xử lý hàng đợi và các màn hình sản phẩm trong tài liệu dưới đây là **thiết kế mục tiêu**, chưa phải tính năng đã hoàn thành.

## Thứ tự đọc

1. [`01-product-overview.md`](./01-product-overview.md) — phạm vi và luồng nghiệp vụ.
2. [`02-architecture.md`](./02-architecture.md) — ranh giới các lớp và dữ liệu.
3. [`03-ui-design.md`](./03-ui-design.md) — đặc tả giao diện mobile.
4. [`.knowledge/index/by-id.yaml`](../../.knowledge/index/by-id.yaml) — tra cứu cách làm việc với từng nguồn dữ liệu.

Mỗi tính năng chỉ được đánh dấu đã triển khai khi có mã nguồn và kiểm thử tương ứng.
