# 02 — Kiến trúc Android mục tiêu

Tài liệu này mô tả kiến trúc cần hướng tới. Các thành phần chưa xuất hiện trong mã nguồn không được xem là đã triển khai.

## Phân lớp

```text
presentation/   Compose screens, reusable components, ViewModel, UI state
domain/         Model chuẩn, use case, hợp đồng repository và adapter
data/           HTTP client, parser, cache, database, preferences, export
worker/         Tác vụ bền vững cần tiếp tục ngoài vòng đời màn hình
```

Luồng phụ thuộc đi từ `presentation` đến `domain`; `data` triển khai các hợp đồng do `domain` định nghĩa. Composable không gọi HTTP, parser hoặc database trực tiếp.

## Thành phần chính

| Thành phần | Trách nhiệm |
|------------|-------------|
| `SourceAdapter` | Validate input, tìm kiếm và tải metadata của một nguồn |
| `CollectionRepository` | Điều phối adapter, cache và lưu kết quả |
| `CollectionViewModel` | Quản lý hàng đợi, tiến trình và sự kiện trên màn hình |
| `ResultRepository` | Đọc, lọc, cập nhật và xóa kết quả cục bộ |
| `SettingsRepository` | Lưu tùy chọn nhỏ bằng DataStore |
| `Exporter` | Tạo nội dung xuất và ghi vào URI do hệ thống cấp |

## Hợp đồng adapter

```kotlin
interface SourceAdapter {
    val id: String
    fun validate(input: String): ValidationResult
    suspend fun search(query: String): List<SearchHit>
    suspend fun fetch(input: ProductInput): ProductResult
}
```

- Adapter nhận mã hoặc URL đã chuẩn hóa.
- Lỗi mạng, giới hạn request, trang chặn bot và lỗi parse là các loại lỗi riêng để giao diện đưa ra hành động phù hợp.
- Kiến thức selector, endpoint và quy tắc từng nguồn nằm trong [`.knowledge`](../../.knowledge/), không lặp lại trong tài liệu giao diện.

## Dữ liệu chuẩn

```text
ProductResult
├── sourceId
├── productId
├── title
├── description
├── creators[]
├── categories[]
├── publishedAt
├── canonicalUrl
├── imageUrl
└── attributes: Map<String, String>
```

Các trường riêng của nguồn được giữ trong `attributes`; giao diện chỉ hiển thị trường có giá trị.

## Trạng thái và vòng đời

- Mỗi màn hình nhận một `UiState` bất biến từ `StateFlow`.
- Hành động người dùng đi qua ViewModel; sự kiện dùng một lần như mở Sharesheet không nằm trong state lâu dài.
- Công việc ngắn chạy bằng coroutine gắn với ViewModel.
- Hàng đợi cần tồn tại qua việc ứng dụng rời foreground phải được lưu trước khi chạy và phục hồi bằng cơ chế công việc bền vững của Android.
- Hủy thao tác phải dừng request có thể hủy và giữ lại những kết quả đã hoàn tất.

## Lưu trữ

- Database cục bộ lưu phiên, đầu vào, kết quả và trạng thái hàng đợi.
- DataStore chỉ lưu tùy chọn nhỏ như theme, adapter gần nhất và giới hạn xử lý.
- Cache response có thời hạn và khóa theo nguồn cùng mã sản phẩm.
- Export ghi qua `content://` URI do Storage Access Framework cung cấp.
- Không lưu client secret của dịch vụ bên thứ ba trực tiếp trong APK; nguồn bắt buộc secret cần backend trung gian hoặc cơ chế xác thực phù hợp.

## Mạng và độ tin cậy

- Không chạy I/O trên main thread.
- Giới hạn số request song song theo từng nguồn và tôn trọng retry/backoff.
- Phân biệt mất mạng, timeout, HTTP error, block page, dữ liệu không tồn tại và parse error.
- Chỉ retry tự động lỗi tạm thời; lỗi input phải chờ người dùng sửa.
- Không ghi token, cookie hoặc nội dung nhạy cảm vào log.

## Kiểm thử

- Unit test cho validate, normalize URL và parser bằng fixture cục bộ.
- Unit test ViewModel cho chuyển trạng thái hàng đợi.
- Test database cho lưu và khôi phục phiên.
- Compose UI test cho nhập dữ liệu, lỗi validation, tiến trình và mở chi tiết.
- Integration test mạng phải có giới hạn tần suất và không phụ thuộc vào dữ liệu sản phẩm dễ thay đổi.
