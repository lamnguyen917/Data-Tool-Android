# 01 — Tổng quan sản phẩm mobile

## Mục tiêu

Data Tool giúp người dùng thu thập metadata sản phẩm theo mã định danh hoặc URL ngay trên thiết bị Android. Dữ liệu từ từng nguồn được chuẩn hóa, lưu cục bộ và có thể chia sẻ hoặc xuất ra tệp bằng giao diện hệ thống.

## Luồng chính

1. Chọn nguồn dữ liệu.
2. Nhập một hoặc nhiều mã, dán danh sách, quét nội dung được chia sẻ vào ứng dụng hoặc tìm theo từ khóa nếu nguồn hỗ trợ.
3. Kiểm tra định dạng và hiển thị lỗi ngay tại từng mục.
4. Bắt đầu xử lý; mỗi mục có trạng thái chờ, đang chạy, thành công hoặc lỗi.
5. Xem kết quả dưới dạng danh sách; mở một mục để xem đầy đủ metadata.
6. Chọn kết quả để sao chép, chia sẻ hoặc xuất tệp qua bộ chọn vị trí lưu của Android.

## Phạm vi chức năng mục tiêu

### Thu thập dữ liệu

- Chấp nhận mã định danh và URL sản phẩm.
- Loại bỏ mục trùng nhưng giữ thứ tự nhập.
- Cho phép sửa hoặc xóa từng mục trước khi chạy.
- Hỗ trợ tìm kiếm từ khóa theo khả năng của từng adapter.
- Có thể thử lại riêng các mục lỗi.

### Kết quả

- Danh sách dùng card với thông tin quan trọng: tiêu đề, mã, nguồn, trạng thái và ảnh nếu có.
- Màn hình chi tiết hiển thị metadata theo nhóm thay vì bảng nhiều cột.
- Bộ lọc theo trạng thái và ô tìm kiếm hoạt động tốt với bàn phím cảm ứng.
- Chia sẻ một hoặc nhiều kết quả bằng Android Sharesheet.
- Xuất dữ liệu thông qua Storage Access Framework; ứng dụng không yêu cầu người dùng nhập đường dẫn thư mục thủ công.

### Lưu phiên

- Danh sách đầu vào và kết quả đang làm việc được lưu cục bộ.
- Khi ứng dụng được mở lại sau khi hệ thống thu hồi tiến trình, trạng thái đã lưu được khôi phục.
- Người dùng có thể xóa một phiên, kết quả hoặc cache từ màn hình phù hợp.

## Nguyên tắc mobile

- Tác vụ chính phải sử dụng được bằng một tay trên màn hình điện thoại.
- Mọi tác vụ chính phải hoàn thành được hoàn toàn bằng thao tác cảm ứng.
- Nút chạm có nhãn rõ ràng và vùng chạm tối thiểu theo Material 3.
- Nút Back của hệ thống đóng lớp đang mở hoặc quay về cấp điều hướng trước; không làm mất dữ liệu chưa xác nhận.
- Trạng thái tải, mất mạng, rỗng và lỗi phải có thông báo cùng hành động tiếp theo.
- Nội dung hỗ trợ cỡ chữ hệ thống, TalkBack và chế độ sáng/tối.

## Ngoài phạm vi hiện tại

- Đồng bộ tài khoản và dữ liệu đa thiết bị.
- Crawl tự động theo lịch khi ứng dụng không được người dùng mở.
- Chạy đồng thời số lượng lớn request không giới hạn.
