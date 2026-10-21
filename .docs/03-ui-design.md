# 03 — Thiết kế giao diện mobile

## Cấu trúc điều hướng

Ứng dụng dùng một Activity và ba đích cấp cao trong bottom navigation:

| Đích | Mục đích |
|------|----------|
| **Thu thập** | Chọn nguồn, nhập danh sách và chạy xử lý |
| **Kết quả** | Duyệt, lọc, chọn và chia sẻ dữ liệu |
| **Cài đặt** | Theme, mạng, cache và tùy chọn theo nguồn |

Chi tiết kết quả, tìm sản phẩm và trạng thái một lần chạy là màn hình cấp hai. Chúng không chiếm một mục cố định trên thanh điều hướng.

## Màn hình Thu thập

Thứ tự từ trên xuống:

1. Top app bar với tên màn hình và hành động trợ giúp.
2. Source selector dạng exposed dropdown, hiển thị icon và tên nguồn.
3. Ô nhập nhiều dòng có nút dán; mỗi dòng là một mã hoặc URL.
4. Hàng hành động phụ: **Tìm sản phẩm**, **Thêm hàng loạt**, **Xóa**.
5. Danh sách mục đã nhận dạng với trạng thái validation tại từng dòng.
6. Thanh hành động bám đáy chứa số mục hợp lệ và nút **Bắt đầu**.

Khi bàn phím mở, nội dung cuộn đến trường đang nhập và thanh hành động không che nội dung. Nút **Bắt đầu** bị vô hiệu hóa nếu không có mục hợp lệ và phải kèm giải thích trong ngữ cảnh.

## Tìm sản phẩm

- Mở thành màn hình toàn chiều cao để bàn phím và danh sách có đủ không gian.
- Search field tự focus, hỗ trợ xóa nhanh và gửi từ IME.
- Kết quả là danh sách card có checkbox, tiêu đề, mã và nguồn.
- Thanh đáy hiển thị số mục đã chọn cùng nút **Thêm**.
- Query mới hủy kết quả request cũ; trạng thái rỗng phân biệt “chưa tìm”, “không có kết quả” và “lỗi mạng”.

## Tiến trình xử lý

- Hiển thị tiến trình tổng thể và danh sách từng mục.
- Trạng thái gồm `Chờ`, `Đang xử lý`, `Hoàn tất`, `Lỗi` và `Đã hủy`.
- Cho phép rời màn hình mà không mất hàng đợi đã lưu.
- Hành động **Hủy** yêu cầu xác nhận nếu còn request đang chạy.
- Sau khi hoàn tất, hành động chính chuyển thành **Xem kết quả**; mục lỗi có nút **Thử lại**.

## Màn hình Kết quả

- Dùng `LazyColumn` với card, không dùng bảng ngang nhiều cột.
- Mỗi card hiển thị tiêu đề, mã, nguồn, một dòng metadata quan trọng và thumbnail tùy chọn.
- Search bar và filter chip lọc theo nguồn hoặc trạng thái.
- Nhấn card mở màn hình chi tiết; nhấn giữ bắt đầu chế độ chọn nhiều.
- Khi chọn nhiều, contextual top app bar cung cấp **Chia sẻ**, **Xuất** và **Xóa**.

### Chi tiết kết quả

- Header có tiêu đề, nguồn, trạng thái và ảnh.
- Metadata chia thành các section có tiêu đề; trường trống không xuất hiện.
- URL mở bằng ứng dụng phù hợp qua intent của Android.
- Các hành động thường dùng nằm ở cuối hoặc bottom action bar: **Sao chép**, **Chia sẻ**, **Mở liên kết**.

## Màn hình Cài đặt

Dùng danh sách section một cột:

- **Giao diện:** theo hệ thống, sáng hoặc tối.
- **Xử lý:** giới hạn song song, timeout và tùy chọn retry.
- **Nguồn dữ liệu:** thiết lập riêng của từng adapter; secret không hiển thị lại toàn bộ.
- **Dữ liệu cục bộ:** dung lượng cache, xóa cache và xóa lịch sử.
- **Giới thiệu:** phiên bản ứng dụng, giấy phép và chính sách dữ liệu.

Thay đổi đơn giản được lưu ngay. Hành động phá hủy dữ liệu phải có dialog xác nhận và mô tả rõ phạm vi bị xóa.

## Trạng thái chung

Mỗi màn hình phải thiết kế đủ các trạng thái:

- Loading có nội dung khung hoặc progress phù hợp, không chặn toàn bộ màn hình nếu không cần.
- Empty state có mô tả ngắn và một hành động chính.
- Error state nói rõ người dùng có thể thử lại, sửa input hay kiểm tra mạng.
- Snackbar dùng cho phản hồi ngắn có thể hoàn tác; lỗi cần quyết định không được chỉ hiển thị bằng snackbar.
- Dialog chỉ dùng cho xác nhận quan trọng hoặc dữ liệu bắt buộc.

## Kích thước và khả năng tiếp cận

- Thiết kế phone-first ở chiều dọc; chiều ngang và màn hình lớn có thể dùng hai pane cho danh sách–chi tiết.
- Tôn trọng system bars, display cutout và IME insets.
- Vùng chạm tối thiểu 48 dp; khoảng cách đủ để tránh chạm nhầm.
- Không truyền đạt trạng thái chỉ bằng màu; luôn có icon, nhãn hoặc mô tả.
- Hỗ trợ font scale lớn mà không cắt nút hoặc che nội dung quan trọng.
- Icon tương tác có content description; thứ tự focus TalkBack theo đúng thứ tự đọc.
