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

Giao diện Compose đã được dựng theo wireframe trong [`.visualizations/wireframe.html`](../.visualizations/wireframe.html): **Thu thập**, **Tìm sản phẩm**, **Kết quả** (card, lọc, chọn nhiều), **Chi tiết kết quả** và **Cài đặt**, cùng điều hướng và theme sáng/tối.

| Nguồn | Tìm kiếm | Thu thập |
|-------|----------|----------|
| Unity Asset Store | ✅ `/search?q=` (RSC payload) | ✅ `/packages/unknown/{id}` + embedded state / ld+json |
| Amazon Books | — | ✅ `/dp/{ASIN}` + product HTML |
| Steam Store, IGDB | — | — (reports “not available yet”) |

Đã có: `UnityAssetStoreAdapter` (`data/adapter/unity`), HTTP client OkHttp (`data/remote`), hàng đợi xử lý `DefaultCollectionQueue` (giới hạn song song, timeout, tự thử lại lỗi tạm thời, hủy, thử lại thủ công), cache theo mã sản phẩm có thời hạn, ảnh bằng Coil. Kiểm thử parser dùng fixture trong `app/src/test/resources/unity`.

Sao chép kết quả: **Copy line** (CSV/TSV, chọn và sắp xếp cột) và **Copy details** (template có placeholder `{title}`, `{sku}`…), cấu hình trong Cài đặt → Copy; logic định dạng ở `domain/copy/CopyFormatter.kt`.

Còn là **thiết kế mục tiêu**: database và DataStore (kết quả, cài đặt đang lưu trong bộ nhớ, mất khi tắt ứng dụng), WorkManager cho hàng đợi bền vững, xuất file.

## Thứ tự đọc

1. [`01-product-overview.md`](./01-product-overview.md) — phạm vi và luồng nghiệp vụ.
2. [`02-architecture.md`](./02-architecture.md) — ranh giới các lớp và dữ liệu.
3. [`03-ui-design.md`](./03-ui-design.md) — đặc tả giao diện mobile.
4. [`.knowledge/index/by-id.yaml`](../../.knowledge/index/by-id.yaml) — tra cứu cách làm việc với từng nguồn dữ liệu.

Mỗi tính năng chỉ được đánh dấu đã triển khai khi có mã nguồn và kiểm thử tương ứng.
